package org.exoplatform.appcenter.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.storage.ApplicationCenterStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.*;

/**
 * A Service to access and store applications
 */
public class ApplicationCenterService {

  private static final Log         LOG                               = ExoLogger.getLogger(ApplicationCenterService.class);

  public static final String       DEFAULT_ADMINISTRATORS_GROUP      = "/platform/administrators";

  public static final String       DEFAULT_ADMINISTRATORS_PERMISSION = "*:" + DEFAULT_ADMINISTRATORS_GROUP;

  public static final String       MAX_FAVORITE_APPS                 = "maxFavoriteApps";

  public static final String       DEFAULT_APP_IMAGE_ID              = "defaultAppImageId";

  public static final String       DEFAULT_APP_IMAGE_NAME            = "defaultAppImageName";

  public static final String       DEFAULT_APP_IMAGE_BODY            = "defaultAppImageBody";

  public static final int          DEFAULT_LIMIT                     = 1000;

  private static final Context     APP_CENTER_CONTEXT                = Context.GLOBAL.id("APP_CENTER");

  private static final Scope       APP_CENTER_SCOPE                  = Scope.APPLICATION.id("APP_CENTER");

  private SettingService           settingService;

  private Authenticator            authenticator;

  private IdentityRegistry         identityRegistry;

  private ApplicationCenterStorage appCenterStorage;

  private String                   defaultAdministratorPermission    = null;

  private long                     maxFavoriteApps                   = -1;

  public ApplicationCenterService(ApplicationCenterStorage appCenterStorage,
                                  SettingService settingService,
                                  IdentityRegistry identityRegistry,
                                  Authenticator authenticator,
                                  InitParams params) {
    this.settingService = settingService;
    this.authenticator = authenticator;
    this.identityRegistry = identityRegistry;
    this.appCenterStorage = appCenterStorage;

    if (params != null && params.containsKey("default.administrators.expression")) {
      this.defaultAdministratorPermission = params.getValueParam("default.administrators.expression").getValue();
    }
    if (StringUtils.isBlank(this.defaultAdministratorPermission)) {
      this.defaultAdministratorPermission = DEFAULT_ADMINISTRATORS_PERMISSION;
    }
  }

  /**
   * Create new Application that will be available for all users. If the
   * application already exits an {@link ApplicationAlreadyExistsException} will
   * be thrown.
   * 
   * @param application application to create
   * @return stored {@link Application} in datasource
   * @throws Exception when application already exists or an error occurs while
   *           creating application or its attached image
   */
  public Application createApplication(Application application) throws Exception {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    Application existingApplication = appCenterStorage.getApplicationByTitleOrURL(application.getTitle(),
                                                                                  application.getUrl());
    if (existingApplication != null) {
      if (StringUtils.equals(existingApplication.getTitle(), application.getTitle())) {
        throw new ApplicationAlreadyExistsException("An application with same title already exists");
      } else {
        throw new ApplicationAlreadyExistsException("An application with same URL already exists");
      }
    }

    if (application.getPermissions() == null || application.getPermissions().length == 0) {
      application.setPermissions(this.defaultAdministratorPermission);
    }
    return appCenterStorage.createApplication(application);
  }

  /**
   * Update an existing application on datasource. If the application doesn't
   * exit an {@link ApplicationNotFoundException} will be thrown.
   * 
   * @param application dto to update on store
   * @param username username storing application
   * @return stored {@link Application} in datasource
   * @throws Exception when {@link ApplicationNotFoundException} is thrown or an
   *           error occurs while saving application
   */
  public Application updateApplication(Application application, String username) throws Exception {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    Long applicationId = application.getId();
    if (applicationId == null) {
      throw new ApplicationNotFoundException("Application with null id wasn't found");
    }
    Application storedApplication = appCenterStorage.getApplicationById(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found");
    }
    if (!hasPermission(username, storedApplication)) {
      throw new IllegalAccessException("User " + username + " is not allowed to modify application : "
          + storedApplication.getTitle());
    }
    return appCenterStorage.updateApplication(application);
  }

  public void deleteApplication(Long applicationId, String username) throws ApplicationNotFoundException, IllegalAccessException {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }

