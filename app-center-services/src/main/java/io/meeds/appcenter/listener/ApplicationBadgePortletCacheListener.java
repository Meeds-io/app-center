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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.storage.ApplicationBadgePortletStorage;
import io.meeds.layout.service.PortletInstanceService;

import jakarta.annotation.PostConstruct;

/**
 * Drops the portlet instance to content id mapping whenever a portlet instance
 * is created, updated or removed.
 * <p>
 * Without this, an administrator adding a portlet instance and binding an
 * application to it would see no badge until the cache expired on its own —
 * looking like the feature is broken. The TTL stays as the backstop; this
 * listener is what makes the change immediate.
 * <p>
 * Pure glue: it holds no logic and only delegates the eviction.
 */
@Component
@Asynchronous
public class ApplicationBadgePortletCacheListener extends Listener<Object, Object> {

  private static final List<String>      EVENT_NAMES = List.of(PortletInstanceService.INSTANCE_CREATED_EVENT,
                                                              PortletInstanceService.INSTANCE_UPDATED_EVENT,
                                                              PortletInstanceService.INSTANCE_DELETED_EVENT);

  @Autowired
  private ApplicationBadgePortletStorage badgePortletStorage;

  @Autowired
  private ListenerService                listenerService;

  @PostConstruct
  public void init() {
    EVENT_NAMES.forEach(eventName -> listenerService.addListener(eventName, this));
  }

  @Override
  public void onEvent(Event<Object, Object> event) throws Exception {
    badgePortletStorage.clearCache();
  }

}
