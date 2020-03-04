package org.exoplatform.appcenter.dao;

import java.util.List;

import javax.persistence.NoResultException;

import org.exoplatform.appcenter.entity.FavoriteApplication;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

/**
 * @author Ayoub Zayati
 */
public class FavoriteApplicationDAO extends
                                   GenericDAOJPAImpl<FavoriteApplication, Long> {

  public List<FavoriteApplication> getFavoriteApps(String userName) {
    try {
      return (List<FavoriteApplication>) getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteApps")
                                                           .setParameter("userName",
                                                                         userName)
                                                           .getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  public FavoriteApplication getFavoriteAppByUserNameAndAppId(Long applicationId,
                                                                              String userName) {
    try {
      return (FavoriteApplication) getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppByUserNameAndAppId")
                                                     .setParameter("applicationId",
                                                                   applicationId)
                                                     .setParameter("userName",
                                                                   userName)
                                                     .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<FavoriteApplication> getFavoriteAppsByAppId(Long applicationId) {
    try {
      return (List<FavoriteApplication>) getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteAppsByAppId")
                                                           .setParameter("applicationId",
                                                                         applicationId)
                                                           .getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }
}
