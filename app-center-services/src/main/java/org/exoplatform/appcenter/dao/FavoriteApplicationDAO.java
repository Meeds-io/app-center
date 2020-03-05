package org.exoplatform.appcenter.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author Ayoub Zayati
 */
public class FavoriteApplicationDAO extends GenericDAOJPAImpl<FavoriteApplicationEntity, Long> {
  private static final Log LOG = ExoLogger.getLogger(FavoriteApplicationDAO.class);

  public List<FavoriteApplicationEntity> getFavoriteApps(String userName) {
    return getEntityManager().createNamedQuery("FavoriteApplicationEntity.getFavoriteApps", FavoriteApplicationEntity.class)
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
    TypedQuery<Long> query = getEntityManager().createNamedQuery("FavoriteApplicationEntity.countFavoritesByUser",
                                                                 Long.class);
    query.setParameter("userName", username);
    return query.getSingleResult();
  }
}
