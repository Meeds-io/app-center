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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.service.ApplicationBadgeService;
import io.meeds.appcenter.service.ApplicationBadgeWebSocketService;

import jakarta.annotation.PostConstruct;

/**
 * Carries a badge invalidation to the browser. Pure glue: it holds no business
 * logic and only delegates.
 * <p>
 * Going through the event bus rather than calling the WebSocket service inline
 * is what makes the ordering structural — the cache eviction has necessarily
 * completed before this listener can run — and it lets other domains observe
 * badge changes without App Center knowing about them.
 */
@Component
@Asynchronous
public class ApplicationBadgeWebSocketListener extends Listener<String, String> {

  @Autowired
  private ApplicationBadgeWebSocketService badgeWebSocketService;

  @Autowired
  private ListenerService                  listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(ApplicationBadgeService.BADGE_UPDATED_EVENT, this);
  }

  @Override
  public void onEvent(Event<String, String> event) throws Exception {
    badgeWebSocketService.sendMessage(event.getEventName(), event.getSource(), event.getData());
  }

}
