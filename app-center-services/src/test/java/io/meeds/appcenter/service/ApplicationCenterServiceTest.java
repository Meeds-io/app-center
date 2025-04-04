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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.GeneralSettings;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.storage.ApplicationCenterStorage;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterServiceTest {

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

  @MockBean
  private ConfigurationManager     configurationManager;

  @MockBean
  private SettingService           settingService;

  @MockBean
  private Authenticator            authenticator;

  @MockBean
  private IdentityRegistry         identityRegistry;

  @MockBean
  private ApplicationCenterStorage appCenterStorage;

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
    lenient().when(identityRegistry.getIdentity(ADMIN_USERNAME)).thenReturn(adminIdentity);
    lenient().when(identityRegistry.getIdentity(TEST_USER)).thenReturn(userIdentity);
  }

  @Test
  @SneakyThrows
  void createApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.createApplication(null));
    Application existingApplication = application();
    existingApplication.setTitle("title2");
    existingApplication.setUrl("test");
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.createApplication(existingApplication));
    existingApplication.setUrl("./test/");
    existingApplication.setHelpPageURL("test");
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.createApplication(existingApplication));

    Application application = application(null);
    applicationCenterService.createApplication(application);
    verify(appCenterStorage).createApplication(application);
  }

  @Test
  @SneakyThrows
  void updateApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.updateApplication(null, ADMIN_USERNAME));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateApplication(application(null), null));

    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateApplication(application(ID), null));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateApplication(application(ID), TEST_USER));
    verify(appCenterStorage, never()).updateApplication(any());
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
    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    assertThrows(IllegalAccessException.class, () -> applicationCenterService.deleteApplication(ID, TEST_USER));

    applicationCenterService.deleteApplication(ID, ADMIN_USERNAME);
    verify(appCenterStorage).deleteApplication(ID);
  }

  @Test
  void getMaxFavoriteApps() {
    long originalMaxFavoriteApps = applicationCenterService.getMaxFavoriteApps();

    applicationCenterService.setMaxFavoriteApps(originalMaxFavoriteApps + 1);
    assertEquals(originalMaxFavoriteApps + 1, applicationCenterService.getMaxFavoriteApps());

    applicationCenterService.setMaxFavoriteApps(0);
    assertEquals(0, applicationCenterService.getMaxFavoriteApps());
  }

  @Test
  void getAppGeneralSettings() {
    applicationCenterService.setMaxFavoriteApps(2);
    GeneralSettings generalSettings = applicationCenterService.getSettings();
    assertNotNull(generalSettings);
    assertEquals(2, generalSettings.getMaxFavoriteApps());
    generalSettings = applicationCenterService.getSettings();
    assertEquals(2, generalSettings.getMaxFavoriteApps());
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
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.deleteFavoriteApplication(0L, TEST_USER));
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
  void getMandatoryAndFavoriteApplicationsList() {
    UserApplication application1 = new UserApplication(application(6l));
    UserApplication application2 = new UserApplication(application(7l));
    UserApplication application3 = new UserApplication(application(8l));
    UserApplication application4 = new UserApplication(application(9l));
    UserApplication application5 = new UserApplication(application(10l));

    when(appCenterStorage.getMandatoryApplications()).thenReturn(Arrays.asList(application1, application2, application3));
    when(appCenterStorage.countFavorites(ADMIN_USERNAME)).thenReturn(1l);
    when(appCenterStorage.getFavoriteApplicationsByUser(ADMIN_USERNAME)).thenReturn(Arrays.asList(application4, application5));
    applicationCenterService.setMaxFavoriteApps(1);

    ApplicationList applicationList = applicationCenterService.getMandatoryAndFavoriteApplicationsList(ADMIN_USERNAME);
    assertFalse(applicationList.isCanAddFavorite());
    assertEquals(5, applicationList.getApplications().size());
    assertEquals(5, applicationList.getSize());

    applicationList = applicationCenterService.getMandatoryAndFavoriteApplicationsList(TEST_USER);
    assertTrue(applicationList.isCanAddFavorite());
    assertEquals(0, applicationList.getApplications().size());
    assertEquals(0, applicationList.getSize());

    application1.setPermissions(Collections.singletonList(PERMISSIONS_2));
    application2.setPermissions(Collections.singletonList(PERMISSIONS_2));
    applicationList = applicationCenterService.getMandatoryAndFavoriteApplicationsList(TEST_USER);
    assertTrue(applicationList.isCanAddFavorite());
    assertEquals(2, applicationList.getApplications().size());
    assertEquals(2, applicationList.getSize());
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
  void getActiveApplications() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getActiveApplications(0, 0, null, null));
    ApplicationList applicationsList = applicationCenterService.getActiveApplications(0, 0, null, TEST_USER);
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
    applicationsList = applicationCenterService.getActiveApplications(0, 0, keyword1, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(1, applicationsList.getSize());
  }

  @Test
  @SneakyThrows
  void getLastUpdated() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getApplicationImageLastUpdated(50000L, null));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.getApplicationImageLastUpdated(50000L, TEST_USER));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.getApplicationImageLastUpdated(application.getId(), TEST_USER));
    Long lastUpdated = applicationCenterService.getApplicationImageLastUpdated(application.getId(), ADMIN_USERNAME);
    assertNotNull(lastUpdated);
  }

  @Test
  @SneakyThrows
  void getImageStream() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getApplicationImageLastUpdated(50000L, null));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.getApplicationImageInputStream(50000L, TEST_USER));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    when(appCenterStorage.getApplicationImageInputStream(IMAGE_FILE_ID)).thenReturn(mock(InputStream.class));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.getApplicationImageInputStream(application.getId(), TEST_USER));
    InputStream stream = applicationCenterService.getApplicationImageInputStream(application.getId(), ADMIN_USERNAME);
    assertNotNull(stream);
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

    applicationCenterService.setMaxFavoriteApps(5);

    ApplicationList applicationList = applicationCenterService.getMandatoryAndFavoriteApplications(TEST_USER, pageable);

    assertNotNull(applicationList);
    assertNotNull(applicationList.getApplications());
    assertEquals(4, applicationList.getApplications().size());

    assertTrue(applicationList.isCanAddFavorite());

    assertEquals(4, applicationList.getSize());
    assertEquals(4, applicationList.getLimit());
    assertEquals(0, applicationList.getOffset());

    when(appCenterStorage.countFavorites(TEST_USER)).thenReturn(6L);
    applicationList = applicationCenterService.getMandatoryAndFavoriteApplications(TEST_USER, pageable);
    assertFalse(applicationList.isCanAddFavorite());
  }

  private Application application() {
    return application(ID);
  }

  private Application application(Long id) {
    return new Application(id,
                           TITLE,
                           URL,
                           HELP_PAGE_URL,
                           DESCRIPTION,
                           ApplicationType.LINK,
                           false,
                           true,
                           false,
                           true,
                           false,
                           Collections.singletonList(PERMISSIONS_1),
                           null,
                           IMAGE_FILE_ID,
                           "icon",
                           null,
                           0l,
                           false);
  }

}
