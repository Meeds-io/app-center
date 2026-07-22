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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.resources.ResourceBundleService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.pwa.model.PwaShortcut;

@ExtendWith(MockitoExtension.class)
public class AppCenterPwaShortcutPluginTest {

  private static final String        TEST_USER = "testuser";

  @Mock
  private PortalContainer            container;

  @Mock
  private UserPortalConfigService    portalConfigService;

  @Mock
  private ApplicationCenterService   applicationCenterService;

  @InjectMocks
  private AppCenterPwaShortcutPlugin pwaShortcutPlugin;

  @BeforeEach
  public void setup() {
    lenient().when(container.getComponentInstanceOfType(ApplicationCenterService.class)).thenReturn(applicationCenterService);
  }

  @Test
  public void getShortcuts() {
    ApplicationList applicationList = new ApplicationList();
    applicationList.setApplications(Collections.emptyList());
    when(applicationCenterService.getMandatoryAndFavoriteApplications(any(),
                                                                      eq(TEST_USER),
                                                                      eq(ResourceBundleService.DEFAULT_CROWDIN_LOCALE))).thenReturn(applicationList);
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getName()).thenReturn("meeds");
    when(portalConfigService.getDefaultSite(TEST_USER)).thenReturn(portalConfig);

    List<PwaShortcut> shortcuts = pwaShortcutPlugin.getShortcuts(TEST_USER);
    assertNotNull(shortcuts);
    assertEquals(0, shortcuts.size());

    Application application = new Application(1l,
                                              "title",
                                              "./url",
                                              true,
                                              "helpPageURL",
                                              "description",
                                              "s",
                                              ApplicationType.LINK,
                                              true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              null,
                                              null,
                                              null,
                                              "icon",
                                              "imageUrl",
                                              null,
                                              false,
                                              false);
    applicationList.setApplications(Collections.singletonList(application));
    shortcuts = pwaShortcutPlugin.getShortcuts(TEST_USER);
    assertNotNull(shortcuts);
    assertEquals(1, shortcuts.size());
    PwaShortcut pwaShortcut = shortcuts.get(0);
    assertNotNull(pwaShortcut);
    assertEquals(application.getTitle(), pwaShortcut.getName());
    assertEquals("/portal/meeds/url", pwaShortcut.getUrl());
    assertEquals(application.getDescription(), pwaShortcut.getDescription());
    assertEquals(application.getTitle(), pwaShortcut.getShortName());
    assertNotNull(pwaShortcut.getIcons());
    assertEquals(1, pwaShortcut.getIcons().size());
    assertEquals(CommonsUtils.getCurrentDomain() + application.getImageUrl() + "&sizes=192x192",
                 pwaShortcut.getIcons().get(0).getSrc());
  }

}