    Application storedApplication = appCenterStorage.getApplicationById(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " not found");
    }

    if (!hasPermission(username, storedApplication.getPermissions())) {
      throw new IllegalAccessException("User " + username + " doesn't have enough permissions to delete application "
          + storedApplication.getTitle());
    }
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
      throw new IllegalArgumentException("username is mandatory");
    }
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found in store");
    }
    if (!hasPermission(username, application)) {
      throw new IllegalAccessException("User " + username + " doesn't have enough permissions to delete application "
          + application.getTitle());
    }
    appCenterStorage.addApplicationToUserFavorite(applicationId, username);
  }

  public void deleteFavoriteApplication(Long applicationId, String username) {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    appCenterStorage.deleteApplicationFavorite(applicationId, username);
  }

  public void setMaxFavoriteApps(long number) {
    if (number > 0) {
      settingService.set(APP_CENTER_CONTEXT,
                         APP_CENTER_SCOPE,
                         MAX_FAVORITE_APPS,
                         SettingValue.create(number));
      this.maxFavoriteApps = number;
    } else {
      settingService.remove(APP_CENTER_CONTEXT,
                            APP_CENTER_SCOPE,
                            MAX_FAVORITE_APPS);
      this.maxFavoriteApps = -1;
    }
  }

  public long getMaxFavoriteApps() {
    if (this.maxFavoriteApps < 0) {
      SettingValue<?> maxFavoriteAppsValue = settingService.get(APP_CENTER_CONTEXT, APP_CENTER_SCOPE, MAX_FAVORITE_APPS);
      if (maxFavoriteAppsValue != null && maxFavoriteAppsValue.getValue() != null) {
        this.maxFavoriteApps = Long.parseLong(maxFavoriteAppsValue.getValue().toString());
      } else {
        this.maxFavoriteApps = 0;
      }
    }
    return this.maxFavoriteApps;
  }

  public ApplicationImage setDefaultAppImage(ApplicationImage defaultAppImage) throws Exception {
    if (defaultAppImage == null
        || (StringUtils.isBlank(defaultAppImage.getFileName()) && StringUtils.isBlank(defaultAppImage.getFileBody()))) {
      settingService.remove(APP_CENTER_CONTEXT,
                            APP_CENTER_SCOPE,
                            DEFAULT_APP_IMAGE_ID);
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

  public GeneralSettings getAppGeneralSettings() throws Exception { // NOSONAR
    GeneralSettings generalsettings = new GeneralSettings();
    generalsettings.setMaxFavoriteApps(getMaxFavoriteApps());

    Long defaultAppImageId = getDefaultImageId();
    if (defaultAppImageId != null) {
      ApplicationImage defaultImage = appCenterStorage.getAppImageFile(defaultAppImageId);
      generalsettings.setDefaultApplicationImage(defaultImage);
    }
    return generalsettings;
  }

  public ApplicationList getApplicationsList(int offset,
                                             int limit,
                                             String keyword) {
    ApplicationList applicationList = new ApplicationList();
    List<Application> applications = appCenterStorage.getApplications(keyword, offset, limit);
    applicationList.setApplications(applications);
    long totalApplications = appCenterStorage.countApplications();
    applicationList.setTotalApplications(totalApplications);
    return applicationList;
  }

  public ApplicationList getAuthorizedApplicationsList(int offset,
                                                       int limit,
                                                       String keyword,
                                                       String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    ApplicationList resultApplicationsList = new ApplicationList();
    List<Application> userApplicationsList = getApplications(offset, limit, keyword, username);
    userApplicationsList = userApplicationsList.stream().map(app -> {
      ApplicationFavorite applicationFavorite = new ApplicationFavorite(app);
      applicationFavorite.setFavorite(appCenterStorage.isFavoriteApplication(applicationFavorite.getId(), username));
      return applicationFavorite;
    }).collect(Collectors.toList());
    resultApplicationsList.setApplications(userApplicationsList);
    long countFavorites = appCenterStorage.countFavorites(username);
    resultApplicationsList.setTotalApplications(countFavorites);
    resultApplicationsList.setCanAddFavorite(countFavorites < getMaxFavoriteApps());
    return resultApplicationsList;
  }

  public List<ApplicationFavorite> getFavoriteApplicationsList(String username) {
    List<ApplicationFavorite> favoriteApplications = appCenterStorage.getFavoriteApplicationsByUser(username);
    return favoriteApplications.stream().filter(app -> hasPermission(username, app)).collect(Collectors.toList());
  }

  public Long getApplicationImageLastUpdated(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                  IllegalAccessException,
                                                                                  FileStorageException {
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found");
    }
    if (!hasPermission(username, application)) {
      throw new IllegalAccessException("User " + username + " isn't allowed to access application with id " + applicationId);
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

  public InputStream getApplicationImageInputStream(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                         IllegalAccessException,
                                                                                         FileStorageException,
                                                                                         IOException {
    Application application = appCenterStorage.getApplicationById(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found");
    }
    if (!hasPermission(username, application)) {
      throw new IllegalAccessException("User " + username + " isn't allowed to access application with id " + applicationId);
    }
    if (application.getImageFileId() != null && application.getImageFileId() > 0) {
      return appCenterStorage.getApplicationImageInputStream(application.getImageFileId());
    } else {
      Long defaultImageId = getDefaultImageId();
      if (defaultImageId != null && defaultImageId > 0) {
        return appCenterStorage.getApplicationImageInputStream(defaultImageId);
      }
    }
    return null;
  }

  private boolean hasPermission(String username, Application application) {
    return hasPermission(username, application.getPermissions());
  }

  private boolean hasPermission(String username, String[] storedPermissions) {
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
    }

    if (StringUtils.isBlank(username)) {
      return false;
    }

    if (StringUtils.equals(IdentityConstants.ANY, permissionExpression)) {
      return true;
    }

    org.exoplatform.services.security.Identity identity = identityRegistry.getIdentity(username);
    if (identity == null) {
      try {
        identity = authenticator.createIdentity(username);
      } catch (Exception e) {
        LOG.warn("Error getting memberships of user {}", username, e);
      }
    }
    if (identity == null) {
      return false;
    }
    MembershipEntry membership = null;
    if (permissionExpression.contains(":")) {
      String[] permissionExpressionParts = permissionExpression.split(":");
      membership = new MembershipEntry(permissionExpressionParts[1], permissionExpressionParts[0]);
    } else if (permissionExpression.contains("/")) {
      membership = new MembershipEntry(permissionExpression, MembershipEntry.ANY_TYPE);
    } else {
      return StringUtils.equals(username, permissionExpression);
    }
    return identity.isMemberOf(membership);
  }

  private Long getDefaultImageId() {
    SettingValue<?> defaultAppImageIdSetting = settingService.get(APP_CENTER_CONTEXT,
                                                                  APP_CENTER_SCOPE,
                                                                  DEFAULT_APP_IMAGE_ID);
    Long defaultAppImageId = null;
    if (defaultAppImageIdSetting != null && defaultAppImageIdSetting.getValue() != null) {
      defaultAppImageId = Long.parseLong(defaultAppImageIdSetting.getValue().toString());
    }
    return defaultAppImageId;
  }

  private List<Application> getApplications(int offset, int limit, String keyword, String username) {
    if (offset < 0) {
      offset = 0;
    }
    if (limit <= 0) {
      limit = DEFAULT_LIMIT;
    }
    List<Application> userApplicationsList = new ArrayList<>();
    int limitToRetrieve = offset + limit;
    int offsetOfSearch = 0;
    do {
      List<Application> applications = appCenterStorage.getApplications(keyword, offsetOfSearch, offset + limit);
      applications = applications.stream().filter(app -> hasPermission(username, app)).collect(Collectors.toList());
      userApplicationsList.addAll(applications);
      limitToRetrieve = applications.isEmpty() ? 0 : limitToRetrieve - userApplicationsList.size();
      offsetOfSearch += limit;
    } while (limitToRetrieve > 0);
    if (offset > 0) {
      if (userApplicationsList.size() > offset) {
        userApplicationsList = userApplicationsList.subList(offset, userApplicationsList.size());
      } else {
        userApplicationsList.clear();
      }
    }
    if (userApplicationsList.size() > limit) {
      userApplicationsList = userApplicationsList.subList(0, limit);
    }
    return userApplicationsList;
  }

}
