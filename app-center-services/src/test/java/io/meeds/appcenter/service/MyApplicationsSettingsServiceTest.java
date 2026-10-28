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
import static io.meeds.appcenter.service.MyApplicationsSettingsService.COLOR;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.H_ALIGN;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.HEADER_TITLE_FIELD;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.ICON_SIZE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LIST_IN_ROW;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.OUTLINED;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHOW_ICON;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHOW_NAME;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHOW_SEE_MORE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.TEMPLATE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.V_ALIGN;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE_FAVORITES;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.LISTING_MODE_SELECTED;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.MAX_APPS_TO_LIST;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE_CATEGORY;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SELECTION_MODE_MANUAL;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHARED_LAYOUT_REFERENCE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SETTING_TYPE;
import static io.meeds.appcenter.service.MyApplicationsSettingsService.SHOW_HEADER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataType;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
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

  @MockitoBean
  private ApplicationCenterStorage      applicationCenterStorage;

  @MockitoBean
  private MetadataService               metadataService;

  @MockitoBean
  private SpaceService                  spaceService;

  @MockitoBean
  private LayoutService                 layoutService;

  @Autowired
  private MyApplicationsSettingsService settingsService;

  @BeforeEach
  void existingApplications() {
    lenient().when(applicationCenterStorage.getApplication(anyLong())).thenReturn(new Application());
    lenient().when(metadataService.getMetadataById(anyLong())).thenAnswer(invocation -> metadata(invocation.getArgument(0), "category"));
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

  /**
   * A window rendered before its space was keyed on its page's site may have
   * recorded another space: fails the mutant reading the recorded space, where
   * the publishers and managers of a space would not edit a window of its page
   * first reached through a portal site's navigation, though they edit one
   * placed there today
   */
  @Test
  void canEditSettingsTakesTheSpaceOfTheRecordedPageNotTheRecordedSpace() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 0));
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(5L));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 5)).thenReturn(true);

    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
  }

  @Test
  void canEditSettingsOfPageWindowUsesPageContentRight() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, PAGE_NAME)).thenReturn(setting(PAGE_NAME, PAGE_REFERENCE, 3));
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(3L));

    assertFalse(settingsService.canEditSettings(PAGE_NAME, USERNAME));
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 3)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(PAGE_NAME, USERNAME));
  }

  /**
   * MyApplicationsPortlet#generateRandomId marks the name of a shared-layout
   * window that has a storage id with -shared- (spec mechanism M2), which a
   * configured shared layout never gives: this pins that name's right.
   */
  /**
   * A window of a configured shared layout has no storage id, so no generated
   * name: fails the mutant telling a shared-layout window by its name only,
   * where the content managers, platform publishers included, would edit a
   * window every site renders
   */
  @Test
  void canEditSettingsOfAConfiguredSharedLayoutWindowIsAdministratorOnly() {
    String configuredName = "shared-shortcuts";
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, configuredName)).thenReturn(setting(configuredName, SHARED_LAYOUT_REFERENCE, 0));
    lenient().when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(configuredName, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(configuredName, USERNAME));
  }

  @Test
  void canEditSettingsOfSharedLayoutWindowIsAdministratorOnly() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SHARED_NAME)).thenReturn(setting(SHARED_NAME, PAGE_REFERENCE, 0));
    when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SHARED_NAME, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
    assertTrue(settingsService.canEditSettings(SHARED_NAME, USERNAME));
  }

  /**
   * A shared-layout window's context is the platform's whatever space its
   * setting recorded, as its applications' audience is: fails the mutant
   * reading the recorded space, where a space's publishers would create an
   * application for every platform user
   */
  @Test
  void canEditSettingsOfSharedMarkerIsAdministratorOnlyWhateverSpaceItsSettingRecorded() {
    Identity identity = mockUser(false);
    when(cmsService.getSetting(SETTING_TYPE, SHARED_NAME)).thenReturn(setting(SHARED_NAME, PAGE_REFERENCE, 3));
    lenient().when(cmsService.hasEditPermission(any(Identity.class), anyString(), anyLong())).thenReturn(true);

    assertFalse(settingsService.canEditSettings(SHARED_NAME, USERNAME));
    when(userAcl.isAdministrator(identity)).thenReturn(true);
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
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(3L));
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
    when(layoutAclService.canEditSite(nullable(SiteKey.class), anyString())).thenReturn(true);

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
  void getWritableSettingsKeepsTheDisplaySettingsWithoutAListingMode() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(TEMPLATE, " CARDS ");
    parameters.put(SHOW_SEE_MORE, "false");
    parameters.put(LIST_IN_ROW, "true");
    parameters.put(SHOW_ICON, "true");
    parameters.put(ICON_SIZE, " 45 ");
    parameters.put(SHOW_NAME, "true");
    parameters.put(COLOR, "#E1E8EE");
    parameters.put(OUTLINED, "false");
    parameters.put(H_ALIGN, "CENTER");
    parameters.put(V_ALIGN, "END");

    Map<String, String> expected = new HashMap<>();
    expected.put(TEMPLATE, "CARDS");
    expected.put(SHOW_SEE_MORE, "false");
    expected.put(LIST_IN_ROW, "true");
    expected.put(SHOW_ICON, "true");
    expected.put(ICON_SIZE, "45");
    expected.put(SHOW_NAME, "true");
    expected.put(COLOR, "#E1E8EE");
    expected.put(OUTLINED, "false");
    expected.put(H_ALIGN, "CENTER");
    expected.put(V_ALIGN, "END");
    assertEquals(expected, settingsService.getWritableSettings(PAGE_NAME, parameters));
  }

  @Test
  void getWritableSettingsKeepsTheDisplaySettingsWithAListingMode() {
    // A drawer save posts its listing mode with the display settings: they are kept in either mode
    Map<String, String> display = new HashMap<>();
    display.put(TEMPLATE, "ROW");
    display.put(SHOW_SEE_MORE, "false");
    display.put(LIST_IN_ROW, "true");
    display.put(SHOW_ICON, "false");
    display.put(ICON_SIZE, "32");
    display.put(SHOW_NAME, "true");
    display.put(COLOR, "");
    display.put(OUTLINED, "true");
    display.put(H_ALIGN, "CENTER");
    display.put(V_ALIGN, "CENTER");

    Map<String, String> favorites = new HashMap<>(display);
    favorites.put(LISTING_MODE, LISTING_MODE_FAVORITES);
    assertEquals(favorites, settingsService.getWritableSettings(PAGE_NAME, favorites));

    Map<String, String> selected = new HashMap<>(display);
    selected.put(LISTING_MODE, LISTING_MODE_SELECTED);
    selected.put(SELECTION_MODE, SELECTION_MODE_MANUAL);
    selected.put(APPLICATION_IDS, "5");
    assertEquals(selected, settingsService.getWritableSettings(PAGE_NAME, selected));
  }

  @Test
  void getWritableSettingsValidatesTheTemplate() {
    for (String template : List.of("ROW", "CARDS", "COLUMN")) {
      assertEquals(Map.of(TEMPLATE, template), settingsService.getWritableSettings(PAGE_NAME, Map.of(TEMPLATE, template)));
    }
    for (String template : List.of("row", "LEGACY", "", "ROW,CARDS", "'ROW'")) {
      assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(TEMPLATE, template)), template);
    }
  }

  @Test
  void getWritableSettingsValidatesTheDisplayFlags() {
    for (String flag : List.of(SHOW_SEE_MORE, LIST_IN_ROW, SHOW_ICON, SHOW_NAME, OUTLINED)) {
      assertEquals(Map.of(flag, "true"), settingsService.getWritableSettings(PAGE_NAME, Map.of(flag, " true ")), flag);
      assertEquals(Map.of(flag, "false"), settingsService.getWritableSettings(PAGE_NAME, Map.of(flag, "false")), flag);
      for (String value : List.of("TRUE", "yes", "1", "", "true,")) {
        assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(flag, value)), flag + "=" + value);
      }
    }
  }

  @Test
  void getWritableSettingsValidatesTheIconSizeRange() {
    assertEquals(Map.of(ICON_SIZE, "12"), settingsService.getWritableSettings(PAGE_NAME, Map.of(ICON_SIZE, "12")));
    assertEquals(Map.of(ICON_SIZE, "60"), settingsService.getWritableSettings(PAGE_NAME, Map.of(ICON_SIZE, "60")));
    assertEquals(Map.of(ICON_SIZE, "32"), settingsService.getWritableSettings(PAGE_NAME, Map.of(ICON_SIZE, "032")));
    for (String value : List.of("11", "61", "0", "-1", "1e1", "45px", "45.5", "", "4500")) {
      assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(ICON_SIZE, value)), value);
    }
  }

  @Test
  void getWritableSettingsValidatesTheColor() {
    assertEquals(Map.of(COLOR, "#e1e8ee"), settingsService.getWritableSettings(PAGE_NAME, Map.of(COLOR, "#e1e8ee")));
    assertEquals(Map.of(COLOR, "#E1E8EE80"), settingsService.getWritableSettings(PAGE_NAME, Map.of(COLOR, " #E1E8EE80 ")));
    // Empty: back to no color
    assertEquals(Map.of(COLOR, ""), settingsService.getWritableSettings(PAGE_NAME, Map.of(COLOR, "")));
    for (String value : List.of("#FFF", "#FFFF", "#GGGGGG", "E1E8EE", "#E1E8EE8", "red", "#E1E8EE</script>", "#E1E8EE';alert(1)//")) {
      assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(COLOR, value)), value);
    }
  }

  @Test
  void getWritableSettingsValidatesTheAlignments() {
    for (String key : List.of(H_ALIGN, V_ALIGN)) {
      for (String alignment : List.of("START", "CENTER", "END")) {
        assertEquals(Map.of(key, alignment), settingsService.getWritableSettings(PAGE_NAME, Map.of(key, alignment)), key);
      }
      for (String alignment : List.of("LEFT", "MIDDLE", "start", "")) {
        assertEquals(Map.of(), settingsService.getWritableSettings(PAGE_NAME, Map.of(key, alignment)), key + "=" + alignment);
      }
    }
  }

  @Test
  void readsStoredDisplaySettingsAsTheirTypeOrNull() {
    assertEquals("COLUMN", MyApplicationsSettingsService.getTemplate("COLUMN"));
    assertNull(MyApplicationsSettingsService.getTemplate(" COLUMN"));
    assertNull(MyApplicationsSettingsService.getTemplate("'+alert(1)+'"));
    assertNull(MyApplicationsSettingsService.getTemplate(null));

    assertEquals(Boolean.TRUE, MyApplicationsSettingsService.getDisplayFlag("true"));
    assertEquals(Boolean.FALSE, MyApplicationsSettingsService.getDisplayFlag("false"));
    assertNull(MyApplicationsSettingsService.getDisplayFlag("alert(1)"));
    assertNull(MyApplicationsSettingsService.getDisplayFlag(null));

    assertEquals(Integer.valueOf(45), MyApplicationsSettingsService.getIconSize("45"));
    assertEquals(Integer.valueOf(12), MyApplicationsSettingsService.getIconSize("12"));
    assertEquals(Integer.valueOf(60), MyApplicationsSettingsService.getIconSize("60"));
    assertNull(MyApplicationsSettingsService.getIconSize("11"));
    assertNull(MyApplicationsSettingsService.getIconSize("61"));
    assertNull(MyApplicationsSettingsService.getIconSize("45);alert(1"));
    assertNull(MyApplicationsSettingsService.getIconSize(null));

    assertEquals("#E1E8EE", MyApplicationsSettingsService.getColor("#E1E8EE"));
    assertEquals("", MyApplicationsSettingsService.getColor("#E1E8EE'</script>"));
    assertEquals("", MyApplicationsSettingsService.getColor(null));

    assertEquals("END", MyApplicationsSettingsService.getAlignment("END"));
    assertNull(MyApplicationsSettingsService.getAlignment("RIGHT"));
    assertNull(MyApplicationsSettingsService.getAlignment(null));
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
    when(metadataService.getMetadataById(4l)).thenReturn(null);
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
  void getWritableSettingsDropsTheIdsOfOtherMetadata() {
    when(metadataService.getMetadataById(5l)).thenReturn(metadata(5l, "tags"));
    assertEquals(Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, CATEGORY_IDS, "7"),
                 settingsService.getWritableSettings(PAGE_NAME, Map.of(LISTING_MODE,
                                                            LISTING_MODE_SELECTED,
                                                            SELECTION_MODE,
                                                            SELECTION_MODE_CATEGORY,
                                                            CATEGORY_IDS,
                                                            "5,7")));
    Map<String, String> onlyTag = Map.of(LISTING_MODE, LISTING_MODE_SELECTED, SELECTION_MODE, SELECTION_MODE_CATEGORY, CATEGORY_IDS, "5");
    assertThrows(IllegalArgumentException.class, () -> settingsService.getWritableSettings(PAGE_NAME, onlyTag));
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
    assertTrue(settingsService.keepsSettingName(pageName, "12", WindowKind.PAGE, PAGE_REFERENCE, this::noScan));
    assertTrue(settingsService.keepsSettingName(siteName, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, this::noScan));
    assertTrue(settingsService.keepsSettingName(sharedName, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, this::noScan));
  }

  /**
   * A section-template draft window's name carries the template: fails the
   * mutants dropping the marker or the template id, by which the template's
   * earlier sessions and its deletion find the applications to delete
   */
  @Test
  void generateSectionTemplateDraftSettingNameMarksTheTemplate() {
    String name = settingsService.generateSectionTemplateDraftSettingName("12", 7L);

    assertTrue(name.matches("^Untitled-12-sdraft-7-[0-9a-f]{8}-[0-9a-f-]{27}$"), name);
    assertTrue(settingsService.isSectionTemplateDraftName(name));
    assertEquals("12", settingsService.getSectionTemplateDraftStorageId(name, 7L));
    assertTrue(settingsService.keepsSettingName(name, "12", WindowKind.SECTION_TEMPLATE_DRAFT, PAGE_REFERENCE, this::noScan));
  }

  @Test
  void getSectionTemplateIdReadsTheTemplateOfASectionTemplateDraftPageOnly() {
    assertEquals(7L, settingsService.getSectionTemplateId("portal::global::section_draft_7_testuser"));
    assertEquals(7L, settingsService.getSectionTemplateId("portal::global::section_draft_7_test_user.name"));
    assertEquals(0L, settingsService.getSectionTemplateId("portal::global::section"));
    assertEquals(0L, settingsService.getSectionTemplateId("portal::dw::section_draft_7_testuser"));
    assertEquals(0L, settingsService.getSectionTemplateId("group::/spaces/team::section_draft_7_testuser"));
    assertEquals(0L, settingsService.getSectionTemplateId("portal::global::home_draft_testuser"));
    assertEquals(0L, settingsService.getSectionTemplateId("portal::global::section_draft_99999999999999999999_testuser"));
    assertEquals(0L, settingsService.getSectionTemplateId("not a reference"));
    assertEquals(0L, settingsService.getSectionTemplateId(null));
  }

  /**
   * Fails the mutants matching another template's names, or an unanchored
   * marker, which would delete another template's applications
   */
  @Test
  void getSectionTemplateDraftStorageIdReadsTheNamesOfThatTemplateOnly() {
    assertEquals("12", settingsService.getSectionTemplateDraftStorageId(SECTION_DRAFT_NAME, 7L));
    assertNull(settingsService.getSectionTemplateDraftStorageId(SECTION_DRAFT_NAME, 77L));
    assertNull(settingsService.getSectionTemplateDraftStorageId("Untitled-12-sdraft-77-1b4e28ba", 7L));
    assertNull(settingsService.getSectionTemplateDraftStorageId("Untitled-12-x-sdraft-7-1b4e28ba", 7L));
    assertNull(settingsService.getSectionTemplateDraftStorageId("Untitled-12-sdraft-99999999999999999999-1b4e28ba", 7L));
    assertNull(settingsService.getSectionTemplateDraftStorageId(PAGE_NAME, 7L));
    assertNull(settingsService.getSectionTemplateDraftStorageId(null, 7L));
    assertEquals("Untitled-%-sdraft-7-%", settingsService.getSectionTemplateDraftNamePattern(7L));
  }

  @Test
  void isSectionTemplateDraftNameTellsTheMarkedNamesOnly() {
    assertTrue(settingsService.isSectionTemplateDraftName(SECTION_DRAFT_NAME));
    assertFalse(settingsService.isSectionTemplateDraftName(PAGE_NAME));
    assertFalse(settingsService.isSectionTemplateDraftName(SITE_NAME));
    assertFalse(settingsService.isSectionTemplateDraftName("Untitled-12-x-sdraft-7-1b4e28ba"));
    assertFalse(settingsService.isSectionTemplateDraftName(null));
  }

  /**
   * The section-template editor saves its draft page again, with new windows,
   * at each session: fails the mutant keeping, through the page scan, a name
   * generated for another window, an earlier session's, whose applications
   * would then be listed by two windows
   */
  @Test
  void keepsSettingNameOfASectionTemplateDraftWindowForItsOwnStorageIdOnly() {
    assertTrue(settingsService.keepsSettingName(SECTION_DRAFT_NAME,
                                                "12",
                                                WindowKind.SECTION_TEMPLATE_DRAFT,
                                                PAGE_REFERENCE,
                                                this::noScan));
    assertFalse(settingsService.keepsSettingName(SECTION_DRAFT_NAME,
                                                 "13",
                                                 WindowKind.SECTION_TEMPLATE_DRAFT,
                                                 PAGE_REFERENCE,
                                                 this::noScan));
    assertFalse(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.SECTION_TEMPLATE_DRAFT, PAGE_REFERENCE, this::noScan));
    verifyNoInteractions(cmsService);
  }

  /**
   * A space's page reached through a portal site's navigation is still a page
   * of the space: fails the mutant keying a page window on anything but its
   * page's site, where its apps would take the platform audience and outlive
   * the space
   */
  @Test
  void getContextSpaceIdRecordsTheSpaceOwningTheRecordedPagesSite() {
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(7L));

    assertEquals(7L, settingsService.getContextSpaceId(PAGE_NAME, PAGE_REFERENCE));
    assertEquals(7L, settingsService.getContextSpaceId(SITE_NAME, PAGE_REFERENCE));
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "portal::meeds::home"));
    assertEquals(0L, settingsService.getContextSpaceId(SITE_NAME, "portal::meeds::home"));
    assertEquals(0L, settingsService.getContextSpaceId(SITE_NAME, "group::/spaces/deleted::home"));
    assertEquals(0L, settingsService.getContextSpaceId(SHARED_NAME, PAGE_REFERENCE));
  }

  /**
   * A space's public site is a portal site carrying the space's id: fails the
   * mutant reading group sites only, where a window of the public site, on a
   * page or in its layout, would take the platform context
   */
  @Test
  void getContextSpaceIdRecordsTheSpaceOfAPublicSite() {
    PortalConfig publicSite = mock(PortalConfig.class);
    when(publicSite.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID)).thenReturn("9");
    when(layoutService.getPortalConfig(SiteKey.portal("space1public"))).thenReturn(publicSite);
    when(spaceService.getSpaceById("9")).thenReturn(space(9L));
    PortalConfig deletedSpaceSite = mock(PortalConfig.class);
    when(deletedSpaceSite.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID)).thenReturn("10");
    when(layoutService.getPortalConfig(SiteKey.portal("deletedpublic"))).thenReturn(deletedSpaceSite);

    assertEquals(9L, settingsService.getContextSpaceId(PAGE_NAME, "portal::space1public::home"));
    assertEquals(9L, settingsService.getContextSpaceId(SITE_NAME, "portal::space1public::home"));
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "portal::deletedpublic::home"));
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "portal::meeds::home"));
  }

  /**
   * Fails the mutant missing a deleted space's site, group or public, where a
   * window of a deleted space would create an application for every platform
   * user; the mutant missing a removed portal site, as a space's public site
   * is once its space's deletion completes; and the mutant taking any other
   * site, or a shared-layout window, for a deleted context
   */
  @Test
  void isContextDeletedForTheSiteOfASpaceThatNoLongerExists() {
    when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(7L));
    PortalConfig publicSite = mock(PortalConfig.class);
    when(publicSite.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID)).thenReturn("10");
    when(layoutService.getPortalConfig(SiteKey.portal("deletedpublic"))).thenReturn(publicSite);
    when(layoutService.getPortalConfig(SiteKey.portal("meeds"))).thenReturn(mock(PortalConfig.class));

    assertTrue(settingsService.isContextDeleted(PAGE_NAME, "group::/spaces/deleted::home"));
    assertTrue(settingsService.isContextDeleted(SITE_NAME, "portal::deletedpublic::home"));
    assertTrue(settingsService.isContextDeleted(SITE_NAME, "portal::removedpublic::home"));
    assertFalse(settingsService.isContextDeleted(PAGE_NAME, PAGE_REFERENCE));
    assertFalse(settingsService.isContextDeleted(PAGE_NAME, "group::/platform/users::home"));
    assertFalse(settingsService.isContextDeleted(PAGE_NAME, "portal::meeds::home"));
    assertFalse(settingsService.isContextDeleted(PAGE_NAME, "group_template::workspace::home"));
    assertFalse(settingsService.isContextDeleted(SHARED_NAME, "group::/spaces/deleted::home"));
  }

  /**
   * The layout editor previews a space template page as a space of the
   * editor's choice, which the request then carries, while a site or space
   * template belongs to no space: fails the mutant resolving a template site
   * as a space site or a public site, where the template's apps would take
   * that space's audience and be deleted with it
   */
  /**
   * The shared-layout reference is never read as a site: fails the mutants
   * telling a shared-layout window by its name only, where the applications of
   * a window of a configured shared layout would take the audience of a space
   * the global site resolved to, or be refused where it resolved to none
   */
  @Test
  void aConfiguredSharedLayoutWindowHasThePlatformContext() {
    String configuredName = "shared-shortcuts";
    PortalConfig site = mock(PortalConfig.class);
    lenient().when(site.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID)).thenReturn("7");
    lenient().when(layoutService.getPortalConfig(SiteKey.portal("global"))).thenReturn(site);
    lenient().when(spaceService.getSpaceById("7")).thenReturn(space(7L));

    assertEquals(0L, settingsService.getContextSpaceId(configuredName, SHARED_LAYOUT_REFERENCE));
    when(layoutService.getPortalConfig(SiteKey.portal("global"))).thenReturn(null);
    assertFalse(settingsService.isContextDeleted(configuredName, SHARED_LAYOUT_REFERENCE));
  }

  @Test
  void getContextSpaceIdRecordsThePlatformContextForATemplateWindow() {
    lenient().when(spaceService.getSpaceByGroupId(anyString())).thenReturn(space(7L));
    PortalConfig site = mock(PortalConfig.class);
    lenient().when(site.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID)).thenReturn("7");
    lenient().when(layoutService.getPortalConfig(any(SiteKey.class))).thenReturn(site);
    lenient().when(spaceService.getSpaceById("7")).thenReturn(space(7L));

    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "group_template::workspace::home"));
    assertEquals(0L, settingsService.getContextSpaceId(PAGE_NAME, "portal_template::intranet::home"));
    assertEquals(7L, settingsService.getContextSpaceId(PAGE_NAME, "group::/spaces/space1::home"));
  }

  /**
   * A template window's name is compared with the platform context, whatever
   * space a preview attaches to the request: a template window whose storage
   * id changed keeps its name, and its apps
   */
  @Test
  void pageWindowOfATemplateKeepsItsNameWhateverSpaceItIsPreviewedAs() {
    String name = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000013";
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name, "group_template::workspace::home", 0L));

    assertTrue(settingsService.keepsSettingName(name,
                                                "21",
                                                WindowKind.PAGE,
                                                "group_template::workspace::home",
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
    long recordedSpaceId = settingsService.getContextSpaceId(name, recordedPage);
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name, recordedPage, recordedSpaceId));

    assertTrue(settingsService.keepsSettingName(name,
                                                "21",
                                                WindowKind.SITE_LAYOUT,
                                                settingsService.getSettingPageReference(name,
                                                                                        "portal::global::activity",
                                                                                        SiteKey.portal("meeds")),
                                                List::of));
    assertTrue(settingsService.keepsSettingName(name,
                                                "22",
                                                WindowKind.SITE_LAYOUT,
                                                settingsService.getSettingPageReference(name,
                                                                                        "portal::mycraft::stream",
                                                                                        SiteKey.portal("meeds")),
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

    assertEquals(7L, settingsService.getContextSpaceId(SITE_NAME, recordedPage));
  }

  /**
   * Fails the mutant memoising every window's decision for the life of the
   * node: past the bound, the memo is emptied and a window is decided again
   */
  @Test
  void keepsSettingNameMemoisesABoundedNumberOfDecisions() {
    String name = "Untitled-99-0a1b2c3d-0000-0000-0000-000000000099";
    when(cmsService.getSetting(SETTING_TYPE, name)).thenReturn(setting(name, PAGE_REFERENCE, 0L));
    for (int storageId = 100; storageId <= 1100; storageId++) {
      settingsService.keepsSettingName(name, String.valueOf(storageId), WindowKind.PAGE, PAGE_REFERENCE, List::of);
    }
    settingsService.keepsSettingName(name, "100", WindowKind.PAGE, PAGE_REFERENCE, List::of);

    verify(cmsService, times(1002)).getSetting(SETTING_TYPE, name);
  }

  @Test
  void keepsSettingNameGeneratedForTheWindowWithoutReadingAnything() {
    assertTrue(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, this::noScan));
    assertTrue(settingsService.keepsSettingName(SITE_NAME, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, this::noScan));
    assertTrue(settingsService.keepsSettingName(SHARED_NAME, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, this::noScan));
    verifyNoInteractions(cmsService);
  }

  @Test
  void keepsSettingNameMarkedForItsKindOfWindowOnly() {
    assertFalse(settingsService.keepsSettingName(SITE_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, this::noScan));
    assertFalse(settingsService.keepsSettingName(SHARED_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, this::noScan));
    assertFalse(settingsService.keepsSettingName(SECTION_DRAFT_NAME, "12", WindowKind.PAGE, PAGE_REFERENCE, this::noScan));
    // A site-layout or shared-layout window from before the markers existed
    assertFalse(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, this::noScan));
    assertFalse(settingsService.keepsSettingName(PAGE_NAME, "12", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, this::noScan));
    // The shared-layout name of another window
    assertFalse(settingsService.keepsSettingName(SHARED_NAME, "13", WindowKind.SHARED_LAYOUT, PAGE_REFERENCE, this::noScan));
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
                                                () -> List.of(PAGE_NAME)));
  }

  /**
   * A page publish gives every window a new storage id, and a window rendered
   * before its space was keyed on its page's site may have recorded another
   * space: fails the mutant comparing the recorded space, where such a window
   * gives up its name at its first render after a publish, and the
   * applications it created are listed nowhere
   */
  @Test
  void pageWindowKeepsANameOfThisPageWhateverSpaceItsSettingRecorded() {
    String legacy = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000003";
    when(cmsService.getSetting(SETTING_TYPE, legacy)).thenReturn(setting(legacy, PAGE_REFERENCE, 0L));
    lenient().when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(7L));

    assertTrue(settingsService.keepsSettingName(legacy, "21", WindowKind.PAGE, PAGE_REFERENCE, List::of));
  }

  @Test
  void pageWindowGivesUpANameOfAnotherPageOrWindow() {
    String otherPage = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000002";
    String duplicated = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000004";
    String forged = "Untitled-13-0a1b2c3d-0000-0000-0000-000000000005";
    when(cmsService.getSetting(SETTING_TYPE, otherPage)).thenReturn(setting(otherPage, "group::/spaces/space1::other", 5L));
    when(cmsService.getSetting(SETTING_TYPE, duplicated)).thenReturn(setting(duplicated, PAGE_REFERENCE, 5L));

    assertFalse(settingsService.keepsSettingName(otherPage, "12", WindowKind.PAGE, PAGE_REFERENCE, List::of));
    assertFalse(settingsService.keepsSettingName(duplicated,
                                                 "12",
                                                 WindowKind.PAGE,
                                                 PAGE_REFERENCE,
                                                 () -> List.of(PAGE_NAME, duplicated)));
    assertFalse(settingsService.keepsSettingName(forged, "12", WindowKind.PAGE, PAGE_REFERENCE, List::of));
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

    assertTrue(settingsService.keepsSettingName(republished, "12", WindowKind.PAGE, PAGE_REFERENCE, otherWindowNames));
    assertTrue(settingsService.keepsSettingName(republished, "12", WindowKind.PAGE, PAGE_REFERENCE, otherWindowNames));
    verify(cmsService, times(1)).getSetting(SETTING_TYPE, republished);
    assertEquals(1, scans.get());
    // Another window carrying the same name is decided on its own
    assertFalse(settingsService.keepsSettingName(republished,
                                                 "14",
                                                 WindowKind.PAGE,
                                                 PAGE_REFERENCE,
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
    assertTrue(settingsService.keepsSettingName(copied, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, () -> List.of(SITE_NAME)));
  }

  /**
   * Fails the mutant comparing the recorded space: a site-layout window of a
   * space's public site, recorded before that site resolved to the space,
   * would give up its name at its first render after a layout publish
   */
  @Test
  void siteLayoutWindowKeepsASiteNameOfThisSiteWhateverSpaceItsSettingRecorded() {
    String legacy = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000009";
    when(cmsService.getSetting(SETTING_TYPE, legacy)).thenReturn(setting(legacy, PAGE_REFERENCE, 0L));
    lenient().when(spaceService.getSpaceByGroupId("/spaces/space1")).thenReturn(space(7L));

    assertTrue(settingsService.keepsSettingName(legacy, "21", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, List::of));
  }

  @Test
  void siteLayoutWindowGivesUpASiteNameOfAnotherSiteOrWindow() {
    String otherSite = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000008";
    String duplicated = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000010";
    String forged = "Untitled-13-site-0a1b2c3d-0000-0000-0000-000000000011";
    when(cmsService.getSetting(SETTING_TYPE, otherSite)).thenReturn(setting(otherSite, "group::/spaces/space2::home", 7L));
    when(cmsService.getSetting(SETTING_TYPE, duplicated)).thenReturn(setting(duplicated, PAGE_REFERENCE, 7L));

    assertFalse(settingsService.keepsSettingName(otherSite, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, List::of));
    assertFalse(settingsService.keepsSettingName(duplicated,
                                                 "12",
                                                 WindowKind.SITE_LAYOUT,
                                                 PAGE_REFERENCE,
                                                 () -> List.of(duplicated)));
    assertFalse(settingsService.keepsSettingName(forged, "12", WindowKind.SITE_LAYOUT, PAGE_REFERENCE, List::of));
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

  private static Space space(long id) {
    Space space = new Space();
    space.setId(id);
    return space;
  }

  private static Metadata metadata(long id, String type) {
    Metadata metadata = new Metadata();
    metadata.setId(id);
    metadata.setType(new MetadataType(1L, type));
    return metadata;
  }

  private CMSSetting setting(String name, String pageReference, long spaceId) {
    return new CMSSetting(SETTING_TYPE, name, pageReference, spaceId);
  }

  private TranslationField field(Map<Locale, String> labels) {
    return new TranslationField(SETTING_TYPE, null, HEADER_TITLE_FIELD, labels, 0);
  }

}
