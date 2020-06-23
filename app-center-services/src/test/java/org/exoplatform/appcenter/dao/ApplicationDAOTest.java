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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.naming.InitialContextInitializer;

public class ApplicationDAOTest {

  private PortalContainer container;

  @BeforeClass
  @SuppressWarnings("deprecation")
  public static void startDB() {
    RootContainer rootContainer = RootContainer.getInstance();
    InitialContextInitializer initializer = rootContainer.getComponentInstanceOfType(InitialContextInitializer.class);
    initializer.recall(); // NOSONAR
  }

  @Before
  public void setup() {
    container = PortalContainer.getInstance();
    assertNotNull(container);
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
  }

  @After
  public void teardown() {
    ApplicationDAO service = ExoContainerContext.getService(ApplicationDAO.class);
    service.deleteAll();

    RequestLifeCycle.end();
    container.stop();
    container = null;
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testServiceInitialized() {
    ApplicationDAO service = ExoContainerContext.getService(ApplicationDAO.class);
    assertNotNull(service);

    ApplicationEntity applicationEntity = new ApplicationEntity(null,
                                                                "title",
                                                                "url",
                                                                5L,
                                                                "description",
                                                                true,
                                                                false,
                                                                "permissions");
    ApplicationEntity storedEntity = service.create(applicationEntity);
    assertNotNull(storedEntity);
    assertNotNull(storedEntity.getId());
    assertEquals(applicationEntity.getTitle(), storedEntity.getTitle());
    assertEquals(applicationEntity.getUrl(), storedEntity.getUrl());
    assertEquals(applicationEntity.getImageFileId(), storedEntity.getImageFileId());
    assertEquals(applicationEntity.getDescription(), storedEntity.getDescription());
    assertEquals(applicationEntity.isActive(), storedEntity.isActive());
    assertEquals(applicationEntity.isMandatory(), storedEntity.isMandatory());
    assertEquals(applicationEntity.getPermissions(), storedEntity.getPermissions());

    // Test setters
    applicationEntity = new ApplicationEntity();
    applicationEntity.setId(null);
    applicationEntity.setTitle("title");
    applicationEntity.setUrl("url");
    applicationEntity.setImageFileId(5L);
    applicationEntity.setDescription("description");
    applicationEntity.setActive(false);
    applicationEntity.setMandatory(true);
    applicationEntity.setPermissions("permissions");

    storedEntity = service.create(applicationEntity);
    assertNotNull(storedEntity);
    assertNotNull(storedEntity.getId());
    assertEquals(applicationEntity.getTitle(), storedEntity.getTitle());
    assertEquals(applicationEntity.getUrl(), storedEntity.getUrl());
    assertEquals(applicationEntity.getImageFileId(), storedEntity.getImageFileId());
    assertEquals(applicationEntity.getDescription(), storedEntity.getDescription());
    assertEquals(applicationEntity.isActive(), storedEntity.isActive());
    assertEquals(applicationEntity.isMandatory(), storedEntity.isMandatory());
    assertEquals(applicationEntity.getPermissions(), storedEntity.getPermissions());
  }

  @Test
  public void testFindApplications() {
    ApplicationDAO service = ExoContainerContext.getService(ApplicationDAO.class);
    assertNotNull(service);

    ApplicationEntity applicationEntity = new ApplicationEntity(null,
                                                                "title",
                                                                "url",
                                                                5L,
                                                                "description",
                                                                true,
                                                                false,
                                                                "permissions");
    applicationEntity = service.create(applicationEntity);
    ApplicationEntity applicationEntity2 = new ApplicationEntity(null,
                                                                 "title2",
                                                                 "url2",
                                                                 5L,
                                                                 "description2",
                                                                 true,
                                                                 false,
                                                                 "permissions");
    applicationEntity2 = service.create(applicationEntity2);

    List<ApplicationEntity> applications = service.getApplications("title");
    assertNotNull(applications);
    assertEquals(2, applications.size());
    assertEquals(applicationEntity.getId(), applications.get(0).getId());

    applications = service.getApplications("title*");
    assertNotNull(applications);
    assertEquals(2, applications.size());
    assertEquals(applicationEntity.getId(), applications.get(0).getId());
    assertEquals(applicationEntity2.getId(), applications.get(1).getId());

    applications = service.getApplications("title*");
    assertNotNull(applications);
    assertEquals(2, applications.size());

    applications = service.getApplications("title2");
    assertNotNull(applications);
    assertEquals(1, applications.size());
    assertEquals(applicationEntity2.getId(), applications.get(0).getId());
  }

  @Test
  public void testGetApplicationByTitleOrUrl() {
    ApplicationDAO applicationDAO = ExoContainerContext.getService(ApplicationDAO.class);
    assertNotNull(applicationDAO);
    FavoriteApplicationDAO favoriteApplicationDAO = ExoContainerContext.getService(FavoriteApplicationDAO.class);
    assertNotNull(favoriteApplicationDAO);

    ApplicationEntity applicationEntity = new ApplicationEntity(null,
                                                                "title",
                                                                "url",
                                                                5L,
                                                                "description",
                                                                true,
                                                                false,
                                                                "permissions");
    applicationDAO.create(applicationEntity);
    ApplicationEntity applicationEntity2 = new ApplicationEntity(null,
                                                                 "title2",
                                                                 "url2",
                                                                 5L,
                                                                 "description2",
                                                                 true,
                                                                 false,
                                                                 "permissions");
    applicationDAO.create(applicationEntity2);

    ApplicationEntity foundEntity = applicationDAO.getApplicationByTitleOrUrl("title", "url");
    assertNotNull(foundEntity);
    assertEquals(applicationEntity.getId(), foundEntity.getId());

    foundEntity = applicationDAO.getApplicationByTitleOrUrl("title2", "url");
    assertNotNull(foundEntity);
    assertEquals(applicationEntity.getId(), foundEntity.getId());

    foundEntity = applicationDAO.getApplicationByTitleOrUrl("title", "url2");
    assertNotNull(foundEntity);
    assertEquals(applicationEntity.getId(), foundEntity.getId());

    foundEntity = applicationDAO.getApplicationByTitleOrUrl("title2", "url2");
    assertNotNull(foundEntity);
    assertEquals(applicationEntity2.getId(), foundEntity.getId());

    foundEntity = applicationDAO.getApplicationByTitleOrUrl("title3", "url3");
    assertNull(foundEntity);
  }

  @Test
  public void testGetFavoriteApps() {
    ApplicationDAO applicationDAO = ExoContainerContext.getService(ApplicationDAO.class);
    assertNotNull(applicationDAO);
    FavoriteApplicationDAO favoriteApplicationDAO = ExoContainerContext.getService(FavoriteApplicationDAO.class);
    assertNotNull(favoriteApplicationDAO);

    ApplicationEntity applicationEntity = new ApplicationEntity(null,
                                                                "title",
                                                                "url",
                                                                5L,
                                                                "description",
                                                                true,
                                                                false,
                                                                "permissions");
    applicationEntity = applicationDAO.create(applicationEntity);

    ApplicationEntity applicationEntity2 = new ApplicationEntity(null,
                                                                 "title2",
                                                                 "url2",
                                                                 5L,
                                                                 "description2",
                                                                 true,
                                                                 false,
                                                                 "permissions");
    applicationEntity2 = applicationDAO.create(applicationEntity2);

    ApplicationEntity applicationEntity3 = new ApplicationEntity(null,
                                                                 "title2",
                                                                 "url2",
                                                                 5L,
                                                                 "description2",
                                                                 true,
                                                                 true,
                                                                 "permissions");
    applicationEntity3 = applicationDAO.create(applicationEntity3);

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser2"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser3"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser4"));

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser3"));

    List<FavoriteApplicationEntity> favorites = favoriteApplicationDAO.getFavoriteAppsByUser("testuser");
    assertNotNull(favorites);
    assertEquals(2, favorites.size());

    favorites = favoriteApplicationDAO.getFavoriteAppsByUser("testuser2");
    assertNotNull(favorites);
    assertEquals(1, favorites.size());

    favorites = favoriteApplicationDAO.getFavoriteAppsByUser("fake");
    assertNotNull(favorites);
    assertEquals(0, favorites.size());
  }

  @Test
  public void testGetMandatoryApps() {
    ApplicationDAO applicationDAO = ExoContainerContext.getService(ApplicationDAO.class);
    assertNotNull(applicationDAO);

    ApplicationEntity applicationEntity1 = new ApplicationEntity(null,
                                                                 "title1",
                                                                 "url1",
                                                                 5L,
                                                                 "description1",
                                                                 true,
                                                                 true,
                                                                 "permissions");
    applicationDAO.create(applicationEntity1);

    ApplicationEntity applicationEntity2 = new ApplicationEntity(null,
                                                                 "title2",
                                                                 "url2",
                                                                 5L,
                                                                 "description2",
                                                                 true,
                                                                 true,
                                                                 "permissions");
    applicationDAO.create(applicationEntity2);

    ApplicationEntity applicationEntity3 = new ApplicationEntity(null,
                                                                 "title3",
                                                                 "url3",
                                                                 5L,
                                                                 "description3",
                                                                 false,
                                                                 true,
                                                                 "permissions");
    applicationDAO.create(applicationEntity3);

    List<ApplicationEntity> mandatoryApps = applicationDAO.getMandatoryActiveApps();
    assertNotNull(mandatoryApps);
    assertEquals(2, mandatoryApps.size());
  }

}
