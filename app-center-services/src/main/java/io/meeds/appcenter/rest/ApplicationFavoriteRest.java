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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("favorites")
@Tag(name = "/app-center/rest/favorites", description = "Manage and access application center applications") // NOSONAR
public class ApplicationFavoriteRest {

  @Autowired
  private ApplicationCenterService appCenterService;

  @GetMapping
  @Secured("users")
  @Operation(
             summary = "Retrieves favorite applications for currently authenticated user",
             method = "GET",
             description = "Return list of applications in json format")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public ApplicationList getFavoriteApplicationsList(HttpServletRequest request, Pageable pageable) {
    return appCenterService.getMandatoryAndFavoriteApplications(request.getRemoteUser(), pageable);
  }

  @PostMapping(path = "{applicationId}")
  @Secured("users")
  @Operation(
             summary = "Adds an existing application identified by its id as favorite for current authenticated user",
             method = "POST",
             description = "Adds an existing application identified by its id as favorite for current authenticated user")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void addFavoriteApplication(
                                     HttpServletRequest request,
                                     @Parameter(description = "Application technical id to add as favorite", required = true)
                                     @PathVariable("applicationId")
                                     Long applicationId) {
    try {
      appCenterService.addFavoriteApplication(applicationId, request.getRemoteUser());
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured("users")
  @Operation(
             summary = "Updates an existing application's order identified by its id",
             method = "PUT",
             description = "Updates an existing application's order identified by its id and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "401", description = "Unauthorized operation"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void updateApplicationsOrder(
                                      HttpServletRequest request,
                                      @RequestBody(required = true)
                                      List<ApplicationOrder> applicationOrders) {
    try {
      for (ApplicationOrder applicationOrder : applicationOrders) {
        appCenterService.updateFavoriteApplicationOrder(applicationOrder, request.getRemoteUser());
      }
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @DeleteMapping(path = "{applicationId}")
  @Secured("users")
  @Operation(
             summary = "Deletes an existing application identified by its id from current authenticated user favorites",
             method = "GET",
             description = "Deletes an existing application identified by its id from current authenticated user favorites and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void deleteFavoriteApplication(
                                        HttpServletRequest request,
                                        @Parameter(description = "Application technical id to delete from favorite",
                                                   required = true)
                                        @PathVariable("applicationId")
                                        Long applicationId) {
    appCenterService.deleteFavoriteApplication(applicationId, request.getRemoteUser());
  }

}
