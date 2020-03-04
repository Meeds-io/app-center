package org.exoplatform.appcenter.dao;

import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.appcenter.entity.Application;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.organization.Group;

/**
 * @author Ayoub Zayati
 */
public class ApplicationDAO extends GenericDAOJPAImpl<Application, Long> {

  public List<Application> getAuthorizedApplications(String userName,
                                                     Collection<Group> groups,
                                                     String keyword) {
    try {
      List<Application> results = new ArrayList<Application>();
      groups.forEach(group -> {
        results.addAll((List<Application>) getEntityManager().createNamedQuery("ApplicationEntity.getAuthorizedApplications")
                                                             .setParameter("permissionPattern1",
                                                                           "%:" + group.getId())
                                                             .setParameter("permissionPattern2",
                                                                           "%:" + group.getId() + ",%")
                                                             .getResultList());
      });
      return results.stream()
                    .distinct()
                    .filter(app -> StringUtils.containsIgnoreCase(app.getTitle(),
                                                                  keyword)
                        || StringUtils.containsIgnoreCase(app.getDescription(),
                                                          keyword))
                    .collect(Collectors.toList());
    } catch (Exception e) {
      return null;
    }
  }

  public List<Application> getDefaultApplications(String userName, Collection<Group> groups) {
    try {
      List<Application> results = new ArrayList<Application>();
      groups.forEach(group -> {
        results.addAll((List<Application>) getEntityManager().createNamedQuery("ApplicationEntity.getDefaultApplications")
                                                             .setParameter("permissionPattern1",
                                                                           "%:" + group.getId())
                                                             .setParameter("permissionPattern2",
                                                                           "%:" + group.getId() + ",%")
                                                             .getResultList());
      });
      return results.stream().distinct().collect(Collectors.toList());
    } catch (Exception e) {
      return null;
    }
  }

  public Application getAppByTitleOrUrl(String title, String url) {
    try {
      return (Application) getEntityManager().createNamedQuery("ApplicationEntity.getAppByTitleOrUrl")
                                             .setParameter("title", title)
                                             .setParameter("url", url)
                                             .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
