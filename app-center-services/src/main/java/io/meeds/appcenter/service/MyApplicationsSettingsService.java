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
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
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

  /** Most categories a Shortcuts portlet lists the apps of (D6). */
  public static final int     MAX_LISTED_CATEGORIES     = 100;

  public static final int     MIN_APPS_TO_LIST          = 1;

  public static final int     MAX_APPS_TO_LIST_LIMIT    = 100;

  private static final Log    LOG                       = ExoLogger.getLogger(MyApplicationsSettingsService.class);

  /**
   * Name generated for a shared-layout window that has a storage id, which a
   * configured shared layout never gives: edited by administrators only.
   */
  private static final Pattern SHARED_LAYOUT_NAME       = Pattern.compile("^Untitled-\\d+-shared-");

  /** Window of a site layout: edited by whoever may edit that site. */
  private static final Pattern SITE_LAYOUT_NAME         = Pattern.compile("^Untitled-\\d+-site-");

  /** Window of a section-template draft page. */
  private static final Pattern SECTION_DRAFT_NAME       = Pattern.compile("^Untitled-\\d+-sdraft-\\d+-");

  private static final String  UNTITLED_PREFIX          = "Untitled-";

  private static final String  SITE_LAYOUT_MARKER       = "site-";

  private static final String  SHARED_LAYOUT_MARKER     = "shared-";

  /**
   * Section-template editor draft page, named section_draft_TEMPLATE_USER in
   * the global site: CMSPortlet records it cut at _draft_, as global::section
   */
  private static final String  SECTION_DRAFT_PAGE_PREFIX = "section_draft_";

  private static final String  SECTION_DRAFT_PAGE_CUT   = "section";

  private static final String  GLOBAL_SITE              = "global";

  private static final Pattern DIGITS                   = Pattern.compile("^\\d{1,3}$");

  private static final Pattern ID                       = Pattern.compile("^\\d{1,18}$");

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

  /**
   * Whether a window keeps a setting name generated for another storage id,
   * by storage id and name: the setting read and the page scan run once per
   * window and node; past {@value #MAX_KEPT_SETTING_NAMES} decisions, the map
   * is emptied, and a window is decided again.
   */
  private final Map<String, Boolean> keptSettingNames = new ConcurrentHashMap<>();

  /**
   * Whether a user may change the settings of a Shortcuts portlet window,
   * header title included: the page-content right of the window's page, the
   * site edit right for a site-layout window, administrators only for the
   * generated name of a shared-layout window and for a window whose setting
   * was recorded on a section-template draft page, whatever its name. A window
   * of a configured shared layout has no storage id, so no generated name: its
   * name must be configured, and it takes the right of the page that first
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
    if (SHARED_LAYOUT_NAME.matcher(settingName).find() && setting.getSpaceId() == 0) {
      return userAcl.isAdministrator(identity);
    } else if (isSectionTemplateDraft(setting.getPageReference())) {
      return userAcl.isAdministrator(identity);
    } else if (SITE_LAYOUT_NAME.matcher(settingName).find()) {
      return canEditSite(setting.getPageReference(), username);
    } else {
      return cmsService.hasEditPermission(identity, setting.getPageReference(), setting.getSpaceId());
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
   * Decides the space a window's setting records: the space owning the site
   * of the recorded page reference for a site-layout window, the site whose
   * layout holds it, none for the generated name of a shared-layout window,
   * which is rendered on every site, nor for a window of a site or space
   * template, which belongs to no space even when previewed as one, and the
   * space of the request otherwise.
   *
   * @param settingName the window's CMS setting name
   * @param pageReference the page reference the window's setting records,
   *          see {@link #getSettingPageReference(String, String, SiteKey)}
   * @param spaceId the space of the request
   *          (SpaceUtils.getSpaceByContext()), 0 for none
   * @return the space id to record, 0 for the platform context
   */
  public long getContextSpaceId(String settingName, String pageReference, long spaceId) {
    if (SHARED_LAYOUT_NAME.matcher(settingName).find()) {
      return 0;
    } else if (SITE_LAYOUT_NAME.matcher(settingName).find()) {
      return getSiteSpaceId(getSiteKey(pageReference));
    } else {
      return isTemplateSite(getSiteKey(pageReference)) ? 0 : spaceId;
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
   * page window keeps an unmarked name recorded on this page, with the space
   * of its context ({@link #getContextSpaceId(String, String, long)}), that no
   * other window of the page carries, as after a same-page publish, and a
   * site-layout window keeps a site-marked name recorded on this site, with
   * the space owning the site, that no other window of the site carries. Every
   * other name is given up, a shared-layout window's included. The decision
   * on a name generated for another storage id is memoised per window.
   *
   * @param settingName the name the window's preferences carry, not blank
   * @param storageId the window's storage id, not blank
   * @param windowKind where the window is placed
   * @param pageReference the page reference the window's setting records,
   *          see {@link #getSettingPageReference(String, String, SiteKey)}
   * @param spaceId the space of the request
   *          (SpaceUtils.getSpaceByContext()), 0 for none
   * @param otherWindowNames reads the setting names of the other windows of
   *          the page, or of the site for a site-layout window
   * @return true when the window keeps its setting name
   */
  public boolean keepsSettingName(String settingName,
                                  String storageId,
                                  WindowKind windowKind,
                                  String pageReference,
                                  long spaceId,
                                  Supplier<Collection<String>> otherWindowNames) {
    if (getWindowKind(settingName) != windowKind) {
      // A name marked for another kind of window, or an unmarked one on a
      // site-layout or shared-layout window, as before this marker existed
      return false;
    } else if (StringUtils.startsWith(settingName, UNTITLED_PREFIX + storageId + "-")) {
      return true;
    } else if (windowKind == WindowKind.SHARED_LAYOUT) {
      return false;
    }
    String key = storageId + "/" + settingName;
    Boolean kept = keptSettingNames.get(key);
    if (kept == null) {
      kept = isSettingOfWindow(settingName, windowKind, pageReference, spaceId, otherWindowNames);
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
                                    long spaceId,
                                    Supplier<Collection<String>> otherWindowNames) {
    CMSSetting setting = getSetting(settingName);
    if (setting == null) {
      return false;
    } else if (windowKind == WindowKind.SITE_LAYOUT) {
      SiteKey site = getSiteKey(pageReference);
      if (site == null
          || !site.equals(getSiteKey(setting.getPageReference()))
          || setting.getSpaceId() != getSiteSpaceId(site)) {
        return false;
      }
    } else if (!StringUtils.equals(setting.getPageReference(), pageReference)
               || setting.getSpaceId() != getContextSpaceId(settingName, pageReference, spaceId)) {
      return false;
    }
    return !otherWindowNames.get().contains(settingName);
  }

  /**
   * @return the kind of window a setting name was generated for, from its
   *         marker, null for a section-template draft window
   */
  private WindowKind getWindowKind(String settingName) {
    if (SITE_LAYOUT_NAME.matcher(settingName).find()) {
      return WindowKind.SITE_LAYOUT;
    } else if (SHARED_LAYOUT_NAME.matcher(settingName).find()) {
      return WindowKind.SHARED_LAYOUT;
    } else if (SECTION_DRAFT_NAME.matcher(settingName).find()) {
      return null;
    } else {
      return WindowKind.PAGE;
    }
  }

  /**
   * @return the space whose group owns a group site, 0 for any other site
   */
  private long getSiteSpaceId(SiteKey site) {
    if (site == null || site.getType() != SiteType.GROUP) {
      return 0;
    }
    Space space = spaceService.getSpaceByGroupId(site.getName());
    return space == null ? 0 : space.getSpaceId();
  }

  private boolean isTemplateSite(SiteKey site) {
    return site != null && (site.getType() == SiteType.GROUP_TEMPLATE || site.getType() == SiteType.PORTAL_TEMPLATE);
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
