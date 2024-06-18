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

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {

  private Long         id;

  private String       title;

  private String       url;

  private String       helpPageURL;

  private String       description;

  private boolean      active;

  private boolean      isMandatory;

  private boolean      isMobile;

  private boolean      system;

  private List<String> permissions;

  private String       imageFileBody;

  private String       imageFileName;

  private Long         imageFileId;

  private Long         imageLastModified;

  private Long         order;

  private boolean      isChangedManually;

  public Application(Long id, // NOSONAR
                     String title,
                     String url,
                     String helpPageURL,
                     Long imageFileId,
                     Long imageLastModified,
                     String imageFileBody,
                     String imageFileName,
                     String description,
                     boolean isSystem,
                     boolean active,
                     boolean isMandatory,
                     boolean isMobile,
                     boolean isChangedManually,
                     String... permissions) {
    this(id,
         title,
         url,
         helpPageURL,
         imageFileId,
         imageLastModified,
         imageFileBody,
         imageFileName,
         description,
         isSystem,
         active,
         isMandatory,
         isMobile,
         isChangedManually,
         permissions == null ? null : Arrays.asList(permissions));
  }

  public Application(Long id, // NOSONAR
                     String title,
                     String url,
                     String helpPageURL,
                     Long imageFileId,
                     Long imageLastModified,
                     String imageFileBody,
                     String imageFileName,
                     String description,
                     boolean isSystem,
                     boolean active,
                     boolean isMandatory,
                     boolean isMobile,
                     boolean isChangedManually,
                     List<String> permissions) {
    this.id = id;
    this.title = title;
    this.url = url;
    this.helpPageURL = helpPageURL;
    this.description = description;
    this.active = active;
    this.isMandatory = isMandatory;
    this.isMobile = isMobile;
    this.system = isSystem;
    this.permissions = permissions;
    this.imageFileId = imageFileId;
    this.imageLastModified = imageLastModified;
    this.imageFileBody = imageFileBody;
    this.imageFileName = imageFileName;
    this.isChangedManually = isChangedManually;
  }
}
