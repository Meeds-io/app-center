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
package io.meeds.appcenter.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.Metadata;
import org.exoplatform.social.metadata.model.MetadataType;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCopy;
import io.meeds.appcenter.model.ApplicationListCopy;
import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class MyApplicationsApplicationSettingsPluginTest {

  private static final String                     USERNAME         = "mary";

  private static final String                     OBJECT_TYPE      = "myApplicationsPortlet";

  private static final String                     FIELD            = "headerTitle";

  private static final String                     PAGE_REFERENCE   = "portal::classic::home";

  private static final String                     OTHER_PAGE       = "portal::classic::news";

  private static final String                     SETTING_NAME     = "Untitled-12-1b4e28ba";

  private static final String                     NEW_NAME         = "Untitled-45-2c5f39cb";

  private static final String                     LEGACY_ID        = "77";

  private static final long                       STORAGE_ID       = 45;

  @Mock
  private MyApplicationsSettingsService           settingsService;

  @Mock
  private MyApplicationsCopyService               copyService;

  @Mock
  private CMSService                              cmsService;

  @Mock
  private TranslationService                      translationService;

  @Mock
  private LocaleConfigService                     localeConfigService;

  @Mock
  private IdentityManager                         identityManager;

  @Mock
  private ApplicationSettingsService              applicationSettingsService;

  @Mock
  private ApplicationCenterStorage                applicationCenterStorage;

  @Mock
  private MetadataService                         metadataService;

  @InjectMocks
  private MyApplicationsApplicationSettingsPlugin plugin;

  /**
   * The drawer's own validation, run for real on the storage and metadata
   * mocks: every Application Center app and every category exists unless a
   * test says otherwise
   */
  @BeforeEach
  void setUp() {
    LocaleConfig english = localeConfig(Locale.ENGLISH);
    LocaleConfig french = localeConfig(Locale.FRENCH);
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(english, french));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(english);
    lenient().when(settingsService.canEditPageWindowSettings(PAGE_REFERENCE, USERNAME)).thenReturn(true);
    lenient().when(settingsService.canEditSiteLayoutWindowSettings(any(), eq(USERNAME))).thenReturn(true);
    lenient().when(settingsService.generateSettingName(String.valueOf(STORAGE_ID), WindowKind.PAGE)).thenReturn(NEW_NAME);
    lenient().when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(new Identity("7"));
    MyApplicationsSettingsService drawerValidation = new MyApplicationsSettingsService();
    ReflectionTestUtils.setField(drawerValidation, "applicationCenterStorage", applicationCenterStorage);
    ReflectionTestUtils.setField(drawerValidation, "metadataService", metadataService);
    lenient().when(settingsService.getWritableSettings(any(), anyMap()))
             .thenAnswer(invocation -> drawerValidation.getWritableSettings(invocation.getArgument(0), invocation.getArgument(1)));
    lenient().when(applicationCenterStorage.getApplication(anyLong())).thenReturn(new Application());
    lenient().when(metadataService.getMetadataById(anyLong())).thenAnswer(invocation -> metadata(invocation.getArgument(0), "category"));
  }

  /**
   * The framework calls {@code init}: the annotation check kills the mutant
   * that removes {@code @PostConstruct}, which a direct call cannot see
   */
  @Test
  void testInitRegistersThePlugin() throws Exception {
    assertTrue(MyApplicationsApplicationSettingsPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    plugin.init();
    verify(applicationSettingsService).addPlugin(plugin);
  }

  @Test
  void testInitWithoutRegistry() {
    MyApplicationsApplicationSettingsPlugin standalone = new MyApplicationsApplicationSettingsPlugin();
    assertDoesNotThrow(standalone::init);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSchemaExposesTheDrawerOptions() {
    assertEquals("AppCenterMyApplicationsPortlet", plugin.getPortletName());
    assertTrue(plugin.isConfigurableInSiteContainer());
    Map<String, Object> properties = (Map<String, Object>) plugin.getSettingsSchema().get("properties");
    assertEquals(List.of("listing_mode",
                         "max_apps_to_list",
                         "selection_mode",
                         "application_ids",
                         "category_ids",
                         "show_header",
                         "header_title",
                         "template",
                         "show_see_more",
                         "list_in_row",
                         "show_icon",
                         "icon_size",
                         "show_name",
                         "color",
                         "outlined",
                         "h_align",
                         "v_align"),
                 List.copyOf(properties.keySet()));
    assertEquals(List.of("FAVORITES", "SELECTED"), ((Map<String, Object>) properties.get("listing_mode")).get("enum"));
    assertEquals(List.of("MANUAL", "CATEGORY"), ((Map<String, Object>) properties.get("selection_mode")).get("enum"));
    assertEquals(List.of("ROW", "CARDS", "COLUMN"), ((Map<String, Object>) properties.get("template")).get("enum"));
    assertEquals(List.of("START", "CENTER", "END"), ((Map<String, Object>) properties.get("h_align")).get("enum"));
    assertEquals(List.of("START", "CENTER", "END"), ((Map<String, Object>) properties.get("v_align")).get("enum"));
    Map<String, Object> maxApps = (Map<String, Object>) properties.get("max_apps_to_list");
    assertEquals(1, maxApps.get("minimum"));
    assertEquals(100, maxApps.get("maximum"));
    Map<String, Object> iconSize = (Map<String, Object>) properties.get("icon_size");
    assertEquals(12, iconSize.get("minimum"));
    assertEquals(60, iconSize.get("maximum"));
    assertEquals("array", ((Map<String, Object>) properties.get("application_ids")).get("type"));
    assertEquals("array", ((Map<String, Object>) properties.get("category_ids")).get("type"));
    assertEquals("string", ((Map<String, Object>) properties.get("color")).get("type"));
    Map<String, Object> header = (Map<String, Object>) properties.get("header_title");
    assertEquals(255, ((Map<String, Object>) header.get("additionalProperties")).get("maxLength"));
    assertEquals(false, plugin.getSettingsSchema().get("additionalProperties"));
  }

  @Test
  void testReadAppliesTheViewDefaults() throws Exception {
    Map<String, Object> settings = plugin.read(context(Map.of(), false));
    assertEquals("FAVORITES", settings.get("listing_mode"));
    assertEquals(4, settings.get("max_apps_to_list"));
    assertEquals("MANUAL", settings.get("selection_mode"));
    assertEquals(List.of(), settings.get("application_ids"));
    assertEquals(List.of(), settings.get("category_ids"));
    assertEquals(true, settings.get("show_header"));
    assertEquals(Map.of(), settings.get("header_title"));
    assertTrue(settings.containsKey("template"));
    assertNull(settings.get("template"));
    assertTrue(settings.containsKey("show_icon"));
    assertNull(settings.get("show_icon"));
    assertNull(settings.get("icon_size"));
    assertNull(settings.get("color"));
    assertNull(settings.get("h_align"));
  }

  /**
   * A stored display setting the view cannot read takes the template's
   * default, as the view renders it
   */
  @Test
  void testReadReturnsTheStoredValuesAsTheViewReadsThem() throws Exception {
    storedHeader(SETTING_NAME, Map.of(Locale.ENGLISH, "Tools", Locale.FRENCH, "Outils"));
    Map<String, String> preferences = new HashMap<>(Map.of("name", SETTING_NAME,
                                                           "listingMode", "SELECTED",
                                                           "selectionMode", "CATEGORY",
                                                           "applicationIds", "3,1",
                                                           "categoryIds", "8",
                                                           "maxAppsToList", "12",
                                                           "showHeader", "false",
                                                           "template", "CARDS",
                                                           "showIcon", "false",
                                                           "iconSize", "99"));
    preferences.put("color", "#AABBCC");
    preferences.put("hAlign", "END");
    Map<String, Object> settings = plugin.read(context(preferences, false));
    assertEquals("SELECTED", settings.get("listing_mode"));
    assertEquals(12, settings.get("max_apps_to_list"));
    assertEquals("CATEGORY", settings.get("selection_mode"));
    assertEquals(List.of(3L, 1L), settings.get("application_ids"));
    assertEquals(List.of(8L), settings.get("category_ids"));
    assertEquals(false, settings.get("show_header"));
    assertEquals(Map.of("en", "Tools", "fr", "Outils"), settings.get("header_title"));
    assertEquals("CARDS", settings.get("template"));
    assertEquals(false, settings.get("show_see_more"));
    assertEquals(true, settings.get("list_in_row"));
    assertEquals(false, settings.get("show_icon"));
    assertEquals(45, settings.get("icon_size"));
    assertEquals(true, settings.get("show_name"));
    assertEquals("#AABBCC", settings.get("color"));
    assertEquals(false, settings.get("outlined"));
    assertEquals("END", settings.get("h_align"));
    assertEquals("CENTER", settings.get("v_align"));
  }

  /**
   * The header as the view shows it: the setting name's labels, else the
   * legacy key's the portlet moves at render, else the ones a copy carries
   */
  @Test
  void testReadHeaderFallsBackToTheLegacyKeyThenTheCarriedCopy() throws Exception {
    storedHeader(LEGACY_ID, Map.of(Locale.ENGLISH, "Legacy"));
    assertEquals(Map.of("en", "Legacy"),
                 plugin.read(context(Map.of("name", SETTING_NAME, "applicationId", LEGACY_ID), false)).get("header_title"));
    assertEquals(Map.of("fr", "Raccourcis"),
                 plugin.read(context(Map.of("data.init", payload(Map.of("fr", "Raccourcis", "", "x"), 1L)), false))
                       .get("header_title"));
    assertEquals(Map.of(), plugin.read(context(Map.of("data.init", "not json"), false)).get("header_title"));
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() {
    ApplicationSettingsContext context = context(Map.of("template", "ROW"), false);
    assertRefused(context, Map.of("listing_mode", "ALL"), "value is not accepted for listing_mode");
    assertRefused(context, Map.of("selection_mode", "manual"), "value is not accepted for selection_mode");
    assertRefused(context, Map.of("max_apps_to_list", 0), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", 101), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", 2.5), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", "5"), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("application_ids", 7), "value is not accepted for application_ids");
    assertRefused(context, Map.of("application_ids", List.of(7, 7)), "value is not accepted for application_ids");
    assertRefused(context, Map.of("application_ids", List.of(0)), "value is not accepted for application_ids");
    assertRefused(context, Map.of("application_ids", List.of("x")), "value is not accepted for application_ids");
    assertRefused(context,
                  Map.of("application_ids", java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList()),
                  "value is not accepted for application_ids");
    assertRefused(context, Map.of("category_ids", List.of(2.5)), "value is not accepted for category_ids");
    assertRefused(context, Map.of("show_header", "false"), "value is not accepted for show_header");
    assertRefused(context, Map.of("template", "List"), "value is not accepted for template");
    assertRefused(context, Map.of("show_icon", "true"), "value is not accepted for show_icon");
    assertRefused(context, Map.of("icon_size", 11), "value is not accepted for icon_size");
    assertRefused(context, Map.of("icon_size", 61), "value is not accepted for icon_size");
    assertRefused(context, Map.of("color", "red"), "value is not accepted for color");
    assertRefused(context, Map.of("color", "#ABC"), "value is not accepted for color");
    assertRefused(context, Map.of("color", 0), "value is not accepted for color");
    assertRefused(context, Map.of("h_align", "LEFT"), "value is not accepted for h_align");
    assertRefused(context, Map.of("v_align", true), "value is not accepted for v_align");
    assertRefused(context, Map.of("view_template", "List"), "value is not accepted for view_template");
    assertRefused(context, Map.of("header_title", Map.of("en", "x".repeat(256))), "value is not accepted for header_title");
    assertRefused(context,
                  Map.of("header_title", Map.of("en", "Tools", "de", "Werkzeuge")),
                  "value is not accepted for header_title");
    assertRefused(context, Map.of("header_title", Map.of("fr", "Outils")), "value is not accepted for header_title");
    assertRefused(context, Map.of("header_title", "Tools"), "value is not accepted for header_title");
  }

  @Test
  void testValidateAcceptsTheBounds() {
    ApplicationSettingsContext context = context(Map.of("template", "ROW"), false);
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("max_apps_to_list", 1)));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("max_apps_to_list", 100)));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("icon_size", 12, "color", "", "h_align", "START")));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("icon_size", 60, "color", "#aabbccdd", "v_align", "END")));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("header_title", Map.of("en", "x".repeat(255)))));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("category_ids", List.of(8, "9"))));
  }

  /**
   * The drawer offers the display options with a template only: refused on
   * the legacy layout, accepted with the template of the same call
   */
  @Test
  void testDisplayOptionsNeedATemplate() throws Exception {
    ApplicationSettingsContext legacy = context(Map.of(), false);
    assertRefused(legacy, Map.of("show_icon", true), "value is not accepted for show_icon");
    assertRefused(legacy, Map.of("icon_size", 30), "value is not accepted for icon_size");
    assertRefused(legacy, Map.of("color", ""), "value is not accepted for color");
    assertDoesNotThrow(() -> plugin.validate(legacy, Map.of("template", "ROW", "show_icon", true)));
    plugin.apply(legacy, Map.of("template", "ROW", "show_icon", true, "list_in_row", true));
    verify(applicationSettingsService).updatePreferences(legacy, Map.of("template", "ROW", "showIcon", "true"));
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  /**
   * A requested id the drawer's validation drops is refused, a stored id it
   * drops is written as dropped, and the listing is validated whole as the
   * drawer posts it, its rule naming the given option
   */
  @Test
  void testListingIsValidatedWholeAsTheDrawerPostsIt() throws Exception {
    when(applicationCenterStorage.getApplication(9L)).thenReturn(null);
    when(metadataService.getMetadataById(5L)).thenReturn(metadata(5L, "tags"));
    ApplicationSettingsContext favorites = context(Map.of(), false);
    assertRefused(favorites, Map.of("application_ids", List.of(1, 9)), "value is not accepted for application_ids");
    assertRefused(favorites, Map.of("category_ids", List.of(5)), "value is not accepted for category_ids");
    assertRefused(favorites, Map.of("listing_mode", "SELECTED"), "value is not accepted for listing_mode");
    assertRefused(favorites,
                  Map.of("listing_mode", "SELECTED", "selection_mode", "CATEGORY", "application_ids", List.of(1)),
                  "value is not accepted for listing_mode");
    assertDoesNotThrow(() -> plugin.validate(favorites, Map.of("listing_mode", "SELECTED", "application_ids", List.of(1))));

    ApplicationSettingsContext selected = context(Map.of("listingMode", "SELECTED", "applicationIds", "1,9"), false);
    assertRefused(selected, Map.of("selection_mode", "CATEGORY"), "value is not accepted for category_ids");
    assertRefused(selected, Map.of("application_ids", List.of()), "value is not accepted for application_ids");
    plugin.apply(selected, Map.of("application_ids", List.of(2, 1)));
    verify(applicationSettingsService).updatePreferences(selected, Map.of("applicationIds", "2,1"));

    ApplicationSettingsContext stale = context(Map.of("applicationIds", "1,9"), false);
    plugin.apply(stale, Map.of("listing_mode", "SELECTED"));
    verify(applicationSettingsService).updatePreferences(stale, Map.of("listingMode", "SELECTED", "applicationIds", "1"));
  }

  /**
   * The drawer saves the header title only while the header is shown: a
   * changed one is refused when the switch is off, whether stored or given
   * in the same call, and accepted when the same call turns it on
   */
  @Test
  void testChangedHeaderIsRefusedWhileTheHeaderIsHidden() {
    Map<String, Object> header = Map.of("en", "Tools");
    assertRefused(context(Map.of("showHeader", "false"), false),
                  Map.of("header_title", header),
                  "value is not accepted for header_title");
    assertRefused(context(Map.of(), false),
                  Map.of("show_header", false, "header_title", header),
                  "value is not accepted for header_title");
    assertDoesNotThrow(() -> plugin.validate(context(Map.of("showHeader", "false"), false),
                                             Map.of("show_header", true, "header_title", header)));
  }

  /**
   * The right is the drawer's, read from the live page of the context: the
   * window's setting name is client-writable and never consulted for it
   */
  @Test
  void testValidateRefusesAUserWhoMayNotEditThePage() throws Exception {
    when(settingsService.canEditPageWindowSettings(PAGE_REFERENCE, USERNAME)).thenReturn(false);
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("show_header", false)));
    assertEquals("not allowed: application", e.getMessage());
    assertThrows(IllegalAccessException.class, () -> plugin.apply(context, Map.of("show_header", false)));
    verify(settingsService, never()).canEditSettings(anyString(), anyString());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
  }

  @Test
  void testValidateRefusesABlankUser() {
    lenient().when(settingsService.canEditPageWindowSettings(PAGE_REFERENCE, "")).thenReturn(true);
    ApplicationSettingsContext context = context(Map.of(), false);
    context.setUsername("");
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("show_header", false)));
    assertEquals("not allowed: application", e.getMessage());
    verifyNoInteractions(cmsService);
  }

  @Test
  void testSiteContainerRightIsTheSiteEditRight() throws Exception {
    ApplicationSettingsContext context = siteContainer(Map.of());
    when(settingsService.canEditSiteLayoutWindowSettings(SiteKey.portal("classic"), USERNAME)).thenReturn(false);
    assertThrows(IllegalAccessException.class, () -> plugin.validate(context, Map.of("show_header", false)));
    verify(settingsService, never()).canEditPageWindowSettings(any(), any());
  }

  @Test
  void testValidateWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(Map.of("applicationIds", "1"), false);
    plugin.validate(context,
                    Map.of("listing_mode", "SELECTED", "header_title", Map.of("en", "Tools"), "template", "COLUMN", "max_apps_to_list", 9));
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(copyService, never()).copyListToSetting(anyMap(), anyString(), anyString());
  }

  /**
   * What the read returns is accepted back, unvalidated and unwritten: a
   * stored count outside the write's bounds, a legacy window, a templated
   * window with unset flags, a window carrying a copy whose list names the
   * source's applications
   */
  @Test
  void testReadIsAcceptedBackUnchanged() throws Exception {
    storedHeader(SETTING_NAME, Map.of(Locale.ENGLISH, "Tools"));
    lenient().when(applicationCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, "Untitled-1-source"));
    List<ApplicationSettingsContext> contexts = List.of(context(Map.of("name", SETTING_NAME,
                                                                       "maxAppsToList", "150",
                                                                       "showHeader", "false",
                                                                       "listingMode", "SELECTED",
                                                                       "applicationIds", "1,2"),
                                                                false),
                                                        context(Map.of("template", "COLUMN", "showIcon", "maybe"), false),
                                                        context(Map.of("data.init", payload(Map.of("en", "Carried"), 1L),
                                                                       "listingMode", "SELECTED",
                                                                       "applicationIds", "1,2"),
                                                                false),
                                                        siteContainer(Map.of("name", SETTING_NAME, "template", "ROW")));
    for (ApplicationSettingsContext context : contexts) {
      Map<String, Object> sent = new LinkedHashMap<>();
      plugin.read(context).forEach((option, value) -> {
        if (value != null) {
          sent.put(option, value);
        }
      });
      plugin.validate(context, sent);
      plugin.apply(context, sent);
    }
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
  }

  /**
   * The view parses the stored number as is and fails on what it cannot
   * parse, while the read shows its default: sending that default back
   * writes it, which repairs the window
   */
  @Test
  void testUnparseableStoredNumberIsRepairedByTheReadValue() throws Exception {
    ApplicationSettingsContext empty = context(Map.of("maxAppsToList", ""), false);
    plugin.apply(empty, Map.of("max_apps_to_list", plugin.read(empty).get("max_apps_to_list")));
    verify(applicationSettingsService).updatePreferences(empty, Map.of("maxAppsToList", "4"));

    ApplicationSettingsContext padded = context(Map.of("maxAppsToList", " 7"), false);
    plugin.apply(padded, Map.of("max_apps_to_list", plugin.read(padded).get("max_apps_to_list")));
    verify(applicationSettingsService).updatePreferences(padded, Map.of("maxAppsToList", "7"));

    ApplicationSettingsContext paddedOutOfRange = context(Map.of("maxAppsToList", " 150"), false);
    plugin.apply(paddedOutOfRange, Map.of("max_apps_to_list", plugin.read(paddedOutOfRange).get("max_apps_to_list")));
    verify(applicationSettingsService).updatePreferences(paddedOutOfRange, Map.of("maxAppsToList", "150"));
  }

  @Test
  void testDefaultSentBackToAWindowWithoutStoredNumberWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), false);
    plugin.apply(context, Map.of("max_apps_to_list", plugin.read(context).get("max_apps_to_list")));
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
  }

  /**
   * A stored translation in a language the platform no longer offers is
   * what the read returns: kept unchecked when another language changes,
   * refused when it is itself changed
   */
  @Test
  void testStoredLanguageNoLongerOfferedIsKeptWhenAnotherChanges() throws Exception {
    storedHeader(SETTING_NAME, Map.of(Locale.ENGLISH, "Tools", Locale.GERMAN, "Werkzeuge"));
    ApplicationSettingsContext context = owned(Map.of("name", SETTING_NAME));
    assertRefused(context,
                  Map.of("header_title", Map.of("en", "Tools", "de", "Apps")),
                  "value is not accepted for header_title");
    plugin.apply(context, Map.of("header_title", Map.of("en", "Apps", "de", "Werkzeuge")));
    verify(translationService).saveTranslationLabels(OBJECT_TYPE,
                                                     SETTING_NAME,
                                                     FIELD,
                                                     Map.of(Locale.ENGLISH, "Apps", Locale.GERMAN, "Werkzeuge"),
                                                     USERNAME,
                                                     false);
  }

  /**
   * The listing and display options are the window's own preferences,
   * written on the draft under the shared setting name, as the drawer does
   */
  @Test
  void testApplyWritesChangedPreferencesWithoutRekeying() throws Exception {
    when(applicationCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, SETTING_NAME));
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME, "template", "ROW", "showIcon", "true"), false);
    plugin.apply(context,
                 Map.of("listing_mode", "SELECTED",
                        "application_ids", List.of(1, 2),
                        "max_apps_to_list", 10,
                        "show_header", false,
                        "show_icon", true,
                        "icon_size", 20,
                        "color", "#AABBCC",
                        "h_align", "END"));
    verify(applicationSettingsService).updatePreferences(context,
                                                         Map.of("maxAppsToList", "10",
                                                                "listingMode", "SELECTED",
                                                                "applicationIds", "1,2",
                                                                "showHeader", "false",
                                                                "iconSize", "20",
                                                                "color", "#AABBCC",
                                                                "hAlign", "END"));
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
  }

  /**
   * A template change keeps the other display options as stored (D55);
   * "unchanged" is judged against the defaults of the template the view
   * will read them with
   */
  @Test
  void testTemplateChangeIsComparedAgainstTheNewTemplateDefaults() throws Exception {
    ApplicationSettingsContext toCards = context(Map.of("template", "ROW"), false);
    plugin.apply(toCards, Map.of("template", "CARDS", "show_icon", false, "list_in_row", true));
    verify(applicationSettingsService).updatePreferences(toCards, Map.of("template", "CARDS", "showIcon", "false"));

    ApplicationSettingsContext toColumn = context(Map.of("template", "ROW", "iconSize", "40"), false);
    plugin.apply(toColumn, Map.of("template", "COLUMN", "show_see_more", true, "h_align", "START", "icon_size", 40));
    verify(applicationSettingsService).updatePreferences(toColumn, Map.of("template", "COLUMN"));
  }

  /**
   * A draft window shares its setting name with the live window: the first
   * header write creates a setting on the live page, copies the header and
   * the applications the window created to it, saves the header as the user
   * under it, then marks the window owner, its list naming the copies
   */
  @Test
  @SuppressWarnings("unchecked")
  void testHeaderWriteRekeysAWindowThatDoesNotOwnItsSetting() throws Exception {
    lenient().when(applicationCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, SETTING_NAME));
    when(applicationCenterStorage.getApplication(41L)).thenReturn(contextApplication(41L, NEW_NAME));
    when(settingsService.getContextSpaceId(NEW_NAME, PAGE_REFERENCE)).thenReturn(5L);
    Map<String, String> stored = Map.of("name", SETTING_NAME,
                                        "applicationId", LEGACY_ID,
                                        "listingMode", "SELECTED",
                                        "applicationIds", "1,2");
    ApplicationSettingsContext context = context(stored, false);
    when(copyService.copyListToSetting(context.getPreferences(), NEW_NAME, USERNAME)).thenReturn(Map.of(1L, 41L));

    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools", "fr", "Outils"), "max_apps_to_list", 6));

    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    InOrder inOrder = inOrder(cmsService, copyService, translationService, applicationSettingsService);
    inOrder.verify(cmsService).saveSettingName(OBJECT_TYPE, NEW_NAME, PAGE_REFERENCE, 5L, 7L);
    inOrder.verify(copyService).copyListToSetting(stored, NEW_NAME, USERNAME);
    inOrder.verify(translationService)
           .saveTranslationLabels(OBJECT_TYPE, NEW_NAME, FIELD, Map.of(Locale.ENGLISH, "Tools", Locale.FRENCH, "Outils"), USERNAME, false);
    inOrder.verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertEquals(Map.of("maxAppsToList", "6", "applicationIds", "41,2", "name", NEW_NAME, "applicationId", ""),
                 preferences.getValue());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(eq(OBJECT_TYPE), eq(SETTING_NAME), anyString(), anyMap(), anyString(), anyBoolean());
  }

  /**
   * A new setting gets the window's list rewritten whole, so the header
   * write validates the stored listing as the drawer would post it, before
   * anything is created: a SELECTED listing whose apps are gone is refused
   * by validate, not by apply
   */
  @Test
  void testHeaderWriteValidatesTheStoredListingItRewrites() throws Exception {
    when(applicationCenterStorage.getApplication(9L)).thenReturn(null);
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME, "listingMode", "SELECTED", "applicationIds", "9"), false);
    assertRefused(context, Map.of("header_title", Map.of("en", "Tools")), "value is not accepted for application_ids");
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    ApplicationSettingsContext owned = owned(Map.of("name", SETTING_NAME, "listingMode", "SELECTED", "applicationIds", "9"));
    assertDoesNotThrow(() -> plugin.validate(owned, Map.of("header_title", Map.of("en", "Tools"))));
  }

  @Test
  void testHeaderWriteOnAnOwnedWindowWritesInPlace() throws Exception {
    ApplicationSettingsContext context = owned(Map.of("name", SETTING_NAME));
    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools"), "max_apps_to_list", 6));
    verify(translationService).saveTranslationLabels(OBJECT_TYPE, SETTING_NAME, FIELD, Map.of(Locale.ENGLISH, "Tools"), USERNAME, false);
    verify(applicationSettingsService).updatePreferences(context, Map.of("maxAppsToList", "6"));
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
  }

  /**
   * The marker alone owns nothing: a setting recorded on another page, or
   * gone, is re-keyed as a shared one would be
   */
  @Test
  void testMarkedWindowWhoseSettingHangsElsewhereIsRekeyed() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    when(settingsService.getSetting(SETTING_NAME)).thenReturn(new CMSSetting(OBJECT_TYPE, SETTING_NAME, OTHER_PAGE, 0));
    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools")));
    verify(cmsService).saveSettingName(eq(OBJECT_TYPE), eq(NEW_NAME), eq(PAGE_REFERENCE), anyLong(), eq(7L));
    verify(applicationSettingsService).markOwner(eq(context), anyMap());
  }

  /**
   * A window copied with no setting name carries the source's header and
   * applications in data.init, which the portlet's first render consumes:
   * the header write consumes it instead, into the setting it creates
   */
  @Test
  @SuppressWarnings("unchecked")
  void testHeaderWriteOnAnUnkeyedWindowKeysItAndConsumesTheCopy() throws Exception {
    when(applicationCenterStorage.getApplication(41L)).thenReturn(contextApplication(41L, NEW_NAME));
    String payload = payload(Map.of("en", "Carried"), 1L);
    ApplicationSettingsContext context = context(Map.of("data.init", payload, "listingMode", "SELECTED", "applicationIds", "1,2"),
                                                 false);
    when(copyService.copyListToSetting(context.getPreferences(), NEW_NAME, USERNAME)).thenReturn(Map.of(1L, 41L));

    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools")));

    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    verify(cmsService).saveSettingName(eq(OBJECT_TYPE), eq(NEW_NAME), eq(PAGE_REFERENCE), anyLong(), eq(7L));
    verify(translationService).saveTranslationLabels(OBJECT_TYPE, NEW_NAME, FIELD, Map.of(Locale.ENGLISH, "Tools"), USERNAME, false);
    verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertEquals(Map.of("applicationIds", "41,2", "name", NEW_NAME, "data.init", ""), preferences.getValue());
  }

  /**
   * A refused copy drops its id from the list the new setting gets, as the
   * portlet's render does
   */
  @Test
  @SuppressWarnings("unchecked")
  void testRekeyDropsAnApplicationWhoseCopyWasRefused() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME, "listingMode", "SELECTED", "applicationIds", "1,2"), false);
    Map<Long, Long> created = new HashMap<>();
    created.put(1L, null);
    when(copyService.copyListToSetting(context.getPreferences(), NEW_NAME, USERNAME)).thenReturn(created);

    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools")));

    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertEquals("2", preferences.getValue().get("applicationIds"));
  }

  /**
   * The applications a window carries in its copy exist once the copy is
   * consumed: the first write of any option on such a window creates its
   * setting and consumes the copy, the applications the list names created
   * as the new setting's, where the portlet's render would create them; a
   * carried application is listable, another setting's never
   */
  @Test
  @SuppressWarnings("unchecked")
  void testWriteOnAWindowCarryingACopyKeysItAndConsumesTheCopy() throws Exception {
    lenient().when(applicationCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, "Untitled-1-source"));
    when(applicationCenterStorage.getApplication(9L)).thenReturn(contextApplication(9L, "Untitled-1-source"));
    lenient().when(applicationCenterStorage.getApplication(41L)).thenReturn(contextApplication(41L, NEW_NAME));
    String payload = payload(Map.of("en", "Carried"), 1L);
    ApplicationSettingsContext context = context(Map.of("data.init", payload, "applicationIds", "1"), false);
    assertRefused(context, Map.of("listing_mode", "SELECTED", "application_ids", List.of(9)), "value is not accepted for application_ids");
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("listing_mode", "SELECTED", "application_ids", List.of(1))));
    when(copyService.copyListToSetting(anyMap(), eq(NEW_NAME), eq(USERNAME))).thenReturn(Map.of(1L, 41L));

    plugin.apply(context, Map.of("template", "ROW", "listing_mode", "SELECTED", "application_ids", List.of(2, 1)));

    ArgumentCaptor<Map<String, String>> source = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    InOrder inOrder = inOrder(cmsService, copyService, applicationSettingsService);
    inOrder.verify(cmsService).saveSettingName(eq(OBJECT_TYPE), eq(NEW_NAME), eq(PAGE_REFERENCE), anyLong(), eq(7L));
    inOrder.verify(copyService).copyListToSetting(source.capture(), eq(NEW_NAME), eq(USERNAME));
    inOrder.verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertEquals("2,1", source.getValue().get("applicationIds"));
    assertEquals(payload, source.getValue().get("data.init"));
    assertEquals(Map.of("template", "ROW", "listingMode", "SELECTED", "applicationIds", "2,41", "name", NEW_NAME, "data.init", ""),
                 preferences.getValue());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
  }

  /**
   * A never-rendered window carries nothing to consume: its preferences are
   * written and the portlet names it at its first render
   */
  @Test
  void testWriteOnANeverRenderedWindowWritesItsPreferencesOnly() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), false);
    plugin.apply(context, Map.of("listing_mode", "SELECTED", "application_ids", List.of(2)));
    verify(applicationSettingsService).updatePreferences(context, Map.of("listingMode", "SELECTED", "applicationIds", "2"));
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(copyService, never()).copyListToSetting(anyMap(), anyString(), anyString());
  }

  /**
   * In a site container the plain options are the site draft's own
   * preferences; the header, keyed by a setting hanging on a page, is not
   * configurable there, an unchanged one accepted back (D53)
   */
  @Test
  void testSiteContainerWritesPlainOptionsAndRefusesAChangedHeader() throws Exception {
    storedHeader(SETTING_NAME, Map.of(Locale.ENGLISH, "Tools"));
    ApplicationSettingsContext context = siteContainer(Map.of("name", SETTING_NAME, "showHeader", "false"));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                              () -> plugin.validate(context,
                                                                    Map.of("show_header", true, "header_title", Map.of("en", "Apps"))));
    assertEquals("not configurable in a site container: header_title", e.getMessage());
    plugin.apply(context, Map.of("show_header", true, "header_title", Map.of("en", "Tools"), "template", "COLUMN"));
    verify(applicationSettingsService).updatePreferences(context, Map.of("showHeader", "true", "template", "COLUMN"));
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
  }

  /**
   * A site container has no page to hang a setting on: a window carrying a
   * copy there keeps it for the site's first render, its carried
   * applications listable meanwhile, and nothing is created
   */
  @Test
  void testSiteContainerWindowCarryingACopyIsNotKeyed() throws Exception {
    lenient().when(applicationCenterStorage.getApplication(1L)).thenReturn(contextApplication(1L, "Untitled-1-source"));
    String payload = payload(Map.of("en", "Carried"), 1L);
    ApplicationSettingsContext context = siteContainer(Map.of("data.init", payload, "listingMode", "SELECTED", "applicationIds", "1"));

    plugin.apply(context, Map.of("template", "ROW", "application_ids", List.of(1, 2)));

    verify(applicationSettingsService).updatePreferences(context, Map.of("template", "ROW", "applicationIds", "1,2"));
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(copyService, never()).copyListToSetting(anyMap(), anyString(), anyString());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  private void storedHeader(String key, Map<Locale, String> labels) throws Exception {
    TranslationField field = new TranslationField();
    field.setLabels(new LinkedHashMap<>(labels));
    lenient().when(translationService.getTranslationField(OBJECT_TYPE, key, FIELD)).thenReturn(field);
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private ApplicationSettingsContext context(Map<String, String> preferences, boolean owner) {
    ApplicationSettingsContext context = new ApplicationSettingsContext(STORAGE_ID,
                                                                        "app-center/AppCenterMyApplicationsPortlet",
                                                                        new HashMap<>(preferences),
                                                                        false,
                                                                        true,
                                                                        null,
                                                                        SiteKey.portal("classic"),
                                                                        PAGE_REFERENCE,
                                                                        0,
                                                                        USERNAME);
    lenient().when(applicationSettingsService.isOwner(context)).thenReturn(owner);
    return context;
  }

  /**
   * A window owning its setting: marked, and the setting recorded on the
   * context's live page
   */
  private ApplicationSettingsContext owned(Map<String, String> preferences) {
    ApplicationSettingsContext context = context(preferences, true);
    String name = preferences.get("name");
    lenient().when(settingsService.getSetting(name)).thenReturn(new CMSSetting(OBJECT_TYPE, name, PAGE_REFERENCE, 0));
    return context;
  }

  private ApplicationSettingsContext siteContainer(Map<String, String> preferences) {
    ApplicationSettingsContext context = context(preferences, false);
    context.setSiteContainer(true);
    context.setPageReference(null);
    return context;
  }

  private Application contextApplication(long id, String owner) {
    Application application = new Application();
    application.setId(id);
    application.setContextSettingName(owner);
    return application;
  }

  private String payload(Map<String, String> headerTitles, long... applicationIds) {
    List<ApplicationCopy> applications = Arrays.stream(applicationIds).mapToObj(id -> {
      ApplicationCopy copy = new ApplicationCopy();
      copy.setId(id);
      copy.setTitle("Wiki " + id);
      return copy;
    }).toList();
    return JsonUtils.toJsonString(new ApplicationListCopy(headerTitles, applications));
  }

  private static Metadata metadata(long id, String type) {
    Metadata metadata = new Metadata();
    metadata.setId(id);
    metadata.setType(new MetadataType(1L, type));
    return metadata;
  }

  private LocaleConfig localeConfig(Locale locale) {
    LocaleConfig config = mock(LocaleConfig.class);
    lenient().when(config.getLocale()).thenReturn(locale);
    return config;
  }

}
