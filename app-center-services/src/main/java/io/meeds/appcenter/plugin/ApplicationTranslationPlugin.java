/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
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

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
public class ApplicationTranslationPlugin extends TranslationPlugin {

  public static final String       APPLICATION_OBJECT_TYPE = "appCenter";

  @Autowired
  private TranslationService       translationService;

  @Autowired
  private ApplicationCenterService applicationCenterService;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return APPLICATION_OBJECT_TYPE;
  }

  /**
   * The labels of an application are read by whoever may see or edit it.
   */
  @Override
  public boolean hasAccessPermission(String applicationId, String username) {
    Application application = getApplication(applicationId);
    return application != null
           && (applicationCenterService.canAccess(application, username)
               || applicationCenterService.canEditContextApplication(application, username));
  }

  /**
   * The labels of an application are written by an administrator, and those
   * of an application created from a Shortcuts portlet by whoever manages
   * that portlet's applications too.
   */
  @Override
  public boolean hasEditPermission(String applicationId, String username) {
    Application application = getApplication(applicationId);
    return application != null && applicationCenterService.canEditContextApplication(application, username);
  }

  @Override
  public long getAudienceId(String applicationId) {
    return 0;
  }

  @Override
  public long getSpaceId(String applicationId) {
    return 0;
  }

  private Application getApplication(String applicationId) {
    long id = NumberUtils.toLong(applicationId, 0);
    return id > 0 ? applicationCenterService.getApplication(id) : null;
  }
}
