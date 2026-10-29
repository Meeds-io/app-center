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
import static io.meeds.appcenter.util.ApplicationSettingsUtils.enumSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.getLanguages;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.idsSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.integerSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.notAccepted;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.objectSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.stringSchema;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toBoolean;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toIds;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toInt;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toLanguageLabels;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toLocaleLabels;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toText;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toTranslations;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.toValue;
import static io.meeds.appcenter.util.ApplicationSettingsUtils.translationsSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.model.ApplicationCopy;
import io.meeds.appcenter.model.ApplicationListCopy;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;

/**
 * Configures a Shortcuts window (AppCenterMyApplicationsPortlet) as its
 * settings drawer does: every option of the drawer, validated by
 * {@link MyApplicationsSettingsService#getWritableSettings} as the drawer's
 * own save is, under the right that save has, computed from the live page
 * holding the window or, in a site container, from the site the context
 * names, a draft site on a draft, whose live site's edit right the layout
 * tool checks first; never from a window preference. The listing
 * and display options are the window's preferences; the header title is a
 * translation keyed by the window's CMS setting name, which a draft shares
 * with the live window: the first header write on a window that does not
 * own its setting creates one for it on the live page and copies the header
 * and the applications the window created to it through
 * {@link MyApplicationsCopyService}, as a copy of the window carries them.
 * A window copied with no setting name carries them in its data.init
 * preference, which a render of the portlet consumes: the first write of any
 * option on such a window creates its setting and consumes the copy into it,
 * as the render would, so that the applications it lists exist. In a site
 * container, which has no page to hang a setting on, the plain options are
 * written on the site's windows, a copy stays for a render of the site to
 * consume, and the header is not configurable.
 */
