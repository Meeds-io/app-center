package org.exoplatform.appCenter.services.rest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import org.exoplatform.appCenter.services.dao.ApplicationDAO;
import org.exoplatform.appCenter.services.dao.FavoriteApplicationDAO;
import org.exoplatform.appCenter.services.entity.ApplicationForm;
import org.exoplatform.appCenter.services.entity.ApplicationImage;
import org.exoplatform.appCenter.services.entity.jpa.Application;
import org.exoplatform.appCenter.services.entity.jpa.FavoriteApplication;
import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.service.rest.Util;

@Path("appCenter/applications")
public class ApplicationsRestService implements ResourceContainer {

  private static final Log       log = ExoLogger.getLogger(ApplicationsRestService.class);

  private ApplicationDAO         applicationDAO;

  private FavoriteApplicationDAO favoriteApplicationDAO;

  private FileService            fileService;

  private SettingService         settingService;

  private OrganizationService    organizationService;
  
  public final static String NAME_SPACE = "appCenter";
  public final static String MAX_FAVORITE_APPS = "maxFavoriteApps";
  public final static String DEFAULT_APP_IMAGE_ID = "defaultAppImageId";
  public final static String DEFAULT_APP_IMAGE_NAME = "defaultAppImageName";
  public final static String DEFAULT_APP_IMAGE_BODY = "defaultAppImageBody";
  public final static String APP_ID = "appId";
  public final static String APP_TITLE = "appTitle";
  public final static String APP_URL = "appUrl";
  public final static String APP_IMAGE_FILE_ID = "appImageFileId";
  public final static String APP_IMAGE_FILE_NAME = "appImageFileName";
  public final static String APP_IMAGE_FILE_BODY = "appImageFileBody";
  public final static String APP_DESCRIPTION = "appDescription";
  public final static String APP_ACTIVE = "appActive";
  public final static String APP_DEFAULT = "appDefault";
  public final static String APP_PERMISSIONS = "appPermissions";
  public final static String ADMINISTRATORS_PERMISSION = "*:/platform/administrators";

  public ApplicationsRestService(ApplicationDAO applicationDAO,
                                      FavoriteApplicationDAO favoriteApplicationDAO,
                                      FileService fileService,
                                      SettingService settingService,
                                      OrganizationService organizationService) {
    this.applicationDAO = applicationDAO;
    this.favoriteApplicationDAO = favoriteApplicationDAO;
    this.fileService = fileService;
    this.settingService = settingService;
    this.organizationService = organizationService;
  }

