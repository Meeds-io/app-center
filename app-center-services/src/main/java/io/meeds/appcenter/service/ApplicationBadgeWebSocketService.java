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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.ws.frameworks.cometd.ContinuationService;

import io.meeds.appcenter.model.BadgeWebSocketMessage;
import io.meeds.social.util.JsonUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Notifies a browser that one of its badges changed.
 * <p>
 * The channel is <strong>push-only</strong>: nothing is ever read from it, so
 * the client-controlled-input trust boundary that an inbound handler would have
 * to guard simply does not exist here. The frame carries the badge <strong>name
 * only, never the count</strong> — the browser has to re-fetch through the
 * authenticated REST endpoint, where the ACL is applied again.
 */
@Service
@Slf4j
public class ApplicationBadgeWebSocketService {

  public static final String  COMETD_CHANNEL = "/eXo/Application/AppCenter/Badge";

  public static final String  BADGE_NAME_KEY = "badgeName";

  @Autowired
  private ContinuationService continuationService;

  /**
   * Tells one user's connected sessions that a badge must be refreshed.
   *
   * @param eventName the WebSocket event name the browser listens to
   * @param badgeName the badge that changed
   * @param username the only user notified
   */
  public void sendMessage(String eventName, String badgeName, String username) {
    if (StringUtils.isBlank(badgeName) || StringUtils.isBlank(username)) {
      return;
    }
    try {
      if (!continuationService.isPresent(username)) {
        return;
      }
      String wsMessage = JsonUtils.toJsonString(new BadgeWebSocketMessage(eventName, Map.of(BADGE_NAME_KEY, badgeName)));
      continuationService.sendMessage(username, COMETD_CHANNEL, wsMessage);
    } catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.debug("Could not notify user {} that badge {} changed; it will refresh on next read", username, badgeName, e);
      } else {
        log.warn("Could not notify user {} that badge {} changed; it will refresh on next read. (Enable debug level for full stack trace)",
                 username,
                 badgeName);
      }
    }
  }

}
