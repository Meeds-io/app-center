/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
import org.springframework.stereotype.Service;

import org.exoplatform.container.PortalContainer;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.social.category.plugin.CategoryPlugin;
import io.meeds.social.category.service.CategoryPluginService;

import jakarta.annotation.PostConstruct;

@Service
public class ApplicationCategoryPlugin implements CategoryPlugin {

  public static final String       OBJECT_TYPE = "appCenter";

  @Autowired
  private PortalContainer          portalContainer;

  @Autowired(required = false)
  private CategoryPluginService    categoryPluginService;

  private ApplicationCenterService applicationCenterService;

  /**
   * Registers this plugin, without which every category read of applications
   * falls back to the default plugin
   */
  @PostConstruct
  public void init() {
    if (categoryPluginService != null) {
      categoryPluginService.addPlugin(this);
    }
  }

  @Override
  public String getType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(String applicationId, String username) {
    if (StringUtils.isBlank(username)) {
      // ApplicationCenterService#canEdit grants a blank username, an internal call
      return false;
    }
    Application application = getApplicationCenterService().getApplication(Long.parseLong(applicationId));
    return application != null
           && ((application.isActive() && getApplicationCenterService().canAccess(application, username))
               || canEdit(applicationId, username));
  }

  @Override
  public boolean canEdit(String applicationId, String username) {
    return getApplicationCenterService().canEdit(username);
  }

  @Override
  public List<Long> getCategoryIds() {
    return getApplicationCenterService().getCategoryIds();
  }

  private ApplicationCenterService getApplicationCenterService() {
    if (applicationCenterService == null) {
      applicationCenterService = portalContainer.getComponentInstanceOfType(ApplicationCenterService.class);
    }
    return applicationCenterService;
  }

}
