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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.translation.plugin.TranslationPlugin;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@Component
public class MyApplicationsHeaderTranslationPlugin extends TranslationPlugin {

  public static final String            MY_APPLICATIONS_OBJECT_TYPE = MyApplicationsSettingsService.SETTING_TYPE;

  @Autowired
  private TranslationService            translationService;

  @Autowired
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @PostConstruct
  public void init() {
    translationService.addPlugin(this);
  }

  @Override
  public String getObjectType() {
    return MY_APPLICATIONS_OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(String objectId, String username) {
    return true;
  }

  @Override
  public boolean hasEditPermission(String objectId, String username) {
    if (StringUtils.isNumeric(objectId)) {
      // Legacy key: the random applicationId the title was stored under
      return myApplicationsSettingsService.canEditLegacyHeaderTitle(username);
    }
    return myApplicationsSettingsService.canEditSettings(objectId, username);
  }

  @Override
  public long getAudienceId(String objectId) {
    return 0;
  }

  @Override
  public long getSpaceId(String objectId) {
    return 0;
  }
}
