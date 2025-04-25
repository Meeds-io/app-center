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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import io.meeds.appcenter.entity.FavoriteApplicationEntity;

public interface FavoriteApplicationDAO extends JpaRepository<FavoriteApplicationEntity, Long> {

  @Query("""
      SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp
      WHERE favoriteApp.application.id = ?1
      AND favoriteApp.userName = ?2
      """)
  FavoriteApplicationEntity getFavoriteAppByUserNameAndAppId(Long applicationId, String userName);

  @Query("""
      SELECT count(*) FROM FavoriteApplicationEntity favoriteApp
      WHERE favoriteApp.userName = ?1
      AND favoriteApp.favorite = TRUE
      """)
  long countFavoritesForUser(String username);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
         DELETE FROM FavoriteApplicationEntity favoriteApp
         WHERE favoriteApp.application.id = ?1
         """)
  void removeAllFavoritesOfApplication(Long applicationId);
}
