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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.layout.service.LayoutAclService;
import io.meeds.social.category.service.CategoryService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;

/**
 * Settings of the Shortcuts portlet (AppCenterMyApplicationsPortlet): who may
 * change them, which preferences a settings write may store, and the header
 * title keyed on the portlet's CMS setting name.
 */
@Service
public class MyApplicationsSettingsService {

  public static final String  SETTING_TYPE              = "myApplicationsPortlet";

  public static final String  HEADER_TITLE_FIELD        = "headerTitle";

  public static final String  MAX_APPS_TO_LIST          = "maxAppsToList";

  public static final String  SHOW_HEADER               = "showHeader";

  public static final String  LISTING_MODE              = "listingMode";

  /** Lists the apps each viewer marked as favorite, the default. */
  public static final String  LISTING_MODE_FAVORITES    = "FAVORITES";

  /** Lists the apps chosen in the settings. */
  public static final String  LISTING_MODE_SELECTED     = "SELECTED";

  public static final String  SELECTION_MODE            = "selectionMode";

  /** Lists the apps picked one by one, in applicationIds, the default. */
  public static final String  SELECTION_MODE_MANUAL     = "MANUAL";

  /** Lists the apps of the categories picked. */
  public static final String  SELECTION_MODE_CATEGORY   = "CATEGORY";

  public static final String  APPLICATION_IDS           = "applicationIds";

  public static final String  CATEGORY_IDS              = "categoryIds";

  /**
   * How the apps are displayed: ROW, CARDS or COLUMN. A window with none
   * renders the legacy layout (D3).
   */
  public static final String  TEMPLATE                  = "template";

  public static final String  TEMPLATE_ROW              = "ROW";

  public static final String  TEMPLATE_CARDS            = "CARDS";

  public static final String  TEMPLATE_COLUMN           = "COLUMN";

  public static final String  SHOW_SEE_MORE             = "showSeeMore";

  /** Whether the apps stay on one line, ignored by the COLUMN template. */
  public static final String  LIST_IN_ROW               = "listInRow";

  public static final String  SHOW_ICON                 = "showIcon";

  /** The icon size, in pixels. */
  public static final String  ICON_SIZE                 = "iconSize";

  public static final String  SHOW_NAME                 = "showName";

  /** The tiles' color, empty for none: a white tile, or the skin's border color when outlined. */
  public static final String  COLOR                     = "color";

  public static final String  OUTLINED                  = "outlined";

  public static final String  H_ALIGN                   = "hAlign";

  public static final String  V_ALIGN                   = "vAlign";

  public static final String  ALIGN_START               = "START";

  public static final String  ALIGN_CENTER              = "CENTER";

  public static final String  ALIGN_END                 = "END";

  public static final int     MIN_ICON_SIZE             = 12;

  public static final int     MAX_ICON_SIZE             = 60;

  /** Most categories a Shortcuts portlet lists the apps of (D6). */
  public static final int     MAX_LISTED_CATEGORIES     = 100;

  public static final int     MIN_APPS_TO_LIST          = 1;

  public static final int     MAX_APPS_TO_LIST_LIMIT    = 100;

  /**
   * Reference a shared-layout window's setting records in place of a page: a
   * window of a configured shared layout has no storage id, so no generated
   * name to carry its kind. It names no page.
   */
  public static final String  SHARED_LAYOUT_REFERENCE   = "portal::global::sharedlayout";

  private static final Log    LOG                       = ExoLogger.getLogger(MyApplicationsSettingsService.class);

  /**
   * Name generated for a shared-layout window that has a storage id, which a
   * configured shared layout never gives: edited by administrators only, as a
   * setting recording {@link #SHARED_LAYOUT_REFERENCE}.
   */
  private static final Pattern SHARED_LAYOUT_NAME       = Pattern.compile("^Untitled-\\d+-shared-");

  /** Window of a site layout: edited by whoever may edit that site. */
  private static final Pattern SITE_LAYOUT_NAME         = Pattern.compile("^Untitled-\\d+-site-");

  /**
   * Window of a section-template draft page: the window's storage id, then
   * the template id.
   */
  private static final Pattern SECTION_DRAFT_NAME       = Pattern.compile("^Untitled-(\\d+)-sdraft-(\\d+)-");

  /** Section-template draft page: the template id, then the user. */
  private static final Pattern SECTION_DRAFT_PAGE_NAME  = Pattern.compile("^section_draft_(\\d+)_");

