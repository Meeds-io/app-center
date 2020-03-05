package org.exoplatform.appcenter.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.*;

import org.exoplatform.appcenter.dao.ApplicationDAO;
import org.exoplatform.appcenter.dao.FavoriteApplicationDAO;
import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.container.*;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.naming.InitialContextInitializer;

public class FavoriteApplicationDAOTest {

  private PortalContainer container;

  @Before
  public void setup() {
    RootContainer rootContainer = RootContainer.getInstance();
    InitialContextInitializer initializer = rootContainer.getComponentInstanceOfType(InitialContextInitializer.class);
    initializer.recall();

    container = PortalContainer.getInstance();
    assertNotNull(container);
    ExoContainerContext.setCurrentContainer(container);
    RequestLifeCycle.begin(container);
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
  public void testServiceInitialized() {
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
    FavoriteApplicationEntity favoriteApp = new FavoriteApplicationEntity();
    favoriteApp.setId(null);
    favoriteApp.setApplication(applicationEntity);
    favoriteApp.setUserName("testuser");
    favoriteApp = favoriteApplicationDAO.create(favoriteApp);
    assertNotNull(favoriteApp);
    assertNotNull(favoriteApp.getId());
    assertNotNull(favoriteApp.getApplication());
    assertEquals(applicationEntity.getId(), favoriteApp.getApplication().getId());
    assertEquals("testuser", favoriteApp.getUserName());
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

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser2"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser3"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser4"));

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser3"));

    List<FavoriteApplicationEntity> favorites = favoriteApplicationDAO.getFavoriteApps("testuser");
    assertNotNull(favorites);
    assertEquals(2, favorites.size());

    favorites = favoriteApplicationDAO.getFavoriteApps("testuser2");
    assertNotNull(favorites);
    assertEquals(1, favorites.size());

    favorites = favoriteApplicationDAO.getFavoriteApps("fake");
    assertNotNull(favorites);
    assertEquals(0, favorites.size());
  }

  @Test
  public void testGetFavoriteAppByUserNameAndAppId() {
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

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser2"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser3"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser4"));

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser3"));

    FavoriteApplicationEntity favoriteApp = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationEntity.getId(),
                                                                                                    "testuser4");
    assertNotNull(favoriteApp);
    assertNotNull(favoriteApp.getId());
    assertNotNull(favoriteApp.getApplication());
    assertEquals(applicationEntity.getId(), favoriteApp.getApplication().getId());
    assertEquals("testuser4", favoriteApp.getUserName());

    favoriteApp = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationEntity2.getId(), "testuser3");
    assertNotNull(favoriteApp);
    assertNotNull(favoriteApp.getId());
    assertNotNull(favoriteApp.getApplication());
    assertEquals(applicationEntity2.getId(), favoriteApp.getApplication().getId());
    assertEquals("testuser3", favoriteApp.getUserName());

    favoriteApp = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationEntity.getId(), "fake");
    assertNull(favoriteApp);
  }

  @Test
  public void testCountFavoritesForUser() {
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

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser2"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser3"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity, "testuser4"));

    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser"));
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(applicationEntity2, "testuser3"));

    assertEquals(2, favoriteApplicationDAO.countFavoritesForUser("testuser"));
    assertEquals(1, favoriteApplicationDAO.countFavoritesForUser("testuser2"));
    assertEquals(2, favoriteApplicationDAO.countFavoritesForUser("testuser3"));
    assertEquals(1, favoriteApplicationDAO.countFavoritesForUser("testuser4"));
  }

}
