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
import java.util.List;

import org.exoplatform.commons.utils.StringListConverter;

import io.meeds.appcenter.constant.ApplicationType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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

  @Column(name = "DESCRIPTION")
  private String                                description;

  @Column(name = "APP_TYPE")
  private ApplicationType                       type;

  @Column(name = "URL")
  private String                                url;

  @Column(name = "SAME_TAB")
  private boolean                               sameTab;

  @Column(name = "ICON")
  private String                                icon;

  @Column(name = "HELP_PAGE_URL")
  private String                                helpPageUrl;

  @Column(name = "IMAGE_FILE_ID")
  private Long                                  imageFileId;

  @Column(name = "ACTIVE")
  private boolean                               active;

  @Column(name = "IS_DEFAULT")
  private boolean                               isDefault;

  @Column(name = "BY_DEFAULT")
  private boolean                               isMandatory;

  @Column(name = "IS_MOBILE")
  private boolean                               isMobile;

  @Column(name = "IS_SYSTEM")
  private boolean                               system;

  @Column(name = "IS_PWA")
  private boolean                               pwa;

  @Column(name = "SHORTCUT")
  private String                                shortcut;

  @Convert(converter = StringListConverter.class)
  @Column(name = "PERMISSIONS")
  private List<String>                          permissions;

  @Column(name = "IS_CHANGED_MANUALLY")
  private boolean                               isChangedManually;

  @Column(name = "APPLICATION_ORDER", nullable = true)
  private Long                                  order;

  @Column(name = "IS_PERSONAL")
  private boolean                               personal;

  @Column(name = "OWNER_IDENTITY_ID")
  private Long                                  ownerIdentityId;

  @lombok.ToString.Exclude
  @lombok.EqualsAndHashCode.Exclude
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.REMOVE)
  private Collection<FavoriteApplicationEntity> favorites;

}