  private static final String  UNTITLED_PREFIX          = "Untitled-";

  private static final String  SITE_LAYOUT_MARKER       = "site-";

  private static final String  SHARED_LAYOUT_MARKER     = "shared-";

  private static final String  SECTION_DRAFT_MARKER     = "sdraft-";

  /**
   * Section-template editor draft page, named section_draft_TEMPLATE_USER in
   * the global site: CMSPortlet records it cut at _draft_, as global::section
   */
  private static final String  SECTION_DRAFT_PAGE_PREFIX = "section_draft_";

  private static final String  SECTION_DRAFT_PAGE_CUT   = "section";

  private static final String  GLOBAL_SITE              = "global";

  private static final Pattern DIGITS                   = Pattern.compile("^\\d{1,3}$");

  private static final Pattern ID                       = Pattern.compile("^\\d{1,18}$");

  private static final Pattern COLOR_VALUE              = Pattern.compile("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");

  private static final List<String> DISPLAY_FLAGS       = List.of(SHOW_SEE_MORE, LIST_IN_ROW, SHOW_ICON, SHOW_NAME, OUTLINED);

  private static final int     MAX_KEPT_SETTING_NAMES   = 1000;

  @Autowired
  private CMSService          cmsService;

  @Autowired
  private LayoutAclService    layoutAclService;

  @Autowired
  private TranslationService  translationService;

  @Autowired
  private UserACL             userAcl;

  @Autowired
  private ApplicationCenterStorage applicationCenterStorage;

  @Autowired
  private CategoryService     categoryService;

  @Autowired
  private SpaceService        spaceService;

  @Autowired
  private LayoutService       layoutService;

  /**
   * Whether a window keeps a setting name generated for another storage id,
   * by storage id and name: the setting read and the page scan run once per
   * window and node; past {@value #MAX_KEPT_SETTING_NAMES} decisions, the map
   * is emptied, and a window is decided again.
   */
  private final Map<String, Boolean> keptSettingNames = new ConcurrentHashMap<>();

  /**
   * Whether a user may change the settings of a Shortcuts portlet window,
   * header title included: the page-content right of the window's page, in the
   * space owning that page's site, whatever space its setting recorded, the
   * site edit right for a site-layout window, administrators only for a
   * shared-layout window, whose context is the platform's whatever space its
   * setting recorded, and for a window whose setting was recorded on a
   * section-template draft page, whatever its name. A shared-layout window is
   * told by its generated name, or, for a window of a configured shared
   * layout, which has no storage id, so no generated name, by the
   * {@link #SHARED_LAYOUT_REFERENCE} its setting records; such a window whose
   * setting was recorded before keeps the right of the page that first
   * rendered it.
   *
   * @param settingName the window's CMS setting name
   * @param username the user, null or anonymous for a guest
   * @return true when the user may write the window's settings
   */
  public boolean canEditSettings(String settingName, String username) {
    if (StringUtils.isBlank(settingName)
        || StringUtils.isBlank(username)
        || StringUtils.equals(username, IdentityConstants.ANONIM)) {
      return false;
    }
    CMSSetting setting = cmsService.getSetting(SETTING_TYPE, settingName);
    if (setting == null) {
      return false;
    }
    Identity identity = userAcl.getUserIdentity(username);
    if (identity == null) {
      return false;
    }
    if (isSharedLayoutWindow(settingName, setting.getPageReference())) {
      return userAcl.isAdministrator(identity);
    } else if (isSectionTemplateDraft(setting.getPageReference())) {
      return userAcl.isAdministrator(identity);
    } else if (SITE_LAYOUT_NAME.matcher(settingName).find()) {
      return canEditSite(setting.getPageReference(), username);
    } else {
      // The page-content right in the space owning the recorded page's site,
      // which a window rendered before that rule may not have recorded
      return cmsService.hasEditPermission(identity,
                                          setting.getPageReference(),
                                          getSiteSpaceId(getSiteKey(setting.getPageReference())));
    }
  }

  /**
   * Whether a user may change a header title stored under a legacy numeric
   * object id, the random applicationId the portlet used before the title was
   * keyed on the setting name: administrators only.
   *
   * @param username the user, null or anonymous for a guest
   * @return true when the user is an administrator
   */
  public boolean canEditLegacyHeaderTitle(String username) {
    if (StringUtils.isBlank(username) || StringUtils.equals(username, IdentityConstants.ANONIM)) {
      return false;
    }
    Identity identity = userAcl.getUserIdentity(username);
    return identity != null && userAcl.isAdministrator(identity);
  }

