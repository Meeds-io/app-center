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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.upgrade.UpgradePluginException;
import org.exoplatform.commons.upgrade.UpgradePluginExecutionContext;
import org.exoplatform.container.configuration.ConfigurationUnmarshaller;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.container.xml.ExternalComponentPlugins;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.model.PortletInstanceDescriptor;
import io.meeds.layout.model.PortletInstanceDescriptorList;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.layout.util.JsonUtils;

@ExtendWith(MockitoExtension.class)
class MyApplicationsPortletInstancePermissionUpgradePluginTest {

  private static final Path      UPGRADE_CONFIGURATION =
                                                       Path.of("../app-center-webapps/src/main/webapp/WEB-INF/conf/app-center/upgrade-configuration.xml");

  private static final Path      PORTLET_INSTANCES     = Path.of("src/main/resources/portlet-instances.json");

  private static final String    UPGRADE_SERVICE       = "org.exoplatform.commons.upgrade.UpgradeProductService";

  private static final String    ADMINISTRATORS        = "*:/platform/administrators";

  private static final String    USERS                 = "*:/platform/users";

  @Mock
  private PortletInstanceService portletInstanceService;

  private ComponentPlugin        declaration;

  private MyApplicationsPortletInstancePermissionUpgradePlugin plugin;

  @BeforeEach
  void setUp() throws Exception {
    declaration = getDeclaration();
    plugin = new MyApplicationsPortletInstancePermissionUpgradePlugin(declaration.getInitParams());
  }

  /**
   * A fresh database imports the catalog entry, an existing one gets the
   * upgrade: both must end with the same permissions.
   */
  @Test
  void shipTheUpgradedPermissionsInTheCatalog() throws Exception {
    PortletInstanceDescriptorList catalog = JsonUtils.fromJsonString(Files.readString(PORTLET_INSTANCES),
                                                                     PortletInstanceDescriptorList.class);
    PortletInstanceDescriptor shortcuts = catalog.getDescriptors()
                                                 .stream()
                                                 .filter(d -> "AppCenterMyApplicationsPortlet".equals(d.getPortletName()))
                                                 .findFirst()
                                                 .orElseThrow();

    assertEquals(MyApplicationsPortletInstancePermissionUpgradePlugin.NEW_PERMISSIONS, shortcuts.getPermissions());
    assertEquals(List.of(USERS), shortcuts.getPermissions());
  }

  @Test
  void replaceFormerPermissionsOfEveryShortcutsInstance() throws Exception {
    PortletInstance shortcuts = shortcutsInstance(1, ADMINISTRATORS);
    PortletInstance duplicatedShortcuts = shortcutsInstance(2, ADMINISTRATORS);
    when(portletInstanceService.getPortletInstances()).thenReturn(Arrays.asList(shortcuts, duplicatedShortcuts));
    when(portletInstanceService.getPortletInstance(1)).thenReturn(shortcuts);
    when(portletInstanceService.getPortletInstance(2)).thenReturn(duplicatedShortcuts);

    assertEquals(2, plugin.replaceFormerPermissions(portletInstanceService));

    assertEquals(List.of(USERS), shortcuts.getPermissions());
    assertEquals(List.of(USERS), duplicatedShortcuts.getPermissions());
    verify(portletInstanceService).updatePortletInstance(shortcuts);
    verify(portletInstanceService).updatePortletInstance(duplicatedShortcuts);
  }

  @Test
  void keepPermissionsAnAdministratorSet() throws Exception {
    List<PortletInstance> customisedInstances = Arrays.asList(shortcutsInstance(1, ADMINISTRATORS, "*:/platform/web-contributors"),
                                                              shortcutsInstance(2, "*:/platform/web-contributors"),
                                                              shortcutsInstance(3, USERS),
                                                              shortcutsInstance(4),
                                                              shortcutsInstance(5, (String[]) null));
    when(portletInstanceService.getPortletInstances()).thenReturn(customisedInstances);

    assertEquals(0, plugin.replaceFormerPermissions(portletInstanceService));

    assertEquals(List.of(ADMINISTRATORS, "*:/platform/web-contributors"), customisedInstances.get(0).getPermissions());
    assertEquals(List.of("*:/platform/web-contributors"), customisedInstances.get(1).getPermissions());
    assertEquals(List.of(USERS), customisedInstances.get(2).getPermissions());
    assertEquals(Collections.emptyList(), customisedInstances.get(3).getPermissions());
    assertNull(customisedInstances.get(4).getPermissions());
    verify(portletInstanceService, never()).updatePortletInstance(any());
  }