@Component
@Profile("layout")
public class MyApplicationsApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String               PORTLET_NAME              = "AppCenterMyApplicationsPortlet";

  public static final String               LISTING_MODE              = "listing_mode";

  public static final String               MAX_APPS_TO_LIST          = "max_apps_to_list";

  public static final String               SELECTION_MODE            = "selection_mode";

  public static final String               APPLICATION_IDS           = "application_ids";

  public static final String               CATEGORY_IDS              = "category_ids";

  public static final String               SHOW_HEADER               = "show_header";

  public static final String               HEADER_TITLE              = "header_title";

  public static final String               TEMPLATE                  = "template";

  public static final String               SHOW_SEE_MORE             = "show_see_more";

  public static final String               LIST_IN_ROW               = "list_in_row";

  public static final String               SHOW_ICON                 = "show_icon";

  public static final String               ICON_SIZE                 = "icon_size";

  public static final String               SHOW_NAME                 = "show_name";

  public static final String               COLOR                     = "color";

  public static final String               OUTLINED                  = "outlined";

  public static final String               H_ALIGN                   = "h_align";

  public static final String               V_ALIGN                   = "v_align";

  public static final String               NAME_PREFERENCE           = MyApplicationsCopyService.NAME_PREFERENCE;

  public static final String               APPLICATION_ID_PREFERENCE = MyApplicationsCopyService.APPLICATION_ID_PREFERENCE;

  public static final String               DATA_INIT_PREFERENCE      = MyApplicationsCopyService.DATA_INIT_PREFERENCE;

  public static final int                  DEFAULT_APPS_TO_LIST      = 4;

  public static final int                  HEADER_MAX_LENGTH         = 255;

  private static final int                 COLOR_MAX_LENGTH          = 9;

  private static final List<String>        LISTING_MODES             =
                                                         List.of(MyApplicationsSettingsService.LISTING_MODE_FAVORITES,
                                                                 MyApplicationsSettingsService.LISTING_MODE_SELECTED);

  private static final List<String>        SELECTION_MODES           =
                                                           List.of(MyApplicationsSettingsService.SELECTION_MODE_MANUAL,
                                                                   MyApplicationsSettingsService.SELECTION_MODE_CATEGORY);

  private static final List<String>        TEMPLATES                 = List.of(MyApplicationsSettingsService.TEMPLATE_ROW,
                                                                               MyApplicationsSettingsService.TEMPLATE_CARDS,
                                                                               MyApplicationsSettingsService.TEMPLATE_COLUMN);

  private static final List<String>        ALIGNMENTS                = List.of(MyApplicationsSettingsService.ALIGN_START,
                                                                               MyApplicationsSettingsService.ALIGN_CENTER,
                                                                               MyApplicationsSettingsService.ALIGN_END);

  /** The options the drawer's listing step saves together */
  private static final List<String>        LISTING_OPTIONS           = List.of(LISTING_MODE,
                                                                               SELECTION_MODE,
                                                                               APPLICATION_IDS,
                                                                               CATEGORY_IDS);

  /** The options a template reads, each with its stored preference */
  private static final Map<String, String> DISPLAY_OPTIONS           = displayOptions();

  /** Every option with its stored preference, the header title apart */
  private static final Map<String, String> PREFERENCES               = preferences();

  private static final List<String>        OPTIONS                   = options();

  @Autowired
  private MyApplicationsSettingsService    settingsService;

  @Autowired
  private MyApplicationsCopyService        copyService;

  @Autowired
  private CMSService                       cmsService;

  @Autowired
  private TranslationService               translationService;

  @Autowired
  private LocaleConfigService              localeConfigService;

  @Autowired
  private IdentityManager                  identityManager;

  @Autowired(required = false)
  private ApplicationSettingsService       applicationSettingsService;

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
    properties.put(LISTING_MODE,
                   enumSchema(LISTING_MODES,
                              "FAVORITES lists the apps each viewer marked as favorite, SELECTED the apps or the categories selected here"));
    properties.put(MAX_APPS_TO_LIST,
                   integerSchema(MyApplicationsSettingsService.MIN_APPS_TO_LIST,
                                 MyApplicationsSettingsService.MAX_APPS_TO_LIST_LIMIT,
                                 "Number of favorite apps listed, read in the FAVORITES listing mode"));
    properties.put(SELECTION_MODE,
                   enumSchema(SELECTION_MODES,
                              "In the SELECTED listing mode, MANUAL lists application_ids and CATEGORY the apps of category_ids"));
    properties.put(APPLICATION_IDS,
                   idsSchema("Ids of the apps listed in the SELECTED MANUAL mode, in order, each once, at most "
                       + ApplicationCenterService.MAX_LISTED_APPLICATIONS
                       + ": existing apps of the Application Center or created from this list; [] empties the list"));
    properties.put(CATEGORY_IDS,
                   idsSchema("Ids of the categories whose apps are listed in the SELECTED CATEGORY mode, each once, at most "
                       + MyApplicationsSettingsService.MAX_LISTED_CATEGORIES + ": existing categories; [] empties the list"));
    properties.put(SHOW_HEADER, booleanSchema("Shows the header; false hides it"));
    properties.put(HEADER_TITLE,
                   translationsSchema(HEADER_MAX_LENGTH,
                                      "Header title per language tag, replacing every language, required in the default language,"
                                          + " an empty text removing a language; only while the header is shown;"
                                          + " not configurable in a site container"));
    properties.put(TEMPLATE,
                   enumSchema(TEMPLATES,
                              "How the apps are displayed; absent, the legacy layout, which no template replaces until one is set."
                                  + " Setting a template keeps the other display options as stored, where the drawer's pick"
                                  + " resets them to the template's defaults"));
    properties.put(SHOW_SEE_MORE, booleanSchema("Shows 'see more'; the display options need a template"));
    properties.put(LIST_IN_ROW, booleanSchema("Keeps the apps on one line; a COLUMN template lists one app per line whatever it says"));
    properties.put(SHOW_ICON, booleanSchema("Shows the apps' icons"));
    properties.put(ICON_SIZE,
                   integerSchema(MyApplicationsSettingsService.MIN_ICON_SIZE,
                                 MyApplicationsSettingsService.MAX_ICON_SIZE,
                                 "Icon size in pixels, kept while the icons are hidden"));
    properties.put(SHOW_NAME, booleanSchema("Shows the apps' names"));
    properties.put(COLOR,
                   stringSchema("Color of the tiles, #RRGGBB or #RRGGBBAA; an empty text removes it: a transparent tile,"
                       + " with the skin's border color when outlined"));
    properties.put(OUTLINED, booleanSchema("Borders the tiles with the color instead of filling them"));
    properties.put(H_ALIGN,
                   enumSchema(ALIGNMENTS, "Horizontal alignment of a tile's content, START being the left in a left-to-right page"));
    properties.put(V_ALIGN, enumSchema(ALIGNMENTS, "Vertical alignment of a tile's content, START being the top"));
    return objectSchema(properties);
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    Map<String, String> stored = context.getPreferences();
    Map<String, Object> settings = new LinkedHashMap<>();
    settings.put(LISTING_MODE, MyApplicationsSettingsService.getListingMode(stored.get(MyApplicationsSettingsService.LISTING_MODE)));
    settings.put(MAX_APPS_TO_LIST, readMaxAppsToList(stored));
    settings.put(SELECTION_MODE,
                 MyApplicationsSettingsService.getSelectionMode(stored.get(MyApplicationsSettingsService.SELECTION_MODE)));
    settings.put(APPLICATION_IDS,
                 MyApplicationsSettingsService.getApplicationIds(stored.get(MyApplicationsSettingsService.APPLICATION_IDS)));
    settings.put(CATEGORY_IDS, MyApplicationsSettingsService.getCategoryIds(stored.get(MyApplicationsSettingsService.CATEGORY_IDS)));
    settings.put(SHOW_HEADER, readShowHeader(stored));
    settings.put(HEADER_TITLE, readHeader(stored));
    settings.put(TEMPLATE, MyApplicationsSettingsService.getTemplate(stored.get(MyApplicationsSettingsService.TEMPLATE)));
    Map<String, Object> display = MyApplicationsSettingsService.getDisplaySettings(stored);
    DISPLAY_OPTIONS.forEach((option, preference) -> settings.put(option, display == null ? null : display.get(preference)));
    return settings;
  }

  /**
   * Nothing to write, as {@link #apply} judges it, is accepted whatever the
   * window. A window given a new setting is then validated under that
   * setting's listability: none of the applications the old setting created,
   * but those a copy of the window would carry to it for the list the caller
   * gives, which {@link MyApplicationsCopyService#getCopyPreferences}
   * previews with no write; a copy refused at creation stays beyond the
   * preview.
   */
  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    requireEditor(context);
    requireKnownOptions(settings, OPTIONS);
    Map<String, String> stored = context.getPreferences();
    Map<String, String> header = toHeader(context, settings);
    String name = stored.get(NAME_PREFERENCE);
    Map<String, String> preferences = toPreferences(context, settings, name, null, getCarriedIds(stored), false);
    if (header == null && preferences.isEmpty()) {
      return;
    }
    if (needsSetting(context, header, name)) {
      Map<String, String> preview = copyService.getCopyPreferences(toCopySource(stored, settings), false, context.getUsername());
      toPreferences(context, settings, null, null, getCarriedIds(preview), true);
    }
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    requireEditor(context);
    requireKnownOptions(settings, OPTIONS);
    Map<String, String> stored = context.getPreferences();
    Map<String, String> header = toHeader(context, settings);
    String name = stored.get(NAME_PREFERENCE);
    Map<String, String> preferences = toPreferences(context, settings, name, null, getCarriedIds(stored), false);
    if (header == null && preferences.isEmpty()) {
      return;
    } else if (!needsSetting(context, header, name)) {
      if (header != null) {
        saveHeader(name, header, context.getUsername());
      }
      if (!preferences.isEmpty()) {
        applicationSettingsService.updatePreferences(context, preferences);
      }
      return;
    }
    String newName = createSetting(context);
    Map<Long, Long> created = copyService.copyListToSetting(toCopySource(stored, settings), newName, context.getUsername());
    preferences = toPreferences(context, settings, newName, created, List.of(), true);
    if (header != null) {
      saveHeader(newName, header, context.getUsername());
    }
    preferences.put(NAME_PREFERENCE, newName);
    if (StringUtils.isNotBlank(stored.get(APPLICATION_ID_PREFERENCE))) {
      preferences.put(APPLICATION_ID_PREFERENCE, "");
    }
    if (carriesCopy(stored)) {
      preferences.put(DATA_INIT_PREFERENCE, "");
    }
    applicationSettingsService.markOwner(context, preferences);
  }

  /**
   * Whether a write gives the window a setting of its own on the live page:
   * a header write, or any write on a window carrying a copy, on a window
   * that does not own its setting. A site container has no page to hang a
   * setting on: its windows keep their preferences and their copy, which a
   * render of the site consumes.
   */
  private boolean needsSetting(ApplicationSettingsContext context, Map<String, String> header, String name) {
    return !context.isSiteContainer()
           && (header != null || carriesCopy(context.getPreferences()))
           && !isOwned(context, name);
  }

  /**
   * @return the window's preferences the copy to a new setting reads, its
   *         list being the one given, when one is: the copy carries the
   *         applications the list names
   */
  private Map<String, String> toCopySource(Map<String, String> stored, Map<String, Object> settings) {
    Map<String, String> source = new LinkedHashMap<>(stored);
    Object applicationIds = settings.get(APPLICATION_IDS);
    if (applicationIds != null) {
      source.put(MyApplicationsSettingsService.APPLICATION_IDS,
                 join(toBoundedIds(applicationIds, APPLICATION_IDS, ApplicationCenterService.MAX_LISTED_APPLICATIONS)));
    }
    return source;
  }

  /**
   * The drawer's own right, computed from the context: the page-window rule
   * on the live page holding the window, the site edit right in a site
   * container. The window's setting name is client-writable and grants
   * nothing.
   */
  private void requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    String username = context.getUsername();
    boolean allowed;
    if (StringUtils.isBlank(username)) {
      allowed = false;
    } else if (context.isSiteContainer()) {
      allowed = settingsService.canEditSiteLayoutWindowSettings(context.getSiteKey(), username);
    } else {
      allowed = settingsService.canEditPageWindowSettings(context.getPageReference(), username);
    }
    if (!allowed) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
  }

  /**
   * Checks the given options and computes the preferences to store, as the
   * drawer's save does: each given value against its vocabulary, a value
   * equal to the stored one, as the view reads it, left out unchecked, then
   * the changed ones through
   * {@link MyApplicationsSettingsService#getWritableSettings}, with the
   * whole listing section when one of its options changes, or when the
   * window is given a new setting, whose applications its list must name. A
   * changed option the service alters is refused; a stored value it drops is
   * written as dropped.
   *
   * @param settingName the setting the applications are listable by, null
   *          when the window is given one that does not exist yet
   * @param createdIds the applications created for a new setting by the id
   *          the list names, null while the window keeps its setting
   * @param carriedIds the applications listable although no setting lists
   *          them: those the window's copy carries, or those a copy to a new
   *          setting would carry
   * @param listingRewritten whether the listing section is validated and
   *          written whole, as on a new setting
   * @return the preferences to store, by name: the ones whose written value
   *         differs from the stored one as the view reads it
   */
  private Map<String, String> toPreferences(ApplicationSettingsContext context, // NOSONAR
                                            Map<String, Object> settings,
                                            String settingName,
                                            Map<Long, Long> createdIds,
                                            List<Long> carriedIds,
                                            boolean listingRewritten) {
    Map<String, String> stored = context.getPreferences();
    Map<String, String> preferences = new LinkedHashMap<>();
    Map<String, String> posted = new HashMap<>();
    putMaxAppsToList(stored, settings, preferences);
    putListingOptions(stored, settings, posted, createdIds, listingRewritten);
    putShowHeader(stored, settings, posted);
    putDisplayOptions(stored, settings, posted);
    if (posted.isEmpty()) {
      return preferences;
    }
    Map<String, String> writable = getWritableSettings(settingName, posted, settings, carriedIds);
    posted.forEach((preference, value) -> {
      String option = optionOf(preference);
      String written = writable.get(preference);
      if (isAltered(option, value, written, viewText(preference, stored))) {
        throw notAccepted(option);
      }
      if (written != null && !StringUtils.equals(written, viewText(preference, stored))) {
        preferences.put(preference, written);
      }
    });
    return preferences;
  }

  /**
   * @return whether the service altered what the caller changed: a value it
   *         rewrote, or, in a list, an id new against the read it dropped; a
   *         stored id it drops is written as dropped
   */
  private static boolean isAltered(String option, String posted, String written, String read) {
    if (StringUtils.equals(posted, written)) {
      return false;
    } else if (!APPLICATION_IDS.equals(option) && !CATEGORY_IDS.equals(option)) {
      return true;
    }
    List<Long> kept = MyApplicationsSettingsService.getApplicationIds(written);
    List<Long> stored = MyApplicationsSettingsService.getApplicationIds(read);
    return MyApplicationsSettingsService.getApplicationIds(posted)
                                        .stream()
                                        .anyMatch(id -> !kept.contains(id) && !stored.contains(id));
  }

  /**
   * @return the stored text of a preference as the view reads it, which a
   *         listing preference posted whole is compared with
   */
  private static String viewText(String preference, Map<String, String> stored) {
    String value = stored.get(preference);
    return switch (preference) {
    case MyApplicationsSettingsService.LISTING_MODE -> MyApplicationsSettingsService.getListingMode(value);
    case MyApplicationsSettingsService.SELECTION_MODE -> MyApplicationsSettingsService.getSelectionMode(value);
    case MyApplicationsSettingsService.APPLICATION_IDS -> join(MyApplicationsSettingsService.getApplicationIds(value));
    case MyApplicationsSettingsService.CATEGORY_IDS -> join(MyApplicationsSettingsService.getCategoryIds(value));
    default -> value;
    };
  }

  /**
   * A value equal to the read is kept unvalidated; the count is compared
   * with the stored text as the view parses it, so a stored value the view
   * cannot parse is written as read, which repairs the window
   */
  private void putMaxAppsToList(Map<String, String> stored, Map<String, Object> settings, Map<String, String> preferences) {
    Object value = settings.get(MAX_APPS_TO_LIST);
    int read = readMaxAppsToList(stored);
    if (value instanceof Number number && number.doubleValue() == read) {
      if (!isViewNumber(stored.get(MyApplicationsSettingsService.MAX_APPS_TO_LIST), read)) {
        preferences.put(MyApplicationsSettingsService.MAX_APPS_TO_LIST, String.valueOf(read));
      }
    } else if (value != null) {
      preferences.put(MyApplicationsSettingsService.MAX_APPS_TO_LIST,
                      String.valueOf(toInt(value,
                                           MAX_APPS_TO_LIST,
                                           MyApplicationsSettingsService.MIN_APPS_TO_LIST,
                                           MyApplicationsSettingsService.MAX_APPS_TO_LIST_LIMIT)));
    }
  }

  /**
   * Posts the changed listing options, and the stored values of the others
   * once one changes or the list is rewritten: the service validates the
   * listing whole, as the drawer posts it
   */
  private void putListingOptions(Map<String, String> stored,
                                 Map<String, Object> settings,
                                 Map<String, String> posted,
                                 Map<Long, Long> createdIds,
                                 boolean listingRewritten) {
    Map<String, String> read = new LinkedHashMap<>();
    read.put(LISTING_MODE, MyApplicationsSettingsService.getListingMode(stored.get(MyApplicationsSettingsService.LISTING_MODE)));
    read.put(SELECTION_MODE,
             MyApplicationsSettingsService.getSelectionMode(stored.get(MyApplicationsSettingsService.SELECTION_MODE)));
    read.put(APPLICATION_IDS,
             join(mapIds(MyApplicationsSettingsService.getApplicationIds(stored.get(MyApplicationsSettingsService.APPLICATION_IDS)),
                         createdIds)));
    read.put(CATEGORY_IDS,
             join(MyApplicationsSettingsService.getCategoryIds(stored.get(MyApplicationsSettingsService.CATEGORY_IDS))));
    Map<String, String> changed = new LinkedHashMap<>();
    for (String option : LISTING_OPTIONS) {
      Object value = settings.get(option);
      if (value == null) {
        continue;
      }
      String text = switch (option) {
      case LISTING_MODE -> toValue(value, option, LISTING_MODES);
      case SELECTION_MODE -> toValue(value, option, SELECTION_MODES);
      case APPLICATION_IDS -> join(mapIds(toBoundedIds(value, option, ApplicationCenterService.MAX_LISTED_APPLICATIONS), createdIds));
      default -> join(toBoundedIds(value, option, MyApplicationsSettingsService.MAX_LISTED_CATEGORIES));
      };
      if (!text.equals(read.get(option))) {
        changed.put(option, text);
      }
    }
    if (!changed.isEmpty() || listingRewritten) {
      LISTING_OPTIONS.forEach(option -> posted.put(PREFERENCES.get(option), changed.getOrDefault(option, read.get(option))));
    }
  }

  private void putShowHeader(Map<String, String> stored, Map<String, Object> settings, Map<String, String> posted) {
    Object value = settings.get(SHOW_HEADER);
    if (value != null && !value.equals(readShowHeader(stored))) {
      posted.put(MyApplicationsSettingsService.SHOW_HEADER, String.valueOf(toBoolean(value, SHOW_HEADER)));
    }
  }

  /**
   * The template, then each display option against the template the view
   * will read it with, the given one or the stored one: refused while none
   * is, as the drawer offers them with a template only
   */
  private void putDisplayOptions(Map<String, String> stored, Map<String, Object> settings, Map<String, String> posted) {
    String storedTemplate = MyApplicationsSettingsService.getTemplate(stored.get(MyApplicationsSettingsService.TEMPLATE));
    String template = storedTemplate;
    Object templateValue = settings.get(TEMPLATE);
    if (templateValue != null) {
      template = toValue(templateValue, TEMPLATE, TEMPLATES);
      if (!template.equals(storedTemplate)) {
        posted.put(MyApplicationsSettingsService.TEMPLATE, template);
      }
    }
    Map<String, Object> display = MyApplicationsSettingsService.getDisplaySettings(stored, template);
    for (Map.Entry<String, String> option : DISPLAY_OPTIONS.entrySet()) {
      Object value = settings.get(option.getKey());
      if (value == null) {
        continue;
      } else if (display == null) {
        throw notAccepted(option.getKey());
      }
      Object typed = toDisplayValue(option.getKey(), value);
      if (!typed.equals(display.get(option.getValue()))) {
        posted.put(option.getValue(), String.valueOf(typed));
      }
    }
  }

  private Object toDisplayValue(String option, Object value) {
    return switch (option) {
    case ICON_SIZE -> toInt(value,
                            option,
                            MyApplicationsSettingsService.MIN_ICON_SIZE,
                            MyApplicationsSettingsService.MAX_ICON_SIZE);
    case COLOR -> toText(value, option, COLOR_MAX_LENGTH);
    case H_ALIGN, V_ALIGN -> toValue(value, option, ALIGNMENTS);
    default -> toBoolean(value, option);
    };
  }

  /**
   * The drawer's own validation. The applications a window carries in its
   * copy, or those a copy to a new setting would carry, exist under a
   * setting only once the copy is made, by the write that gives the window
   * its setting or, otherwise, by a render of the portlet that consumes it:
   * until then they are listable, and the listing rule is checked over them
   * as it will be once they are created. A refusal of the listing whole
   * names the list of the selection mode when it is given, else the listing
   * mode when it is, else that list.
   *
   * @param carriedIds the applications listable although no setting lists
   *          them yet: those the window's copy carries, or those a copy to a
   *          new setting would carry; none once the copy is made
   */
  private Map<String, String> getWritableSettings(String settingName,
                                                  Map<String, String> posted,
                                                  Map<String, Object> settings,
                                                  List<Long> carriedIds) {
    List<Long> postedIds = MyApplicationsSettingsService.getApplicationIds(posted.get(MyApplicationsSettingsService.APPLICATION_IDS));
    if (carriedIds.isEmpty() || postedIds.stream().noneMatch(carriedIds::contains)) {
      try {
        return settingsService.getWritableSettings(settingName, posted);
      } catch (IllegalArgumentException e) {
        throw listingRefused(posted, settings);
      }
    }
    Map<String, String> stripped = new HashMap<>(posted);
    stripped.put(MyApplicationsSettingsService.LISTING_MODE, MyApplicationsSettingsService.LISTING_MODE_FAVORITES);
    stripped.put(MyApplicationsSettingsService.APPLICATION_IDS,
                 join(postedIds.stream().filter(id -> !carriedIds.contains(id)).toList()));
    Map<String, String> writable = settingsService.getWritableSettings(settingName, stripped);
    List<Long> listable = MyApplicationsSettingsService.getApplicationIds(writable.get(MyApplicationsSettingsService.APPLICATION_IDS));
    List<Long> kept = postedIds.stream().filter(id -> listable.contains(id) || carriedIds.contains(id)).toList();
    boolean manual = !MyApplicationsSettingsService.SELECTION_MODE_CATEGORY.equals(writable.get(MyApplicationsSettingsService.SELECTION_MODE));
    boolean selected = MyApplicationsSettingsService.LISTING_MODE_SELECTED.equals(posted.get(MyApplicationsSettingsService.LISTING_MODE));
    if (selected
        && ((manual && kept.isEmpty())
            || (!manual
                && MyApplicationsSettingsService.getCategoryIds(writable.get(MyApplicationsSettingsService.CATEGORY_IDS)).isEmpty()))) {
      throw listingRefused(posted, settings);
    }
    writable.put(MyApplicationsSettingsService.LISTING_MODE, posted.get(MyApplicationsSettingsService.LISTING_MODE));
    writable.put(MyApplicationsSettingsService.APPLICATION_IDS, join(kept));
    return writable;
  }

  private IllegalArgumentException listingRefused(Map<String, String> posted, Map<String, Object> settings) {
    boolean category = MyApplicationsSettingsService.SELECTION_MODE_CATEGORY.equals(posted.get(MyApplicationsSettingsService.SELECTION_MODE));
    String list = category ? CATEGORY_IDS : APPLICATION_IDS;
    return notAccepted(settings.containsKey(list) || !settings.containsKey(LISTING_MODE) ? list : LISTING_MODE);
  }

  /**
   * @return the header title to store, null when none is given or when it is
   *         the stored one; a changed one only while the header is shown, as
   *         the drawer saves it, and never in a site container, where the
   *         setting it is keyed by hangs on no page
   */
  private Map<String, String> toHeader(ApplicationSettingsContext context, Map<String, Object> settings) {
    Object value = settings.get(HEADER_TITLE);
    Map<String, String> stored = readHeader(context.getPreferences());
    if (value == null || value.equals(stored)) {
      return null; // NOSONAR
    } else if (context.isSiteContainer()) {
      throw new IllegalArgumentException(String.format(ApplicationSettingsService.NOT_CONFIGURABLE_IN_SITE_CONTAINER, HEADER_TITLE));
    }
    Object showHeader = settings.get(SHOW_HEADER);
    boolean shown = showHeader instanceof Boolean bool ? bool : readShowHeader(context.getPreferences());
    if (!shown) {
      throw notAccepted(HEADER_TITLE);
    }
    Map<String, String> header = toTranslations(value, HEADER_TITLE, HEADER_MAX_LENGTH, getLanguages(localeConfigService), stored);
    if (StringUtils.isBlank(header.get(getDefaultLanguage()))) {
      throw notAccepted(HEADER_TITLE);
    }
    return header;
  }

  /**
   * The window owns its setting when it carries the ownership marker naming
   * itself and the setting hangs on the live page holding the window
   */
  private boolean isOwned(ApplicationSettingsContext context, String name) {
    if (StringUtils.isBlank(name) || !applicationSettingsService.isOwner(context)) {
      return false;
    }
    CMSSetting setting = settingsService.getSetting(name);
    return setting != null && StringUtils.equals(setting.getPageReference(), context.getPageReference());
  }

  private String createSetting(ApplicationSettingsContext context) {
    String name;
    do {
      name = settingsService.generateSettingName(String.valueOf(context.getApplicationId()), WindowKind.PAGE);
    } while (cmsService.isSettingNameExists(MyApplicationsSettingsService.SETTING_TYPE, name));
    try {
      cmsService.saveSettingName(MyApplicationsSettingsService.SETTING_TYPE,
                                 name,
                                 context.getPageReference(),
                                 settingsService.getContextSpaceId(name, context.getPageReference()),
                                 Long.parseLong(identityManager.getOrCreateUserIdentity(context.getUsername()).getId()));
    } catch (ObjectAlreadyExistsException e) {
      throw new IllegalStateException(String.format("Shortcuts setting %s already exists", name), e);
    }
    return name;
  }

  private int readMaxAppsToList(Map<String, String> preferences) {
    return NumberUtils.toInt(StringUtils.trim(preferences.get(MyApplicationsSettingsService.MAX_APPS_TO_LIST)), DEFAULT_APPS_TO_LIST);
  }

  private boolean readShowHeader(Map<String, String> preferences) {
    String value = preferences.get(MyApplicationsSettingsService.SHOW_HEADER);
    return value == null || Boolean.parseBoolean(value);
  }

  /**
   * @return whether the view parses the stored text as the read number: when
   *         it does not, the read number sent back is written as is, which
   *         repairs the window without bounding a value it already holds
   */
  private boolean isViewNumber(String stored, int read) {
    try {
      return Integer.parseInt(stored == null ? String.valueOf(DEFAULT_APPS_TO_LIST) : stored) == read;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * @return the header title as the view shows it: the labels of the
   *         window's setting name, else of its legacy applicationId key, else
   *         the ones the copy it carries holds
   */
  private Map<String, String> readHeader(Map<String, String> preferences) {
    Map<String, String> header = getLabels(preferences.get(NAME_PREFERENCE));
    if (header.isEmpty()) {
      header = getLabels(preferences.get(APPLICATION_ID_PREFERENCE));
    }
    if (header.isEmpty()) {
      header = getCarriedHeader(preferences);
    }
    return header;
  }

  private Map<String, String> getCarriedHeader(Map<String, String> preferences) {
    Map<String, String> header = new LinkedHashMap<>();
    ApplicationListCopy carried = getCarriedCopy(preferences);
    if (carried != null && carried.getHeaderTitles() != null) {
      carried.getHeaderTitles().forEach((language, text) -> {
        if (StringUtils.isNotBlank(language) && StringUtils.isNotBlank(text)) {
          header.put(language, text);
        }
      });
    }
    return header;
  }

  private Map<String, String> getLabels(String key) {
    if (StringUtils.isBlank(key)) {
      return new LinkedHashMap<>();
    }
    try {
      TranslationField field = translationService.getTranslationField(MyApplicationsSettingsService.SETTING_TYPE,
                                                                      key,
                                                                      MyApplicationsSettingsService.HEADER_TITLE_FIELD);
      return field == null ? new LinkedHashMap<>() : toLanguageLabels(field.getLabels());
    } catch (ObjectNotFoundException e) {
      return new LinkedHashMap<>();
    }
  }

  private void saveHeader(String name, Map<String, String> header, String username) throws IllegalAccessException,
                                                                                     ObjectNotFoundException {
    translationService.saveTranslationLabels(MyApplicationsSettingsService.SETTING_TYPE,
                                             name,
                                             MyApplicationsSettingsService.HEADER_TITLE_FIELD,
                                             toLocaleLabels(header),
                                             username,
                                             false);
  }

  /**
   * @return the ids of the applications a window's data.init preference
   *         carries, none when it carries nothing readable
   */
  private List<Long> getCarriedIds(Map<String, String> preferences) {
    ApplicationListCopy carried = getCarriedCopy(preferences);
    if (carried == null || carried.getApplications() == null) {
      return List.of();
    }
    return carried.getApplications().stream().filter(Objects::nonNull).map(ApplicationCopy::getId).toList();
  }

  private ApplicationListCopy getCarriedCopy(Map<String, String> preferences) {
    if (!carriesCopy(preferences)) {
      return null;
    }
    try {
      return JsonUtils.fromJsonString(preferences.get(DATA_INIT_PREFERENCE), ApplicationListCopy.class);
    } catch (Exception e) { // NOSONAR the parser throws its checked exceptions unchecked
      return null;
    }
  }

  private static boolean carriesCopy(Map<String, String> preferences) {
    return StringUtils.isNotBlank(preferences.get(DATA_INIT_PREFERENCE));
  }

  private String getDefaultLanguage() {
    return localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag();
  }

  private static List<Long> toBoundedIds(Object value, String option, int maxIds) {
    List<Long> ids = toIds(value, option);
    if (ids.size() > maxIds) {
      throw notAccepted(option);
    }
    return ids;
  }

  /**
   * @return the ids with each one a new setting's copy replaces mapped to
   *         the copy, one the copy refused dropped
   */
  private static List<Long> mapIds(List<Long> ids, Map<Long, Long> createdIds) {
    if (createdIds == null || createdIds.isEmpty()) {
      return ids;
    }
    List<Long> mapped = new ArrayList<>();
    for (Long id : ids) {
      if (!createdIds.containsKey(id)) {
        mapped.add(id);
      } else if (createdIds.get(id) != null) {
        mapped.add(createdIds.get(id));
      }
    }
    return mapped;
  }

  private static String join(List<Long> ids) {
    return StringUtils.join(ids, ",");
  }

  private static String optionOf(String preference) {
    return PREFERENCES.entrySet()
                      .stream()
                      .filter(entry -> entry.getValue().equals(preference))
                      .map(Map.Entry::getKey)
                      .findFirst()
                      .orElse(preference);
  }

  private static Map<String, String> displayOptions() {
    Map<String, String> options = new LinkedHashMap<>();
    options.put(SHOW_SEE_MORE, MyApplicationsSettingsService.SHOW_SEE_MORE);
    options.put(LIST_IN_ROW, MyApplicationsSettingsService.LIST_IN_ROW);
    options.put(SHOW_ICON, MyApplicationsSettingsService.SHOW_ICON);
    options.put(ICON_SIZE, MyApplicationsSettingsService.ICON_SIZE);
    options.put(SHOW_NAME, MyApplicationsSettingsService.SHOW_NAME);
    options.put(COLOR, MyApplicationsSettingsService.COLOR);
    options.put(OUTLINED, MyApplicationsSettingsService.OUTLINED);
    options.put(H_ALIGN, MyApplicationsSettingsService.H_ALIGN);
    options.put(V_ALIGN, MyApplicationsSettingsService.V_ALIGN);
    return options;
  }

  private static Map<String, String> preferences() {
    Map<String, String> preferences = new LinkedHashMap<>();
    preferences.put(LISTING_MODE, MyApplicationsSettingsService.LISTING_MODE);
    preferences.put(MAX_APPS_TO_LIST, MyApplicationsSettingsService.MAX_APPS_TO_LIST);
    preferences.put(SELECTION_MODE, MyApplicationsSettingsService.SELECTION_MODE);
    preferences.put(APPLICATION_IDS, MyApplicationsSettingsService.APPLICATION_IDS);
    preferences.put(CATEGORY_IDS, MyApplicationsSettingsService.CATEGORY_IDS);
    preferences.put(SHOW_HEADER, MyApplicationsSettingsService.SHOW_HEADER);
    preferences.put(TEMPLATE, MyApplicationsSettingsService.TEMPLATE);
    preferences.putAll(DISPLAY_OPTIONS);
    return preferences;
  }

  private static List<String> options() {
    List<String> options = new ArrayList<>(PREFERENCES.keySet());
    options.add(HEADER_TITLE);
    return options;
  }

}