  /**
   * Decides a settings write of a Shortcuts portlet window: refuses a user who
   * may not edit the window's settings, before reading any parameter, then
   * keeps only the preferences the portlet owns, each with a valid value.
   *
   * @param settingName the window's CMS setting name, read from its stored
   *          preferences, never from the request
   * @param username the user, null or anonymous for a guest
   * @param parameters posted parameters
   * @return the preferences to store, by name
   * @throws IllegalAccessException when the user may not edit the settings
   * @throws IllegalArgumentException when the write selects the SELECTED
   *           listing mode with no application to list
   */
  public Map<String, String> getSettingsToStore(String settingName,
                                                String username,
                                                Map<String, String> parameters) throws IllegalAccessException {
    if (!canEditSettings(settingName, username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to edit settings %s", username, settingName));
    }
    return getWritableSettings(settingName, parameters);
  }

  /**
   * Filters the parameters of a settings write down to the preferences the
   * Shortcuts portlet owns, each with a valid value. Any other name, among
   * which name, applicationId, data.init, canEdit and settingName, and any
   * invalid value is dropped, and so is the id of an application that no
   * longer exists or that another Shortcuts portlet window created, and of a
   * category that no longer exists. The selection mode, the application ids
   * and the category ids are stored only with a listing mode, both lists
   * whatever the selection mode, so that switching modes keeps the other
   * list. A write selecting the SELECTED listing mode is refused as a whole
   * unless it selects the MANUAL mode with at least one application, or the
   * CATEGORY mode with at least one category: a SELECTED listing is never
   * stored empty.
   *
   * @param settingName the window's CMS setting name
   * @param parameters posted parameters
   * @return the preferences to store, by name
   * @throws IllegalArgumentException when the write selects the SELECTED
   *           listing mode with nothing to list
   */
  public Map<String, String> getWritableSettings(String settingName, Map<String, String> parameters) {
    Map<String, String> settings = new HashMap<>();
    if (parameters == null) {
      return settings;
    }
    String maxAppsToList = StringUtils.trim(parameters.get(MAX_APPS_TO_LIST));
    if (maxAppsToList != null && DIGITS.matcher(maxAppsToList).matches()) {
      int value = Integer.parseInt(maxAppsToList);
      if (value >= MIN_APPS_TO_LIST && value <= MAX_APPS_TO_LIST_LIMIT) {
        settings.put(MAX_APPS_TO_LIST, String.valueOf(value));
      }
    }
    String showHeader = StringUtils.trim(parameters.get(SHOW_HEADER));
    if (StringUtils.equalsAny(showHeader, "true", "false")) {
      settings.put(SHOW_HEADER, showHeader);
    }
    putDisplaySettings(parameters, settings);
    String listingMode = StringUtils.trim(parameters.get(LISTING_MODE));
    if (!StringUtils.equalsAny(listingMode, LISTING_MODE_FAVORITES, LISTING_MODE_SELECTED)) {
      // The selection is written only with its listing mode, so that the
      // stored state is always the one this write validates
      return settings;
    }
    String selectionMode = StringUtils.trim(parameters.get(SELECTION_MODE));
    if (StringUtils.equalsAny(selectionMode, SELECTION_MODE_MANUAL, SELECTION_MODE_CATEGORY)) {
      settings.put(SELECTION_MODE, selectionMode);
    }
    String applicationIds = StringUtils.deleteWhitespace(parameters.get(APPLICATION_IDS));
    List<Long> ids = applicationIds == null ? null : parseIds(applicationIds, ApplicationCenterService.MAX_LISTED_APPLICATIONS);
    if (ids != null) {
      // The id of a deleted application is dropped: no one could see or remove
      // it, and so is the id of an application another window created, which
      // only that window lists
      ids = ids.stream().filter(id -> isListable(applicationCenterStorage.getApplication(id), settingName)).toList();
      settings.put(APPLICATION_IDS, StringUtils.join(ids, ","));
    }
    String categoryIds = StringUtils.deleteWhitespace(parameters.get(CATEGORY_IDS));
    List<Long> categories = categoryIds == null ? null : parseIds(categoryIds, MAX_LISTED_CATEGORIES);
    if (categories != null) {
      // The id of a deleted category is dropped as well
      categories = categories.stream().filter(id -> categoryService.getCategory(id) != null).toList();
      settings.put(CATEGORY_IDS, StringUtils.join(categories, ","));
    }
    if (StringUtils.equals(listingMode, LISTING_MODE_SELECTED)
        && !(StringUtils.equals(selectionMode, SELECTION_MODE_MANUAL) && CollectionUtils.isNotEmpty(ids))
        && !(StringUtils.equals(selectionMode, SELECTION_MODE_CATEGORY) && CollectionUtils.isNotEmpty(categories))) {
      throw new IllegalArgumentException("A SELECTED listing needs at least one application or category");
    }
    settings.put(LISTING_MODE, listingMode);
    return settings;
  }

  /**
   * Reads a stored listing mode: FAVORITES or SELECTED, FAVORITES for any
   * other value or none.
   *
   * @param storedValue the listingMode preference
   * @return the listing mode to render
   */
  public static String getListingMode(String storedValue) {
    return StringUtils.equals(storedValue, LISTING_MODE_SELECTED) ? LISTING_MODE_SELECTED : LISTING_MODE_FAVORITES;
  }

  /**
   * Reads a stored selection mode: MANUAL or CATEGORY, MANUAL for any other
   * value or none.
   *
   * @param storedValue the selectionMode preference
   * @return the selection mode to render
   */
  public static String getSelectionMode(String storedValue) {
    return StringUtils.equals(storedValue, SELECTION_MODE_CATEGORY) ? SELECTION_MODE_CATEGORY : SELECTION_MODE_MANUAL;
  }

  /**
   * Reads stored application ids: the ordered ids, without duplicates, empty
   * for a value that is not a list of at most
   * {@link ApplicationCenterService#MAX_LISTED_APPLICATIONS} ids.
   *
   * @param storedValue the applicationIds preference, comma-separated
   * @return the ids to render, never null
   */
  public static List<Long> getApplicationIds(String storedValue) {
    List<Long> ids = parseIds(StringUtils.deleteWhitespace(storedValue), ApplicationCenterService.MAX_LISTED_APPLICATIONS);
    return ids == null ? Collections.emptyList() : ids;
  }

  /**
   * Reads stored category ids: the ordered ids, without duplicates, empty for
   * a value that is not a list of at most {@link #MAX_LISTED_CATEGORIES} ids.
   *
   * @param storedValue the categoryIds preference, comma-separated
   * @return the ids to render, never null
   */
  public static List<Long> getCategoryIds(String storedValue) {
    List<Long> ids = parseIds(StringUtils.deleteWhitespace(storedValue), MAX_LISTED_CATEGORIES);
    return ids == null ? Collections.emptyList() : ids;
  }

  /**
   * Reads a stored template: ROW, CARDS or COLUMN, null for any other value
   * or none, which renders the legacy layout.
   *
   * @param storedValue the template preference
   * @return the template to render, or null
   */
  public static String getTemplate(String storedValue) {
    return StringUtils.equalsAny(storedValue, TEMPLATE_ROW, TEMPLATE_CARDS, TEMPLATE_COLUMN) ? storedValue : null;
  }

  /**
   * Reads a stored display flag: showSeeMore, listInRow, showIcon, showName
   * or outlined.
   *
   * @param storedValue the preference
   * @return true or false, null for any other value or none, which the
   *         template's default replaces
   */
  public static Boolean getDisplayFlag(String storedValue) {
    if (StringUtils.equals(storedValue, "true")) {
      return Boolean.TRUE;
    } else if (StringUtils.equals(storedValue, "false")) {
      return Boolean.FALSE;
    } else {
      return null; // NOSONAR
    }
  }

  /**
   * Reads a stored icon size.
   *
   * @param storedValue the iconSize preference
   * @return the size, from {@link #MIN_ICON_SIZE} to {@link #MAX_ICON_SIZE},
   *         null for any other value or none, which the template's default
   *         replaces
   */
  public static Integer getIconSize(String storedValue) {
    if (storedValue == null || !DIGITS.matcher(storedValue).matches()) {
      return null;
    }
    int value = Integer.parseInt(storedValue);
    return value >= MIN_ICON_SIZE && value <= MAX_ICON_SIZE ? value : null;
  }

  /**
   * Reads a stored color: #RRGGBB or #RRGGBBAA.
   *
   * @param storedValue the color preference
   * @return the color, empty for any other value or none: no color
   */
  public static String getColor(String storedValue) {
    return storedValue != null && COLOR_VALUE.matcher(storedValue).matches() ? storedValue : "";
  }

  /**
   * Reads a stored alignment, horizontal or vertical: START, CENTER or END,
   * START being the left in a left-to-right page and the top vertically.
   *
   * @param storedValue the hAlign or vAlign preference
   * @return the alignment, null for any other value or none, which the
   *         template's default replaces
   */
  public static String getAlignment(String storedValue) {
    return StringUtils.equalsAny(storedValue, ALIGN_START, ALIGN_CENTER, ALIGN_END) ? storedValue : null;
  }

  /**
   * @return the ordered ids without duplicates, an empty list for an empty
   *         value, null for a value that is not a list of at most maxIds ids
   */
  private static List<Long> parseIds(String value, int maxIds) {
    if (value == null) {
      return null; // NOSONAR
    } else if (value.isEmpty()) {
      return Collections.emptyList();
    }
    // Each id is matched alone: a repeated group over the whole list recurses per id
    String[] tokens = value.split(",", -1);
    if (!Arrays.stream(tokens).allMatch(token -> ID.matcher(token).matches())) {
      return null; // NOSONAR
    }
    List<Long> ids = Arrays.stream(tokens).map(Long::valueOf).distinct().toList();
    return ids.size() > maxIds ? null : ids;
  }

  /**
   * Copies the header title, every language, from one key to another when the
   * latter holds none: from the legacy applicationId key to the setting name,
   * or from the setting name a window gave up to the one it got. Reads and
   * writes go through the ACL-free translation API: the copy runs at render,
   * for any viewer.
   *
   * @param sourceKey the key the header title is stored under
   * @param settingName the window's CMS setting name
   */
  public void copyHeaderTitle(String sourceKey, String settingName) {
    if (StringUtils.isBlank(sourceKey) || StringUtils.isBlank(settingName)) {
      return;
    }
    try {
      TranslationField current = translationService.getTranslationField(SETTING_TYPE, settingName, HEADER_TITLE_FIELD);
      if (hasLabels(current)) {
        return;
      }
      TranslationField source = translationService.getTranslationField(SETTING_TYPE, sourceKey, HEADER_TITLE_FIELD);
      if (hasLabels(source)) {
        translationService.saveTranslationLabels(SETTING_TYPE,
                                                 settingName,
                                                 HEADER_TITLE_FIELD,
                                                 new HashMap<>(source.getLabels()),
                                                 false);
      }
    } catch (ObjectNotFoundException e) {
      LOG.debug("No header title to copy from {} to {}", sourceKey, settingName, e);
    }
  }

  /**
   * Generates the setting name of a window: Untitled-STORAGEID-UUID for a page
   * window, with a site- or shared- marker before the UUID for a site-layout
   * or a shared-layout window, so that its name tells which right and which
   * context apply to it.
   *
   * @param storageId the window's storage id
   * @param windowKind where the window is placed
   * @return a new setting name
   */
  public String generateSettingName(String storageId, WindowKind windowKind) {
    String marker = switch (windowKind) {
    case SITE_LAYOUT -> SITE_LAYOUT_MARKER;
    case SHARED_LAYOUT -> SHARED_LAYOUT_MARKER;
    default -> "";
    };
    return UNTITLED_PREFIX + storageId + "-" + marker + UUID.randomUUID();
  }

  /**
   * Generates the setting name of a window of a section-template draft page:
   * Untitled-STORAGEID-sdraft-TEMPLATEID-UUID, so that the applications of the
   * template's earlier editing sessions, and those to delete with the
   * template, are told by their owner's name.
   *
   * @param storageId the window's storage id
   * @param templateId the section template id
   * @return a new setting name
   */
  public String generateSectionTemplateDraftSettingName(String storageId, long templateId) {
    return UNTITLED_PREFIX + storageId + "-" + SECTION_DRAFT_MARKER + templateId + "-" + UUID.randomUUID();
  }

  /**
   * @param pageReference a page reference
   * @return the id of the section template whose draft page the reference
   *         names, section_draft_TEMPLATE_USER in the global site, 0 for any
   *         other page
   */
  public long getSectionTemplateId(String pageReference) {
    PageKey pageKey = StringUtils.isBlank(pageReference) ? null : getPageKey(pageReference);
    if (pageKey == null
        || pageKey.getSite().getType() != SiteType.PORTAL
        || !StringUtils.equals(pageKey.getSite().getName(), GLOBAL_SITE)) {
      return 0;
    }
    Matcher matcher = SECTION_DRAFT_PAGE_NAME.matcher(pageKey.getName());
    return matcher.find() ? NumberUtils.toLong(matcher.group(1), 0) : 0;
  }

  /**
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @return true when the name was generated for a window of a
   *         section-template draft page
   */
  public boolean isSectionTemplateDraftName(String settingName) {
    return settingName != null && SECTION_DRAFT_NAME.matcher(settingName).find();
  }

  /**
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @param templateId a section template id
   * @return the storage id of the window of that template's draft page the
   *         name was generated for, null for any other name
   */
  public String getSectionTemplateDraftStorageId(String settingName, long templateId) {
    Matcher matcher = settingName == null ? null : SECTION_DRAFT_NAME.matcher(settingName);
    return matcher != null
           && matcher.find()
           && templateId > 0
           && NumberUtils.toLong(matcher.group(2), 0) == templateId ? matcher.group(1) : null;
  }

  /**
   * @param templateId a section template id
   * @return a LIKE pattern matching at least the setting names generated for
   *         the windows of that template's draft pages, to check one by one
   *         with {@link #getSectionTemplateDraftStorageId(String, long)}
   */
  public String getSectionTemplateDraftNamePattern(long templateId) {
    return UNTITLED_PREFIX + "%-" + SECTION_DRAFT_MARKER + templateId + "-%";
  }

  /**
   * Decides the space a window's setting records: the space owning the site
   * of the recorded page reference, for a page window as for a site-layout
   * window, whose recorded page is on the site whose layout holds it, whatever
   * site's navigation rendered the page; none for a shared-layout window,
   * which a configured layout renders on every page of the sites it serves.
   *
   * @param settingName the window's CMS setting name
   * @param pageReference the page reference the window's setting records,
   *          see {@link #getSettingPageReference(String, String, SiteKey)}
   * @return the space id to record, 0 for the platform context
   */
  public long getContextSpaceId(String settingName, String pageReference) {
    return isSharedLayoutWindow(settingName, pageReference) ? 0 : getSiteSpaceId(getSiteKey(pageReference));
  }

  /**
   * Whether a window's context no longer exists: the page its setting records
   * is on a space's group site, or on a space's public site, and that space
   * is gone, or on a portal site that was removed. A space's public site is
   * removed with its space, after which nothing tells it from another removed
   * site. A shared-layout window's context is the platform's.
   *
   * @param settingName the window's CMS setting name
   * @param pageReference the page reference the window's setting records
   * @return true when the window's space or portal site was deleted
   */
  public boolean isContextDeleted(String settingName, String pageReference) {
    SiteKey site = getSiteKey(pageReference);
    if (site == null || isSharedLayoutWindow(settingName, pageReference)) {
      return false;
    } else if (site.getType() == SiteType.GROUP) {
      return StringUtils.startsWith(site.getName(), SpaceUtils.SPACE_GROUP_PREFIX)
             && spaceService.getSpaceByGroupId(site.getName()) == null;
    } else if (site.getType() == SiteType.PORTAL) {
      PortalConfig portalConfig = layoutService.getPortalConfig(site);
      String spaceId = portalConfig == null ? null : portalConfig.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID);
      return portalConfig == null || StringUtils.isNotBlank(spaceId) && spaceService.getSpaceById(spaceId) == null;
    } else {
      return false;
    }
  }

