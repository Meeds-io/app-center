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

import static io.meeds.appcenter.rest.util.RestUtils.privateAnswer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("pins")
@Tag(name = "/app-center/rest/pins", description = "Manage the applications pinned to the topbar of the authenticated user") // NOSONAR
public class ApplicationPinRest {

  @Autowired
  private ApplicationCenterService appCenterService;

  @GetMapping
  @Secured("users")
  @Operation(summary = "Retrieves the ids of the applications pinned to the topbar of the authenticated user", method = "GET", description = "Returns the application ids in pin order")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public ResponseEntity<List<Long>> getPinnedApplicationIds(HttpServletRequest request) {
    return privateAnswer(appCenterService.getPinnedApplicationIds(request.getRemoteUser()));
  }

  @PutMapping(path = "{applicationId}")
  @Secured("users")
  @Operation(summary = "Pins an application to the topbar of the authenticated user", method = "PUT", description = "Pins an active application the authenticated user can access, after the applications already pinned, up to 100 pinned applications")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
    @ApiResponse(responseCode = "400", description = "Invalid application id, inactive application (appCenter.pin.pinNotAllowed), or an application not pinned yet while 100 applications are already pinned (appCenter.pin.tooManyPins)"),
    @ApiResponse(responseCode = "403", description = "The authenticated user can't access the application"),
    @ApiResponse(responseCode = "404", description = "Application not found") })
  public void pinApplication(
                             HttpServletRequest request,
                             @Parameter(description = "Application technical id to pin", required = true)
                             @PathVariable("applicationId")
                             long applicationId) {
    try {
      appCenterService.pinApplication(applicationId, request.getRemoteUser());
    } catch (ApplicationNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @DeleteMapping(path = "{applicationId}")
  @Secured("users")
  @Operation(summary = "Unpins an application from the topbar of the authenticated user", method = "DELETE", description = "Unpins the application, if pinned")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled") })
  public void unpinApplication(
                               HttpServletRequest request,
                               @Parameter(description = "Application technical id to unpin", required = true)
                               @PathVariable("applicationId")
                               long applicationId) {
    appCenterService.unpinApplication(applicationId, request.getRemoteUser());
  }

}
