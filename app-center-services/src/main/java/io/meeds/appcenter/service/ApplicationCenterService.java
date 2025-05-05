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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationImage;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.GeneralSettings;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationAlreadyExistsException;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.storage.ApplicationCenterStorage;

import lombok.SneakyThrows;

/**
 * A Service to access and store applications
 */
@Service
public class ApplicationCenterService {

  private static final String      APPLICATION_IS_MANDATORY_MESSAGE    = "application is mandatory";

  public static final String       DEFAULT_ADMINISTRATORS_GROUP        = "/platform/administrators";

  public static final String       DEFAULT_ADMINISTRATORS_PERMISSION   = "*:" + DEFAULT_ADMINISTRATORS_GROUP;

  public static final String       ANY_PERMISSION                      = "any";

  public static final String       DEFAULT_USERS_GROUP                 = "/platform/users";

  public static final String       DEFAULT_USERS_PERMISSION            = "*:" + DEFAULT_USERS_GROUP;

  public static final String       MAX_FAVORITE_APPS                   = "maxFavoriteApps";

  public static final String       DEFAULT_APP_IMAGE_ID                = "defaultAppImageId";

  public static final String       DEFAULT_APP_IMAGE_NAME              = "defaultAppImageName";

  public static final String       DEFAULT_APP_IMAGE_BODY              = "defaultAppImageBody";

  public static final int          DEFAULT_LIMIT                       = 10;

  public static final Context      APP_CENTER_CONTEXT                  = Context.GLOBAL.id("APP_CENTER");

  public static final Scope        APP_CENTER_SCOPE                    = Scope.APPLICATION.id("APP_CENTER");

  private static final String      USERNAME_IS_MANDATORY_MESSAGE       = "username is mandatory";

  private static final String      APPLICATION_ID_IS_MANDATORY_MESSAGE = "applicationId is mandatory";

  private static final String      USER_NOT_ALLOWED_MESSAGE            = "User %s is not allowed to save application : %s";

  private static final String      APPLICATION_NOT_FOUND_MESSAGE       = "Application with id %s doesn't exist";

  @Autowired
  private SettingService           settingService;

  @Autowired
  private Authenticator            authenticator;

  @Autowired(required = false)
  private IdentityRegistry         identityRegistry;

  @Autowired
  private ApplicationCenterStorage appCenterStorage;

  @Value("${appcenter.administrators.expression:*:/platform/administrators}") // NOSONAR
  private String                   defaultAdministratorPermission      = null;

  @Value("${appcenter.favorites.count:12}") // NOSONAR
  private long                     defaultMaxFavoriteApps;

  @Value("${appcenter.favorites.max:-1}") // NOSONAR
  private long                     maxFavoriteApps;