  /**
   * Decides the page reference a window's setting records and is compared
   * with: for a site-layout window, the rendered page's name on the site whose
   * layout holds the window, whatever page and site render it, so that its
   * context, its right and the check of its name follow that site; the
   * rendered page otherwise. A site renders the pages of other sites, and a
   * displayed site renders the meta portal's layout.
   *
   * @param settingName the window's CMS setting name
   * @param pageReference the page the window is rendered on
   * @param layoutSite the site whose layout holds a site-layout window
   * @return the page reference to record and compare
   */
  public String getSettingPageReference(String settingName, String pageReference, SiteKey layoutSite) {
    PageKey pageKey = layoutSite == null || !SITE_LAYOUT_NAME.matcher(settingName).find() ? null : getPageKey(pageReference);
    return pageKey == null ? pageReference : new PageKey(layoutSite, pageKey.getName()).format();
  }

  /**
   * Whether a window keeps the setting name its preferences carry. A layout
   * copy carries the preferences of its source, the setting name included, and
   * a page editor may rewrite it: such a window would act in the context of
   * another window's setting. It keeps its name when the name was generated
   * for it: its storage id and the marker of its kind of window. Otherwise, a
   * page window keeps an unmarked name recorded on this page that no other
   * window of the page carries, as after a same-page publish, and a
   * site-layout window keeps a site-marked name recorded on this site that no
   * other window of the site carries, whatever space their setting recorded,
   * since the space is read from the recorded page
   * ({@link #getContextSpaceId(String, String)}). Every
   * other name is given up, a shared-layout window's and a section-template
   * draft window's included. The decision
   * on a name generated for another storage id is memoised per window.
   *
   * @param settingName the name the window's preferences carry, not blank
   * @param storageId the window's storage id, not blank
   * @param windowKind where the window is placed
   * @param pageReference the page reference the window's setting records,
   *          see {@link #getSettingPageReference(String, String, SiteKey)}
   * @param otherWindowNames reads the setting names of the other windows of
   *          the page, or of the site for a site-layout window
   * @return true when the window keeps its setting name
   */
  public boolean keepsSettingName(String settingName,
                                  String storageId,
                                  WindowKind windowKind,
                                  String pageReference,
                                  Supplier<Collection<String>> otherWindowNames) {
    if (getWindowKind(settingName) != windowKind) {
      // A name marked for another kind of window, or an unmarked one on a
      // site-layout or shared-layout window, as before this marker existed
      return false;
    } else if (StringUtils.startsWith(settingName, UNTITLED_PREFIX + storageId + "-")) {
      return true;
    } else if (windowKind == WindowKind.SHARED_LAYOUT || windowKind == WindowKind.SECTION_TEMPLATE_DRAFT) {
      // A section-template draft page is saved again, with new windows, at
      // each editing session: a name generated for another window is an
      // earlier session's
      return false;
    }
    String key = storageId + "/" + settingName;
    Boolean kept = keptSettingNames.get(key);
    if (kept == null) {
      kept = isSettingOfWindow(settingName, windowKind, pageReference, otherWindowNames);
      if (keptSettingNames.size() >= MAX_KEPT_SETTING_NAMES) {
        keptSettingNames.clear();
      }
      keptSettingNames.put(key, kept);
    }
    return kept;
  }

