package org.exoplatform.appCenter.services.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.appCenter.services.entity.jpa.Application;
import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

/**
 * @author Ayoub Zayati
 */
public class ApplicationDAO extends GenericDAOJPAImpl<Application, Long> {

  private OrganizationService organizationService;

  public ApplicationDAO(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  public List<Application> getAuthorizedApplications(String userName,
                                                     String keyword) {
    try {
      Collection<Group> groups = organizationService.getGroupHandler()
                                                    .findGroupsOfUser(userName);
      List<Application> results = new ArrayList<Application>();
      groups.forEach(group -> {
        results.addAll((List<Application>) getEntityManager().createNamedQuery("ApplicationEntity.getAuthorizedApplications")
                                                             .setParameter("permission",
                                                                           "%"
                                                                               + group.getGroupName()
                                                                               + "%")
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

  public List<Application> getDefaultApplications(String userName) {
    try {
      Collection<Group> groups = organizationService.getGroupHandler()
                                                    .findGroupsOfUser(userName);
      List<Application> results = new ArrayList<Application>();
      groups.forEach(group -> {
        results.addAll((List<Application>) getEntityManager().createNamedQuery("ApplicationEntity.getDefaultApplications")
                                                             .setParameter("permission",
                                                                           "%"
                                                                               + group.getGroupName()
                                                                               + "%")
                                                             .getResultList());
      });
      return results.stream().distinct().collect(Collectors.toList());
    } catch (Exception e) {
      return null;
    }
  }

  public Application getAppByNameOrTitle(String title, String url) {
    try {
      return (Application) getEntityManager().createNamedQuery("ApplicationEntity.getAppByNameOrTitle")
                                             .setParameter("title", title)
                                             .setParameter("url", url)
                                             .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
