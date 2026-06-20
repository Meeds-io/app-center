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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.thumbnail.ImageThumbnailService;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCenterSettings;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

/**
 * A Service to access and store applications
 */
@Service
public class ApplicationCenterService {

  private static final String      APPLICATION_IS_MANDATORY_MESSAGE    = "application is mandatory";

  public static final String       ANY_PERMISSION                      = "any";

  public static final String       DEFAULT_USERS_GROUP                 = "/platform/users";

  public static final String       DEFAULT_USERS_PERMISSION            = "*:" + DEFAULT_USERS_GROUP;

  public static final String       MAX_FAVORITE_APPS                   = "maxFavoriteApps";

  public static final String       ALLOW_USER_PERSONAL_APPS            = "allowUserPersonalApps";

  public static final String       DEFAULT_APP_IMAGE_NAME              = "defaultAppImageName";

  public static final String       DEFAULT_APP_IMAGE_BODY              = "defaultAppImageBody";

  public static final int          DEFAULT_LIMIT                       = 10;

  public static final Context      APP_CENTER_CONTEXT                  = Context.GLOBAL.id("APP_CENTER");

  public static final Scope        APP_CENTER_SCOPE                    = Scope.APPLICATION.id("APP_CENTER");

  private static final String      USERNAME_IS_MANDATORY_MESSAGE       = "username is mandatory";

  private static final String      APPLICATION_ID_IS_MANDATORY_MESSAGE = "applicationId is mandatory";

  private static final String      USER_NOT_ALLOWED_MESSAGE            = "User %s is not allowed to save application : %s";

  private static final String      APPLICATION_NOT_FOUND_MESSAGE       = "Application with id %s doesn't exist";

  private static final String      PERSONAL_APPS_NOT_ENABLED_MESSAGE   = "User personal apps feature is not enabled";

  private static final String      NOT_PERSONAL_APP_OWNER_MESSAGE      = "User %s is not the owner of personal application %s";

  @Autowired
  private UserACL                  userAcl;

  @Autowired
  private SettingService           settingService;

  @Autowired
  private ApplicationCenterStorage appCenterStorage;

  @Autowired
  private TranslationService       translationService;

  @Autowired
  private FileService              fileService;

  @Autowired
  private ImageThumbnailService    imageThumbnailService;

  @Autowired
  private PortalContainer          portalContainer;

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
      if (CollectionUtils.isNotEmpty(application.getCategoryIds())) {
        application.getCategoryIds()
                   .forEach(id -> getCategoryLinkService().unlink(id,
                                                                  new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE,
                                                                                     String.valueOf(applicationId),
                                                                                     0l)));
      }
      appCenterStorage.deleteApplication(applicationId);
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
   */
  public void addFavoriteApplication(long applicationId, String username) throws ApplicationNotFoundException,
                                                                          IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    Application application = appCenterStorage.getApplication(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (!canAccess(application, username)) {
      throw new IllegalAccessException(String.format("User %s doesn't have enough permissions to delete application '%s'",
                                                     username,
                                                     application.getTitle()));
    }
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
   * Returns global app-center settings.
   */
  public ApplicationCenterSettings getSettings() {
    return new ApplicationCenterSettings(isUserPersonalAppsEnabled());
  }

  /**
   * Returns whether users are allowed to create personal URL apps.
   */
  public boolean isUserPersonalAppsEnabled() {
    SettingValue<?> value = settingService.get(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, ALLOW_USER_PERSONAL_APPS);
    return value != null && Boolean.parseBoolean(String.valueOf(value.getValue()));
  }

  /**
   * Enables or disables the user personal apps feature. Admin only.
   */
  public void setUserPersonalAppsEnabled(boolean enabled, String username) throws IllegalAccessException {
    if (!canEdit(username)) {
      throw new IllegalAccessException(String.format("User %s is not allowed to change app-center settings", username));
    }
    settingService.set(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, ALLOW_USER_PERSONAL_APPS, SettingValue.create(enabled));
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
    application.setPersonal(true);
    application.setOwnerUsername(username);
    application.setActive(true);
    application.setSystem(false);
    application.setMandatory(false);
    return createApplication(application);
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
    Application stored = appCenterStorage.getApplication(application.getId());
    if (stored == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, application.getId()));
    }
    if (!stored.isPersonal()) {
      throw new IllegalAccessException(String.format("Application %s is not a personal app", application.getId()));
    }
    if (!StringUtils.equals(stored.getOwnerUsername(), username)) {
      throw new IllegalAccessException(String.format(NOT_PERSONAL_APP_OWNER_MESSAGE, username, application.getId()));
    }
    application.setPersonal(true);
    application.setOwnerUsername(username);
    application.setActive(true);
    application.setSystem(false);
    application.setMandatory(false);
    updateApplication(application);
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
    Application stored = appCenterStorage.getApplication(applicationId);
    if (stored == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (!stored.isPersonal()) {
      throw new IllegalAccessException(String.format("Application %s is not a personal app", applicationId));
    }
    if (!StringUtils.equals(stored.getOwnerUsername(), username)) {
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
    List<Application> applications = appCenterStorage.getApplications(keyword)
                                                     .stream()
                                                     .filter(app -> !app.isPersonal())
                                                     .toList();
    int totalApplictions = applications.size();
    if (limit <= 0) {
      limit = applications.size();
    }
    applications = applications.stream().skip(offset).limit(limit).toList();
    setApplicationLabels(applications, locale);
    setApplicationCategories(applications);
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
   */
  public void updateFavoriteApplicationOrder(ApplicationOrder applicationOrder,
                                             String userName) throws ApplicationNotFoundException {
    if (StringUtils.isBlank(userName)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    if (applicationOrder.getId() <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    Application application = appCenterStorage.getApplication(applicationOrder.getId());
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationOrder.getId()));
    }
    if (!application.isMandatory()) {
      appCenterStorage.addApplicationToUserFavorite(applicationOrder.getId(), userName);
    }
    appCenterStorage.updateFavoriteApplicationOrder(applicationOrder.getId(), userName, applicationOrder.getOrder());
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

  public ApplicationList getMandatoryAndFavoriteApplications(Pageable pageable, String username, Locale locale) {
    List<Application> applications = appCenterStorage.getMandatoryAndFavoriteApplications(username, pageable)
                                                     .stream()
                                                     .filter(app -> canAccess(app, username))
                                                     .collect(Collectors.toList());
    int appCount = applications.size();
    ApplicationList applicationList = new ApplicationList();
    setApplicationLabels(applications, locale);
    setApplicationCategories(applications);
    return applicationList.setApplications(applications)
                          .setLimit(appCount)
                          .setSize(appCount)
                          .setOffset(0);
  }

  public List<String> getApplicationShortcuts(String username) {
    return getActiveApplications(null, username).stream()
                                                .map(Application::getShortcut)
                                                .filter(StringUtils::isNotBlank)
                                                .toList();
  }

  public boolean canAccess(Application application, String username) {
    if (application.isPersonal()) {
      return canEdit(username) || StringUtils.equals(application.getOwnerUsername(), username);
    }
    return canAccess(application.getPermissions(), username);
  }

  public boolean canEdit(String username) {
    return StringUtils.isBlank(username) || userAcl.isAdministrator(getUserIdentity(username));
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

  private List<Application> getActiveApplications(String keyword, String username) {
    return appCenterStorage.getApplications(keyword)
                           .stream()
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
