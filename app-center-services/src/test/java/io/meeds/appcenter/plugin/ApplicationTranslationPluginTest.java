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

@SpringBootTest(classes = { ApplicationTranslationPlugin.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationTranslationPluginTest {

  private static final String          TEST_USER = "testuser";

  private static final String          APP_ID    = "15";

  @MockBean
  private TranslationService           translationService;

  @MockBean
  private ApplicationCenterService     applicationCenterService;

  @Autowired
  private ApplicationTranslationPlugin translationPlugin;

  @Test
  void init() {
    translationPlugin.init();
    verify(translationService).addPlugin(translationPlugin);
  }

  @Test
  void getObjectType() {
    assertEquals("appCenter", translationPlugin.getObjectType());
  }

  @Test
  void hasEditPermission() {
    assertFalse(translationPlugin.hasEditPermission(APP_ID, TEST_USER));
    when(applicationCenterService.canEdit(TEST_USER)).thenReturn(true);
    assertTrue(translationPlugin.hasEditPermission(APP_ID, TEST_USER));
  }

  @Test
  void hasAccessPermission() {
    assertTrue(translationPlugin.hasAccessPermission(APP_ID, TEST_USER));
  }

  @Test
  void getAudienceId() {
    assertEquals(0l, translationPlugin.getAudienceId(APP_ID));
  }

  @Test
  void getSpaceId() {
    assertEquals(0l, translationPlugin.getSpaceId(APP_ID));
  }

}
