/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.appcenter.rest;

import java.io.InputStream;
import java.util.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.service.*;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.MembershipEntry;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;

@Path("app-center")
@RolesAllowed("users")
@Api(value = "/app-center", description = "Manage and access application center applications") // NOSONAR
public class ApplicationCenterREST implements ResourceContainer {

  private static final String      APPLICATIONS_ENDPOINT               = "applications";

  private static final String      SETTINGS_ENDPOINT                   = "settings";

  private static final String      FAVORITES_APPLICATIONS_ENDPOINT     = "applications/favorites";

  private static final String      AUTHORIZED_APPLICATIONS_ENDPOINT    = "applications/authorized";

  private static final String      LOG_OPEN_DRAWER_ENDPOINT            = "applications/logOpenDrawer";

  private static final String      LOG_CLICK_ALL_APPLICATIONS_ENDPOINT = "applications/logClickAllApplications";
  
  private static final String      LOG_CLICK_ONE_APPLICATION_ENDPOINT = "applications/logClickApplication";
  
  
  private static final String      ADMINISTRATORS_GROUP                = "/platform/administrators";

  private static final Log         LOG                                 = ExoLogger.getLogger(ApplicationCenterREST.class);

  private ApplicationCenterService appCenterService;

  private final String             baseURI;

  public ApplicationCenterREST(ApplicationCenterService appCenterService, PortalContainer container) {
    this.appCenterService = appCenterService;
    this.baseURI = "/" + container.getName() + "/" + container.getRestContextName() + "/";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves all available subresources of current endpoint", httpMethod = "GET", response = Response.class, produces = "application/json")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getAvailableSubResources() {
    try {
      Map<String, List<String>> availableEnpoints = new HashMap<>();
      ConversationState current = ConversationState.getCurrent();
      if (current.getIdentity().isMemberOf(new MembershipEntry(ADMINISTRATORS_GROUP, "*"))) {
        availableEnpoints.put("subResourcesHref",
                              Arrays.asList(this.baseURI + FAVORITES_APPLICATIONS_ENDPOINT,
                                            this.baseURI + AUTHORIZED_APPLICATIONS_ENDPOINT,
                                            this.baseURI + SETTINGS_ENDPOINT,
                                            this.baseURI + APPLICATIONS_ENDPOINT));
      } else {
        availableEnpoints.put("subResourcesHref",
                              Arrays.asList(this.baseURI + FAVORITES_APPLICATIONS_ENDPOINT,
                                            this.baseURI + AUTHORIZED_APPLICATIONS_ENDPOINT,
                                            this.baseURI + SETTINGS_ENDPOINT));
      }
      return Response.ok(availableEnpoints).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(APPLICATIONS_ENDPOINT)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Retrieves all available applications", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getApplicationsList(@ApiParam(value = "Query Offset", required = true) @QueryParam("offset") int offset,
                                      @ApiParam(value = "Query results limit", required = true) @QueryParam("limit") int limit,
                                      @ApiParam(value = "Keyword to search in applications title and url", required = true) @QueryParam("keyword") String keyword) {
    try {
      ApplicationList applicationList = appCenterService.getApplicationsList(offset, limit, keyword);
      return Response.ok(applicationList).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(AUTHORIZED_APPLICATIONS_ENDPOINT)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves all authorized applications for currently authenticated user", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getAuthorizedApplicationsList(@ApiParam(value = "Query Offset", required = true) @QueryParam("offset") int offset,
                                                @ApiParam(value = "Query results limit", required = true) @QueryParam("limit") int limit,
                                                @ApiParam(value = "Keyword to search in applications title and url", required = true) @QueryParam("keyword") String keyword) {

    try {
      ApplicationList applicationList = appCenterService.getAuthorizedApplicationsList(offset,
                                                                                       limit,
                                                                                       keyword,
                                                                                       getCurrentUserName());
      return Response.ok(applicationList).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(FAVORITES_APPLICATIONS_ENDPOINT)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Retrieves favorite applications for currently authenticated user", httpMethod = "GET", response = Response.class, produces = "application/json", notes = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getFavoriteApplicationsList() {
    try {
      ApplicationList applicationList = appCenterService.getMandatoryAndFavoriteApplicationsList(getCurrentUserName());
      return Response.ok(applicationList).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(LOG_CLICK_ALL_APPLICATIONS_ENDPOINT)
  @RolesAllowed("users")
  @ApiOperation(value = "Log that the currently authenticated user clicked on View All Applications button", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response logClickAllApplications() {
    try {
      LOG.info("service={} operation={} parameters=\"user:{}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_CLICK_ALL_APPLICATIONS,
               getCurrentUserName(),
               "0");
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }
  
  
  @GET
  @Path(LOG_CLICK_ONE_APPLICATION_ENDPOINT+ "/{applicationId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Log that the currently authenticated user clicked on one Application", httpMethod = "GET",
      response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response logClickOneApplications(@ApiParam(value = "Application technical id to log", required = true) @PathParam("applicationId") Long applicationId) {
    try {
      Application application = appCenterService.findApplication(applicationId);
      
      LOG.info("service={} operation={} parameters=\"user:{},applicationId={},applicationName={}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_OPEN_APPLICATION,
               getCurrentUserName(),
               applicationId,
               application.getTitle(),
               "0");
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(LOG_OPEN_DRAWER_ENDPOINT)
  @RolesAllowed("users")
  @ApiOperation(value = "Log that the currently authenticated user has openend the favorites drawer", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response logOpenDrawer() {
    try {
      LOG.info("service={} operation={} parameters=\"user:{}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_OPEN_FAVORITE_DRAWER,
               getCurrentUserName(),
               "0");
      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(SETTINGS_ENDPOINT)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Modifies default application image setting", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getAppGeneralSettings() {
    try {
      GeneralSettings generalSettings = appCenterService.getAppGeneralSettings();
      return Response.ok(generalSettings).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path(APPLICATIONS_ENDPOINT)
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Creates a new application in application center", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response createApplication(@ApiParam(value = "Application to save", required = true) Application application) {
    try {
      appCenterService.createApplication(application);
    } catch (ApplicationAlreadyExistsException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while creating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @PUT
  @Path(APPLICATIONS_ENDPOINT)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Updates an existing application identified by its id or title or url", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response updateApplication(@ApiParam(value = "Application to update", required = true) Application application) {
    try {
      application.setChangedManually(true);
      appCenterService.updateApplication(application, getCurrentUserName());
    } catch (IllegalAccessException e) {
      LOG.warn(e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (ApplicationAlreadyExistsException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @DELETE
  @Path(APPLICATIONS_ENDPOINT + "/{applicationId}")
  @RolesAllowed("administrators")
  @ApiOperation(value = "Deletes an existing application identified by its id", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response deleteApplication(@ApiParam(value = "Application technical id to delete", required = true) @PathParam("applicationId") Long applicationId) {
    try {
      appCenterService.deleteApplication(applicationId, getCurrentUserName());
    } catch (IllegalAccessException e) {
      LOG.warn(e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while deleting application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @POST
  @Path(FAVORITES_APPLICATIONS_ENDPOINT + "/{applicationId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Adds an existing application identified by its id as favorite for current authenticated user", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response addFavoriteApplication(@ApiParam(value = "Application technical id to add as favorite", required = true) @PathParam("applicationId") Long applicationId) {
    try {
      long startTime = System.currentTimeMillis();
      Application application = appCenterService.findApplication(applicationId);
      appCenterService.addFavoriteApplication(applicationId, getCurrentUserName());
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.info("service={} operation={} parameters=\"user:{},applicationId={},applicationName={}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_ADD_FAVORITE,
               getCurrentUserName(),
               applicationId,
               application.getTitle(),
               totalTime);
      return Response.noContent().build();
    } catch (IllegalAccessException e) {
      LOG.warn(e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while adding application as favorite", e);
      return Response.serverError().build();
    }
  }

  @PUT
  @Path(FAVORITES_APPLICATIONS_ENDPOINT)
  @RolesAllowed("users")
  @ApiOperation(value = "Updates an existing application's order identified by its id", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response updateApplicationsOrder(@ApiParam(value = "Application to update", required = true) List<ApplicationOrder> applicationOrders) {
    try {
      long startTime = System.currentTimeMillis();
      for (ApplicationOrder applicationOrder : applicationOrders) {
        appCenterService.updateFavoriteApplicationOrder(applicationOrder, getCurrentUserName());
      }
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.info("service={} operation={} parameters=\"user:{}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_REORGANIZE_FAVORITES,
               getCurrentUserName(),
               totalTime);
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while adding application as favorite", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @DELETE
  @Path(FAVORITES_APPLICATIONS_ENDPOINT + "/{applicationId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Deletes an existing application identified by its id from current authenticated user favorites", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response deleteFavoriteApplication(@ApiParam(value = "Application technical id to delete from favorite", required = true) @PathParam("applicationId") Long applicationId) {
    try {
      long startTime = System.currentTimeMillis();
      Application application = appCenterService.findApplication(applicationId);
      appCenterService.deleteFavoriteApplication(applicationId, getCurrentUserName());
      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      LOG.info("service={} operation={} parameters=\"user:{},applicationId={},applicationName={}\" status=ok " + "duration_ms={}",
               ApplicationCenterService.LOG_SERVICE_NAME,
               ApplicationCenterService.LOG_REMOVE_FAVORITE,
               getCurrentUserName(),
               applicationId,
               application.getTitle(),
               totalTime);
      return Response.noContent().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while deleting application from favorites", e);
      return Response.serverError().build();
    }
  }

  @PATCH
  @Path(SETTINGS_ENDPOINT + "/maxFavorites")
  @RolesAllowed("administrators")
  @ApiOperation(value = "Modifies maximum application count to add as favorites for all users", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response setMaxFavoriteApps(@ApiParam(value = "Max favorites number", required = true) @QueryParam("number") long number) {
    try {
      appCenterService.setMaxFavoriteApps(number);
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @PATCH
  @Path(SETTINGS_ENDPOINT + "/image")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  @ApiOperation(value = "Modifies default application image setting", httpMethod = "GET", response = Response.class, notes = "empty response")
  @ApiResponses(value = { @ApiResponse(code = HTTPStatus.NO_CONTENT, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response setDefaultAppImage(@ApiParam(value = "Application image id, body and name", required = true) ApplicationImage defaultAppImage) {
    try {
      appCenterService.setDefaultAppImage(defaultAppImage);
      return Response.noContent().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(APPLICATIONS_ENDPOINT + "/illustration/{applicationId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Gets an application illustration by application id", httpMethod = "GET", response = Response.class, notes = "This can only be done by the logged in user.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error"), @ApiResponse(code = 400, message = "Invalid query input"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getApplicationIllustration(@Context Request request,
                                             @ApiParam(value = "Application id", required = true) @PathParam("applicationId") long applicationId) {
    try {
      Long lastUpdated = appCenterService.getApplicationImageLastUpdated(applicationId, getCurrentUserName());
      if (lastUpdated == null) {
        return Response.status(404).build();
      }
      EntityTag eTag = new EntityTag(Integer.toString(lastUpdated.hashCode()));
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        InputStream stream = appCenterService.getApplicationImageInputStream(applicationId, getCurrentUserName());
        if (stream == null) {
          return Response.status(404).build();
        }
        /*
         * As recommended in the the RFC1341
         * (https://www.w3.org/Protocols/rfc1341/4_Content-Type.html), we set the avatar
         * content-type to "image/png". So, its data would be recognized as "image" by
         * the user-agent.
         */
        builder = Response.ok(stream, "image/png");
        builder.tag(eTag);
      }
      CacheControl cc = new CacheControl();
      cc.setMaxAge(86400);
      builder.cacheControl(cc);
      return builder.cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      LOG.warn(e);
      return Response.status(HTTPStatus.UNAUTHORIZED).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while getting application illustration", e);
      return Response.serverError().build();
    }
  }

  @Path(APPLICATIONS_ENDPOINT + "/search")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(
          value = "Search the list of applications available with query, identified by its identity technical identifier."
              + " If no designated owner, all events available for authenticated user will be retrieved.",
          httpMethod = "GET",
          response = Response.class,
          produces = "application/json"
  )
  @ApiResponses(
          value = { @ApiResponse(code = HTTPStatus.OK, message = "Request fulfilled"),
              @ApiResponse(code = HTTPStatus.UNAUTHORIZED, message = "Unauthorized operation"),
              @ApiResponse(code = HTTPStatus.BAD_REQUEST, message = "The offset or the limit is not positive"), }
  )
  public Response search(
                        @ApiParam(value = "Term to search", required = true)
                        @QueryParam(
                          "query"
                        )
                        String query,
                        @ApiParam(value = "Offset", required = false, defaultValue = "0")
                        @QueryParam(
                          "offset"
                        )
                        int offset,
                        @ApiParam(value = "Limit", required = false, defaultValue = "20")
                        @QueryParam(
                          "limit"
                        )
                        int limit) throws Exception {
    if (offset < 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Offset must be 0 or positive").build();
    }
    if (limit < 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Limit must be positive").build();
    }
    String currentUser = getCurrentUserName();
    List<ApplicationSearchResultEntity> searchResults = appCenterService.search(currentUser, query, offset, limit);
    return  Response.ok(searchResults).build();
  }

  private String getCurrentUserName() {
    ConversationState state = ConversationState.getCurrent();
    return state == null || state.getIdentity() == null ? null : state.getIdentity().getUserId();
  }

}
