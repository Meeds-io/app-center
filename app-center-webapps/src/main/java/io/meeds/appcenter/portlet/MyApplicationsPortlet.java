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

package io.meeds.appcenter.portlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.portlet.CMSPortlet;

public class MyApplicationsPortlet extends CMSPortlet {

  private static final String           APPLICATION_ID = "applicationId";

  private MyApplicationsSettingsService settingsService;

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.contentType = MyApplicationsSettingsService.SETTING_TYPE;
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    PortletPreferences preferences = request.getPreferences();
    Map<String, String> parameters = new HashMap<>();
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String parameterName = parameterNames.nextElement();
      parameters.put(parameterName, request.getParameter(parameterName));
    }
    Map<String, String> settings;
    try {
      settings = getSettingsService().getSettingsToStore(preferences.getValue(NAME, null), getCurrentUsername(), parameters);
    } catch (IllegalAccessException e) {
      throw new PortletException("User is not allowed to edit settings", e);
    } catch (IllegalArgumentException e) {
      throw new PortletException("Settings refused: " + e.getMessage(), e);
    }
    for (Map.Entry<String, String> setting : settings.entrySet()) {
      preferences.setValue(setting.getKey(), setting.getValue());
    }
    preferences.store();
  }

  @Override
  protected boolean canEdit(String name, Identity userAclIdentity) {
    return getSettingsService().canEditSettings(name, userAclIdentity == null ? null : userAclIdentity.getUserId());
  }

  @Override
  protected void setViewRequestAttributes(String name, RenderRequest request, RenderResponse response) {
    String legacyApplicationId = request.getPreferences().getValue(APPLICATION_ID, null);
    if (StringUtils.isNotBlank(legacyApplicationId)) {
      getSettingsService().migrateHeaderTitle(legacyApplicationId, name);
      savePreference(APPLICATION_ID, "");
    }
  }

  private String getCurrentUsername() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity().getUserId();
  }

  private MyApplicationsSettingsService getSettingsService() {
    if (settingsService == null) {
      settingsService = ExoContainerContext.getService(MyApplicationsSettingsService.class);
    }
    return settingsService;
  }
}
