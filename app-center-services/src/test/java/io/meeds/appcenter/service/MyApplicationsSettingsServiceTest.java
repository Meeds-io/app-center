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
package io.meeds.appcenter.service;

import static io.meeds.appcenter.service.MyApplicationsSettingsService.APPLICATION_IDS;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.CATEGORY_IDS;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.HEADER_TITLE_FIELD;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE_FAVORITES;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE_SELECTED;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.MAX_APPS_TO_LIST;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE_CATEGORY;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE_MANUAL;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SETTING_TYPE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHOW_HEADER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.social.category.model.Category;
import io.meeds.social.category.service.CategoryService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@SpringBootTest(classes = { MyApplicationsSettingsService.class })
@ExtendWith(MockitoExtension.class)
class MyApplicationsSettingsServiceTest {

  private static final Map<String, String> FAVORITES_ONLY = Map.of(LISTING_MODE, LISTING_MODE_FAVORITES);

  private static final String           USERNAME            = "testuser";

  private static final String           PAGE_REFERENCE      = "group::/spaces/space1::home";

  private static final String           PAGE_NAME           = "Untitled-12-1b4e28ba-2fa1-11d2-883f-0016d3cca427";

  private static final String           SHARED_NAME         = "Untitled-12-shared-1b4e28ba-2fa1-11d2-883f-0016d3cca427";

  private static final String           SECTION_DRAFT_NAME  = "Untitled-12-sdraft-7-1b4e28ba-2fa1-11d2-883f-0016d3cca427";

  private static final String           SITE_NAME           = "Untitled-12-site-1b4e28ba-2fa1-11d2-883f-0016d3cca427";

  private static final String           SECTION_DRAFT_PAGE  = "portal::global::section_draft_7_testuser";

  private static final String           LEGACY_ID           = "4528710563";

  @MockitoBean
  private CMSService                    cmsService;

  @MockitoBean
  private LayoutAclService              layoutAclService;

  @MockitoBean
  private TranslationService            translationService;

  @MockitoBean
  private UserACL                       userAcl;

  @MockitoBean
  private ApplicationCenterStorage      applicationCenterStorage;

  @MockitoBean
  private CategoryService               categoryService;

  @MockitoBean
  private SpaceService                  spaceService;

  @Autowired
  private MyApplicationsSettingsService settingsService;

  @BeforeEach
  void existingApplications() {
    lenient().when(applicationCenterStorage.getApplication(anyLong())).thenReturn(new Application());
    lenient().when(categoryService.getCategory(anyLong())).thenReturn(new Category());
  }

