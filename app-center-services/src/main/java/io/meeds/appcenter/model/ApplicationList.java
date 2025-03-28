/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
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

import java.util.List;

public class ApplicationList {

  private List<Application> applications;
  private long offset;
  private long limit;
  private long size;
  private boolean canAddFavorite;

  // Fluent API style setters (return `this` for method chaining)
  public List<Application> getApplications() {
    return applications;
  }

  public ApplicationList setApplications(List<Application> applications) {
    this.applications = applications;
    return this;
  }

  public boolean isCanAddFavorite() {
    return canAddFavorite;
  }

  public ApplicationList setCanAddFavorite(boolean canAddFavorite) {
    this.canAddFavorite = canAddFavorite;
    return this;
  }

  public long getOffset() {
    return offset;
  }

  public ApplicationList setOffset(long offset) {
    this.offset = offset;
    return this;
  }

  public long getLimit() {
    return limit;
  }

  public ApplicationList setLimit(long limit) {
    this.limit = limit;
    return this;
  }

  public long getSize() {
    return size;
  }

  public ApplicationList setSize(long size) {
    this.size = size;
    return this;
  }
}
