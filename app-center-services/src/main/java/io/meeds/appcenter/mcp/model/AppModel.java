/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.appcenter.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A thin representation of an App-Center application returned to the AI agent
 * (EVA). It exposes only the fields relevant to a user managing their own apps
 * and never the internal permission/category wiring of the raw entity.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppModel {

  @JsonProperty("app_id")
  private Long    id;

  private String  title;

  private String  url;

  private String  icon;

  @JsonProperty("image_file_id")
  private Long    imageFileId;

  /** Application type: LINK, DRAWER or PORTLET. */
  private String  type;

  /** Whether this app is one of the current user's favorites. */
  private boolean favorite;

  /** Whether this app is mandatory (always shown, cannot be removed). */
  private boolean mandatory;

  /** Whether this is a personal URL app created by (and owned by) the user. */
  private boolean personal;

}
