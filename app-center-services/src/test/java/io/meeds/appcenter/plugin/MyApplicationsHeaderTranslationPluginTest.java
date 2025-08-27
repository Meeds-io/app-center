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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.social.translation.service.TranslationService;

@SpringBootTest(classes = { MyApplicationsHeaderTranslationPlugin.class })
@ExtendWith(MockitoExtension.class)
public class MyApplicationsHeaderTranslationPluginTest {

  private static final String                   TEST_USER = "testuser";

  @MockBean
  private TranslationService                    translationService;

  @MockBean
  private ApplicationCenterService              applicationCenterService;

  @Autowired
  private MyApplicationsHeaderTranslationPlugin translationPlugin;

  @Test
  void init() {
    translationPlugin.init();
    verify(translationService).addPlugin(translationPlugin);
  }

  @Test
  void getObjectType() {
    assertEquals("myApplicationsPortlet", translationPlugin.getObjectType());
  }

  @Test
  void hasEditPermission() {
    assertFalse(translationPlugin.hasEditPermission("15", TEST_USER));
    when(applicationCenterService.canEdit(TEST_USER)).thenReturn(true);
    assertTrue(translationPlugin.hasEditPermission("15", TEST_USER));
  }

  @Test
  void hasAccessPermission() {
    assertTrue(translationPlugin.hasAccessPermission("15", TEST_USER));
  }

  @Test
  void getAudienceId() {
    assertEquals(0l, translationPlugin.getAudienceId("15"));
  }

  @Test
  void getSpaceId() {
    assertEquals(0l, translationPlugin.getSpaceId("15"));
  }

}
