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
package io.meeds.appcenter.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.service.ApplicationBadgeCounter;
import io.meeds.appcenter.service.ApplicationBadgePluginRegistry;

/**
 * Runs against a real cache manager on purpose: a drift between the
 * {@code @Cacheable} and {@code @CacheEvict} keys would make eviction a silent
 * no-op, and a mock-based test could not see it. On this cache that failure
 * means every user keeps a stale badge until the TTL expires, which is exactly
 * the bug the whole eviction path exists to prevent.
 */
@SpringBootTest(classes = { ApplicationBadgeStorage.class, ApplicationBadgeStorageTest.CacheTestConfiguration.class })
@ExtendWith(MockitoExtension.class)
class ApplicationBadgeStorageTest {

  private static final String            BADGE_NAME = "emailUnread";

  private static final String            USERNAME   = "testuser";

  @MockitoBean
  private ApplicationBadgePluginRegistry pluginRegistry;

  @MockitoBean
  private ApplicationBadgeCounter        badgeCounter;

  @Autowired
  private ApplicationBadgeStorage        badgeStorage;

  @Autowired
  private CacheManager                   cacheManager;

  private ApplicationBadgePlugin         plugin;

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(ApplicationBadgeStorage.CACHE_NAME);
    }
  }

  /**
   * The cache manager is a singleton of the shared test context, so an entry
   * left by one test would leak into the next.
   */
  @BeforeEach
  void setup() {
    cacheManager.getCache(ApplicationBadgeStorage.CACHE_NAME).clear();
    plugin = mock(ApplicationBadgePlugin.class);
    lenient().when(pluginRegistry.getPlugin(BADGE_NAME)).thenReturn(plugin);
  }

  @Test
  void readsThroughTheCacheOnlyOnce() {
    when(badgeCounter.count(plugin, USERNAME)).thenReturn(3L);

    assertEquals(3L, badgeStorage.getBadge(BADGE_NAME, USERNAME));
    assertEquals(3L, badgeStorage.getBadge(BADGE_NAME, USERNAME));

    // The same badge is displayed on up to four surfaces at once: displaying it
    // N times must not count it N times
    verify(badgeCounter, times(1)).count(plugin, USERNAME);
  }

  @Test
  void evictionMakesTheNextReadRecompute() {
    when(badgeCounter.count(plugin, USERNAME)).thenReturn(3L, 1L);

    assertEquals(3L, badgeStorage.getBadge(BADGE_NAME, USERNAME));
    badgeStorage.evict(BADGE_NAME, USERNAME);

    // If the evict key did not match the cacheable key, this would still be 3
    assertEquals(1L, badgeStorage.getBadge(BADGE_NAME, USERNAME));
    verify(badgeCounter, times(2)).count(plugin, USERNAME);
  }

  @Test
  void countsAreCachedPerUser() {
    ApplicationBadgePlugin otherPlugin = mock(ApplicationBadgePlugin.class);
    lenient().when(pluginRegistry.getPlugin("chatUnread")).thenReturn(otherPlugin);
    when(badgeCounter.count(plugin, USERNAME)).thenReturn(3L);
    when(badgeCounter.count(plugin, "otheruser")).thenReturn(7L);

    assertEquals(3L, badgeStorage.getBadge(BADGE_NAME, USERNAME));
    // A badge count is personal data: one user's value must never be served to
    // another
    assertEquals(7L, badgeStorage.getBadge(BADGE_NAME, "otheruser"));
  }

  @Test
  void evictingOneUserLeavesTheOthersCached() {
    when(badgeCounter.count(plugin, USERNAME)).thenReturn(3L);
    when(badgeCounter.count(plugin, "otheruser")).thenReturn(7L);
    badgeStorage.getBadge(BADGE_NAME, USERNAME);
    badgeStorage.getBadge(BADGE_NAME, "otheruser");

    badgeStorage.evict(BADGE_NAME, USERNAME);

    assertEquals(7L, badgeStorage.getBadge(BADGE_NAME, "otheruser"));
    verify(badgeCounter, times(1)).count(plugin, "otheruser");
  }

  @Test
  void returnsZeroWhenNoPluginIsRegistered() {
    when(pluginRegistry.getPlugin("unknown")).thenReturn(null);

    assertEquals(0L, badgeStorage.getBadge("unknown", USERNAME));
    verify(badgeCounter, never()).count(any(), anyString());
  }

}
