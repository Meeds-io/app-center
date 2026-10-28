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
package io.meeds.appcenter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.storage.ApplicationCategoryStorage;
import io.meeds.appcenter.storage.ApplicationCenterStorage;
import io.meeds.appcenter.storage.ApplicationPlacementStorage;
import io.meeds.appcenter.storage.PortletWindowStorage;
import io.meeds.layout.service.PortletInstanceService;
import io.meeds.portal.navigation.service.NavigationConfigurationService;
import io.meeds.social.translation.service.TranslationService;

import jakarta.persistence.EntityManagerFactory;

/**
 * Runs the favorites reorder and the creation of an application from a
 * Shortcuts portlet through the real storage, repositories and
 * transaction manager on HSQLDB: only a write failing midway on a real
 * database tells one transaction from a sequence of commits, which a mocked
 * storage cannot.
 */
@SpringBootTest(classes = { ApplicationCenterService.class, ApplicationCenterStorage.class,
    ApplicationCenterServiceTransactionTest.JpaConfiguration.class })
class ApplicationCenterServiceTransactionTest {

  private static final String            USERNAME = "testuser";

  @MockitoBean
  private UserACL                        userAcl;

  @MockitoBean
  private SettingService                 settingService;

  @MockitoBean
  private ApplicationPlacementStorage    placementStorage;

  @MockitoBean
  private NavigationConfigurationService navigationConfigurationService;

  @MockitoBean
  private UserPortalConfigService        userPortalConfigService;

  @MockitoBean
  private ApplicationBadgePluginRegistry badgePluginRegistry;

  @MockitoBean
  private TranslationService             translationService;

  @MockitoBean
  private FileService                    fileService;

  @MockitoBean
  private UploadService                  uploadService;

  @MockitoBean
  private ImageThumbnailService          imageThumbnailService;

  @MockitoBean
  private PortalContainer                portalContainer;

  @MockitoBean
  private MyApplicationsSettingsService  myApplicationsSettingsService;

  @MockitoBean
  private ApplicationCategoryStorage     applicationCategoryStorage;

  @MockitoBean
  private ResourceBundleService          resourceBundleService;

  @MockitoBean
  private SpaceService                   spaceService;

  @MockitoBean
  private PortletInstanceService         portletInstanceService;

  @MockitoBean
  private MetadataService                metadataService;

  @MockitoBean
  private PortletWindowStorage           windowStorage;

  @MockitoSpyBean
  private ApplicationCenterStorage       appCenterStorage;

  @Autowired
  private ApplicationCenterService       applicationCenterService;

  @Autowired
  private ApplicationDAO                 applicationDAO;

  @Autowired
  private FavoriteApplicationDAO         favoriteApplicationDAO;

  @BeforeEach
  void accessibleApplications() {
    Identity identity = mock(Identity.class);
    when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    when(userAcl.hasPermission(identity, ApplicationCenterService.DEFAULT_USERS_PERMISSION)).thenReturn(true);
  }

  @AfterEach
  void deleteApplications() {
    favoriteApplicationDAO.deleteAll();
    applicationDAO.deleteAll();
  }

  @Test
  void updateFavoriteApplicationsOrderWritesEveryOrderInOneTransaction() throws Exception {
    long first = saveApplication("Wiki");
    long second = saveApplication("Tasks");

    applicationCenterService.updateFavoriteApplicationsOrder(List.of(new ApplicationOrder(first, 1L),
                                                                     new ApplicationOrder(second, 0L)),
                                                             USERNAME);

    List<FavoriteApplicationEntity> favorites = favoriteApplicationDAO.findAll();
    assertEquals(2, favorites.size());
    assertEquals(Map.of(first, 1L, second, 0L),
                 Map.of(favorites.get(0).getApplication().getId(),
                        favorites.get(0).getOrder(),
                        favorites.get(1).getApplication().getId(),
                        favorites.get(1).getOrder()));
  }

  /**
   * Fails the mutant removing the transaction of
   * {@link ApplicationCenterService#updateFavoriteApplicationsOrder}: the first
   * application's favorite and order, written before the failure, would stay
   */
  @Test
  void updateFavoriteApplicationsOrderWritesNoOrderWhenAWriteFailsMidway() throws Exception {
    long first = saveApplication("Wiki");
    long second = saveApplication("Tasks");
    doThrow(new IllegalStateException("Database unreachable")).when(appCenterStorage)
                                                              .updateFavoriteApplicationOrder(eq(second), eq(USERNAME), any());
    List<ApplicationOrder> orders = List.of(new ApplicationOrder(first, 0L), new ApplicationOrder(second, 1L));

    assertThrows(IllegalStateException.class, () -> applicationCenterService.updateFavoriteApplicationsOrder(orders, USERNAME));

    assertEquals(0, favoriteApplicationDAO.count());
  }

  /**
   * Fails the mutant leaving the deprecated single-order method without its
   * own transaction: a call on this instance to the transactional batch
   * method bypasses the proxy, and the favorite added before the failing
   * order write would stay
   */
  @Test
  @SuppressWarnings("removal")
  void updateFavoriteApplicationOrderWritesNothingWhenItsOrderWriteFails() throws Exception {
    long application = saveApplication("Wiki");
    doThrow(new IllegalStateException("Database unreachable")).when(appCenterStorage)
                                                              .updateFavoriteApplicationOrder(eq(application), eq(USERNAME), any());
    ApplicationOrder order = new ApplicationOrder(application, 0L);

    assertThrows(IllegalStateException.class, () -> applicationCenterService.updateFavoriteApplicationOrder(order, USERNAME));

    assertEquals(0, favoriteApplicationDAO.count());
  }

  /**
   * Has the real database refuse the insert: fails the mutant enclosing it in
   * a transaction of the storage method, where the refusal surfaces at the
   * commit, after the compensation, which a mocked repository cannot show
   */
  @Test
  void createContextApplicationDeletesTheIllustrationOfAnApplicationTheDatabaseRefuses() {
    Application application = new Application();
    application.setTitle("Wiki");
    // Longer than the column the schema gives the url
    application.setUrl("https://meeds.io/" + "p".repeat(300));
    application.setImageFileId(55L);

    assertThrows(RuntimeException.class,
                 () -> appCenterStorage.createContextApplication(application, "Untitled-12-0a1b2c3d", 5L));
    verify(fileService).deleteFile(55L);
    assertEquals(0, applicationDAO.count());
  }

  private long saveApplication(String title) {
    ApplicationEntity application = new ApplicationEntity();
    application.setTitle(title);
    application.setUrl("https://meeds.io/" + title);
    application.setActive(true);
    return applicationDAO.save(application).getId();
  }

  @Configuration
  @EnableJpaRepositories(basePackageClasses = ApplicationDAO.class)
  @EnableTransactionManagement
  static class JpaConfiguration {

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
                                          .setName("app-center-transaction")
                                          .build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
      entityManagerFactory.setDataSource(dataSource);
      entityManagerFactory.setPackagesToScan(ApplicationEntity.class.getPackageName());
      entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      entityManagerFactory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return entityManagerFactory;
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
    }

  }

}
