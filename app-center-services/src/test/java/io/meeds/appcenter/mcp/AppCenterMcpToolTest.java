/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.appcenter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.mcp.model.AppModel;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;

class AppCenterMcpToolTest {

  private static final String            USERNAME = "testuser1";

  private static final long              APP_ID   = 7L;

  private ApplicationCenterService       applicationCenterService;

  private AppCenterMcpTool               appCenterMcpTool;

  @BeforeEach
  void setUp() {
    applicationCenterService = mock(ApplicationCenterService.class);
    Identity currentIdentity = new Identity(USERNAME);
    appCenterMcpTool = new AppCenterMcpTool(applicationCenterService) {
      @Override
      public Identity getCurrentUserAclIdentity() {
        return currentIdentity;
      }

      @Override
      public Locale getCurrentUserLocale() {
        return Locale.ENGLISH;
      }
    };
  }

  private Application app(long id, String title, boolean personal) {
    Application application = new Application();
    application.setId(id);
    application.setTitle(title);
    application.setUrl("https://example.org/" + id);
    application.setType(ApplicationType.LINK);
    application.setPersonal(personal);
    return application;
  }

  // --- list_my_apps --------------------------------------------------------

  @Test
  void listMyApps() {
    UserApplication userApplication = new UserApplication(app(APP_ID, "Wiki", false));
    userApplication.setFavorite(true);
    ApplicationList list = new ApplicationList().setApplications(List.of(userApplication)).setSize(1);
    when(applicationCenterService.getActiveApplications(eq(0), eq(20), isNull(), any(Locale.class), eq(USERNAME)))
        .thenReturn(list);

    List<AppModel> result = appCenterMcpTool.listMyApps(null, null, null);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(APP_ID, result.get(0).getId());
    assertTrue(result.get(0).isFavorite());
    assertEquals("LINK", result.get(0).getType());
  }

  @Test
  void listMyAppsWithKeywordAndPaging() {
    ApplicationList list = new ApplicationList().setApplications(List.of()).setSize(0);
    when(applicationCenterService.getActiveApplications(eq(5), eq(3), eq("chat"), any(Locale.class), eq(USERNAME)))
        .thenReturn(list);

    List<AppModel> result = appCenterMcpTool.listMyApps("chat", 5, 3);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(applicationCenterService).getActiveApplications(eq(5), eq(3), eq("chat"), any(Locale.class), eq(USERNAME));
  }

  // --- list_my_app_drawer --------------------------------------------------

