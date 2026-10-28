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

import static io.meeds.appcenter.util.ApplicationSettingsUtils.booleanSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.getLanguages;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.integerSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.notAccepted;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.objectSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toBoolean;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toInt;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toLanguageLabels;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toLocaleLabels;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toTranslations;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.translationsSchema;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.resources.LocaleConfigService;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

import jakarta.annotation.PostConstruct;

/**
 * Configures a My Applications window as its settings drawer does: the
 * number of applications and the header switch are window preferences, the
 * header title a translation keyed by the window's {@code applicationId}
 * preference, and every change is an administrator's, as the portlet's
 * action is. A draft window shares its {@code applicationId} with the live
 * one, so the first header write on a window that does not own it creates a
 * new one for it. The translation hangs on no page: the window is
 * configurable in a site container too.
 */
@Component
@Profile("layout")
public class MyApplicationsApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String           PORTLET_NAME                 = "AppCenterMyApplicationsPortlet";

  public static final String           MAX_APPS_TO_LIST             = "max_apps_to_list";

  public static final String           SHOW_HEADER                  = "show_header";

  public static final String           HEADER_TITLE                 = "header_title";

  public static final String           APPLICATION_ID_PREFERENCE    = "applicationId";

  public static final String           HEADER_TITLE_FIELD           = "headerTitle";

  public static final int              MIN_APPS_TO_LIST             = 1;

  public static final int              MAX_APPS_TO_LIST_BOUND       = 100;

  public static final int              DEFAULT_APPS_TO_LIST         = 4;

  public static final int              HEADER_MAX_LENGTH            = 255;

  private static final String          MAX_APPS_TO_LIST_PREFERENCE  = "maxAppsToList";

  private static final String          SHOW_HEADER_PREFERENCE       = "showHeader";

  private static final List<String>    OPTIONS                      = Arrays.asList(MAX_APPS_TO_LIST,
                                                                                    SHOW_HEADER,
                                                                                    HEADER_TITLE);

  @Autowired
  private ApplicationCenterService     applicationCenterService;

  @Autowired
  private TranslationService           translationService;

  @Autowired
  private LocaleConfigService          localeConfigService;

  @Autowired(required = false)
  private ApplicationSettingsService   applicationSettingsService;

  @PostConstruct
  public void init() {
    if (applicationSettingsService != null) {
      applicationSettingsService.addPlugin(this);
    }
  }

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public boolean isConfigurableInSiteContainer() {
    return true;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(MAX_APPS_TO_LIST,
                   integerSchema(MIN_APPS_TO_LIST, MAX_APPS_TO_LIST_BOUND, "Number of applications listed"));
    properties.put(SHOW_HEADER, booleanSchema("Shows the header; false hides it"));
    properties.put(HEADER_TITLE,
                   translationsSchema(HEADER_MAX_LENGTH,
                                      "Header title per language tag, replacing every language, required in the default language; only while the header is shown"));
    return objectSchema(properties);
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    Map<String, String> preferences = context.getPreferences();
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(MAX_APPS_TO_LIST, readMaxAppsToList(preferences));
    settings.put(SHOW_HEADER, readShowHeader(preferences));
    settings.put(HEADER_TITLE, readHeader(preferences.get(APPLICATION_ID_PREFERENCE)));
    return settings;
  }

  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    requireEditor(context);
    toPreferences(context, settings);
    toHeader(context, settings);
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    requireEditor(context);
    Map<String, String> preferences = toPreferences(context, settings);
    Map<String, String> header = toHeader(context, settings);
    String applicationId = context.getPreferences().get(APPLICATION_ID_PREFERENCE);
    if (header == null) {
      if (!preferences.isEmpty()) {
        applicationSettingsService.updatePreferences(context, preferences);
      }
    } else if (applicationSettingsService.isOwner(context) && StringUtils.isNotBlank(applicationId)) {
      saveHeader(applicationId, header, context.getUsername());
      if (!preferences.isEmpty()) {
        applicationSettingsService.updatePreferences(context, preferences);
      }
    } else {
      String newApplicationId = createApplicationId();
      saveHeader(newApplicationId, header, context.getUsername());
      preferences.put(APPLICATION_ID_PREFERENCE, newApplicationId);
      applicationSettingsService.markOwner(context, preferences);
    }
  }

  /**
   * The portlet's action rule: only an administrator changes its settings.
   * {@link ApplicationCenterService#canEdit(String)} answers true for a
   * blank user name, which is therefore refused first.
   */
  private void requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    if (StringUtils.isBlank(context.getUsername()) || !applicationCenterService.canEdit(context.getUsername())) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
  }

  /**
   * @return the preferences to store for the given options, a value equal to
   *         the stored one left out unchecked, as the read shows it
   */
  private Map<String, String> toPreferences(ApplicationSettingsContext context, Map<String, Object> settings) {
    requireKnownOptions(settings, OPTIONS);
    Map<String, String> stored = context.getPreferences();
    Map<String, String> preferences = new LinkedHashMap<>();
    Object maxAppsToList = settings.get(MAX_APPS_TO_LIST);
    if (maxAppsToList != null && !isStoredNumber(maxAppsToList, stored.get(MAX_APPS_TO_LIST_PREFERENCE))) {
      preferences.put(MAX_APPS_TO_LIST_PREFERENCE,
                      String.valueOf(toInt(maxAppsToList, MAX_APPS_TO_LIST, MIN_APPS_TO_LIST, MAX_APPS_TO_LIST_BOUND)));
    }
    Object showHeader = settings.get(SHOW_HEADER);
    if (showHeader != null && !showHeader.equals(readShowHeader(stored))) {
      preferences.put(SHOW_HEADER_PREFERENCE, String.valueOf(toBoolean(showHeader, SHOW_HEADER)));
    }
    return preferences;
  }

  /**
   * @return the header title to store, null when none is given or when it is
   *         the stored one; a changed one only while the header is shown, as
   *         the drawer saves it
   */
  private Map<String, String> toHeader(ApplicationSettingsContext context, Map<String, Object> settings) {
    Object value = settings.get(HEADER_TITLE);
    Map<String, String> stored = readHeader(context.getPreferences().get(APPLICATION_ID_PREFERENCE));
    if (value == null || value.equals(stored)) {
      return null; // NOSONAR
    }
    Object showHeader = settings.get(SHOW_HEADER);
    boolean shown = showHeader instanceof Boolean bool ? bool : readShowHeader(context.getPreferences());
    if (!shown) {
      throw notAccepted(HEADER_TITLE);
    }
    Map<String, String> header = toTranslations(value,
                                                HEADER_TITLE,
                                                HEADER_MAX_LENGTH,
                                                getLanguages(localeConfigService),
                                                stored);
    if (StringUtils.isBlank(header.get(getDefaultLanguage()))) {
      throw notAccepted(HEADER_TITLE);
    }
    return header;
  }

  private int readMaxAppsToList(Map<String, String> preferences) {
    return NumberUtils.toInt(StringUtils.trim(preferences.get(MAX_APPS_TO_LIST_PREFERENCE)), DEFAULT_APPS_TO_LIST);
  }

  private boolean readShowHeader(Map<String, String> preferences) {
    String value = preferences.get(SHOW_HEADER_PREFERENCE);
    return value == null || Boolean.parseBoolean(value);
  }

  /**
   * @return whether the value is the stored number as the view parses it:
   *         a stored value the view cannot parse is never unchanged, so
   *         sending the read default repairs it
   */
  private boolean isStoredNumber(Object value, String stored) {
    try {
      return stored != null && value instanceof Number number && number.doubleValue() == Integer.parseInt(stored);
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private Map<String, String> readHeader(String applicationId) {
    if (StringUtils.isBlank(applicationId)) {
      return new LinkedHashMap<>();
    }
    try {
      TranslationField field = translationService.getTranslationField(MyApplicationsHeaderTranslationPlugin.MY_APPLICATIONS_OBJECT_TYPE,
                                                                      applicationId,
                                                                      HEADER_TITLE_FIELD);
      return field == null ? new LinkedHashMap<>() : toLanguageLabels(field.getLabels());
    } catch (ObjectNotFoundException e) {
      return new LinkedHashMap<>();
    }
  }

  private void saveHeader(String applicationId, Map<String, String> header, String username) throws IllegalAccessException,
                                                                                              ObjectNotFoundException {
    translationService.saveTranslationLabels(MyApplicationsHeaderTranslationPlugin.MY_APPLICATIONS_OBJECT_TYPE,
                                             applicationId,
                                             HEADER_TITLE_FIELD,
                                             toLocaleLabels(header),
                                             username,
                                             false);
  }

  /**
   * @return a new positive application id holding no header translation yet
   */
  private String createApplicationId() {
    String applicationId;
    do {
      applicationId = String.valueOf(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
    } while (!readHeader(applicationId).isEmpty());
    return applicationId;
  }

  private String getDefaultLanguage() {
    return localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag();
  }

}
