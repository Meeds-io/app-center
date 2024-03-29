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
package org.exoplatform.appcenter.dao;

import java.util.List;

import jakarta.persistence.TypedQuery;

import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author Ayoub Zayati
 */
public class FavoriteApplicationDAO extends GenericDAOJPAImpl<FavoriteApplicationEntity, Long> {
  private static final Log LOG = ExoLogger.getLogger(FavoriteApplicationDAO.class);

  public List<FavoriteApplicationEntity> getFavoriteAppsByUser(String userName) {
    return getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppsByUser", FavoriteApplicationEntity.class)
                             .setParameter("userName", userName)
                             .getResultList();
  }

  public FavoriteApplicationEntity getFavoriteAppByUserNameAndAppId(Long applicationId, String userName) {
    TypedQuery<FavoriteApplicationEntity> query =
                                                getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppByUserNameAndAppId",
                                                                                    FavoriteApplicationEntity.class)
                                                                  .setParameter("applicationId", applicationId)
                                                                  .setParameter("userName", userName);
    List<FavoriteApplicationEntity> result = query.getResultList();
    if (result == null || result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("More than one application was found with applicationId '{}' and userName '{}'", applicationId, userName);
      return result.get(0);
    } else {
      return result.get(0);
    }
  }

  public long countFavoritesForUser(String username) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("FavoriteApplicationEntity.countFavoritesByUser", Long.class);
    query.setParameter("userName", username);
    return query.getSingleResult();
  }

  public void removeAllFavoritesOfApplication(Long applicationId) {
    getEntityManager().getTransaction().begin();
    getEntityManager().createQuery("DELETE FROM FavoriteApplicationEntity favoriteApp WHERE favoriteApp.application.id = :applicationId ")
                      .setParameter("applicationId", applicationId)
                      .executeUpdate();
    getEntityManager().getTransaction().commit();
  }
}
