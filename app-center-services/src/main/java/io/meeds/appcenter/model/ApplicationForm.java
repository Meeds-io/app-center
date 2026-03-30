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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApplicationForm extends Application {

  @Exclude
  private String imageUploadId;

  @Exclude
  private String illustrationMimeType;

  public ApplicationForm(Application application) {
    super(application.getId(),
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
          application.getPermissions(),
          application.getCategoryIds(),
          application.getImageFileId(),
          application.getIcon(),
          application.getImageUrl(),
          application.getOrder(),
          application.isChangedManually());
  }

}
