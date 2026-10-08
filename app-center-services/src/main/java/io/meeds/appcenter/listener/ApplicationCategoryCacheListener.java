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
package io.meeds.appcenter.listener;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;

import jakarta.annotation.PostConstruct;

/**
 * Clears the applications listed per category when a link or a category
 * changes. Synchronous, so that the change is listed by the next read on this
 * node.
 */
@Component
public class ApplicationCategoryCacheListener extends Listener<Object, Object> {

  public static final List<String> EVENT_NAMES = List.of(CategoryLinkService.EVENT_CATEGORY_LINK_ADDED,
                                                         CategoryLinkService.EVENT_CATEGORY_LINK_REMOVED,
                                                         CategoryService.EVENT_SOCIAL_CATEGORY_CREATED,
                                                         CategoryService.EVENT_SOCIAL_CATEGORY_UPDATED,
                                                         CategoryService.EVENT_SOCIAL_CATEGORY_DELETED);

  @Autowired
  private ApplicationCenterService applicationCenterService;

  @Autowired
  private ListenerService          listenerService;

  @PostConstruct
  public void init() {
    EVENT_NAMES.forEach(eventName -> listenerService.addListener(eventName, this));
  }

  @Override
  public void onEvent(Event<Object, Object> event) {
    applicationCenterService.clearCategoryApplications();
  }

}