  @Test
  void canEditSettingsRefusesBlankNameAndGuest() {
    Identity identity = mockUser(true);
    when(cmsService.getSetting(eq(SETTING_TYPE), anyString())).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(null, USERNAME));
    assertFalse(settingsService.canEditSettings(" ", USERNAME));
    assertFalse(settingsService.canEditSettings(PAGE_NAME, null));
    assertFalse(settingsService.canEditSettings(PAGE_NAME, IdentityConstants.ANONIM));
    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    verify(cmsService).hasEditPermission(identity, PAGE_REFERENCE, 0);
  }

  @Test
  void canEditSettingsRefusesMissingSettingWhateverTheRole() {
    mockUser(true);
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    verify(cmsService, never()).hasEditPermission(any(Identity.class), anyString(), anyLong());
    verify(cmsService, never()).hasEditPermission(any(Identity.class), anyString(), anyString());
  }

  @Test
  void canEditSettingsRefusesUnknownUser() {
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));
    when(cmsService.hasEditPermission(any(), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(PAGE_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfPageWindowUsesPageContentRight() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 3));

    assertFalse(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 3)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
  }

  /**
   * MyApplicationsPortlet#generateRandomId marks the name of a shared-layout
   * window that has a storage id with -shared- (spec mechanism M2), which a
   * configured shared layout never gives: this pins that name's right.
   */
  @Test
  void canEditSettingsOfSharedLayoutWindowIsAdministratorOnly() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SHARED_NAME)).thenReturn(setting(SHARED_NAME, PAGE_REFERENCE, 0));
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SHARED_NAME, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(SHARED_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfSharedMarkerInSpaceUsesPageContentRight() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SHARED_NAME)).thenReturn(setting(SHARED_NAME, PAGE_REFERENCE, 3));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 3)).thenReturn(true);

    assertTrue(settingsService.canEditSettings(SHARED_NAME, USERNAME));
  }

  /**
   * MyApplicationsPortlet marks no section-template draft window with
   * -sdraft- nor records its untruncated section_draft_ reference (spec
   * mechanism M2, D15): this pins the right of such a name; the cut reference
   * CMSPortlet records is pinned by
   * canEditSettingsOfWindowRecordedOnSectionTemplateDraftIsAdministratorOnly.
   */
  @Test
  void canEditSettingsOfSectionTemplateDraftIsAdministratorOnly() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SECTION_DRAFT_NAME)).thenReturn(setting(SECTION_DRAFT_NAME, SECTION_DRAFT_PAGE, 0));
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SECTION_DRAFT_NAME, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(SECTION_DRAFT_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfSectionDraftMarkerOutsideDraftUsesPageContentRight() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SECTION_DRAFT_NAME)).thenReturn(setting(SECTION_DRAFT_NAME, PAGE_REFERENCE, 3));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 3)).thenReturn(true);

    assertTrue(settingsService.canEditSettings(SECTION_DRAFT_NAME, USERNAME));
  }

  /**
   * MyApplicationsPortlet#generateRandomId marks the name of a site-layout
   * window with -site- (spec mechanism M2, D13): this pins that name's right.
   */
  @Test
  void canEditSettingsOfSiteLayoutWindowUsesSiteEditRight() {
    mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SITE_NAME)).thenReturn(setting(SITE_NAME, PAGE_REFERENCE, 3));
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SITE_NAME, USERNAME));
    when(layoutAclService.canEditSite(SiteKey.group("/spaces/space1"), USERNAME)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(SITE_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfSiteLayoutWindowWithUnparsablePageIsRefused() {
    mockUser(true);
    when(cmsService.getSetting(SETTING_TYPE, SITE_NAME)).thenReturn(setting(SITE_NAME, "not-a-page", 0));
    when(layoutAclService.canEditSite(any(), anyString())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SITE_NAME, USERNAME));
  }

  @Test
  void canEditSettingsMatchesMarkersAsPrefixOnly() {
    Identity identity = mockUser(false);
    String name = "Untitled-12-1b4e28ba-Untitled-3-shared-2fa1";
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name, PAGE_REFERENCE, 0));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 0)).thenReturn(true);

    assertTrue(settingsService.canEditSettings(name, USERNAME));
    verify(userAcl, never()).isAdministrator(any());
  }

  @Test
  void canEditSettingsOfWindowRecordedOnSectionTemplateDraftIsAdministratorOnly() {
    Identity identity = mockUser(false);
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);
    // CMSPortlet records a section-template draft page cut at _draft_
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, "portal::global::section", 0));
    when(cmsService.getSetting(SETTING_TYPE, SITE_NAME)).thenReturn(setting(SITE_NAME, SECTION_DRAFT_PAGE, 0));

    assertFalse(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    assertFalse(settingsService.canEditSettings(SITE_NAME, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    assertTrue(settingsService.canEditSettings(SITE_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfSectionPageOutsideGlobalSiteUsesPageContentRight() {
    Identity identity = mockUser(false);
    String otherPortalName = "Untitled-12-other-portal";
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, "portal::classic::section", 0));
    when(cmsService.getSetting(SETTING_TYPE, otherPortalName)).thenReturn(setting(otherPortalName,
                                                                                   "group::global::section_draft_7_testuser",
                                                                                   0));
    when(cmsService.hasEditPermission(identity, "portal::classic::section", 0)).thenReturn(true);
    when(cmsService.hasEditPermission(identity, "group::global::section_draft_7_testuser", 0)).thenReturn(true);

    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    assertTrue(settingsService.canEditSettings(otherPortalName, USERNAME));
    verify(userAcl, never()).isAdministrator(any());
  }

  @Test
  void getSettingsToStoreRefusesBeforeFiltering() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));
    Map<String, String> parameters = Map.of(MAX_APPS_TO_LIST, "8", "name", "Untitled-1-forged");

    assertThrows(IllegalAccessException.class, () -> settingsService.getSettingsToStore(PAGE_NAME, USERNAME, parameters));
    assertThrows(IllegalAccessException.class, () -> settingsService.getSettingsToStore(null, USERNAME, parameters));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 0)).thenReturn(true);
    assertThrows(IllegalAccessException.class, () -> settingsService.getSettingsToStore(PAGE_NAME, null, parameters));
    assertEquals(Map.of(MAX_APPS_TO_LIST, "8"), assertDoesNotThrow(() -> settingsService.getSettingsToStore(PAGE_NAME,
                                                                                                         USERNAME,
                                                                                                         parameters)));
  }

  @Test
  void hasSettingChecksTheCmsSetting() {
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));

    assertTrue(settingsService.hasSetting(PAGE_NAME));
    assertFalse(settingsService.hasSetting("Untitled-404"));
    assertFalse(settingsService.hasSetting(null));
  }

  @Test
  void canEditLegacyHeaderTitleIsAdministratorOnly() {
    Identity identity = mockUser(false);

    assertFalse(settingsService.canEditLegacyHeaderTitle(null));
    assertFalse(settingsService.canEditLegacyHeaderTitle(IdentityConstants.ANONIM));
    assertFalse(settingsService.canEditLegacyHeaderTitle(USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditLegacyHeaderTitle(USERNAME));
  }

  @Test
  void getWritableSettingsKeepsOnlyOwnedValidPreferences() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(MAX_APPS_TO_LIST, " 12 ");
    parameters.put(SHOW_HEADER, "false");
    parameters.put("name", "Untitled-1-forged");
    parameters.put("applicationId", "15");
    parameters.put("data.init", "{}");
    parameters.put("canEdit", "true");
    parameters.put("settingName", "other");
    parameters.put("action", "save");

    assertEquals(Map.of(MAX_APPS_TO_LIST, "12", SHOW_HEADER, "false"), settingsService.getWritableSettings(PAGE_NAME, parameters));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, null));
  }

  @Test
  void getWritableSettingsValidatesMaxAppsToListRange() {
    assertEquals(Map.of(MAX_APPS_TO_LIST, "1"), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "1")));
    assertEquals(Map.of(MAX_APPS_TO_LIST, "100"), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "100")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "0")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "101")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "-1")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "1e2")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(MAX_APPS_TO_LIST, "99999999999")));
  }

  @Test
  void getWritableSettingsValidatesShowHeader() {
    assertEquals(Map.of(SHOW_HEADER, "true"), settingsService.getWritableSettings(PAGE_NAME, Map.of(SHOW_HEADER, "true")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(SHOW_HEADER, "yes")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(SHOW_HEADER, "<script>")));
  }

  @Test
  void getWritableSettingsKeepsFavoritesListingMode() {
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE, " FAVORITES ")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE, "favorites")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE, "MANUAL")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE, "")));
  }

  @Test
  void getWritableSettingsRefusesAnEmptySelectedListingWhole() {
    Map<String, String> parameters1 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, MAX_APPS_TO_LIST, "8");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, parameters1));
    Map<String, String> parameters2 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, parameters2));
    Map<String, String> parameters3 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "1,x");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, parameters3));
  }

  @Test
  void getWritableSettingsRefusesACategoryListingWithApplicationsOnly() {
    Map<String, String> parameters4 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, APPLICATION_IDS, "3");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, parameters4));
  }

  @Test
  void getWritableSettingsStoresASelectedManualListing() {
    assertEquals(Map.of(LISTING_MODE,
                        LISTING_MODE_SELECTED,
                        SELECTION_MODE,
                        SELECTION_MODE_MANUAL,
                        APPLICATION_IDS,
                        "5,3,9"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_MANUAL,
                                                            APPLICATION_IDS,
                                                            " 5, 3,9,3 ")));
  }

  @Test
  void getWritableSettingsRefusesASelectedListingWithoutItsSelectionMode() {
    Map<String, String> parameters5 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, APPLICATION_IDS, "4");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, parameters5));
  }

  @Test
  void getWritableSettingsIgnoresASelectionWrittenWithoutItsListingMode() {
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(APPLICATION_IDS, "")));
    assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(APPLICATION_IDS, "4", SELECTION_MODE, SELECTION_MODE_MANUAL)));
    assertEquals(Map.of(MAX_APPS_TO_LIST, "8"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(APPLICATION_IDS, "4", MAX_APPS_TO_LIST, "8")));
  }

  @Test
  void getWritableSettingsKeepsTheApplicationIdsInFavoritesMode() {
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, SELECTION_MODE, SELECTION_MODE_CATEGORY, APPLICATION_IDS, "5"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_FAVORITES,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            APPLICATION_IDS,
                                                            "5")));
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, APPLICATION_IDS, ""),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, APPLICATION_IDS, "")));
  }

  @Test
  void getWritableSettingsDropsInvalidSelectionValues() {
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(SELECTION_MODE, "manual")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, "1;2")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, "-1")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, "1,,2")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, "1234567890123456789")));
    String overCap = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS + 1L)
                               .mapToObj(String::valueOf)
                               .collect(Collectors.joining(","));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, overCap)));
    String atCapWithDuplicate = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS)
                                          .mapToObj(String::valueOf)
                                          .collect(Collectors.joining(","))
        + ",1";
    assertEquals(ApplicationCenterService.MAX_LISTED_APPLICATIONS,
                 settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, atCapWithDuplicate)).get(APPLICATION_IDS).split(",").length);
  }

  private static Map<String, String> withFavorites(String name, String value) {
    return Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, name, value);
  }

  @Test
  void getWritableSettingsDropsAVeryLongIdListWithoutOverflowing() {
    String veryLong = LongStream.rangeClosed(1, 200_000).mapToObj(String::valueOf).collect(Collectors.joining(","));

    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(APPLICATION_IDS, veryLong)));
    assertEquals(List.of(), MyApplicationsSettingsService.getApplicationIds(veryLong));
  }

  @Test
  void getWritableSettingsDropsTheIdsOfDeletedApplications() {
    when(applicationCenterStorage.getApplication(3L)).thenReturn(null);

    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "5,9"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_MANUAL,
                                                            APPLICATION_IDS,
                                                            "5,3,9")));
    Map<String, String> onlyDeleted = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "3");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, onlyDeleted));
  }

  @Test
  void getWritableSettingsDropsTheIdsOfApplicationsAnotherWindowCreated() {
    Application owned = new Application();
    owned.setContextSettingName(PAGE_NAME);
    Application other = new Application();
    other.setContextSettingName(SITE_NAME);
    when(applicationCenterStorage.getApplication(4L)).thenReturn(owned);
    when(applicationCenterStorage.getApplication(6L)).thenReturn(other);

    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "5,4"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                                       LISTING_MODE_SELECTED,
                                                                       SELECTION_MODE,
                                                                       SELECTION_MODE_MANUAL,
                                                                       APPLICATION_IDS,
                                                                       "5,6,4")));
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "5,6"),
                 settingsService.getWritableSettings(SITE_NAME, Map.of(LISTING_MODE,
                                                                       LISTING_MODE_SELECTED,
                                                                       SELECTION_MODE,
                                                                       SELECTION_MODE_MANUAL,
                                                                       APPLICATION_IDS,
                                                                       "5,6,4")));
  }

  @Test
  void getWritableSettingsStoresASelectedCategoryListing() {
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, CATEGORY_IDS, "7,4"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            CATEGORY_IDS,
                                                            " 7, 4,7")));
  }

  @Test
  void getWritableSettingsRefusesAnEmptySelectedCategoryListing() {
    Map<String, String> noCategory = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY);
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, noCategory));
    Map<String, String> emptyCategories = Map.of(LISTING_MODE,
                                                 LISTING_MODE_SELECTED,
                                                 SELECTION_MODE,
                                                 SELECTION_MODE_CATEGORY,
                                                 CATEGORY_IDS,
                                                 "",
                                                 APPLICATION_IDS,
                                                 "3");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, emptyCategories));
    Map<String, String> invalidCategories = Map.of(LISTING_MODE,
                                                   LISTING_MODE_SELECTED,
                                                   SELECTION_MODE,
                                                   SELECTION_MODE_CATEGORY,
                                                   CATEGORY_IDS,
                                                   "1,x");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, invalidCategories));
    // The categories of a MANUAL listing do not make it listable
    Map<String, String> manualWithCategories = Map.of(LISTING_MODE,
                                                      LISTING_MODE_SELECTED,
                                                      SELECTION_MODE,
                                                      SELECTION_MODE_MANUAL,
                                                      CATEGORY_IDS,
                                                      "7");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, manualWithCategories));
  }

  @Test
  void getWritableSettingsKeepsTheOtherModeList() {
    assertEquals(Map.of(LISTING_MODE,
                        LISTING_MODE_SELECTED,
                        SELECTION_MODE,
                        SELECTION_MODE_CATEGORY,
                        APPLICATION_IDS,
                        "5,9",
                        CATEGORY_IDS,
                        "7"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            APPLICATION_IDS,
                                                            "5,9",
                                                            CATEGORY_IDS,
                                                            "7")));
    assertEquals(Map.of(LISTING_MODE,
                        LISTING_MODE_SELECTED,
                        SELECTION_MODE,
                        SELECTION_MODE_MANUAL,
                        APPLICATION_IDS,
                        "5",
                        CATEGORY_IDS,
                        "7,8"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_MANUAL,
                                                            APPLICATION_IDS,
                                                            "5",
                                                            CATEGORY_IDS,
                                                            "7,8")));
  }

  @Test
  void getWritableSettingsDropsInvalidCategoryIds() {
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(CATEGORY_IDS, "1;2")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(CATEGORY_IDS, "-1")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(CATEGORY_IDS, "1,,2")));
    String overCap = IntStream.rangeClosed(1, 101).mapToObj(String::valueOf).collect(Collectors.joining(","));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(PAGE_NAME, withFavorites(CATEGORY_IDS, overCap)));
    String atCap = IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(Collectors.joining(","));
    assertEquals(atCap, settingsService.getWritableSettings(PAGE_NAME, withFavorites(CATEGORY_IDS, atCap)).get(CATEGORY_IDS));
  }

  @Test
  void getWritableSettingsDropsTheIdsOfDeletedCategories() {
    when(categoryService.getCategory(4l)).thenReturn(null);
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, CATEGORY_IDS, "7"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            CATEGORY_IDS,
                                                            "4,7")));
    Map<String, String> onlyDeleted = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, CATEGORY_IDS, "4");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, onlyDeleted));
  }

  @Test
  void getCategoryIdsReadsAValidStoredListOnly() {
    assertEquals(List.of(7l, 4l), MyApplicationsSettingsService.getCategoryIds("7,4,7"));
    assertEquals(List.of(), MyApplicationsSettingsService.getCategoryIds("7,x"));
    assertEquals(List.of(), MyApplicationsSettingsService.getCategoryIds(null));
    String overCap = IntStream.rangeClosed(1, 101).mapToObj(String::valueOf).collect(Collectors.joining(","));
    assertEquals(List.of(), MyApplicationsSettingsService.getCategoryIds(overCap));
  }

  @Test
  void getSelectionModeDefaultsToManual() {
    assertEquals(SELECTION_MODE_CATEGORY, MyApplicationsSettingsService.getSelectionMode(SELECTION_MODE_CATEGORY));
    assertEquals(SELECTION_MODE_MANUAL, MyApplicationsSettingsService.getSelectionMode(null));
    assertEquals(SELECTION_MODE_MANUAL, MyApplicationsSettingsService.getSelectionMode("category"));
  }

  @Test
  void getApplicationIdsReadsAValidStoredListOnly() {
    assertEquals(List.of(5L, 3L), MyApplicationsSettingsService.getApplicationIds("5,3,5"));
    assertEquals(List.of(), MyApplicationsSettingsService.getApplicationIds(null));
    assertEquals(List.of(), MyApplicationsSettingsService.getApplicationIds("5,</script>"));
  }

  @Test
  void getSettingsToStoreChecksTheRightBeforeTheListingMode() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));
    Map<String, String> parameters = Map.of(LISTING_MODE, LISTING_MODE_SELECTED);

    assertThrows(IllegalAccessException.class, () -> settingsService.getSettingsToStore(PAGE_NAME, USERNAME, parameters));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 0)).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> settingsService.getSettingsToStore(PAGE_NAME, USERNAME, parameters));
  }

  @Test
  void getListingModeDefaultsToFavorites() {
    assertEquals(LISTING_MODE_SELECTED, MyApplicationsSettingsService.getListingMode(LISTING_MODE_SELECTED));
    assertEquals(LISTING_MODE_FAVORITES, MyApplicationsSettingsService.getListingMode(LISTING_MODE_FAVORITES));
    assertEquals(LISTING_MODE_FAVORITES, MyApplicationsSettingsService.getListingMode(null));
    assertEquals(LISTING_MODE_FAVORITES, MyApplicationsSettingsService.getListingMode("selected"));
    assertEquals(LISTING_MODE_FAVORITES, MyApplicationsSettingsService.getListingMode("CATEGORY"));
  }

  @Test
  @SneakyThrows
  void copyHeaderTitleCopiesLegacyLabelsWhenNoneUnderSettingName() {
    Map<Locale, String> labels = Map.of(Locale.ENGLISH, "My shortcuts", Locale.FRENCH, "Mes raccourcis");
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(labels));

    settingsService.copyHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService).saveTranslationLabels(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD, labels, false);
  }

  @Test
  @SneakyThrows
  void copyHeaderTitleCopiesLegacyLabelsOverBlankLabels() {
    Map<Locale, String> labels = Map.of(Locale.ENGLISH, "My shortcuts");
    when(translationService.getTranslationField(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             " ")));
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(labels));

    settingsService.copyHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService).saveTranslationLabels(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD, labels, false);
  }

  @Test
  @SneakyThrows
  void copyHeaderTitleKeepsLabelsAlreadyUnderSettingName() {
    when(translationService.getTranslationField(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             "New")));
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             "Old")));

    settingsService.copyHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  @SneakyThrows
  void copyHeaderTitleWithoutLegacyLabelsSavesNothing() {
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenThrow(new ObjectNotFoundException(LEGACY_ID));

    settingsService.copyHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  void copyHeaderTitleWithBlankKeysDoesNothing() {
    settingsService.copyHeaderTitle(null, PAGE_NAME);
    settingsService.copyHeaderTitle(LEGACY_ID, " ");
    verifyNoInteractions(translationService);
  }

  @Test
  void generateSettingNameMarksTheKindOfWindow() {
    String pageName = settingsService.generateSettingName("12", WindowKind.PAGE);
    String siteName = settingsService.generateSettingName("12", WindowKind.SITE_LAYOUT);
    String sharedName = settingsService.generateSettingName("12", WindowKind.SHARED_LAYOUT);

    assertTrue(pageName.matches("^Untitled-12-[0-9a-f]{8}-[0-9a-f-]{27}$"), pageName);
    assertTrue(siteName.matches("^Untitled-12-site-[0-9a-f]{8}-[0-9a-f-]{27}$"), siteName);
    assertTrue(sharedName.matches("^Untitled-12-shared-[0-9a-f]{8}-[0-9a-f-]{27}$"), sharedName);
    assertNotEquals(pageName, settingsService.generateSettingName("12", WindowKind.PAGE));
    assertTrue(settingsService.keepsSettingName(pageName, "12", WindowKind.PAGE, PAGE_REFERENCE, 0, this::noScan));
    assertTrue(settingsService.keepsSettingName(siteName, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 0, this::noScan));
    assertTrue(settingsService.keepsSettingName(sharedName, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, 0, this::noScan));
  }

  @Test
  void getContextSpaceIdRecordsTheSpaceOwningTheRecordedSiteOfASiteLayoutWindow() {
    Space space = new Space();
    space.setId(7L);
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space);

    assertEquals(7L, settingsService.getContextSpaceId(SITE_NAME, PAGE_REFERENCE, 0L));
    assertEquals(0L, settingsService.getContextSpaceId(SITE_NAME, "portal::meeds::home", 0L));
    assertEquals(0L, settingsService.getContextSpaceId(SITE_NAME, "group::/spaces/deleted::home", 0L));
    assertEquals(0L, settingsService.getContextSpaceId(SHARED_NAME, PAGE_REFERENCE, 3L));
    assertEquals(3L, settingsService.getContextSpaceId(PAGE_NAME, PAGE_REFERENCE, 3L));
  }

  /**
   * The layout editor previews a space template page as a space of the
   * editor's choice, which the request then carries: fails the mutant
   * recording that space, where the template's apps would take its audience
   * and be deleted with it
   */
  @Test
  void getContextSpaceIdRecordsThePlatformContextForATemplateWindow() {
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "group_template::workspace::home", 7L));
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "portal_template::intranet::home", 7L));
    assertEquals(7L, settingsService.getContextSpaceId(PAGE_NAME, "group::/spaces/space1::home", 7L));
  }

  /**
   * Fails the mutant comparing the space the request carries: a template
   * window whose storage id changed would give up its name, and its apps, when
   * rendered previewed as a space
   */
  @Test
  void pageWindowOfATemplateKeepsItsNameWhateverSpaceItIsPreviewedAs() {
    String name = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000013";
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name, "group_template::workspace::home", 0L));

    assertTrue(settingsService.keepsSettingName(name,
                                                "21",
                                                WindowKind.PAGE,
                                                "group_template::workspace::home",
                                                7L,
                                                List::of));
  }

  @Test
  void getSettingPageReferencePutsASiteLayoutWindowOnTheSiteWhoseLayoutHoldsIt() {
    assertEquals("portal::meeds::profile",
                 settingsService.getSettingPageReference(SITE_NAME, "portal::global::profile", SiteKey.portal("meeds")));
    assertEquals("portal::meeds::stream",
                 settingsService.getSettingPageReference(SITE_NAME, "portal::mycraft::stream", SiteKey.portal("meeds")));
    assertEquals("group::/spaces/space1::profile",
                 settingsService.getSettingPageReference(SITE_NAME,
                                                         "portal::global::profile",
                                                         SiteKey.group("/spaces/space1")));
  }

  @Test
  void getSettingPageReferenceKeepsTheRenderedPageOfAnyOtherWindow() {
    assertEquals("portal::global::profile",
                 settingsService.getSettingPageReference(PAGE_NAME, "portal::global::profile", SiteKey.portal("meeds")));
    assertEquals("portal::global::profile",
                 settingsService.getSettingPageReference(SHARED_NAME, "portal::global::profile", SiteKey.portal("meeds")));
    assertEquals("portal::global::profile",
                 settingsService.getSettingPageReference(SITE_NAME, "portal::global::profile", null));
    assertEquals("not a page", settingsService.getSettingPageReference(SITE_NAME, "not a page", SiteKey.portal("meeds")));
  }

  /**
   * A site-layout window of the meta portal's layout is rendered with the
   * pages of other sites and under every displayed site, and gets a new
   * storage id on each publish of that layout: fails the mutant comparing and
   * recording the rendered page, where the window takes a new name, so that
   * its apps are listed nowhere, and the right of the rendered page's site
   */
  @Test
  void siteLayoutWindowKeepsItsNameAndRightWhateverPageRendersIt() {
    String name = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000012";
    String recordedPage = settingsService.getSettingPageReference(name, "portal::meeds::home", SiteKey.portal("meeds"));
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name,
                                                                       recordedPage,
                                                                       settingsService.getContextSpaceId(name, recordedPage, 0L)));

    assertTrue(settingsService.keepsSettingName(name,
                                                "21",
                                                WindowKind.SITE_LAYOUT,
                                                settingsService.getSettingPageReference(name,
                                                                                        "portal::global::activity",
                                                                                        SiteKey.portal("meeds")),
                                                0L,
                                                List::of));
    assertTrue(settingsService.keepsSettingName(name,
                                                "22",
                                                WindowKind.SITE_LAYOUT,
                                                settingsService.getSettingPageReference(name,
                                                                                        "portal::mycraft::stream",
                                                                                        SiteKey.portal("meeds")),
                                                0L,
                                                List::of));
    mockUser(false);
    when(layoutAclService.canEditSite(SiteKey.portal("meeds"), USERNAME)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(name, USERNAME));
  }

  /**
   * Fails the mutant recording the rendered page: a space site's layout
   * window first rendered with a page of another site would take the platform
   * context, its apps the platform audience, and would not be deleted with
   * the space
   */
  @Test
  void siteLayoutWindowOfASpaceSiteRecordsTheSpaceWhateverPageRendersIt() {
    Space space = new Space();
    space.setId(7L);
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space);

    String recordedPage = settingsService.getSettingPageReference(SITE_NAME,
                                                                  "portal::global::profile",
                                                                  SiteKey.group("/spaces/space1"));

    assertEquals(7L, settingsService.getContextSpaceId(SITE_NAME, recordedPage, 0L));
  }

  @Test
  void keepsSettingNameGeneratedForTheWindowWithoutReadingAnything() {
    assertTrue(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, this::noScan));
    assertTrue(settingsService.keepsSettingName(SITE_NAME, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 5L, this::noScan));
    assertTrue(settingsService.keepsSettingName(SHARED_NAME, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, 5L, this::noScan));
    verifyNoInteractions(cmsService);
  }

  @Test
  void keepsSettingNameMarkedForItsKindOfWindowOnly() {
    assertFalse(settingsService.keepsSettingName(SITE_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, this::noScan));
    assertFalse(settingsService.keepsSettingName(SHARED_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, this::noScan));
    assertFalse(settingsService.keepsSettingName(SECTION_DRAFT_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, this::noScan));
    // A site-layout or shared-layout window from before the markers existed
    assertFalse(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 5L, this::noScan));
    assertFalse(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, 5L, this::noScan));
    // The shared-layout name of another window
    assertFalse(settingsService.keepsSettingName(SHARED_NAME, "13", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, 5L, this::noScan));
    verifyNoInteractions(cmsService);
  }

  @Test
  void pageWindowKeepsANameOfThisPageAndSpaceThatNoOtherWindowCarries() {
    String republished = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000001";
    when(cmsService.getSetting(SETTING_TYPE, republished)).thenReturn(setting(republished, PAGE_REFERENCE, 5L));

    assertTrue(settingsService.keepsSettingName(republished,
                                                "12",
                                                WindowKind.PAGE,
                                                PAGE_REFERENCE,
                                                5L,
                                                () -> List.of(PAGE_NAME)));
  }

  @Test
  void pageWindowGivesUpANameOfAnotherPageSpaceOrWindow() {
    String otherPage = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000002";
    String otherSpace = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000003";
    String duplicated = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000004";
    String forged = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000005";
    when(cmsService.getSetting(SETTING_TYPE, otherPage)).thenReturn(setting(otherPage, "group::/spaces/space1::other", 5L));
    when(cmsService.getSetting(SETTING_TYPE, otherSpace)).thenReturn(setting(otherSpace, PAGE_REFERENCE, 6L));
    when(cmsService.getSetting(SETTING_TYPE, duplicated)).thenReturn(setting(duplicated, PAGE_REFERENCE, 5L));

    assertFalse(settingsService.keepsSettingName(otherPage, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, List::of));
    assertFalse(settingsService.keepsSettingName(otherSpace, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, List::of));
    assertFalse(settingsService.keepsSettingName(duplicated,
                                                 "12",
                                                 WindowKind.PAGE,
                                                 PAGE_REFERENCE,
                                                 5L,
                                                 () -> List.of(PAGE_NAME, duplicated)));
    assertFalse(settingsService.keepsSettingName(forged, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, List::of));
  }

  @Test
  void keepsSettingNameDecidesOncePerWindow() {
    String republished = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000006";
    when(cmsService.getSetting(SETTING_TYPE, republished)).thenReturn(setting(republished, PAGE_REFERENCE, 5L));
    AtomicInteger scans = new AtomicInteger();
    Supplier<Collection<String>> otherWindowNames = () -> {
      scans.incrementAndGet();
      return List.of();
    };

    assertTrue(settingsService.keepsSettingName(republished, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, otherWindowNames));
    assertTrue(settingsService.keepsSettingName(republished, "12", WindowKind.PAGE, PAGE_REFERENCE, 5L, otherWindowNames));
    verify(cmsService, times(1)).getSetting(SETTING_TYPE, republished);
    assertEquals(1, scans.get());
    // Another window carrying the same name is decided on its own
    assertFalse(settingsService.keepsSettingName(republished,
                                                 "14",
                                                 WindowKind.PAGE,
                                                 PAGE_REFERENCE,
                                                 5L,
                                                 () -> List.of(republished)));
  }

  @Test
  void siteLayoutWindowKeepsASiteNameOfThisSiteAndItsSpaceThatNoOtherWindowCarries() {
    String copied = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000007";
    Space space = new Space();
    space.setId(7L);
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space);
    when(cmsService.getSetting(SETTING_TYPE, copied)).thenReturn(setting(copied, "group::/spaces/space1::other", 7L));

    // Rendered on another page of the same site
    assertTrue(settingsService.keepsSettingName(copied, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 0L, () -> List.of(SITE_NAME)));
  }

  @Test
  void siteLayoutWindowGivesUpASiteNameOfAnotherSiteSpaceOrWindow() {
    String otherSite = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000008";
    String otherSpace = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000009";
    String duplicated = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000010";
    String forged = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000011";
    Space space = new Space();
    space.setId(7L);
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space);
    when(cmsService.getSetting(SETTING_TYPE, otherSite)).thenReturn(setting(otherSite, "group::/spaces/space2::home", 7L));
    when(cmsService.getSetting(SETTING_TYPE, otherSpace)).thenReturn(setting(otherSpace, PAGE_REFERENCE, 8L));
    when(cmsService.getSetting(SETTING_TYPE, duplicated)).thenReturn(setting(duplicated, PAGE_REFERENCE, 7L));

    assertFalse(settingsService.keepsSettingName(otherSite, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 0L, List::of));
    assertFalse(settingsService.keepsSettingName(otherSpace, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 0L, List::of));
    assertFalse(settingsService.keepsSettingName(duplicated,
                                                 "12",
                                                 WindowKind.SITE_LAYOUT,
                                                 PAGE_REFERENCE,
                                                 0L,
                                                 () -> List.of(duplicated)));
    assertFalse(settingsService.keepsSettingName(forged, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, 0L, List::of));
  }

  private Collection<String> noScan() {
    throw new AssertionError("The other windows must not be read for a name generated for the window");
  }

  private Identity mockUser(boolean administrator) {
    Identity identity = mock(Identity.class);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(userAcl.isAdministrator(identity)).thenReturn(administrator);
    return identity;
  }

  private CMSSetting setting(String name, String pageReference, long spaceId) {
    return new CMSSetting(SETTING_TYPE, name, pageReference, spaceId);
  }

  private TranslationField field(Map<Locale, String> labels) {
    return new TranslationField(SETTING_TYPE, null, HEADER_TITLE_FIELD, labels, 0);
  }

}
