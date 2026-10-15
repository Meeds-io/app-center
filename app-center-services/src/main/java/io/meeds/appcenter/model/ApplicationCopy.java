/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.model;

import java.util.List;
import java.util.Map;

import io.meeds.appcenter.constant.ApplicationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An application a Shortcuts portlet window created, carried by value in the
 * preferences of a copy of that window: the fields its form offers, its
 * labels in every language and its illustration's content. The copy's first
 * render creates a new application from it, every field validated again.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCopy {

  /** The source application id, which the copy's applicationIds still lists. */
  private long                id;

  private String              title;

  private String              description;

  /** Title labels, by language tag. */
  private Map<String, String> titles;

  /** Description labels, by language tag. */
  private Map<String, String> descriptions;

  private ApplicationType     type;

  private String              url;

  private String              helpPageURL;

  private String              icon;

  private String              badgeName;

  private boolean             sameTab;

  private boolean             mobile;

  private boolean             allowDetach;

  /** The illustration's content, base64-encoded, null when none is copied. */
  private String              illustration;

  /** The ids of the source application's categories. */
  private List<Long>          categoryIds;

  /**
   * Whether an administrator emptied the source application's audience: the
   * copy keeps it empty, shown to administrators only.
   */
  private boolean             hidden;

}
