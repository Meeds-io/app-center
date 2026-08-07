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
package io.meeds.appcenter.plugin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.service.ApplicationBadgePluginRegistry;

import jakarta.annotation.PostConstruct;

/**
 * Reports the notification centre counter on its Application Center tile.
 * <p>
 * Reuses {@link WebNotificationService#getNumberOnBadge(String)} — the very
 * value {@code topbarNotification.jsp} renders on the topbar bell — so the tile
 * and the bell are two views of one number and can never disagree. Note this is
 * deliberately <strong>not</strong> an unread count: it counts what the user has
 * not yet seen, and is reset wholesale when they open the drawer, exactly as the
 * bell behaves today.
 * <p>
 * Unlike the other contributors, this plugin lives in App Center rather than in
 * the addon owning the data: notifications live in {@code commons}, which sits
 * <em>below</em> App Center in the dependency tree, so it cannot depend back on
 * the badge SPI.
 */
@Component
public class WebNotificationApplicationBadgePlugin implements ApplicationBadgePlugin {

  private static final Log               LOG        = ExoLogger.getLogger(WebNotificationApplicationBadgePlugin.class);

  public static final String             BADGE_NAME = "webNotifications";

  @Autowired
  private ApplicationBadgePluginRegistry applicationBadgePluginRegistry;

  @Autowired
  private WebNotificationService         webNotificationService;

  /**
   * The drawer this badge belongs to, matching the {@code QuickAction}
   * contributed in {@code quick-actions/extensions.js} and the entry shipped in
   * {@code applications.json}.
   */
  @Value("${appcenter.badge.webNotifications.drawerNames:notifications}")
  private List<String>                   drawerNames;

  @PostConstruct
  public void init() {
    applicationBadgePluginRegistry.addPlugin(this);
  }

  @Override
  public String getName() {
    return BADGE_NAME;
  }

  @Override
  public List<String> getDrawerNames() {
    return drawerNames;
  }

  /**
   * The notification domain already caches this counter — with single-flight,
   * through {@code CachedWebNotificationStorage}'s {@code FutureExoCache} — and
   * invalidates it on every notification change. Caching it a second time in App
   * Center would be two caches over one value with independent eviction, which
   * is precisely the stale-badge pattern the design rejects.
   */
  @Override
  public boolean isSelfCached() {
    return true;
  }

  @Override
  public long countBadge(String username) {
    if (StringUtils.isBlank(username)) {
      return 0;
    }
    try {
      return webNotificationService.getNumberOnBadge(username);
    } catch (Exception e) {
      LOG.warn("Error counting web notifications of user {}", username, e);
      return 0;
    }
  }

}
