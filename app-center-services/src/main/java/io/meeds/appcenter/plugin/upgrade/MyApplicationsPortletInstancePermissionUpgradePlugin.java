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

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.upgrade.UpgradePluginException;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.monitor.jvm.ServerStartupWaiter;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;

/**
 * Opens the Shortcuts portlet instance of an existing database to all users,
 * as a fresh database gets it from portlet-instances.json, so that whoever may
 * edit a page can add the portlet to it. Only an instance still holding the
 * permissions formerly shipped is changed: a value an administrator set is
 * kept: each instance is read again at its turn, since the run starts once the
 * server has started and an administrator may change or delete it meanwhile,
 * which leaves only the time between that read and the save. Where an operator
 * sets commons.upgrade.proceedIfFirstRun to false, a
 * platform installed at this version runs it at its first version upgrade
 * instead, and opens an instance an administrator restricted to exactly the
 * platform administrators in between. The portlet instance service is a Spring
 * bean of the layout context, resolved when the plugin runs and after the
 * server has finished starting: taken in the constructor, the Kernel would
 * create it while that context is still starting. The plugin is asynchronous:
 * the wait for the server startup is refused on the startup thread, and the
 * asynchronous path of the upgrade framework binds the container itself. A
 * failure is thrown back to the upgrade framework, which records no execution:
 * the plugin runs again at a startup on a version later than the one it was
 * upgrading from.
 */
public class MyApplicationsPortletInstancePermissionUpgradePlugin extends UpgradeProductPlugin {

  public static final String       PORTLET_CONTENT_ID = "app-center/AppCenterMyApplicationsPortlet";

  public static final List<String> FORMER_PERMISSIONS = List.of("*:/platform/administrators");

  public static final List<String> NEW_PERMISSIONS    = List.of("*:/platform/users");

  private static final Log         LOG                = ExoLogger.getLogger(MyApplicationsPortletInstancePermissionUpgradePlugin.class);

  private static final String      DESCRIPTION        = "Shortcuts portlet instance permissions upgrade";

  public MyApplicationsPortletInstancePermissionUpgradePlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long start = System.currentTimeMillis();
    try {
      awaitServerStartup();
      int upgradedCount = replaceFormerPermissions(portletInstanceService());
      LOG.info("{}: {} Shortcuts portlet instance(s) opened to all users in {} ms",
               DESCRIPTION,
               upgradedCount,
               System.currentTimeMillis() - start);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new UpgradePluginException(DESCRIPTION + " was interrupted, " + retryMessage(oldVersion), e);
    } catch (Exception e) {
      throw new UpgradePluginException(DESCRIPTION + " did not complete, " + retryMessage(oldVersion), e);
    }
  }

  int replaceFormerPermissions(PortletInstanceService portletInstanceService) {
    int upgradedCount = 0;
    for (PortletInstance listedInstance : portletInstanceService.getPortletInstances()) {
      if (isShippedShortcutsInstance(listedInstance)) {
        PortletInstance portletInstance = portletInstanceService.getPortletInstance(listedInstance.getId());
        if (isShippedShortcutsInstance(portletInstance) && openToAllUsers(portletInstanceService, portletInstance)) {
          upgradedCount++;
        }
      }
    }
    return upgradedCount;
  }

  private boolean openToAllUsers(PortletInstanceService portletInstanceService, PortletInstance portletInstance) {
    portletInstance.setPermissions(new ArrayList<>(NEW_PERMISSIONS));
    try {
      portletInstanceService.updatePortletInstance(portletInstance);
      return true;
    } catch (ObjectNotFoundException e) {
      LOG.debug("{}: portlet instance {} deleted before being opened to all users", DESCRIPTION, portletInstance.getId(), e);
      return false;
    }
  }

  private boolean isShippedShortcutsInstance(PortletInstance portletInstance) {
    return portletInstance != null
           && StringUtils.equals(PORTLET_CONTENT_ID, portletInstance.getContentId())
           && FORMER_PERMISSIONS.equals(portletInstance.getPermissions());
  }

  private String retryMessage(String oldVersion) {
    return "it will be attempted again at a startup on a version later than " + oldVersion;
  }

  void awaitServerStartup() throws InterruptedException {
    ServerStartupWaiter.awaitServerStartup(PortalContainer.getInstance(), DESCRIPTION);
  }

  PortletInstanceService portletInstanceService() {
    return ExoContainerContext.getService(PortletInstanceService.class);
  }

}
