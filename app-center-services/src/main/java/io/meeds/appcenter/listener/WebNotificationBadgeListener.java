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
package io.meeds.appcenter.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.notification.service.storage.WebNotificationStorage;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.plugin.WebNotificationApplicationBadgePlugin;
import io.meeds.appcenter.service.ApplicationBadgeService;

import jakarta.annotation.PostConstruct;

/**
 * Refreshes the notification centre badge whenever the user's counter changes —
 * a new notification, one read or removed, or the counter reset when the drawer
 * is opened.
 * <p>
 * Pure glue: it holds no counting logic and only reports staleness. Because the
 * notification plugin is self-cached, the eviction is a no-op here and this
 * listener effectively just carries the notification to the browser, which then
 * re-reads the value from the notification domain's own cache.
 */
@Component
@Asynchronous
public class WebNotificationBadgeListener extends Listener<String, Object> {

  @Autowired
  private ApplicationBadgeService applicationBadgeService;

  @Autowired
  private ListenerService         listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(WebNotificationStorage.NOTIFICATION_WEB_BADGE_UPDATED_EVENT, this);
  }

  @Override
  public void onEvent(Event<String, Object> event) throws Exception {
    String username = event.getSource();
    if (StringUtils.isNotBlank(username)) {
      applicationBadgeService.updateBadge(WebNotificationApplicationBadgePlugin.BADGE_NAME, username);
    }
  }

}
