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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.storage.utils.MOPUtils;
import org.exoplatform.portal.pom.spi.portlet.Portlet;

import io.meeds.appcenter.model.exception.WindowPreferencesTimeoutException;
import io.meeds.common.ContainerTransactional;

import jakarta.annotation.PreDestroy;

/**
 * The read and the write themselves, and that they run on a thread of the
 * storage: {@link PortletWindowStorage#callInRequest(Supplier)} is not called
 * for real, its advice would start a portal container.
 */
@ExtendWith(MockitoExtension.class)
class PortletWindowStorageTest {

  private static final String  STORAGE_ID = "12";

  @Mock
  private LayoutService        layoutService;

  private PortletWindowStorage storage;

  /** The window's preferences as committed, which a save writes */
  private final AtomicReference<byte[]> committedPreferences = new AtomicReference<>();

  @BeforeEach
  void setUp() {
    storage = spy(new PortletWindowStorage());
    ReflectionTestUtils.setField(storage, "layoutService", layoutService);
  }

  @AfterEach
  void tearDown() {
    storage.stop();
  }

  /**
   * The layout service's cache can hold a render's stale view of the window:
   * fails the mutant reading through it
   */
  @Test
  void readPreferencesReadsTheCommittedWindowNotTheCache() {
    givenTheWindow(portlet("name", "Untitled-12-a", "data.init", "{}"));

    assertEquals(Map.of("name", "Untitled-12-a", "data.init", "{}"), storage.readPreferences(STORAGE_ID));
    verify(layoutService, never()).load(any());
  }

  @Test
  void readPreferencesOfAWindowWithoutPreferencesIsEmpty() {
    when(layoutService.loadCommitted(STORAGE_ID)).thenReturn(new Portlet());

    assertTrue(storage.readPreferences(STORAGE_ID).isEmpty());
  }

  @Test
  void readPreferencesOfAMissingWindowIsEmpty() {
    assertTrue(storage.readPreferences(STORAGE_ID).isEmpty());
  }

  /**
   * Fails the mutants answering true for a window gone, false for one that
   * exists, reading outside a request of its own, where a transaction of its
   * caller decides what the read sees, and reading the window's preferences,
   * as large as an unconsumed payload, only to test that it exists
   */
  @Test
  void existsReadsTheCommittedWindow() {
    givenATaskRunsInTheCallingRequest();
    when(layoutService.existsCommitted(STORAGE_ID)).thenReturn(true);

    assertTrue(storage.exists(STORAGE_ID));
    assertFalse(storage.exists("13"));
    assertFalse(storage.exists(null));
    verify(storage, times(3)).callInRequest(any());
    verify(layoutService, never()).loadCommitted(any());
  }

  /**
   * Fails the mutants writing over the cache's view of the window, which a
   * render's stale entity manager can have filled, and writing through
   * {@link LayoutService#save}, which writes that view's other columns back
   */
  @Test
  void writePreferencesSetsTheValuesOverTheCommittedOnes() {
    givenTheWindow(portlet("name", "Untitled-12-a", "data.init", "{}", "applicationIds", "1"));

    storage.writePreferences(STORAGE_ID, Map.of("data.init", "", "applicationIds", "41"));

    ArgumentCaptor<Portlet> saved = ArgumentCaptor.forClass(Portlet.class);
    verify(layoutService).saveCommitted(eq(STORAGE_ID), saved.capture());
    assertEquals("Untitled-12-a", saved.getValue().getValue("name"));
    assertEquals("", saved.getValue().getValue("data.init"));
    assertEquals("41", saved.getValue().getValue("applicationIds"));
    verify(layoutService, never()).load(any());
    verify(layoutService, never()).save(any(ApplicationState.class), any(Portlet.class));
  }

  @Test
  void writePreferencesOfAMissingWindowWritesNothing() {
    storage.writePreferences(STORAGE_ID, Map.of("data.init", ""));

    verify(layoutService, never()).saveCommitted(any(), any());
    verify(layoutService, never()).save(any(ApplicationState.class), any(Portlet.class));
  }