  @POST
  @Path("/addApplication")
  @RolesAllowed("administrators")
  public Response addApplication(@Context UriInfo uriInfo,
                                 ApplicationForm applicationForm) throws Exception {
    Application existingApplication = applicationDAO.getAppByNameOrTitle(applicationForm.getTitle(),
                                                                         applicationForm.getUrl());
    if (existingApplication != null) {
      return existingApplication.getTitle().equals(applicationForm.getTitle()) ? Util.getResponse(applicationForm.getTitle()
                                                                                                      + " application already exists",
                                                                                                  uriInfo,
                                                                                                  MediaType.TEXT_PLAIN_TYPE,
                                                                                                  Response.Status.UNAUTHORIZED)
                                                                              : Util.getResponse(applicationForm.getUrl()
                                                                                                     + " application already exists",
                                                                                                 uriInfo,
                                                                                                 MediaType.TEXT_PLAIN_TYPE,
                                                                                                 Response.Status.UNAUTHORIZED);
    }

    FileItem fileItem = createAppImageFileItem(applicationForm.getImageFileName(),
                                               applicationForm.getImageFileBody());

    Long fileId = fileItem != null ? fileItem.getFileInfo().getId() : null;

    Application application = new Application(applicationForm.getTitle(),
                                              applicationForm.getUrl(),
                                              fileId,
                                              applicationForm.getDescription(),
                                              applicationForm.isActive(),
                                              applicationForm.isByDefault(),
                                              applicationForm.getPermissions().length == 0 ? ADMINISTRATORS_PERMISSION
                                                                                          : String.join(",",
                                                                                                        applicationForm.getPermissions()));
    applicationDAO.create(application);
    return Util.getResponse(application.getTitle()
                                + " application is created successfully",
                            uriInfo,
                            MediaType.TEXT_PLAIN_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/addFavoriteApplication/{applicationId}")
  @RolesAllowed("users")
  public Response addFavoriteApplication(@Context UriInfo uriInfo,
                                         @PathParam("applicationId") Long applicationId) throws Exception {

    String currentUser = ConversationState.getCurrent()
                                          .getIdentity()
                                          .getUserId();
    Application application = applicationDAO.find(applicationId);
    if (application == null) {
      return Util.getResponse("Can not find application with id "
                                  + applicationId,
                              uriInfo,
                              MediaType.TEXT_PLAIN_TYPE,
                              Response.Status.NOT_FOUND);
    }
    FavoriteApplication favoriteApplication = new FavoriteApplication(application,
                                                                      currentUser);
    favoriteApplicationDAO.create(favoriteApplication);
    return getFavoriteApplicationsList(uriInfo);
  }

  @POST
  @Path("/editApplication")
  @RolesAllowed("administrators")
  @ExoTransactional
  public Response editApplication(@Context UriInfo uriInfo,
                                  ApplicationForm applicationForm) throws Exception {
    Application application = applicationDAO.find(applicationForm.getId());
    if (application == null) {
      return Util.getResponse("No application found",
                              uriInfo,
                              MediaType.TEXT_PLAIN_TYPE,
                              Response.Status.NOT_FOUND);
    }
    FileItem fileItem = createAppImageFileItem(applicationForm.getImageFileName(),
                                               applicationForm.getImageFileBody());
    Long fileId = fileItem != null ? fileItem.getFileInfo().getId() : null;

    application.setTitle(applicationForm.getTitle());
    application.setUrl(applicationForm.getUrl());
    application.setImageFileId(fileId);
    application.setDescription(applicationForm.getDescription());
    application.setActive(applicationForm.isActive());
    application.setByDefault(applicationForm.isByDefault());
    application.setPermissions(applicationForm.getPermissions().length == 0 ? ADMINISTRATORS_PERMISSION
                                                                           : String.join(",",
                                                                                         applicationForm.getPermissions()));
    applicationDAO.update(application);
    List<FavoriteApplication> favoriteApplications = favoriteApplicationDAO.getFavoriteAppsByAppId(application.getId());
    if (favoriteApplications.size() > 0) {
      if (!application.isActive()) {
        favoriteApplications.forEach(favoriteApplication -> {
          favoriteApplicationDAO.delete(favoriteApplication);
        });
      } else {
        favoriteApplications.forEach(favoriteApplication -> {
          try {
            Collection<Group> groups = organizationService.getGroupHandler()
                                                          .findGroupsOfUser(favoriteApplication.getUserName());
            boolean isMember = false;
            for (Group group : groups) {
              if (favoriteApplication.getApplication()
                                     .getPermissions()
                                     .contains(group.getGroupName())) {
                isMember = true;
                break;
              }
            }
            if (!isMember) {
              favoriteApplicationDAO.delete(favoriteApplication);
            }
          } catch (Exception e) {
            log.error("Can not get groups of user "
                          + favoriteApplication.getUserName(),
                      e);
          }
        });
      }
    }
    return Util.getResponse(application.getTitle()
                                + " application is updated successfully",
                            uriInfo,
                            MediaType.TEXT_PLAIN_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/deleteApplication/{applicationId}")
  @RolesAllowed("administrators")
  public Response deleteApplication(@Context UriInfo uriInfo,
                                    @PathParam("applicationId") Long applicationId) throws Exception {
    Application application = applicationDAO.find(applicationId);
    if (application == null) {
      return Util.getResponse("No application found",
                              uriInfo,
                              MediaType.TEXT_PLAIN_TYPE,
                              Response.Status.NOT_FOUND);
    }
    String title = application.getTitle();
    applicationDAO.delete(application);
    return Util.getResponse(title + " application is deleted successfully",
                            uriInfo,
                            MediaType.TEXT_PLAIN_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/deleteFavoriteApplication/{applicationId}")
  @RolesAllowed("users")
  public Response deleteFavoriteApplication(@Context UriInfo uriInfo,
                                            @PathParam("applicationId") Long applicationId) throws Exception {
    Application application = applicationDAO.find(applicationId);
    if (application == null) {
      return Util.getResponse("Can not find application with id "
                                  + applicationId,
                              uriInfo,
                              MediaType.TEXT_PLAIN_TYPE,
                              Response.Status.NOT_FOUND);
    }
    String currentUser = ConversationState.getCurrent()
                                          .getIdentity()
                                          .getUserId();
    FavoriteApplication favoriteApplication = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId,
                                                                                                      currentUser);
    if (favoriteApplication == null) {
      return Util.getResponse("Application " + applicationId
                                  + " is not a favorite application for user "
                                  + currentUser,
                              uriInfo,
                              MediaType.TEXT_PLAIN_TYPE,
                              Response.Status.OK);
    }
    favoriteApplicationDAO.delete(favoriteApplication);
    return getFavoriteApplicationsList(uriInfo);
  }

  @GET
  @Path("/setMaxFavorite")
  @RolesAllowed("administrators")
  public Response setMaxFavoriteApps(@Context UriInfo uriInfo,
                                     @QueryParam("number") String number) throws Exception {

    if (number != null) {
      settingService.set(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                         Scope.GLOBAL,
                         MAX_FAVORITE_APPS,
                         SettingValue.create(number));
    } else {
      settingService.remove(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                            Scope.GLOBAL,
                            MAX_FAVORITE_APPS);
    }
    return Util.getResponse("Max favorite applications is set successfully",
                            uriInfo,
                            MediaType.TEXT_PLAIN_TYPE,
                            Response.Status.OK);
  }

  @POST
  @Path("/setDefaultImage")
  @RolesAllowed("administrators")
  public Response setDefaultAppImage(@Context UriInfo uriInfo,
                                     ApplicationImage defaultAppImage) throws Exception {

    FileItem fileItem = createAppImageFileItem(defaultAppImage.getFileName(),
                                               defaultAppImage.getFileBody());
    if (fileItem != null) {
      settingService.set(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                         Scope.GLOBAL,
                         DEFAULT_APP_IMAGE_ID,
                         SettingValue.create(String.valueOf(fileItem.getFileInfo()
                                                                    .getId())));
    } else {
      settingService.remove(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                            Scope.GLOBAL,
                            DEFAULT_APP_IMAGE_ID);
    }
    return Util.getResponse("Default application image is set successfully",
                            uriInfo,
                            MediaType.TEXT_PLAIN_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/getGeneralSettings")
  @RolesAllowed("users")
  public Response getAppGeneralSettings(@Context UriInfo uriInfo) throws Exception {
    JSONObject generalsettings = new JSONObject();
    generalsettings.put(MAX_FAVORITE_APPS,
                        getMaxFavoriteApps());
    if (settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                           Scope.GLOBAL,
                           DEFAULT_APP_IMAGE_ID) != null) {
      String defaultAppImageId = settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                                                    Scope.GLOBAL,
                                                    DEFAULT_APP_IMAGE_ID)
                                               .getValue()
                                               .toString();
      Map<String, String> defaultAppImageFile = getAppImageFile(Long.parseLong(defaultAppImageId));
      if (defaultAppImageFile.containsKey(APP_IMAGE_FILE_NAME)) {
        generalsettings.put(DEFAULT_APP_IMAGE_NAME,
                            defaultAppImageFile.get(APP_IMAGE_FILE_NAME));
      }
      if (defaultAppImageFile.containsKey(APP_IMAGE_FILE_BODY)) {
        generalsettings.put(DEFAULT_APP_IMAGE_BODY,
                            defaultAppImageFile.get(APP_IMAGE_FILE_BODY));
      }
    }
    return Util.getResponse(generalsettings.toString(),
                            uriInfo,
                            MediaType.APPLICATION_JSON_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/getApplicationsList")
  @RolesAllowed("administrators")
  public Response getApplicationsList(@Context UriInfo uriInfo,
                                      @QueryParam("offset") String offset,
                                      @QueryParam("limit") String limit,
                                      @QueryParam("keyword") String keyword) throws Exception {
    List<Application> applicationsList = applicationDAO.findAll()
                                                       .stream()
                                                       .filter(app -> StringUtils.containsIgnoreCase(app.getTitle(),
                                                                                                     keyword)
                                                           || StringUtils.containsIgnoreCase(app.getDescription(),
                                                                                             keyword))
                                                       .collect(Collectors.toList());
    JSONObject applicationsListJson = getApplicationsListJson(applicationsList,
                                                              offset,
                                                              limit,
                                                              null);
    return Util.getResponse(applicationsListJson.toString(),
                            uriInfo,
                            MediaType.APPLICATION_JSON_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/getAuthorizedApplicationsList")
  @RolesAllowed("users")
  public Response getAuthorizedApplicationsList(@Context UriInfo uriInfo,
                                                @QueryParam("offset") String offset,
                                                @QueryParam("limit") String limit,
                                                @QueryParam("keyword") String keyword) throws Exception {

    String currentUser = ConversationState.getCurrent()
                                          .getIdentity()
                                          .getUserId();
    List<Application> authorizedApplicationsList = applicationDAO.getAuthorizedApplications(currentUser,
                                                                                            keyword);
    JSONObject authorizedApplicationsListJson = getApplicationsListJson(authorizedApplicationsList,
                                                                        offset,
                                                                        limit,
                                                                        "authorized");
    return Util.getResponse(authorizedApplicationsListJson.toString(),
                            uriInfo,
                            MediaType.APPLICATION_JSON_TYPE,
                            Response.Status.OK);
  }

  @GET
  @Path("/getFavoriteApplicationsList")
  @RolesAllowed("users")
  public Response getFavoriteApplicationsList(@Context UriInfo uriInfo) throws Exception {

    List<Application> favoriteApplicationsList = getFavoriteApplications();
    JSONObject favoriteApplicationsListJson = getApplicationsListJson(favoriteApplicationsList,
                                                                      null,
                                                                      null,
                                                                      "favorite");
    return Util.getResponse(favoriteApplicationsListJson.toString(),
                            uriInfo,
                            MediaType.APPLICATION_JSON_TYPE,
                            Response.Status.OK);
  }

  private JSONObject getApplicationsListJson(List<Application> applicationsList,
                                             String offset,
                                             String limit,
                                             String type) throws Exception {
    applicationsList = applicationsList.stream()
                                       .sorted(Comparator.comparing(Application::getTitle))
                                       .collect(Collectors.toList());
    JSONObject applicationtsListJson = new JSONObject();

    if (offset != null && limit != null) {

      applicationtsListJson.put("totalApplications", applicationsList.size());
      applicationtsListJson.put("totalPages",
                                applicationsList.size()
                                    % Integer.parseInt(limit) == 0 ? applicationsList.size()
                                                                      / Integer.parseInt(limit)
                                                                  : applicationsList.size()
                                                                      / Integer.parseInt(limit)
                                                                      + 1);
      applicationsList = applicationsList.stream()
                                         .skip(Integer.parseInt(offset)
                                             * Integer.parseInt(limit))
                                         .limit(Integer.parseInt(limit))
                                         .collect(Collectors.toList());
    }
    JSONArray applicationsListJsonArray = new JSONArray();
    applicationsList.stream()
                    .forEachOrdered(application -> {
                      try {
                        JSONObject applicationtJson = new JSONObject();
                        applicationtJson.put(APP_ID,
                                             application.getId());
                        applicationtJson.put(APP_TITLE,
                                             application.getTitle());
                        applicationtJson.put(APP_URL,
                                             application.getUrl());
                        Map<String, String> appImageFile = getAppImageFile(application.getImageFileId());
                        if (appImageFile.containsKey(APP_IMAGE_FILE_NAME)) {
                          applicationtJson.put(APP_IMAGE_FILE_NAME,
                                               appImageFile.get(APP_IMAGE_FILE_NAME));
                        }
                        if (appImageFile.containsKey(APP_IMAGE_FILE_BODY)) {
                          applicationtJson.put(APP_IMAGE_FILE_BODY,
                                               appImageFile.get(APP_IMAGE_FILE_BODY));
                        }
                        applicationtJson.put(APP_DESCRIPTION,
                                             application.getDescription());
                        applicationtJson.put(APP_ACTIVE,
                                             application.isActive());
                        applicationtJson.put(APP_DEFAULT,
                                             application.isByDefault());
                        String[] permissions = Arrays.stream(application.getPermissions()
                                                                        .split(","))
                                                     .toArray(String[]::new);
                        applicationtJson.put(APP_PERMISSIONS,
                                             permissions);
                        if (type != null && type.equals("authorized")) {
                          applicationtJson.put("isFavorite",
                                               isFavoriteApplication(application.getId()));
                        }
                        applicationsListJsonArray.put(applicationtJson);
                      } catch (Exception e) {
                        log.error("Can not build application json", e);
                      }
                    });
    applicationtsListJson.put("applications", applicationsListJsonArray);
    if (type != null && type.equals("authorized")) {
      applicationtsListJson.put("canAddFavorite",
                                getMaxFavoriteApps() == null
                                    || getFavoriteApplications().size() < Integer.parseInt(getMaxFavoriteApps()));
    }
    return applicationtsListJson;
  }

  private FileItem createAppImageFileItem(String fileName, String fileBody) throws Exception {
    FileItem fileItem = null;
    if (fileName != null && !fileName.equals("") && fileBody != null
        && !fileBody.equals("")) {
      String[] file = fileBody.split(",");
      String fileContent = file[1];
      // Retrieve mime file from "data:application/pdf;base64"
      String fileMime = file[0].replace("data:", "").replace(";base64", "");
      Date currentDate = new Date();
      byte[] fileContentBytes = Base64.getDecoder().decode(fileContent);
      long size = 0;
      String currentUser = ConversationState.getCurrent()
                                            .getIdentity()
                                            .getUserId();
      fileItem = new FileItem(null,
                              fileName,
                              fileMime,
                              NAME_SPACE,
                              size,
                              currentDate,
                              currentUser,
                              false,
                              new ByteArrayInputStream(fileContentBytes));
      fileItem = fileService.writeFile(fileItem);
    }
    return fileItem;
  }

  private Map<String, String> getAppImageFile(Long fileId) throws Exception {
    if (fileId == null
        && settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                              Scope.GLOBAL,
                              DEFAULT_APP_IMAGE_ID) == null) {
      return new HashMap<String, String>();
    }

    if (fileId == null) {
      fileId = Long.parseLong(settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                                                 Scope.GLOBAL,
                                                 DEFAULT_APP_IMAGE_ID)
                                            .getValue()
                                            .toString());
    }
    FileItem fileItem = fileService.getFile(fileId);
    Map<String, String> file = new HashMap<String, String>();
    if (fileItem != null) {
      byte[] bytes = fileItem.getAsByte();
      String fileBody = Base64.getEncoder().encodeToString(bytes);
      String fileMime = fileItem.getFileInfo().getMimetype();
      String fileName = fileItem.getFileInfo().getName();
      file.put(APP_IMAGE_FILE_NAME, fileName);
      file.put(APP_IMAGE_FILE_BODY, "data:" + fileMime
          + ";base64," + fileBody);
    }
    return file;
  }

  private boolean isFavoriteApplication(Long applicationId) throws Exception {
    Application application = applicationDAO.find(applicationId);
    if (application == null) {
      return false;
    }
    String currentUser = ConversationState.getCurrent()
                                          .getIdentity()
                                          .getUserId();
    FavoriteApplication favoriteApplication = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId,
                                                                                                      currentUser);
    return favoriteApplication != null;
  }

  private String getMaxFavoriteApps() {
    return settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                              Scope.GLOBAL,
                              MAX_FAVORITE_APPS) != null ? settingService.get(org.exoplatform.commons.api.settings.data.Context.GLOBAL,
                                                                                               Scope.GLOBAL,
                                                                                               MAX_FAVORITE_APPS)
                                                                                          .getValue()
                                                                                          .toString()
                                                                         : null;
  }

  private List<Application> getFavoriteApplications() {
    String currentUser = ConversationState.getCurrent()
                                          .getIdentity()
                                          .getUserId();
    List<Application> favoriteApplications = new ArrayList<Application>();
    favoriteApplications = Stream.concat(favoriteApplicationDAO.getFavoriteApps(currentUser)
                                                               .stream()
                                                               .map(userFavoriteApp -> userFavoriteApp.getApplication()),
                                         applicationDAO.getDefaultApplications(currentUser)
                                                       .stream())
                                 .distinct()
                                 .sorted(Comparator.comparing(Application::getTitle))
                                 .collect(Collectors.toList());
    if (getMaxFavoriteApps() != null) {
      favoriteApplications = favoriteApplications.stream()
                                                 .limit(Long.parseLong(getMaxFavoriteApps()))
                                                 .collect(Collectors.toList());
    }
    return favoriteApplications;
  }
}
