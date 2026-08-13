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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationBadgeProvider;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.storage.ApplicationBadgeStorage;
import io.meeds.appcenter.storage.ApplicationCenterStorage;

/**
 * Serves the per-user count displayed on an application tile, and propagates
 * its invalidation.
 * <p>
 * A count stays fresh in one of two ways, with no scheduled job on either side:
 * <ul>
 * <li>through <strong>events</strong>, for a source whose changes the platform
 * can observe — the owning addon calls
 * {@link #updateBadge(String, String)} from a glue listener;</li>
 * <li>through <strong>cache expiry then lazy recompute</strong>, for a source
 * it cannot observe. Nothing is polled, so a user who never looks at their
 * badges costs nothing.</li>
 * </ul>
 */
@Service
public class ApplicationBadgeService {

  private static final Log                     LOG                 = ExoLogger.getLogger(ApplicationBadgeService.class);

  /** Broadcast after eviction; the glue listener turns it into a WebSocket frame. */
  public static final String                   BADGE_UPDATED_EVENT = "appcenter.badge.updated";

  @Autowired
  private ApplicationBadgePluginRegistry       pluginRegistry;

  @Autowired
  private ApplicationBadgeStorage              badgeStorage;

  @Autowired
  private ApplicationBadgeCounter              badgeCounter;

  @Autowired
  private ApplicationCenterStorage             appCenterStorage;

  @Autowired
  private ApplicationCenterService             appCenterService;

  @Autowired
  private ListenerService                      listenerService;

  /**
   * Reads a badge count for one user.
   *
   * @param  badgeName               the badge identifier
   * @param  username                the authenticated user, resolved by the
   *                                   caller from the session
   * @return                         the count, 0 when the badge does not apply
   *                                   to that user
   * @throws ObjectNotFoundException when no plugin is registered under that
   *                                   name
   * @throws IllegalAccessException  when the badge is bound to no application
   *                                   the user may access
   */
  public long getBadge(String badgeName, String username) throws ObjectNotFoundException, IllegalAccessException {
    ApplicationBadgePlugin plugin = pluginRegistry.getPlugin(badgeName);
    if (plugin == null) {
      throw new ObjectNotFoundException(String.format("Badge %s doesn't exist", badgeName));
    }
    if (!canAccessBadge(plugin, username)) {
      throw new IllegalAccessException(String.format("User %s isn't allowed to access badge %s", username, badgeName));
    }
    if (!plugin.isEnabled(username)) {
      return 0L;
    }
    // A self-cached plugin owns its own caching and single-flight, so it is
    // called directly instead of through the App Center badge cache. Its cold
    // cache is then unmediated, which is why the counter's time budget and
    // breaker apply here too
    return plugin.isSelfCached() ? badgeCounter.count(plugin, username) : badgeStorage.getBadge(badgeName, username);
  }

  /**
   * Invalidates a user's badge and tells their browser to refresh it.
   * <p>
   * The eviction runs first, and the notification is only ever a broadcast:
   * that ordering is what stops the browser from re-fetching the stale value it
   * was just told to refresh.
   *
   * @param badgeName the badge that changed
   * @param username  the user whose count changed
   */
  public void updateBadge(String badgeName, String username) {
    if (StringUtils.isBlank(badgeName) || StringUtils.isBlank(username)) {
      return;
    }
    badgeStorage.evict(badgeName, username);
    try {
      listenerService.broadcast(BADGE_UPDATED_EVENT, badgeName, username);
    } catch (Exception e) {
      LOG.warn("Error broadcasting badge update of {} for user {}", badgeName, username, e);
    }
  }

  /**
   * Resolves which badge an application displays: an explicitly bound name
   * wins, otherwise a Drawer or Portlet entry matches the url declared by a
   * plugin.
   *
   * @param  application the catalog entry
   * @return             the badge identifier, or null when the application
   *                       carries none
   */
  public String getBadgeName(Application application) {
    return pluginRegistry.resolveBadgeName(application);
  }

  /**
   * @return every registered badge provider with the urls it declares, so that
   *         the administration form can resolve an internal application's
   *         binding without asking anyone
   */
  public List<ApplicationBadgeProvider> getBadgeProviders() {
    return pluginRegistry.getPlugins()
                         .stream()
                         .map(plugin -> new ApplicationBadgeProvider(plugin.getName(),
                                                                     plugin.getDeclaredUrls(ApplicationType.DRAWER),
                                                                     plugin.getDeclaredUrls(ApplicationType.PORTLET)))
                         .sorted((first, second) -> StringUtils.compare(first.name(), second.name()))
                         .toList();
  }

  /**
   * A badge may only be read by a user allowed to access at least one of the
   * applications it is bound to. Without this, any authenticated user could
   * read any other application's counter.
   */
  private boolean canAccessBadge(ApplicationBadgePlugin plugin, String username) {
    List<Application> applications = appCenterStorage.getApplicationsByBadge(plugin.getName(), pluginRegistry.getBoundUrls(plugin));
    return CollectionUtils.isNotEmpty(applications)
           && applications.stream().anyMatch(application -> appCenterService.canAccess(application, username));
  }

}
