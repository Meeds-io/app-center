package org.exoplatform.appcenter.rest;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.json.JSONObject;

import org.exoplatform.appcenter.dao.*;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationImage;
import org.exoplatform.appcenter.dto.ApplicationList;
import org.exoplatform.appcenter.service.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

@Path("appCenter/applications")
public class ApplicationCenterREST implements ResourceContainer {

  private static final Log         LOG = ExoLogger.getLogger(ApplicationCenterREST.class);

  private ApplicationCenterService appCenterService;

  public ApplicationCenterREST(ApplicationCenterService appCenterService) {
    this.appCenterService = appCenterService;
  }

  @POST
  @Path("/addApplication")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response createApplication(Application application) {
    try {
      appCenterService.createApplication(application);
    } catch (ApplicationAlreadyExistsException e) {
      LOG.warn(e.getMessage());
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while adding application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @POST
  @Path("/editApplication")
  @RolesAllowed("administrators")
  public Response updateApplication(Application application) {
    try {
      appCenterService.updateApplication(application, getCurrentUserName());
    } catch (IllegalAccessException e) {
      LOG.warn(e.getMessage());
      return Response.status(403).build();
    } catch (ApplicationAlreadyExistsException e) {
      LOG.warn(e);
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/deleteApplication/{applicationId}")
  @RolesAllowed("administrators")
  public Response deleteApplication(@PathParam("applicationId") Long applicationId) {
    try {
      appCenterService.deleteApplication(applicationId, getCurrentUserName());
    } catch (IllegalAccessException e) {
      LOG.warn(e.getMessage());
      return Response.status(403).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e.getMessage());
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/addFavoriteApplication/{applicationId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response addFavoriteApplication(@PathParam("applicationId") Long applicationId) {
    try {
      appCenterService.addFavoriteApplication(applicationId, getCurrentUserName());
    } catch (IllegalAccessException e) {
      LOG.warn(e.getMessage());
      return Response.status(403).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e.getMessage());
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while adding application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/deleteFavoriteApplication/{applicationId}")
  @RolesAllowed("users")
  public Response deleteFavoriteApplication(@PathParam("applicationId") Long applicationId) {
    try {
      appCenterService.deleteFavoriteApplication(applicationId, getCurrentUserName());
    } catch (Exception e) {
      LOG.error("Unknown error occurred while deleting application from favorites", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/setMaxFavorite")
  @RolesAllowed("administrators")
  public Response setMaxFavoriteApps(@QueryParam("number") long number) {
    try {
      appCenterService.setMaxFavoriteApps(number);
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @POST
  @Path("/setDefaultImage")
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response setDefaultAppImage(ApplicationImage defaultAppImage) {
    try {
      appCenterService.setDefaultAppImage(defaultAppImage);
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
    return Response.noContent().build();
  }

  @GET
  @Path("/getGeneralSettings")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getAppGeneralSettings() {
    try {
      JSONObject appGeneralSettings = appCenterService.getAppGeneralSettings();
      return Response.ok(appGeneralSettings).build();
    } catch (ApplicationNotFoundException e) {
      LOG.warn(e.getMessage());
      return Response.serverError().build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/getApplicationsList")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("administrators")
  public Response getApplicationsList(@QueryParam("offset") int offset,
                                      @QueryParam("limit") int limit,
                                      @QueryParam("keyword") String keyword) {
    try {
      ApplicationList applicationList = appCenterService.getApplicationsList(offset,
                                                                             limit,
                                                                             keyword);
      return Response.ok(applicationList).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/getAuthorizedApplicationsList")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getAuthorizedApplicationsList(@QueryParam("offset") int offset,
                                                @QueryParam("limit") int limit,
                                                @QueryParam("keyword") String keyword) {

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
  @Path("/getFavoriteApplicationsList")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getFavoriteApplicationsList(@Context UriInfo uriInfo) {

    try {
      List<Application> applications = appCenterService.getFavoriteApplicationsList(getCurrentUserName());
      return Response.ok(applications).build();
    } catch (Exception e) {
      LOG.error("Unknown error occurred while updating application", e);
      return Response.serverError().build();
    }
  }

  private String getCurrentUserName() {
    ConversationState state = ConversationState.getCurrent();
    return state == null || state.getIdentity() == null ? null
                                                        : state.getIdentity()
                                                               .getUserId();
  }

}
