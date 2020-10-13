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
package org.exoplatform.appcenter.entity;

import java.util.Collection;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author Ayoub Zayati
 */
@Entity(name = "ApplicationEntity")
@ExoEntity
@Table(name = "AC_APPLICATION")
@NamedQueries({
    @NamedQuery(name = "ApplicationEntity.getAppByTitle", query = "SELECT app FROM ApplicationEntity app "
        + "WHERE app.title = :title"),
    @NamedQuery(name = "ApplicationEntity.getApplicationsByKeyword", query = "SELECT app FROM ApplicationEntity app "
        + "WHERE LOWER(app.title) like :title OR LOWER(app.description) like :description OR LOWER(app.url) like :url"),
    @NamedQuery(name = "ApplicationEntity.getApplications", query = "SELECT app FROM ApplicationEntity app"),
    @NamedQuery(name = "ApplicationEntity.getSystemApplications", query = "SELECT app FROM ApplicationEntity app WHERE app.system = TRUE"),
    @NamedQuery(name = "ApplicationEntity.getMandatoryActiveApps", query = "SELECT app FROM ApplicationEntity app "
        + " WHERE app.active = TRUE AND app.isMandatory = TRUE "), })
public class ApplicationEntity {

  @Id
  @SequenceGenerator(name = "SEQ_APPLICATION_ID", sequenceName = "SEQ_APPLICATION_ID")
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
  private Boolean                               system;

  @Column(name = "PERMISSIONS")
  private String                                permissions;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "application", cascade = CascadeType.REMOVE)
  private Collection<FavoriteApplicationEntity> favorites;

  public ApplicationEntity() {
  }

  public ApplicationEntity(Long id,
                           String title,
                           String url,
                           Long imageFileId,
                           String description,
                           boolean active,
                           boolean isMandatory,
                           String permissions) {
    this.id = id;
    this.title = title;
    this.url = url;
    this.imageFileId = imageFileId;
    this.description = description;
    this.active = active;
    this.isMandatory = isMandatory;
    this.permissions = permissions;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the help page url
   */
  public String getHelpPageUrl() {
    return helpPageUrl;
  }

  /**
   * @param helpPageUrl the help page url to set
   */
  public void setHelpPageUrl(String helpPageUrl) {
    this.helpPageUrl = helpPageUrl;
  }

  /**
   * @return the imageFileId
   */
  public Long getImageFileId() {
    return imageFileId;
  }

  /**
   * @param imageFileId the imageFileId to set
   */
  public void setImageFileId(Long imageFileId) {
    this.imageFileId = imageFileId;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active the active to set
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * @return the isMandatory
   */
  public boolean isMandatory() {
    return isMandatory;
  }

  /**
   * @param mandatory the isMandatory to set
   */
  public void setMandatory(boolean mandatory) {
    this.isMandatory = mandatory;
  }

  /**
   * @return the isMobile
   */
  public boolean isMobile() {
    return isMobile;
  }

  /**
   * @param isMobile the isMobile to set
   */
  public void setIsMobile(boolean isMobile) {
    this.isMobile = isMobile;
  }

  /**
   * @return the permissions
   */
  public String getPermissions() {
    return permissions;
  }

  /**
   * @param permissions the permissions to set
   */
  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }

  public boolean isSystem() {
    return system != null && system;
  }

  public void setSystem(boolean system) {
    this.system = system;
  }

}
