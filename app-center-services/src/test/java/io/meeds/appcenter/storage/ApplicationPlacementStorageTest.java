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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

import io.meeds.appcenter.constant.PlacementSide;

@ExtendWith(MockitoExtension.class)
class ApplicationPlacementStorageTest {

  private static final String         USERNAME = "testuser";

  private static final long           APP_ID   = 42L;

  @Mock
  private SettingService              settingService;

  @InjectMocks
  private ApplicationPlacementStorage placementStorage;

  @Test
  void getPlacedApplicationIdWhenUnset() {
    assertNull(placementStorage.getPlacedApplicationId(USERNAME, PlacementSide.LEFT));
  }

  @Test
  void getPlacedApplicationIdWhenSet() {
    when(settingService.get(any(Context.class), any(Scope.class), eq(ApplicationPlacementStorage.RIGHT_PLACEMENT_KEY)))
                       .thenReturn((SettingValue) SettingValue.create(String.valueOf(APP_ID)));
    assertEquals(APP_ID, placementStorage.getPlacedApplicationId(USERNAME, PlacementSide.RIGHT));
  }

  @Test
  void getPlacedApplicationIdWhenUnparsable() {
    when(settingService.get(any(Context.class), any(Scope.class), anyString()))
                       .thenReturn((SettingValue) SettingValue.create("junk"));
    assertNull(placementStorage.getPlacedApplicationId(USERNAME, PlacementSide.LEFT));
  }

  @Test
  void setPlacedApplicationId() {
    placementStorage.setPlacedApplicationId(USERNAME, PlacementSide.LEFT, APP_ID);

    ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> scope = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<SettingValue> value = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(context.capture(),
                               scope.capture(),
                               eq(ApplicationPlacementStorage.LEFT_PLACEMENT_KEY),
                               value.capture());
    assertEquals(USERNAME, context.getValue().getId());
    assertEquals("AppCenterPlacement", scope.getValue().getId());
    assertEquals(String.valueOf(APP_ID), value.getValue().getValue());
  }

  @Test
  void removePlacedApplicationId() {
    placementStorage.removePlacedApplicationId(USERNAME, PlacementSide.RIGHT);

    ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
    verify(settingService).remove(context.capture(),
                                  any(Scope.class),
                                  eq(ApplicationPlacementStorage.RIGHT_PLACEMENT_KEY));
    assertEquals(USERNAME, context.getValue().getId());
  }

}
