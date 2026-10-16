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
package io.meeds.appcenter.plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;
import org.exoplatform.services.security.ConversationState;

import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.layout.service.PortletInstanceService;

import jakarta.annotation.PostConstruct;

/**
 * Computes the preferences of a copy of a Shortcuts portlet window, which
 * layout asks for when it duplicates a section, saves a page or a section as
 * a template, creates a site or a space from a template, or exports one: see
 * {@link MyApplicationsCopyService#getCopyPreferences(Map, boolean, String)}.
 */
@Component
@Profile("layout")
public class MyApplicationsPortletInstancePreferencePlugin implements PortletInstancePreferencePlugin {

  public static final String     PORTLET_NAME   = "AppCenterMyApplicationsPortlet";

  @Autowired
  private MyApplicationsCopyService copyService;

  @Autowired(required = false)
  private PortletInstanceService portletInstanceService;

  @PostConstruct
  public void init() {
    if (portletInstanceService == null) {
      portletInstanceService = ExoContainerContext.getService(PortletInstanceService.class);
    }
    portletInstanceService.addPortletInstancePreferencePlugin(this);
  }

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public List<PortletInstancePreference> generatePreferences(Application application,
                                                             Portlet preferences,
                                                             PortletInstanceContext portletInstanceContext) {
    Map<String, String> values = null;
    if (preferences != null) {
      values = new LinkedHashMap<>();
      for (Preference preference : preferences) {
        values.put(preference.getName(), preference.getValue());
      }
    }
    boolean draft = portletInstanceContext != null
                    && portletInstanceContext.getProperties() != null
                    && "true".equals(portletInstanceContext.getProperties().get(PortletInstanceContext.DRAFT_PROPERTY));
    return copyService.getCopyPreferences(values, draft, getCurrentUsername())
                      .entrySet()
                      .stream()
                      .map(preference -> new PortletInstancePreference(preference.getKey(), preference.getValue()))
                      .toList();
  }

  /**
   * @return the user a copy is made for, null when no user makes it, as at
   *         startup or in a job
   */
  private String getCurrentUsername() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null || conversationState.getIdentity() == null ? null
                                                                                : conversationState.getIdentity().getUserId();
  }

}