  /**
   * Create new Application that will be available for all users. If the
   * application already exits an {@link ApplicationAlreadyExistsException} will
   * be thrown.
   *
   * @param application application to create
   * @param username user making the operation
   * @return stored {@link Application} in datasource
   * @throws ApplicationAlreadyExistsException when application already exists
   * @throws IllegalAccessException if user is not allowed to create an
   *           application
   */
  public Application createApplication(Application application, String username) throws ApplicationAlreadyExistsException,
                                                                                 IllegalAccessException {
    if (StringUtils.isBlank(username) || !isAdministrator(username)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE,
                                                     username,
                                                     application.getTitle()));
    }
    return createApplication(application);
  }

  /**
   * Create new Application that will be available for all users. If the
   * application already exits an {@link ApplicationAlreadyExistsException} will
   * be thrown.
   *
   * @param application application to create
   * @return stored {@link Application} in datasource
   * @throws ApplicationAlreadyExistsException when application already exists
   */
  public Application createApplication(Application application) throws ApplicationAlreadyExistsException {
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    Application existingApplication = appCenterStorage.getApplicationByTitle(application.getTitle());
    if (existingApplication != null) {
      throw new ApplicationAlreadyExistsException("appcenter.sameTitleAlreadyExists");
    }
    if (!isUrlValid(application.getUrl())) {
      throw new IllegalArgumentException("appcenter.malformedUrl");
    }
    if (StringUtils.isNotBlank(application.getHelpPageURL()) && !isUrlValid(application.getHelpPageURL())) {
      throw new IllegalArgumentException("appcenter.malformedHelpUrl");
    }
    if (application.getPermissions() == null || application.getPermissions().isEmpty()) {
      application.setPermissions(Collections.singletonList(DEFAULT_USERS_PERMISSION));
    }
    return appCenterStorage.createApplication(application);
  }

  /**
   * Get an application by id
   *
   * @param applicationId application to find
   * @return stored {@link Application} in datasource
   * @throws ApplicationNotFoundException when application doesn't exists
   */
  public Application findApplication(long applicationId) throws ApplicationNotFoundException {
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    return application;
  }

  /**
   * Update an existing application on datasource. If the application doesn't
   * exit an {@link ApplicationNotFoundException} will be thrown.
   *
   * @param application dto to update on store
   * @param username username storing application
   * @return stored {@link Application} in datasource
   * @throws IllegalAccessException if user is not allowed to update application
   * @throws ApplicationNotFoundException if application wasn't found
   */
  public Application updateApplication(Application application, String username) throws IllegalAccessException,
                                                                                 ApplicationNotFoundException {
    if (application == null) {
      throw new IllegalArgumentException(APPLICATION_IS_MANDATORY_MESSAGE);
    }
    Long applicationId = application.getId();
    if (applicationId == null) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (!isUrlValid(application.getUrl())) {
      throw new IllegalArgumentException("appcenter.malformedUrl");
    }
    Application storedApplication = appCenterStorage.getApplicationById(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (StringUtils.isBlank(username) || !isAdministrator(username)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE,
                                                     username,
                                                     application.getTitle()));
    }
    if (application.getPermissions() == null || application.getPermissions().isEmpty()) {
      application.setPermissions(Collections.singletonList(DEFAULT_USERS_PERMISSION));
    }

    return updateApplication(application);
  }

  public Application updateApplication(Application application) throws ApplicationNotFoundException {
    return appCenterStorage.updateApplication(application);
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
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }

    Application storedApplication = appCenterStorage.getApplicationById(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    } else if (!isAdministrator(username)) {
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
    appCenterStorage.deleteApplication(applicationId);
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
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    if (!hasPermission(username, application)) {
      throw new IllegalAccessException(String.format("User %s doesn't have enough permissions to delete application '%s'",
                                                     username,
                                                     application.getTitle()));
    }
    boolean isFavoriteApplication = appCenterStorage.isFavoriteApplication(applicationId, username);
    if (!isFavoriteApplication) {
      appCenterStorage.addApplicationToUserFavorite(applicationId, username);
    }
  }

  /**
   * Deletes an application identified by its id from favorite applications of
   * user
   *
   * @param applicationId application technical identifier
   * @param username login of user currently deleting application
   */
  public void deleteFavoriteApplication(Long applicationId, String username) {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    appCenterStorage.deleteApplicationFavorite(applicationId, username);
  }

  /**
   * Change general setting for maximum allowed favorites that a user can have
   *
   * @param maxFavoriteApplications max favorite applications count
   */
  public void setMaxFavoriteApps(long maxFavoriteApplications) {
    if (maxFavoriteApplications >= 0) {
      settingService.set(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, MAX_FAVORITE_APPS, SettingValue.create(maxFavoriteApplications));
      this.maxFavoriteApps = maxFavoriteApplications;
    } else {
      settingService.remove(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, MAX_FAVORITE_APPS);
      this.maxFavoriteApps = -1;
    }
  }

  /**
   * @return the maximum favorite applications that a user can have as favorite
   */
  public long getMaxFavoriteApps() {
    if (this.maxFavoriteApps < 0) {
      SettingValue<?> maxFavoriteAppsValue = settingService.get(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, MAX_FAVORITE_APPS);
      if (maxFavoriteAppsValue != null && maxFavoriteAppsValue.getValue() != null) {
        this.maxFavoriteApps = Long.parseLong(maxFavoriteAppsValue.getValue().toString());
      } else {
        this.maxFavoriteApps = this.defaultMaxFavoriteApps;
      }
    }
    return this.maxFavoriteApps;
  }

  /**
   * Stores default image for applications not having an attached illustration
   *
   * @param defaultAppImage image content and name
   * @return stored image
   */
  public ApplicationImage setDefaultAppImage(ApplicationImage defaultAppImage) {
    if (defaultAppImage == null
        || (StringUtils.isBlank(defaultAppImage.getFileName()) && StringUtils.isBlank(defaultAppImage.getFileBody()))) {
      settingService.remove(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, DEFAULT_APP_IMAGE_ID);
    } else {
      ApplicationImage applicationImage = appCenterStorage.saveAppImageFileItem(defaultAppImage);
      if (applicationImage != null && applicationImage.getId() != null && applicationImage.getId() > 0) {
        settingService.set(APP_CENTER_CONTEXT,
                           APP_CENTER_SCOPE,
                           DEFAULT_APP_IMAGE_ID,
                           SettingValue.create(String.valueOf(applicationImage.getId())));
        return applicationImage;
      }
    }
    return null;
  }

  /**
   * @return {@link GeneralSettings} of application including default image and
   *         maximum favorite applications count
   */
  public GeneralSettings getSettings() { // NOSONAR
    GeneralSettings generalsettings = new GeneralSettings();
    generalsettings.setMaxFavoriteApps(getMaxFavoriteApps());

    Long defaultAppImageId = getDefaultImageId();
    if (defaultAppImageId != null) {
      ApplicationImage defaultImage = appCenterStorage.getAppImageFile(defaultAppImageId);
      generalsettings.setDefaultApplicationImage(defaultImage);
    }
    return generalsettings;
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
    ApplicationList applicationList = new ApplicationList();
    List<Application> applications = appCenterStorage.getApplications(keyword);
    int totalApplictions = applications.size();
    if (limit <= 0) {
      limit = applications.size();
    }
    applications = applications.stream().skip(offset).limit(limit).toList();
    applicationList.setApplications(applications);
    applicationList.setSize(totalApplictions);
    applicationList.setOffset(offset);
    applicationList.setLimit(limit);
    return applicationList;
  }

  public Application getApplicationByTitle(String title) {
    return appCenterStorage.getApplicationByTitle(title);
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
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    ApplicationList resultApplicationsList = new ApplicationList();
    List<Application> applications = getActiveApplications(keyword, username).stream().toList();
    int totalApplication = applications.size();
    if (limit > 0) {
      if (offset < 0) {
        offset = 0;
      }
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
    resultApplicationsList.setApplications(applications);
    long countFavorites = appCenterStorage.countFavorites(username);
    resultApplicationsList.setCanAddFavorite(countFavorites < getMaxFavoriteApps());
    resultApplicationsList.setOffset(offset);
    resultApplicationsList.setLimit(limit);
    resultApplicationsList.setSize(totalApplication);
    return resultApplicationsList;
  }

  /**
   * Retrieves all the list of applications for a user
   *
   * @param username login of user
   * @return {@link ApplicationList} that contains {@link List} of
   *         {@link UserApplication}
   */
  public ApplicationList getMandatoryAndFavoriteApplicationsList(String username) {
    List<UserApplication> mandatoryAndFavoriteApplications =
                                                           new ArrayList<>(appCenterStorage.getFavoriteApplicationsByUser(username));
    List<Long> mandatoryAndFavoriteApplicationsId = mandatoryAndFavoriteApplications.stream()
                                                                                    .map(UserApplication::getId)
                                                                                    .toList();
    appCenterStorage.getMandatoryApplications().forEach(userApplication -> {
      if (!mandatoryAndFavoriteApplicationsId.contains(userApplication.getId())) {
        mandatoryAndFavoriteApplications.add(userApplication);
      }
    });

    List<Application> applications = mandatoryAndFavoriteApplications.stream()
                                                                     .filter(app -> hasPermission(username, app))
                                                                     .collect(Collectors.toList());
    ApplicationList applicationList = new ApplicationList();
    applicationList.setApplications(applications);
    long countFavorites = appCenterStorage.countFavorites(username);
    applicationList.setCanAddFavorite(countFavorites < getMaxFavoriteApps());
    applicationList.setLimit(applications.size());
    applicationList.setSize(applications.size());
    applicationList.setOffset(0);
    return applicationList;
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
    Application application = appCenterStorage.getApplicationById(applicationOrder.getId());
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationOrder.getId()));
    }
    appCenterStorage.updateFavoriteApplicationOrder(applicationOrder.getId(), userName, applicationOrder.getOrder());
  }

  /**
   * Return the {@link Application} illustration last modifed timestamp (in ms),
   * if not found, the default image last modifed timestamp will be retrieved
   *
   * @param applicationId technical id of application
   * @param username login of user accessing application
   * @return timestamp in milliseconds of last modified date of illustration
   * @throws ApplicationNotFoundException if application wasn't found
   * @throws IllegalAccessException if user doesn't have access permission to
   *           application
   */
  public Long getApplicationImageLastUpdated(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                  IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    // if user is admin then no need to check for permissions
    if (!isAdministrator(username) && !hasPermission(username, application)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE, username, application.getTitle()));
    }
    if (application.getImageFileId() != null && application.getImageFileId() > 0) {
      return appCenterStorage.getApplicationImageLastUpdated(application.getImageFileId());
    } else {
      Long defaultImageId = getDefaultImageId();
      if (defaultImageId != null && defaultImageId > 0) {
        return appCenterStorage.getApplicationImageLastUpdated(defaultImageId);
      }
    }
    return null;
  }

  /**
   * Return the {@link Application} illustration {@link InputStream}, if not
   * found, the default image {@link InputStream} will be retrieved
   *
   * @param applicationId technical id of application
   * @param username login of user accessing application
   * @return {@link InputStream} of application illustration
   * @throws ApplicationNotFoundException if application wasn't found
   * @throws IllegalAccessException if user doesn't have access permission to
   *           application
   */
  public InputStream getApplicationImageInputStream(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                         IllegalAccessException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    // if user is admin then no need to check for permissions
    if (!isAdministrator(username) && !hasPermission(username, application)) {
      throw new IllegalAccessException(String.format(USER_NOT_ALLOWED_MESSAGE, username, application.getTitle()));
    }
    InputStream applicationImageInputStream = null;
    if (application.getImageFileId() != null && application.getImageFileId() > 0) {
      applicationImageInputStream = appCenterStorage.getApplicationImageInputStream(application.getImageFileId());
    }
    if (applicationImageInputStream == null) {
      // result is null if there is no image associated to the application
      // or if the image is not readable (data corruption, or quarantined by an
      // antivirus)
      Long defaultImageId = getDefaultImageId();
      if (defaultImageId != null && defaultImageId > 0) {
        applicationImageInputStream = appCenterStorage.getApplicationImageInputStream(defaultImageId);
      }
    }
    return applicationImageInputStream;
  }

  public List<Application> getSystemApplications() {
    return appCenterStorage.getSystemApplications();
  }

  public ApplicationList getMandatoryAndFavoriteApplications(String username, Pageable pageable) {
    List<Application> applications = appCenterStorage.getMandatoryAndFavoriteApplications(username, pageable)
                                                     .stream()
                                                     .filter(app -> hasPermission(username, app))
                                                     .collect(Collectors.toList());
    int appCount = applications.size();
    return new ApplicationList().setApplications(applications)
                                .setCanAddFavorite(appCount < getMaxFavoriteApps())
                                .setLimit(appCount)
                                .setSize(appCount)
                                .setOffset(0);
  }
  
  private boolean isAdministrator(String username) {
    return hasPermission(username, DEFAULT_ADMINISTRATORS_GROUP);
  }

  private boolean hasPermission(String username, Application application) {
    return hasPermission(username, application.getPermissions());
  }

  private boolean hasPermission(String username, List<String> storedPermissions) {
    if (storedPermissions == null) {
      return true;
    }
    for (String storedPermission : storedPermissions) {
      if (hasPermission(username, storedPermission)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasPermission(String username, String permissionExpression) {
    if (StringUtils.isBlank(permissionExpression)) {
      return true;
    } else if (StringUtils.isBlank(username)) {
      return false;
    }

    // In general case, the user is already loggedin, thus we will get the
    // Identity from registry without having to compute it again from
    // OrganisationService, thus the condition (identity == null) will be false
    // most of the time for better performances
    MembershipEntry membership = null;
    if (permissionExpression.contains(":")) {
      String[] permissionExpressionParts = permissionExpression.split(":");
      membership = new MembershipEntry(permissionExpressionParts[1], permissionExpressionParts[0]);
    } else if (permissionExpression.contains("/")) {
      membership = new MembershipEntry(permissionExpression, MembershipEntry.ANY_TYPE);
    } else {
      return StringUtils.equals(username, permissionExpression);
    }
    return getUserIdentity(username).isMemberOf(membership);
  }

  @SneakyThrows
  private Identity getUserIdentity(String username) {
    Identity identity = getIdentityRegistry().getIdentity(username);
    if (identity == null) {
      return authenticator.createIdentity(username);
    } else {
      return identity;
    }
  }

  private Long getDefaultImageId() {
    SettingValue<?> defaultAppImageIdSetting = settingService.get(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, DEFAULT_APP_IMAGE_ID);
    Long defaultAppImageId = null;
    if (defaultAppImageIdSetting != null && defaultAppImageIdSetting.getValue() != null) {
      defaultAppImageId = Long.parseLong(defaultAppImageIdSetting.getValue().toString());
    }
    return defaultAppImageId;
  }

  private List<Application> getActiveApplications(String keyword, String username) {
    return appCenterStorage.getApplications(keyword)
                           .stream()
                           .filter(app -> hasPermission(username, app))
                           .filter(Application::isActive)
                           .toList();

  }

  private boolean isUrlValid(String url) {
    // [-a-zA-Z0-9@:%._\\\\/+~#=] allowed characters
    String regex = "([a-zA-Z0-9-@:._\\/?&]+:\\/\\/)?http(s)?:\\/\\/[-a-zA-Z0-9@:%._\\\\/+~#=?&]{2,256}";
    Pattern pattern = Pattern.compile(regex);
    return url != null && !url.isBlank()
           && (url.startsWith("/portal/") || url.startsWith("./") || pattern.matcher(url).matches());
  }

  public IdentityRegistry getIdentityRegistry() {
    if (identityRegistry == null) {
      // Kernel Container Service not recognized, thus retrieve it
      // differently when not possible through @Autowired
      identityRegistry = ExoContainerContext.getService(IdentityRegistry.class);
    }
    return identityRegistry;
  }
}
