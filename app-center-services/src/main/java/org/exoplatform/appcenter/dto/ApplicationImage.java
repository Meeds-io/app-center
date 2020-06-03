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

/**
 * @author Ayoub Zayati
 */
public class ApplicationImage implements Serializable {

  private static final long serialVersionUID = -2001197615359890089L;

  private Long              id;

  private String            fileBody;

  private String            fileName;

  private Long              lastUpdated;

  public ApplicationImage() {
    this(null, null, null);
  }

  public ApplicationImage(String fileName, String fileBody) {
    this(null, fileName, fileBody);
  }

  public ApplicationImage(Long id, String fileName, String fileBody) {
    this.id = id;
    this.fileName = fileName;
    this.fileBody = fileBody;
  }

  /**
   * @return the fileBody
   */
  public String getFileBody() {
    return fileBody;
  }

  /**
   * @param fileBody the fileBody to set
   */
  public void setFileBody(String fileBody) {
    this.fileBody = fileBody;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @param fileName the fileName to set
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
