package org.exoplatform.appcenter.dao;

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;

/**
 * @author Ayoub Zayati
 */
public class ApplicationDAO extends GenericDAOJPAImpl<ApplicationEntity, Long> {

  public List<ApplicationEntity> findApplications(String keyword, int offset, int limit) {
    TypedQuery<ApplicationEntity> query = getEntityManager().createNamedQuery("ApplicationEntity.findApplications",
                                                                              ApplicationEntity.class);
    keyword = "%" + keyword + "%";
    query.setParameter("title", keyword);
    query.setParameter("url", keyword);
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public ApplicationEntity getApplicationByTitleOrUrl(String title, String url) {
    try {
      return getEntityManager().createNamedQuery("ApplicationEntity.getAppByTitleOrUrl", ApplicationEntity.class)
                               .setParameter("title", title)
                               .setParameter("url", url)
                               .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