  /**
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @return true when the window's setting exists
   */
  public boolean hasSetting(String settingName) {
    return getSetting(settingName) != null;
  }

  /**
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @return the window's setting, recorded server-side when the window was
   *         first rendered, null when it does not exist
   */
  public CMSSetting getSetting(String settingName) {
    return StringUtils.isBlank(settingName) ? null : cmsService.getSetting(SETTING_TYPE, settingName);
  }

  private boolean isSettingOfWindow(String settingName,
                                    WindowKind windowKind,
                                    String pageReference,
                                    Supplier<Collection<String>> otherWindowNames) {
    CMSSetting setting = getSetting(settingName);
    if (setting == null) {
      return false;
    } else if (windowKind == WindowKind.SITE_LAYOUT) {
      SiteKey site = getSiteKey(pageReference);
      if (site == null || !site.equals(getSiteKey(setting.getPageReference()))) {
        return false;
      }
    } else if (!StringUtils.equals(setting.getPageReference(), pageReference)) {
      return false;
    }
    return !otherWindowNames.get().contains(settingName);
  }

  /**
   * @return whether a setting is a shared-layout window's: its generated name,
   *         or the reference a window of a configured shared layout records
   */
  private boolean isSharedLayoutWindow(String settingName, String pageReference) {
    return SHARED_LAYOUT_NAME.matcher(settingName).find() || StringUtils.equals(pageReference, SHARED_LAYOUT_REFERENCE);
  }

