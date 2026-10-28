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
package io.meeds.appcenter.util;

import static io.meeds.layout.service.ApplicationSettingsService.VALUE_NOT_ACCEPTED;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.resources.LocaleConfigService;

/**
 * Reads and checks the values of an application settings object as the
 * layout tools pass them: JSON values, every refusal being
 * {@code value is not accepted for <option>}
 */
public class ApplicationSettingsUtils {

  private ApplicationSettingsUtils() {
    // Utility class
  }

  public static IllegalArgumentException notAccepted(String option) {
    return new IllegalArgumentException(String.format(VALUE_NOT_ACCEPTED, option));
  }

  public static void requireKnownOptions(Map<String, Object> settings, Collection<String> options) {
    for (String option : settings.keySet()) {
      if (!options.contains(option)) {
        throw notAccepted(option);
      }
    }
  }

  public static boolean toBoolean(Object value, String option) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    throw notAccepted(option);
  }

  public static int toInt(Object value, String option, int min, int max) {
    if (value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue())) {
      double doubleValue = number.doubleValue();
      if (doubleValue >= min && doubleValue <= max) {
        return (int) doubleValue;
      }
    }
    throw notAccepted(option);
  }

  /**
   * @param  value     a JSON object of language tag to text
   * @param  option    the option holding it
   * @param  maxLength the longest text accepted
   * @param  languages the language tags the platform offers
   * @param  stored    the stored translations, an entry equal to one of
   *                     them accepted unchecked, as the read returned it
   * @return           the translations, an empty text dropped: an empty
   *                   text removes the translation of its language
   */
  public static Map<String, String> toTranslations(Object value,
                                                   String option,
                                                   int maxLength,
                                                   Collection<String> languages,
                                                   Map<String, String> stored) {
    if (!(value instanceof Map<?, ?> map)) {
      throw notAccepted(option);
    }
    Map<String, String> translations = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String language) || !(entry.getValue() instanceof String text)) {
        throw notAccepted(option);
      }
      if (!text.equals(stored.get(language)) && (!languages.contains(language) || text.length() > maxLength)) {
        throw notAccepted(option);
      }
      if (StringUtils.isNotBlank(text)) {
        translations.put(language, text);
      }
    }
    return translations;
  }

  public static Set<String> getLanguages(LocaleConfigService localeConfigService) {
    return localeConfigService.getLocalConfigs()
                              .stream()
                              .map(config -> config.getLocale().toLanguageTag())
                              .collect(Collectors.toSet());
  }

  public static Map<Locale, String> toLocaleLabels(Map<String, String> translations) {
    Map<Locale, String> labels = new LinkedHashMap<>();
    translations.forEach((language, text) -> labels.put(Locale.forLanguageTag(language), text));
    return labels;
  }

  public static Map<String, String> toLanguageLabels(Map<Locale, String> labels) {
    Map<String, String> translations = new LinkedHashMap<>();
    if (labels != null) {
      labels.forEach((locale, text) -> {
        if (StringUtils.isNotBlank(text)) {
          translations.put(locale.toLanguageTag(), text);
        }
      });
    }
    return translations;
  }

  public static Map<String, Object> objectSchema(Map<String, Object> properties) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("additionalProperties", false);
    return schema;
  }

  public static Map<String, Object> booleanSchema(String description) {
    return schema("boolean", description);
  }

  public static Map<String, Object> integerSchema(int min, int max, String description) {
    Map<String, Object> schema = schema("integer", description);
    schema.put("minimum", min);
    schema.put("maximum", max);
    return schema;
  }

  public static Map<String, Object> translationsSchema(int maxLength, String description) {
    Map<String, Object> schema = schema("object", description);
    Map<String, Object> text = schema("string", null);
    text.put("maxLength", maxLength);
    schema.put("additionalProperties", text);
    return schema;
  }

  private static Map<String, Object> schema(String type, String description) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", type);
    if (description != null) {
      schema.put("description", description);
    }
    return schema;
  }

}
