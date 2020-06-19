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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ayoub Zayati
 */
public class Application implements Serializable {

  private static final long serialVersionUID = -3486306198744219949L;

  private Long              id;

  private String            title;

  private String            url;

  private String            helpPageURL;

  private String            description;

  private boolean           active;

  private boolean           byDefault;

  private boolean           system;

  private List<String>      permissions;

  private String            imageFileBody;

  private String            imageFileName;

  private Long              imageFileId;

  private Long              order;

  public Application() {
  }

  public Application(Long id,
                     String title,
                     String url,
                     Long imageFileId,
                     String imageFileBody,
                     String imageFileName,
                     String description,
                     boolean active,
                     boolean byDefault,
                     String... permissions) {
    this(id,
         title,
         url,
         imageFileId,
         imageFileBody,
         imageFileName,
         description,
         active,
         byDefault,
         permissions == null ? null : Arrays.asList(permissions));
  }

  public Application(Long id,
                     String title,
                     String url,
                     Long imageFileId,
                     String imageFileBody,
                     String imageFileName,
                     String description,
                     boolean active,
                     boolean byDefault,
                     List<String> permissions) {
    this.id = id;
    this.title = title;
    this.url = url;
    this.description = description;
    this.active = active;
    this.byDefault = byDefault;
    this.permissions = permissions;
    this.imageFileId = imageFileId;
    this.imageFileBody = imageFileBody;
    this.imageFileName = imageFileName;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHelpPageURL() {
    return helpPageURL;
  }

  public void setHelpPageURL(String helpPageURL) {
    this.helpPageURL = helpPageURL;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isByDefault() {
    return byDefault;
  }

  public void setByDefault(boolean byDefault) {
    this.byDefault = byDefault;
  }

  public List<String> getPermissions() {
    return permissions;
  }

  public void setPermissions(String... permissions) {
    this.permissions = Arrays.asList(permissions);
  }

  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }

  public String getImageFileBody() {
    return imageFileBody;
  }

  public void setImageFileBody(String imageFileBody) {
    this.imageFileBody = imageFileBody;
  }

  public String getImageFileName() {
    return imageFileName;
  }

  public void setImageFileName(String imageFileName) {
    this.imageFileName = imageFileName;
  }

  public Long getImageFileId() {
    return imageFileId;
  }

  public void setImageFileId(Long imageFileId) {
    this.imageFileId = imageFileId;
  }

  public boolean isSystem() {
    return system;
  }

  public void setSystem(boolean system) {
    this.system = system;
  }

  public Long getOrder() {
    return order;
  }

  public void setOrder(Long order) {
    this.order = order;
  }

  @Override
  public String toString() {
    return "[Application: title = '" + title + "', url = '" + url + "']";
  }
}