  /**
   * @return the kind of window a setting name was generated for, from its
   *         marker
   */
  private WindowKind getWindowKind(String settingName) {
    if (SITE_LAYOUT_NAME.matcher(settingName).find()) {
      return WindowKind.SITE_LAYOUT;
    } else if (SHARED_LAYOUT_NAME.matcher(settingName).find()) {
      return WindowKind.SHARED_LAYOUT;
    } else if (SECTION_DRAFT_NAME.matcher(settingName).find()) {
      return WindowKind.SECTION_TEMPLATE_DRAFT;
    } else {
      return WindowKind.PAGE;
    }
  }

  /**
   * @return the space owning a site: a space's own group site, or its public
   *         site, a portal site carrying the space's id; 0 for any other site,
   *         a site or space template included
   */
  private long getSiteSpaceId(SiteKey site) {
    Space space = null;
    if (site == null) {
      return 0;
    } else if (site.getType() == SiteType.GROUP) {
      space = spaceService.getSpaceByGroupId(site.getName());
    } else if (site.getType() == SiteType.PORTAL) {
      String spaceId = getPublicSiteSpaceId(site);
      space = StringUtils.isBlank(spaceId) ? null : spaceService.getSpaceById(spaceId);
    }
    return space == null ? 0 : space.getSpaceId();
  }

