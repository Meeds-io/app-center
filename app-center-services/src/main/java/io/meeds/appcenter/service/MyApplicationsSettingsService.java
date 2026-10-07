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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.layout.service.LayoutAclService;
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

  public static final int     MIN_APPS_TO_LIST          = 1;

  public static final int     MAX_APPS_TO_LIST_LIMIT    = 100;

  private static final Log    LOG                       = ExoLogger.getLogger(MyApplicationsSettingsService.class);

  /** Window of a shared layout: rendered on every site, edited by administrators only. */
  private static final Pattern SHARED_LAYOUT_NAME       = Pattern.compile("^Untitled-\\d+-shared-");

  /** Window of a site layout: edited by whoever may edit that site. */
  private static final Pattern SITE_LAYOUT_NAME         = Pattern.compile("^Untitled-\\d+-site-");

  /**
   * Section-template editor draft page, named section_draft_TEMPLATE_USER in
   * the global site: CMSPortlet records it cut at _draft_, as global::section
   */
  private static final String  SECTION_DRAFT_PAGE_PREFIX = "section_draft_";

  private static final String  SECTION_DRAFT_PAGE_CUT   = "section";

  private static final String  GLOBAL_SITE              = "global";

  private static final Pattern DIGITS                   = Pattern.compile("^\\d{1,3}$");

  private static final Pattern ID                       = Pattern.compile("^\\d{1,18}$");

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

  /**
   * Whether a user may change the settings of a Shortcuts portlet window,
   * header title included: the page-content right of the window's page, the
   * site edit right for a site-layout window, administrators only for a
   * shared-layout window and for a window whose setting was recorded on a
   * section-template draft page, whatever its name.
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
    return getWritableSettings(parameters);
  }

  /**
   * Filters the parameters of a settings write down to the preferences the
   * Shortcuts portlet owns, each with a valid value. Any other name, among
   * which name, applicationId, data.init, canEdit and settingName, and any
   * invalid value is dropped, and so is the id of an application that no
   * longer exists. The selection mode and the application ids are stored only
   * with a listing mode. A write selecting the SELECTED listing
   * mode is refused as a whole unless it selects the MANUAL mode with at least
   * one application: a SELECTED listing is never stored empty, and no category
   * can be chosen yet.
   *
   * @param parameters posted parameters
   * @return the preferences to store, by name
   * @throws IllegalArgumentException when the write selects the SELECTED
   *           listing mode with no application to list
   */
  public Map<String, String> getWritableSettings(Map<String, String> parameters) {
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
    List<Long> ids = applicationIds == null ? null : parseApplicationIds(applicationIds);
    if (ids != null) {
      // The id of a deleted application is dropped: no one could see or remove it
      ids = ids.stream().filter(id -> applicationCenterStorage.getApplication(id) != null).toList();
      settings.put(APPLICATION_IDS, StringUtils.join(ids, ","));
    }
    if (StringUtils.equals(listingMode, LISTING_MODE_SELECTED)
        && (!StringUtils.equals(selectionMode, SELECTION_MODE_MANUAL) || ids == null || ids.isEmpty())) {
      throw new IllegalArgumentException("A SELECTED listing needs at least one application");
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
    List<Long> ids = parseApplicationIds(StringUtils.deleteWhitespace(storedValue));
    return ids == null ? Collections.emptyList() : ids;
  }

  /**
   * @return the ordered ids without duplicates, an empty list for an empty
   *         value, null for a value that is not a list of at most
   *         {@link ApplicationCenterService#MAX_LISTED_APPLICATIONS} ids
   */
  private static List<Long> parseApplicationIds(String value) {
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
    return ids.size() > ApplicationCenterService.MAX_LISTED_APPLICATIONS ? null : ids;
  }

  /**
   * Copies the header title, every language, from the legacy applicationId key
   * to the setting name key when the latter holds none. Reads and writes go
   * through the ACL-free translation API: the migration runs at render, for
   * any viewer.
   *
   * @param legacyApplicationId the window's legacy applicationId preference
   * @param settingName the window's CMS setting name
   */
  public void migrateHeaderTitle(String legacyApplicationId, String settingName) {
    if (StringUtils.isBlank(legacyApplicationId) || StringUtils.isBlank(settingName)) {
      return;
    }
    try {
      TranslationField current = translationService.getTranslationField(SETTING_TYPE, settingName, HEADER_TITLE_FIELD);
      if (hasLabels(current)) {
        return;
      }
      TranslationField legacy = translationService.getTranslationField(SETTING_TYPE, legacyApplicationId, HEADER_TITLE_FIELD);
      if (hasLabels(legacy)) {
        translationService.saveTranslationLabels(SETTING_TYPE,
                                                 settingName,
                                                 HEADER_TITLE_FIELD,
                                                 new HashMap<>(legacy.getLabels()),
                                                 false);
      }
    } catch (ObjectNotFoundException e) {
      LOG.debug("No header title to migrate from {} to {}", legacyApplicationId, settingName, e);
    }
  }

  /**
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @return true when the window's setting exists
   */
  public boolean hasSetting(String settingName) {
    return StringUtils.isNotBlank(settingName) && cmsService.getSetting(SETTING_TYPE, settingName) != null;
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

  private boolean hasLabels(TranslationField translationField) {
    return translationField != null
           && translationField.getLabels() != null
           && translationField.getLabels().values().stream().anyMatch(StringUtils::isNotBlank);
  }

}