  /**
   * The run starts once the server has started: an administrator may change
   * or delete an instance between the listing and its turn.
   */
  @Test
  void keepAnInstanceChangedOrDeletedSinceTheListing() throws Exception {
    PortletInstance changedInstance = shortcutsInstance(1, ADMINISTRATORS, "*:/platform/web-contributors");
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(shortcutsInstance(1, ADMINISTRATORS),
                                                                         shortcutsInstance(2, ADMINISTRATORS)));
    when(portletInstanceService.getPortletInstance(1)).thenReturn(changedInstance);
    when(portletInstanceService.getPortletInstance(2)).thenReturn(null);

    assertEquals(0, plugin.replaceFormerPermissions(portletInstanceService));

    assertEquals(List.of(ADMINISTRATORS, "*:/platform/web-contributors"), changedInstance.getPermissions());
    verify(portletInstanceService, never()).updatePortletInstance(any());
  }

  @Test
  void skipAnInstanceDeletedBeforeItsSave() throws Exception {
    PortletInstance deletedInstance = shortcutsInstance(1, ADMINISTRATORS);
    PortletInstance shortcuts = shortcutsInstance(2, ADMINISTRATORS);
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(shortcutsInstance(1, ADMINISTRATORS),
                                                                         shortcutsInstance(2, ADMINISTRATORS)));
    when(portletInstanceService.getPortletInstance(1)).thenReturn(deletedInstance);
    when(portletInstanceService.getPortletInstance(2)).thenReturn(shortcuts);
    when(portletInstanceService.updatePortletInstance(deletedInstance)).thenThrow(new ObjectNotFoundException("deleted"));

    assertEquals(1, plugin.replaceFormerPermissions(portletInstanceService));

    verify(portletInstanceService).updatePortletInstance(shortcuts);
  }

  @Test
  void keepOtherPortletInstances() throws Exception {
    PortletInstance otherInstance = shortcutsInstance(1, ADMINISTRATORS);
    otherInstance.setContentId("app-center/AppCenterAppLauncherPortlet");
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(otherInstance));

    assertEquals(0, plugin.replaceFormerPermissions(portletInstanceService));

    assertEquals(List.of(ADMINISTRATORS), otherInstance.getPermissions());
    verify(portletInstanceService, never()).updatePortletInstance(any());
  }

  @Test
  void doNothingWhenTheInstanceIsNotImportedYet() throws Exception {
    when(portletInstanceService.getPortletInstances()).thenReturn(Collections.emptyList());

    assertEquals(0, plugin.replaceFormerPermissions(portletInstanceService));

    verify(portletInstanceService, never()).updatePortletInstance(any());
  }

  @Test
  void openTheShippedInstanceOnceTheServerHasStarted() throws Exception {
    PortletInstance shortcuts = shortcutsInstance(1, ADMINISTRATORS);
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(shortcuts));
    when(portletInstanceService.getPortletInstance(1)).thenReturn(shortcuts);
    MyApplicationsPortletInstancePermissionUpgradePlugin upgradePlugin = startedPlugin();

    upgradePlugin.processUpgrade("7.2.0", "7.3.0");

    InOrder inOrder = inOrder(upgradePlugin, portletInstanceService);
    inOrder.verify(upgradePlugin).awaitServerStartup();
    inOrder.verify(upgradePlugin).portletInstanceService();
    inOrder.verify(portletInstanceService).getPortletInstances();
    inOrder.verify(portletInstanceService).updatePortletInstance(shortcuts);
    assertEquals(List.of(USERS), shortcuts.getPermissions());
  }

  /**
   * A failure must reach UpgradeProductService as an exception, so that it
   * records no execution: the upgrade then runs again at a startup on a
   * version later than the one it was upgrading from.
   */
  @Test
  void failTheRunWhenAnUpdateFails() throws Exception {
    PortletInstance shortcuts = shortcutsInstance(1, ADMINISTRATORS);
    IllegalStateException failure = new IllegalStateException("database unavailable");
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(shortcuts));
    when(portletInstanceService.getPortletInstance(1)).thenReturn(shortcuts);
    when(portletInstanceService.updatePortletInstance(shortcuts)).thenThrow(failure);
    MyApplicationsPortletInstancePermissionUpgradePlugin upgradePlugin = startedPlugin();

    UpgradePluginException exception = assertThrows(UpgradePluginException.class,
                                                    () -> upgradePlugin.processUpgrade("7.2.0", "7.3.0"));
    assertEquals(failure, exception.getCause());
    assertTrue(exception.getMessage().endsWith("a startup on a version later than 7.2.0"));
  }

  @Test
  void failTheRunWhenAnInstanceCannotBeReadAgain() throws Exception {
    IllegalStateException failure = new IllegalStateException("database unavailable");
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(shortcutsInstance(1, ADMINISTRATORS)));
    when(portletInstanceService.getPortletInstance(1)).thenThrow(failure);
    MyApplicationsPortletInstancePermissionUpgradePlugin upgradePlugin = startedPlugin();

    UpgradePluginException exception = assertThrows(UpgradePluginException.class,
                                                    () -> upgradePlugin.processUpgrade("7.2.0", "7.3.0"));
    assertEquals(failure, exception.getCause());
    verify(portletInstanceService, never()).updatePortletInstance(any());
  }

  @Test
  void failTheRunAndKeepTheInterruptionWhenTheWaitIsInterrupted() throws Exception {
    MyApplicationsPortletInstancePermissionUpgradePlugin upgradePlugin = spy(plugin);
    doThrow(new InterruptedException()).when(upgradePlugin).awaitServerStartup();

    try {
      assertThrows(UpgradePluginException.class, () -> upgradePlugin.processUpgrade("7.2.0", "7.3.0"));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
    verify(upgradePlugin, never()).portletInstanceService();
    verify(portletInstanceService, never()).getPortletInstances();
  }

  /**
   * Reads the shipped declaration: the plugin must run once, including on an
   * acceptance server redeployed with an unchanged version, which a target
   * version would prevent, and asynchronously, since it waits for the server
   * startup.
   */
  @Test
  void runOnceWhateverTheVersion() {
    assertEquals(MyApplicationsPortletInstancePermissionUpgradePlugin.class.getName(), declaration.getType());
    assertEquals("org.exoplatform.social", plugin.getProductGroupId());
    assertTrue(plugin.isExecuteOnlyOnce());
    assertNull(plugin.getTargetVersion());
    assertTrue(plugin.isAsyncUpgradeExecution());

    assertTrue(plugin.shouldProceedToUpgrade("7.3.0", "7.3.0", null));
    assertTrue(plugin.shouldProceedToUpgrade("7.3.0", "7.2.0", new UpgradePluginExecutionContext("7.2.0", 0)));
    assertFalse(plugin.shouldProceedToUpgrade("7.4.0", "7.3.0", new UpgradePluginExecutionContext("7.3.0", 1)));
  }

  private ComponentPlugin getDeclaration() throws Exception {
    Configuration configuration = new ConfigurationUnmarshaller().unmarshall(UPGRADE_CONFIGURATION.toUri().toURL());
    ExternalComponentPlugins upgradePlugins = configuration.getExternalComponentPlugins(UPGRADE_SERVICE);
    assertNotNull(upgradePlugins);
    return upgradePlugins.getComponentPlugins()
                         .stream()
                         .filter(p -> MyApplicationsPortletInstancePermissionUpgradePlugin.class.getName().equals(p.getType()))
                         .findFirst()
                         .orElseThrow();
  }

  private MyApplicationsPortletInstancePermissionUpgradePlugin startedPlugin() throws InterruptedException {
    MyApplicationsPortletInstancePermissionUpgradePlugin upgradePlugin = spy(plugin);
    doNothing().when(upgradePlugin).awaitServerStartup();
    doReturn(portletInstanceService).when(upgradePlugin).portletInstanceService();
    return upgradePlugin;
  }

  private PortletInstance shortcutsInstance(long id, String... permissions) {
    PortletInstance portletInstance = new PortletInstance();
    portletInstance.setId(id);
    portletInstance.setContentId(MyApplicationsPortletInstancePermissionUpgradePlugin.PORTLET_CONTENT_ID);
    portletInstance.setPermissions(permissions == null ? null : new ArrayList<>(Arrays.asList(permissions)));
    return portletInstance;
  }

}
