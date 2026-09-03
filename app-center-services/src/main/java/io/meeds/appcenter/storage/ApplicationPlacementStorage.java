/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.storage;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import io.meeds.appcenter.constant.PlacementSide;

@Component
public class ApplicationPlacementStorage {

  public static final Scope   PLACEMENT_SCOPE   = Scope.APPLICATION.id("AppCenterPlacement");

  public static final String  LEFT_PLACEMENT_KEY  = "stuck.left";

  public static final String  RIGHT_PLACEMENT_KEY = "stuck.right";

  @Autowired
  private SettingService      settingService;

  public Long getPlacedApplicationId(String username, PlacementSide side) {
    SettingValue<?> value = settingService.get(Context.USER.id(username), PLACEMENT_SCOPE, placementKey(side));
    if (value == null || value.getValue() == null) {
      return null;
    }
    String applicationId = value.getValue().toString();
    return NumberUtils.isParsable(applicationId) ? Long.valueOf(applicationId) : null;
  }

  public void setPlacedApplicationId(String username, PlacementSide side, long applicationId) {
    settingService.set(Context.USER.id(username),
                       PLACEMENT_SCOPE,
                       placementKey(side),
                       SettingValue.create(String.valueOf(applicationId)));
  }

  public void removePlacedApplicationId(String username, PlacementSide side) {
    settingService.remove(Context.USER.id(username), PLACEMENT_SCOPE, placementKey(side));
  }

  private String placementKey(PlacementSide side) {
    return side == PlacementSide.LEFT ? LEFT_PLACEMENT_KEY : RIGHT_PLACEMENT_KEY;
  }

}
