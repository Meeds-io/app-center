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
package io.meeds.appcenter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds the global app-center configuration. This POJO is serialized as a single
 * JSON blob and persisted under one SettingService key
 * ({@code ApplicationCenterService.APP_CENTER_SETTINGS_KEY}), so new settings can
 * be added here as plain fields without introducing a new setting key nor a data
 * upgrade plugin. It doubles as the REST DTO exposed by the settings endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCenterSettings {

  /** Whether end users are allowed to add their own personal URL applications. */
  private boolean allowUserPersonalApps;

}
