package org.exoplatform.appcenter.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.impl.util.Base64;
import org.picocontainer.Startable;

import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.plugin.ApplicationPlugin;
import org.exoplatform.appcenter.storage.ApplicationCenterStorage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.services.FileStorageException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.*;

/**
 * A Service to access and store applications
 */
public class ApplicationCenterService implements Startable {

  private static final Log               LOG                               = ExoLogger.getLogger(ApplicationCenterService.class);

  public static final String             DEFAULT_ADMINISTRATORS_GROUP      = "/platform/administrators";

  public static final String             DEFAULT_ADMINISTRATORS_PERMISSION = "*:" + DEFAULT_ADMINISTRATORS_GROUP;

  public static final String             MAX_FAVORITE_APPS                 = "maxFavoriteApps";

  public static final String             DEFAULT_APP_IMAGE_ID              = "defaultAppImageId";

  public static final String             DEFAULT_APP_IMAGE_NAME            = "defaultAppImageName";

  public static final String             DEFAULT_APP_IMAGE_BODY            = "defaultAppImageBody";

  public static final int                DEFAULT_LIMIT                     = 1000;

  private static final Context           APP_CENTER_CONTEXT                = Context.GLOBAL.id("APP_CENTER");

  private static final Scope             APP_CENTER_SCOPE                  = Scope.APPLICATION.id("APP_CENTER");

  private PortalContainer                container;

  private ConfigurationManager           configurationManager;

  private SettingService                 settingService;

  private Authenticator                  authenticator;

  private IdentityRegistry               identityRegistry;

  private ApplicationCenterStorage       appCenterStorage;

  private String                         defaultAdministratorPermission    = null;

  private long                           maxFavoriteApps                   = -1;

  private long                           defaultMaxFavoriteApps            = 0;

  private Map<String, ApplicationPlugin> defaultApplications               = new LinkedHashMap<>();
  
  
  public static String LOG_SERVICE_NAME          = "application-center";
  
  public static String LOG_OPEN_FAVORITE_DRAWER    = "open-favorite-drawer";
  
  public ApplicationCenterService(ConfigurationManager configurationManager,
                                  ApplicationCenterStorage appCenterStorage,
                                  SettingService settingService,
                                  IdentityRegistry identityRegistry,
                                  Authenticator authenticator,
                                  PortalContainer container,
                                  InitParams params) {
    this.container = container;
    this.configurationManager = configurationManager;
    this.settingService = settingService;
    this.authenticator = authenticator;
    this.identityRegistry = identityRegistry;
    this.appCenterStorage = appCenterStorage;

    if (params != null && params.containsKey("default.administrators.expression")) {
      this.defaultAdministratorPermission = params.getValueParam("default.administrators.expression").getValue();
    }
    if (params != null && params.containsKey("default.favorites")) {
      this.defaultMaxFavoriteApps = Long.parseLong(params.getValueParam("default.favorites").getValue());
    }
    if (StringUtils.isBlank(this.defaultAdministratorPermission)) {
      this.defaultAdministratorPermission = DEFAULT_ADMINISTRATORS_PERMISSION;
    }
  }

