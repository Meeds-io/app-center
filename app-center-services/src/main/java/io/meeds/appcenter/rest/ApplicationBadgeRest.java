/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.appcenter.model.ApplicationBadgeProvider;
import io.meeds.appcenter.service.ApplicationBadgeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("badges")
@Tag(name = "/app-center/rest/badges", description = "Access unread counters displayed on application tiles") // NOSONAR
public class ApplicationBadgeRest {

  @Autowired
  private ApplicationBadgeService badgeService;

  @GetMapping(path = "{badgeName}")
  @Secured("users")
  @Operation(
             summary = "Retrieves the badge count of an application for the currently authenticated user",
             method = "GET",
             description = "Returns the number of items to display on the application tile")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "403", description = "Forbidden"),
                          @ApiResponse(responseCode = "404", description = "Badge not found"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public long getBadge(
                       HttpServletRequest request,
                       @Parameter(description = "Badge technical name", required = true)
                       @PathVariable("badgeName")
                       String badgeName) {
    try {
      return badgeService.getBadge(badgeName, request.getRemoteUser());
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    }
  }

  @GetMapping(path = "providers")
  @Secured("administrators")
  @Operation(
             summary = "Lists the badge providers contributed by installed addons",
             method = "GET",
             description = "Returns the names an administrator can bind to an application")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "401", description = "Unauthorized"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public List<ApplicationBadgeProvider> getBadgeProviders() {
    return badgeService.getBadgeProviders();
  }

}
