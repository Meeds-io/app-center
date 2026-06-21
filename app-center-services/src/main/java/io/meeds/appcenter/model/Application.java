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

import java.util.List;

import io.meeds.appcenter.constant.ApplicationType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode.Exclude;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

  private Long            id;

  private String          title;

  private String          url;

  private boolean         sameTab;

  private String          helpPageURL;

  private String          description;

  private String          shortcut;

  private ApplicationType type;

  private boolean         active;

  private boolean         isMandatory;

  private boolean         isDefault;

  private boolean         isMobile;

  private boolean         system;

  private boolean         pwa;

  private List<String>    permissions;

  @Exclude
  private List<Long>      categoryIds;

  private Long            imageFileId;

  private String          icon;

  @Exclude
  private String          imageUrl;

  private Long            order;

  private boolean         isChangedManually;

  private boolean         personal;

  private Long            ownerIdentityId;

}
