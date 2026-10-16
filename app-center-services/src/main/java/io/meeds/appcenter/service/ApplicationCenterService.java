/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.Metadata;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.constant.PlacementSide;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCenterSettings;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationListFilter;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.ApplicationPlacements;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.ApplicationCategoryStorage;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.appcenter.storage.ApplicationPlacementStorage;
import io.meeds.appcenter.storage.PortletWindowStorage;
import io.meeds.appcenter.util.ImageTypeUtils;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.portal.navigation.service.NavigationConfigurationService;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

import lombok.SneakyThrows;

/**
 * A Service to access and store applications
 */
@Service
public class ApplicationCenterService {

  private static final Log         LOG                                 = ExoLogger.getLogger(ApplicationCenterService.class);

  private static final String      APPLICATION_IS_MANDATORY_MESSAGE    = "application is mandatory";

  public static final String       ANY_PERMISSION                      = "any";

  public static final String       DEFAULT_USERS_GROUP                 = "/platform/users";

  public static final String       DEFAULT_USERS_PERMISSION            = "*:" + DEFAULT_USERS_GROUP;

  public static final String       EXTERNAL_USERS_PERMISSION           = "*:/platform/externals";

  public static final String       MAX_FAVORITE_APPS                   = "maxFavoriteApps";

  /**
   * SettingService key under which the whole app-center configuration is stored,
   * serialized as a single JSON blob (see {@link ApplicationCenterSettings}).
   * Storing all settings under one key lets new settings be added as POJO fields
   * without introducing a new setting key nor an upgrade plugin.
   */
  public static final String       APP_CENTER_SETTINGS_KEY             = "appCenterSettings";

  public static final String       DEFAULT_APP_IMAGE_NAME              = "defaultAppImageName";

  public static final String       DEFAULT_APP_IMAGE_BODY              = "defaultAppImageBody";

  public static final int          DEFAULT_LIMIT                       = 10;

  public static final Context      APP_CENTER_CONTEXT                  = Context.GLOBAL.id("APP_CENTER");

  public static final Scope        APP_CENTER_SCOPE                    = Scope.APPLICATION.id("APP_CENTER");

  /** Most applications a Shortcuts portlet lists in its SELECTED mode (D6). */
  public static final int          MAX_LISTED_APPLICATIONS             = 100;

  /** The bundle of the titles the portlets show system applications with. */
  public static final String       SYSTEM_APPLICATIONS_BUNDLE          = "locale.addon.appcenter";

  /** Most suggestions returned by one call of the Shortcuts app suggester. */
  public static final int          MAX_SUGGESTIONS                     = 20;

  /** Largest illustration of an application created from a Shortcuts portlet, in bytes. */
  public static final int          MAX_ILLUSTRATION_SIZE               = 102400;

  public static final String       NOT_FAVORITABLE_MESSAGE             = "appCenter.contextual.notFavoritable";

  public static final String       NOT_CONTEXTUAL_MESSAGE              = "appCenter.contextual.notContextual";

  public static final String       NOT_OWNED_MESSAGE                   = "appCenter.contextual.notOwned";

  public static final String       INVALID_UPLOAD_MESSAGE              = "appCenter.illustration.invalidUpload";

  public static final String       INVALID_TYPE_MESSAGE                = "appCenter.illustration.invalidType";

  public static final String       TOO_LARGE_MESSAGE                   = "appCenter.illustration.tooLarge";

  public static final String       TITLE_MANDATORY_MESSAGE             = "appCenter.contextual.titleMandatory";

  public static final String       TOO_MANY_CATEGORIES_MESSAGE         = "appCenter.contextual.tooManyCategories";

  public static final String       INVALID_URL_MESSAGE                 = "appCenter.contextual.invalidUrl";

  public static final String       INVALID_HELP_PAGE_URL_MESSAGE       = "appCenter.contextual.invalidHelpPageURL";

  public static final String       INVALID_ICON_MESSAGE                = "appCenter.contextual.invalidIcon";

  /** Prefix of the codes naming a field: {@code <prefix><field>TooLong} */
  public static final String       CONTEXTUAL_MESSAGE_PREFIX           = "appCenter.contextual.";

  private static final String      SYSTEM_APPLICATION_KEY_PREFIX       = "appCenter.system.application.";

  private static final Pattern     WHITESPACE                          = Pattern.compile("\\s");

  private static final String      USERNAME_IS_MANDATORY_MESSAGE       = "username is mandatory";

  private static final String      APPLICATION_ID_IS_MANDATORY_MESSAGE = "applicationId is mandatory";

  private static final String      USER_NOT_ALLOWED_MESSAGE            = "User %s is not allowed to save application : %s";

  private static final String      APPLICATION_NOT_FOUND_MESSAGE       = "Application with id %s doesn't exist";

  private static final String      PERSONAL_APPS_NOT_ENABLED_MESSAGE   = "User personal apps feature is not enabled";

  private static final String      NOT_PERSONAL_APP_OWNER_MESSAGE      = "User %s is not the owner of personal application %s";

  private static final String      INVALID_PERSONAL_URL_MESSAGE        = "url %s is not a valid http(s) link";

  private static final String      SIDE_IS_MANDATORY_MESSAGE           = "side is mandatory";

  private static final String      STICK_NOT_ALLOWED_MESSAGE           = "appCenter.placement.stickNotAllowed";

  private static final String      USER_CANNOT_ACCESS_APP_MESSAGE      = "User %s is not allowed to access application %s";

  private static final Pattern     SCHEME_PATTERN                      = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

  private static final Pattern     CONTROL_CHARS_PATTERN               = Pattern.compile("[\\t\\n\\r]");

  private static final String      SETTING_NOT_FOUND_MESSAGE           = "Shortcuts portlet setting %s doesn't exist";

  private static final String      ANY_MEMBERSHIP                      = "*:";

  /** Name of a drawer an application of type DRAWER opens */
  private static final Pattern     DRAWER_NAME_PATTERN                 = Pattern.compile("^[A-Za-z0-9_-]{1,100}$");

  /** Name of the font icon of an application */
  private static final Pattern     ICON_PATTERN                        = Pattern.compile("^[a-z0-9 -]{1,100}$");

  /** Lengths of the AC_APPLICATION columns a Shortcuts portlet form fills */
  private static final int         MAX_TITLE_LENGTH                    = 250;

  private static final int         MAX_DESCRIPTION_LENGTH              = 1000;

  private static final int         MAX_URL_LENGTH                      = 500;

  /** The metadata type of social's categories, which social keeps private */
  private static final String      CATEGORY_METADATA_TYPE              = "category";

  @Autowired
  private UserACL                  userAcl;

  @Autowired
  private SettingService           settingService;

  @Autowired
  private ApplicationCenterStorage appCenterStorage;

  @Autowired
  private ApplicationPlacementStorage placementStorage;

  @Autowired
  private NavigationConfigurationService navigationConfigurationService;

  @Autowired
  private UserPortalConfigService  userPortalConfigService;

  @Autowired
  private ApplicationBadgePluginRegistry badgePluginRegistry;

  @Autowired
  private TranslationService       translationService;

  @Autowired
  private FileService              fileService;

  @Autowired
  private ImageThumbnailService    imageThumbnailService;

  @Autowired
  private PortalContainer          portalContainer;

  @Autowired
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @Autowired
  private ApplicationCategoryStorage    applicationCategoryStorage;

  @Autowired
  private ResourceBundleService         resourceBundleService;

  @Autowired
  private SpaceService                  spaceService;

  @Autowired
  private PortletInstanceService        portletInstanceService;

  @Autowired
  private MetadataService               metadataService;

  @Autowired
  private PortletWindowStorage          windowStorage;

  private CategoryLinkService      categoryLinkService;

  @Value("${appcenter.administrators.expression:*:/platform/administrators}") // NOSONAR
  private String                   defaultAdministratorPermission      = null;

  @Value("${appcenter.favorites.count:12}") // NOSONAR
  private long                     defaultMaxFavoriteApps;

