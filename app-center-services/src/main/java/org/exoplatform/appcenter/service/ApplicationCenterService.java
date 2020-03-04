package org.exoplatform.appcenter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.storage.ApplicationCenterStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.*;

/**
 * A Service to access and store applications
 */
public class ApplicationCenterService {

  private static final Log   LOG                               = ExoLogger.getLogger(ApplicationCenterService.class);

  public static final String DEFAULT_ADMINISTRATORS_PERMISSION = "*:/platform/administrators";

  public static final String MAX_FAVORITE_APPS                 = "maxFavoriteApps";

  public static final String DEFAULT_APP_IMAGE_ID              = "defaultAppImageId";

  public static final String DEFAULT_APP_IMAGE_NAME            = "defaultAppImageName";

  public static final String DEFAULT_APP_IMAGE_BODY            = "defaultAppImageBody";

  public static final String APP_ID                            = "appId";

  public static final String APP_TITLE                         = "appTitle";

  public static final String APP_URL                           = "appUrl";

  public static final String APP_IMAGE_FILE_ID                 = "appImageFileId";

  public static final String APP_IMAGE_FILE_NAME               = "appImageFileName";

  public static final String APP_IMAGE_FILE_BODY               = "appImageFileBody";

  public static final String APP_DESCRIPTION                   = "appDescription";

  public static final String APP_ACTIVE                        = "appActive";

  public static final String APP_DEFAULT                       = "appDefault";

  public static final String APP_PERMISSIONS                   = "appPermissions";

  public static final int    DEFAULT_LIMIT                     = 1000;

  private SettingService     settingService;

  private Authenticator      authenticator;

  private IdentityRegistry   identityRegistry;

  private ApplicationCenterStorage   appCenterStorage;

  private String             defaultAdministratorPermission    = null;

  private long               maxFavoriteApps                   = -1;

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
   * Create new Application that will be available for all users
   * 
   * @param application application to create
   * @throws Exception when application already exists or an error occurs while
   *           creating application or its attached image
   */
  public void createApplication(Application application) throws Exception {
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
    appCenterStorage.createApplication(application);
  }

  /**
   * Add an application, identified by its technical id, as favorite of a user
   * 
   * @param applicationId technical application id
   * @param username user login
   * @throws ApplicationNotFoundException when application is not found
   */
  public void addFavoriteApplication(long applicationId, String username) throws ApplicationNotFoundException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    appCenterStorage.addApplicationToUserFavorite(applicationId, username);
  }

  public void updateApplication(Application application, String username) throws Exception {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    Long applicationId = application.getId();
    Application storedApplication = appCenterStorage.getApplicationById(applicationId);
    if (storedApplication == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + "wasn't found");
    }

    String[] storedPermissions = storedApplication.getPermissions();
    boolean isNotallowedToModifyApplication = hasPermission(username, storedPermissions);
    if (!isNotallowedToModifyApplication) {
      throw new IllegalAccessException("User " + username + "is not allowed to modify application : "
          + storedApplication.getTitle());
    }
    appCenterStorage.updateApplication(application);
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
      settingService.set(Context.GLOBAL,
                         Scope.GLOBAL,
                         MAX_FAVORITE_APPS,
                         SettingValue.create(number));
      this.maxFavoriteApps = number;
    } else {
      settingService.remove(Context.GLOBAL,
                            Scope.GLOBAL,
                            MAX_FAVORITE_APPS);
      this.maxFavoriteApps = 0;
    }
  }

  public long getMaxFavoriteApps() {
    if (this.maxFavoriteApps < 0) {
      SettingValue<?> maxFavoriteAppsValue = settingService.get(Context.GLOBAL, Scope.GLOBAL, MAX_FAVORITE_APPS);
      if (maxFavoriteAppsValue != null && maxFavoriteAppsValue.getValue() != null) {
        this.maxFavoriteApps = Long.parseLong(maxFavoriteAppsValue.getValue().toString());
      } else {
        this.maxFavoriteApps = 0;
      }
    }
    return this.maxFavoriteApps;
  }

  public void setDefaultAppImage(ApplicationImage defaultAppImage) throws Exception {
    if (defaultAppImage == null
        || (StringUtils.isBlank(defaultAppImage.getFileName()) && StringUtils.isBlank(defaultAppImage.getFileBody()))) {
      settingService.remove(Context.GLOBAL,
                            Scope.GLOBAL,
                            DEFAULT_APP_IMAGE_ID);
    } else {
      FileItem fileItem = appCenterStorage.createAppImageFileItem(defaultAppImage.getFileName(),
                                                                  defaultAppImage.getFileBody());
      if (fileItem != null) {
        settingService.set(Context.GLOBAL,
                           Scope.GLOBAL,
                           DEFAULT_APP_IMAGE_ID,
                           SettingValue.create(String.valueOf(fileItem.getFileInfo())));
      }
    }
  }

  public JSONObject getAppGeneralSettings() throws Exception {
    JSONObject generalsettings = new JSONObject();
    generalsettings.put(MAX_FAVORITE_APPS,
                        getMaxFavoriteApps());
    SettingValue<?> defaultAppImageIdSetting = settingService.get(Context.GLOBAL,
                                                                  Scope.GLOBAL,
                                                                  DEFAULT_APP_IMAGE_ID);
    if (defaultAppImageIdSetting != null && defaultAppImageIdSetting.getValue() != null) {
      long defaultAppImageId = Long.parseLong(defaultAppImageIdSetting.getValue().toString());
      ApplicationImage defaultImage = appCenterStorage.getAppImageFile(defaultAppImageId);
      if (defaultImage != null) {
        generalsettings.put(DEFAULT_APP_IMAGE_NAME, defaultImage.getFileName());
        generalsettings.put(DEFAULT_APP_IMAGE_BODY, defaultImage.getFileBody());
      }
    }
    return generalsettings;
  }

  public ApplicationList getApplicationsList(int offset,
                                             int limit,
                                             String keyword) {
    ApplicationList applicationList = new ApplicationList();
    List<Application> applications = appCenterStorage.findApplications(keyword, offset, limit);
    applicationList.setApplications(applications);
    long totalApplications = appCenterStorage.countApplications();
    applicationList.setTotalApplications(totalApplications);
    return applicationList;
  }

  public ApplicationList getAuthorizedApplicationsList(int offset,
                                                       int limit,
                                                       String keyword,
                                                       String username) {
    ApplicationList resultApplicationsList = new ApplicationList();
    List<Application> userApplicationsList = getApplications(offset, limit, keyword, username);
    resultApplicationsList.setApplications(userApplicationsList);
    long countFavorites = appCenterStorage.countFavorites(username);
    resultApplicationsList.setTotalApplications(countFavorites);
    resultApplicationsList.setCanAddFavorite(countFavorites < getMaxFavoriteApps());
    return resultApplicationsList;
  }

  public List<Application> getFavoriteApplicationsList(String username) {
    List<Application> favoriteApplications = appCenterStorage.getFavoriteApplicationsByUser(username);
    return favoriteApplications.stream().filter(app -> hasPermission(username, app)).collect(Collectors.toList());
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
      throw new IllegalArgumentException("Permission expression is mandatory");
    }
    if (StringUtils.isBlank(username)) {
      return false;
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
      List<Application> applications = appCenterStorage.findApplications(keyword, offsetOfSearch, offset + limit);
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
