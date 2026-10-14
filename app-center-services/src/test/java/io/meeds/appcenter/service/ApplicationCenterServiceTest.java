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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.constant.PlacementSide;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCenterSettings;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationListFilter;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.ApplicationPlacements;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.ApplicationCategoryStorage;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.appcenter.storage.ApplicationPlacementStorage;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.portal.navigation.service.NavigationConfigurationService;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterServiceTest {

  private static final String CONTEXTUAL_PREFIX = ApplicationCenterService.CONTEXTUAL_MESSAGE_PREFIX;

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

  private static final String      SETTING_NAME   = "Untitled-12";

  private static final String      OTHER_SETTING_NAME = "Untitled-13";

  private static final String      PAGE_REFERENCE = "group::/spaces/team::home";

  private static final long        SPACE_ID       = 5L;

  private static final String      SPACE_GROUP    = "/spaces/team";

  private static final long        CREATED_ID     = 30L;

  private static final String      UPLOAD_ID      = "uploadId";

  @MockitoBean
  private ApplicationBadgePluginRegistry badgePluginRegistry;

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
  private ApplicationPlacementStorage placementStorage;

  @MockitoBean
  private NavigationConfigurationService navigationConfigurationService;

  @MockitoBean
  private UserPortalConfigService  userPortalConfigService;

  @MockitoBean
  private CategoryLinkService      categoryLinkService;

  @MockitoBean
  private PortalContainer          portalContainer;

  @MockitoBean
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @MockitoBean
  private ApplicationCategoryStorage applicationCategoryStorage;

  @MockitoBean
  private ResourceBundleService    resourceBundleService;

  @MockitoBean
  private SpaceService             spaceService;

  @MockitoBean
  private PortletInstanceService   portletInstanceService;

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
    lenient().when(myApplicationsSettingsService.hasSetting(SETTING_NAME)).thenReturn(true);
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
  void getApplicationsKeepsTheStoredBadgeBindingForTheAdministrationForm() {
    Application application = application();
    application.setBadgeName("none");
    when(appCenterStorage.getApplications(KEYWORD)).thenReturn(Collections.singletonList(application));
    when(badgePluginRegistry.resolveBadgeName(application)).thenReturn(null);

    Application returned = applicationCenterService.getApplications(0, 1, KEYWORD).getApplications().get(0);
    assertNull(returned.getBadgeName());
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
  void updateFavoriteApplicationsOrder() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(ID, 1L)), ""));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(0L, 1L)),
                                                                                ADMIN_USERNAME));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(null, ADMIN_USERNAME));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(ID, 1L)),
                                                                                ADMIN_USERNAME));

    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(ID, 1L)), ADMIN_USERNAME);
    verify(appCenterStorage).updateFavoriteApplicationOrder(ID, ADMIN_USERNAME, 1L);
  }

  @Test
  @SneakyThrows
  @SuppressWarnings("removal")
  void updateFavoriteApplicationOrderOrdersOneApplicationAsTheBatchDoes() {
    when(appCenterStorage.getApplication(ID)).thenReturn(application());
    applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 1L), ADMIN_USERNAME);
    verify(appCenterStorage).updateFavoriteApplicationOrder(ID, ADMIN_USERNAME, 1L);

    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(ID, 2L), TEST_USER));
    verify(appCenterStorage, never()).updateFavoriteApplicationOrder(ID, TEST_USER, 2L);
  }

  @Test
  @SneakyThrows
  void updateFavoriteApplicationsOrderWhenMandatory() {
    Application application = application();

    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(ID, 1L)), ADMIN_USERNAME);
    verify(appCenterStorage).updateFavoriteApplicationOrder(ID, ADMIN_USERNAME, 1L);
    verify(appCenterStorage, never()).addApplicationToUserFavorite(ID, ADMIN_USERNAME);

    application.setMandatory(false);
    applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(ID, 1L)), ADMIN_USERNAME);
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

  // --- Personal URL apps ---------------------------------------------------

  @Test
  @SneakyThrows
  void isUserPersonalAppsEnabledAndSettings() {
    assertFalse(applicationCenterService.isUserPersonalAppsEnabled());
    assertNotNull(applicationCenterService.getSettings());
    assertFalse(applicationCenterService.getSettings().isAllowUserPersonalApps());

    enablePersonalApps();
    assertTrue(applicationCenterService.isUserPersonalAppsEnabled());
    assertTrue(applicationCenterService.getSettings().isAllowUserPersonalApps());
  }

  @Test
  @SneakyThrows
  void setUserPersonalAppsEnabled() {
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.setUserPersonalAppsEnabled(true, TEST_USER));
    verify(settingService, never()).set(any(), any(), any(), any());

    applicationCenterService.setUserPersonalAppsEnabled(true, ADMIN_USERNAME);
    verify(settingService).set(any(), any(), any(), any());
  }

  @Test
  @SneakyThrows
  void settingsStoredAsSingleJsonBlob() {
    // Read: the whole settings object is deserialized from the single JSON blob
    doReturn(SettingValue.create("{\"allowUserPersonalApps\":true}")).when(settingService)
                                                                     .get(any(),
                                                                          any(),
                                                                          eq(ApplicationCenterService.APP_CENTER_SETTINGS_KEY));
    assertTrue(applicationCenterService.getSettings().isAllowUserPersonalApps());
    assertTrue(applicationCenterService.isUserPersonalAppsEnabled());

    // Write: the whole object is serialized back under the single blob key
    ArgumentCaptor<SettingValue<?>> captor = ArgumentCaptor.forClass(SettingValue.class);
    applicationCenterService.saveSettings(new ApplicationCenterSettings(false), ADMIN_USERNAME);
    verify(settingService).set(eq(ApplicationCenterService.APP_CENTER_CONTEXT),
                               eq(ApplicationCenterService.APP_CENTER_SCOPE),
                               eq(ApplicationCenterService.APP_CENTER_SETTINGS_KEY),
                               captor.capture());
    String storedJson = String.valueOf(captor.getValue().getValue());
    assertTrue(storedJson.contains("allowUserPersonalApps"));
  }

  @Test
  @SneakyThrows
  void createPersonalApplication() {
    Application emptyApp = new Application();
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.createPersonalApplication(emptyApp, ""));

    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.createPersonalApplication(emptyApp, TEST_USER));

    enablePersonalApps();
    Application saved = personalApp(TEST_USER);
    when(appCenterStorage.createApplication(any(Application.class))).thenReturn(saved);
    when(appCenterStorage.getApplication(ID)).thenReturn(saved);
    when(appCenterStorage.getMandatoryAndFavoriteApplications(TEST_USER, null)).thenReturn(Collections.emptyList());

    Application input = new Application();
    input.setTitle(TITLE);
    input.setUrl(URL);
    input.setAllowStick(true);
    input.setAllowDetach(true);
    Application result = applicationCenterService.createPersonalApplication(input, TEST_USER);

    assertNotNull(result);
    assertTrue(input.isPersonal());
    assertTrue(input.isActive());
    // a personal app is always available on mobile, whatever the client sent
    assertTrue(input.isMobile());
    // a personal app is a plain link, it can never be stuck nor detached
    assertFalse(input.isAllowStick());
    assertFalse(input.isAllowDetach());
    assertEquals(Collections.singletonList(TEST_USER), input.getPermissions());
    verify(appCenterStorage).createApplication(input);
    verify(appCenterStorage).addApplicationToUserFavorite(ID, TEST_USER);
  }

  @Test
  @SneakyThrows
  void updatePersonalApplication() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updatePersonalApplication(null, TEST_USER));
    Application emptyApp = new Application();
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updatePersonalApplication(emptyApp, TEST_USER));

    Application input = personalApp(TEST_USER);
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updatePersonalApplication(input, ""));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updatePersonalApplication(input, TEST_USER));

    enablePersonalApps();
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.updatePersonalApplication(input, TEST_USER));

    Application nonPersonal = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(nonPersonal);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updatePersonalApplication(input, TEST_USER));

    Application owned = personalApp(TEST_USER);
    when(appCenterStorage.getApplication(ID)).thenReturn(owned);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updatePersonalApplication(input, "someoneElse"));

    input.setMobile(false);
    input.setAllowStick(true);
    input.setAllowDetach(true);
    applicationCenterService.updatePersonalApplication(input, TEST_USER);
    verify(appCenterStorage).updateApplication(input);
    assertTrue(input.isPersonal());
    assertTrue(input.isActive());
    // an app stored before personal apps were made available on mobile is fixed
    // by its first update
    assertTrue(input.isMobile());
    assertFalse(input.isAllowStick());
    assertFalse(input.isAllowDetach());
  }

  @Test
  @SneakyThrows
  void getApplicationPlacements() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getApplicationPlacements("", "dw"));

    when(userPortalConfigService.getMetaPortal()).thenReturn("dw");
    when(placementStorage.getPlacedApplicationId(TEST_USER, PlacementSide.LEFT)).thenReturn(null);
    when(placementStorage.getPlacedApplicationId(TEST_USER, PlacementSide.RIGHT)).thenReturn(null);
    ApplicationPlacements placements = applicationCenterService.getApplicationPlacements(TEST_USER, "dw");
    assertNotNull(placements);
    assertTrue(placements.isSiteEligible());
    assertNull(placements.getLeft());
    assertNull(placements.getRight());

    Application application = application();
    application.setPermissions(Collections.singletonList(PERMISSIONS_2));
    application.setAllowStick(true);
    when(placementStorage.getPlacedApplicationId(TEST_USER, PlacementSide.LEFT)).thenReturn(ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    placements = applicationCenterService.getApplicationPlacements(TEST_USER, "dw");
    assertEquals(application, placements.getLeft());
    assertNull(placements.getRight());

    // the placement no longer qualifies once the admin turns the capability
    // off: the read drops it and clears the stored setting
    application.setAllowStick(false);
    placements = applicationCenterService.getApplicationPlacements(TEST_USER, "dw");
    assertNull(placements.getLeft());
    verify(placementStorage).removePlacedApplicationId(TEST_USER, PlacementSide.LEFT);

    application.setAllowStick(true);
    placements = applicationCenterService.getApplicationPlacements(TEST_USER, "administration");
    assertFalse(placements.isSiteEligible());

    when(navigationConfigurationService.isMetaSiteNavigation("intranet")).thenReturn(true);
    placements = applicationCenterService.getApplicationPlacements(TEST_USER, "intranet");
    assertTrue(placements.isSiteEligible());

    placements = applicationCenterService.getApplicationPlacements(TEST_USER, null);
    assertFalse(placements.isSiteEligible());
  }

  @Test
  @SneakyThrows
  void stickApplication() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, ""));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.stickApplication(ID, null, TEST_USER));

    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER));

    Application application = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(application);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER));

    application.setPermissions(Collections.singletonList(PERMISSIONS_2));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER));

    application.setAllowStick(true);
    application.setActive(false);
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER));

    application.setActive(true);
    when(placementStorage.getPlacedApplicationId(TEST_USER, PlacementSide.RIGHT)).thenReturn(ID);
    applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER);
    verify(placementStorage).setPlacedApplicationId(TEST_USER, PlacementSide.LEFT, ID);
    verify(placementStorage).removePlacedApplicationId(TEST_USER, PlacementSide.RIGHT);
  }

  @Test
  void unstickApplication() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.unstickApplication(PlacementSide.LEFT, ""));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.unstickApplication(null, TEST_USER));

    applicationCenterService.unstickApplication(PlacementSide.RIGHT, TEST_USER);
    verify(placementStorage).removePlacedApplicationId(TEST_USER, PlacementSide.RIGHT);
  }

  @Test
  @SneakyThrows
  void deletePersonalApplication() {
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.deletePersonalApplication(null, TEST_USER));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.deletePersonalApplication(ID, ""));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.deletePersonalApplication(ID, TEST_USER));

    enablePersonalApps();
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.deletePersonalApplication(ID, TEST_USER));

    Application nonPersonal = application();
    when(appCenterStorage.getApplication(ID)).thenReturn(nonPersonal);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.deletePersonalApplication(ID, TEST_USER));

    Application owned = personalApp(TEST_USER);
    when(appCenterStorage.getApplication(ID)).thenReturn(owned);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.deletePersonalApplication(ID, "someoneElse"));

    applicationCenterService.deletePersonalApplication(ID, TEST_USER);
    verify(appCenterStorage).deleteApplication(ID);
  }

  @Test
  void normalizePersonalUrlAddsMissingScheme() {
    assertEquals("https://meeds.io", applicationCenterService.normalizePersonalUrl("meeds.io"));
    assertEquals("https://www.meeds.io/apps?id=1", applicationCenterService.normalizePersonalUrl(" www.meeds.io/apps?id=1 "));
  }

  @Test
  void normalizePersonalUrlKeepsExplicitLinks() {
    assertEquals("https://meeds.io", applicationCenterService.normalizePersonalUrl("https://meeds.io"));
    // an explicit scheme is trusted, so an intranet host without a dot is allowed
    assertEquals("http://intranet/tools", applicationCenterService.normalizePersonalUrl("http://intranet/tools"));
    assertEquals("/portal/dw", applicationCenterService.normalizePersonalUrl("/portal/dw"));
    assertEquals("./dw", applicationCenterService.normalizePersonalUrl("./dw"));
  }

  @Test
  void normalizePersonalUrlRejectsSchemeRelativeLinks() {
    // a browser resolves all of these to https://evil.com, they are not portal
    // relative links whatever their leading slash suggests
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("//evil.com"));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("/\\evil.com"));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("/\t/evil.com"));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("/\n/evil.com"));
  }

  @Test
  void normalizePersonalUrlRejectsSchemesHiddenByControlChars() {
    // browsers strip the tab and execute java<TAB>script: as javascript:
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.normalizePersonalUrl("java\tscript:alert(1)"));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.normalizePersonalUrl("java\nscript:alert(1)"));
  }

  @Test
  void normalizePersonalUrlRejectsNonLinks() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl(null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("  "));
    // without a scheme, a bare word is not a host
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("toto"));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("javascript:alert(1)"));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.normalizePersonalUrl("ftp://meeds.io"));
  }

  private void enablePersonalApps() {
    SettingValue<?> value = SettingValue.create("{\"allowUserPersonalApps\":true}");
    doReturn(value).when(settingService).get(any(), any(), eq(ApplicationCenterService.APP_CENTER_SETTINGS_KEY));
  }

  @Test
  void getListedApplicationsKeepsTheOrderOfAccessibleActiveApplications() throws Exception {
    Application second = accessibleApplication(3L);
    Application forbidden = application(2L);
    Application inactive = accessibleApplication(4L);
    inactive.setActive(false);
    Application first = accessibleApplication(6L);
    when(appCenterStorage.getApplication(3L)).thenReturn(second);
    when(appCenterStorage.getApplication(2L)).thenReturn(forbidden);
    when(appCenterStorage.getApplication(4L)).thenReturn(inactive);
    when(appCenterStorage.getApplication(6L)).thenReturn(first);

    List<Application> applications = applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME,
                                                                                                              Arrays.asList(6L,
                                                                                                                            5L,
                                                                                                                            3L,
                                                                                                                            2L,
                                                                                                                            4L,
                                                                                                                            3L,
                                                                                                                            null)),
                                                                                    Locale.ENGLISH,
                                                                                    TEST_USER);

    assertEquals(Arrays.asList(6L, 3L), applications.stream().map(Application::getId).toList());
  }

  @Test
  void getListedApplicationsDecoratesCopiesNeverTheCachedInstance() throws Exception {
    Application cached = accessibleApplication(ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(cached);
    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         ID,
                                                         "title",
                                                         Locale.FRENCH)).thenReturn("Titre");
    when(badgePluginRegistry.resolveBadgeName(any())).thenReturn("badge");
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, List.of(ID));

    Application french = applicationCenterService.getListedApplications(filter, Locale.FRENCH, TEST_USER).get(0);
    Application english = applicationCenterService.getListedApplications(filter, Locale.ENGLISH, TEST_USER).get(0);

    assertEquals("Titre", french.getTitle());
    assertEquals(TITLE, english.getTitle());
    assertEquals("badge", english.getBadgeName());
    assertEquals(TITLE, cached.getTitle());
    assertNull(cached.getBadgeName());
    assertEquals(List.of(PERMISSIONS_2), cached.getPermissions());
    assertNull(english.getPermissions());
  }

  @Test
  void getListedApplicationsRefusesMoreThanTheCap() throws Exception {
    List<Long> ids = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS + 1L).boxed().toList();
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, ids);

    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getListedApplications(filter, null, TEST_USER));
    assertTrue(applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME, null), null, TEST_USER)
                                       .isEmpty());
    assertThrows(ObjectNotFoundException.class, () -> applicationCenterService.getListedApplications(null, null, TEST_USER));
  }

  @Test
  void getListedApplicationsGroupsCategoriesInOrderSortedByTitle() throws Exception {
    titled(1L, "alpha");
    titled(2L, "Bravo");
    titled(3L, "Charlie");
    titled(4L, "Delta");
    titled(5L, "Echo");
    when(appCenterStorage.getApplication(6L)).thenReturn(application(6L));
    titled(7L, "Foxtrot").setActive(false);
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(3L, 1L, 2L));
    when(applicationCategoryStorage.getApplicationIds(20L)).thenReturn(List.of(5L, 6L, 1L, 7L, 8L, 4L));

    assertEquals(List.of(1L, 2L, 3L, 4L, 5L), listedIds(Arrays.asList(10L, 20L, 10L, null), Locale.ENGLISH));
    // An application listed by several categories is listed in the first one only
    assertEquals(List.of(1L, 4L, 5L, 2L, 3L), listedIds(List.of(20L, 10L), Locale.ENGLISH));
  }

  @Test
  void getListedApplicationsSortsEachCategoryByTheLocalizedTitle() throws Exception {
    titled(1L, "alpha");
    Application third = titled(3L, "Charlie");
    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         3L,
                                                         "title",
                                                         Locale.FRENCH)).thenReturn("Abricot");
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(1L, 3L));

    assertEquals(List.of(3L, 1L), listedIds(List.of(10L), Locale.FRENCH));
    assertEquals(List.of(1L, 3L), listedIds(List.of(10L), Locale.ENGLISH));
    assertEquals("Charlie", third.getTitle());
  }

  @Test
  void getListedApplicationsSortsSystemApplicationsByTheirDisplayedTitle() throws Exception {
    titled(1L, "Lima");
    Application drives = titled(2L, "Drives");
    drives.setSystem(true);
    Application spaces = titled(3L, "List Spaces");
    spaces.setSystem(true);
    spaces.setDescription(null);
    // Not a system application: its stored title is the displayed one
    titled(4L, "Wallet");
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(1L, 2L, 3L, 4L));
    when(resourceBundleService.getResourceBundle(ApplicationCenterService.SYSTEM_APPLICATIONS_BUNDLE,
                                                 Locale.ENGLISH)).thenReturn(bundle("appCenter.system.application.drives",
                                                                                    "My Drive",
                                                                                    "appCenter.system.application.list.spaces",
                                                                                    "Spaces",
                                                                                    "appCenter.system.application.list.spaces.description",
                                                                                    "List your spaces",
                                                                                    "appCenter.system.application.drives.description",
                                                                                    "Your files",
                                                                                    "appCenter.system.application.wallet",
                                                                                    "A Wallet"));
    when(resourceBundleService.getResourceBundle(ApplicationCenterService.SYSTEM_APPLICATIONS_BUNDLE,
                                                 Locale.FRENCH)).thenReturn(bundle("appCenter.system.application.drives",
                                                                                   "Lecteurs"));

    // Stored titles would give Drives, Lima, List Spaces, Wallet
    assertEquals(List.of(1L, 2L, 3L, 4L), listedIds(List.of(10L), Locale.ENGLISH));
    assertEquals(List.of(2L, 1L, 3L, 4L), listedIds(List.of(10L), Locale.FRENCH));
    assertEquals("Drives", drives.getTitle());
    Map<Long, Application> english = applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME,
                                                                                                              null,
                                                                                                              List.of(10L)),
                                                                                    Locale.ENGLISH,
                                                                                    TEST_USER)
                                                             .stream()
                                                             .collect(Collectors.toMap(Application::getId, Function.identity()));
    // The description is filled only when the application has none
    assertEquals("List your spaces", english.get(3L).getDescription());
    assertEquals(DESCRIPTION, english.get(2L).getDescription());
    assertNull(spaces.getDescription());
  }

  @Test
  void getListedApplicationsSortsACategoryBeforeTruncatingIt() throws Exception {
    List<Long> ids = LongStream.rangeClosed(1, 150).boxed().toList();
    ids.forEach(id -> titled(id, String.format("App %03d", 151 - id)));
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(ids);

    List<Long> listedIds = listedIds(List.of(10L, 20L), Locale.ENGLISH);

    assertEquals(ApplicationCenterService.MAX_LISTED_APPLICATIONS, listedIds.size());
    assertEquals(150L, listedIds.get(0));
    assertEquals(51L, listedIds.get(listedIds.size() - 1));
    verify(applicationCategoryStorage, never()).getApplicationIds(20L);
  }

  @Test
  void getListedApplicationsFillsTheCapAcrossCategories() throws Exception {
    List<Long> ids = LongStream.rangeClosed(1, 99).boxed().toList();
    ids.forEach(id -> titled(id, String.format("App %03d", id)));
    titled(200L, "Zulu");
    titled(201L, "Yankee");
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(ids);
    when(applicationCategoryStorage.getApplicationIds(20L)).thenReturn(List.of(200L, 201L));

    List<Long> listedIds = listedIds(List.of(10L, 20L), Locale.ENGLISH);

    assertEquals(ApplicationCenterService.MAX_LISTED_APPLICATIONS, listedIds.size());
    assertEquals(201L, listedIds.get(99));
  }

  @Test
  void getListedApplicationsRefusesAnInvalidCategoryListing() throws Exception {
    ApplicationListFilter both = new ApplicationListFilter(SETTING_NAME, List.of(ID), List.of(10L));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getListedApplications(both, null, TEST_USER));
    List<Long> overCap = LongStream.rangeClosed(1, MyApplicationsSettingsService.MAX_LISTED_CATEGORIES + 1L).boxed().toList();
    ApplicationListFilter tooMany = new ApplicationListFilter(SETTING_NAME, null, overCap);
    assertThrows(IllegalArgumentException.class, () -> applicationCenterService.getListedApplications(tooMany, null, TEST_USER));
    verify(applicationCategoryStorage, never()).getApplicationIds(anyLong());

    List<Long> atCap = LongStream.rangeClosed(1, MyApplicationsSettingsService.MAX_LISTED_CATEGORIES).boxed().toList();
    when(applicationCategoryStorage.getApplicationIds(anyLong())).thenReturn(List.of());
    assertTrue(applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME, List.of(), atCap),
                                                              null,
                                                              TEST_USER)
                                       .isEmpty());
  }

  @Test
  void getListedApplicationsByCategoryDecoratesCopiesNeverTheCachedInstance() throws Exception {
    Application cached = titled(ID, TITLE);
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(ID));
    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         ID,
                                                         "title",
                                                         Locale.FRENCH)).thenReturn("Titre");
    when(badgePluginRegistry.resolveBadgeName(any())).thenReturn("badge");
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, null, List.of(10L));

    Application french = applicationCenterService.getListedApplications(filter, Locale.FRENCH, TEST_USER).get(0);
    Application english = applicationCenterService.getListedApplications(filter, Locale.ENGLISH, TEST_USER).get(0);

    assertEquals("Titre", french.getTitle());
    assertEquals(TITLE, english.getTitle());
    assertEquals("badge", english.getBadgeName());
    assertEquals(TITLE, cached.getTitle());
    assertNull(cached.getBadgeName());
    assertNull(english.getPermissions());
  }

  @Test
  void getContextApplicationsByCategoryRequiresTheSettingsRight() throws Exception {
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, null, List.of(10L));
    titled(ID, TITLE);
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(ID));

    assertThrows(IllegalAccessException.class, () -> applicationCenterService.getContextApplications(filter, null, TEST_USER));
    verify(applicationCategoryStorage, never()).getApplicationIds(10L);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    assertEquals(1, applicationCenterService.getContextApplications(filter, null, TEST_USER).size());
  }

  @Test
  void clearCategoryApplicationsClearsTheStorageCache() {
    applicationCenterService.clearCategoryApplications();
    verify(applicationCategoryStorage).clearCache();
  }

  @Test
  void listingAndSuggestionReadsRefuseAnUnknownSetting() {
    ApplicationListFilter filter = new ApplicationListFilter("Untitled-404", List.of(ID));
    when(myApplicationsSettingsService.canEditSettings("Untitled-404", TEST_USER)).thenReturn(true);

    assertThrows(ObjectNotFoundException.class, () -> applicationCenterService.getListedApplications(filter, null, TEST_USER));
    assertThrows(ObjectNotFoundException.class, () -> applicationCenterService.getContextApplications(filter, null, TEST_USER));
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.getContextSuggestions("Untitled-404", null, null, 0, 10, null, TEST_USER));
    verify(appCenterStorage, never()).getApplication(ID);
    verify(appCenterStorage, never()).getApplicationIds(any());
  }

  @Test
  void getContextSuggestionsDecorateCopiesNeverTheCachedInstance() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    Application cached = accessibleApplication(ID);
    when(appCenterStorage.getApplicationIds(null)).thenReturn(List.of(ID));
    when(appCenterStorage.getApplication(ID)).thenReturn(cached);
    when(translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                         ID,
                                                         "title",
                                                         Locale.FRENCH)).thenReturn("Titre");
    when(badgePluginRegistry.resolveBadgeName(any())).thenReturn("badge");

    Application french = applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 10, Locale.FRENCH, TEST_USER)
                                                 .get(0);
    Application english = applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 10, Locale.ENGLISH, TEST_USER)
                                                  .get(0);

    assertEquals("Titre", french.getTitle());
    assertEquals(TITLE, english.getTitle());
    assertEquals(TITLE, cached.getTitle());
    assertNull(cached.getBadgeName());
    assertEquals(List.of(PERMISSIONS_2), cached.getPermissions());
    assertNull(english.getPermissions());
  }

  @Test
  void getContextApplicationsRequiresTheSettingsRight() throws Exception {
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, List.of(ID));
    when(appCenterStorage.getApplication(ID)).thenReturn(accessibleApplication(ID));

    assertThrows(IllegalAccessException.class, () -> applicationCenterService.getContextApplications(filter, null, TEST_USER));
    verify(appCenterStorage, never()).getApplication(ID);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    assertEquals(1, applicationCenterService.getContextApplications(filter, null, TEST_USER).size());
  }

  /**
   * The rendered view lists the same applications to every viewer: fails the
   * mutant listing to the window's editors, in either selection mode, an
   * application the window created whose audience excludes them
   */
  @Test
  void getListedApplicationsShowsTheWindowsOwnApplicationToItsAudienceOnly() throws Exception {
    Application outsideAudience = contextApplication(ID, SETTING_NAME);
    outsideAudience.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(outsideAudience);
    lenient().when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(ID));
    lenient().when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);

    assertTrue(applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME, List.of(ID)),
                                                              null,
                                                              TEST_USER)
                                       .isEmpty());
    assertTrue(listedIds(List.of(10L), null).isEmpty());
  }

  /**
   * The window's editors get what it created from its settings listing,
   * whatever its audience: fails the mutant listing it only to whoever may
   * access it, where an editor outside the audience, such as a publisher
   * outside the space, creates an application the drawer then hides from them,
   * whether the filter selects applications or categories, and the mutant
   * listing it once an administrator emptied its audience
   */
  @Test
  void getContextApplicationsShowsTheWindowsOwnApplicationToItsEditors() throws Exception {
    Application outsideAudience = contextApplication(ID, SETTING_NAME);
    outsideAudience.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(outsideAudience);
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(ID));
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    ApplicationListFilter byIds = new ApplicationListFilter(SETTING_NAME, List.of(ID));
    ApplicationListFilter byCategories = new ApplicationListFilter(SETTING_NAME, null, List.of(10L));

    assertEquals(List.of(ID), ids(applicationCenterService.getContextApplications(byIds, null, TEST_USER)));
    assertEquals(List.of(ID), ids(applicationCenterService.getContextApplications(byCategories, null, TEST_USER)));

    // An administrator emptied its audience: shown to administrators only
    Application emptied = contextApplication(ID, SETTING_NAME);
    emptied.setPermissions(List.of());
    when(appCenterStorage.getApplication(ID)).thenReturn(emptied);
    assertTrue(applicationCenterService.getContextApplications(byIds, null, TEST_USER).isEmpty());
    assertTrue(applicationCenterService.getContextApplications(byCategories, null, TEST_USER).isEmpty());
  }

  /**
   * Fails the mutant also requiring the editor to access the application: the
   * window's right authorizes the delete of what the window created
   */
  @Test
  void deleteContextApplicationDeletesTheWindowsOwnApplicationOutsideTheEditorsAudience() throws Exception {
    mockContext(SPACE_ID);
    Application outsideAudience = contextApplication(ID, SETTING_NAME);
    outsideAudience.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(outsideAudience);

    applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER);

    verify(appCenterStorage).deleteApplication(ID);
  }

  /**
   * The url columns of a MySQL database are utf8mb3: fails the mutant letting
   * a character outside the Basic Multilingual Plane reach the insert, which
   * MySQL refuses, answering a 500
   */
  @Test
  void createContextApplicationRefusesAUrlCharacterTheColumnCannotStore() throws Exception {
    mockContext(SPACE_ID);

    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "urlUnsupportedCharacter",
                                    form -> form.setUrl("https://meeds.io/a\uD83D\uDE00b"));
    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "helpPageURLUnsupportedCharacter",
                                    form -> form.setHelpPageURL("/portal/\uD83D\uDE00"));
    assertEquals("https://meeds.io/caf\u00e9", createdContextApplication(form -> form.setUrl("https://meeds.io/caf\u00e9")).getUrl());
  }

  @Test
  void getContextSuggestionsRequiresTheSettingsRight() {
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.getContextSuggestions(SETTING_NAME, KEYWORD, null, 0, 10, null, TEST_USER));
    verify(appCenterStorage, never()).getApplicationIds(any());
  }

  @Test
  void getContextSuggestionsOffersActiveNonPersonalAccessibleApplications() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    Application personal = personalApp(TEST_USER);
    personal.setId(7L);
    Application inactive = accessibleApplication(8L);
    inactive.setActive(false);
    when(appCenterStorage.getApplicationIds(KEYWORD)).thenReturn(Arrays.asList(1L, 2L, 7L, 8L, 9L, 10L));
    when(appCenterStorage.getApplication(1L)).thenReturn(accessibleApplication(1L));
    when(appCenterStorage.getApplication(2L)).thenReturn(application(2L));
    when(appCenterStorage.getApplication(7L)).thenReturn(personal);
    when(appCenterStorage.getApplication(8L)).thenReturn(inactive);
    when(appCenterStorage.getApplication(10L)).thenReturn(accessibleApplication(10L));

    assertEquals(Arrays.asList(1L, 10L),
                 applicationCenterService.getContextSuggestions(SETTING_NAME, KEYWORD, null, 0, 10, null, TEST_USER)
                                         .stream()
                                         .map(Application::getId)
                                         .toList());
    assertEquals(List.of(10L),
                 applicationCenterService.getContextSuggestions(SETTING_NAME, KEYWORD, null, 1, 10, null, TEST_USER)
                                         .stream()
                                         .map(Application::getId)
                                         .toList());
  }

  @Test
  void getContextSuggestionsReturnsAtMostTheCap() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    List<Long> ids = LongStream.rangeClosed(1, 30).boxed().toList();
    when(appCenterStorage.getApplicationIds(null)).thenReturn(ids);
    ids.forEach(id -> when(appCenterStorage.getApplication(id)).thenReturn(accessibleApplication(id)));

    assertEquals(ApplicationCenterService.MAX_SUGGESTIONS,
                 applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 100, null, TEST_USER).size());
    assertEquals(ApplicationCenterService.MAX_SUGGESTIONS,
                 applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 0, null, TEST_USER).size());
    assertEquals(5, applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 5, null, TEST_USER).size());
  }

  @Test
  void getContextSuggestionsExcludesTheSelectedApplicationsBeforeThePage() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    List<Long> ids = LongStream.rangeClosed(1, 30).boxed().toList();
    when(appCenterStorage.getApplicationIds(null)).thenReturn(ids);
    ids.forEach(id -> lenient().when(appCenterStorage.getApplication(id)).thenReturn(accessibleApplication(id)));
    List<Long> excluded = LongStream.rangeClosed(1, 15).boxed().toList();

    List<Long> suggested = applicationCenterService.getContextSuggestions(SETTING_NAME, null, excluded, 0, 20, null, TEST_USER)
                                                   .stream()
                                                   .map(Application::getId)
                                                   .toList();

    assertEquals(LongStream.rangeClosed(16, 30).boxed().toList(), suggested);
    verify(appCenterStorage, never()).getApplication(1L);
  }

  @Test
  void getContextSuggestionsRefusesMoreExcludedIdsThanTheCap() {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    List<Long> excluded = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS + 1L).boxed().toList();

    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.getContextSuggestions(SETTING_NAME, null, excluded, 0, 20, null, TEST_USER));
  }

  @Test
  void createContextApplicationRefusesAnUnknownSettingBeforeTheRight() throws Exception {
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER));
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.createContextApplication(contextForm(), null, TEST_USER));
    verify(myApplicationsSettingsService, never()).canEditSettings(any(), any());
    verifyNoContextApplicationCreated();
  }

  @Test
  void createContextApplicationRequiresTheRightToManageTheWindowApplications() throws Exception {
    mockContext(SPACE_ID);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(false);
    ApplicationForm form = contextForm();
    form.setImageUploadId(UPLOAD_ID);

    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER));
    verify(appCenterStorage, never()).readUpload(any(), anyInt());
    verifyNoContextApplicationCreated();
  }

  @Test
  void createContextApplicationGivesTheSpaceMembersAudienceInASpaceContext() throws Exception {
    mockContext(SPACE_ID);
    ApplicationForm form = contextForm();
    form.setPermissions(List.of("*:/platform/administrators"));

    Application created = applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    verify(appCenterStorage).createContextApplication(argThat(application -> List.of("*:" + SPACE_GROUP)
                                                                                 .equals(application.getPermissions())),
                                                      eq(SETTING_NAME),
                                                      eq(SPACE_ID));
    assertEquals(CREATED_ID, created.getId());
    assertEquals(SETTING_NAME, created.getContextSettingName());
    assertEquals(SPACE_ID, created.getContextSpaceId());
  }

  /**
   * A window rendered before its space was keyed on its page's site recorded
   * the space of the request: fails the mutant reading that recorded space,
   * where a space's page first reached through a portal site's navigation
   * gives its apps the platform audience, and they outlive the space
   */
  @Test
  void createContextApplicationTakesTheSpaceOfTheRecordedPageNotTheRecordedSpace() throws Exception {
    mockContext(0L);
    when(myApplicationsSettingsService.getContextSpaceId(SETTING_NAME, PAGE_REFERENCE)).thenReturn(SPACE_ID);

    Application created = applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER);

    verify(appCenterStorage).createContextApplication(argThat(application -> List.of("*:" + SPACE_GROUP)
                                                                                 .equals(application.getPermissions())),
                                                      eq(SETTING_NAME),
                                                      eq(SPACE_ID));
    assertEquals(SPACE_ID, created.getContextSpaceId());
  }

  @Test
  void createContextApplicationGivesEveryPlatformUserAudienceInThePlatformContext() throws Exception {
    mockContext(0L);

    applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER);

    verify(appCenterStorage).createContextApplication(argThat(application -> List.of("*:/platform/users",
                                                                                     "*:/platform/externals")
                                                                                 .equals(application.getPermissions())),
                                                      eq(SETTING_NAME),
                                                      eq(0L));
    verify(spaceService, never()).getSpaceById(anyLong());
  }

  @Test
  void createContextApplicationRefusesTheContextOfADeletedSpace() throws Exception {
    mockContext(SPACE_ID);
    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(null);

    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER));
    verifyNoContextApplicationCreated();
  }

  /**
   * A deleted space's site no longer resolves to a space: fails the mutant
   * creating without checking it, where a window of a deleted space would
   * create an application for every platform user
   */
  @Test
  void createContextApplicationRefusesAWindowOfADeletedSpace() throws Exception {
    mockContext(0L);
    when(myApplicationsSettingsService.isContextDeleted(SETTING_NAME, PAGE_REFERENCE)).thenReturn(true);

    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER));
    verifyNoContextApplicationCreated();
  }

  /**
   * A space deleted while the creation resolves it resolves to none: fails
   * the mutant checking the deletion before resolving the space, where such a
   * window would create an application for every platform user
   */
  @Test
  void createContextApplicationRefusesAWindowWhoseSpaceIsDeletedWhileItResolves() throws Exception {
    mockContext(SPACE_ID);
    AtomicBoolean deleted = new AtomicBoolean();
    when(myApplicationsSettingsService.getContextSpaceId(SETTING_NAME, PAGE_REFERENCE)).thenAnswer(invocation -> {
      deleted.set(true);
      return 0L;
    });
    when(myApplicationsSettingsService.isContextDeleted(SETTING_NAME, PAGE_REFERENCE)).thenAnswer(invocation -> deleted.get());

    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER));
    verifyNoContextApplicationCreated();
  }

  /**
   * A portal page first rendered through a space's navigation recorded that
   * space: fails the mutant falling back to the recorded space, where an
   * application created from a portal page takes a space's audience
   */
  @Test
  void createContextApplicationGivesAPortalPageThePlatformAudienceWhateverSpaceItsSettingRecorded() throws Exception {
    mockContext(SPACE_ID);
    when(myApplicationsSettingsService.getContextSpaceId(SETTING_NAME, PAGE_REFERENCE)).thenReturn(0L);

    applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER);

    verify(appCenterStorage).createContextApplication(argThat(application -> List.of("*:/platform/users",
                                                                                     "*:/platform/externals")
                                                                                 .equals(application.getPermissions())),
                                                      eq(SETTING_NAME),
                                                      eq(0L));
  }

  @Test
  void createContextApplicationForcesTheFieldsTheFormDoesNotOffer() throws Exception {
    mockContext(SPACE_ID);
    ApplicationForm form = contextForm();
    form.setMandatory(true);
    form.setDefault(true);
    form.setPwa(true);
    form.setSystem(true);
    form.setPersonal(true);
    form.setAllowStick(true);
    form.setShortcut(SHORTCUT);
    form.setActive(false);
    form.setImageFileId(IMAGE_FILE_ID);
    form.setOrder(3L);
    form.setChangedManually(true);
    form.setContextSettingName("Untitled-13-forged");
    form.setContextSpaceId(SPACE_ID + 1);

    applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
    verify(appCenterStorage).createContextApplication(captor.capture(), eq(SETTING_NAME), eq(SPACE_ID));
    Application stored = captor.getValue();
    assertFalse(stored.isMandatory());
    assertFalse(stored.isDefault());
    assertFalse(stored.isPwa());
    assertFalse(stored.isSystem());
    assertFalse(stored.isPersonal());
    assertFalse(stored.isAllowStick());
    assertNull(stored.getShortcut());
    assertTrue(stored.isActive());
    assertNull(stored.getImageFileId());
    assertNull(stored.getOrder());
    assertFalse(stored.isChangedManually());
    assertNull(stored.getContextSettingName());
    assertNull(stored.getContextSpaceId());
  }

  @Test
  void createContextApplicationKeepsTheFieldsTheFormOffers() throws Exception {
    mockContext(SPACE_ID);
    when(badgePluginRegistry.getPlugin("agenda")).thenReturn(mock(ApplicationBadgePlugin.class));
    ApplicationForm form = contextForm();
    form.setSameTab(false);
    form.setMobile(false);
    form.setAllowDetach(true);
    form.setBadgeName("agenda");
    form.setHelpPageURL("meeds.io/help");

    applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    Application stored = lastCreatedContextApplication();
    assertEquals(TITLE, stored.getTitle());
    assertEquals(DESCRIPTION, stored.getDescription());
    assertEquals(ApplicationType.LINK, stored.getType());
    assertEquals("https://meeds.io", stored.getUrl());
    assertFalse(stored.isSameTab());
    assertFalse(stored.isMobile());
    assertTrue(stored.isAllowDetach());
    assertEquals("fa-star", stored.getIcon());
    assertEquals("agenda", stored.getBadgeName());
    assertEquals("https://meeds.io/help", stored.getHelpPageURL());
  }

  @Test
  void createContextApplicationValidatesTheUrlOfEachType() throws Exception {
    mockContext(SPACE_ID);
    assertEquals("https://meeds.io", createdContextApplication(form -> form.setUrl("meeds.io")).getUrl());
    assertEquals("/portal/dw", createdContextApplication(form -> form.setUrl("/portal/dw")).getUrl());
    assertEquals("https://meeds.io", createdContextApplication(form -> {
      form.setType(null);
      form.setUrl("meeds.io");
    }).getUrl());
    assertEquals("notifications", createdContextApplication(form -> {
      form.setType(ApplicationType.DRAWER);
      form.setUrl("notifications");
    }).getUrl());
    assertEquals("35", createdContextApplication(form -> {
      form.setType(ApplicationType.PORTLET);
      form.setUrl("35");
    }).getUrl());
    verify(portletInstanceService).getPortletInstance(35L, TEST_USER, null, false);

    when(portletInstanceService.getPortletInstance(36L, TEST_USER, null, false)).thenThrow(new IllegalAccessException());
    when(portletInstanceService.getPortletInstance(37L, TEST_USER, null, false)).thenThrow(new ObjectNotFoundException("missing"));
    assertContextApplicationRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> form.setUrl("javascript:alert(1)"));
    assertContextApplicationRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> form.setUrl("//evil.example.org"));
    assertContextApplicationRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> form.setUrl(null));
    for (String url : Arrays.asList("../notifications", "notifications\"><img>", "", null)) {
      assertContextApplicationRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> {
        form.setType(ApplicationType.DRAWER);
        form.setUrl(url);
      });
    }
    for (String url : Arrays.asList("abc", "0", "-35", "36", "37", null)) {
      assertContextApplicationRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> {
        form.setType(ApplicationType.PORTLET);
        form.setUrl(url);
      });
    }
    verify(appCenterStorage, times(5)).createContextApplication(any(), eq(SETTING_NAME), eq(SPACE_ID));
  }

  @Test
  void createContextApplicationValidatesTheIconTheBadgeAndTheHelpPage() throws Exception {
    mockContext(SPACE_ID);
    when(badgePluginRegistry.getPlugin("agenda")).thenReturn(mock(ApplicationBadgePlugin.class));

    assertEquals("agenda", createdContextApplication(form -> form.setBadgeName("agenda")).getBadgeName());
    assertEquals(ApplicationBadgePluginRegistry.BADGE_DISABLED,
                 createdContextApplication(form -> form.setBadgeName(ApplicationBadgePluginRegistry.BADGE_DISABLED))
                                                                                                                  .getBadgeName());
    assertNull(createdContextApplication(form -> form.setBadgeName("unknown")).getBadgeName());
    assertEquals("fas fa-star", createdContextApplication(form -> form.setIcon("fas fa-star")).getIcon());
    assertNull(createdContextApplication(form -> form.setIcon(" ")).getIcon());
    assertNull(createdContextApplication(form -> form.setHelpPageURL(" ")).getHelpPageURL());

    assertContextApplicationRefused(ApplicationCenterService.INVALID_ICON_MESSAGE, form -> form.setIcon("fa-star\"><img src=x>"));
    assertContextApplicationRefused(ApplicationCenterService.INVALID_ICON_MESSAGE, form -> form.setIcon("fa-star;color:red"));
    assertContextApplicationRefused(ApplicationCenterService.INVALID_HELP_PAGE_URL_MESSAGE,
                                    form -> form.setHelpPageURL("javascript:alert(1)"));
  }

  @Test
  void createContextApplicationRefusesABlankTitleOrTooManyCategories() throws Exception {
    mockContext(SPACE_ID);

    assertContextApplicationRefused(ApplicationCenterService.TITLE_MANDATORY_MESSAGE, form -> form.setTitle(" "));
    assertContextApplicationRefused(ApplicationCenterService.TOO_MANY_CATEGORIES_MESSAGE, form -> form.setCategoryIds(LongStream.rangeClosed(1,
                                                                                      MyApplicationsSettingsService.MAX_LISTED_CATEGORIES
                                                                                         + 1L)
                                                                          .boxed()
                                                                          .toList()));
    verifyNoContextApplicationCreated();
  }

  @Test
  void createContextApplicationStoresAnUploadedImageWithTheTypeOfItsContent() throws Exception {
    mockContext(SPACE_ID);
    byte[] png = png(ApplicationCenterService.MAX_ILLUSTRATION_SIZE);
    when(appCenterStorage.readUpload(UPLOAD_ID, ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(png);
    when(appCenterStorage.saveIllustration(png, "image/png")).thenReturn(IMAGE_FILE_ID);
    ApplicationForm form = contextForm();
    form.setImageUploadId(UPLOAD_ID);
    form.setIllustrationMimeType("image/svg+xml");

    applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    assertEquals(IMAGE_FILE_ID, lastCreatedContextApplication().getImageFileId());
    InOrder inOrder = inOrder(appCenterStorage);
    inOrder.verify(appCenterStorage).saveIllustration(png, "image/png");
    inOrder.verify(appCenterStorage).createContextApplication(any(), eq(SETTING_NAME), eq(SPACE_ID));
    inOrder.verify(appCenterStorage).removeUpload(UPLOAD_ID);
  }

  @Test
  void createContextApplicationRefusesAnInvalidIllustration() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.readUpload("unknown", ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(null);
    when(appCenterStorage.readUpload("tooLarge",
                                     ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(png(ApplicationCenterService.MAX_ILLUSTRATION_SIZE
                                         + 1));
    when(appCenterStorage.readUpload("svg", ApplicationCenterService.MAX_ILLUSTRATION_SIZE))
                                                                                             .thenReturn("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

    assertIllustrationRefused("unknown", ApplicationCenterService.INVALID_UPLOAD_MESSAGE);
    assertIllustrationRefused("tooLarge", ApplicationCenterService.TOO_LARGE_MESSAGE);
    assertIllustrationRefused("svg", ApplicationCenterService.INVALID_TYPE_MESSAGE);
    verify(appCenterStorage, never()).saveIllustration(any(), any());
    verify(appCenterStorage, never()).removeUpload(any());
    verifyNoContextApplicationCreated();
  }

  @Test
  void createContextApplicationLinksTheCategoriesTheUserMayLink() throws Exception {
    mockContext(SPACE_ID);
    doThrow(new IllegalAccessException("denied")).when(categoryLinkService).link(eq(20L), any(), eq(TEST_USER));
    ApplicationForm form = contextForm();
    form.setCategoryIds(Arrays.asList(10L, 20L, 10L, null, 30L));

    Application created = applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    assertEquals(List.of(10L, 30L), created.getCategoryIds());
    CategoryObject object = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(CREATED_ID), 0L);
    verify(categoryLinkService).link(10L, object, TEST_USER);
    verify(categoryLinkService).link(30L, object, TEST_USER);
    verify(categoryLinkService, times(3)).link(anyLong(), any(), any());
  }

  @Test
  void createContextApplicationKeepsTheUploadWhenTheApplicationIsNotCreated() throws Exception {
    mockContext(SPACE_ID);
    byte[] png = png(100);
    when(appCenterStorage.readUpload(UPLOAD_ID, ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(png);
    when(appCenterStorage.saveIllustration(png, "image/png")).thenReturn(IMAGE_FILE_ID);
    when(appCenterStorage.createContextApplication(any(), eq(SETTING_NAME), anyLong())).thenThrow(new IllegalStateException("insert refused"));
    ApplicationForm form = contextForm();
    form.setImageUploadId(UPLOAD_ID);

    assertThrows(IllegalStateException.class, () -> applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER));
    verify(appCenterStorage, never()).removeUpload(any());
    verify(categoryLinkService, never()).link(anyLong(), any(), any());
  }

  @Test
  void createContextApplicationReturnsTheApplicationWhenACategoryFailsToLink() throws Exception {
    mockContext(SPACE_ID);
    doThrow(new IllegalStateException("database unreachable")).when(categoryLinkService).link(eq(20L), any(), eq(TEST_USER));
    ApplicationForm form = contextForm();
    form.setCategoryIds(List.of(20L, 30L));

    Application created = applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);

    assertEquals(CREATED_ID, created.getId());
    assertEquals(List.of(30L), created.getCategoryIds());
  }

  @Test
  void createContextApplicationRefusesAFieldLongerThanItsColumn() throws Exception {
    mockContext(SPACE_ID);
    assertEquals(250, createdContextApplication(form -> form.setTitle("t".repeat(250))).getTitle().length());
    assertEquals(1000, createdContextApplication(form -> form.setDescription("d".repeat(1000))).getDescription().length());
    String longestUrl = "https://meeds.io/" + "p".repeat(500 - "https://meeds.io/".length());
    assertEquals(longestUrl, createdContextApplication(form -> form.setUrl(longestUrl)).getUrl());

    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "titleTooLong", form -> form.setTitle("t".repeat(251)));
    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "descriptionTooLong", form -> form.setDescription("d".repeat(1001)));
    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "urlTooLong", form -> form.setUrl(longestUrl + "p"));
    // The scheme added to a schemeless url counts
    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "urlTooLong", form -> form.setUrl(longestUrl.substring("https://".length()) + "p"));
    assertContextApplicationRefused(CONTEXTUAL_PREFIX + "helpPageURLTooLong", form -> form.setHelpPageURL(longestUrl + "p"));
    verify(appCenterStorage, times(3)).createContextApplication(any(), eq(SETTING_NAME), eq(SPACE_ID));
  }

  @Test
  void deleteContextApplicationChecksTheSettingTheRightTheApplicationAndItsOwner() throws Exception {
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER));
    mockContext(SPACE_ID);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(false);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER));
    verify(appCenterStorage, never()).getApplication(ID);

    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER));
    Application inaccessible = contextApplication(ID, OTHER_SETTING_NAME);
    inaccessible.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(inaccessible);
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER));
    when(appCenterStorage.getApplication(ID)).thenReturn(accessibleApplication(ID));
    assertEquals(ApplicationCenterService.NOT_CONTEXTUAL_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER))
                                                                                                                   .getMessage());
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, OTHER_SETTING_NAME));
    assertEquals(ApplicationCenterService.NOT_OWNED_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER))
                                                                                                                   .getMessage());
    verify(appCenterStorage, never()).deleteApplication(anyLong());

    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    when(categoryLinkService.getLinkedIds(categoryObject)).thenReturn(List.of(10L));
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    applicationCenterService.deleteContextApplication(ID, SETTING_NAME, TEST_USER);
    InOrder inOrder = inOrder(appCenterStorage);
    inOrder.verify(appCenterStorage).deleteApplication(ID);
    inOrder.verify(appCenterStorage).deleteIllustration(IMAGE_FILE_ID);
    verify(categoryLinkService).unlink(10L, categoryObject);
  }

  /**
   * Fails the mutants dropping or reordering a check: an editor of another
   * window, or of none, would rewrite an application of the Application Center
   * or of another window
   */
  @Test
  void updateContextApplicationChecksTheSettingTheRightTheApplicationAndItsOwner() throws Exception {
    ApplicationForm form = contextForm();
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER));
    mockContext(SPACE_ID);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(false);
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER));
    verify(appCenterStorage, never()).getApplication(ID);

    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER));
    Application inaccessible = contextApplication(ID, OTHER_SETTING_NAME);
    inaccessible.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(inaccessible);
    assertThrows(ObjectNotFoundException.class,
                 () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER));
    when(appCenterStorage.getApplication(ID)).thenReturn(accessibleApplication(ID));
    assertEquals(ApplicationCenterService.NOT_CONTEXTUAL_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER))
                                                                                                                         .getMessage());
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, OTHER_SETTING_NAME));
    assertEquals(ApplicationCenterService.NOT_OWNED_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER))
                                                                                                                         .getMessage());
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    assertThrows(IllegalArgumentException.class,
                 () -> applicationCenterService.updateContextApplication(ID, null, SETTING_NAME, TEST_USER));
    verifyNoContextApplicationUpdated();

    assertEquals(ID, applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER).getId());
    verify(appCenterStorage).updateContextApplication(any());
  }

  /**
   * Fails the mutant also requiring the editor to access the application: the
   * window's right authorizes the update of what the window created
   */
  @Test
  void updateContextApplicationUpdatesTheWindowsOwnApplicationOutsideTheEditorsAudience() throws Exception {
    mockContext(SPACE_ID);
    Application outsideAudience = contextApplication(ID, SETTING_NAME);
    outsideAudience.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(ID)).thenReturn(outsideAudience);

    applicationCenterService.updateContextApplication(ID, contextForm(), SETTING_NAME, TEST_USER);

    assertEquals(List.of(PERMISSIONS_1), lastUpdatedContextApplication().getPermissions());
  }

  /**
   * Fails the mutants taking from the body the owner, the audience, the order,
   * the illustration file id, the id or a field the form does not offer
   */
  @Test
  void updateContextApplicationKeepsTheStoredFieldsAndForcesTheOnesTheFormDoesNotOffer() throws Exception {
    mockContext(SPACE_ID);
    Application stored = contextApplication(ID, SETTING_NAME);
    stored.setOrder(7L);
    stored.setChangedManually(true);
    when(appCenterStorage.getApplication(ID)).thenReturn(stored);
    ApplicationForm form = contextForm();
    form.setId(ID + 1);
    form.setMandatory(true);
    form.setDefault(true);
    form.setPwa(true);
    form.setSystem(true);
    form.setPersonal(true);
    form.setAllowStick(true);
    form.setShortcut(SHORTCUT);
    form.setActive(false);
    form.setImageFileId(IMAGE_FILE_ID + 1);
    form.setOrder(3L);
    form.setChangedManually(false);
    form.setContextSettingName(OTHER_SETTING_NAME);
    form.setContextSpaceId(SPACE_ID + 1);
    form.setPermissions(List.of("*:/platform/users"));

    applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER);

    Application updated = lastUpdatedContextApplication();
    assertEquals(ID, updated.getId());
    assertFalse(updated.isMandatory());
    assertFalse(updated.isDefault());
    assertFalse(updated.isPwa());
    assertFalse(updated.isSystem());
    assertFalse(updated.isPersonal());
    assertFalse(updated.isAllowStick());
    assertNull(updated.getShortcut());
    assertTrue(updated.isActive());
    assertEquals(IMAGE_FILE_ID, updated.getImageFileId());
    assertEquals(7L, updated.getOrder());
    assertTrue(updated.isChangedManually());
    assertEquals(SETTING_NAME, updated.getContextSettingName());
    assertEquals(SPACE_ID, updated.getContextSpaceId());
    assertEquals(List.of(PERMISSIONS_2), updated.getPermissions());
    assertEquals(TITLE, updated.getTitle());
    assertEquals(DESCRIPTION, updated.getDescription());
    assertEquals("https://meeds.io", updated.getUrl());
    assertEquals("fa-star", updated.getIcon());
  }

  /**
   * The listing gives the form a resolved badge, which the form of an editor
   * who may not read the badge providers sends back: fails the mutant storing
   * it, which turns on again a badge an administrator turned off, or freezes
   * an url-derived binding on its drawer; and the mutant ignoring the badge an
   * administrator sets
   */
  @Test
  void updateContextApplicationKeepsTheStoredBadgeUnlessAnAdministratorUpdatesIt() throws Exception {
    mockContext(SPACE_ID);
    when(badgePluginRegistry.getPlugin("agenda")).thenReturn(mock(ApplicationBadgePlugin.class));
    Application turnedOff = contextApplication(ID, SETTING_NAME);
    turnedOff.setBadgeName(ApplicationBadgePluginRegistry.BADGE_DISABLED);
    when(appCenterStorage.getApplication(ID)).thenReturn(turnedOff);
    assertEquals(ApplicationBadgePluginRegistry.BADGE_DISABLED,
                 updatedContextApplication(form -> form.setBadgeName(null)).getBadgeName());

    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    assertNull(updatedContextApplication(form -> form.setBadgeName("agenda")).getBadgeName());

    // A new type drops the binding, which follows the url
    Application boundLink = contextApplication(ID, SETTING_NAME);
    boundLink.setBadgeName("agenda");
    when(appCenterStorage.getApplication(ID)).thenReturn(boundLink);
    assertEquals("agenda", updatedContextApplication(form -> form.setBadgeName(null)).getBadgeName());
    assertNull(updatedContextApplication(form -> {
      form.setType(ApplicationType.DRAWER);
      form.setUrl("agendaDrawer");
      form.setBadgeName(null);
    }).getBadgeName());

    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, ADMIN_USERNAME)).thenReturn(true);
    ApplicationForm form = contextForm();
    form.setBadgeName("agenda");
    applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, ADMIN_USERNAME);
    assertEquals("agenda", lastUpdatedContextApplication().getBadgeName());
  }

  /**
   * Fails the mutant keeping, while the form reads a turned off or url-derived
   * badge as the resolved one the listing gives, the stored binding in the
   * created or updated application the form edits next
   */
  @Test
  void createAndUpdateContextApplicationReturnTheResolvedBadge() throws Exception {
    mockContext(SPACE_ID);
    when(badgePluginRegistry.resolveBadgeName(any())).thenReturn("resolved");
    Application stored = contextApplication(ID, SETTING_NAME);
    stored.setBadgeName(ApplicationBadgePluginRegistry.BADGE_DISABLED);
    when(appCenterStorage.getApplication(ID)).thenReturn(stored);

    assertEquals("resolved", applicationCenterService.createContextApplication(contextForm(), SETTING_NAME, TEST_USER).getBadgeName());
    assertEquals("resolved",
                 applicationCenterService.updateContextApplication(ID, contextForm(), SETTING_NAME, TEST_USER).getBadgeName());
    assertEquals(ApplicationBadgePluginRegistry.BADGE_DISABLED, stored.getBadgeName());
  }

  /**
   * Fails the mutant storing the body as sent: the update is a write path of
   * an application url, refused unless normalized as on creation
   */
  @Test
  void updateContextApplicationValidatesTheFieldsAsOnCreation() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));

    assertContextUpdateRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> form.setUrl("javascript:alert(1)"));
    assertContextUpdateRefused(ApplicationCenterService.INVALID_URL_MESSAGE, form -> form.setUrl("//evil.example"));
    assertContextUpdateRefused(ApplicationCenterService.TITLE_MANDATORY_MESSAGE, form -> form.setTitle(" "));
    assertContextUpdateRefused(ApplicationCenterService.INVALID_ICON_MESSAGE, form -> form.setIcon("fa-star\"><script>"));
    assertContextUpdateRefused(ApplicationCenterService.TOO_MANY_CATEGORIES_MESSAGE,
                               form -> form.setCategoryIds(LongStream.rangeClosed(1, 101).boxed().toList()));
    verifyNoContextApplicationUpdated();
  }

  @Test
  void updateContextApplicationReplacesTheIllustrationWithAnUploadOfTheTypeOfItsContent() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    byte[] png = png(ApplicationCenterService.MAX_ILLUSTRATION_SIZE);
    when(appCenterStorage.readUpload(UPLOAD_ID, ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(png);
    when(appCenterStorage.saveIllustration(png, "image/png")).thenReturn(IMAGE_FILE_ID + 1);
    ApplicationForm form = contextForm();
    form.setImageUploadId(UPLOAD_ID);
    form.setIllustrationMimeType("image/svg+xml");
    form.setIllustrationRemoved(true);

    applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER);

    assertEquals(IMAGE_FILE_ID + 1, lastUpdatedContextApplication().getImageFileId());
    InOrder inOrder = inOrder(appCenterStorage);
    inOrder.verify(appCenterStorage).saveIllustration(png, "image/png");
    inOrder.verify(appCenterStorage).updateContextApplication(any());
    inOrder.verify(appCenterStorage).removeUpload(UPLOAD_ID);
  }

  @Test
  void updateContextApplicationRefusesAnInvalidIllustration() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    when(appCenterStorage.readUpload("unknown", ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(null);
    when(appCenterStorage.readUpload("tooLarge", ApplicationCenterService.MAX_ILLUSTRATION_SIZE))
                                                                                                  .thenReturn(png(ApplicationCenterService.MAX_ILLUSTRATION_SIZE
                                                                                                      + 1));
    when(appCenterStorage.readUpload("svg", ApplicationCenterService.MAX_ILLUSTRATION_SIZE))
                                                                                             .thenReturn("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

    assertContextUpdateRefused(ApplicationCenterService.INVALID_UPLOAD_MESSAGE, form -> form.setImageUploadId("unknown"));
    assertContextUpdateRefused(ApplicationCenterService.TOO_LARGE_MESSAGE, form -> form.setImageUploadId("tooLarge"));
    assertContextUpdateRefused(ApplicationCenterService.INVALID_TYPE_MESSAGE, form -> {
      form.setImageUploadId("svg");
      form.setIllustrationMimeType("image/png");
    });
    verifyNoContextApplicationUpdated();
    verify(appCenterStorage, never()).removeUpload(any());
  }

  /**
   * Fails the mutants removing the stored illustration of a body that does
   * not state it, as one built from scratch, and keeping it on request
   */
  @Test
  void updateContextApplicationRemovesTheIllustrationOnTheFormsRequestOnly() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));

    assertEquals(IMAGE_FILE_ID, updatedContextApplication(form -> form.setImageFileId(null)).getImageFileId());
    assertNull(updatedContextApplication(form -> form.setIllustrationRemoved(true)).getImageFileId());
    verify(appCenterStorage, never()).saveIllustration(any(), any());
  }

  /**
   * Fails the mutants linking or unlinking without the user, keeping a
   * category the form removed, unlinking one it keeps, and changing the
   * categories of a body that does not state them
   */
  @Test
  void updateContextApplicationMakesTheCategoriesTheFormsOnes() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    CategoryObject object = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    when(categoryLinkService.getLinkedIds(object)).thenReturn(List.of(10L, 20L));

    Application updated = applicationCenterService.updateContextApplication(ID,
                                                                            contextFormWith(Arrays.asList(20L, 30L, null, 30L)),
                                                                            SETTING_NAME,
                                                                            TEST_USER);

    assertEquals(List.of(20L, 30L), updated.getCategoryIds());
    verify(categoryLinkService).unlink(10L, object, TEST_USER);
    verify(categoryLinkService).link(30L, object, TEST_USER);
    verify(categoryLinkService, times(1)).unlink(anyLong(), any(), any());
    verify(categoryLinkService, times(1)).link(anyLong(), any(), any());
    verify(categoryLinkService, never()).unlink(anyLong(), any());

    assertEquals(List.of(10L, 20L),
                 applicationCenterService.updateContextApplication(ID, contextForm(), SETTING_NAME, TEST_USER).getCategoryIds());
    verify(categoryLinkService, times(1)).unlink(anyLong(), any(), any());
    verify(categoryLinkService, times(1)).link(anyLong(), any(), any());
  }

  /**
   * Fails the mutants returning the form's categories when one fails to link
   * or unlink, and failing an update done because of a category
   */
  @Test
  void updateContextApplicationReturnsTheCategoriesTheApplicationHas() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    CategoryObject object = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    when(categoryLinkService.getLinkedIds(object)).thenReturn(List.of(10L, 20L));
    doThrow(new IllegalAccessException("denied")).when(categoryLinkService).unlink(10L, object, TEST_USER);
    doThrow(new IllegalStateException("database unreachable")).when(categoryLinkService).link(30L, object, TEST_USER);

    assertEquals(List.of(10L, 40L),
                 applicationCenterService.updateContextApplication(ID, contextFormWith(List.of(30L, 40L)), SETTING_NAME, TEST_USER)
                                         .getCategoryIds());
    verify(categoryLinkService).unlink(20L, object, TEST_USER);

    when(categoryLinkService.getLinkedIds(object)).thenThrow(new IllegalStateException("database unreachable"));
    assertNull(applicationCenterService.updateContextApplication(ID, contextFormWith(List.of(50L)), SETTING_NAME, TEST_USER)
                                       .getCategoryIds());
    verify(categoryLinkService, never()).link(eq(50L), any(), any());
  }

  /**
   * Fails the mutants changing the categories, or consuming the upload, of an
   * application the storage refuses to update
   */
  @Test
  void updateContextApplicationKeepsTheCategoriesAndUploadWhenTheApplicationIsNotUpdated() throws Exception {
    mockContext(SPACE_ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    byte[] png = png(100);
    when(appCenterStorage.readUpload(UPLOAD_ID, ApplicationCenterService.MAX_ILLUSTRATION_SIZE)).thenReturn(png);
    when(appCenterStorage.saveIllustration(png, "image/png")).thenReturn(IMAGE_FILE_ID + 1);
    doThrow(new IllegalStateException("update refused")).when(appCenterStorage).updateContextApplication(any());
    ApplicationForm form = contextFormWith(List.of(30L));
    form.setImageUploadId(UPLOAD_ID);

    assertThrows(IllegalStateException.class,
                 () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER));
    verify(appCenterStorage, never()).removeUpload(any());
    verify(categoryLinkService, never()).link(anyLong(), any(), any());
    verify(categoryLinkService, never()).unlink(anyLong(), any(), any());
  }

  /**
   * The window's form edits the categories of what the window created: fails
   * the mutants reading them per application, reading those of the
   * Application Center's applications, and failing the listing when the read
   * fails
   */
  @Test
  void getContextApplicationsGivesTheWindowsOwnApplicationsTheirCategories() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    when(appCenterStorage.getApplication(1L)).thenReturn(accessibleApplication(1L));
    when(appCenterStorage.getApplication(2L)).thenReturn(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, SETTING_NAME));
    when(categoryLinkService.getLinkedIds(ApplicationCategoryPlugin.OBJECT_TYPE, List.of("2", "3"))).thenReturn(Map.of("2", List.of(10L)));
    ApplicationListFilter filter = new ApplicationListFilter(SETTING_NAME, List.of(1L, 2L, 3L));

    List<Application> applications = applicationCenterService.getContextApplications(filter, null, TEST_USER);

    assertNull(applications.get(0).getCategoryIds());
    assertEquals(List.of(10L), applications.get(1).getCategoryIds());
    assertEquals(List.of(), applications.get(2).getCategoryIds());
    verify(categoryLinkService, never()).getLinkedIds(any(CategoryObject.class));
    assertNull(appCenterStorage.getApplication(2L).getCategoryIds());

    when(categoryLinkService.getLinkedIds(ApplicationCategoryPlugin.OBJECT_TYPE,
                                          List.of("2", "3"))).thenThrow(new IllegalStateException("database unreachable"));
    applications = applicationCenterService.getContextApplications(filter, null, TEST_USER);
    assertEquals(List.of(1L, 2L, 3L), ids(applications));
    assertNull(applications.get(1).getCategoryIds());
  }

  /**
   * The storage returns an application without its categories, as its DTO
   * mapping does: fails the mutant unlinking the categories the application
   * carries instead of the ones linked to it
   */
  @Test
  void deleteApplicationUnlinksTheCategoriesLinkedToTheApplication() throws Exception {
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    when(categoryLinkService.getLinkedIds(categoryObject)).thenReturn(List.of(10L, 20L));
    when(appCenterStorage.getApplication(ID)).thenReturn(application(ID));

    applicationCenterService.deleteApplication(ID);

    InOrder inOrder = inOrder(categoryLinkService, appCenterStorage);
    inOrder.verify(appCenterStorage).deleteApplication(ID);
    inOrder.verify(categoryLinkService).unlink(10L, categoryObject);
    inOrder.verify(categoryLinkService).unlink(20L, categoryObject);
  }

  /**
   * Fails the mutants letting a failing category read or unlink end the
   * delete: the deleted application's illustration would stay, its other
   * links too, and the delete would be reported as failed
   */
  @Test
  void deleteApplicationDeletesAContextApplicationWhoseCategoriesFailToUnlink() throws Exception {
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    when(categoryLinkService.getLinkedIds(categoryObject)).thenReturn(List.of(10L, 20L));
    doThrow(new IllegalStateException("database unreachable")).when(categoryLinkService).unlink(10L, categoryObject);
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));

    assertDoesNotThrow(() -> applicationCenterService.deleteApplication(ID));
    verify(categoryLinkService).unlink(20L, categoryObject);
    verify(appCenterStorage).deleteIllustration(IMAGE_FILE_ID);

    when(categoryLinkService.getLinkedIds(categoryObject)).thenThrow(new IllegalStateException("database unreachable"));
    assertDoesNotThrow(() -> applicationCenterService.deleteApplication(ID));
    verify(appCenterStorage, times(2)).deleteIllustration(IMAGE_FILE_ID);
  }

  /**
   * Fails the mutants unlinking the categories, or deleting the illustration,
   * before or whatever the delete of the application: a refused delete would
   * leave the application without them
   */
  @Test
  void deleteApplicationKeepsTheCategoriesAndIllustrationOfAContextApplicationItFailsToDelete() throws Exception {
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(ID), 0L);
    lenient().when(categoryLinkService.getLinkedIds(categoryObject)).thenReturn(List.of(10L));
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    doThrow(new IllegalStateException("delete refused")).when(appCenterStorage).deleteApplication(ID);

    assertThrows(IllegalStateException.class, () -> applicationCenterService.deleteApplication(ID));
    verify(categoryLinkService, never()).unlink(anyLong(), any(CategoryObject.class));
    verify(appCenterStorage, never()).deleteIllustration(anyLong());
  }

  @Test
  void deleteApplicationKeepsTheIllustrationOfAnApplicationOfTheApplicationCenter() throws Exception {
    when(appCenterStorage.getApplication(ID)).thenReturn(application(ID));

    applicationCenterService.deleteApplication(ID);

    verify(appCenterStorage).deleteApplication(ID);
    verify(appCenterStorage, never()).deleteIllustration(anyLong());
  }

  @Test
  void deleteSpaceApplicationsDeletesTheApplicationsOfThatSpaceContext() throws Exception {
    when(appCenterStorage.getContextApplicationIds(SPACE_ID)).thenReturn(List.of(1L, 2L, 3L));
    when(appCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, SETTING_NAME));
    when(appCenterStorage.getApplication(2L)).thenReturn(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, OTHER_SETTING_NAME));
    doThrow(new IllegalStateException("database unreachable")).when(appCenterStorage).deleteApplication(2L);

    applicationCenterService.deleteSpaceApplications(SPACE_ID);

    verify(appCenterStorage).deleteApplication(1L);
    verify(appCenterStorage).deleteApplication(3L);
    verify(myApplicationsSettingsService, never()).getSetting(any());
  }

  @Test
  void deleteSpaceApplicationsIgnoresThePlatformContext() {
    applicationCenterService.deleteSpaceApplications(0);
    applicationCenterService.deleteSpaceApplications(-1);
    verify(appCenterStorage, never()).getContextApplicationIds(anyLong());
  }

  @Test
  void canEditContextApplication() {
    Application owned = contextApplication(ID, SETTING_NAME);
    Application catalog = accessibleApplication(ID);
    assertTrue(applicationCenterService.canEditContextApplication(catalog, ADMIN_USERNAME));
    assertTrue(applicationCenterService.canEditContextApplication(owned, ADMIN_USERNAME));
    assertFalse(applicationCenterService.canEditContextApplication(owned, TEST_USER));

    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    assertTrue(applicationCenterService.canEditContextApplication(owned, TEST_USER));
    // Whoever manages a window's applications edits those it created only
    assertFalse(applicationCenterService.canEditContextApplication(catalog, TEST_USER));
    assertFalse(applicationCenterService.canEditContextApplication(contextApplication(ID, OTHER_SETTING_NAME), TEST_USER));
    assertFalse(applicationCenterService.canEditContextApplication(owned, null));
    assertFalse(applicationCenterService.canEditContextApplication(owned, IdentityConstants.ANONIM));

    // An administrator emptied its audience: administrators only, as it is
    // shown to them only
    owned.setPermissions(List.of());
    assertFalse(applicationCenterService.canEditContextApplication(owned, TEST_USER));
    assertTrue(applicationCenterService.canEditContextApplication(owned, ADMIN_USERNAME));
  }

  @Test
  void canAccessAContextApplicationWithoutAudienceIsForAdministratorsOnly() {
    Application owned = contextApplication(ID, SETTING_NAME);
    owned.setPermissions(Collections.emptyList());
    lenient().when(userAcl.hasPermission(any(Identity.class),
                                         eq(ApplicationCenterService.DEFAULT_USERS_PERMISSION))).thenReturn(true);
    assertTrue(applicationCenterService.canAccess(owned, ADMIN_USERNAME));
    assertFalse(applicationCenterService.canAccess(owned, TEST_USER));
    assertFalse(applicationCenterService.canAccess(owned, null));
    owned.setPermissions(null);
    assertFalse(applicationCenterService.canAccess(owned, TEST_USER));

    // Any other application without permissions is for every platform user
    Application catalog = application(ID);
    catalog.setPermissions(Collections.emptyList());
    assertTrue(applicationCenterService.canAccess(catalog, TEST_USER));
  }

  @Test
  void usersApplicationCenterNeverListsAContextApplication() {
    Application catalog = accessibleApplication(1L);
    Application owned = contextApplication(2L, SETTING_NAME);
    owned.setShortcut("K");
    when(appCenterStorage.getApplications(null)).thenReturn(List.of(catalog, owned));

    assertEquals(List.of(1L), ids(applicationCenterService.getActiveApplications(0, 0, null, TEST_USER).getApplications()));
    assertEquals(List.of(SHORTCUT), applicationCenterService.getApplicationShortcuts(TEST_USER));
    // The administrators' list shows them
    assertEquals(List.of(1L, 2L), ids(applicationCenterService.getApplications(0, 0, null).getApplications()));
  }

  @Test
  void favoritesNeverListAContextApplication() {
    UserApplication catalog = new UserApplication(accessibleApplication(1L));
    UserApplication owned = new UserApplication(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getMandatoryAndFavoriteApplications(TEST_USER, null)).thenReturn(List.of(catalog, owned));

    assertEquals(List.of(1L),
                 ids(applicationCenterService.getMandatoryAndFavoriteApplications(null, TEST_USER, null).getApplications()));
  }

  @Test
  void addFavoriteApplicationRefusesAContextApplication() throws Exception {
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));

    assertEquals(ApplicationCenterService.NOT_FAVORITABLE_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.addFavoriteApplication(ID, TEST_USER)).getMessage());
    verify(appCenterStorage, never()).addApplicationToUserFavorite(anyLong(), any());
  }

  @Test
  void updateFavoriteApplicationsOrderChecksEveryApplicationBeforeWritingAnyOrder() throws Exception {
    when(appCenterStorage.getApplication(1L)).thenReturn(accessibleApplication(1L));
    when(appCenterStorage.getApplication(2L)).thenReturn(application(2L));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, SETTING_NAME));

    List<ApplicationOrder> inaccessible = List.of(new ApplicationOrder(1L, 0L), new ApplicationOrder(2L, 1L));
    assertThrows(IllegalAccessException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(inaccessible, TEST_USER));
    List<ApplicationOrder> contextual = List.of(new ApplicationOrder(1L, 0L), new ApplicationOrder(3L, 1L));
    assertEquals(ApplicationCenterService.NOT_FAVORITABLE_MESSAGE,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.updateFavoriteApplicationsOrder(contextual, TEST_USER))
                                                                                                                    .getMessage());
    List<ApplicationOrder> unknown = List.of(new ApplicationOrder(1L, 0L), new ApplicationOrder(4L, 1L));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterService.updateFavoriteApplicationsOrder(unknown, TEST_USER));
    verify(appCenterStorage, never()).addApplicationToUserFavorite(anyLong(), any());
    verify(appCenterStorage, never()).updateFavoriteApplicationOrder(anyLong(), any(), any());

    applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(1L, 0L)), TEST_USER);
    verify(appCenterStorage).updateFavoriteApplicationOrder(1L, TEST_USER, 0L);
  }

  @Test
  void stickApplicationRefusesAContextApplication() {
    Application owned = contextApplication(ID, SETTING_NAME);
    owned.setAllowStick(true);
    when(appCenterStorage.getApplication(ID)).thenReturn(owned);

    assertEquals("appCenter.placement.stickNotAllowed",
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.stickApplication(ID, PlacementSide.LEFT, TEST_USER)).getMessage());
    verify(placementStorage, never()).setPlacedApplicationId(any(), any(), anyLong());
  }

  @Test
  void getApplicationPlacementsDropsAStuckContextApplication() {
    Application owned = contextApplication(ID, SETTING_NAME);
    owned.setAllowStick(true);
    when(placementStorage.getPlacedApplicationId(TEST_USER, PlacementSide.LEFT)).thenReturn(ID);
    when(appCenterStorage.getApplication(ID)).thenReturn(owned);

    assertNull(applicationCenterService.getApplicationPlacements(TEST_USER, "dw").getLeft());
    verify(placementStorage).removePlacedApplicationId(TEST_USER, PlacementSide.LEFT);
  }

  @Test
  void updateApplicationRestoresTheForcedFieldsOfAStoredContextApplication() throws Exception {
    when(appCenterStorage.getApplication(ID)).thenReturn(contextApplication(ID, SETTING_NAME));
    Application update = application(ID);
    update.setDefault(true);
    update.setPersonal(true);
    update.setAllowStick(true);
    update.setActive(false);
    update.setPermissions(List.of(PERMISSIONS_2));

    applicationCenterService.updateApplication(update, ADMIN_USERNAME);

    verify(appCenterStorage).updateApplication(argThat(application -> !application.isMandatory()
                                                                      && !application.isDefault()
                                                                      && !application.isPwa()
                                                                      && !application.isSystem()
                                                                      && !application.isPersonal()
                                                                      && !application.isAllowStick()
                                                                      && application.getShortcut() == null
                                                                      && !application.isActive()
                                                                      && List.of(PERMISSIONS_2)
                                                                             .equals(application.getPermissions())));
  }

  @Test
  void updateApplicationKeepsTheFieldsAnAdministratorSetsOnAStoredCatalogApplication() throws Exception {
    when(appCenterStorage.getApplication(ID)).thenReturn(application(ID));
    Application update = application(ID);
    update.setAllowStick(true);
    update.setContextSettingName(SETTING_NAME);

    applicationCenterService.updateApplication(update, ADMIN_USERNAME);

    verify(appCenterStorage).updateApplication(argThat(application -> application.isMandatory()
                                                                      && application.isPwa()
                                                                      && application.isAllowStick()
                                                                      && SHORTCUT.equals(application.getShortcut())));
  }

  @Test
  void getListedApplicationsListsAContextApplicationForItsWindowOnly() throws Exception {
    when(myApplicationsSettingsService.hasSetting(OTHER_SETTING_NAME)).thenReturn(true);
    when(appCenterStorage.getApplication(1L)).thenReturn(accessibleApplication(1L));
    when(appCenterStorage.getApplication(2L)).thenReturn(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, OTHER_SETTING_NAME));

    assertEquals(List.of(1L, 2L),
                 ids(applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME, List.of(1L, 2L, 3L)),
                                                                    null,
                                                                    TEST_USER)));
    assertEquals(List.of(1L, 3L),
                 ids(applicationCenterService.getListedApplications(new ApplicationListFilter(OTHER_SETTING_NAME,
                                                                                              List.of(1L, 2L, 3L)),
                                                                    null,
                                                                    TEST_USER)));
  }

  @Test
  void getListedApplicationsByCategoryListsAContextApplicationForItsWindowOnly() throws Exception {
    titled(1L, "alpha");
    titled(2L, "bravo").setContextSettingName(SETTING_NAME);
    titled(3L, "charlie").setContextSettingName(OTHER_SETTING_NAME);
    when(applicationCategoryStorage.getApplicationIds(10L)).thenReturn(List.of(3L, 2L, 1L));

    assertEquals(List.of(1L, 2L), listedIds(List.of(10L), Locale.ENGLISH));
  }

  @Test
  void getContextApplicationsListsAContextApplicationForItsWindowOnly() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    when(appCenterStorage.getApplication(2L)).thenReturn(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, OTHER_SETTING_NAME));

    List<Application> applications = applicationCenterService.getContextApplications(new ApplicationListFilter(SETTING_NAME,
                                                                                                               List.of(3L, 2L)),
                                                                                     null,
                                                                                     TEST_USER);
    assertEquals(List.of(2L), ids(applications));
    assertTrue(applications.get(0).isContextual());
  }

  /**
   * Fails the mutants offering another window's applications, and hiding the
   * window's own: one created then left out of the list, by a failed save or a
   * closed drawer, could never be selected again, even by an editor outside
   * its audience
   */
  @Test
  void getContextSuggestionsOffersTheWindowsOwnApplicationsOnly() throws Exception {
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    when(appCenterStorage.getApplicationIds(null)).thenReturn(List.of(1L, 2L, 3L, 4L));
    when(appCenterStorage.getApplication(1L)).thenReturn(accessibleApplication(1L));
    when(appCenterStorage.getApplication(2L)).thenReturn(contextApplication(2L, SETTING_NAME));
    when(appCenterStorage.getApplication(3L)).thenReturn(contextApplication(3L, OTHER_SETTING_NAME));
    Application outsideAudience = contextApplication(4L, SETTING_NAME);
    outsideAudience.setPermissions(List.of(PERMISSIONS_1));
    when(appCenterStorage.getApplication(4L)).thenReturn(outsideAudience);

    assertEquals(List.of(1L, 2L, 4L),
                 ids(applicationCenterService.getContextSuggestions(SETTING_NAME, null, null, 0, 10, null, TEST_USER)));
  }

  private void mockContext(long spaceId) {
    lenient().when(myApplicationsSettingsService.getSetting(SETTING_NAME))
             .thenReturn(new CMSSetting(MyApplicationsSettingsService.SETTING_TYPE, SETTING_NAME, PAGE_REFERENCE, spaceId));
    lenient().when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, TEST_USER)).thenReturn(true);
    lenient().when(myApplicationsSettingsService.getContextSpaceId(SETTING_NAME, PAGE_REFERENCE)).thenReturn(spaceId);
    Space space = new Space();
    space.setGroupId(SPACE_GROUP);
    lenient().when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    lenient().when(appCenterStorage.createContextApplication(any(), eq(SETTING_NAME), anyLong())).thenAnswer(invocation -> {
      Application created = new Application(invocation.getArgument(0, Application.class));
      created.setId(CREATED_ID);
      created.setContextSettingName(invocation.getArgument(1));
      created.setContextSpaceId(invocation.getArgument(2));
      return created;
    });
  }

  private ApplicationForm contextForm() {
    ApplicationForm form = new ApplicationForm();
    form.setTitle(TITLE);
    form.setDescription(DESCRIPTION);
    form.setType(ApplicationType.LINK);
    form.setUrl("https://meeds.io");
    form.setSameTab(true);
    form.setMobile(true);
    form.setIcon("fa-star");
    return form;
  }

  private Application createdContextApplication(Consumer<ApplicationForm> formChange) throws Exception {
    ApplicationForm form = contextForm();
    formChange.accept(form);
    applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER);
    return lastCreatedContextApplication();
  }

  private void assertContextApplicationRefused(String messageCode, Consumer<ApplicationForm> formChange) {
    ApplicationForm form = contextForm();
    formChange.accept(form);
    assertEquals(messageCode,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER)).getMessage());
  }

  private void assertIllustrationRefused(String uploadId, String messageCode) {
    ApplicationForm form = contextForm();
    form.setImageUploadId(uploadId);
    form.setIllustrationMimeType("image/png");
    assertEquals(messageCode,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.createContextApplication(form, SETTING_NAME, TEST_USER))
                                                                                                                     .getMessage());
  }

  private Application lastCreatedContextApplication() {
    ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
    verify(appCenterStorage, atLeastOnce()).createContextApplication(captor.capture(), eq(SETTING_NAME), anyLong());
    return captor.getValue();
  }

  private ApplicationForm contextFormWith(List<Long> categoryIds) {
    ApplicationForm form = contextForm();
    form.setCategoryIds(categoryIds);
    return form;
  }

  private Application updatedContextApplication(Consumer<ApplicationForm> formChange) throws Exception {
    ApplicationForm form = contextForm();
    formChange.accept(form);
    applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER);
    return lastUpdatedContextApplication();
  }

  private void assertContextUpdateRefused(String messageCode, Consumer<ApplicationForm> formChange) {
    ApplicationForm form = contextForm();
    formChange.accept(form);
    assertEquals(messageCode,
                 assertThrows(IllegalArgumentException.class,
                              () -> applicationCenterService.updateContextApplication(ID, form, SETTING_NAME, TEST_USER))
                                                                                                                         .getMessage());
  }

  private Application lastUpdatedContextApplication() {
    ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
    verify(appCenterStorage, atLeastOnce()).updateContextApplication(captor.capture());
    return captor.getValue();
  }

  private void verifyNoContextApplicationUpdated() throws Exception {
    verify(appCenterStorage, never()).updateContextApplication(any());
    verify(appCenterStorage, never()).saveIllustration(any(), any());
    verify(categoryLinkService, never()).link(anyLong(), any(), any());
    verify(categoryLinkService, never()).unlink(anyLong(), any(), any());
  }

  private void verifyNoContextApplicationCreated() throws Exception {
    verify(appCenterStorage, never()).createContextApplication(any(), any(), anyLong());
    verify(appCenterStorage, never()).saveIllustration(any(), any());
    verify(categoryLinkService, never()).link(anyLong(), any(), any());
  }

  private Application contextApplication(Long id, String settingName) {
    Application application = accessibleApplication(id);
    application.setMandatory(false);
    application.setPwa(false);
    application.setContextSettingName(settingName);
    application.setContextSpaceId(SPACE_ID);
    return application;
  }

  private static byte[] png(int size) {
    byte[] content = new byte[size];
    System.arraycopy(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' }, 0, content, 0, 8);
    return content;
  }

  private static List<Long> ids(List<Application> applications) {
    return applications.stream().map(Application::getId).toList();
  }

  private List<Long> listedIds(List<Long> categoryIds, Locale locale) throws ObjectNotFoundException {
    return applicationCenterService.getListedApplications(new ApplicationListFilter(SETTING_NAME, null, categoryIds),
                                                          locale,
                                                          TEST_USER)
                                   .stream()
                                   .map(Application::getId)
                                   .toList();
  }

  private static ResourceBundle bundle(String... keyValues) {
    Object[][] contents = new Object[keyValues.length / 2][];
    for (int i = 0; i < contents.length; i++) {
      contents[i] = new Object[] { keyValues[2 * i], keyValues[2 * i + 1] };
    }
    return new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return contents;
      }
    };
  }

  private Application titled(Long id, String title) {
    Application application = accessibleApplication(id);
    application.setTitle(title);
    lenient().when(appCenterStorage.getApplication(id)).thenReturn(application);
    return application;
  }

  private Application accessibleApplication(Long id) {
    Application application = application(id);
    application.setPermissions(new ArrayList<>(List.of(PERMISSIONS_2)));
    return application;
  }

  private Application personalApp(String owner) {
    Application app = application();
    app.setPersonal(true);
    app.setMandatory(false);
    app.setPermissions(Collections.singletonList(owner));
    return app;
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
                           false,
                           false,
                           null,
                           false,
                           false,
                           null,
                           null);
  }

}