  /**
   * A method that will be invoked when the server starts (
   * {@link PortalContainer} starts ) to inject default application and to
   * delete injected default applications
   */
  @Override
  public void start() {
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(this.container);
    try {
      List<Application> systemApplications = appCenterStorage.getSystemApplications();
      systemApplications.forEach(application -> {
        if (!isDefaultSystemApplication(application)) {
          try {
            LOG.info("Delete application '{}' that was previously injected as system application and that doesn't exist in configuration anymore",
                     application.getTitle());
            appCenterStorage.deleteApplication(application.getId());
          } catch (Exception e) {
            LOG.warn("An unknown error occurs while deleting not found system application '{}' in store",
                     application.getTitle());
          }
        }
      });

      this.defaultApplications.values().forEach(applicationPlugin -> {
        Application application = applicationPlugin.getApplication();
        String pluginName = applicationPlugin.getName();
        if (application == null) {
          LOG.warn("An application plugin '{}' holds an empty application", pluginName);
          return;
        }

        String title = application.getTitle();
        if (StringUtils.isBlank(title)) {
          LOG.warn("Plugin '{}' has an application with empty title, it will not be injected", pluginName);
          return;
        }

        String url = application.getUrl();
        if (StringUtils.isBlank(url)) {
          LOG.warn("Plugin '{}' has an application with empty url, it will not be injected", pluginName);
          return;
        }

        Application storedApplication = appCenterStorage.getApplicationByTitleOrURL(title, url);
        if (storedApplication != null && !applicationPlugin.isOverride()) {
          LOG.info("Ignore updating system application '{}', override flag is turned off", application.getTitle());
          return;
        }

        List<String> permissions = application.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
          // Set default permission if empty
          application.setPermissions(IdentityConstants.ANY);
        }

        String imagePath = applicationPlugin.getImagePath();
        if (StringUtils.isNotBlank(imagePath)) {
          try {
            InputStream inputStream = configurationManager.getInputStream(imagePath);
            String fileBody = new String(Base64.encode(IOUtils.toByteArray(inputStream)));
            application.setImageFileBody(fileBody);
          } catch (Exception e) {
            LOG.warn("Error reading image from file {}. Application will be injected without image", imagePath, e);
          }
        }

        if (StringUtils.isBlank(application.getImageFileName())) {
          application.setImageFileName(application.getTitle() + ".png");
        }

        if (storedApplication == null) {
          try {
            LOG.info("Create system application '{}'",
                     application.getTitle());
            application.setSystem(true);
            application.setImageFileId(null);
            this.createApplication(application);
          } catch (Exception e) {
            LOG.error("Error creating application {}", application, e);
          }
        } else {
          try {
            LOG.info("Update system application '{}'",
                     application.getTitle());
            application.setSystem(true);
            application.setId(storedApplication.getId());
            application.setImageFileId(storedApplication.getImageFileId());
            appCenterStorage.updateApplication(application);
          } catch (Exception e) {
            LOG.error("Error updating application {}", application, e);
          }
        }
      });
    } finally {
      RequestLifeCycle.end();
    }
  }

  @Override
  public void stop() {
    // Nothing to do
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

    if (application.getPermissions() == null || application.getPermissions().isEmpty()) {
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

  /**
   * Delete application identified by its id and check if username has
   * permission to delete it.
   * 
   * @param applicationId technical identifier of application
   * @param username user currently deleting application
   * @throws ApplicationNotFoundException if application wasn't found
   * @throws IllegalAccessException if user is not allowed to delete application
   */
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
    if (storedApplication.isSystem()) {
      throw new IllegalAccessException("Application with id " + applicationId
          + " is a system application, thus it can't be deleted");
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

  /**
   * Deletes an application identified by its id from favorite applications of
   * user
   * 
   * @param applicationId application technical identifier
   * @param username login of user currently deleting application
   */
  public void deleteFavoriteApplication(Long applicationId, String username) {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
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
      settingService.set(APP_CENTER_CONTEXT,
                         APP_CENTER_SCOPE,
                         MAX_FAVORITE_APPS,
                         SettingValue.create(maxFavoriteApplications));
      this.maxFavoriteApps = maxFavoriteApplications;
    } else {
      settingService.remove(APP_CENTER_CONTEXT,
                            APP_CENTER_SCOPE,
                            MAX_FAVORITE_APPS);
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
   * @throws Exception if an exception occurs while storing image into database
   */
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

  /**
   * @return {@link GeneralSettings} of application including default image and
   *         maximum favorite applications count
   * @throws Exception if an exception occurs while retrieving image data from
   *           store
   */
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
  public ApplicationList getApplicationsList(int offset,
                                             int limit,
                                             String keyword) {
    ApplicationList applicationList = new ApplicationList();
    List<Application> applications = appCenterStorage.getApplications(keyword, offset, limit);
    applicationList.setApplications(applications);
    long totalApplications = appCenterStorage.countApplications();
    applicationList.setSize(totalApplications);
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
      UserApplication applicationFavorite = new UserApplication(app);
      applicationFavorite.setFavorite(appCenterStorage.isFavoriteApplication(applicationFavorite.getId(), username));
      return applicationFavorite;
    }).collect(Collectors.toList());
    resultApplicationsList.setApplications(userApplicationsList);
    long countFavorites = appCenterStorage.countFavorites(username);
    resultApplicationsList.setCanAddFavorite(countFavorites < getMaxFavoriteApps());
    resultApplicationsList.setOffset(offset);
    resultApplicationsList.setLimit(limit);
    resultApplicationsList.setSize(countFavorites);
    return resultApplicationsList;
  }

  /**
   * Retrieves all the list of applications for a user
   * 
   * @param username login of user
   * @return {@link ApplicationList} that contains {@link List} of
   *         {@link UserApplication}
   */
  public ApplicationList getFavoriteApplicationsList(String username) {
    List<UserApplication> favoriteApplications = appCenterStorage.getFavoriteApplicationsByUser(username);
    List<Application> applications = favoriteApplications.stream()
                                                         .filter(app -> hasPermission(username, app))
                                                         .collect(Collectors.toList());
    ApplicationList applicationList = new ApplicationList();
    applicationList.setApplications(applications);
    applicationList.setLimit(favoriteApplications.size());
    applicationList.setSize(favoriteApplications.size());
    applicationList.setOffset(0);
    return applicationList;
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
   * @throws FileStorageException if an error occurs while accessing file from
   *           store
   */
  public Long getApplicationImageLastUpdated(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                  IllegalAccessException,
                                                                                  FileStorageException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
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
   * @throws FileStorageException if an error occurs while accessing file from
   *           store
   * @throws IOException if an error occurs while building {@link InputStream}
   */
  public InputStream getApplicationImageInputStream(long applicationId, String username) throws ApplicationNotFoundException,
                                                                                         IllegalAccessException,
                                                                                         FileStorageException,
                                                                                         IOException {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
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

  /**
   * Inject a default application using IOC {@link ComponentPlugin} using
   * configuration
   * 
   * @param applicationPlugin plugin containing application to inject
   */
  public void addApplicationPlugin(ApplicationPlugin applicationPlugin) {
    if (applicationPlugin == null) {
      throw new IllegalArgumentException("'applicationPlugin' is mandatory");
    }
    if (StringUtils.isBlank(applicationPlugin.getName())) {
      throw new IllegalStateException("'applicationPlugin' name is mandatory");
    }
    this.defaultApplications.put(applicationPlugin.getName(), applicationPlugin);
  }

  /**
   * Delete an injected plugin identified by its name
   * 
   * @param pluginName plugin name to delete
   */
  public void removeApplicationPlugin(String pluginName) {
    if (StringUtils.isBlank(pluginName)) {
      throw new IllegalArgumentException("'pluginName' is mandatory");
    }
    this.defaultApplications.remove(pluginName);
  }

  /**
   * Checks whether the application is a system application injected by
   * configuration or not
   * 
   * @param application application to check its state
   * @return true if the configuration of the application exists with same title
   *         and URL, else false.
   */
  public boolean isDefaultSystemApplication(Application application) {
    if (application == null) {
      throw new IllegalArgumentException("'application' is mandatory");
    }
    return this.defaultApplications.values()
                                   .stream()
                                   .anyMatch(app -> StringUtils.equals(app.getApplication().getTitle(), application.getTitle())
                                       && StringUtils.equals(app.getApplication().getUrl(), application.getUrl()));
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
    }

    if (StringUtils.isBlank(username)) {
      return false;
    }

    if (StringUtils.equals(IdentityConstants.ANY, permissionExpression)) {
      return true;
    }

    // Ingeneral case, the user is already loggedin, thus we will get the
    // Identity from registry without having to compute it again from
    // OrganisationService, thus the condition (identity == null) will be false
    // most of the time for better performances
    Identity identity = identityRegistry.getIdentity(username);
    if (identity == null) {
      try {
        identity = authenticator.createIdentity(username);
      } catch (Exception e) {
        LOG.warn("Error getting memberships of user {}", username, e);
        return false;
      }

      // Check null again after building identity
      if (identity == null) {
        return false;
      }
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
