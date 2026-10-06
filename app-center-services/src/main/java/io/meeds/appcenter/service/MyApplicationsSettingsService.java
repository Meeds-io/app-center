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

import java.util.HashMap;
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

  @Autowired
  private CMSService          cmsService;

  @Autowired
  private LayoutAclService    layoutAclService;

  @Autowired
  private TranslationService  translationService;

  @Autowired
  private UserACL             userAcl;

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
   *           listing mode
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
   * invalid value is dropped. A write selecting the SELECTED listing mode is
   * refused as a whole: no application or category can be chosen yet, and a
   * SELECTED listing with none is never stored.
   *
   * @param parameters posted parameters
   * @return the preferences to store, by name
   * @throws IllegalArgumentException when the write selects the SELECTED
   *           listing mode
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
    if (StringUtils.equals(listingMode, LISTING_MODE_SELECTED)) {
      throw new IllegalArgumentException("A SELECTED listing needs at least one application or category");
    } else if (StringUtils.equals(listingMode, LISTING_MODE_FAVORITES)) {
      settings.put(LISTING_MODE, listingMode);
    }
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
