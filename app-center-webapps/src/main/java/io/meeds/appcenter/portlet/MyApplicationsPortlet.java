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
import java.security.SecureRandom;
import java.util.Enumeration;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.social.portlet.CMSPortlet;

public class MyApplicationsPortlet extends CMSPortlet {

  private static final String OBJECT_TYPE    = "myApplicationsPortlet";

  private static final String APPLICATION_ID = "applicationId";

  private static final String IS_ADMIN       = "isAdmin";

  private UserACL             userAcl;

  private final SecureRandom  random         = new SecureRandom();

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.contentType = OBJECT_TYPE;
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    if (!canEditSettings()) {
      throw new PortletException("User is not allowed to edit settings");
    }
    PortletPreferences preferences = request.getPreferences();
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String name = parameterNames.nextElement();
      if (StringUtils.equals(name, "action") || StringUtils.contains(name, "portal:")) {
        continue;
      }
      String value = request.getParameter(name);
      preferences.setValue(name, value);
    }
    preferences.store();
  }

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
    request.setAttribute(APPLICATION_ID, getOrCreateApplicationId(request.getPreferences()));
    request.setAttribute(IS_ADMIN, getUserAcl().isAdministrator(getCurrentIdentity()));
    super.doView(request, response);
  }

  private boolean canEditSettings() {
    return getUserAcl().isAdministrator(getCurrentIdentity());
  }

  private Identity getCurrentIdentity() {
    return ConversationState.getCurrent() == null ? null : ConversationState.getCurrent().getIdentity();
  }

  private UserACL getUserAcl() {
    if (userAcl == null) {
      userAcl = ExoContainerContext.getService(UserACL.class);
    }
    return userAcl;
  }

  private String getOrCreateApplicationId(PortletPreferences preferences) {
    String applicationId = preferences.getValue(APPLICATION_ID, null);
    if (applicationId == null) {
      applicationId = String.valueOf(random.nextLong() & Long.MAX_VALUE);
      savePreference(APPLICATION_ID, applicationId);
    }
    return applicationId;
  }
}
