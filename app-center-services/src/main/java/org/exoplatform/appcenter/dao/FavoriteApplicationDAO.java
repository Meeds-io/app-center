package org.exoplatform.appcenter.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

/**
 * @author Ayoub Zayati
 */
public class FavoriteApplicationDAO extends GenericDAOJPAImpl<FavoriteApplicationEntity, Long> {

  public List<FavoriteApplicationEntity> getFavoriteApps(String userName) {
    try {
      return getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteApps", FavoriteApplicationEntity.class)
                               .setParameter("userName", userName)
                               .getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public FavoriteApplicationEntity getFavoriteAppByUserNameAndAppId(Long applicationId, String userName) {
    try {
      return getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppByUserNameAndAppId",
                                                 FavoriteApplicationEntity.class)
                               .setParameter("applicationId", applicationId)
                               .setParameter("userName", userName)
                               .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<FavoriteApplicationEntity> getFavoriteAppsByAppId(Long applicationId) {
    try {
      return getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppsByAppId",
                                                 FavoriteApplicationEntity.class)
                               .setParameter("applicationId", applicationId)
                               .getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public long countFavoritesForUser(String username) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("FavoriteApplicationEntity.countFavoritesByUser",
                                                                 Long.class);
    query.setParameter("userName", username);
    return query.getSingleResult();
  }
}
