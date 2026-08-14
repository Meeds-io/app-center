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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.service.ApplicationBadgeCounter;
import io.meeds.appcenter.service.ApplicationBadgePluginRegistry;

/**
 * Caches badge counts per {@code badgeName:username}.
 * <p>
 * {@code sync = true} is what keeps the same badge — displayed simultaneously
 * on the launcher card, the "My Applications" tile and the pinned topbar button
 * — from being computed several times at once. It only holds because
 * {@code KernelCacheManagerAdapter} delegates to {@code FutureExoCache}; without
 * that, concurrent misses each run the loader.
 * <p>
 * The cache TTL doubles as a safety net: it bounds staleness for a source that
 * cannot push, it self-heals an eviction that was missed (dropped WebSocket
 * frame, swallowed listener exception), and it bounds how long a cluster node
 * that missed an eviction can serve a stale value.
 */
@Component
public class ApplicationBadgeStorage {

  public static final String             CACHE_NAME = "app-center.badge";

  @Autowired
  private ApplicationBadgePluginRegistry pluginRegistry;

  @Autowired
  private ApplicationBadgeCounter        badgeCounter;

  /**
   * Returns the cached count for a badge, computing it through the contributing
   * plugin on a miss.
   *
   * @param  badgeName the badge identifier
   * @param  username  the user the count is computed for
   * @return           the count, {@code 0} when no plugin is registered under
   *                     that name
   */
  @Cacheable(cacheNames = CACHE_NAME, key = "#p0 + ':' + #p1", sync = true)
  public long getBadge(String badgeName, String username) {
    ApplicationBadgePlugin plugin = pluginRegistry.getPlugin(badgeName);
    // Counting under a time budget matters especially here: with sync = true a
    // plugin that hangs inside the loader would hold the single-flight lock and
    // block every concurrent reader of that badge, not just its own
    return plugin == null ? 0L : badgeCounter.count(plugin, username);
  }

  /**
   * Drops the cached count so that the next read recomputes it. Must run
   * <strong>before</strong> the frontend is notified, otherwise the browser
   * re-fetches the very value it was told to refresh.
   *
   * @param badgeName the badge identifier
   * @param username  the user whose count changed
   */
  @CacheEvict(cacheNames = CACHE_NAME, key = "#p0 + ':' + #p1")
  public void evict(String badgeName, String username) {
    // Cache eviction only
  }

}
