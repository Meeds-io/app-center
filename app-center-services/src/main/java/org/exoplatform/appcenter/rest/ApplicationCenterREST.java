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
import javax.ws.rs.core.Response.Status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import org.exoplatform.services.rest.http.PATCH;

@Path("app-center")
@RolesAllowed("users")
@Tag(name = "/app-center", description = "Manage and access application center applications") // NOSONAR
public class ApplicationCenterREST implements ResourceContainer {

  private static final String       APPLICATIONS_ENDPOINT               = "applications";

  private static final String       SETTINGS_ENDPOINT                   = "settings";

  private static final String       FAVORITES_APPLICATIONS_ENDPOINT     = "applications/favorites";

  private static final String       AUTHORIZED_APPLICATIONS_ENDPOINT    = "applications/authorized";

  private static final String       LOG_OPEN_DRAWER_ENDPOINT            = "applications/logOpenDrawer";

  private static final String       LOG_CLICK_ALL_APPLICATIONS_ENDPOINT = "applications/logClickAllApplications";

  private static final String       LOG_CLICK_ONE_APPLICATION_ENDPOINT  = "applications/logClickApplication";

  private static final String       ADMINISTRATORS_GROUP                = "/platform/administrators";

  private static final int          CACHE_DURATION_SECONDS              = 31536000;

  private static final long         CACHE_DURATION_MILLISECONDS         = CACHE_DURATION_SECONDS * 1000l;

  private static final CacheControl ILLUSTRATION_CACHE_CONTROL          = new CacheControl();

  static {
    ILLUSTRATION_CACHE_CONTROL.setMaxAge(CACHE_DURATION_SECONDS);
  }

  private static final Log          LOG                                 = ExoLogger.getLogger(ApplicationCenterREST.class);

  private ApplicationCenterService appCenterService;

  private final String             baseURI;

  public ApplicationCenterREST(ApplicationCenterService appCenterService, PortalContainer container) {
    this.appCenterService = appCenterService;
    this.baseURI = "/" + container.getName() + "/" + container.getRestContextName() + "/";
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(
          summary = "Retrieves all available sub resources of current endpoint",
          method = "GET",
          description = "Retrieves all available sub resources of current endpoint")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @Operation(
          summary = "Retrieves all available applications",
          method = "GET",
          description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getApplicationsList(@Parameter(description = "Query Offset", required = true) @QueryParam("offset") int offset,
                                      @Parameter(description = "Query results limit", required = true) @QueryParam("limit") int limit,
                                      @Parameter(description = "Keyword to search in applications title and url", required = true) @QueryParam("keyword") String keyword) {
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
  @Operation(
          summary = "Retrieves all authorized applications for currently authenticated user",
          method = "GET",
          description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response getAuthorizedApplicationsList(@Parameter(description = "Query Offset", required = true) @QueryParam("offset") int offset,
                                                @Parameter(description = "Query results limit", required = true) @QueryParam("limit") int limit,
                                                @Parameter(description = "Keyword to search in applications title and url", required = true) @QueryParam("keyword") String keyword) {

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
  @Operation(
          summary = "Retrieves favorite applications for currently authenticated user",
          method = "GET",
          description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @Operation(
          summary = "Log that the currently authenticated user clicked on View All Applications button",
          method = "GET",
          description = "Log that the currently authenticated user clicked on View All Applications button and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @Operation(
          summary = "Log that the currently authenticated user clicked on one Application",
          method = "GET",
          description = "Log that the currently authenticated user clicked on one Application and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response logClickOneApplications(@Parameter(description = "Application technical id to log", required = true) @PathParam("applicationId") Long applicationId) {
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
  @Operation(
          summary = "Log that the currently authenticated user has opened the favorites drawer",
          method = "GET",
          description = "Log that the currently authenticated user has opened the favorites drawer and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @Operation(
          summary = "Modifies default application image setting",
          method = "GET",
          description = "Modifies default application image setting and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
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
  @Operation(
          summary = "Creates a new application in application center",
          method = "GET",
          description = "Creates a new application in application center and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response createApplication(@RequestBody(description = "Application to save", required = true) Application application) {
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
  @Operation(
          summary = "Updates an existing application identified by its id or title or url",
          method = "GET",
          description = "Updates an existing application identified by its id or title or url")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response updateApplication(@RequestBody(description = "Application to update", required = true) Application application) {
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
  @Operation(
          summary = "Deletes an existing application identified by its id",
          method = "GET",
          description = "Deletes an existing application identified by its id")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteApplication(@Parameter(description = "Application technical id to delete", required = true) @PathParam("applicationId") Long applicationId) {
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
  @Operation(
          summary = "Adds an existing application identified by its id as favorite for current authenticated user",
          method = "GET",
          description = "Adds an existing application identified by its id as favorite for current authenticated user")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response addFavoriteApplication(@Parameter(description = "Application technical id to add as favorite", required = true) @PathParam("applicationId") Long applicationId) {
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
  @Operation(
          summary = "Updates an existing application's order identified by its id",
          method = "GET",
          description = "Updates an existing application's order identified by its id and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response updateApplicationsOrder(@RequestBody(description = "Application to update", required = true) List<ApplicationOrder> applicationOrders) {
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
  @Operation(
          summary = "Deletes an existing application identified by its id from current authenticated user favorites",
          method = "GET",
          description = "Deletes an existing application identified by its id from current authenticated user favorites and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response deleteFavoriteApplication(@Parameter(description = "Application technical id to delete from favorite", required = true) @PathParam("applicationId") Long applicationId) {
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
  @Operation(
          summary = "Modifies maximum application count to add as favorites for all users",
          method = "GET",
          description = "Modifies maximum application count to add as favorites for all users")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response setMaxFavoriteApps(@Parameter(description = "Max favorites number", required = true) @QueryParam("number") long number) {
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
  @Operation(
          summary = "Modifies default application image setting",
          method = "GET",
          description = "Modifies default application image setting")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
      @ApiResponse(responseCode = "500", description = "Internal server error") })
  public Response setDefaultAppImage(@Parameter(description = "Application image id, body and name", required = true) ApplicationImage defaultAppImage) {
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
  @Operation(
          summary = "Gets an application illustration by application id",
          method = "GET",
          description = "This can only be done by the logged in user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getApplicationIllustration(@Context Request request,
                                             @Parameter(description = "Application id", required = true) @PathParam("applicationId") long applicationId,
                                             @Parameter(description = "Optional last modified parameter") @QueryParam("v") long lastModified) {
    try {
      Long lastUpdated = appCenterService.getApplicationImageLastUpdated(applicationId, getCurrentUserName());
      if (lastUpdated == null) {
        return Response.status(404).build();
      }
      EntityTag eTag = new EntityTag(String.valueOf(lastUpdated), true);
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
        if (lastModified > 0) {
          builder.lastModified(new Date(lastUpdated));
          builder.expires(new Date(System.currentTimeMillis() + CACHE_DURATION_MILLISECONDS));
          builder.cacheControl(ILLUSTRATION_CACHE_CONTROL);
        }
      }
      return builder.build();
    } catch (IllegalAccessException e) {
      LOG.warn("Unauthorised access to application {} illustration", applicationId, e);
      return Response.status(Status.NOT_FOUND).build();
    } catch (ApplicationNotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (Exception e) {
      LOG.error("An error occurred while getting application illustration", e);
      return Response.serverError().build();
    }
  }

  private String getCurrentUserName() {
    ConversationState state = ConversationState.getCurrent();
    return state == null || state.getIdentity() == null ? null : state.getIdentity().getUserId();
  }

}
