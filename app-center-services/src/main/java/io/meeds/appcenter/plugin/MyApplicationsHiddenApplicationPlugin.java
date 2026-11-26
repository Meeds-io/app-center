/**
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

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.plugin.HiddenApplicationPlugin;
import io.meeds.social.translation.service.TranslationService;

/**
 * Lets each user hide a Shortcuts window whose editor allowed it
 */
@Service("myApplicationsHiddenApplicationPlugin")
public class MyApplicationsHiddenApplicationPlugin implements HiddenApplicationPlugin {

  public static final String            MY_APPLICATIONS_BUNDLE_NAME = "locale.portlet.MyApplications";

  public static final String            DEFAULT_NAME_KEY            = "myApplications.hide.defaultName";

  @Autowired
  private CMSService                    cmsService;

  @Autowired
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @Autowired
  private TranslationService            translationService;

  @Autowired
  private ResourceBundleService         resourceBundleService;

  @Override
  public String getApplicationType() {
    return MyApplicationsSettingsService.APPLICATION_TYPE;
  }

  @Override
  public HiddenApplication getApplication(String name, String language) {
    CMSSetting setting = cmsService.getSetting(MyApplicationsSettingsService.SETTING_TYPE, name);
    if (setting == null) {
      return null;
    }
    // A shared-layout or site-layout window is on every page of its site, not
    // on the page its setting records: it is listed with no page
    return new HiddenApplication(getApplicationType(),
                                 name,
                                 getTitle(name, language),
                                 isLayoutWindow(name, setting) ? null : setting.getPageReference(),
                                 myApplicationsSettingsService.isHideAllowed(name));
  }

  @Override
  public boolean canView(String name, Identity identity) {
    CMSSetting setting = cmsService.getSetting(MyApplicationsSettingsService.SETTING_TYPE, name);
    if (setting != null && isSharedLayoutWindow(name, setting)) {
      // The shared layout shows to every user of the platform, whose page
      // reference names no page to read access permissions from
      return identity != null && !IdentityConstants.ANONIM.equals(identity.getUserId());
    } else if (setting != null && myApplicationsSettingsService.isSiteLayoutWindow(name, setting.getPageReference())) {
      return myApplicationsSettingsService.canViewSiteLayoutWindow(setting.getPageReference(), identity);
    }
    return cmsService.hasAccessPermission(identity, MyApplicationsSettingsService.SETTING_TYPE, name);
  }

  private boolean isSharedLayoutWindow(String name, CMSSetting setting) {
    return myApplicationsSettingsService.isSharedLayoutWindow(name, setting.getPageReference());
  }

  private boolean isLayoutWindow(String name, CMSSetting setting) {
    return isSharedLayoutWindow(name, setting)
           || myApplicationsSettingsService.isSiteLayoutWindow(name, setting.getPageReference());
  }

  private String getTitle(String name, String language) {
    Locale locale = StringUtils.isBlank(language) ? Locale.ENGLISH : Locale.forLanguageTag(language);
    String header = translationService.getTranslationLabelOrDefault(MyApplicationsSettingsService.SETTING_TYPE,
                                                                    name,
                                                                    MyApplicationsSettingsService.HEADER_TITLE_FIELD,
                                                                    locale);
    if (StringUtils.isNotBlank(header)) {
      return header;
    }
    ResourceBundle bundle = resourceBundleService.getResourceBundle(MY_APPLICATIONS_BUNDLE_NAME, locale);
    return bundle != null && bundle.containsKey(DEFAULT_NAME_KEY) ? bundle.getString(DEFAULT_NAME_KEY) : null;
  }

}
