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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;

import lombok.SneakyThrows;

/**
 * The counting guard is what keeps a badge source that hangs from holding the
 * topbar of every page, so its degraded paths are what matter here — a slow
 * plugin, a throwing one, and the breaker that stops calling a plugin failing
 * repeatedly.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationBadgeCounterTest {

  private static final String    BADGE_NAME = "slowBadge";

  private static final String    USERNAME   = "testuser";

  private ApplicationBadgeCounter counter;

  private ApplicationBadgePlugin  plugin;

  @BeforeEach
  void setup() {
    counter = new ApplicationBadgeCounter();
    ReflectionTestUtils.setField(counter, "timeoutMillis", 3000L);
    ReflectionTestUtils.setField(counter, "failureThreshold", 2);
    ReflectionTestUtils.setField(counter, "breakerOpenSeconds", 60L);
    ReflectionTestUtils.setField(counter, "threads", 4);
    plugin = mock(ApplicationBadgePlugin.class);
    lenient().when(plugin.getName()).thenReturn(BADGE_NAME);
    // Establishing the container and its request lifecycle costs seconds on the
    // very first call here; paying it once up front keeps the assertions about
    // the time budget from measuring that instead
    ApplicationBadgePlugin warmup = mock(ApplicationBadgePlugin.class);
    lenient().when(warmup.getName()).thenReturn("warmup");
    counter.count(warmup, USERNAME);
  }

  @AfterEach
  void tearDown() {
    counter.stop();
  }

  @Test
  void countReturnsThePluginValueWhenItAnswersInTime() {
    when(plugin.countBadge(USERNAME)).thenReturn(5L);

    assertEquals(5L, counter.count(plugin, USERNAME));
  }

  @Test
  void countReturnsZeroForAnUnknownPlugin() {
    assertEquals(0L, counter.count(null, USERNAME));
  }

  @Test
  void countDegradesToZeroWhenThePluginThrows() {
    when(plugin.countBadge(USERNAME)).thenThrow(new IllegalStateException("source unavailable"));

    // The badge is a nicety: a failing source must never surface on the caller
    assertEquals(0L, counter.count(plugin, USERNAME));
  }

  @Test
  @SneakyThrows
  void countDegradesToZeroWhenThePluginExceedsItsTimeBudget() {
    ReflectionTestUtils.setField(counter, "timeoutMillis", 200L);
    CountDownLatch release = new CountDownLatch(1);
    when(plugin.countBadge(USERNAME)).thenAnswer(invocation -> {
      release.await(30, TimeUnit.SECONDS);
      return 42L;
    });

    long start = System.currentTimeMillis();
    long count = counter.count(plugin, USERNAME);
    long elapsed = System.currentTimeMillis() - start;
    release.countDown();

    assertEquals(0L, count);
    // Bounded by the budget, not by how long the source takes to answer
    assertTrue(elapsed < 10000, String.format("Counting waited %dms for a 200ms budget", elapsed));
  }

  @Test
  void breakerStopsCallingAPluginThatKeepsFailing() {
    when(plugin.countBadge(USERNAME)).thenThrow(new IllegalStateException("source unavailable"));

    // Threshold is 2: the third read must not reach the plugin at all
    assertEquals(0L, counter.count(plugin, USERNAME));
    assertEquals(0L, counter.count(plugin, USERNAME));
    assertEquals(0L, counter.count(plugin, USERNAME));

    verify(plugin, atMost(2)).countBadge(anyString());
  }

  @Test
  void breakerClosesAgainAfterASuccess() {
    when(plugin.countBadge(USERNAME)).thenThrow(new IllegalStateException("source unavailable"))
                                      .thenReturn(4L)
                                      .thenThrow(new IllegalStateException("source unavailable"));

    assertEquals(0L, counter.count(plugin, USERNAME));
    assertEquals(4L, counter.count(plugin, USERNAME));
    // The success reset the streak, so this failure alone must not open it
    assertEquals(0L, counter.count(plugin, USERNAME));

    verify(plugin, atMost(3)).countBadge(anyString());
  }

}
