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
package org.exoplatform.appcenter.storage;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;

import org.exoplatform.appcenter.dao.ApplicationDAO;
import org.exoplatform.appcenter.dao.FavoriteApplicationDAO;
import org.exoplatform.appcenter.dto.*;
import org.exoplatform.appcenter.service.ApplicationNotFoundException;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.commons.file.services.impl.NameSpaceServiceImpl;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.naming.InitialContextInitializer;

public class ApplicationCenterStorageTest {

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

    // Workaround, the service wasn't started
    NameSpaceService fileNameSpaceService = ExoContainerContext.getService(NameSpaceService.class);
    assertNotNull(fileNameSpaceService);
    ((NameSpaceServiceImpl) fileNameSpaceService).start();
  }

  @After
  public void teardown() {
    FavoriteApplicationDAO favoriteApplicationDAO = ExoContainerContext.getService(FavoriteApplicationDAO.class);
    favoriteApplicationDAO.deleteAll();

    ApplicationDAO applicationDAO = ExoContainerContext.getService(ApplicationDAO.class);
    applicationDAO.deleteAll();

    RequestLifeCycle.end();
    container.stop();
    container = null;
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testCreateApplication() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.createApplication(null);
      fail("Shouldn't allow to add null application");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertEquals(application.getPermissions(), storedApplication.getPermissions());
  }

  @Test
  public void testUpdateApplication() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    application.setId(storedApplication.getId());
    application.setActive(!storedApplication.isActive());
    application.setByDefault(!storedApplication.isByDefault());
    application.setDescription("description2");
    application.setUrl("url2");
    application.setUrl("title2");
    application.setImageFileId(6L);
    application.setPermissions("permissions3", "permissions4");

    storedApplication = applicationCenterStorage.updateApplication(application);

    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertEquals(application.getPermissions(), storedApplication.getPermissions());
  }

  @Test
  public void testDeleteApplication() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.deleteApplication(0);
      fail("Shouldn't allow to delete not existing application");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.deleteApplication(5000L);
      fail("Shouldn't allow to delete application with not positive id");
    } catch (ApplicationNotFoundException e) {
      // Expected
    }

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.deleteApplication(storedApplication.getId());

    assertNull(applicationCenterStorage.getApplicationById(storedApplication.getId()));
  }

  @Test
  public void testGetApplication() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.getApplicationById(0);
      fail("Shouldn't allow to get application with not positive id");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    assertNull(applicationCenterStorage.getApplicationById(50000));

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    storedApplication = applicationCenterStorage.getApplicationById(storedApplication.getId());
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertEquals(application.getPermissions(), storedApplication.getPermissions());
  }

  @Test
  public void testGetApplicationByTitleOrURL() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.getApplicationByTitleOrURL(null, null);
      fail("Shouldn't allow to get application by null values");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.getApplicationByTitleOrURL(null, "url");
      fail("Shouldn't allow to get application by null values");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.getApplicationByTitleOrURL("title", null);
      fail("Shouldn't allow to get application by null values");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    assertNull(applicationCenterStorage.getApplicationByTitleOrURL("title", "url"));

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    applicationCenterStorage.createApplication(application);
    Application storedApplication = applicationCenterStorage.getApplicationByTitleOrURL(application.getTitle(),
                                                                                        application.getUrl());
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertEquals(application.getPermissions(), storedApplication.getPermissions());
  }

  @Test
  public void testAddApplicationToUserFavorite() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.addApplicationToUserFavorite(0, "testuser");
      fail("Shouldn't allow to add application as favorite with not positive id");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.addApplicationToUserFavorite(50000, "testuser");
      fail("Shouldn't allow to add not existing application as favorite");
    } catch (ApplicationNotFoundException e) {
      // Expected
    }

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");
  }

  @Test
  public void testUpdateApplicationFavoriteOrder() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");
    Long appID = applicationCenterStorage.getFavoriteApplicationsByUser("testuser").get(0).getId();
    applicationCenterStorage.updateFavoriteApplicationOrder(appID, "testuser", new Long(1));
    
    assertEquals(new Long(1), applicationCenterStorage.getFavoriteApplicationsByUser("testuser").get(0).getOrder());
  }

  @Test
  public void testDeleteApplicationFavorite() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.deleteApplicationFavorite(0L, "testuser");
      fail("Shouldn't allow to delete application as favorite with not positive id");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    applicationCenterStorage.deleteApplicationFavorite(50000L, "testuser");

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");
    applicationCenterStorage.deleteApplicationFavorite(storedApplication.getId(), "testuser");
  }

  @Test
  public void testGetFavoriteApplicationsByUser() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.getFavoriteApplicationsByUser(null);
      fail("Shouldn't allow to get favorite of null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    List<UserApplication> favoriteApplications = applicationCenterStorage.getFavoriteApplicationsByUser("testuser");
    assertNotNull(favoriteApplications);
    assertEquals(0, favoriteApplications.size());

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");
    assertEquals(1, applicationCenterStorage.getFavoriteApplicationsByUser("testuser").size());
  }

  @Test
  public void testGetMandatoryApplicationsByUser() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.getFavoriteApplicationsByUser(null);
      fail("Shouldn't allow to get favorite of null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    List<UserApplication> mandatoryApplications = applicationCenterStorage.getMandatoryApplications();
    assertNotNull(mandatoryApplications);
    assertEquals(0, mandatoryApplications.size());

    Application application1 = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              true,
                                              "permissions1",
                                              "permissions2");

    Application application2 = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              false,
                                              true,
                                              "permissions1",
                                              "permissions2");

    applicationCenterStorage.createApplication(application1);
    applicationCenterStorage.createApplication(application2);
    
    assertEquals(1, applicationCenterStorage.getMandatoryApplications().size());
  }

  @Test
  public void testGetApplications() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    List<Application> applications = applicationCenterStorage.getApplications(null);
    assertNotNull(applications);
    assertEquals(0, applications.size());

    applications = applicationCenterStorage.getApplications("title");
    assertNotNull(applications);
    assertEquals(0, applications.size());

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    applicationCenterStorage.createApplication(application);
    applications = applicationCenterStorage.getApplications(null);
    assertNotNull(applications);
    assertEquals(1, applications.size());

    applications = applicationCenterStorage.getApplications("title");
    assertNotNull(applications);
    assertEquals(1, applications.size());

    applications = applicationCenterStorage.getApplications("url");
    assertNotNull(applications);
    assertEquals(1, applications.size());
  }

  @Test
  public void testCountApplications() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    assertEquals(0, applicationCenterStorage.countApplications());

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    applicationCenterStorage.createApplication(application);
    assertEquals(1, applicationCenterStorage.countApplications());
  }

  @Test
  public void testIsFavoriteApplication() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.isFavoriteApplication(null, null);
      fail("Shouldn't allow to get favorite of null user or applicationId");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.isFavoriteApplication(null, "testuser");
      fail("Shouldn't allow to get favorite of null applicationId");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.isFavoriteApplication(0L, "testuser");
      fail("Shouldn't allow to get favorite of non positive applicationId");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterStorage.isFavoriteApplication(1L, null);
      fail("Shouldn't allow to get favorite of null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    assertFalse(applicationCenterStorage.isFavoriteApplication(1L, "testuser"));

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");

    assertTrue(applicationCenterStorage.isFavoriteApplication(storedApplication.getId(), "testuser"));
  }

  @Test
  public void testCountFavorites() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    try {
      applicationCenterStorage.countFavorites(null);
      fail("Shouldn't allow to get favorite of null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    assertEquals(0, applicationCenterStorage.countFavorites("testuser"));

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              "permissions1",
                                              "permissions2");

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), "testuser");

    assertEquals(1, applicationCenterStorage.countFavorites("testuser"));
  }

  @Test
  public void testCreateAppImageFileItem() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    assertNull(applicationCenterStorage.createAppImageFileItem(null, null));
    assertNull(applicationCenterStorage.createAppImageFileItem("name", null));
    assertNull(applicationCenterStorage.createAppImageFileItem(null, "fileContent"));
    ApplicationImage applicationImage = applicationCenterStorage.createAppImageFileItem("name", "fileContent");
    assertNotNull(applicationImage);
    assertNotNull(applicationImage.getFileName());
    assertNotNull(applicationImage.getFileBody());
  }

  @Test
  public void testGetAppImageFile() throws Exception {
    ApplicationCenterStorage applicationCenterStorage = ExoContainerContext.getService(ApplicationCenterStorage.class);
    assertNotNull(applicationCenterStorage);

    ApplicationImage applicationImage = applicationCenterStorage.createAppImageFileItem("name", "fileContent");
    assertNotNull(applicationImage);

    applicationImage = applicationCenterStorage.getAppImageFile(applicationImage.getId());
    assertNotNull(applicationImage);
    assertNotNull(applicationImage.getFileName());
    assertNotNull(applicationImage.getFileBody());
  }

}