  /**
   * Create new Application that will be available for all users.
   *
   * @param application application to create
   * @param username user making the operation
   * @return stored {@link Application} in datasource
   * @throws IllegalAccessException if user is not allowed to create an
   *           application
   */
  public Application createApplication(Application application, String username) throws IllegalAccessException {
    if (!canEdit(username)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE,
                                                     username,
                                                     application.getTitle()));
    }
    return createApplication(application);
  }

  /**
   * Create new Application that will be available for all users.
   *
   * @param application application to create
   * @return stored {@link Application} in datasource
   */
  public Application createApplication(Application application) {
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    return appCenterStorage.createApplication(application);
  }

  /**
   * Get an application by id
   *
   * @param applicationId application to find
   * @return stored {@link Application} in datasource
   */
  public Application getApplication(long applicationId) {
    return appCenterStorage.getApplication(applicationId);
  }

  public Application findSystemApplicationByUrl(String url) {
    return appCenterStorage.findSystemApplicationByUrl(url);
  }

  /**
   * Update an existing application on datasource. If the application doesn't
   * exit an {@link ApplicationNotFoundException} will be thrown.
   *
   * @param application dto to update on store
   * @param username username storing application
   * @throws IllegalAccessException if user is not allowed to update application
   * @throws ApplicationNotFoundException if application wasn't found
   */
  public void updateApplication(Application application, String username) throws IllegalAccessException,
                                                                          ApplicationNotFoundException {
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    Long applicationId = application.getId();
    if (applicationId == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    Application storedApplication = appCenterStorage.getApplication(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (StringUtils.isBlank(username) || !canEdit(username)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE,
                                                     username,
                                                     application.getTitle()));
    }
    if (storedApplication.isContextual()) {
      // Whatever an administrator sends, an application created from a
      // Shortcuts portlet stays out of the users' Application Center
      setContextualForcedFields(application);
    }
    updateApplication(application);
  }

  public void updateApplication(Application application) {
    appCenterStorage.updateApplication(application);
  }

  /**
   * Delete application identified by its id and check if username has
   * permission to delete it.
   *
   * @param applicationId technical identifier of application
   * @param username user currently deleting application
   * @throws IllegalAccessException if user is not allowed to delete application
   * @throws ApplicationNotFoundException if application wasn't found
   */
  public void deleteApplication(Long applicationId, String username) throws ApplicationNotFoundException,
                                                                     IllegalAccessException {
    if (applicationId == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }

    Application storedApplication = appCenterStorage.getApplication(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (!canEdit(username)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE,
                                                     username,
                                                     storedApplication.getTitle()));
    } else if (storedApplication.isSystem()) {
      throw new IllegalAccessException(String.format("Application with id %s is a system application, thus it can't be deleted",
                                                     applicationId));
    }
    deleteApplication(applicationId);
  }

  public void deleteApplication(Long applicationId) throws ApplicationNotFoundException {
    Application application = appCenterStorage.getApplication(applicationId);
    if (application != null) {
      appCenterStorage.deleteApplication(applicationId);
      unlinkCategories(applicationId);
      if (application.isContextual() && application.getImageFileId() != null) {
        // The server stored the illustration of an application created from
        // a Shortcuts portlet for that application alone
        appCenterStorage.deleteIllustration(application.getImageFileId());
      }
    }
  }

  /**
   * Add an application, identified by its technical id, as favorite of a user
   *
   * @param applicationId technical application id
   * @param username user login
   * @throws ApplicationNotFoundException when application is not found
   * @throws IllegalAccessException if user hasn't access permission to the
   *           application
   * @throws IllegalArgumentException with the message
   *           {@value #NOT_FAVORITABLE_MESSAGE} for an application created from
   *           a Shortcuts portlet
   */
  public void addFavoriteApplication(long applicationId, String username) throws ApplicationNotFoundException,
                                                                          IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    Application application = getFavoritableApplication(applicationId, username);
    appCenterStorage.addApplicationToUserFavorite(applicationId, username);
    reorderFavoritesForNewEntry(application.getId(), username);
  }

  /**
   * Deletes an application identified by its id from favorite applications of
   * user
   *
   * @param applicationId application technical identifier
   * @param username login of user currently deleting application
   */
  public void deleteFavoriteApplication(Long applicationId, String username) {
    if (applicationId == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    } else if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    appCenterStorage.deleteApplicationFavorite(applicationId, username);
  }

  /**
   * Returns the global app-center settings, deserialized from the single JSON
   * blob stored in {@link SettingService}. When no blob is stored yet, sane
   * defaults are returned (see {@link #getStoredSettings()}).
   *
   * @return the current {@link ApplicationCenterSettings}, never {@code null}
   */
  public ApplicationCenterSettings getSettings() {
    return getStoredSettings();
  }

  /**
   * Persists the whole app-center settings blob. The incoming settings are
   * merged onto the currently stored ones (so a partial update never drops
   * fields it didn't set) then serialized back as a single JSON blob. Admin
   * only.
   *
   * @param settings the settings to persist, must not be {@code null}
   * @param username the user performing the operation
   * @throws IllegalAccessException if the user is not allowed to change
   *           app-center settings
   */
  public void saveSettings(ApplicationCenterSettings settings, String username) throws IllegalAccessException {
    if (settings == null) {
      throw new IllegalArgumentException("settings is mandatory");
    }
    if (!canEdit(username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to change app-center settings", username));
    }
    ApplicationCenterSettings merged = getStoredSettings();
    merged.setAllowUserPersonalApps(settings.isAllowUserPersonalApps());
    saveStoredSettings(merged);
  }

  /**
   * Returns whether users are allowed to create personal URL apps.
   *
   * @return {@code true} if the personal URL apps feature is enabled
   */
  public boolean isUserPersonalAppsEnabled() {
    return getStoredSettings().isAllowUserPersonalApps();
  }

  /**
   * Enables or disables the user personal apps feature. Admin only. The value is
   * merged into the settings JSON blob (see {@link #saveSettings}).
   *
   * @param enabled whether the personal apps feature should be enabled
   * @param username the user performing the operation
   * @throws IllegalAccessException if the user is not allowed to change
   *           app-center settings
   */
  public void setUserPersonalAppsEnabled(boolean enabled, String username) throws IllegalAccessException {
    if (!canEdit(username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to change app-center settings", username));
    }
    ApplicationCenterSettings settings = getStoredSettings();
    settings.setAllowUserPersonalApps(enabled);
    saveStoredSettings(settings);
  }

  /**
   * Reads and deserializes the app-center settings JSON blob, falling back to a
   * default {@link ApplicationCenterSettings} instance when nothing is stored or
   * the blob can't be parsed.
   *
   * @return the stored {@link ApplicationCenterSettings}, never {@code null}
   */
  private ApplicationCenterSettings getStoredSettings() {
    SettingValue<?> value = settingService.get(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, APP_CENTER_SETTINGS_KEY);
    if (value != null && value.getValue() != null) {
      String json = String.valueOf(value.getValue());
      if (StringUtils.isNotBlank(json)) {
        try {
          ApplicationCenterSettings settings = JsonUtils.fromJsonString(json, ApplicationCenterSettings.class);
          if (settings != null) {
            return settings;
          }
        } catch (Exception e) {
          LOG.warn("Unable to parse app-center settings JSON blob, falling back to defaults", e);
        }
      }
    }
    return new ApplicationCenterSettings();
  }

  /**
   * Serializes the given settings to JSON and stores them under the single
   * {@link #APP_CENTER_SETTINGS_KEY} setting.
   *
   * @param settings the settings to serialize and store
   */
  private void saveStoredSettings(ApplicationCenterSettings settings) {
    settingService.set(APP_CENTER_CONTEXT,
                       APP_CENTER_SCOPE,
                       APP_CENTER_SETTINGS_KEY,
                       SettingValue.create(JsonUtils.toJsonString(settings)));
  }

  /**
   * Creates a personal URL application for the given user.
   */
  public Application createPersonalApplication(Application application, String username) throws IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (!isUserPersonalAppsEnabled()) {
      throw new IllegalAccessException(PERSONAL_APPS_NOT_ENABLED_MESSAGE);
    }
    application.setUrl(normalizePersonalUrl(application.getUrl()));
    application.setPersonal(true);
    application.setPermissions(Collections.singletonList(username));
    application.setActive(true);
    application.setSystem(false);
    application.setMandatory(false);
    // a personal app is a shortcut the user created for themselves, it has to be
    // usable wherever they are: the personal form has no mobile switch on
    // purpose, so the flag is forced here instead of being left to the client
    application.setMobile(true);
    application.setAllowStick(false);
    application.setAllowDetach(false);
    Application saved = createApplication(application);
    try {
      addFavoriteApplication(saved.getId(), username);
    } catch (Exception e) {
      LOG.warn("Failed to auto-add personal app {} to favorites for user {}", saved.getId(), username, e);
    }
    return saved;
  }

  /**
   * Updates a personal URL application. Only the owner may update it.
   */
  public void updatePersonalApplication(Application application, String username) throws IllegalAccessException,
                                                                                  ApplicationNotFoundException {
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    if (application.getId() == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (!isUserPersonalAppsEnabled()) {
      throw new IllegalAccessException(PERSONAL_APPS_NOT_ENABLED_MESSAGE);
    }
    Application stored = appCenterStorage.getApplication(application.getId());
    if (stored == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, application.getId()));
    }
    if (!stored.isPersonal()) {
      throw new IllegalAccessException(String.format("Application %s is not a personal app", application.getId()));
    }
    if (!isOwner(stored, username)) {
      throw new IllegalAccessException(String.format(NOT_PERSONAL_APP_OWNER_MESSAGE, username, application.getId()));
    }
    application.setUrl(normalizePersonalUrl(application.getUrl()));
    application.setPersonal(true);
    application.setPermissions(stored.getPermissions());
    application.setActive(true);
    application.setSystem(false);
    application.setMandatory(false);
    application.setMobile(true);
    application.setAllowStick(false);
    application.setAllowDetach(false);
    updateApplication(application);
  }

  /**
   * Retrieves the applications the given user stuck to each side of the
   * layout, resolved and re-validated at read time, along with whether the
   * given site is allowed to display stuck panels: only the meta site and the
   * sites listed in its sidebar configuration are, by design.
   */
  public ApplicationPlacements getApplicationPlacements(String username, String siteName) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    return new ApplicationPlacements(isPlacementEligibleSite(siteName),
                                     resolvePlacedApplication(username, PlacementSide.LEFT),
                                     resolvePlacedApplication(username, PlacementSide.RIGHT));
  }

  /**
   * A placement is validated at stick time, but the world moves afterwards:
   * the administrator can deactivate the application, revoke the user's
   * access or turn the stick capability off. Every read therefore
   * re-validates the stored placement and drops AND clears it when it no
   * longer qualifies, so callers only ever render what the read returns.
   */
  private Application resolvePlacedApplication(String username, PlacementSide side) {
    Long applicationId = placementStorage.getPlacedApplicationId(username, side);
    if (applicationId == null) {
      return null;
    }
    Application application = appCenterStorage.getApplication(applicationId);
    boolean stillQualifies = application != null
                             && application.isActive()
                             && application.isAllowStick()
                             && !application.isContextual()
                             && canAccess(application, username);
    if (!stillQualifies) {
      placementStorage.removePlacedApplicationId(username, side);
      return null;
    }
    return application;
  }

  public boolean isPlacementEligibleSite(String siteName) {
    return StringUtils.isNotBlank(siteName)
           && (StringUtils.equals(siteName, userPortalConfigService.getMetaPortal())
               || navigationConfigurationService.isMetaSiteNavigation(siteName));
  }

  /**
   * Sticks an application to one side of the layout for the given user,
   * replacing the application previously stuck to that side if any.
   */
  public void stickApplication(long applicationId, PlacementSide side, String username) throws IllegalAccessException,
                                                                                        ApplicationNotFoundException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (side == null) {
      throw new IllegalArgumentException(SIDE_IS_MANDATORY_MESSAGE);
    }
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (!canAccess(application, username)) {
      throw new IllegalAccessException(String.format(USER_CANNOT_ACCESS_APP_MESSAGE, username, applicationId));
    }
    // An application created from a Shortcuts portlet is used in that list only
    if (!application.isActive() || !application.isAllowStick() || application.isContextual()) {
      throw new IllegalArgumentException(STICK_NOT_ALLOWED_MESSAGE);
    }
    PlacementSide otherSide = side == PlacementSide.LEFT ? PlacementSide.RIGHT : PlacementSide.LEFT;
    Long otherSideApplicationId = placementStorage.getPlacedApplicationId(username, otherSide);
    if (otherSideApplicationId != null && otherSideApplicationId.longValue() == applicationId) {
      placementStorage.removePlacedApplicationId(username, otherSide);
    }
    placementStorage.setPlacedApplicationId(username, side, applicationId);
  }

  /**
   * Removes the application stuck to the given side for the given user. A side
   * already empty is left as is, so the operation stays idempotent.
   */
  public void unstickApplication(PlacementSide side, String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (side == null) {
      throw new IllegalArgumentException(SIDE_IS_MANDATORY_MESSAGE);
    }
    placementStorage.removePlacedApplicationId(username, side);
  }

  /**
   * Normalizes an URL entered by an end user for a personal application. The
   * scheme is optional, so that "meeds.io" is stored as "https://meeds.io"; when
   * it is omitted the host must be qualified, so that a plain word is rejected
   * instead of silently becoming a link to a non existing host. Only http and
   * https are accepted, which rules out "javascript:" links. Portal relative
   * links ("/portal/dw", "./dw") are kept as is, but scheme relative ones
   * ("//host", which a browser resolves to an external origin) are rejected.
   * This method is the security boundary: the mirror written in
   * ApplicationUrlService.js only exists to give an immediate feedback in the
   * form, and both must be updated together.
   *
   * @param url the URL as entered by the user
   * @return the normalized URL, either an absolute http(s) link or a portal
   *         relative one
   * @throws IllegalArgumentException if the URL is blank or isn't an http(s)
   *           link
   */
  protected String normalizePersonalUrl(String url) {
    // browsers drop tabs and line breaks before parsing an URL, so they must not
    // be able to hide either a scheme ("java<TAB>script:") or a scheme relative
    // prefix ("/<TAB>/host")
    String trimmed = StringUtils.trim(RegExUtils.removeAll(url, CONTROL_CHARS_PATTERN));
    if (StringUtils.isBlank(trimmed)) {
      throw new IllegalArgumentException("url is mandatory");
    }
    if (StringUtils.startsWith(trimmed, "/") || StringUtils.startsWith(trimmed, "./")) {
      if (StringUtils.startsWithAny(trimmed, "//", "/\\")) {
        throw new IllegalArgumentException(String.format(INVALID_PERSONAL_URL_MESSAGE, url));
      }
      return trimmed;
    }
    boolean schemeOmitted = !SCHEME_PATTERN.matcher(trimmed).find();
    String withScheme = schemeOmitted ? "https://" + trimmed : trimmed;
    URI uri;
    try {
      uri = new URI(withScheme);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(String.format(INVALID_PERSONAL_URL_MESSAGE, url), e);
    }
    if (!StringUtils.equalsAnyIgnoreCase(uri.getScheme(), "http", "https")
        || StringUtils.isBlank(uri.getHost())
        || (schemeOmitted && !StringUtils.contains(uri.getHost(), '.'))) {
      throw new IllegalArgumentException(String.format(INVALID_PERSONAL_URL_MESSAGE, url));
    }
    return withScheme;
  }

  /**
   * Deletes a personal URL application. Only the owner may delete it.
   */
  public void deletePersonalApplication(Long applicationId, String username) throws IllegalAccessException,
                                                                              ApplicationNotFoundException {
    if (applicationId == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (!isUserPersonalAppsEnabled()) {
      throw new IllegalAccessException(PERSONAL_APPS_NOT_ENABLED_MESSAGE);
    }
    Application stored = appCenterStorage.getApplication(applicationId);
    if (stored == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (!stored.isPersonal()) {
      throw new IllegalAccessException(String.format("Application %s is not a personal app", applicationId));
    }
    if (!isOwner(stored, username)) {
      throw new IllegalAccessException(String.format(NOT_PERSONAL_APP_OWNER_MESSAGE, username, applicationId));
    }
    deleteApplication(applicationId);
  }

  /**
   * Retrieves the list of applications with offset, limit and a keyword that
   * can be empty
   *
   * @param offset offset of the query
   * @param limit limit of the query that can be less or equal to 0, which mean,
   *          getting all available applications
   * @param keyword used to search in title and url
   * @return {@link ApplicationList} that contains the list of applications
   */
  public ApplicationList getApplications(int offset, int limit, String keyword) {
    return getApplications(offset, limit, keyword, null);
  }

  /**
   * Retrieves the list of applications with offset, limit and a keyword that
   * can be empty
   *
   * @param offset offset of the query
   * @param limit limit of the query that can be less or equal to 0, which mean,
   *          getting all available applications
   * @param keyword used to search in title and url
   * @param locale used language to retrieve application title and description
   * @return {@link ApplicationList} that contains the list of applications
   */
  public ApplicationList getApplications(int offset, int limit, String keyword, Locale locale) {
    ApplicationList applicationList = new ApplicationList();
    // The applications of a section template's draft windows are the
    // editor's working state, listed by those windows only
    List<Application> applications = appCenterStorage.getApplications(keyword)
                                                     .stream()
                                                     .filter(app -> !app.isPersonal())
                                                     .filter(app -> !myApplicationsSettingsService.isSectionTemplateDraftName(app.getContextSettingName()))
                                                     .toList();
    int totalApplictions = applications.size();
    if (limit <= 0) {
      limit = applications.size();
    }
    applications = applications.stream().skip(offset).limit(limit).toList();
    setApplicationLabels(applications, locale);
    setApplicationCategories(applications);
    setApplicationBadges(applications);
    applicationList.setApplications(applications);
    applicationList.setSize(totalApplictions);
    applicationList.setOffset(offset);
    applicationList.setLimit(limit);
    return applicationList;
  }

  /**
   * Retrieves the list of applications switch offset and limit of the query, a
   * keyword to filter on title and url of {@link Application} and the username
   * to filter on authorized applications
   *
   * @param offset offset of the query
   * @param limit limit of the query that can be less or equal to 0, which mean,
   *          getting all available applications
   * @param keyword used to search in title and url
   * @param username login of user to use to filter on authorized applications
   * @return {@link ApplicationList} that contains the {@link List} of
   *         authorized {@link UserApplication}
   */
  public ApplicationList getActiveApplications(int offset,
                                               int limit,
                                               String keyword,
                                               String username) {
    return getActiveApplications(offset, limit, keyword, null, username);
  }

  /**
   * Retrieves the list of applications switch offset and limit of the query, a
   * keyword to filter on title and url of {@link Application} and the username
   * to filter on authorized applications
   *
   * @param offset offset of the query
   * @param limit limit of the query that can be less or equal to 0, which mean,
   *          getting all available applications
   * @param keyword used to search in title and url
   * @param username login of user to use to filter on authorized applications
   * @return {@link ApplicationList} that contains the {@link List} of
   *         authorized {@link UserApplication}
   */
  public ApplicationList getActiveApplications(int offset,
                                               int limit,
                                               String keyword,
                                               Locale locale,
                                               String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    ApplicationList resultApplicationsList = new ApplicationList();
    List<Application> applications = getActiveApplications(keyword, username).stream().toList();
    int totalApplication = applications.size();
    if (limit > 0) {
      applications = applications.stream()
                                 .skip(offset)
                                 .limit(limit)
                                 .toList();
    }
    applications = applications.stream()
                               .map(app -> {
                                 UserApplication applicationFavorite = new UserApplication(app);
                                 applicationFavorite.setFavorite(appCenterStorage.isFavoriteApplication(applicationFavorite.getId(),
                                                                                                        username));
                                 return (Application) applicationFavorite;
                               })
                               .toList();
    setApplicationLabels(applications, locale);
    setApplicationCategories(applications);
    setApplicationBadges(applications);
    resultApplicationsList.setApplications(applications);
    resultApplicationsList.setOffset(offset);
    resultApplicationsList.setLimit(limit);
    resultApplicationsList.setSize(totalApplication);
    return resultApplicationsList;
  }

  /**
   * Update favorite applications order for a user
   *
   * @param applicationOrder
   * @param userName
   * @deprecated use {@link #updateFavoriteApplicationsOrder(List, String)},
   *             which checks every application before it writes any order;
   *             since 7.3.0, for removal
   */
  @Deprecated(since = "7.3.0", forRemoval = true)
  @Transactional(rollbackFor = Exception.class)
  public void updateFavoriteApplicationOrder(ApplicationOrder applicationOrder,
                                             String userName) throws ApplicationNotFoundException,
                                                              IllegalAccessException {
    writeFavoriteApplicationsOrder(Collections.singletonList(applicationOrder), userName);
  }

  /**
   * Orders the favorite applications of a user, an application not yet
   * favorite becoming one, unless mandatory. Every application is checked
   * before any order is written: the batch is refused as a whole when one of
   * them does not exist, is not accessible to the user or was created from a
   * Shortcuts portlet. The orders are written in one transaction: a write
   * failing midway writes none.
   *
   * @param applicationOrders the applications and their order
   * @param userName the user
   * @throws ApplicationNotFoundException when an application does not exist
   * @throws IllegalAccessException when the user may not access an
   *           application
   * @throws IllegalArgumentException with the message
   *           {@value #NOT_FAVORITABLE_MESSAGE} for an application created from
   *           a Shortcuts portlet
   */
  @Transactional(rollbackFor = Exception.class)
  public void updateFavoriteApplicationsOrder(List<ApplicationOrder> applicationOrders,
                                              String userName) throws ApplicationNotFoundException,
                                                               IllegalAccessException {
    writeFavoriteApplicationsOrder(applicationOrders, userName);
  }

  /**
   * Checks every application of the batch, then writes the orders: run inside
   * the transaction of the public method calling it, never through a call on
   * this instance to another transactional method, which the proxy would not
   * see
   */
  private void writeFavoriteApplicationsOrder(List<ApplicationOrder> applicationOrders,
                                              String userName) throws ApplicationNotFoundException,
                                                               IllegalAccessException {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (applicationOrders == null
        || applicationOrders.stream()
                            .anyMatch(applicationOrder -> applicationOrder == null
                                                          || applicationOrder.getId() == null
                                                          || applicationOrder.getId() <= 0)) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    List<Application> applications = new ArrayList<>();
    for (ApplicationOrder applicationOrder : applicationOrders) {
      applications.add(getFavoritableApplication(applicationOrder.getId(), userName));
    }
    for (int i = 0; i < applicationOrders.size(); i++) {
      ApplicationOrder applicationOrder = applicationOrders.get(i);
      if (!applications.get(i).isMandatory()) {
        appCenterStorage.addApplicationToUserFavorite(applicationOrder.getId(), userName);
      }
      appCenterStorage.updateFavoriteApplicationOrder(applicationOrder.getId(), userName, applicationOrder.getOrder());
    }
  }

  /**
   * Return the {@link Application} illustration last modifed timestamp (in ms),
   * if not found, the default image last modifed timestamp will be retrieved
   *
   * @param applicationId technical id of application
   * @return timestamp in milliseconds of last modified date of illustration
   * @throws ApplicationNotFoundException if application wasn't found
   */
  public Long getApplicationImageLastUpdated(long applicationId) throws ApplicationNotFoundException {
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (application.getImageFileId() != null) {
      return appCenterStorage.getApplicationImageLastUpdated(application.getImageFileId());
    } else {
      return null;
    }
  }

  /**
   * @param applicationId technical id of application
   * @param token the token of an illustration URL
   * @param lastModified the version of the illustration the URL names
   * @return true when the token is the one the application's illustration URL
   *         carries for that version
   */
  public boolean isIllustrationTokenValid(long applicationId, String token, long lastModified) {
    return appCenterStorage.isIllustrationTokenValid(applicationId, token, lastModified);
  }

  /**
   * Return the {@link Application} illustration {@link InputStream}, if not
   * found, the default image {@link InputStream} will be retrieved
   *
   * @param applicationId technical id of application
   * @return {@link InputStream} of application illustration
   * @throws ApplicationNotFoundException if application wasn't found
   */
  public InputStream getApplicationImageInputStream(long applicationId) throws ApplicationNotFoundException {
    return getApplicationImageInputStream(applicationId, null);
  }

  /**
   * Return the {@link Application} illustration {@link InputStream}, if not
   * found, the default image {@link InputStream} will be retrieved
   *
   * @param applicationId technical id of application
   * @param dimensions Image dimensions to retrieve
   * @return {@link InputStream} of application illustration
   * @throws ApplicationNotFoundException if application wasn't found
   */
  @SneakyThrows
  public InputStream getApplicationImageInputStream(long applicationId, String dimensions) throws ApplicationNotFoundException {
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (application.getImageFileId() != null) {
      if (StringUtils.contains(dimensions, "x")) {
        FileItem fileItem = fileService.getFile(application.getImageFileId());
        Integer[] dimensionParts = Arrays.stream(dimensions.split("x")).map(Integer::parseInt).toArray(Integer[]::new);
        FileItem thumbnailFileItem = imageThumbnailService.getOrCreateThumbnail(fileItem,
                                                                                dimensionParts[0],
                                                                                dimensionParts[1]);
        return appCenterStorage.getApplicationImageInputStream(thumbnailFileItem.getFileInfo().getId());
      } else {
        return appCenterStorage.getApplicationImageInputStream(application.getImageFileId());
      }
    } else {
      return null;
    }
  }

  public List<Application> getSystemApplications() {
    return appCenterStorage.getSystemApplications();
  }

  public List<Long> getCategoryIds() {
    return getCategoryLinkService().getLinkedIds(ApplicationCategoryPlugin.OBJECT_TYPE);
  }

  /**
   * Reads the mandatory and favorite applications of a user, never one created
   * from a Shortcuts portlet, which only that portlet lists.
   *
   * @param pageable the page to read
   * @param username the user
   * @param locale the language of the labels, none when null
   * @return the applications
   */
  public ApplicationList getMandatoryAndFavoriteApplications(Pageable pageable, String username, Locale locale) {
    List<Application> applications = appCenterStorage.getMandatoryAndFavoriteApplications(username, pageable)
                                                     .stream()
                                                     .filter(app -> !app.isContextual())
                                                     .filter(app -> canAccess(app, username))
                                                     .collect(Collectors.toList());
    int appCount = applications.size();
    ApplicationList applicationList = new ApplicationList();
    setApplicationLabels(applications, locale);
    setApplicationCategories(applications);
    setApplicationBadges(applications);
    return applicationList.setApplications(applications)
                          .setLimit(appCount)
                          .setSize(appCount)
                          .setOffset(0);
  }

  /**
   * Reads the applications a Shortcuts portlet window lists. With application
   * ids, in the order of its selection. With category ids, the applications
   * linked to each category or to one of its sub-categories, grouped per
   * category in the order of the selection, sorted by title within a group,
   * each application once, where it first appears, and at most
   * {@link #MAX_LISTED_APPLICATIONS}. Each application is read from the
   * app-center.application cache, kept when it is active and the user may
   * access it, and decorated on a copy with its labels and badge name only.
   * Unknown and duplicate ids are skipped.
   *
   * @param filter the window's setting name and selected ids or category ids
   * @param locale the language of the labels and of the title order, none
   *          when null
   * @param username the viewer
   * @return the applications to list, never null
   * @throws ObjectNotFoundException when the window's setting does not exist
   * @throws IllegalArgumentException when both ids and category ids are
   *           given, when more than {@link #MAX_LISTED_APPLICATIONS} ids or
   *           more than {@link MyApplicationsSettingsService#MAX_LISTED_CATEGORIES}
   *           category ids are selected
   */
  public List<Application> getListedApplications(ApplicationListFilter filter,
                                                 Locale locale,
                                                 String username) throws ObjectNotFoundException {
    checkSettingExists(filter == null ? null : filter.getSettingName());
    return listApplications(filter, locale, username, false);
  }

  /**
   * Clears the applications listed per category, after a change of a category
   * or of a category link.
   */
  public void clearCategoryApplications() {
    applicationCategoryStorage.clearCache();
  }

  /**
   * @param editor true when the user may edit the window's settings, who then
   *          also sees the applications the window created outside their
   *          audience
   */
  private List<Application> listApplications(ApplicationListFilter filter, Locale locale, String username, boolean editor) {
    if (CollectionUtils.isEmpty(filter.getCategoryIds())) {
      return listApplications(filter.getSettingName(), filter.getIds(), locale, username, editor);
    } else if (CollectionUtils.isNotEmpty(filter.getIds())) {
      throw new IllegalArgumentException("Either application ids or category ids can be listed");
    } else if (filter.getCategoryIds().size() > MyApplicationsSettingsService.MAX_LISTED_CATEGORIES) {
      throw new IllegalArgumentException(String.format("At most %s categories can be listed",
                                                       MyApplicationsSettingsService.MAX_LISTED_CATEGORIES));
    }
    Comparator<Application> byTitle = Comparator.comparing(Application::getTitle,
                                                           Comparator.nullsLast(Collator.getInstance(locale == null ? Locale.ENGLISH
                                                                                                                    : locale)));
    Set<Long> listedIds = new HashSet<>();
    List<Application> applications = new ArrayList<>();
    for (Long categoryId : filter.getCategoryIds().stream().filter(Objects::nonNull).distinct().toList()) {
      // A group is read whole before it is sorted, then truncated
      List<Application> group = new ArrayList<>(applicationCategoryStorage.getApplicationIds(categoryId)
                                                                          .stream()
                                                                          .filter(id -> !listedIds.contains(id))
                                                                          .map(appCenterStorage::getApplication)
                                                                          .filter(Objects::nonNull)
                                                                          .filter(application -> isListedBy(application,
                                                                                                            filter.getSettingName()))
                                                                          .filter(Application::isActive)
                                                                          .filter(application -> isShown(application,
                                                                                                         filter.getSettingName(),
                                                                                                         username,
                                                                                                         editor))
                                                                          .map(this::toTile)
                                                                          .toList());
      setApplicationLabels(group, locale);
      setSystemApplicationTitles(group, locale);
      group.sort(byTitle);
      group.stream()
           .limit((long) MAX_LISTED_APPLICATIONS - applications.size())
           .forEach(application -> {
             listedIds.add(application.getId());
             applications.add(application);
           });
      if (applications.size() >= MAX_LISTED_APPLICATIONS) {
        break;
      }
    }
    setApplicationBadges(applications);
    return applications;
  }

  /**
   * Reads, in the order of the ids, the active applications the user may
   * access, and for an editor the ones the window created, decorated on
   * copies, once the caller checked the window. An application created from a
   * Shortcuts portlet is read for that window only.
   */
  private List<Application> listApplications(String settingName,
                                             List<Long> ids,
                                             Locale locale,
                                             String username,
                                             boolean editor) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    } else if (ids.size() > MAX_LISTED_APPLICATIONS) {
      throw new IllegalArgumentException(String.format("At most %s applications can be listed", MAX_LISTED_APPLICATIONS));
    }
    List<Application> applications = ids.stream()
                                        .filter(Objects::nonNull)
                                        .distinct()
                                        .map(appCenterStorage::getApplication)
                                        .filter(Objects::nonNull)
                                        .filter(application -> isListedBy(application, settingName))
                                        .filter(Application::isActive)
                                        .filter(application -> isShown(application, settingName, username, editor))
                                        .map(this::toTile)
                                        .toList();
    setApplicationLabels(applications, locale);
    setApplicationBadges(applications);
    return applications;
  }

  /**
   * Reads the applications a Shortcuts portlet window lists, for a user
   * editing its settings: as {@link #getListedApplications}, plus the
   * applications the window created whose audience excludes the user, unless
   * an administrator emptied that audience. The applications the window
   * created carry their category ids, which its form edits: an empty list
   * when they have none, null when they could not be read.
   *
   * @param filter the window's setting name and selected ids
   * @param locale the language of the labels, none when null
   * @param username the user editing the window's settings
   * @return the applications to list, never null
   * @throws ObjectNotFoundException when the window's setting does not exist
   * @throws IllegalAccessException when the user may not edit the window's
   *           settings
   * @throws IllegalArgumentException when more than
   *           {@link #MAX_LISTED_APPLICATIONS} ids are selected
   */
  public List<Application> getContextApplications(ApplicationListFilter filter,
                                                  Locale locale,
                                                  String username) throws ObjectNotFoundException, IllegalAccessException {
    String settingName = filter == null ? null : filter.getSettingName();
    checkSettingExists(settingName);
    if (!myApplicationsSettingsService.canEditSettings(settingName, username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to edit settings %s", username, settingName));
    }
    List<Application> applications = listApplications(filter, locale, username, true);
    setContextApplicationCategories(applications);
    return applications;
  }

  /**
   * Suggests the applications a Shortcuts portlet window may list: active,
   * non-personal applications the user may access, matching the keyword, read
   * from the app-center.application cache and decorated on a copy. An
   * application created from a Shortcuts portlet is suggested to its own
   * window only, whatever its audience unless an administrator emptied it, so
   * that one left out of the list, by a failed save or a closed drawer, can be
   * selected again; it carries its category ids as on
   * {@link #getContextApplications}.
   *
   * @param settingName the window's CMS setting name
   * @param keyword matched in title and url, all applications when blank
   * @param excludedIds the applications already selected, never suggested, at
   *          most {@link #MAX_LISTED_APPLICATIONS}
   * @param offset the number of matching applications to skip
   * @param limit the maximum number of suggestions, at most
   *          {@link #MAX_SUGGESTIONS}
   * @param locale the language of the labels, none when null
   * @param username the user editing the window's settings
   * @return the suggested applications, never null
   * @throws ObjectNotFoundException when the window's setting does not exist
   * @throws IllegalAccessException when the user may not edit the window's
   *           settings
   * @throws IllegalArgumentException when more than
   *           {@link #MAX_LISTED_APPLICATIONS} ids are excluded
   */
  public List<Application> getContextSuggestions(String settingName,
                                                 String keyword,
                                                 List<Long> excludedIds,
                                                 int offset,
                                                 int limit,
                                                 Locale locale,
                                                 String username) throws ObjectNotFoundException, IllegalAccessException {
    checkSettingExists(settingName);
    if (!myApplicationsSettingsService.canEditSettings(settingName, username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to edit settings %s", username, settingName));
    }
    if (excludedIds != null && excludedIds.size() > MAX_LISTED_APPLICATIONS) {
      throw new IllegalArgumentException(String.format("At most %s applications can be excluded", MAX_LISTED_APPLICATIONS));
    }
    int suggestionsLimit = limit <= 0 ? MAX_SUGGESTIONS : Math.min(limit, MAX_SUGGESTIONS);
    List<Long> excluded = excludedIds == null ? Collections.emptyList() : excludedIds;
    List<Application> applications = appCenterStorage.getApplicationIds(keyword)
                                                     .stream()
                                                     .filter(id -> !excluded.contains(id))
                                                     .map(appCenterStorage::getApplication)
                                                     .filter(Objects::nonNull)
                                                     .filter(Application::isActive)
                                                     .filter(application -> !application.isPersonal())
                                                     .filter(application -> isListedBy(application, settingName))
                                                     .filter(application -> canAccess(application, username)
                                                                            || isManagedBy(application, settingName))
                                                     .skip(Math.max(offset, 0))
                                                     .limit(suggestionsLimit)
                                                     .map(this::toTile)
                                                     .toList();
    setApplicationLabels(applications, locale);
    setApplicationBadges(applications);
    // A suggestion of its own is edited from the window's form as a listed one
    setContextApplicationCategories(applications);
    return applications;
  }

  /**
   * Creates an application from the settings of a Shortcuts portlet window,
   * owned by that window: listed there only, never in the users' Application
   * Center. Its audience derives from the context the window's setting
   * recorded server-side, the members of its space, or every platform user,
   * internal and external alike, never from the request. Every field the form
   * does not offer is forced, every one it offers is validated, and its
   * illustration is read from an upload only.
   *
   * @param application the form's fields
   * @param settingName the window's CMS setting name
   * @param username the user creating the application
   * @return the created application, its badge resolved
   * @throws ObjectNotFoundException when the window's setting, or the space of
   *           its context, does not exist
   * @throws IllegalAccessException when the user may not manage the window's
   *           applications
   * @throws IllegalArgumentException when a field or the illustration is
   *           invalid, the message naming it
   */
  public Application createContextApplication(ApplicationForm application,
                                              String settingName,
                                              String username) throws ObjectNotFoundException, IllegalAccessException {
    CMSSetting setting = getContextSetting(settingName);
    checkCanManageContextApplications(settingName, username);
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    // The space owning the recorded page's site, which a window rendered
    // before that rule may not have recorded. A window whose space is gone is
    // refused, checked once resolved so that a deletion in between is seen
    long spaceId = myApplicationsSettingsService.getContextSpaceId(settingName, setting.getPageReference());
    if (spaceId <= 0 && myApplicationsSettingsService.isContextDeleted(settingName, setting.getPageReference())) {
      throw new ObjectNotFoundException(String.format("The context of setting %s doesn't exist", settingName));
    }
    Application contextApplication = toContextApplication(application, getContextAudience(spaceId), null, username, false);
    String uploadId = application.getImageUploadId();
    byte[] illustration = null;
    String illustrationMimeType = null;
    if (StringUtils.isNotBlank(uploadId)) {
      illustration = appCenterStorage.readUpload(uploadId, MAX_ILLUSTRATION_SIZE);
      illustrationMimeType = getIllustrationMimeType(illustration);
    }
    if (illustration != null) {
      contextApplication.setImageFileId(appCenterStorage.saveIllustration(illustration, illustrationMimeType));
    }
    Application createdApplication = new Application(appCenterStorage.createContextApplication(contextApplication,
                                                                                                 settingName,
                                                                                                 spaceId));
    if (illustration != null) {
      appCenterStorage.removeUpload(uploadId);
    }
    createdApplication.setCategoryIds(linkCategories(createdApplication.getId(), application.getCategoryIds(), username));
    // Resolved, as the portlet's listing gives it, for the form that edits it next
    createdApplication.setBadgeName(badgePluginRegistry.resolveBadgeName(createdApplication));
    return createdApplication;
  }

  /**
   * Creates, in the context of a copy of a Shortcuts portlet window, the copy
   * of an application the source window created, when the copy is first
   * rendered: owned by the copy, with the audience of the context the copy's
   * setting recorded. No right applies, as the first viewer of the copy can be
   * anyone: the payload was written by the copy step or by an editor of the
   * page, and the application is created in the copy's own context only.
   * Every field is validated as on {@link #createContextApplication}, the
   * one exception being a portlet instance, which needs only to exist. The
   * illustration's type is decided from its content; an invalid or too large
   * one is dropped, the application being created without it. Its categories
   * are linked with no right checked, as the copy's first viewer can be
   * anyone, a category that does not exist, or that fails to link, being
   * skipped.
   *
   * @param application the copied application's fields and categories
   * @param illustration the copied illustration's content, null when none
   * @param settingName the copy's CMS setting name
   * @return the created application, with the categories it got
   * @throws ObjectNotFoundException when the copy's setting, or the space of
   *           its context, does not exist
   * @throws IllegalArgumentException when a field is invalid, the message
   *           naming it
   */
  public Application createCopiedContextApplication(ApplicationForm application,
                                                    byte[] illustration,
                                                    String settingName) throws ObjectNotFoundException {
    CMSSetting setting = getContextSetting(settingName);
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    long spaceId = myApplicationsSettingsService.getContextSpaceId(settingName, setting.getPageReference());
    if (spaceId <= 0 && myApplicationsSettingsService.isContextDeleted(settingName, setting.getPageReference())) {
      throw new ObjectNotFoundException(String.format("The context of setting %s doesn't exist", settingName));
    }
    List<String> audience = getContextAudience(spaceId);
    Application contextApplication = toContextApplication(application, audience, null, null, true);
    String illustrationMimeType = getCopiedIllustrationMimeType(illustration, settingName);
    if (illustrationMimeType != null) {
      contextApplication.setImageFileId(appCenterStorage.saveIllustration(illustration, illustrationMimeType));
    }
    Application createdApplication;
    try {
      createdApplication = new Application(appCenterStorage.createContextApplication(contextApplication, settingName, spaceId));
    } catch (RuntimeException e) {
      deleteNewIllustration(contextApplication.getImageFileId());
      throw e;
    }
    createdApplication.setCategoryIds(linkCopiedCategories(createdApplication.getId(), application.getCategoryIds()));
    return createdApplication;
  }

  /**
   * @param applicationIds application ids
   * @return the ids of each application's categories, read in one query, by
   *         application id, none for an application without category
   */
  public Map<Long, List<Long>> getApplicationCategoryIds(List<Long> applicationIds) {
    Map<String, List<Long>> linkedIds = getCategoryLinkService().getLinkedIds(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                                              applicationIds.stream()
                                                                                            .map(String::valueOf)
                                                                                            .toList());
    Map<Long, List<Long>> categoryIds = new HashMap<>();
    if (linkedIds != null) {
      linkedIds.forEach((applicationId, ids) -> categoryIds.put(Long.valueOf(applicationId), ids));
    }
    return categoryIds;
  }

  /**
   * Deletes an application created from the settings of a Shortcuts portlet
   * window, from that window's settings.
   *
   * @param applicationId the application id
   * @param settingName the window's CMS setting name
   * @param username the user deleting the application
   * @throws ObjectNotFoundException when the window's setting does not exist,
   *           or when the application does not exist, or the window does
   *           not manage it and the user may not access it
   * @throws IllegalAccessException when the user may not manage the window's
   *           applications
   * @throws IllegalArgumentException with the message
   *           {@value #NOT_CONTEXTUAL_MESSAGE} for an application of the
   *           Application Center, {@value #NOT_OWNED_MESSAGE} for one another
   *           window created
   */
  public void deleteContextApplication(long applicationId,
                                       String settingName,
                                       String username) throws ObjectNotFoundException, IllegalAccessException {
    getOwnedContextApplication(applicationId, settingName, username);
    try {
      deleteApplication(applicationId);
    } catch (ApplicationNotFoundException e) {
      throw new ObjectNotFoundException(e.getMessage());
    }
  }

  /**
   * Updates an application created from the settings of a Shortcuts portlet
   * window, from that window's settings, with the fields its form offers, each
   * one validated as on creation, but a portlet instance it keeps, which it
   * does not check again. Its owner, audience, order and every field
   * the form does not offer keep their stored or forced value. Its badge
   * binding is the administrators' to change: for another editor, a new type
   * drops it, and so does, unless the badge is turned off, a new url a badge
   * provider declares. Its
   * illustration is replaced from an upload only, removed only on the form's
   * explicit request, and kept otherwise. Its categories become the form's
   * ones as far as they can: a category the user may not link or unlink, or
   * that fails to, keeps its state, and none changes when the form sends no
   * list of them or when the stored ones cannot be read.
   *
   * @param applicationId the application id
   * @param application the form's fields
   * @param settingName the window's CMS setting name
   * @param username the user updating the application
   * @return the updated application, as a listing tile without its
   *         permissions, its badge resolved, with its categories, null when
   *         they could not be read
   * @throws ObjectNotFoundException when the window's setting does not exist,
   *           when the application does not exist, or the window does not
   *           manage it and the user may not access it, or when the update's
   *           own read of the application, or its read back once saved, finds
   *           none
   * @throws IllegalAccessException when the user may not manage the window's
   *           applications
   * @throws IllegalArgumentException with the message
   *           {@value #NOT_CONTEXTUAL_MESSAGE} for an application of the
   *           Application Center, {@value #NOT_OWNED_MESSAGE} for one another
   *           window created, or a message naming the invalid field or
   *           illustration
   */
  public Application updateContextApplication(long applicationId,
                                              ApplicationForm application,
                                              String settingName,
                                              String username) throws ObjectNotFoundException, IllegalAccessException {
    Application storedApplication = getOwnedContextApplication(applicationId, settingName, username);
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    Application contextApplication = toContextApplication(application, List.of(), storedApplication, username, false);
    contextApplication.setId(applicationId);
    contextApplication.setPermissions(storedApplication.getPermissions() == null ? null
                                                                                 : new ArrayList<>(storedApplication.getPermissions()));
    contextApplication.setOrder(storedApplication.getOrder());
    contextApplication.setChangedManually(storedApplication.isChangedManually());
    contextApplication.setContextSettingName(storedApplication.getContextSettingName());
    contextApplication.setContextSpaceId(storedApplication.getContextSpaceId());
    if (!isAdministrator(username)) {
      // The badge binding is offered to administrators only, who read the
      // badge providers: any other editor's form sends back the resolved
      // badge, which would replace a stored 'none' or an url-derived binding
      contextApplication.setBadgeName(getKeptBadgeName(storedApplication, contextApplication));
    }
    String uploadId = application.getImageUploadId();
    Long newImageFileId = null;
    if (StringUtils.isNotBlank(uploadId)) {
      byte[] illustration = appCenterStorage.readUpload(uploadId, MAX_ILLUSTRATION_SIZE);
      String illustrationMimeType = getIllustrationMimeType(illustration);
      newImageFileId = appCenterStorage.saveIllustration(illustration, illustrationMimeType);
      contextApplication.setImageFileId(newImageFileId);
    } else if (!application.isIllustrationRemoved()) {
      contextApplication.setImageFileId(storedApplication.getImageFileId());
    }
    // The new illustration only, never the kept one, is deleted when the update fails
    try {
      appCenterStorage.updateContextApplication(contextApplication);
    } catch (ApplicationNotFoundException e) {
      // Deleted since it was checked
      deleteNewIllustration(newImageFileId);
      throw new ObjectNotFoundException(e.getMessage());
    } catch (RuntimeException e) {
      deleteNewIllustration(newImageFileId);
      throw e;
    }
    if (StringUtils.isNotBlank(uploadId)) {
      appCenterStorage.removeUpload(uploadId);
    }
    Application storedUpdatedApplication = appCenterStorage.getApplication(applicationId);
    if (storedUpdatedApplication == null) {
      throw new ObjectNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    Application updatedApplication = toTile(storedUpdatedApplication);
    updatedApplication.setCategoryIds(updateCategories(applicationId, application.getCategoryIds(), username));
    // Resolved, as the portlet's listing gives it, for the form that edits it next
    updatedApplication.setBadgeName(badgePluginRegistry.resolveBadgeName(updatedApplication));
    return updatedApplication;
  }

  /**
   * Deletes the applications Shortcuts portlet windows created in the context
   * of a deleted space: a space created later with the same name gets the
   * same group, so the same audience. One application failing to delete does
   * not keep the others.
   *
   * @param spaceId the deleted space id
   */
  public void deleteSpaceApplications(long spaceId) {
    if (spaceId <= 0) {
      return;
    }
    for (Long applicationId : appCenterStorage.getContextApplicationIds(spaceId)) {
      try {
        deleteApplication(applicationId);
      } catch (ApplicationNotFoundException e) {
        LOG.debug("Application {} of space {} is already deleted", applicationId, spaceId, e);
      } catch (RuntimeException e) {
        LOG.warn("Unable to delete application {} of deleted space {}", applicationId, spaceId, e);
      }
    }
  }

  /**
   * Deletes the applications the windows of a section template's drafts
   * created, every administrator's, once the template is deleted.
   *
   * @param templateId the deleted section template id
   */
  public void deleteSectionTemplateApplications(long templateId) {
    deleteSectionTemplateDraftApplications(templateId, application -> true);
  }

  /**
   * Deletes the applications an earlier editing session of a section template
   * left behind on a user's draft page: the section-template editor saves the
   * draft page again, with new windows, each time it opens the template, so
   * that the applications of the windows it replaced are listed nowhere. Kept:
   * those of a window of the current draft page, among which the window being
   * initialized, whatever their order of rendering, those of a window
   * recorded on another draft page, another administrator's, and those whose
   * window's setting cannot be read. Each application is checked when its
   * turn comes, after all were selected: one created meanwhile waits for the
   * next session, one deleted meanwhile is skipped. Nothing is deleted once
   * the window being initialized no longer exists as committed: its render
   * loaded the draft page before a later session saved it again, so that the
   * given windows are not those of the current draft page. That window is
   * read once, when the first application to delete is reached, after the
   * selection, which lists the applications of such a later session's
   * windows only once the draft page was saved again; a read that fails
   * stops the deletion, its exception thrown.
   *
   * @param settingName the CMS setting name of the window being initialized,
   *          which names that window: when it does not, nothing is deleted
   * @param draftPageReference the draft page the window is rendered on
   * @param windowStorageIds the storage ids of the windows of that page, as
   *          the render loaded it
   */
  public void deleteSectionTemplateDraftLeftovers(String settingName,
                                                  String draftPageReference,
                                                  Collection<String> windowStorageIds) {
    long templateId = myApplicationsSettingsService.getSectionTemplateId(draftPageReference);
    if (templateId <= 0) {
      return;
    }
    String windowStorageId = myApplicationsSettingsService.getSectionTemplateDraftStorageId(settingName, templateId);
    AtomicReference<Boolean> windowCommitted = new AtomicReference<>();
    deleteSectionTemplateDraftApplications(templateId, application -> {
      String owner = application.getContextSettingName();
      if (StringUtils.equals(owner, settingName)
          || windowStorageIds.contains(myApplicationsSettingsService.getSectionTemplateDraftStorageId(owner, templateId))) {
        return false;
      }
      CMSSetting ownerSetting;
      try {
        ownerSetting = myApplicationsSettingsService.getSetting(owner);
      } catch (RuntimeException e) {
        LOG.debug("Application {} kept: the setting of {} cannot be read", application.getId(), owner, e);
        return false;
      }
      if (ownerSetting != null && !StringUtils.equals(ownerSetting.getPageReference(), draftPageReference)) {
        return false;
      } else if (windowCommitted.get() == null) {
        windowCommitted.set(windowStorage.exists(windowStorageId));
      }
      return windowCommitted.get();
    });
  }

  /**
   * Whether a user may edit an application from the settings of a Shortcuts
   * portlet window: an administrator any application, whoever manages the
   * applications of the window that created one that application only. When
   * that window's setting no longer exists, or an administrator emptied the
   * application's audience, administrators only.
   *
   * @param application the application
   * @param username the user, blank for a guest
   * @return true when the user may edit the application
   */
  public boolean canEditContextApplication(Application application, String username) {
    return isAdministrator(username)
           || (application != null
               && isManagedBy(application, application.getContextSettingName())
               && myApplicationsSettingsService.canEditSettings(application.getContextSettingName(), username));
  }

  /**
   * Gives the system applications the title, and the description when they
   * have none, the portlet shows them with, from
   * the {@value #SYSTEM_APPLICATIONS_BUNDLE} bundle, keyed as
   * MyApplicationsApp.vue#i18nSystemApplicationTitle keys it, so that a
   * category is sorted on the titles it displays
   */
  private void setSystemApplicationTitles(List<Application> applications, Locale locale) {
    if (locale == null || applications.stream().noneMatch(Application::isSystem)) {
      return;
    }
    ResourceBundle bundle = resourceBundleService.getResourceBundle(SYSTEM_APPLICATIONS_BUNDLE, locale);
    if (bundle == null) {
      return;
    }
    applications.stream()
                .filter(Application::isSystem)
                .filter(application -> StringUtils.isNotBlank(application.getTitle()))
                .forEach(application -> {
                  String title = application.getTitle();
                  String key = SYSTEM_APPLICATION_KEY_PREFIX
                      + (WHITESPACE.matcher(title).find() ? title.replace(' ', '.') : title).toLowerCase(Locale.ROOT);
                  if (bundle.containsKey(key)) {
                    application.setTitle(bundle.getString(key));
                    if (StringUtils.isBlank(application.getDescription()) && bundle.containsKey(key + ".description")) {
                      application.setDescription(bundle.getString(key + ".description"));
                    }
                  }
                });
  }

  private void checkSettingExists(String settingName) throws ObjectNotFoundException {
    if (!myApplicationsSettingsService.hasSetting(settingName)) {
      throw new ObjectNotFoundException(String.format(SETTING_NOT_FOUND_MESSAGE, settingName));
    }
  }

  private CMSSetting getContextSetting(String settingName) throws ObjectNotFoundException {
    CMSSetting setting = myApplicationsSettingsService.getSetting(settingName);
    if (setting == null) {
      throw new ObjectNotFoundException(String.format(SETTING_NOT_FOUND_MESSAGE, settingName));
    }
    return setting;
  }

  /**
   * Deletes the applications owned by the windows of a section template's
   * drafts that the predicate selects. One application failing to delete does
   * not keep the others.
   */
  private void deleteSectionTemplateDraftApplications(long templateId, Predicate<Application> selected) {
    String ownerPattern = myApplicationsSettingsService.getSectionTemplateDraftNamePattern(templateId);
    for (Long applicationId : appCenterStorage.getContextApplicationIdsByOwnerPattern(ownerPattern)) {
      Application application = appCenterStorage.getApplication(applicationId);
      // The pattern's wildcards match more than the names generated for the
      // template's draft windows: each owner is checked on its own
      if (application == null
          || myApplicationsSettingsService.getSectionTemplateDraftStorageId(application.getContextSettingName(),
                                                                           templateId) == null
          || !selected.test(application)) {
        continue;
      }
      try {
        deleteApplication(applicationId);
      } catch (ApplicationNotFoundException e) {
        LOG.debug("Application {} of section template {} is already deleted", applicationId, templateId, e);
      } catch (RuntimeException e) {
        LOG.warn("Unable to delete application {} of section template {}", applicationId, templateId, e);
      }
    }
  }

  private void deleteNewIllustration(Long imageFileId) {
    if (imageFileId != null) {
      appCenterStorage.deleteIllustration(imageFileId);
    }
  }

  /**
   * The badge binding an update keeps for an editor who may not choose it,
   * whatever the form sends, as the binding follows the url: a new type drops
   * it, a turned off badge included; on the same type, a new url a badge
   * provider declares drops it unless the badge is turned off; an unchanged
   * url, or a new one no provider declares, keeps it.
   *
   * @return the stored binding, or null to let the new url resolve it
   */
  private String getKeptBadgeName(Application storedApplication, Application updatedApplication) {
    String badgeName = storedApplication.getBadgeName();
    if (updatedApplication.getType() != storedApplication.getType()) {
      return null;
    } else if (StringUtils.equals(updatedApplication.getUrl(), storedApplication.getUrl())
               || ApplicationBadgePluginRegistry.BADGE_DISABLED.equals(badgeName)) {
      return badgeName;
    }
    Application unbound = new Application(updatedApplication);
    unbound.setBadgeName(null);
    return badgePluginRegistry.resolveBadgeName(unbound) == null ? badgeName : null;
  }

  /**
   * Checks, in this order, the window's setting, the user's right to manage
   * the window's applications, the application, its access and its owner.
   *
   * @return the stored application, the cache's shared instance
   */
  private Application getOwnedContextApplication(long applicationId,
                                                 String settingName,
                                                 String username) throws ObjectNotFoundException, IllegalAccessException {
    getContextSetting(settingName);
    checkCanManageContextApplications(settingName, username);
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null || !isManagedBy(application, settingName) && !canAccess(application, username)) {
      // The window's right covers the applications it created, whatever
      // their audience
      throw new ObjectNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (!application.isContextual()) {
      throw new IllegalArgumentException(NOT_CONTEXTUAL_MESSAGE);
    } else if (!StringUtils.equals(application.getContextSettingName(), settingName)) {
      throw new IllegalArgumentException(NOT_OWNED_MESSAGE);
    }
    return application;
  }

  private void checkCanManageContextApplications(String settingName, String username) throws IllegalAccessException {
    if (!myApplicationsSettingsService.canEditSettings(settingName, username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to manage the applications of %s",
                                                     username,
                                                     settingName));
    }
  }

  /**
   * @return the audience of an application created in a context: the members
   *         of its space, external ones included, or every platform user,
   *         internal and external alike
   */
  private List<String> getContextAudience(long spaceId) throws ObjectNotFoundException {
    if (spaceId <= 0) {
      return List.of(DEFAULT_USERS_PERMISSION, EXTERNAL_USERS_PERMISSION);
    }
    Space space = spaceService.getSpaceById(spaceId);
    if (space == null) {
      throw new ObjectNotFoundException(String.format("Space %s doesn't exist", spaceId));
    }
    return List.of(ANY_MEMBERSHIP + space.getGroupId());
  }

  /**
   * Builds an application created from a Shortcuts portlet from the fields
   * its form offers, each one validated, every other one forced.
   *
   * @param storedApplication the application an update changes, null on
   *          creation
   * @param copied whether the application is the copy of another, whose
   *          portlet instance needs only to exist, no user binding it
   */
  private Application toContextApplication(ApplicationForm form,
                                           List<String> audience,
                                           Application storedApplication,
                                           String username,
                                           boolean copied) {
    if (StringUtils.isBlank(form.getTitle())) {
      throw new IllegalArgumentException(TITLE_MANDATORY_MESSAGE);
    } else if (form.getCategoryIds() != null && form.getCategoryIds().size() > MyApplicationsSettingsService.MAX_LISTED_CATEGORIES) {
      throw new IllegalArgumentException(TOO_MANY_CATEGORIES_MESSAGE);
    }
    ApplicationType type = form.getType() == null ? ApplicationType.LINK : form.getType();
    Application application = new Application();
    application.setTitle(checkLength("title", StringUtils.trim(form.getTitle()), MAX_TITLE_LENGTH));
    application.setDescription(checkLength("description", form.getDescription(), MAX_DESCRIPTION_LENGTH));
    application.setType(type);
    application.setUrl(checkUrl("url", getContextApplicationUrl(type, form.getUrl(), storedApplication, username, copied)));
    application.setHelpPageURL(StringUtils.isBlank(form.getHelpPageURL()) ? null
                                                                          : checkUrl("helpPageURL",
                                                                                     getContextLink(form.getHelpPageURL(),
                                                                                                    INVALID_HELP_PAGE_URL_MESSAGE)));
    application.setIcon(getContextApplicationIcon(form.getIcon()));
    application.setBadgeName(getContextApplicationBadgeName(form.getBadgeName()));
    application.setSameTab(form.isSameTab());
    application.setMobile(form.isMobile());
    application.setAllowDetach(form.isAllowDetach());
    application.setActive(true);
    application.setPermissions(new ArrayList<>(audience));
    setContextualForcedFields(application);
    return application;
  }

  /**
   * @return the url of an application created from a Shortcuts portlet: an
   *         http(s) or portal-relative link, the name of a drawer, which the
   *         server cannot check is registered, or a portlet instance the user
   *         binding it may access
   */
  private String getContextApplicationUrl(ApplicationType type,
                                          String url,
                                          Application storedApplication,
                                          String username,
                                          boolean copied) {
    return switch (type) {
    case DRAWER -> getDrawerName(url);
    case PORTLET -> getPortletInstanceId(url, storedApplication, username, copied);
    default -> getContextLink(url, INVALID_URL_MESSAGE);
    };
  }

  private String getContextLink(String url, String invalidMessage) {
    try {
      return normalizePersonalUrl(url);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(invalidMessage, e);
    }
  }

  /**
   * @return the url of a field, refused when it does not fit its column or
   *         holds a character outside the Basic Multilingual Plane, which the
   *         url columns of a MySQL database, utf8mb3, cannot store
   */
  private String checkUrl(String fieldName, String url) {
    if (url != null && url.codePoints().anyMatch(Character::isSupplementaryCodePoint)) {
      throw new IllegalArgumentException(CONTEXTUAL_MESSAGE_PREFIX + fieldName + "UnsupportedCharacter");
    }
    return checkLength(fieldName, url, MAX_URL_LENGTH);
  }

  private String getDrawerName(String url) {
    if (url == null || !DRAWER_NAME_PATTERN.matcher(url).matches()) {
      throw new IllegalArgumentException(INVALID_URL_MESSAGE);
    }
    return url;
  }

  private String getPortletInstanceId(String url, Application storedApplication, String username, boolean copied) {
    long portletInstanceId = NumberUtils.toLong(url, 0);
    if (portletInstanceId <= 0) {
      throw new IllegalArgumentException(INVALID_URL_MESSAGE);
    } else if (copied) {
      // The copy's first viewer can be anyone: the instance has only to exist
      if (portletInstanceService.getPortletInstance(portletInstanceId) == null) {
        throw new IllegalArgumentException(INVALID_URL_MESSAGE);
      }
      return String.valueOf(portletInstanceId);
    } else if (storedApplication != null
               && storedApplication.getType() == ApplicationType.PORTLET
               && StringUtils.equals(storedApplication.getUrl(), String.valueOf(portletInstanceId))) {
      // A kept instance is not checked again: an editor who keeps it needs no
      // access to it
      return storedApplication.getUrl();
    }
    try {
      portletInstanceService.getPortletInstance(portletInstanceId, username, null, false);
    } catch (ObjectNotFoundException | IllegalAccessException e) {
      throw new IllegalArgumentException(INVALID_URL_MESSAGE, e);
    }
    return String.valueOf(portletInstanceId);
  }

  /**
   * @return the value, refused with a message naming the field when it does
   *         not fit its column
   */
  private String checkLength(String fieldName, String value, int maxLength) {
    if (value != null && value.length() > maxLength) {
      throw new IllegalArgumentException(CONTEXTUAL_MESSAGE_PREFIX + fieldName + "TooLong");
    }
    return value;
  }

  private String getContextApplicationIcon(String icon) {
    if (StringUtils.isBlank(icon)) {
      return null;
    } else if (!ICON_PATTERN.matcher(icon).matches()) {
      throw new IllegalArgumentException(INVALID_ICON_MESSAGE);
    }
    return icon;
  }

  /**
   * @return the badge name when a badge plugin declares it, or when it turns
   *         the badge off, null otherwise
   */
  private String getContextApplicationBadgeName(String badgeName) {
    return StringUtils.equals(badgeName, ApplicationBadgePluginRegistry.BADGE_DISABLED)
           || (StringUtils.isNotBlank(badgeName) && badgePluginRegistry.getPlugin(badgeName) != null) ? badgeName : null;
  }

  /**
   * @return the type of an illustration decided from its content, once its
   *         upload is known and small enough
   */
  private String getIllustrationMimeType(byte[] illustration) {
    if (illustration == null) {
      throw new IllegalArgumentException(INVALID_UPLOAD_MESSAGE);
    } else if (illustration.length > MAX_ILLUSTRATION_SIZE) {
      throw new IllegalArgumentException(TOO_LARGE_MESSAGE);
    }
    String mimeType = ImageTypeUtils.detectImageMimeType(illustration);
    if (mimeType == null) {
      throw new IllegalArgumentException(INVALID_TYPE_MESSAGE);
    }
    return mimeType;
  }

  /**
   * @return the type of a copied illustration decided from its content, null
   *         when there is none, or when it is too large or not an image of an
   *         accepted type, the copy being created without it
   */
  private String getCopiedIllustrationMimeType(byte[] illustration, String settingName) {
    if (illustration == null) {
      return null;
    }
    try {
      return getIllustrationMimeType(illustration);
    } catch (IllegalArgumentException e) {
      LOG.debug("Illustration of an application copied for {} dropped", settingName, e);
      return null;
    }
  }

  /**
   * Links a copied application to its source's categories with no right
   * checked: a category that does not exist on this deployment, or that fails
   * to link, is skipped, never created. The link takes any metadata id, so an
   * id is linked only once read as a category: one that names other
   * metadata, a tag or a favorite for instance, is skipped, nothing being
   * written for it.
   *
   * @return the ids of the linked categories
   */
  private List<Long> linkCopiedCategories(long applicationId, List<Long> categoryIds) {
    List<Long> linkedCategoryIds = new ArrayList<>();
    if (CollectionUtils.isEmpty(categoryIds)) {
      return linkedCategoryIds;
    }
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, String.valueOf(applicationId), 0L);
    for (Long categoryId : categoryIds.stream().filter(Objects::nonNull).distinct().toList()) {
      try {
        if (isCategory(categoryId)) {
          getCategoryLinkService().link(categoryId, categoryObject);
          linkedCategoryIds.add(categoryId);
        }
      } catch (RuntimeException e) {
        LOG.debug("Copied application {} not linked to category {}", applicationId, categoryId, e);
      }
    }
    return linkedCategoryIds;
  }

  /**
   * @return true when the metadata exists and is a category
   */
  private boolean isCategory(long metadataId) {
    Metadata metadata = metadataService.getMetadataById(metadataId);
    return metadata != null && StringUtils.equals(metadata.getTypeName(), CATEGORY_METADATA_TYPE);
  }

  /**
   * Links an application created from a Shortcuts portlet to the categories
   * of its form. A category the user may not link to, that no longer exists or
   * that fails to link is skipped: the application is created already, and is
   * returned with the categories it got.
   *
   * @return the ids of the linked categories
   */
  private List<Long> linkCategories(long applicationId, List<Long> categoryIds, String username) {
    if (CollectionUtils.isEmpty(categoryIds)) {
      return new ArrayList<>();
    }
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                       String.valueOf(applicationId),
                                                       0L);
    List<Long> linkedCategoryIds = new ArrayList<>();
    for (Long categoryId : categoryIds.stream().filter(Objects::nonNull).distinct().toList()) {
      try {
        getCategoryLinkService().link(categoryId, categoryObject, username);
        linkedCategoryIds.add(categoryId);
      } catch (ObjectNotFoundException | ObjectAlreadyExistsException | IllegalAccessException e) {
        LOG.debug("Application {} not linked to category {} by {}", applicationId, categoryId, username, e);
      } catch (RuntimeException e) {
        LOG.warn("Unable to link application {} to category {}", applicationId, categoryId, e);
      }
    }
    return linkedCategoryIds;
  }

  /**
   * Gives the applications a window created, among the listed copies, their
   * categories, which the window's form edits, in one read. When the read
   * fails, they stay unknown (null).
   */
  private void setContextApplicationCategories(List<Application> applications) {
    List<String> contextApplicationIds = applications.stream()
                                                     .filter(Application::isContextual)
                                                     .map(application -> String.valueOf(application.getId()))
                                                     .toList();
    if (contextApplicationIds.isEmpty()) {
      return;
    }
    Map<String, List<Long>> linkedIds;
    try {
      linkedIds = getCategoryLinkService().getLinkedIds(ApplicationCategoryPlugin.OBJECT_TYPE, contextApplicationIds);
    } catch (RuntimeException e) {
      LOG.warn("Unable to read the categories of the applications {}", contextApplicationIds, e);
      return;
    }
    // An application with no category is absent from the map
    Map<String, List<Long>> categoryIds = linkedIds == null ? Map.of() : linkedIds;
    applications.stream()
                .filter(Application::isContextual)
                .forEach(application -> application.setCategoryIds(new ArrayList<>(CollectionUtils.emptyIfNull(categoryIds.get(String.valueOf(application.getId()))))));
  }

  /**
   * Makes the categories of an updated application created from a Shortcuts
   * portlet the ones of its form: links the new ones, unlinks the others, as
   * the user may. A category that fails to link or unlink keeps its state: the
   * application is updated already, and is returned with the categories it
   * has.
   *
   * @return the ids of the linked categories
   */
  private List<Long> updateCategories(long applicationId, List<Long> categoryIds, String username) {
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                       String.valueOf(applicationId),
                                                       0L);
    List<Long> storedCategoryIds;
    try {
      storedCategoryIds = getCategoryLinkService().getLinkedIds(categoryObject);
    } catch (RuntimeException e) {
      // Unknown, so left as they are and returned as unknown
      LOG.warn("Unable to read the categories of updated application {}", applicationId, e);
      return null; // NOSONAR
    }
    List<Long> linkedCategoryIds = new ArrayList<>(CollectionUtils.emptyIfNull(storedCategoryIds));
    if (categoryIds == null) {
      return linkedCategoryIds;
    }
    List<Long> newCategoryIds = categoryIds.stream().filter(Objects::nonNull).distinct().toList();
    for (Long categoryId : linkedCategoryIds.stream().filter(id -> !newCategoryIds.contains(id)).toList()) {
      try {
        getCategoryLinkService().unlink(categoryId, categoryObject, username);
        linkedCategoryIds.remove(categoryId);
      } catch (ObjectNotFoundException | IllegalAccessException e) {
        LOG.debug("Application {} not unlinked from category {} by {}", applicationId, categoryId, username, e);
      } catch (RuntimeException e) {
        LOG.warn("Unable to unlink application {} from category {}", applicationId, categoryId, e);
      }
    }
    linkedCategoryIds.addAll(linkCategories(applicationId,
                                            newCategoryIds.stream().filter(id -> !linkedCategoryIds.contains(id)).toList(),
                                            username));
    return linkedCategoryIds;
  }

  /**
   * Removes the category links of a deleted application, read from the
   * category service since the stored application carries no category: a
   * link that fails to go is left, with a warning, so that the delete still
   * removes what the application owns
   */
  private void unlinkCategories(long applicationId) {
    CategoryObject categoryObject = new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                       String.valueOf(applicationId),
                                                       0L);
    List<Long> categoryIds;
    try {
      categoryIds = getCategoryLinkService().getLinkedIds(categoryObject);
    } catch (RuntimeException e) {
      LOG.warn("Unable to read the categories of deleted application {}", applicationId, e);
      return;
    }
    for (Long categoryId : CollectionUtils.emptyIfNull(categoryIds)) {
      try {
        getCategoryLinkService().unlink(categoryId, categoryObject);
      } catch (RuntimeException e) {
        LOG.warn("Unable to unlink deleted application {} from category {}", applicationId, categoryId, e);
      }
    }
  }

  /**
   * Copies a cached application into the tile a listing returns, without its
   * permissions, which only the server checks.
   */
  private Application toTile(Application application) {
    Application tile = new Application(application);
    tile.setPermissions(null);
    return tile;
  }

  public List<String> getApplicationShortcuts(String username) {
    return getActiveApplications(null, username).stream()
                                                .map(Application::getShortcut)
                                                .filter(StringUtils::isNotBlank)
                                                .toList();
  }

  /**
   * Whether a user may see an application. An application created from a
   * Shortcuts portlet whose audience an administrator emptied is shown to
   * administrators only, where any other application without permissions is
   * shown to every platform user.
   *
   * @param application the application
   * @param username the user, blank for a guest
   * @return true when the user may see the application
   */
  public boolean canAccess(Application application, String username) {
    if (application.isPersonal()) {
      return isOwner(application, username);
    } else if (application.isContextual() && CollectionUtils.isEmpty(application.getPermissions())) {
      return isAdministrator(username);
    }
    return canAccess(application.getPermissions(), username);
  }

  public boolean canEdit(String username) {
    return StringUtils.isBlank(username) || userAcl.isAdministrator(getUserIdentity(username));
  }

  private boolean isOwner(Application application, String username) {
    return StringUtils.isNotBlank(username)
           && application.getPermissions() != null
           && application.getPermissions().contains(username);
  }

  /**
   * @return true for an administrator, false for a blank, guest or unknown
   *         user, where {@link #canEdit(String)} grants a blank username
   */
  private boolean isAdministrator(String username) {
    if (StringUtils.isBlank(username) || StringUtils.equals(username, IdentityConstants.ANONIM)) {
      return false;
    }
    Identity identity = userAcl.getUserIdentity(username);
    return identity != null && userAcl.isAdministrator(identity);
  }

  /**
   * @return true when the window may list the application: any application of
   *         the Application Center, one created from a Shortcuts portlet by
   *         that window only
   */
  private boolean isListedBy(Application application, String settingName) {
    return !application.isContextual() || isOwnedBy(application, settingName);
  }

  private boolean isOwnedBy(Application application, String settingName) {
    return application.isContextual() && StringUtils.equals(application.getContextSettingName(), settingName);
  }

  /**
   * @param application the application
   * @param settingName a Shortcuts portlet window's CMS setting name
   * @return true when the window's editors may see and delete the application
   *         outside its audience: one the window created, unless an
   *         administrator emptied its audience, which shows it to
   *         administrators only
   */
  private boolean isManagedBy(Application application, String settingName) {
    return isOwnedBy(application, settingName) && CollectionUtils.isNotEmpty(application.getPermissions());
  }

  /**
   * Whether a Shortcuts portlet window's settings drawer lists an application
   * to a user, the rule a copy of the window applies too.
   *
   * @param application the application
   * @param settingName the window's CMS setting name
   * @param username the user
   * @param editor whether the user may edit the window's settings
   * @return true for an application the user may access, and for an editor of
   *         the window's settings one the window manages
   */
  public boolean isShown(Application application, String settingName, String username, boolean editor) {
    return canAccess(application, username) || editor && isManagedBy(application, settingName);
  }

  private Application getFavoritableApplication(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                    IllegalAccessException {
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (!canAccess(application, username)) {
      throw new IllegalAccessException(String.format(USER_CANNOT_ACCESS_APP_MESSAGE, username, applicationId));
    } else if (application.isContextual()) {
      throw new IllegalArgumentException(NOT_FAVORITABLE_MESSAGE);
    }
    return application;
  }

  /**
   * Sets the fields an application created from a Shortcuts portlet never
   * changes: it is neither mandatory, default, PWA-suggested, system nor
   * personal, it cannot be stuck to a side of the layout and has no keyboard
   * shortcut.
   */
  private void setContextualForcedFields(Application application) {
    application.setMandatory(false);
    application.setDefault(false);
    application.setPwa(false);
    application.setSystem(false);
    application.setPersonal(false);
    application.setAllowStick(false);
    application.setShortcut(null);
  }

  private boolean canAccess(List<String> storedPermissions, String username) {
    Identity identity = getUserIdentity(username);
    if (CollectionUtils.isEmpty(storedPermissions)) {
      return identity != null && userAcl.hasPermission(identity, DEFAULT_USERS_PERMISSION);
    } else {
      return storedPermissions.stream().anyMatch(exp -> userAcl.hasPermission(identity, exp));
    }
  }

  @SneakyThrows
  private Identity getUserIdentity(String username) {
    if (StringUtils.isBlank(username)) {
      return new Identity(IdentityConstants.ANONIM);
    } else {
      return userAcl.getUserIdentity(username);
    }
  }

  /**
   * Reads the applications of the user's Application Center: never one
   * created from a Shortcuts portlet, which only that portlet lists.
   */
  private List<Application> getActiveApplications(String keyword, String username) {
    return appCenterStorage.getApplications(keyword)
                           .stream()
                           .filter(app -> !app.isContextual())
                           .filter(app -> canAccess(app, username))
                           .filter(Application::isActive)
                           .toList();

  }

  private void setApplicationLabels(List<Application> applications, Locale locale) {
    if (locale != null) {
      applications.forEach(application -> setApplicationLabels(application, locale));
    }
  }

  private void setApplicationCategories(List<Application> applications) {
    applications.forEach(this::setApplicationCategories);
  }

  /**
   * Replaces the stored badge name by the resolved one, so that a Drawer or
   * Portlet entry whose provider declares its url exposes its badge without any
   * value ever being persisted. Applied on the copies returned to the caller,
   * never on the cached entities.
   */
  private void setApplicationBadges(List<Application> applications) {
    applications.forEach(application -> application.setBadgeName(badgePluginRegistry.resolveBadgeName(application)));
  }

  private void setApplicationCategories(Application application) {
    List<Long> categoryIds = getCategoryLinkService().getLinkedIds(new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                                                      String.valueOf(application.getId()),
                                                                                      0));
    application.setCategoryIds(categoryIds);
  }

  private void setApplicationLabels(Application application, Locale locale) {
    String title = translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                                   application.getId(),
                                                                   "title",
                                                                   locale);
    if (StringUtils.isNotBlank(title)) {
      application.setTitle(title);
    }
    String description = translationService.getTranslationLabelOrDefault(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
                                                                         application.getId(),
                                                                         "description",
                                                                         locale);
    if (StringUtils.isNotBlank(description)) {
      application.setDescription(description);
    }
  }

  private CategoryLinkService getCategoryLinkService() {
    if (categoryLinkService == null) {
      categoryLinkService = portalContainer.getComponentInstanceOfType(CategoryLinkService.class);
    }
    return categoryLinkService;
  }

  private void reorderFavoritesForNewEntry(Long applicationId, String username) throws ApplicationNotFoundException {
    List<UserApplication> userApplication = appCenterStorage.getMandatoryAndFavoriteApplications(username, null)
                                                            .stream()
                                                            .filter(app -> app.getOrder() != null)
                                                            .toList();
    for (UserApplication application : userApplication) {
      appCenterStorage.updateFavoriteApplicationOrder(application.getId(),
                                                      username,
                                                      application.getId().equals(applicationId) ? 0 : application.getOrder() + 1);
    }
  }

}
