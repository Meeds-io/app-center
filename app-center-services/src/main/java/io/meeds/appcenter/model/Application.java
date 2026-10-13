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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

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

  /**
   * Identifier of the badge plugin bound to this application. Null when the
   * binding is derived from the url, or when the application carries no badge.
   */
  private String          badgeName;

  private boolean         allowStick;

  private boolean         allowDetach;

  /**
   * CMS setting name of the Shortcuts portlet window that created the
   * application, the only list showing it; null for an application of the
   * Application Center. Set on creation by the server, never changed.
   */
  private String          contextSettingName;

  /**
   * Space of the context the application was created in, 0 for the platform
   * context, null for an application of the Application Center.
   */
  private Long            contextSpaceId;

  /**
   * Copies every field, the permissions and category ids into new lists, so
   * that decorating or changing the copy never writes into the instance the
   * app-center.application cache serves.
   *
   * @param application the application to copy
   */
  public Application(Application application) {
    this(application.getId(),
         application.getTitle(),
         application.getUrl(),
         application.isSameTab(),
         application.getHelpPageURL(),
         application.getDescription(),
         application.getShortcut(),
         application.getType(),
         application.isActive(),
         application.isMandatory(),
         application.isDefault(),
         application.isMobile(),
         application.isSystem(),
         application.isPwa(),
         application.getPermissions() == null ? null : new ArrayList<>(application.getPermissions()),
         application.getCategoryIds() == null ? null : new ArrayList<>(application.getCategoryIds()),
         application.getImageFileId(),
         application.getIcon(),
         application.getImageUrl(),
         application.getOrder(),
         application.isChangedManually(),
         application.isPersonal(),
         application.getBadgeName(),
         application.isAllowStick(),
         application.isAllowDetach(),
         application.getContextSettingName(),
         application.getContextSpaceId());
  }

  /**
   * @return true for an application created from a Shortcuts portlet window,
   *         listed only by that window and never in the users' Application
   *         Center; read only, never taken from a request body
   */
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public boolean isContextual() {
    return StringUtils.isNotBlank(contextSettingName);
  }

}