  /**
   * Fails the mutant reading on the request's thread, where a transaction of
   * the request decides what the read sees
   */
  @Test
  void getPreferencesReadsOnAThreadOfItsOwnInARequestOfItsOwn() {
    givenTheWindow(portlet("name", "Untitled-12-a"));
    AtomicReference<Thread> requestThread = new AtomicReference<>();
    doAnswer(invocation -> {
      requestThread.set(Thread.currentThread());
      return invocation.<Supplier<?>> getArgument(0).get();
    }).when(storage).callInRequest(any());

    assertEquals(Map.of("name", "Untitled-12-a"), storage.getPreferences(STORAGE_ID));
    assertNotEquals(Thread.currentThread(), requestThread.get());
    assertTrue(requestThread.get().getName().startsWith("AppCenter-PortletWindow-"));
  }

  @Test
  void savePreferencesWritesOnAThreadOfItsOwnAndRethrowsItsFailure() {
    IllegalStateException failure = new IllegalStateException("database unreachable");
    doAnswer(invocation -> {
      throw failure;
    }).when(storage).callInRequest(any());

    assertSame(failure, assertThrows(IllegalStateException.class, () -> storage.savePreferences(STORAGE_ID, Map.of("a", "b"))));
  }

  @Test
  void getOrSetPreferenceKeepsTheValueTheWindowHolds() {
    givenTheWindow(portlet("name", "Untitled-12-a"));
    givenATaskRunsInTheCallingRequest();

    assertEquals("Untitled-12-a", storage.getOrSetPreference(STORAGE_ID, "name", "Untitled-12-b"));
    verify(layoutService, never()).saveCommitted(any(), any());
  }

  @Test
  void getOrSetPreferenceSetsTheValueOfAWindowHoldingNone() {
    givenTheWindow(portlet("data.init", "{}"));
    givenATaskRunsInTheCallingRequest();

    assertEquals("Untitled-12-b", storage.getOrSetPreference(STORAGE_ID, "name", "Untitled-12-b"));
    ArgumentCaptor<Portlet> saved = ArgumentCaptor.forClass(Portlet.class);
    verify(layoutService).saveCommitted(eq(STORAGE_ID), saved.capture());
    assertEquals("Untitled-12-b", saved.getValue().getValue("name"));
    assertEquals("{}", saved.getValue().getValue("data.init"));
  }

  /**
   * Two renders naming one window at once, each on a storage thread: the
   * first one's read waits for a second read that the window's lock holds
   * back. Fails the mutant releasing the lock between the read and the write,
   * where the second read comes, both find no name and each writes its own
   */
  @Test
  void getOrSetPreferenceByTwoCallersAtOnceSetsOneValue() throws Exception {
    givenTheCommittedWindow(new Portlet());
    givenATaskRunsInTheCallingRequest();
    AtomicInteger reads = new AtomicInteger();
    CountDownLatch firstRead = new CountDownLatch(1);
    CountDownLatch secondRead = new CountDownLatch(1);
    when(layoutService.loadCommitted(STORAGE_ID)).thenAnswer(invocation -> {
      if (reads.incrementAndGet() == 1) {
        firstRead.countDown();
        secondRead.await(500, TimeUnit.MILLISECONDS);
      } else {
        secondRead.countDown();
      }
      return MOPUtils.unserialize(committedPreferences.get());
    });
    ExecutorService callers = Executors.newFixedThreadPool(2);
    try {
      Future<String> first = callers.submit(() -> storage.getOrSetPreference(STORAGE_ID, "name", "Untitled-12-a"));
      assertTrue(firstRead.await(5, TimeUnit.SECONDS));
      Future<String> second = callers.submit(() -> storage.getOrSetPreference(STORAGE_ID, "name", "Untitled-12-b"));
      assertEquals("Untitled-12-a", first.get(5, TimeUnit.SECONDS));
      assertEquals("Untitled-12-a", second.get(5, TimeUnit.SECONDS));
    } finally {
      callers.shutdownNow();
    }
  }