  /**
   * @return the id of the space whose public site a portal site is, null for
   *         any other portal site
   */
  private String getPublicSiteSpaceId(SiteKey site) {
    PortalConfig portalConfig = layoutService.getPortalConfig(site);
    return portalConfig == null ? null : portalConfig.getProperty(SpaceUtils.PUBLIC_SITE_SPACE_ID);
  }

  private SiteKey getSiteKey(String pageReference) {
    PageKey pageKey = getPageKey(pageReference);
    return pageKey == null ? null : pageKey.getSite();
  }

  private PageKey getPageKey(String pageReference) {
    try {
      return PageKey.parse(pageReference);
    } catch (RuntimeException e) {
      LOG.debug("Unable to parse page reference {}", pageReference, e);
      return null;
    }
  }

  private boolean canEditSite(String pageReference, String username) {
    try {
      return layoutAclService.canEditSite(PageKey.parse(pageReference).getSite(), username);
    } catch (RuntimeException e) {
      LOG.debug("Unable to resolve the site of page {}", pageReference, e);
      return false;
    }
  }

  private boolean isSectionTemplateDraft(String pageReference) {
    try {
      PageKey pageKey = PageKey.parse(pageReference);
      return pageKey.getSite().getType() == SiteType.PORTAL
             && StringUtils.equals(pageKey.getSite().getName(), GLOBAL_SITE)
             && (StringUtils.equals(pageKey.getName(), SECTION_DRAFT_PAGE_CUT)
                 || StringUtils.startsWith(pageKey.getName(), SECTION_DRAFT_PAGE_PREFIX));
    } catch (RuntimeException e) {
      LOG.debug("Unable to parse page reference {}", pageReference, e);
      return false;
    }
  }

