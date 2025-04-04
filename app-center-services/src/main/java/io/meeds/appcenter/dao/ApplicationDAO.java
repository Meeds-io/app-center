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
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;

@Component
public interface ApplicationDAO extends JpaRepository<ApplicationEntity, Long> {

  @Query("""
      SELECT app.id FROM ApplicationEntity app
      WHERE app.active = TRUE
      AND app.isMandatory = TRUE
      """)
  List<Long> getMandatoryActiveApplicationIds();

  Stream<ApplicationEntity> findByIsSystemAndUrl(String url);

  @Query("""
      SELECT app.id FROM ApplicationEntity app
      """)
  List<Long> getApplicationIds(Sort sort);

  default List<Long> getApplicationIds() {
    return getApplicationIds(Sort.by(Sort.Order.asc("title").ignoreCase()));
  }

  @Query("""
      SELECT app.id FROM ApplicationEntity app
      WHERE LOWER(app.title) LIKE %?1%
      OR LOWER(app.description) like %?1%
      OR LOWER(app.url) LIKE %?1%
      ORDER BY LOWER(app.title)
      """)
  List<Long> getApplicationIds(String keyword, Sort sort);

  default List<Long> getApplicationIds(String keyword) {
    return getApplicationIds(keyword, Sort.by(Sort.Order.asc("title").ignoreCase()));
  }

  @Query("""
      SELECT app.id FROM ApplicationEntity app
      WHERE app.system = TRUE
      """)
  List<Long> getSystemApplicationIds();

  @Query("""
      SELECT new FavoriteApplicationEntity(favoriteApp.id, app, favoriteApp.userName, favoriteApp.order)
      FROM ApplicationEntity app
      LEFT JOIN FavoriteApplicationEntity favoriteApp \
            ON app.id = favoriteApp.application.id AND favoriteApp.userName = :userName
      WHERE app.active = TRUE AND (favoriteApp.id IS NOT NULL OR app.isMandatory = TRUE)
      ORDER BY favoriteApp.order NULLS LAST, app.isMandatory DESC
      """)
  Page<FavoriteApplicationEntity> findFavoriteAndMandatoryApplications(@Param("userName")
  String userName, Pageable pageable);

}
