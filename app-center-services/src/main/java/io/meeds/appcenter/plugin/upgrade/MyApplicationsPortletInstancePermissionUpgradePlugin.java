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
package io.meeds.appcenter.plugin.upgrade;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.common.ContainerTransactional;
import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;

/**
 * Opens the Shortcuts portlet instance of an existing database to all users,
 * as a fresh database gets it from portlet-instances.json, so that whoever may
 * edit a page can add the portlet to it. Only an instance still holding the
 * permissions formerly shipped is changed: a value an administrator set is
 * kept.
 */
public class MyApplicationsPortletInstancePermissionUpgradePlugin extends UpgradeProductPlugin {

  public static final String        PORTLET_CONTENT_ID = "app-center/AppCenterMyApplicationsPortlet";

  public static final List<String>  FORMER_PERMISSIONS = List.of("*:/platform/administrators");

  public static final List<String>  NEW_PERMISSIONS    = List.of("*:/platform/users");

  private static final Log          LOG                = ExoLogger.getLogger(MyApplicationsPortletInstancePermissionUpgradePlugin.class);

  private PortletInstanceService    portletInstanceService;

  public MyApplicationsPortletInstancePermissionUpgradePlugin(SettingService settingService,
                                                              PortletInstanceService portletInstanceService,
                                                              InitParams initParams) {
    super(settingService, initParams);
    this.portletInstanceService = portletInstanceService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long start = System.currentTimeMillis();
    int upgradedCount = upgradePortletInstances();
    LOG.info("Upgrade {}: {} Shortcuts portlet instance(s) opened to all users in {} ms",
             getName(),
             upgradedCount,
             System.currentTimeMillis() - start);
  }

  @ContainerTransactional
  public int upgradePortletInstances() {
    try {
      return replaceFormerPermissions();
    } catch (ObjectNotFoundException e) {
      throw new IllegalStateException("A Shortcuts portlet instance was deleted while being upgraded", e);
    }
  }

  int replaceFormerPermissions() throws ObjectNotFoundException {
    int upgradedCount = 0;
    for (PortletInstance portletInstance : portletInstanceService.getPortletInstances()) {
      if (StringUtils.equals(PORTLET_CONTENT_ID, portletInstance.getContentId())
          && FORMER_PERMISSIONS.equals(portletInstance.getPermissions())) {
        portletInstance.setPermissions(new ArrayList<>(NEW_PERMISSIONS));
        portletInstanceService.updatePortletInstance(portletInstance);
        upgradedCount++;
      }
    }
    return upgradedCount;
  }

}
