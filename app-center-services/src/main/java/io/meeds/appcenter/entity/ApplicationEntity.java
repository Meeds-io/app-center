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
package io.meeds.appcenter.entity;

import java.util.Collection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "ApplicationEntity")
@Table(name = "AC_APPLICATION")
public class ApplicationEntity {

  @Id
  @SequenceGenerator(name = "SEQ_APPLICATION_ID", sequenceName = "SEQ_APPLICATION_ID", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_APPLICATION_ID")
  @Column(name = "ID")
  private Long                                  id;

  @Column(name = "TITLE")
  private String                                title;

  @Column(name = "URL")
  private String                                url;

  @Column(name = "HELP_PAGE_URL")
  private String                                helpPageUrl;

  @Column(name = "IMAGE_FILE_ID")
  private Long                                  imageFileId;

  @Column(name = "DESCRIPTION")
  private String                                description;

  @Column(name = "ACTIVE")
  private boolean                               active;

  @Column(name = "BY_DEFAULT")
  private boolean                               isMandatory;

  @Column(name = "IS_MOBILE")
  private boolean                               isMobile;

  @Column(name = "IS_SYSTEM")
  private boolean                               system;

  @Column(name = "PERMISSIONS")
  private String                                permissions;

  @Column(name = "IS_CHANGED_MANUALLY")
  private boolean                               isChangedManually;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.REMOVE)
  private Collection<FavoriteApplicationEntity> favorites;

  public ApplicationEntity(Long id, // NOSONAR
                           String title,
                           String url,
                           Long imageFileId,
                           String description,
                           boolean active,
                           boolean isMandatory,
                           String permissions,
                           boolean isChangedManually) {
    this.id = id;
    this.title = title;
    this.url = url;
    this.imageFileId = imageFileId;
    this.description = description;
    this.active = active;
    this.isMandatory = isMandatory;
    this.permissions = permissions;
    this.isChangedManually = isChangedManually;
  }

}
