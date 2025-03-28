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
package io.meeds.appcenter.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.entity.ApplicationEntity;

@Component
public interface ApplicationDAO extends JpaRepository<ApplicationEntity, Long> {

  @Query("""
      SELECT app FROM ApplicationEntity app
      WHERE app.active = TRUE
      AND app.isMandatory = TRUE
      """)
  List<ApplicationEntity> getMandatoryActiveApps();

  @Query("""
      SELECT app FROM ApplicationEntity app
      """)
  List<ApplicationEntity> getApplications(Sort sort);
  default List<ApplicationEntity> getApplications() {
    return getApplications(Sort.by(Sort.Order.asc("title").ignoreCase()));
  }

  @Query("""
      SELECT app FROM ApplicationEntity app
      WHERE LOWER(app.title) LIKE %?1%
      OR LOWER(app.description) like %?1%
      OR LOWER(app.url) LIKE %?1%
      ORDER BY LOWER(app.title)
      """)
  List<ApplicationEntity> getApplications(String keyword, Sort sort) ;
  default List<ApplicationEntity> getApplications(String keyword) {
    return getApplications(keyword, Sort.by(Sort.Order.asc("title").ignoreCase()));
  }

  @Query("""
      SELECT app FROM ApplicationEntity app
      WHERE app.system = TRUE
      """)
  List<ApplicationEntity> getSystemApplications();

  @Query("""
      SELECT app FROM ApplicationEntity app
      WHERE app.title = ?1
      """)
  ApplicationEntity getApplicationByTitle(String title);

  default List<ApplicationEntity> findAll() {
    return findAll(Sort.by(Sort.Order.asc("title").ignoreCase()));
  }

  @Query("""
      SELECT app
      FROM ApplicationEntity app
      LEFT JOIN FavoriteApplicationEntity favoriteApp ON app.id = favoriteApp.application.id
      WHERE (favoriteApp.userName = :userName)
         OR (app.active = TRUE AND app.isMandatory = TRUE)
      ORDER BY favoriteApp.order NULLS LAST, app.isMandatory DESC
      """)
  Page<ApplicationEntity> findFavoriteAndMandatoryApplications(@Param("userName") String userName, Pageable pageable);
}
