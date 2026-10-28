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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class MyApplicationsApplicationSettingsPluginTest {

  private static final String                     USERNAME       = "mary";

  private static final String                     OBJECT_TYPE    = "myApplicationsPortlet";

  private static final String                     FIELD          = "headerTitle";

  private static final String                     APPLICATION_ID = "77";

  @Mock
  private ApplicationCenterService                applicationCenterService;

  @Mock
  private TranslationService                      translationService;

  @Mock
  private LocaleConfigService                     localeConfigService;

  @Mock
  private ApplicationSettingsService              applicationSettingsService;

  @InjectMocks
  private MyApplicationsApplicationSettingsPlugin plugin;

  @BeforeEach
  void setUp() {
    LocaleConfig english = localeConfig(Locale.ENGLISH);
    LocaleConfig french = localeConfig(Locale.FRENCH);
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(Arrays.asList(english, french));
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(english);
    lenient().when(applicationCenterService.canEdit(USERNAME)).thenReturn(true);
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
    assertEquals(List.of("max_apps_to_list", "show_header", "header_title"), List.copyOf(properties.keySet()));
    Map<String, Object> maxApps = (Map<String, Object>) properties.get("max_apps_to_list");
    assertEquals(1, maxApps.get("minimum"));
    assertEquals(100, maxApps.get("maximum"));
    Map<String, Object> header = (Map<String, Object>) properties.get("header_title");
    assertEquals(255, ((Map<String, Object>) header.get("additionalProperties")).get("maxLength"));
    assertEquals(false, plugin.getSettingsSchema().get("additionalProperties"));
  }

  @Test
  void testReadAppliesTheViewDefaults() throws Exception {
    Map<String, Object> settings = plugin.read(context(Map.of(), false));
    assertEquals(4, settings.get("max_apps_to_list"));
    assertEquals(true, settings.get("show_header"));
    assertEquals(Map.of(), settings.get("header_title"));
  }

  @Test
  void testReadReturnsTheStoredValues() throws Exception {
    storedHeader(APPLICATION_ID, Map.of(Locale.ENGLISH, "Tools", Locale.FRENCH, "Outils"));
    Map<String, Object> settings = plugin.read(context(Map.of("maxAppsToList", "12",
                                                              "showHeader", "false",
                                                              "applicationId", APPLICATION_ID),
                                                       false));
    assertEquals(12, settings.get("max_apps_to_list"));
    assertEquals(false, settings.get("show_header"));
    assertEquals(Map.of("en", "Tools", "fr", "Outils"), settings.get("header_title"));
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() {
    ApplicationSettingsContext context = context(Map.of(), false);
    assertRefused(context, Map.of("max_apps_to_list", 0), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", 101), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", 2.5), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("max_apps_to_list", "5"), "value is not accepted for max_apps_to_list");
    assertRefused(context, Map.of("show_header", "false"), "value is not accepted for show_header");
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
    ApplicationSettingsContext context = context(Map.of(), false);
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("max_apps_to_list", 1)));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("max_apps_to_list", 100)));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("header_title", Map.of("en", "x".repeat(255)))));
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

  @Test
  void testValidateRefusesANonAdministrator() throws Exception {
    when(applicationCenterService.canEdit(USERNAME)).thenReturn(false);
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context(Map.of(), false), Map.of("show_header", false)));
    assertEquals("not allowed: application", e.getMessage());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
  }

  /**
   * {@link ApplicationCenterService#canEdit(String)} answers true for a
   * blank user name
   */
  @Test
  void testValidateRefusesABlankUser() {
    lenient().when(applicationCenterService.canEdit("")).thenReturn(true);
    ApplicationSettingsContext context = context(Map.of(), false);
    context.setUsername("");
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("show_header", false)));
    assertEquals("not allowed: application", e.getMessage());
  }

  @Test
  void testApplyRefusesANonAdministrator() throws Exception {
    when(applicationCenterService.canEdit(USERNAME)).thenReturn(false);
    assertThrows(IllegalAccessException.class,
                 () -> plugin.apply(context(Map.of(), false), Map.of("header_title", Map.of("en", "Tools"))));
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  /**
   * A stored value outside the write's vocabulary, and no header at all, are
   * what the read returns: sent back, they are accepted and nothing is
   * written
   */
  @Test
  void testReadIsAcceptedBackUnchanged() throws Exception {
    ApplicationSettingsContext context = context(Map.of("maxAppsToList", "150",
                                                        "showHeader", "false",
                                                        "applicationId", APPLICATION_ID),
                                                 false);
    Map<String, Object> read = plugin.read(context);
    assertEquals(150, read.get("max_apps_to_list"));
    plugin.validate(context, read);
    plugin.apply(context, read);

    storedHeader(APPLICATION_ID, Map.of(Locale.ENGLISH, "Tools"));
    read = plugin.read(context);
    plugin.validate(context, read);
    plugin.apply(context, read);

    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
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
    plugin.apply(paddedOutOfRange, plugin.read(paddedOutOfRange));
    verify(applicationSettingsService).updatePreferences(paddedOutOfRange, Map.of("maxAppsToList", "150"));
  }

  /**
   * The view reads an absent number as its default, as the read does:
   * sending that default back is unchanged and writes nothing
   */
  @Test
  void testDefaultSentBackToAWindowWithoutStoredNumberWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), false);
    plugin.apply(context, plugin.read(context));
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  /**
   * A stored translation in a language the platform no longer offers is
   * what the read returns: kept unchecked when another language changes,
   * refused when it is itself changed
   */
  @Test
  void testStoredLanguageNoLongerOfferedIsKeptWhenAnotherChanges() throws Exception {
    storedHeader(APPLICATION_ID, Map.of(Locale.ENGLISH, "Tools", Locale.GERMAN, "Werkzeuge"));
    ApplicationSettingsContext context = context(Map.of("applicationId", APPLICATION_ID), true);
    assertRefused(context,
                  Map.of("header_title", Map.of("en", "Tools", "de", "Apps")),
                  "value is not accepted for header_title");
    plugin.apply(context, Map.of("header_title", Map.of("en", "Apps", "de", "Werkzeuge")));
    verify(translationService).saveTranslationLabels(OBJECT_TYPE,
                                                     APPLICATION_ID,
                                                     FIELD,
                                                     Map.of(Locale.ENGLISH, "Apps", Locale.GERMAN, "Werkzeuge"),
                                                     USERNAME,
                                                     false);
  }

  @Test
  void testApplyWritesPreferencesWithoutRekeying() throws Exception {
    ApplicationSettingsContext context = context(Map.of("applicationId", APPLICATION_ID), false);
    plugin.apply(context, Map.of("max_apps_to_list", 10, "show_header", false));
    verify(applicationSettingsService).updatePreferences(context, Map.of("maxAppsToList", "10", "showHeader", "false"));
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), anyMap(), anyString(), anyBoolean());
  }

  /**
   * A draft window shares its applicationId, and so its header translation,
   * with the live window: the first header write gives it its own id, saves
   * the header as the user under it, then marks the window owner of it
   */
  @Test
  @SuppressWarnings("unchecked")
  void testHeaderWriteRekeysAWindowThatDoesNotOwnItsId() throws Exception {
    ApplicationSettingsContext context = context(Map.of("applicationId", APPLICATION_ID), false);
    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools", "fr", "Outils"), "max_apps_to_list", 6));

    ArgumentCaptor<String> applicationId = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    InOrder inOrder = inOrder(translationService, applicationSettingsService);
    inOrder.verify(translationService)
           .saveTranslationLabels(eq(OBJECT_TYPE),
                                  applicationId.capture(),
                                  eq(FIELD),
                                  eq(Map.of(Locale.ENGLISH, "Tools", Locale.FRENCH, "Outils")),
                                  eq(USERNAME),
                                  eq(false));
    inOrder.verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertNotEquals(APPLICATION_ID, applicationId.getValue());
    assertTrue(Long.parseLong(applicationId.getValue()) > 0);
    assertEquals(Map.of("maxAppsToList", "6", "applicationId", applicationId.getValue()), preferences.getValue());
    verify(applicationSettingsService, never()).updatePreferences(any(), anyMap());
  }

  @Test
  void testHeaderWriteOnAnOwnedWindowWritesInPlace() throws Exception {
    ApplicationSettingsContext context = context(Map.of("applicationId", APPLICATION_ID), true);
    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools"), "max_apps_to_list", 6));
    verify(translationService).saveTranslationLabels(OBJECT_TYPE,
                                                     APPLICATION_ID,
                                                     FIELD,
                                                     Map.of(Locale.ENGLISH, "Tools"),
                                                     USERNAME,
                                                     false);
    verify(applicationSettingsService).updatePreferences(context, Map.of("maxAppsToList", "6"));
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHeaderWriteOnANeverRenderedWindowCreatesItsId() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), true);
    plugin.apply(context, Map.of("header_title", Map.of("en", "Tools")));
    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    String applicationId = preferences.getValue().get("applicationId");
    assertTrue(Long.parseLong(applicationId) > 0);
    verify(translationService).saveTranslationLabels(OBJECT_TYPE,
                                                     applicationId,
                                                     FIELD,
                                                     Map.of(Locale.ENGLISH, "Tools"),
                                                     USERNAME,
                                                     false);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testHeaderWriteInASiteContainer() throws Exception {
    ApplicationSettingsContext context = context(Map.of("showHeader", "false", "applicationId", APPLICATION_ID), false);
    context.setSiteContainer(true);
    context.setPageReference(null);
    plugin.apply(context, Map.of("show_header", true, "header_title", Map.of("en", "Tools")));
    ArgumentCaptor<Map<String, String>> preferences = ArgumentCaptor.forClass(Map.class);
    verify(applicationSettingsService).markOwner(eq(context), preferences.capture());
    assertEquals("true", preferences.getValue().get("showHeader"));
    assertNotEquals(APPLICATION_ID, preferences.getValue().get("applicationId"));
  }

  private void storedHeader(String applicationId, Map<Locale, String> labels) throws Exception {
    TranslationField field = new TranslationField();
    field.setLabels(new LinkedHashMap<>(labels));
    when(translationService.getTranslationField(OBJECT_TYPE, applicationId, FIELD)).thenReturn(field);
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private ApplicationSettingsContext context(Map<String, String> preferences, boolean owner) {
    ApplicationSettingsContext context = new ApplicationSettingsContext(45,
                                                                        "app-center/AppCenterMyApplicationsPortlet",
                                                                        new HashMap<>(preferences),
                                                                        false,
                                                                        true,
                                                                        null,
                                                                        null,
                                                                        "portal::classic::home",
                                                                        0,
                                                                        USERNAME);
    lenient().when(applicationSettingsService.isOwner(context)).thenReturn(owner);
    return context;
  }

  private LocaleConfig localeConfig(Locale locale) {
    LocaleConfig config = mock(LocaleConfig.class);
    lenient().when(config.getLocale()).thenReturn(locale);
    return config;
  }

}
