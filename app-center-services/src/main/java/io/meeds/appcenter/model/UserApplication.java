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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserApplication extends Application {

  private boolean favorite;

  public UserApplication(Application app) {
    super(app.getId(),
          app.getTitle(),
          app.getUrl(),
          app.getHelpPageURL(),
          app.getImageFileId(),
          app.getImageLastModified(),
          app.getImageFileBody(),
          app.getImageFileName(),
          app.getDescription(),
          app.isSystem(),
          app.isActive(),
          app.isMandatory(),
          app.isMobile(),
          app.isChangedManually(),
          app.getPermissions());
  }

  public UserApplication(Long id, // NOSONAR
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
                         boolean favorite,
                         boolean isChangedManually,
                         String... permissions) {
    super(id,
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
          permissions);
    this.favorite = favorite;
  }

}
