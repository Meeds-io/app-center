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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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

import io.meeds.layout.service.LayoutAclService;
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

  @Autowired
  private MyApplicationsSettingsService settingsService;

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
   * The -shared- marker is generated only once MyApplicationsPortlet overrides
   * generateRandomId (shared-layout windows, spec mechanism M2): this pins the
   * right for the names that change will produce.
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
   * The -sdraft- marker and the untruncated section_draft_ reference are
   * recorded only once MyApplicationsPortlet overrides generateRandomId and
   * saveSettingName (spec mechanism M2, D15); the reference CMSPortlet records
   * today is pinned by canEditSettingsOfWindowRecordedOnSectionTemplateDraftIsAdministratorOnly.
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
   * The -site- marker is generated only once MyApplicationsPortlet overrides
   * generateRandomId (site-layout windows, spec mechanism M2, D13): this pins
   * the right for the names that change will produce.
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

    assertEquals(Map.of(MAX_APPS_TO_LIST, "12", SHOW_HEADER, "false"), settingsService.getWritableSettings(parameters));
    assertEquals(Map.of(), settingsService.getWritableSettings(null));
  }

  @Test
  void getWritableSettingsValidatesMaxAppsToListRange() {
    assertEquals(Map.of(MAX_APPS_TO_LIST, "1"), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "1")));
    assertEquals(Map.of(MAX_APPS_TO_LIST, "100"), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "100")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "0")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "101")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "-1")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "1e2")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(MAX_APPS_TO_LIST, "99999999999")));
  }

  @Test
  void getWritableSettingsValidatesShowHeader() {
    assertEquals(Map.of(SHOW_HEADER, "true"), settingsService.getWritableSettings(Map.of(SHOW_HEADER, "true")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(SHOW_HEADER, "yes")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(SHOW_HEADER, "<script>")));
  }

  @Test
  void getWritableSettingsKeepsFavoritesListingMode() {
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES),
                 settingsService.getWritableSettings(Map.of(LISTING_MODE, " FAVORITES ")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(LISTING_MODE, "favorites")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(LISTING_MODE, "MANUAL")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(LISTING_MODE, "")));
  }

  @Test
  void getWritableSettingsRefusesAnEmptySelectedListingWhole() {
    Map<String, String> parameters1 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, MAX_APPS_TO_LIST, "8");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(parameters1));
    Map<String, String> parameters2 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(parameters2));
    Map<String, String> parameters3 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_MANUAL, APPLICATION_IDS, "1,x");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(parameters3));
  }

  @Test
  void getWritableSettingsRefusesASelectedCategoryListing() {
    Map<String, String> parameters4 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, APPLICATION_IDS, "3");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(parameters4));
  }

  @Test
  void getWritableSettingsStoresASelectedManualListing() {
    assertEquals(Map.of(LISTING_MODE,
                        LISTING_MODE_SELECTED,
                        SELECTION_MODE,
                        SELECTION_MODE_MANUAL,
                        APPLICATION_IDS,
                        "5,3,9"),
                 settingsService.getWritableSettings(Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_MANUAL,
                                                            APPLICATION_IDS,
                                                            " 5, 3,9,3 ")));
  }

  @Test
  void getWritableSettingsRefusesASelectedListingWithoutItsSelectionMode() {
    Map<String, String> parameters5 = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, APPLICATION_IDS, "4");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(parameters5));
  }

  @Test
  void getWritableSettingsIgnoresASelectionWrittenWithoutItsListingMode() {
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(APPLICATION_IDS, "")));
    assertEquals(Map.of(), settingsService.getWritableSettings(Map.of(APPLICATION_IDS, "4", SELECTION_MODE, SELECTION_MODE_MANUAL)));
    assertEquals(Map.of(MAX_APPS_TO_LIST, "8"),
                 settingsService.getWritableSettings(Map.of(APPLICATION_IDS, "4", MAX_APPS_TO_LIST, "8")));
  }

  @Test
  void getWritableSettingsKeepsTheApplicationIdsInFavoritesMode() {
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, SELECTION_MODE, SELECTION_MODE_CATEGORY, APPLICATION_IDS, "5"),
                 settingsService.getWritableSettings(Map.of(LISTING_MODE,
                                                            LISTING_MODE_FAVORITES,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            APPLICATION_IDS,
                                                            "5")));
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, APPLICATION_IDS, ""),
                 settingsService.getWritableSettings(Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, APPLICATION_IDS, "")));
  }

  @Test
  void getWritableSettingsDropsInvalidSelectionValues() {
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(SELECTION_MODE, "manual")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, "1;2")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, "-1")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, "1,,2")));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, "1234567890123456789")));
    String overCap = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS + 1L)
                               .mapToObj(String::valueOf)
                               .collect(Collectors.joining(","));
    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, overCap)));
    String atCapWithDuplicate = LongStream.rangeClosed(1, ApplicationCenterService.MAX_LISTED_APPLICATIONS)
                                          .mapToObj(String::valueOf)
                                          .collect(Collectors.joining(","))
        + ",1";
    assertEquals(ApplicationCenterService.MAX_LISTED_APPLICATIONS,
                 settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, atCapWithDuplicate)).get(APPLICATION_IDS).split(",").length);
  }

  private static Map<String, String> withFavorites(String name, String value) {
    return Map.of(LISTING_MODE, LISTING_MODE_FAVORITES, name, value);
  }

  @Test
  void getWritableSettingsDropsAVeryLongIdListWithoutOverflowing() {
    String veryLong = LongStream.rangeClosed(1, 200_000).mapToObj(String::valueOf).collect(Collectors.joining(","));

    assertEquals(FAVORITES_ONLY, settingsService.getWritableSettings(withFavorites(APPLICATION_IDS, veryLong)));
    assertEquals(List.of(), MyApplicationsSettingsService.getApplicationIds(veryLong));
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
  void migrateHeaderTitleCopiesLegacyLabelsWhenNoneUnderSettingName() {
    Map<Locale, String> labels = Map.of(Locale.ENGLISH, "My shortcuts", Locale.FRENCH, "Mes raccourcis");
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(labels));

    settingsService.migrateHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService).saveTranslationLabels(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD, labels, false);
  }

  @Test
  @SneakyThrows
  void migrateHeaderTitleCopiesLegacyLabelsOverBlankLabels() {
    Map<Locale, String> labels = Map.of(Locale.ENGLISH, "My shortcuts");
    when(translationService.getTranslationField(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             " ")));
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(labels));

    settingsService.migrateHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService).saveTranslationLabels(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD, labels, false);
  }

  @Test
  @SneakyThrows
  void migrateHeaderTitleKeepsLabelsAlreadyUnderSettingName() {
    when(translationService.getTranslationField(SETTING_TYPE, PAGE_NAME, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             "New")));
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenReturn(field(Map.of(Locale.ENGLISH,
                                                                                                                             "Old")));

    settingsService.migrateHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  @SneakyThrows
  void migrateHeaderTitleWithoutLegacyLabelsSavesNothing() {
    when(translationService.getTranslationField(SETTING_TYPE, LEGACY_ID, HEADER_TITLE_FIELD)).thenThrow(new ObjectNotFoundException(LEGACY_ID));

    settingsService.migrateHeaderTitle(LEGACY_ID, PAGE_NAME);
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  void migrateHeaderTitleWithBlankKeysDoesNothing() {
    settingsService.migrateHeaderTitle(null, PAGE_NAME);
    settingsService.migrateHeaderTitle(LEGACY_ID, " ");
    verifyNoInteractions(translationService);
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
