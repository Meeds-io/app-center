/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import org.hibernate.jpa.HibernatePersistenceConfiguration;

import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Runs the queries of {@link ApplicationDAO} through the Spring Data proxy on
 * HSQLDB, whose LIKE is case-sensitive as PostgreSQL's is.
 */
class ApplicationDAOTest {

  private static final long           SPACE_ID       = 5L;

  private static final long           OTHER_SPACE_ID = 6L;

  private static EntityManagerFactory entityManagerFactory;

  private static EntityManager        entityManager;

  private static ApplicationDAO       applicationDAO;

  private static ApplicationEntity    spaceApplication;

  private static ApplicationEntity    otherSpaceApplication;

  private static ApplicationEntity    secondSpaceApplication;

  private static ApplicationEntity    platformApplication;

  @BeforeAll
  static void setup() {
    entityManagerFactory = new HibernatePersistenceConfiguration("app-center-dao-test")
                                                                                     .managedClass(ApplicationEntity.class)
                                                                                     .managedClass(FavoriteApplicationEntity.class)
                                                                                     .property("jakarta.persistence.jdbc.url",
                                                                                               "jdbc:hsqldb:mem:app-center-dao;shutdown=true")
                                                                                     .property("jakarta.persistence.jdbc.user", "SA")
                                                                                     .property("jakarta.persistence.jdbc.password", "")
                                                                                     .property("hibernate.hbm2ddl.auto", "create-drop")
                                                                                     .createEntityManagerFactory();
    entityManager = entityManagerFactory.createEntityManager();
    applicationDAO = new JpaRepositoryFactory(entityManager).getRepository(ApplicationDAO.class);
    entityManager.getTransaction().begin();
    entityManager.persist(application("Documents", "Shared files", "/portal/documents"));
    entityManager.persist(application("Agenda", "Events", "/portal/agenda"));
    spaceApplication = contextApplication("Team wiki", SPACE_ID);
    secondSpaceApplication = contextApplication("Team board", SPACE_ID);
    otherSpaceApplication = contextApplication("Other team wiki", OTHER_SPACE_ID);
    platformApplication = contextApplication("Platform wiki", 0L);
    entityManager.flush();
  }

  @AfterAll
  static void teardown() {
    entityManager.getTransaction().rollback();
    entityManager.close();
    entityManagerFactory.close();
  }

  @Test
  void getApplicationIdsMatchesTheKeywordWhateverItsCase() {
    List<Long> documents = applicationDAO.getApplicationIds("doc");

    assertEquals(1, documents.size());
    assertEquals(documents, applicationDAO.getApplicationIds("Doc"));
    assertEquals(documents, applicationDAO.getApplicationIds("DOCUMENTS"));
    assertEquals(documents, applicationDAO.getApplicationIds("SHARED"));
    assertEquals(2, applicationDAO.getApplicationIds("/PORTAL/").size());
    assertEquals(0, applicationDAO.getApplicationIds("Calendar").size());
  }

  @Test
  void findIdsByContextSpaceIdReadsTheApplicationsOfThatSpaceContextOnly() {
    assertEquals(Set.of(spaceApplication.getId(), secondSpaceApplication.getId()),
                 new HashSet<>(applicationDAO.findIdsByContextSpaceId(SPACE_ID)));
    assertEquals(List.of(otherSpaceApplication.getId()), applicationDAO.findIdsByContextSpaceId(OTHER_SPACE_ID));
    assertEquals(List.of(platformApplication.getId()), applicationDAO.findIdsByContextSpaceId(0L));
    assertEquals(List.of(), applicationDAO.findIdsByContextSpaceId(7L));
  }

  private static ApplicationEntity application(String title, String description, String url) {
    ApplicationEntity application = new ApplicationEntity();
    application.setTitle(title);
    application.setDescription(description);
    application.setUrl(url);
    return application;
  }

  private static ApplicationEntity contextApplication(String title, long spaceId) {
    ApplicationEntity application = application(title, title, "https://wiki.example.org/" + spaceId);
    application.setContextSettingName("Untitled-12-" + title);
    application.setContextSpaceId(spaceId);
    entityManager.persist(application);
    return application;
  }

}
