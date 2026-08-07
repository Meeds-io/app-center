/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.storage.ApplicationBadgeStorage;
import io.meeds.appcenter.storage.ApplicationCenterStorage;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationBadgeService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationBadgeServiceTest {

  private static final String            BADGE_NAME  = "emailUnread";

  private static final String            DRAWER_NAME = "emailBox";

  private static final String            USERNAME    = "testuser";

  @MockBean
  private ApplicationBadgePluginRegistry pluginRegistry;

  @MockBean
  private ApplicationBadgeStorage        badgeStorage;

  @MockBean
  private ApplicationCenterStorage       appCenterStorage;

  @MockBean
  private ApplicationCenterService       appCenterService;

  @MockBean
  private ListenerService                listenerService;

  @Autowired
  private ApplicationBadgeService        badgeService;

  private ApplicationBadgePlugin         plugin;

  @BeforeEach
  void setup() {
    plugin = mock(ApplicationBadgePlugin.class);
    lenient().when(plugin.getName()).thenReturn(BADGE_NAME);
  }

  @Test
  void getBadgeThrowsWhenNoPluginRegistered() {
    when(pluginRegistry.getPlugin("unknown")).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> badgeService.getBadge("unknown", USERNAME));
  }

  @Test
  @SneakyThrows
  void getBadgeThrowsWhenBoundToNoAccessibleApplication() {
    Application application = application();
    registerPlugin();
    when(appCenterStorage.getApplicationsByBadge(eq(BADGE_NAME), anyList())).thenReturn(List.of(application));
    when(appCenterService.canAccess(application, USERNAME)).thenReturn(false);

    assertThrows(IllegalAccessException.class, () -> badgeService.getBadge(BADGE_NAME, USERNAME));
    // The count must never be computed for a user who may not see the app
    verify(badgeStorage, never()).getBadge(anyString(), anyString());
    verify(plugin, never()).countBadge(anyString());
  }

  @Test
  @SneakyThrows
  void getBadgeThrowsWhenBoundToNoApplicationAtAll() {
    registerPlugin();
    when(appCenterStorage.getApplicationsByBadge(eq(BADGE_NAME), anyList())).thenReturn(Collections.emptyList());

    assertThrows(IllegalAccessException.class, () -> badgeService.getBadge(BADGE_NAME, USERNAME));
  }

  @Test
  @SneakyThrows
  void getBadgeReadsThroughTheCacheByDefault() {
    grantAccess();
    when(plugin.isEnabled(USERNAME)).thenReturn(true);
    when(plugin.isSelfCached()).thenReturn(false);
    when(badgeStorage.getBadge(BADGE_NAME, USERNAME)).thenReturn(3L);

    assertEquals(3L, badgeService.getBadge(BADGE_NAME, USERNAME));
    verify(plugin, never()).countBadge(anyString());
  }

  @Test
  @SneakyThrows
  void getBadgeBypassesTheCacheForASelfCachedPlugin() {
    grantAccess();
    when(plugin.isEnabled(USERNAME)).thenReturn(true);
    when(plugin.isSelfCached()).thenReturn(true);
    when(plugin.countBadge(USERNAME)).thenReturn(7L);

    assertEquals(7L, badgeService.getBadge(BADGE_NAME, USERNAME));
    verify(badgeStorage, never()).getBadge(anyString(), anyString());
  }

  @Test
  @SneakyThrows
  void getBadgeReturnsZeroForAnOptedOutUser() {
    grantAccess();
    when(plugin.isEnabled(USERNAME)).thenReturn(false);

    assertEquals(0L, badgeService.getBadge(BADGE_NAME, USERNAME));
    verify(badgeStorage, never()).getBadge(anyString(), anyString());
    verify(plugin, never()).countBadge(anyString());
  }

  @Test
  @SneakyThrows
  void updateBadgeEvictsBeforeBroadcasting() {
    badgeService.updateBadge(BADGE_NAME, USERNAME);

    // Reversing this order makes the browser re-fetch the very value it was
    // just told to refresh
    InOrder inOrder = inOrder(badgeStorage, listenerService);
    inOrder.verify(badgeStorage).evict(BADGE_NAME, USERNAME);
    inOrder.verify(listenerService).broadcast(ApplicationBadgeService.BADGE_UPDATED_EVENT, BADGE_NAME, USERNAME);
  }

  @Test
  @SneakyThrows
  void updateBadgeIgnoresBlankArguments() {
    badgeService.updateBadge(null, USERNAME);
    badgeService.updateBadge(BADGE_NAME, null);

    verify(badgeStorage, never()).evict(any(), any());
    verify(listenerService, never()).broadcast(anyString(), any(), any());
  }

  @Test
  void getBadgeNameDelegatesResolutionToTheRegistry() {
    // Resolution lives in the registry so that ApplicationCenterService can
    // decorate applications without a bean cycle between the two services
    Application application = application();
    when(pluginRegistry.resolveBadgeName(application)).thenReturn(BADGE_NAME);

    assertEquals(BADGE_NAME, badgeService.getBadgeName(application));
  }

  private void registerPlugin() {
    when(pluginRegistry.getPlugin(BADGE_NAME)).thenReturn(plugin);
  }

  private void grantAccess() {
    Application application = application();
    registerPlugin();
    when(appCenterStorage.getApplicationsByBadge(eq(BADGE_NAME), anyList())).thenReturn(List.of(application));
    when(appCenterService.canAccess(application, USERNAME)).thenReturn(true);
  }

  private Application application() {
    Application application = new Application();
    application.setId(1L);
    application.setTitle("Mail");
    application.setUrl(DRAWER_NAME);
    application.setType(ApplicationType.DRAWER);
    return application;
  }

}
