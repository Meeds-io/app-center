package org.exoplatform.appcenter.dao;

import java.util.List;

import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author Ayoub Zayati
 */
public class ApplicationDAO extends GenericDAOJPAImpl<ApplicationEntity, Long> {
  private static final Log LOG = ExoLogger.getLogger(ApplicationDAO.class);

  public List<ApplicationEntity> getFavoriteActiveApps(String userName) {
    return getEntityManager().createNamedQuery("ApplicationEntity.getFavoriteActiveApps", ApplicationEntity.class)
                             .setParameter("userName", userName)
                             .getResultList();
  }

  public List<ApplicationEntity> getApplications(String keyword, int offset, int limit) {
    TypedQuery<ApplicationEntity> query = null;
    if (StringUtils.isBlank(keyword)) {
      query = getEntityManager().createNamedQuery("ApplicationEntity.getApplications",
                                                  ApplicationEntity.class);
    } else {
      query = getEntityManager().createNamedQuery("ApplicationEntity.getApplicationsByKeyword",
                                                  ApplicationEntity.class);
      keyword = "%" + keyword.replaceAll("%", "").replaceAll("\\*", "%") + "%";
      query.setParameter("title", keyword);
      query.setParameter("url", keyword);
    }

    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public List<ApplicationEntity> getSystemApplications() {
    TypedQuery<ApplicationEntity> query = getEntityManager().createNamedQuery("ApplicationEntity.getSystemApplications",
                                                                              ApplicationEntity.class);
    return query.getResultList();
  }

  public ApplicationEntity getApplicationByTitleOrUrl(String title, String url) {
    TypedQuery<ApplicationEntity> query = getEntityManager()
                                                            .createNamedQuery("ApplicationEntity.getAppByTitleOrUrl",
                                                                              ApplicationEntity.class)
                                                            .setParameter("title", title)
                                                            .setParameter("url", url);
    List<ApplicationEntity> result = query.getResultList();
    if (result == null || result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("More than one application was found with URL '{}' or Title '{}'", url, title);
      return result.get(0);
    } else {
      return result.get(0);
    }
  }

}
