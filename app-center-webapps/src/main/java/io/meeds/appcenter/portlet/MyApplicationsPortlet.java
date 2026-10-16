/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
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

package io.meeds.appcenter.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.portlet.CMSPortlet;

public class MyApplicationsPortlet extends CMSPortlet {

  private static final String           APPLICATION_ID = "applicationId";

  private static final String           DRAFT_SUFFIX   = "_draft_";

  private static final Log              LOG            = ExoLogger.getLogger(MyApplicationsPortlet.class);

  private MyApplicationsSettingsService settingsService;

  private MyApplicationsCopyService     copyService;

  private ApplicationCenterService      applicationCenterService;

  @Override
  public void init(PortletConfig config) throws PortletException {
    super.init(config);
    this.contentType = MyApplicationsSettingsService.SETTING_TYPE;
  }

  @Override
  public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
    // Before the setting name is read: the request preferences are the ones
    // CMSPortlet reads, so a name set here is the one it initializes
    checkSettingName(request.getPreferences());
    super.doView(request, response);
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
    if (isDraftSite()) {
      throw new PortletException("Settings are not written on a draft site");
    }
    PortletPreferences preferences = request.getPreferences();
    Map<String, String> parameters = new HashMap<>();
    Enumeration<String> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      String parameterName = parameterNames.nextElement();
      parameters.put(parameterName, request.getParameter(parameterName));
    }
    Map<String, String> settings;
    try {
      settings = getSettingsService().getSettingsToStore(preferences.getValue(NAME, null), getCurrentUsername(), parameters);
    } catch (IllegalAccessException e) {
      throw new PortletException("User is not allowed to edit settings", e);
    } catch (IllegalArgumentException e) {
      throw new PortletException("Settings refused: " + e.getMessage(), e);
    }
    for (Map.Entry<String, String> setting : settings.entrySet()) {
      preferences.setValue(setting.getKey(), setting.getValue());
    }
    // Stored through the request, whose render then reads it back: a view of
    // the window read before its copy was consumed would restore the payload
    preferences.setValue(DATA_INIT_PREFERENCE_NAME, "");
    preferences.store();
  }

  @Override
  protected boolean canEdit(String name, Identity userAclIdentity) {
    return !isDraftSite()
           && getSettingsService().canEditSettings(name, userAclIdentity == null ? null : userAclIdentity.getUserId());
  }

  /**
   * Names a window rendered with no name, a new window or a copy: the name
   * another render stored meanwhile is adopted, so that two first renders of
   * a copy initialize it once, under one name.
   */
  @Override
  protected String generateRandomId() {
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    if (uiPortlet == null || StringUtils.isBlank(uiPortlet.getStorageId())) {
      return super.generateRandomId();
    }
    return getCopyService().getOrStoreSettingName(uiPortlet.getStorageId(), () -> generateSettingName(uiPortlet));
  }

  @Override
  protected void saveSettingName(String name, String pageReference, long spaceId) {
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    WindowKind windowKind = uiPortlet == null ? null : getWindowKind(uiPortlet);
    String settingPageReference;
    if (windowKind == WindowKind.SHARED_LAYOUT) {
      settingPageReference = MyApplicationsSettingsService.SHARED_LAYOUT_REFERENCE;
    } else if (windowKind == WindowKind.SECTION_TEMPLATE_DRAFT) {
      // The draft page itself, which CMSPortlet would cut at _draft_ into
      // global::section: its name tells the template and the administrator
      settingPageReference = uiPortlet.getAncestorOfType(UIPage.class).getPageId();
    } else {
      settingPageReference = getSettingsService().getSettingPageReference(name, pageReference, getLayoutSiteKey());
    }
    super.saveSettingName(name, settingPageReference, getSettingsService().getContextSpaceId(name, settingPageReference));
  }

  /**
   * Deletes, for a window of a section-template draft page named by this
   * render, what the template's earlier editing sessions left. A failure
   * leaves them, and does not fail the render.
   */
  @Override
  protected void postSettingInit(PortletPreferences preferences, String name) {
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    if (uiPortlet == null || getWindowKind(uiPortlet) != WindowKind.SECTION_TEMPLATE_DRAFT) {
      return;
    }
    UIPage uiPage = uiPortlet.getAncestorOfType(UIPage.class);
    try {
      getApplicationCenterService().deleteSectionTemplateDraftLeftovers(name, uiPage.getPageId(), getWindowStorageIds(uiPage));
    } catch (RuntimeException e) {
      LOG.warn("Unable to delete the applications earlier sessions of section template draft {} left", uiPage.getPageId(), e);
    }
  }

  /**
   * Writes a preference of the rendered window over its committed
   * preferences: the render's own view of them, read when it started, would
   * restore what another render changed since.
   */
  @Override
  protected void savePreference(String name, String value) {
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    if (uiPortlet == null || StringUtils.isBlank(uiPortlet.getStorageId())) {
      super.savePreference(name, value);
      return;
    }
    getCopyService().saveWindowPreference(uiPortlet.getStorageId(), name, value);
  }

  /**
   * Moves a legacy header title to the window's setting name, then consumes
   * what a copy of a window carries, at the first render that finds it, and
   * at each render until it is consumed: the applications it carries are
   * created in the copy's own context, its list names them, and data.init is
   * blanked. Nothing is created on a draft site, whose windows keep their own
   * setting. A failure is logged and the render goes on with the window's
   * list as it is.
   */
  @Override
  protected void setViewRequestAttributes(String name, RenderRequest request, RenderResponse response) {
    PortletPreferences preferences = request.getPreferences();
    String legacyApplicationId = preferences.getValue(APPLICATION_ID, null);
    if (StringUtils.isNotBlank(legacyApplicationId)) {
      getSettingsService().copyHeaderTitle(legacyApplicationId, name);
      savePreference(APPLICATION_ID, "");
    }
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    if (StringUtils.isBlank(preferences.getValue(DATA_INIT_PREFERENCE_NAME, null))
        || isDraftSite()
        || uiPortlet == null
        || StringUtils.isBlank(uiPortlet.getStorageId())) {
      return;
    }
    try {
      Map<String, String> window = getCopyService().consumeWindowCopy(uiPortlet.getStorageId(), name);
      if (window != null) {
        setRequestPreference(preferences,
                             MyApplicationsSettingsService.APPLICATION_IDS,
                             window.get(MyApplicationsSettingsService.APPLICATION_IDS));
        setRequestPreference(preferences, DATA_INIT_PREFERENCE_NAME, window.get(DATA_INIT_PREFERENCE_NAME));
      }
    } catch (RuntimeException e) {
      LOG.warn("Unable to create the applications the copy {} carries", name, e);
    }
  }

  /**
   * Gives a window its own setting name when its preferences carry one it
   * must not keep, a copy's or a forged one, so that it never acts in the
   * context of another window's setting: the new name is stored, set on the
   * request preferences and given the header title of the old one. Nothing is
   * written on a draft site, where the site layout editor renders the site,
   * nor for a window without a storage id, as every window of a configured
   * shared layout, whose name must be configured.
   */
  private void checkSettingName(PortletPreferences preferences) throws PortletException {
    String name = preferences.getValue(NAME, null);
    UIPortlet uiPortlet = UIPortlet.getCurrentUIPortlet();
    UIPage uiPage = Util.getUIPage();
    if (StringUtils.isBlank(name)
        || uiPortlet == null
        || StringUtils.isBlank(uiPortlet.getStorageId())
        || uiPage == null
        || isDraftSite()) {
      return;
    }
    WindowKind windowKind = getWindowKind(uiPortlet);
    if (!getSettingsService().keepsSettingName(name,
                                               uiPortlet.getStorageId(),
                                               windowKind,
                                               getSettingsService().getSettingPageReference(name,
                                                                                            getPageReference(uiPage),
                                                                                            getLayoutSiteKey()),
                                               () -> getOtherWindowNames(uiPortlet, uiPage, windowKind))) {
      String newName = generateSettingName(uiPortlet);
      savePreference(NAME, newName);
      try {
        preferences.setValue(NAME, newName);
      } catch (ReadOnlyException e) {
        throw new PortletException("Unable to rename the setting of the window", e);
      }
      getSettingsService().copyHeaderTitle(name, newName);
    }
  }

  /**
   * @return where a window is placed: a page window has a page ancestor, a
   *         site-layout window a site one only, a shared-layout window neither
   */
  private WindowKind getWindowKind(UIPortlet uiPortlet) {
    UIPage uiPage = uiPortlet.getAncestorOfType(UIPage.class);
    if (uiPage != null) {
      return getSettingsService().getSectionTemplateId(uiPage.getPageId()) > 0 ? WindowKind.SECTION_TEMPLATE_DRAFT :
                                                                               WindowKind.PAGE;
    } else if (uiPortlet.getAncestorOfType(UIPortal.class) != null) {
      return WindowKind.SITE_LAYOUT;
    } else {
      return WindowKind.SHARED_LAYOUT;
    }
  }

  /**
   * @return the setting names of the other windows of the page, of the site
   *         and its page for a site-layout window
   */
  private Collection<String> getOtherWindowNames(UIPortlet uiPortlet, UIPage uiPage, WindowKind windowKind) {
    List<UIPortlet> windows = new ArrayList<>();
    if (windowKind == WindowKind.SITE_LAYOUT) {
      UIPortal uiPortal = Util.getUIPortal();
      if (uiPortal != null) {
        uiPortal.findComponentOfType(windows, UIPortlet.class);
      }
    }
    uiPage.findComponentOfType(windows, UIPortlet.class);
    return windows.stream()
                  .filter(window -> !StringUtils.equals(window.getStorageId(), uiPortlet.getStorageId()))
                  .map(this::getSettingName)
                  .filter(Objects::nonNull)
                  .toList();
  }

  private String getSettingName(UIPortlet window) {
    try {
      return window.getPreferences().getValue(NAME);
    } catch (Exception e) {
      LOG.debug("Unable to read the preferences of window {}", window.getStorageId(), e);
      return null;
    }
  }

  /**
   * @return a new setting name for the window, marked for its kind, that no
   *         setting holds yet
   */
  private String generateSettingName(UIPortlet uiPortlet) {
    WindowKind windowKind = getWindowKind(uiPortlet);
    long sectionTemplateId = windowKind == WindowKind.SECTION_TEMPLATE_DRAFT ?
                                                                             getSettingsService().getSectionTemplateId(uiPortlet.getAncestorOfType(UIPage.class)
                                                                                                                                .getPageId()) :
                                                                             0;
    String name;
    do {
      name = sectionTemplateId > 0 ?
                                   getSettingsService().generateSectionTemplateDraftSettingName(uiPortlet.getStorageId(),
                                                                                                sectionTemplateId) :
                                   getSettingsService().generateSettingName(uiPortlet.getStorageId(), windowKind);
    } while (isSettingNameExists(name));
    return name;
  }

  /**
   * @return the storage ids of the windows of a page
   */
  private List<String> getWindowStorageIds(UIPage uiPage) {
    List<UIPortlet> windows = new ArrayList<>();
    uiPage.findComponentOfType(windows, UIPortlet.class);
    return windows.stream().map(UIPortlet::getStorageId).filter(Objects::nonNull).toList();
  }

  private void setRequestPreference(PortletPreferences preferences, String name, String value) {
    try {
      preferences.setValue(name, value);
    } catch (ReadOnlyException e) {
      LOG.debug("Unable to set preference {} on the rendered window", name, e);
    }
  }

  /**
   * @return the page reference CMSPortlet records: a draft page as the page
   *         it is a draft of
   */
  private String getPageReference(UIPage uiPage) {
    String pageReference = uiPage.getPageId();
    return StringUtils.contains(pageReference, DRAFT_SUFFIX) ? pageReference.split(DRAFT_SUFFIX)[0] : pageReference;
  }

  /**
   * @return the site whose layout holds the site-layout windows of the
   *         request: portal renders a displayed site with the meta portal's
   *         layout, any other site with its own
   */
  private SiteKey getLayoutSiteKey() {
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    if (requestContext == null) {
      return null;
    }
    PortalConfig portalConfig = requestContext.getPortalConfig();
    return portalConfig != null && portalConfig.isDisplayed() ? SiteKey.portal(requestContext.getMetaPortal()) :
                                                              requestContext.getSiteKey();
  }

  private boolean isDraftSite() {
    PortalRequestContext requestContext = Util.getPortalRequestContext();
    return requestContext != null && requestContext.isDraftSite();
  }

  private String getCurrentUsername() {
    ConversationState conversationState = ConversationState.getCurrent();
    return conversationState == null ? null : conversationState.getIdentity().getUserId();
  }

  private MyApplicationsSettingsService getSettingsService() {
    if (settingsService == null) {
      settingsService = ExoContainerContext.getService(MyApplicationsSettingsService.class);
    }
    return settingsService;
  }

  private MyApplicationsCopyService getCopyService() {
    if (copyService == null) {
      copyService = ExoContainerContext.getService(MyApplicationsCopyService.class);
    }
    return copyService;
  }

  private ApplicationCenterService getApplicationCenterService() {
    if (applicationCenterService == null) {
      applicationCenterService = ExoContainerContext.getService(ApplicationCenterService.class);
    }
    return applicationCenterService;
  }

}
