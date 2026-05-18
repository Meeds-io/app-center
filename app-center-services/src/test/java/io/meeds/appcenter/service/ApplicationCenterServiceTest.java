/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.thumbnail.ImageThumbnailService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterServiceTest {

  private static final String      SHORTCUT       = "G";

  private static final String      KEYWORD        = "keyword";

  private static final String      ADMIN_USERNAME = "admin";

  private static final long        IMAGE_FILE_ID  = 5l;

  private static final String      HELP_PAGE_URL  = "./helpPageUrl";

  private static final String      URL            = "./url";

  private static final String      PERMISSIONS_2  = "/permissions2";

  private static final String      PERMISSIONS_1  = "/permissions1";

  private static final String      DESCRIPTION    = "description";

  private static final String      TITLE          = "title";

  private static final String      TEST_USER      = "testuser";

  private static final Long        ID             = 2l;

  @MockitoBean
  private ConfigurationManager     configurationManager;

  @MockitoBean
  private SettingService           settingService;

  @MockitoBean
  private TranslationService       translationService;

  @MockitoBean
  private FileService              fileService;

  @MockitoBean
  private ImageThumbnailService    imageThumbnailService;

  @MockitoBean
  private UserACL                  userAcl;

  @MockitoBean
  private ApplicationCenterStorage appCenterStorage;

  @MockitoBean
  private CategoryLinkService      categoryLinkService;

  @MockitoBean
  private PortalContainer          portalContainer;

  @Autowired
  private ApplicationCenterService applicationCenterService;

  @BeforeEach
  @SneakyThrows
  void setup() {
    Identity userIdentity = mock(Identity.class);
    Identity adminIdentity = mock(Identity.class);
    lenient().when(adminIdentity.isMemberOf(argThat((ArgumentMatcher<MembershipEntry>) m -> m.getGroup()
                                                                                             .equals("/platform/administrators")
                                                                                            || m.getGroup()
                                                                                                .equals(PERMISSIONS_1))))
             .thenReturn(true);
    lenient().when(userIdentity.isMemberOf(argThat((ArgumentMatcher<MembershipEntry>) m -> m.getGroup()
                                                                                            .equals(PERMISSIONS_2))))
             .thenReturn(true);
    lenient().when(userAcl.getUserIdentity(ADMIN_USERNAME)).thenReturn(adminIdentity);
    lenient().when(userAcl.getUserIdentity(TEST_USER)).thenReturn(userIdentity);
    lenient().when(userAcl.isAdministrator(adminIdentity)).thenReturn(true);
    lenient().when(userAcl.hasPermission(adminIdentity, PERMISSIONS_1)).thenReturn(true);
    lenient().when(userAcl.hasPermission(adminIdentity, PERMISSIONS_2)).thenReturn(true);
    lenient().when(userAcl.hasPermission(userIdentity, ADMIN_USERNAME)).thenReturn(true);
    lenient().when(userAcl.hasPermission(userIdentity, PERMISSIONS_2)).thenReturn(true);
    lenient().when(userAcl.hasPermission(userIdentity, TEST_USER)).thenReturn(true);
    lenient().when(portalContainer.getComponentInstanceOfType(CategoryLinkService.class)).thenReturn(categoryLinkService);
  }

  @Test
  @SneakyThrows
  void createApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.createApplication(null));
    Application application = application(null);
    assertThrows(IllegalAccessException.class, () -> applicationCenterService.createApplication(application, TEST_USER));
    Identity identity = mock(Identity.class);
    when(userAcl.getUserIdentity(TEST_USER)).thenReturn(identity);
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    applicationCenterService.createApplication(application, TEST_USER);
    verify(appCenterStorage).createApplication(application);
  }

  @Test
  void getApplication() {
    applicationCenterService.getApplication(5l);
    verify(appCenterStorage).getApplication(5l);
  }

  @Test
  void findSystemApplicationByUrl() {
    applicationCenterService.findSystemApplicationByUrl("url");
    verify(appCenterStorage).findSystemApplicationByUrl("url");
  }

  @Test
  @SneakyThrows
  void updateApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.updateApplication(null, ADMIN_USERNAME));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateApplication(application(null), null));
    verify(appCenterStorage, never()).updateApplication(any());

    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.updateApplication(application(ID), TEST_USER));
    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateApplication(application(ID), null));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateApplication(application(ID), TEST_USER));
    applicationCenterService.updateApplication(application(ID), ADMIN_USERNAME);
    verify(appCenterStorage).updateApplication(any());
  }

  @Test
  @SneakyThrows
  void deleteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteApplication(null, null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteApplication(null, ADMIN_USERNAME));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteApplication(ID, null));
    assertThrows(ApplicationNotFoundException.class, () -> applicationCenterService.deleteApplication(ID, ADMIN_USERNAME));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    assertThrows(IllegalAccessException.class, () -> applicationCenterService.deleteApplication(ID, TEST_USER));

    applicationCenterService.deleteApplication(ID, ADMIN_USERNAME);
    verify(appCenterStorage).deleteApplication(ID);

    application.setSystem(true);
    assertThrows(IllegalAccessException.class, () -> applicationCenterService.deleteApplication(ID, ADMIN_USERNAME));
  }

  @Test
  @SneakyThrows
  void addFavoriteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.addFavoriteApplication(0, null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.addFavoriteApplication(0, TEST_USER));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.addFavoriteApplication(ID, TEST_USER));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.addFavoriteApplication(ID, TEST_USER));
    applicationCenterService.addFavoriteApplication(ID, ADMIN_USERNAME);
    verify(appCenterStorage).addApplicationToUserFavorite(ID, ADMIN_USERNAME);

    application.setPermissions(Collections.singletonList(TEST_USER));
    applicationCenterService.addFavoriteApplication(ID, TEST_USER);
    verify(appCenterStorage).addApplicationToUserFavorite(ID, TEST_USER);
  }

  @Test
  @SneakyThrows
  void deleteFavoriteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteFavoriteApplication(null, null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteFavoriteApplication(5l, null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteFavoriteApplication(null, TEST_USER));
    applicationCenterService.deleteFavoriteApplication(ID, TEST_USER);
    verify(appCenterStorage).deleteApplicationFavorite(ID, TEST_USER);
  }

  @Test
  @SneakyThrows
  void getApplicationsList() {
    ApplicationList applicationsList = applicationCenterService.getApplications(0, 0, KEYWORD);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());
    assertEquals(0, applicationsList.getOffset());
    assertEquals(0, applicationsList.getLimit());

    Application application = application();
    when(appCenterStorage.getApplications(KEYWORD)).thenReturn(Collections.singletonList(application));
    applicationsList = applicationCenterService.getApplications(1, 2, KEYWORD);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(1, applicationsList.getSize());
    assertEquals(1, applicationsList.getOffset());
    assertEquals(2, applicationsList.getLimit());

    applicationsList = applicationCenterService.getApplications(0, 2, KEYWORD);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(1, applicationsList.getSize());
    assertEquals(0, applicationsList.getOffset());
    assertEquals(2, applicationsList.getLimit());
  }

  @Test
  @SneakyThrows
  void getPaginatedApplicationsList() {
    Application application1 = application(11L);
    Application application2 = application(12L);
    Application application3 = application(13L);
    Application application4 = application(14L);
    Application application5 = application(15L);

    when(appCenterStorage.getApplications(null)).thenReturn(Arrays.asList(application1,
                                                                          application2,
                                                                          application3,
                                                                          application4,
                                                                          application5));

    ApplicationList applicationsList = applicationCenterService.getApplications(0, 2, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(5, applicationsList.getSize());
    assertEquals(0, applicationsList.getOffset());
    assertEquals(2, applicationsList.getLimit());

    applicationsList = applicationCenterService.getApplications(2, 2, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(5, applicationsList.getSize());
    assertEquals(2, applicationsList.getOffset());
    assertEquals(2, applicationsList.getLimit());

    applicationsList = applicationCenterService.getApplications(4, 2, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(5, applicationsList.getSize());
    assertEquals(4, applicationsList.getOffset());
    assertEquals(2, applicationsList.getLimit());

  }

  @Test
  @SneakyThrows
  void updateFavoriteApplicationOrder() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ""));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(0L, 1L), ADMIN_USERNAME));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ADMIN_USERNAME));

    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ADMIN_USERNAME);
    verify(appCenterStorage).updateFavoriteApplicationOrder(ID, ADMIN_USERNAME, 1L);
  }

  @Test
  @SneakyThrows
  void updateFavoriteApplicationOrderWhenMandatory() {
    Application application = application();

    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ADMIN_USERNAME);
    verify(appCenterStorage).updateFavoriteApplicationOrder(ID, ADMIN_USERNAME, 1L);
    verify(appCenterStorage, never()).addApplicationToUserFavorite(ID, ADMIN_USERNAME);

    application.setMandatory(false);
    applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ADMIN_USERNAME);
    verify(appCenterStorage).addApplicationToUserFavorite(ID, ADMIN_USERNAME);
  }

  @Test
  @SneakyThrows
  void getActiveApplications() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getActiveApplications(0, 0, null, null));
    ApplicationList applicationsList = applicationCenterService.getActiveApplications(0, 0, null, Locale.ENGLISH, TEST_USER);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());
    assertEquals(0, applicationsList.getOffset());
    assertEquals(0, applicationsList.getLimit());

    applicationsList = applicationCenterService.getActiveApplications(2, 3, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());
    assertEquals(2, applicationsList.getOffset());
    assertEquals(3, applicationsList.getLimit());

    String keyword1 = "keyword1";
    Application application = application();
    application.setActive(false);
    when(appCenterStorage.getApplications(keyword1)).thenReturn(Collections.singletonList(application));

    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());

    application.setActive(true);
    String englishTitle = "titleEN";
    String englishDesc = "descEN";
    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, Locale.ENGLISH, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(1, applicationsList.getSize());
    assertEquals(TITLE, applicationsList.getApplications().get(0).getTitle());
    assertEquals(DESCRIPTION, applicationsList.getApplications().get(0).getDescription());

    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         application.getId(),
                                                         TITLE,
                                                         Locale.ENGLISH)).thenReturn(englishTitle);
    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         application.getId(),
                                                         DESCRIPTION,
                                                         Locale.ENGLISH)).thenReturn(englishDesc);
    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, Locale.ENGLISH, ADMIN_USERNAME);
    assertEquals(englishTitle, applicationsList.getApplications().get(0).getTitle());
    assertEquals(englishDesc, applicationsList.getApplications().get(0).getDescription());

    application.setPermissions(null);
    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());

    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, "anotherUser");
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getSize());

    when(userAcl.hasPermission(userAcl.getUserIdentity(ADMIN_USERNAME),
                               ApplicationCenterService.DEFAULT_USERS_PERMISSION)).thenReturn(true);
    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(1, applicationsList.getSize());
  }

  @Test
  @SneakyThrows
  void getLastUpdated() {
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.getApplicationImageLastUpdated(50000L));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    Long lastUpdated = applicationCenterService.getApplicationImageLastUpdated(application.getId());
    assertNotNull(lastUpdated);

    application.setImageFileId(null);
    assertNull(applicationCenterService.getApplicationImageLastUpdated(application.getId()));
  }

  @Test
  @SneakyThrows
  void getImageStream() {
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.getApplicationImageInputStream(50000L));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    when(appCenterStorage.getApplicationImageInputStream(IMAGE_FILE_ID)).thenReturn(mock(InputStream.class));
    InputStream stream = applicationCenterService.getApplicationImageInputStream(application.getId());
    assertNotNull(stream);

    application.setImageFileId(null);
    assertNull(applicationCenterService.getApplicationImageInputStream(application.getId()));
  }

  @Test
  void getSystemApplications() {
    applicationCenterService.getSystemApplications();
    verify(appCenterStorage).getSystemApplications();
  }

  @Test
  @SneakyThrows
  void getApplicationShortcuts() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getActiveApplications(0, 0, null, null));

    Application application = application();
    when(appCenterStorage.getApplications(null)).thenReturn(Collections.singletonList(application));

    List<String> shortcuts = applicationCenterService.getApplicationShortcuts(ADMIN_USERNAME);
    assertNotNull(shortcuts);
    assertEquals(1, shortcuts.size());
    assertEquals(SHORTCUT, shortcuts.get(0));
  }

  @Test
  @SneakyThrows
  void testGetMandatoryAndFavoriteApplications() {
    Pageable pageable = PageRequest.of(0, 5);

    UserApplication application1 = new UserApplication(application(1L));
    UserApplication application2 = new UserApplication(application(2L));
    UserApplication application3 = new UserApplication(application(3L));
    UserApplication application4 = new UserApplication(application(4L));
    UserApplication application5 = new UserApplication(application(5L));

    application1.setPermissions(Collections.singletonList(PERMISSIONS_2));
    application2.setPermissions(Collections.singletonList(PERMISSIONS_2));
    application3.setPermissions(Collections.singletonList(PERMISSIONS_2));
    application4.setPermissions(Collections.singletonList(PERMISSIONS_2));

    when(appCenterStorage.getMandatoryAndFavoriteApplications(TEST_USER, pageable))
                                                                                   .thenReturn(Arrays.asList(application1,
                                                                                                             application2,
                                                                                                             application3,
                                                                                                             application4,
                                                                                                             application5));
    when(appCenterStorage.countFavorites(TEST_USER)).thenReturn(3L);

    ApplicationList applicationList = applicationCenterService.getMandatoryAndFavoriteApplications(pageable, TEST_USER, null);

    assertNotNull(applicationList);
    assertNotNull(applicationList.getApplications());
    assertEquals(4, applicationList.getApplications().size());

    assertEquals(4, applicationList.getSize());
    assertEquals(4, applicationList.getLimit());
    assertEquals(0, applicationList.getOffset());
  }

  private Application application() {
    return application(ID);
  }

  private Application application(Long id) {
    return new Application(id,
                           TITLE,
                           URL,
                           true,
                           HELP_PAGE_URL,
                           DESCRIPTION,
                           SHORTCUT,
                           ApplicationType.LINK,
                           true,
                           true,
                           false,
                           true,
                           false,
                           true,
                           Collections.singletonList(PERMISSIONS_1),
                           null,
                           IMAGE_FILE_ID,
                           "icon",
                           null,
                           null,
                           false);
  }

}