  @Test
  void listMyAppDrawer() {
    Application mandatory = app(1L, "Home", false);
    mandatory.setMandatory(true);
    UserApplication favorite = new UserApplication(app(2L, "Tasks", false));
    favorite.setFavorite(true);
    ApplicationList list = new ApplicationList().setApplications(List.of(mandatory, favorite)).setSize(2);
    when(applicationCenterService.getMandatoryAndFavoriteApplications(any(), eq(USERNAME), any(Locale.class)))
        .thenReturn(list);

    List<AppModel> result = appCenterMcpTool.listMyAppDrawer();

    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.get(0).isMandatory());
    assertFalse(result.get(0).isFavorite());
    assertTrue(result.get(1).isFavorite());
  }

  // --- add_favorite_app ----------------------------------------------------

  @Test
  void addFavoriteApp() throws Exception {
    String message = appCenterMcpTool.addFavoriteApp(APP_ID);
    assertTrue(message.contains(String.valueOf(APP_ID)));
    verify(applicationCenterService).addFavoriteApplication(APP_ID, USERNAME);
  }

  @Test
  void addFavoriteAppInvalidIdFails() {
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.addFavoriteApp(null));
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.addFavoriteApp(0L));
  }

  @Test
  void addFavoriteAppUnknownFails() throws Exception {
    doThrow(new ApplicationNotFoundException("missing")).when(applicationCenterService)
                                                        .addFavoriteApplication(APP_ID, USERNAME);
    assertThrows(ObjectNotFoundException.class, () -> appCenterMcpTool.addFavoriteApp(APP_ID));
  }

  @Test
  void addFavoriteAppDeniedFails() throws Exception {
    doThrow(new IllegalAccessException("denied")).when(applicationCenterService)
                                                 .addFavoriteApplication(APP_ID, USERNAME);
    assertThrows(IllegalStateException.class, () -> appCenterMcpTool.addFavoriteApp(APP_ID));
  }

  // --- remove_favorite_app -------------------------------------------------

  @Test
  void removeFavoriteApp() {
    String message = appCenterMcpTool.removeFavoriteApp(APP_ID);
    assertTrue(message.contains(String.valueOf(APP_ID)));
    verify(applicationCenterService).deleteFavoriteApplication(APP_ID, USERNAME);
  }

  @Test
  void removeFavoriteAppInvalidIdFails() {
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.removeFavoriteApp(null));
  }

  // --- reorder_favorite_apps -----------------------------------------------

  @Test
  void reorderFavoriteApps() throws Exception {
    String message = appCenterMcpTool.reorderFavoriteApps(List.of(3L, 1L, 2L));
    assertTrue(message.contains("3"));
    ArgumentCaptor<ApplicationOrder> captor = ArgumentCaptor.forClass(ApplicationOrder.class);
    verify(applicationCenterService, times(3)).updateFavoriteApplicationOrder(captor.capture(), eq(USERNAME));
    List<ApplicationOrder> orders = captor.getAllValues();
    assertEquals(3L, orders.get(0).getId());
    assertEquals(0L, orders.get(0).getOrder());
    assertEquals(1L, orders.get(1).getId());
    assertEquals(1L, orders.get(1).getOrder());
    assertEquals(2L, orders.get(2).getId());
    assertEquals(2L, orders.get(2).getOrder());
  }

  @Test
  void reorderFavoriteAppsEmptyFails() {
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.reorderFavoriteApps(List.of()));
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.reorderFavoriteApps(null));
  }

  @Test
  void reorderFavoriteAppsUnknownFails() throws Exception {
    doThrow(new ApplicationNotFoundException("missing")).when(applicationCenterService)
                                                        .updateFavoriteApplicationOrder(any(ApplicationOrder.class),
                                                                                        eq(USERNAME));
    assertThrows(ObjectNotFoundException.class, () -> appCenterMcpTool.reorderFavoriteApps(List.of(APP_ID)));
  }

  // --- create_personal_app -------------------------------------------------

  @Test
  void createPersonalApp() throws Exception {
    when(applicationCenterService.isUserPersonalAppsEnabled()).thenReturn(true);
    Application created = app(APP_ID, "My link", true);
    when(applicationCenterService.createPersonalApplication(any(Application.class), eq(USERNAME))).thenReturn(created);

    AppModel result = appCenterMcpTool.createPersonalApp("My link", "https://example.org/7", "fa-link");

    assertNotNull(result);
    assertEquals(APP_ID, result.getId());
    assertTrue(result.isPersonal());
    ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
    verify(applicationCenterService).createPersonalApplication(captor.capture(), eq(USERNAME));
    Application sent = captor.getValue();
    assertEquals("My link", sent.getTitle());
    assertEquals("https://example.org/7", sent.getUrl());
    assertEquals("fa-link", sent.getIcon());
    assertEquals(ApplicationType.LINK, sent.getType());
  }

  @Test
  void createPersonalAppDisabledFails() {
    when(applicationCenterService.isUserPersonalAppsEnabled()).thenReturn(false);
    assertThrows(IllegalStateException.class,
                 () -> appCenterMcpTool.createPersonalApp("My link", "https://example.org", null));
  }

  @Test
  void createPersonalAppBlankTitleFails() {
    when(applicationCenterService.isUserPersonalAppsEnabled()).thenReturn(true);
    assertThrows(IllegalArgumentException.class,
                 () -> appCenterMcpTool.createPersonalApp("  ", "https://example.org", null));
  }

  @Test
  void createPersonalAppBlankUrlFails() {
    when(applicationCenterService.isUserPersonalAppsEnabled()).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.createPersonalApp("Title", " ", null));
  }

  @Test
  void createPersonalAppDeniedFails() throws Exception {
    when(applicationCenterService.isUserPersonalAppsEnabled()).thenReturn(true);
    when(applicationCenterService.createPersonalApplication(any(Application.class), eq(USERNAME)))
        .thenThrow(new IllegalAccessException("denied"));
    assertThrows(IllegalStateException.class,
                 () -> appCenterMcpTool.createPersonalApp("Title", "https://example.org", null));
  }

  // --- update_personal_app -------------------------------------------------

  @Test
  void updatePersonalApp() throws Exception {
    Application stored = app(APP_ID, "Old", true);
    when(applicationCenterService.getApplication(APP_ID)).thenReturn(stored);

    AppModel result = appCenterMcpTool.updatePersonalApp(APP_ID, "New title", null, "fa-star");

    assertNotNull(result);
    ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
    verify(applicationCenterService).updatePersonalApplication(captor.capture(), eq(USERNAME));
    Application sent = captor.getValue();
    assertEquals("New title", sent.getTitle());
    assertEquals("https://example.org/7", sent.getUrl()); // unchanged
    assertEquals("fa-star", sent.getIcon());
  }

  @Test
  void updatePersonalAppNoFieldsFails() {
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.updatePersonalApp(APP_ID, null, null, null));
  }

  @Test
  void updatePersonalAppUnknownFails() {
    when(applicationCenterService.getApplication(APP_ID)).thenReturn(null);
    assertThrows(ObjectNotFoundException.class, () -> appCenterMcpTool.updatePersonalApp(APP_ID, "New", null, null));
  }

  @Test
  void updatePersonalAppNotOwnerFails() throws Exception {
    Application stored = app(APP_ID, "Old", true);
    when(applicationCenterService.getApplication(APP_ID)).thenReturn(stored);
    doThrow(new IllegalAccessException("not owner")).when(applicationCenterService)
                                                    .updatePersonalApplication(any(Application.class), eq(USERNAME));
    assertThrows(IllegalStateException.class, () -> appCenterMcpTool.updatePersonalApp(APP_ID, "New", null, null));
  }

  // --- delete_personal_app -------------------------------------------------

  @Test
  void deletePersonalApp() throws Exception {
    String message = appCenterMcpTool.deletePersonalApp(APP_ID);
    assertTrue(message.contains(String.valueOf(APP_ID)));
    verify(applicationCenterService).deletePersonalApplication(APP_ID, USERNAME);
  }

  @Test
  void deletePersonalAppInvalidIdFails() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> appCenterMcpTool.deletePersonalApp(0L));
    verify(applicationCenterService, never()).deletePersonalApplication(anyLong(), anyString());
  }

  @Test
  void deletePersonalAppUnknownFails() throws Exception {
    doThrow(new ApplicationNotFoundException("missing")).when(applicationCenterService)
                                                        .deletePersonalApplication(APP_ID, USERNAME);
    assertThrows(ObjectNotFoundException.class, () -> appCenterMcpTool.deletePersonalApp(APP_ID));
  }

  @Test
  void deletePersonalAppDeniedFails() throws Exception {
    doThrow(new IllegalAccessException("not owner")).when(applicationCenterService)
                                                    .deletePersonalApplication(APP_ID, USERNAME);
    assertThrows(IllegalStateException.class, () -> appCenterMcpTool.deletePersonalApp(APP_ID));
  }

}
