/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.appcenter.dto;

public class UserApplication extends Application {

  private static final long serialVersionUID = -2451635329141517005L;

  private boolean           favorite;

  public UserApplication(Application app) {
    super(app.getId(),
          app.getTitle(),
          app.getUrl(),
          app.getHelpPageURL(),
          app.getImageFileId(),
          app.getImageFileBody(),
          app.getImageFileName(),
          app.getDescription(),
          app.isActive(),
          app.isMandatory(),
          app.isMobile(),
          app.getPermissions());
  }

  public UserApplication(Long id,
                         String title,
                         String url,
                         String helpPageURL,
                         Long imageFileId,
                         String imageFileBody,
                         String imageFileName,
                         String description,
                         boolean active,
                         boolean isMandatory,
                         boolean isMobile,
                         boolean favorite,
                         String... permissions) {
    super(id,
          title,
          url,
          helpPageURL,
          imageFileId,
          imageFileBody,
          imageFileName,
          description,
          active,
          isMandatory,
          isMobile,
          permissions);
    this.favorite = favorite;
  }

  public boolean isFavorite() {
    return favorite;
  }

  public void setFavorite(boolean favorite) {
    this.favorite = favorite;
  }
}