  /**
   * A caller that stops waiting does not cut the write, running or queued:
   * fails the mutants cancelling it, with or without interrupting it, whose
   * consumption would keep applications its list never names
   */
  @Test
  void savePreferencesQueuedWhenItsCallerStopsWaitingStillWrites() throws Exception {
    givenTheWindow(portlet("data.init", "{}"));
    storage.timeoutMillis = 100;
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch blocked = new CountDownLatch(4);
    AtomicInteger blockedWorkers = new AtomicInteger(4);
    doAnswer(invocation -> {
      if (blockedWorkers.getAndDecrement() > 0) {
        blocked.countDown();
        release.await(5, TimeUnit.SECONDS);
      }
      return invocation.<Supplier<?>> getArgument(0).get();
    }).when(storage).callInRequest(any());
    ExecutorService callers = Executors.newFixedThreadPool(4);
    try {
      for (int i = 0; i < 4; i++) {
        callers.submit(() -> storage.getPreferences(STORAGE_ID));
      }
      // Every storage thread busy: the write is queued when its wait ends
      assertTrue(blocked.await(5, TimeUnit.SECONDS));
      assertThrows(WindowPreferencesTimeoutException.class, () -> storage.savePreferences(STORAGE_ID, Map.of("data.init", "")));
      release.countDown();
      verify(layoutService, timeout(5000)).saveCommitted(eq(STORAGE_ID), any(Portlet.class));
    } finally {
      callers.shutdownNow();
    }
  }

  @Test
  void callOnWindowRunsItsEndWhetherTheTaskSucceedsOrFails() {
    givenTheWindow(portlet("name", "Untitled-12-a"));
    givenATaskRunsInTheCallingRequest();
    AtomicInteger ends = new AtomicInteger();

    assertEquals("Untitled-12-a",
                 storage.callOnWindow(STORAGE_ID, window -> window.getPreferences().get("name"), ends::incrementAndGet));
    assertThrows(IllegalStateException.class, () -> storage.callOnWindow(STORAGE_ID, window -> {
      throw new IllegalStateException("database unreachable");
    }, ends::incrementAndGet));
    assertEquals(2, ends.get());
  }

  /**
   * Fails the mutant leaving the end of a task that could not be queued
   * unrun, whose caller's claim would then never be released
   */
  @Test
  void callOnWindowRunsItsEndWhenTheTaskCannotBeQueued() {
    storage.stop();
    AtomicInteger ends = new AtomicInteger();

    assertThrows(RejectedExecutionException.class, () -> storage.callOnWindow(STORAGE_ID, window -> null, ends::incrementAndGet));
    assertEquals(1, ends.get());
  }

  /**
   * The storage threads are stopped with the context: fails the mutant
   * removing the annotation, which a test calling the method cannot detect
   */
  @Test
  void stopRunsWhenTheContextCloses() throws NoSuchMethodException {
    assertTrue(PortletWindowStorage.class.getMethod("stop").isAnnotationPresent(PreDestroy.class));
  }

  /**
   * The new entity manager comes from the advice: fails the mutant removing
   * the annotation, which a test stubbing the method cannot detect
   */
  @Test
  void callInRequestRunsInARequestOfItsOwn() throws NoSuchMethodException {
    assertTrue(PortletWindowStorage.class.getMethod("callInRequest", Supplier.class)
                                         .isAnnotationPresent(ContainerTransactional.class));
  }

  private void givenTheCommittedWindow(Portlet portlet) {
    committedPreferences.set(MOPUtils.serialize(portlet));
    lenient().when(layoutService.saveCommitted(eq(STORAGE_ID), any(Portlet.class))).thenAnswer(invocation -> {
      committedPreferences.set(MOPUtils.serialize(invocation.getArgument(1, Portlet.class)));
      return true;
    });
  }

  private void givenATaskRunsInTheCallingRequest() {
    doAnswer(invocation -> invocation.<Supplier<?>> getArgument(0).get()).when(storage).callInRequest(any());
  }

  private void givenTheWindow(Portlet portlet) {
    byte[] preferences = MOPUtils.serialize(portlet);
    when(layoutService.loadCommitted(STORAGE_ID)).thenAnswer(invocation -> MOPUtils.unserialize(preferences));
    // What a stale cache entry would answer
    lenient().when(layoutService.load(any())).thenReturn(portlet("name", "stale"));
  }

  private Portlet portlet(String... namesAndValues) {
    Portlet portlet = new Portlet();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      portlet.setValue(namesAndValues[i], namesAndValues[i + 1]);
    }
    return portlet;
  }

}
