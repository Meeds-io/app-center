package org.exoplatform.appcenter.service;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.*;
import org.picocontainer.Startable;

import org.exoplatform.appcenter.dao.ApplicationDAO;
import org.exoplatform.appcenter.dao.FavoriteApplicationDAO;
import org.exoplatform.appcenter.dto.*;
import org.exoplatform.commons.file.services.NameSpaceService;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.security.IdentityConstants;

public class ApplicationCenterServiceTest {

  private static final String      ADMIN_USERNAME  = "admin";

  private static final String      SIMPLE_USERNAME = "simple";

  private ApplicationCenterService applicationCenterService;

  @BeforeClass
  @SuppressWarnings("deprecation")
  public static void startDB() {
    RootContainer rootContainer = RootContainer.getInstance();
    InitialContextInitializer initializer = rootContainer.getComponentInstanceOfType(InitialContextInitializer.class);
    initializer.recall(); // NOSONAR
  }

  @Before
  @SuppressWarnings("deprecation")
  public void setup() throws Exception {
    PortalContainer container = PortalContainer.getInstance();
    assertNotNull(container);

    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
    applicationCenterService = ExoContainerContext.getService(ApplicationCenterService.class);

    OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
    UserHandler userHandler = organizationService.getUserHandler();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();

    User adminUser = userHandler.findUserByName(ADMIN_USERNAME);
    if (adminUser == null) {
      adminUser = userHandler.createUserInstance(ADMIN_USERNAME);
      userHandler.createUser(adminUser, true);

      Membership membership = new MembershipImpl("member:" + ADMIN_USERNAME + ":"
          + ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
      membershipHandler.createMembership(membership, true); // NOSONAR
    }
    User simpleUser = userHandler.findUserByName(SIMPLE_USERNAME);
    if (simpleUser == null) {
      simpleUser = userHandler.createUserInstance(SIMPLE_USERNAME);
      userHandler.createUser(simpleUser, true);
    }

    // Workaround, the service wasn't started
    Startable fileNameSpaceService = (Startable) ExoContainerContext.getService(NameSpaceService.class);
    assertNotNull(fileNameSpaceService);
    fileNameSpaceService.start();
  }

  @After
  public void teardown() {
    FavoriteApplicationDAO favoriteApplicationDAO = ExoContainerContext.getService(FavoriteApplicationDAO.class);
    favoriteApplicationDAO.deleteAll();

    ApplicationDAO applicationDAO = ExoContainerContext.getService(ApplicationDAO.class);
    applicationDAO.deleteAll();

    RequestLifeCycle.end();
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  public void testCreateApplication() throws Exception {
    try {
      applicationCenterService.createApplication(null);
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

    Application storedApplication = applicationCenterService.createApplication(application);
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertNotNull(application.getPermissions());
    assertNotNull(storedApplication.getPermissions());
    assertEquals(application.getPermissions().length, storedApplication.getPermissions().length);
    assertEquals(application.getPermissions()[0], storedApplication.getPermissions()[0]);

    try {
      applicationCenterService.createApplication(storedApplication);
      fail("Shouldn't allow to create the application again");
    } catch (ApplicationAlreadyExistsException e) {
      // Expected
    }
  }

  @Test
  public void testUpdateApplication() throws Exception {
    try {
      applicationCenterService.updateApplication(null, null);
      fail("Shouldn't allow to add null arguments");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.updateApplication(null, "testuser");
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
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);

    try {
      applicationCenterService.updateApplication(application, null);
      fail("Shouldn't allow to add null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.updateApplication(application, ADMIN_USERNAME);
      fail("Shouldn't allow to update non existing application");
    } catch (ApplicationNotFoundException e) {
      // Expected
    }

    try {
      application.setId(50000L);
      applicationCenterService.updateApplication(application, ADMIN_USERNAME);
      fail("Shouldn't allow to update non existing application");
    } catch (ApplicationNotFoundException e) {
      // Expected
    } finally {
      application.setId(null);
    }

    Application storedApplication = applicationCenterService.createApplication(application);

    application.setId(storedApplication.getId());
    application.setTitle("title2");
    storedApplication = applicationCenterService.updateApplication(application, ADMIN_USERNAME);

    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isByDefault(), storedApplication.isByDefault());
    assertNotNull(application.getPermissions());
    assertNotNull(storedApplication.getPermissions());
    assertEquals(application.getPermissions().length, storedApplication.getPermissions().length);
    assertEquals(application.getPermissions()[0], storedApplication.getPermissions()[0]);

    try {
      storedApplication = applicationCenterService.updateApplication(application, SIMPLE_USERNAME);
      fail("Simple user shouldn't be able to update non authorized application");
    } catch (IllegalAccessException e) {
      // Expected
    }

    application.setPermissions(IdentityConstants.ANY);
    storedApplication = applicationCenterService.updateApplication(application, ADMIN_USERNAME);

    application.setUrl("url2");
    storedApplication = applicationCenterService.updateApplication(application, SIMPLE_USERNAME);
    assertNotNull("simple user should be able to modify application after an admin changed its permissions", storedApplication);
  }

  @Test
  public void testDeleteApplication() throws Exception {
    try {
      applicationCenterService.deleteApplication(null, null);
      fail("Shouldn't allow to add null arguments");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.deleteApplication(null, "testuser");
      fail("Shouldn't allow to add null application");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.deleteApplication(50000L, null);
      fail("Shouldn't allow to add null user");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.deleteApplication(50000L, ADMIN_USERNAME);
      fail("Shouldn't allow to update non existing application");
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
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    Application storedApplication = applicationCenterService.createApplication(application);
    applicationCenterService.deleteApplication(storedApplication.getId(), ADMIN_USERNAME);

    application = new Application(null,
                                  "title",
                                  "url",
                                  5L,
                                  null,
                                  null,
                                  "description",
                                  true,
                                  false,
                                  ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    storedApplication = applicationCenterService.createApplication(application);
    try {
      applicationCenterService.deleteApplication(storedApplication.getId(), SIMPLE_USERNAME);
      fail("Simple user shouldn't be able to delete non authorized application");
    } catch (IllegalAccessException e1) {
      // Expected
    }

    application.setId(storedApplication.getId());
    application.setPermissions(IdentityConstants.ANY);
    storedApplication = applicationCenterService.updateApplication(application, ADMIN_USERNAME);

    applicationCenterService.deleteApplication(storedApplication.getId(), SIMPLE_USERNAME);
  }

  @Test
  public void testMaxFavoriteApps() {
    long originalMaxFavoriteApps = applicationCenterService.getMaxFavoriteApps();

    applicationCenterService.setMaxFavoriteApps(originalMaxFavoriteApps + 1);
    assertEquals(originalMaxFavoriteApps + 1, applicationCenterService.getMaxFavoriteApps());

    applicationCenterService.setMaxFavoriteApps(0);
    assertEquals(0, applicationCenterService.getMaxFavoriteApps());
  }

  @Test
  public void testSetDefaultAppImage() throws Exception {
    ApplicationImage applicationImage = applicationCenterService.setDefaultAppImage(null);
    assertNull(applicationImage);

    applicationImage = new ApplicationImage(null, null, null);
    applicationImage = applicationCenterService.setDefaultAppImage(applicationImage);
    assertNull(applicationImage);

    applicationImage = new ApplicationImage(null, "name", "content");
    applicationImage = applicationCenterService.setDefaultAppImage(applicationImage);
    assertNotNull(applicationImage);
    assertNotNull(applicationImage.getId());

    ApplicationImage storedApplicationImage = applicationCenterService.setDefaultAppImage(applicationImage);
    assertNotNull(storedApplicationImage);
    assertEquals(applicationImage.getId(), storedApplicationImage.getId());
  }

  @Test
  public void testGetAppGeneralSettings() throws Exception {
    applicationCenterService.setDefaultAppImage(null);
    applicationCenterService.setMaxFavoriteApps(0);

    JSONObject generalSettings = applicationCenterService.getAppGeneralSettings();
    assertNotNull(generalSettings);
    assertEquals(0, generalSettings.getLong(ApplicationCenterService.MAX_FAVORITE_APPS));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_NAME));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_BODY));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_ID));

