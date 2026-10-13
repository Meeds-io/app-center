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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleListener;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.appcenter.service.ApplicationCenterService;

import jakarta.annotation.PostConstruct;

/**
 * Deletes the applications Shortcuts portlet windows created in the context
 * of a space when the space is deleted. Runs in the thread deleting the space,
 * not asynchronously: the applications are gone once the deletion returns.
 */
@Component
public class ContextApplicationSpaceListener implements SpaceLifeCycleListener {

  @Autowired
  private SpaceService             spaceService;

  @Autowired
  private ApplicationCenterService applicationCenterService;

  @PostConstruct
  public void init() {
    spaceService.registerSpaceLifeCycleListener(this);
  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {
    applicationCenterService.deleteSpaceApplications(event.getSpace().getSpaceId());
  }

}
