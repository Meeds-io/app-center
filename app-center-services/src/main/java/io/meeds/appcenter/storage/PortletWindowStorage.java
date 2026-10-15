/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter.storage;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.jdbc.entity.WindowEntity;
import org.exoplatform.portal.mop.dao.WindowDAO;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.storage.utils.MOPUtils;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.model.exception.WindowPreferencesTimeoutException;
import io.meeds.common.ContainerTransactional;

import jakarta.annotation.PreDestroy;

/**
 * The preferences of a portlet window as committed. A portal request reads a
 * window through an entity manager it keeps for its whole duration, which
 * returns the window as that request first loaded it, whatever another
 * request committed since; the layout service's preferences cache is filled
 * through the entity manager of the request that misses it, so it can hold
 * such a view too. This storage reads the window's row on a thread of its
 * own, in a request of its own, never through that cache, and writes through
 * the layout service, which empties the window's cache entry. Each
 * read-modify-write holds a lock of this node per window on that thread,
 * from its read to its committed write, so that a caller who stops waiting
 * never lets another write run over it.
 */
@Component
public class PortletWindowStorage {

  /**
   * Longest wait for a read or a write of a window's preferences, its time
   * queued included.
   */
  public static final long            TIMEOUT_SECONDS = 10;

  private static final Log            LOG             = ExoLogger.getLogger(PortletWindowStorage.class);

  private static final AtomicInteger  THREAD_COUNT    = new AtomicInteger();

  /** {@link #TIMEOUT_SECONDS}, in milliseconds, a test's to shorten. */
  long                                timeoutMillis   = TIMEOUT_SECONDS * 1000;

  private final ReentrantLock[]       windowLocks     = IntStream.range(0, 64)
                                                                 .mapToObj(i -> new ReentrantLock())
                                                                 .toArray(ReentrantLock[]::new);

  private final ExecutorService       executor        = Executors.newFixedThreadPool(4, runnable -> {
                                                        Thread thread = new Thread(runnable,
                                                                                   "AppCenter-PortletWindow-"
                                                                                       + THREAD_COUNT.incrementAndGet());
                                                        thread.setDaemon(true);
                                                        return thread;
                                                      });

  @Autowired
  private LayoutService               layoutService;

  @Autowired
  private WindowDAO                   windowDAO;

  /**
   * @param storageId the window's storage id
   * @return the window's preferences as committed, by name, empty when the
   *         window does not exist
   */
  public Map<String, String> getPreferences(String storageId) {
    return callInOwnRequest(() -> readPreferences(storageId));
  }

  /**
   * Sets preferences of a window over its committed ones, the others kept.
   * When the wait ends with a {@link WindowPreferencesTimeoutException}, the
   * write still runs, and is committed unless it fails.
   *
   * @param storageId the window's storage id
   * @param values the preferences to set, by name
   */
  public void savePreferences(String storageId, Map<String, String> values) {
    callOnWindow(storageId, window -> {
      window.savePreferences(values);
      return null;
    }, null);
  }

  /**
   * Reads a preference of a window, and sets it when the window holds no
   * value for it, in one read-modify-write.
   *
   * @param storageId the window's storage id
   * @param name the preference name
   * @param value the value to set when the window holds none
   * @return the value the window holds once done
   */
  public String getOrSetPreference(String storageId, String name, String value) {
    return callOnWindow(storageId, window -> {
      String storedValue = window.getPreferences().get(name);
      if (StringUtils.isNotBlank(storedValue)) {
        return storedValue;
      }
      window.savePreferences(Map.of(name, value));
      return value;
    }, null);
  }

  /**
   * Runs a task on a window's preferences as one task of the storage thread,
   * in a request of its own, holding the window's lock from its first read to
   * its last write, and waits for it. The task runs to its end whether the
   * caller still waits or not; {@code onEnd} runs once it ended, whatever its
   * outcome, or at once when it could not be queued.
   *
   * @param storageId the window's storage id
   * @param task the task, given the window's committed preferences
   * @param onEnd run once the task ended, null for nothing
   * @return what the task returns
   * @throws WindowPreferencesTimeoutException when the caller stops waiting,
   *           the task still running
   */
  public <T> T callOnWindow(String storageId, Function<PortletWindow, T> task, Runnable onEnd) {
    Future<T> future;
    try {
      future = executor.submit(() -> {
        try {
          return callInRequest(() -> withWindowLock(storageId, () -> task.apply(new PortletWindow(this, storageId))));
        } finally {
          if (onEnd != null) {
            onEnd.run();
          }
        }
      });
    } catch (RuntimeException e) {
      if (onEnd != null) {
        onEnd.run();
      }
      throw e;
    }
    return await(future);
  }

  /**
   * Runs a task in a request of its own: called on a thread of this storage,
   * where no request is running, it gets its own entity manager.
   */
  @ContainerTransactional
  public <T> T callInRequest(Supplier<T> task) {
    return task.get();
  }

  @PreDestroy
  public void stop() {
    executor.shutdownNow();
  }

  Map<String, String> readPreferences(String storageId) {
    Map<String, String> preferences = new HashMap<>();
    Portlet portlet = readPortlet(storageId);
    if (portlet != null) {
      for (Preference preference : portlet) {
        preferences.put(preference.getName(), preference.getValue());
      }
    }
    return preferences;
  }

  void writePreferences(String storageId, Map<String, String> values) {
    // Read first: the layout service's model of a window gone is no null
    Portlet portlet = readPortlet(storageId);
    ApplicationState state = portlet == null ? null : getState(storageId);
    if (state == null) {
      LOG.debug("Window {} no longer exists, its preferences are not written", storageId);
      return;
    }
    Map<String, Preference> preferences = new LinkedHashMap<>();
    for (Preference preference : portlet) {
      preferences.put(preference.getName(), preference);
    }
    Portlet updated = new Portlet(preferences);
    values.forEach(updated::setValue);
    layoutService.save(state, updated);
  }

  /**
   * @return the window's preferences as its row holds them, null when the
   *         window does not exist
   */
  private Portlet readPortlet(String storageId) {
    long id = NumberUtils.toLong(storageId, 0);
    WindowEntity window = id > 0 ? windowDAO.find(id) : null;
    if (window == null) {
      return null;
    }
    byte[] customization = window.getCustomization();
    return customization == null ? new Portlet() : (Portlet) MOPUtils.unserialize(customization);
  }

  private <T> T withWindowLock(String storageId, Supplier<T> task) {
    ReentrantLock lock = windowLocks[Math.floorMod(storageId.hashCode(), windowLocks.length)];
    lock.lock();
    try {
      return task.get();
    } finally {
      lock.unlock();
    }
  }

  private ApplicationState getState(String storageId) {
    Application applicationModel = layoutService.getApplicationModel(storageId);
    return applicationModel == null ? null : applicationModel.getState();
  }

  /**
   * Runs a task on a thread of this storage and waits for it.
   */
  private <T> T callInOwnRequest(Supplier<T> task) {
    return await(executor.submit(() -> callInRequest(task)));
  }

  /**
   * Waits for a task of this storage: a task the caller stops waiting for is
   * never cancelled, whether it already runs or is queued, so that a write
   * is always committed or failed, never cut.
   */
  private <T> T await(Future<T> future) {
    try {
      return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WindowPreferencesTimeoutException("Interrupted while reading or writing window preferences", e);
    } catch (TimeoutException e) {
      throw new WindowPreferencesTimeoutException("Window preferences not read or written in time", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Unable to read or write window preferences", e.getCause());
    }
  }

}
