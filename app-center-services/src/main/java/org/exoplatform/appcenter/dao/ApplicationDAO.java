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

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author Ayoub Zayati
 */
public class ApplicationDAO extends GenericDAOJPAImpl<ApplicationEntity, Long> {
  private static final Log LOG = ExoLogger.getLogger(ApplicationDAO.class);

  public List<ApplicationEntity> getMandatoryActiveApps() {
    return getEntityManager().createNamedQuery("ApplicationEntity.getMandatoryActiveApps", ApplicationEntity.class)
                             .getResultList();
  }

  public List<ApplicationEntity> getApplications(String keyword) {
    TypedQuery<ApplicationEntity> query = null;
    if (StringUtils.isBlank(keyword)) {
      query = getEntityManager().createNamedQuery("ApplicationEntity.getApplications", ApplicationEntity.class);
    } else {
      query = getEntityManager().createNamedQuery("ApplicationEntity.getApplicationsByKeyword", ApplicationEntity.class);
      keyword = keyword.toLowerCase();
      keyword = "%" + keyword.replaceAll("%", "").replaceAll("\\*", "%") + "%";
      query.setParameter("title", keyword);
      query.setParameter("description", keyword);
      query.setParameter("url", keyword);
    }
    return query.getResultList();
  }

  public List<ApplicationEntity> getSystemApplications() {
    TypedQuery<ApplicationEntity> query = getEntityManager().createNamedQuery("ApplicationEntity.getSystemApplications",
                                                                              ApplicationEntity.class);
    return query.getResultList();
  }

  public ApplicationEntity getApplicationByTitle(String title) {
    TypedQuery<ApplicationEntity> query = getEntityManager()
                                                            .createNamedQuery("ApplicationEntity.getAppByTitle",
                                                                              ApplicationEntity.class)
                                                            .setParameter("title", title);
    List<ApplicationEntity> result = query.getResultList();
    if (result == null || result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("More than one application was found with Title '{}'", title);
      return result.get(0);
    } else {
      return result.get(0);
    }
  }

}