  /**
   * Keeps the display settings of a write that have a valid value, whatever
   * the listing mode, an empty color included.
   */
  private void putDisplaySettings(Map<String, String> parameters, Map<String, String> settings) {
    String template = getTemplate(StringUtils.trim(parameters.get(TEMPLATE)));
    if (template != null) {
      settings.put(TEMPLATE, template);
    }
    for (String flag : DISPLAY_FLAGS) {
      Boolean value = getDisplayFlag(StringUtils.trim(parameters.get(flag)));
      if (value != null) {
        settings.put(flag, value.toString());
      }
    }
    Integer iconSize = getIconSize(StringUtils.trim(parameters.get(ICON_SIZE)));
    if (iconSize != null) {
      settings.put(ICON_SIZE, iconSize.toString());
    }
    String color = StringUtils.trim(parameters.get(COLOR));
    if (color != null && (color.isEmpty() || COLOR_VALUE.matcher(color).matches())) {
      settings.put(COLOR, color);
    }
    for (String alignmentKey : List.of(H_ALIGN, V_ALIGN)) {
      String alignment = getAlignment(StringUtils.trim(parameters.get(alignmentKey)));
      if (alignment != null) {
        settings.put(alignmentKey, alignment);
      }
    }
  }

  private boolean isListable(Application application, String settingName) {
    return application != null
           && (!application.isContextual() || StringUtils.equals(application.getContextSettingName(), settingName));
  }

  private boolean hasLabels(TranslationField translationField) {
    return translationField != null
           && translationField.getLabels() != null
           && translationField.getLabels().values().stream().anyMatch(StringUtils::isNotBlank);
  }

}