    applicationCenterService.setDefaultAppImage(new ApplicationImage(null, null, null));
    generalSettings = applicationCenterService.getAppGeneralSettings();

    assertEquals(0, generalSettings.getLong(ApplicationCenterService.MAX_FAVORITE_APPS));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_NAME));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_BODY));
    assertFalse(generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_ID));

    applicationCenterService.setDefaultAppImage(new ApplicationImage(null, "name", "content"));

    generalSettings = applicationCenterService.getAppGeneralSettings();
    assertEquals(0, generalSettings.getLong(ApplicationCenterService.MAX_FAVORITE_APPS));
    assertTrue(generalSettings.toString() + "does not contain file",
               generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_NAME));
    assertEquals("name", generalSettings.getString(ApplicationCenterService.DEFAULT_APP_IMAGE_NAME));
    assertTrue(generalSettings.toString() + "does not contain file",
               generalSettings.has(ApplicationCenterService.DEFAULT_APP_IMAGE_BODY));
    assertTrue(generalSettings.getString(ApplicationCenterService.DEFAULT_APP_IMAGE_BODY).contains("content"));
    assertNotNull(generalSettings.getLong(ApplicationCenterService.DEFAULT_APP_IMAGE_ID));
  }

  @Test
  public void testAddFavoriteApplication() throws Exception {
    try {
      applicationCenterService.addFavoriteApplication(0, null);
      fail("Shouldn't allow to add null arguments");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.addFavoriteApplication(0, SIMPLE_USERNAME);
      fail("Shouldn't allow to add zero as applicationId");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.addFavoriteApplication(5000L, SIMPLE_USERNAME);
      fail("Shouldn't allow to add an application as favorite while it doesn't exist");
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
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    Application storedApplication = applicationCenterService.createApplication(application);
    try {
      applicationCenterService.addFavoriteApplication(storedApplication.getId(), SIMPLE_USERNAME);
      fail("Shouldn't allow to add an application as favorite while user doesn't have access to it");
    } catch (IllegalAccessException e) {
      // Expected
    }

    applicationCenterService.addFavoriteApplication(storedApplication.getId(), ADMIN_USERNAME);
    storedApplication.setPermissions(IdentityConstants.ANY);
    storedApplication = applicationCenterService.updateApplication(storedApplication, ADMIN_USERNAME);
    applicationCenterService.addFavoriteApplication(storedApplication.getId(), SIMPLE_USERNAME);
  }

  @Test
  public void testDeleteFavoriteApplication() throws Exception {
    try {
      applicationCenterService.deleteFavoriteApplication(null, null);
      fail("Shouldn't allow to add null arguments");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      applicationCenterService.deleteFavoriteApplication(0L, SIMPLE_USERNAME);
      fail("Shouldn't allow to add zero as applicationId");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    applicationCenterService.deleteFavoriteApplication(5000L, SIMPLE_USERNAME);

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    Application storedApplication = applicationCenterService.createApplication(application);
    applicationCenterService.deleteFavoriteApplication(storedApplication.getId(), SIMPLE_USERNAME);
    applicationCenterService.deleteFavoriteApplication(storedApplication.getId(), ADMIN_USERNAME);

    applicationCenterService.addFavoriteApplication(storedApplication.getId(), ADMIN_USERNAME);
    applicationCenterService.deleteFavoriteApplication(storedApplication.getId(), ADMIN_USERNAME);

    storedApplication.setPermissions(IdentityConstants.ANY);
    storedApplication = applicationCenterService.updateApplication(storedApplication, ADMIN_USERNAME);

    applicationCenterService.addFavoriteApplication(storedApplication.getId(), SIMPLE_USERNAME);
    applicationCenterService.deleteFavoriteApplication(storedApplication.getId(), SIMPLE_USERNAME);
  }

  @Test
  public void testGetApplicationsList() throws Exception {
    ApplicationList applicationsList = applicationCenterService.getApplicationsList(0, 0, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    applicationCenterService.createApplication(application);

    Application application2 = new Application(null,
                                               "title2",
                                               "url2",
                                               6L,
                                               null,
                                               null,
                                               "description",
                                               true,
                                               false,
                                               ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    applicationCenterService.createApplication(application2);

    applicationsList = applicationCenterService.getApplicationsList(0, 0, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(2, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getApplicationsList(1, 0, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(2, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getApplicationsList(2, 0, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(2, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getApplicationsList(3, 0, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(2, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getApplicationsList(0, 10, null);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(2, applicationsList.getTotalApplications());
  }

  @Test
  public void testGetAuthorizedApplicationsList() throws Exception {
    try {
      applicationCenterService.getAuthorizedApplicationsList(0, 0, null, null);
      fail("Shouldn't retrieve applications with null username");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    ApplicationList applicationsList = applicationCenterService.getAuthorizedApplicationsList(0, 0, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    Application application = new Application(null,
                                              "title",
                                              "url",
                                              5L,
                                              null,
                                              null,
                                              "description",
                                              true,
                                              false,
                                              ApplicationCenterService.DEFAULT_ADMINISTRATORS_GROUP);
    applicationCenterService.createApplication(application);

    Application application2 = new Application(null,
                                               "title2",
                                               "url2",
                                               6L,
                                               null,
                                               null,
                                               "description",
                                               true,
                                               false,
                                               IdentityConstants.ANY);
    applicationCenterService.createApplication(application2);

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(0, 0, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(0, 0, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(1, 0, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(1, 0, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(2, 0, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(2, 0, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(3, 0, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(3, 0, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(0, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(0, 10, null, ADMIN_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(2, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());

    applicationsList = applicationCenterService.getAuthorizedApplicationsList(0, 10, null, SIMPLE_USERNAME);
    assertNotNull(applicationsList);
    assertNotNull(applicationsList.getApplications());
    assertEquals(1, applicationsList.getApplications().size());
    assertEquals(0, applicationsList.getTotalApplications());
  }

}
