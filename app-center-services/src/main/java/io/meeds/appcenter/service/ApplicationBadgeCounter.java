/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;

import jakarta.annotation.PreDestroy;

/**
 * Calls a contributing plugin's {@code countBadge} under a bounded time budget,
 * and stops calling one that keeps failing.
 * <p>
 * Badges are read from the topbar, which renders on every page: a source that
 * hangs — a third-party system behind an unresponsive HTTP call, typically —
 * must degrade to "no badge" rather than hold the request. The call therefore
 * runs on a separate thread and the caller waits at most
 * {@code appcenter.badge.count.timeout} milliseconds for it.
 * <p>
 * The timeout alone is not enough: under load, a hung source would still tie up
 * one thread per read. After
 * {@code appcenter.badge.count.breaker.failureThreshold} consecutive failures a
 * plugin's breaker opens, and its count is served as {@code 0} without calling
 * it at all for
 * {@code appcenter.badge.count.breaker.openSeconds}. A single success closes it
 * again.
 * <p>
 * This is the only protection left for a {@link ApplicationBadgePlugin#isSelfCached()
 * self-cached} plugin, whose cold-cache read App Center does not otherwise
 * mediate. A degraded {@code 0} obtained on the generic path is cached like any
 * other value, so it is served until the cache TTL expires or an event evicts
 * it.
 */
@Component
public class ApplicationBadgeCounter {

  private static final Log                   LOG = ExoLogger.getLogger(ApplicationBadgeCounter.class);

  @Value("${appcenter.badge.count.timeout:2000}")
  private long                               timeoutMillis;

  @Value("${appcenter.badge.count.breaker.failureThreshold:3}")
  private int                                failureThreshold;

  @Value("${appcenter.badge.count.breaker.openSeconds:60}")
  private long                               breakerOpenSeconds;

  @Value("${appcenter.badge.count.threads:10}")
  private int                                threads;

  private ExecutorService                    executorService;

  private final Map<String, PluginBreaker>   breakers = new ConcurrentHashMap<>();

  @PreDestroy
  public void stop() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  /**
   * Counts a badge for one user, degrading to {@code 0} when the plugin is too
   * slow, fails, or has its breaker open.
   *
   * @param  plugin   the contributing plugin
   * @param  username the user the count is computed for
   * @return          the count, {@code 0} when it could not be obtained within
   *                    the time budget
   */
  public long count(ApplicationBadgePlugin plugin, String username) {
    if (plugin == null) {
      return 0L;
    }
    PluginBreaker breaker = breakers.computeIfAbsent(plugin.getName(), name -> new PluginBreaker());
    if (breaker.isOpen()) {
      LOG.debug("Badge {} not counted for user {}: circuit breaker open", plugin.getName(), username);
      return 0L;
    }
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    Future<Long> future;
    try {
      future = getExecutorService().submit(() -> countInContainer(plugin, username, container));
    } catch (RejectedExecutionException e) {
      // Every counting thread is already held by a source that does not answer
      breaker.recordFailure();
      LOG.debug("Badge {} not counted for user {}: no counting thread available", plugin.getName(), username, e);
      return 0L;
    }
    try {
      long count = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
      breaker.recordSuccess();
      return count;
    } catch (TimeoutException e) {
      // Interrupting is best effort: a thread blocked on a socket read ignores
      // it, which is precisely why the breaker exists
      future.cancel(true);
      breaker.recordFailure();
      LOG.debug("Badge {} timed out after {}ms for user {}", plugin.getName(), timeoutMillis, username, e);
      return 0L;
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      LOG.debug("Interrupted while counting badge {} for user {}", plugin.getName(), username, e);
      return 0L;
    } catch (Exception e) {
      breaker.recordFailure();
      LOG.debug("Error counting badge {} for user {}", plugin.getName(), username, e);
      return 0L;
    }
  }

  private long countInContainer(ApplicationBadgePlugin plugin, String username, ExoContainer container) {
    // The count runs off the request thread, so the container context and the
    // request lifecycle the plugin's own Service layer relies on — a JPA
    // EntityManager, typically — have to be re-established here. A plugin that
    // needs neither must still be counted, so failing to establish them is
    // logged and not treated as a failed count.
    boolean lifecycleStarted = false;
    if (container != null) {
      try {
        ExoContainerContext.setCurrentContainer(container);
        RequestLifeCycle.begin(container);
        lifecycleStarted = true;
      } catch (Exception e) {
        LOG.debug("Could not start a request lifecycle to count badge {}", plugin.getName(), e);
      }
    }
    try {
      return plugin.countBadge(username);
    } finally {
      if (lifecycleStarted) {
        RequestLifeCycle.end();
        ExoContainerContext.setCurrentContainer(null);
      }
    }
  }

  private synchronized ExecutorService getExecutorService() {
    if (executorService == null) {
      executorService = Executors.newFixedThreadPool(threads, runnable -> {
        Thread thread = new Thread(runnable, "app-center-badge-counter");
        thread.setDaemon(true);
        return thread;
      });
    }
    return executorService;
  }

  /**
   * Per-plugin failure state. Counts consecutive failures and, past the
   * threshold, keeps the plugin from being called until the open period
   * elapses.
   */
  private class PluginBreaker {

    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    private final AtomicLong    openedUntil         = new AtomicLong();

    private boolean isOpen() {
      long until = openedUntil.get();
      if (until == 0) {
        return false;
      }
      if (System.currentTimeMillis() < until) {
        return true;
      }
      // Let one call through to probe whether the source recovered
      openedUntil.compareAndSet(until, 0);
      return false;
    }

    private void recordSuccess() {
      consecutiveFailures.set(0);
      openedUntil.set(0);
    }

    private void recordFailure() {
      if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
        consecutiveFailures.set(0);
        openedUntil.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(breakerOpenSeconds));
      }
    }
  }

}
