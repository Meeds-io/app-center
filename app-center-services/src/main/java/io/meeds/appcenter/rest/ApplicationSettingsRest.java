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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.meeds.appcenter.model.GeneralSettings;
import io.meeds.appcenter.service.ApplicationCenterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("settings")
@Tag(name = "/app-center/rest/settings", description = "Manage and access application center applications") // NOSONAR
public class ApplicationSettingsRest {

  @Autowired
  private ApplicationCenterService appCenterService;

  @GetMapping
  @Secured("users")
  @Operation(
             summary = "Modifies default application image setting",
             method = "GET",
             description = "Modifies default application image setting and returns an empty response")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public GeneralSettings getSettings() {
    return appCenterService.getSettings();
  }

  @PatchMapping(path = "maxFavorites")
  @Secured("administrators")
  @Operation(
             summary = "Modifies maximum application count to add as favorites for all users",
             method = "PATCH",
             description = "Modifies maximum application count to add as favorites for all users")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
                          @ApiResponse(responseCode = "500", description = "Internal server error") })
  public void setMaxFavoriteApps(
                                 @Parameter(description = "Max favorites number", required = true)
                                 @RequestParam("number")
                                 long number) {
    appCenterService.setMaxFavoriteApps(number);
  }

}
