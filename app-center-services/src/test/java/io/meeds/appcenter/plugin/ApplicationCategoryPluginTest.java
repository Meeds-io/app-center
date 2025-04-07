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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.container.PortalContainer;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.service.ApplicationCenterService;

@SpringBootTest(classes = { ApplicationCategoryPlugin.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCategoryPluginTest {

  private static final String       TEST_USER = "testuser";

  private static final String       APP_ID    = "15";

  @MockBean
  private ApplicationCenterService  applicationCenterService;

  @MockBean
  private PortalContainer           portalContainer;

  @Autowired
  private ApplicationCategoryPlugin categoryPlugin;

  @BeforeEach
  void setup() {
    lenient().when(portalContainer.getComponentInstanceOfType(ApplicationCenterService.class))
             .thenReturn(applicationCenterService);
  }

  @Test
  void getObjectType() {
    assertEquals("appCenter", categoryPlugin.getType());
  }

  @Test
  void canAccessWhenAdmin() {
    assertFalse(categoryPlugin.canAccess(APP_ID, TEST_USER));

    Application application = mock(Application.class);
    when(applicationCenterService.getApplication(Long.parseLong(APP_ID))).thenReturn(application);
    assertFalse(categoryPlugin.canAccess(APP_ID, TEST_USER));

    when(applicationCenterService.canEdit(TEST_USER)).thenReturn(true);
    assertTrue(categoryPlugin.canAccess(APP_ID, TEST_USER));
  }

  @Test
  void canAccessWhenActive() {
    assertFalse(categoryPlugin.canAccess(APP_ID, TEST_USER));

    Application application = mock(Application.class);
    when(applicationCenterService.getApplication(Long.parseLong(APP_ID))).thenReturn(application);
    assertFalse(categoryPlugin.canAccess(APP_ID, TEST_USER));

    when(applicationCenterService.canAccess(application, TEST_USER)).thenReturn(true);
    assertFalse(categoryPlugin.canAccess(APP_ID, TEST_USER));

    when(application.isActive()).thenReturn(true);
    assertTrue(categoryPlugin.canAccess(APP_ID, TEST_USER));
  }

  @Test
  void canEdit() {
    assertFalse(categoryPlugin.canEdit(APP_ID, TEST_USER));
    when(applicationCenterService.canEdit(TEST_USER)).thenReturn(true);
    assertTrue(categoryPlugin.canEdit(APP_ID, TEST_USER));
  }

}
