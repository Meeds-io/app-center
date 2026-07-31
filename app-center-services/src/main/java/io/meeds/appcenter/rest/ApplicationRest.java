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
package io.meeds.appcenter.rest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.file.services.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCenterSettings;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("applications")
@Tag(name = "/app-center/rest/applications", description = "Manage and access application center applications") // NOSONAR
public class ApplicationRest {

  private static final Log         LOG = ExoLogger.getLogger(ApplicationRest.class);

  @Autowired
  private ApplicationCenterService appCenterService;

  @Autowired
  private FileService fileService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieves all authorized applications for currently authenticated user", method = "GET", description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ApplicationList getActiveApplications(
                                               HttpServletRequest request,
                                               @Parameter(description = "Query Offset", required = true)
                                               @RequestParam("offset")
                                               Optional<Integer> offset,
                                               @Parameter(description = "Query results limit", required = true)
                                               @RequestParam("limit")
                                               Optional<Integer> limit,
                                               @Parameter(description = "Keyword to search in applications title and url", required = false)
                                               @RequestParam("keyword")
                                               Optional<String> keyword) {
    return appCenterService.getActiveApplications(offset.orElse(0),
                                                  limit.orElse(0),
                                                  keyword.orElse(null),
                                                  request.getLocale(),
                                                  request.getRemoteUser());
  }

  @GetMapping(path = "all")
  @Secured("administrators")
  @Operation(summary = "Retrieves all available applications", method = "GET", description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ApplicationList getApplications(
                                         HttpServletRequest request,
                                         @Parameter(description = "Query Offset", required = true)
                                         @RequestParam("offset")
                                         Optional<Integer> offset,
                                         @Parameter(description = "Query results limit", required = true)
                                         @RequestParam("limit")
                                         Optional<Integer> limit,
                                         @Parameter(description = "Keyword to search in applications title and url", required = true)
                                         @RequestParam("keyword")
                                         Optional<String> keyword) {
    return appCenterService.getApplications(offset.orElse(0),
                                            limit.orElse(0),
                                            keyword.orElse(null),
                                            request.getLocale());
  }

  @PostMapping
  @Secured("administrators")
  @Operation(summary = "Creates a new application in application center", method = "GET", description = "Creates a new application in application center and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
    @ApiResponse(responseCode = "409", description = "Conflict") })
  public Application createApplication(
                                       HttpServletRequest request,
                                       @RequestBody
                                       ApplicationForm application) {
    try {
      return appCenterService.createApplication(application, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PutMapping
  @Secured("administrators")
  @Operation(summary = "Updates an existing application identified by its id or title or url", method = "GET", description = "Updates an existing application identified by its id or title or url")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void updateApplication(
                                HttpServletRequest request,
                                @RequestBody
                                ApplicationForm application) {
    try {
      application.setChangedManually(true);
      appCenterService.updateApplication(application, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping(path = "{applicationId}")
  @Secured("administrators")
  @Operation(summary = "Deletes an existing application identified by its id", method = "GET", description = "Deletes an existing application identified by its id")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
    @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void deleteApplication(
                                HttpServletRequest request,
                                @Parameter(description = "Application technical id to delete", required = true)
                                @PathVariable("applicationId")
                                Long applicationId) {
    try {
      appCenterService.deleteApplication(applicationId, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping(path = "/settings")
  @Secured("users")
  @Operation(summary = "Retrieves app-center global settings", method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public ApplicationCenterSettings getSettings() {
    return appCenterService.getSettings();
  }

  @PostMapping(path = "/settings")
  @Secured("administrators")
  @Operation(summary = "Saves app-center global settings", method = "POST")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation") })
  public void saveSettings(
                           HttpServletRequest request,
                           @RequestBody
                           ApplicationCenterSettings settings) {
    try {
      appCenterService.saveSettings(settings, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
  }

  @PostMapping(path = "/personal")
  @Secured("users")
  @Operation(summary = "Creates a personal URL application for the authenticated user", method = "POST")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "400", description = "Bad Request"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation") })
  public Application createPersonalApplication(
                                               HttpServletRequest request,
                                               @RequestBody
                                               ApplicationForm application) {
    try {
      return appCenterService.createPersonalApplication(application, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PutMapping(path = "/personal/{applicationId}")
  @Secured("users")
  @Operation(summary = "Updates a personal URL application owned by the authenticated user", method = "PUT")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
    @ApiResponse(responseCode = "404", description = "Not found") })
  public void updatePersonalApplication(
                                        HttpServletRequest request,
                                        @PathVariable("applicationId")
                                        Long applicationId,
                                        @RequestBody
                                        ApplicationForm application) {
    application.setId(applicationId);
    try {
      appCenterService.updatePersonalApplication(application, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping(path = "/personal/{applicationId}")
  @Secured("users")
  @Operation(summary = "Deletes a personal URL application owned by the authenticated user", method = "DELETE")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
    @ApiResponse(responseCode = "404", description = "Not found") })
  public void deletePersonalApplication(
                                        HttpServletRequest request,
                                        @PathVariable("applicationId")
                                        Long applicationId) {
    try {
      appCenterService.deletePersonalApplication(applicationId, request.getRemoteUser());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @GetMapping(path = "/illustration/{applicationId}")
  @Operation(summary = "Gets an application illustration by application id", method = "GET", description = "This can only be done by the logged in user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "500", description = "Internal server error"),
    @ApiResponse(responseCode = "400", description = "Invalid query input"),
    @ApiResponse(responseCode = "404", description = "Resource not found") })
  public ResponseEntity<InputStreamResource> getApplicationIllustration(
                                                                        HttpServletRequest request,
                                                                        @Parameter(description = "Application id", required = true)
                                                                        @PathVariable("applicationId")
                                                                        long applicationId,
                                                                        @Parameter(description = "A token that is used to authorize anonymous request")
                                                                        @RequestParam(name = "r", required = false)
                                                                        String token,
                                                                        @Parameter(description = "Dimensions of size")
                                                                        @RequestParam(name = "sizes", required = false)
                                                                        String dimensions,
                                                                        @Parameter(description = "Last modified parameter", required = false)
                                                                        @RequestParam(name = "v", required = false)
                                                                        Optional<Long> lastModified) {
    Application application = appCenterService.getApplication(applicationId);
    if (application == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } else {
      if (StringUtils.isBlank(request.getRemoteUser())) {
        if (!LinkProvider.isAttachmentTokenValid(token,
                                                 "appcenter",
                                                 String.valueOf(applicationId),
                                                 "icon",
                                                 String.valueOf(lastModified.orElse(0l)))) {
          LOG.warn("An anonymous user attempts to access avatar of user {} without a valid access token", applicationId);
          throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
      } else if (!appCenterService.canEdit(request.getRemoteUser())
                 && !appCenterService.canAccess(application, request.getRemoteUser())
                 && !LinkProvider.isAttachmentTokenValid(token,
                                                         "appCenter",
                                                         String.valueOf(applicationId),
                                                         "icon",
                                                         String.valueOf(lastModified.orElse(0l)))) {

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
      }
    }
    try {
      String mimeType = fileService.getFileInfo(application.getImageFileId()).getMimetype();
      InputStream stream = appCenterService.getApplicationImageInputStream(applicationId, dimensions);
      if (stream == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      Long lastUpdated = appCenterService.getApplicationImageLastUpdated(applicationId);
      BodyBuilder builder = ResponseEntity.ok();
      if (lastModified.isPresent()) {
        builder.lastModified(lastUpdated)
               .eTag(String.valueOf(Objects.hash(lastUpdated)))
               .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
      } else {
        builder.cacheControl(CacheControl.noStore());
      }
      return builder.contentType(MediaType.valueOf(mimeType))
                    .body(new InputStreamResource(stream));
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

}
