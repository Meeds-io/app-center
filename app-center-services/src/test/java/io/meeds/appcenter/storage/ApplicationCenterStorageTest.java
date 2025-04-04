/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterStorage.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterStorageTest {

  private static final String      HELP_PAGE_URL = "helpPageUrl";

  private static final String      URL           = "url";

  private static final String      PERMISSIONS_2 = "permissions2";

  private static final String      PERMISSIONS_1 = "permissions1";

  private static final String      DESCRIPTION   = "description";

  private static final String      TITLE         = "title";

  private static final String      TEST_USER     = "testuser";

  private static final Long        ID            = 2l;

  @MockBean
  private FileService              fileService;

  @MockBean
  private ApplicationDAO           applicationDAO;

  @MockBean
  private FavoriteApplicationDAO   favoriteApplicationDAO;

  @MockBean
  private UploadService            uploadService;

  @Autowired
  private ApplicationCenterStorage applicationCenterStorage;

  @BeforeEach
  void setup() {
    when(applicationDAO.save(any())).thenAnswer(invocation -> {
      ApplicationEntity entity = invocation.getArgument(0);
      if (entity.getId() == null) {
        entity.setId(ID);
      }
      when(applicationDAO.findById(ID)).thenReturn(Optional.of(entity));
      return entity;
    });
    doAnswer(invocation -> {
      ApplicationEntity entity = invocation.getArgument(0);
      when(applicationDAO.findById(entity.getId())).thenReturn(Optional.empty());
      return null;
    }).when(applicationDAO).delete(any());
  }

  @Test
  void testCreateApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.createApplication(null));
    Application application = application(null);
    Application storedApplication = applicationCenterStorage.createApplication(application);
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertTrue(storedApplication.getId() > 0);

    storedApplication.setId(null);
    assertEquals(application, storedApplication);
  }

  @Test
  @SneakyThrows
  void testUpdateApplication() {
    ApplicationEntity existingApplication = applicationEntity(ID);
    when(applicationDAO.findById(ID)).thenReturn(Optional.of(existingApplication));

    Application application = application(ID);
    applicationCenterStorage.updateApplication(application);
    Application storedApplication = applicationCenterStorage.getApplication(ID);

    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application, storedApplication);
  }

  @Test
  @SneakyThrows
  void testDeleteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.deleteApplication(0));
    assertThrows(ApplicationNotFoundException.class, () -> applicationCenterStorage.deleteApplication(5000L));

    Application application = application(null);

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.deleteApplication(storedApplication.getId());
    assertNull(applicationCenterStorage.getApplication(storedApplication.getId()));
  }

  @Test
  @SneakyThrows
  void testGetApplication() {
    assertNull(applicationCenterStorage.getApplication(50000l));

    Application application = application(null);
    Application storedApplication = applicationCenterStorage.createApplication(application);
    storedApplication = applicationCenterStorage.getApplication(storedApplication.getId());
    assertNotNull(storedApplication);
    assertNotNull(storedApplication.getId());
    assertEquals(application.getTitle(), storedApplication.getTitle());
    assertEquals(application.getUrl(), storedApplication.getUrl());
    assertEquals(application.getImageFileId(), storedApplication.getImageFileId());
    assertEquals(application.getDescription(), storedApplication.getDescription());
    assertEquals(application.isActive(), storedApplication.isActive());
    assertEquals(application.isMandatory(), storedApplication.isMandatory());
    assertEquals(application.getPermissions(), storedApplication.getPermissions());
  }

  @Test
  @SneakyThrows
  void testAddApplicationToUserFavorite() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.addApplicationToUserFavorite(0, TEST_USER));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterStorage.addApplicationToUserFavorite(50000, TEST_USER));

    Application application = application(null);

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), TEST_USER);
  }

  @Test
  @SneakyThrows
  void testUpdateApplicationFavoriteOrder() {
    FavoriteApplicationEntity favoriteApplicationEntity = mock(FavoriteApplicationEntity.class);
    when(favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(ID, TEST_USER)).thenReturn(favoriteApplicationEntity);
    applicationCenterStorage.updateFavoriteApplicationOrder(ID, TEST_USER, 1l);

    verify(favoriteApplicationDAO).save(favoriteApplicationEntity);

    applicationCenterStorage.updateFavoriteApplicationOrder(ID, TEST_USER, 1l);
    verify(favoriteApplicationDAO, times(2)).save(favoriteApplicationEntity);
  }

  @Test
  @SneakyThrows
  void testDeleteApplicationFavorite() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.deleteApplicationFavorite(0L, TEST_USER));
    applicationCenterStorage.deleteApplicationFavorite(50000L, TEST_USER);
    Application application = application(null);
    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.addApplicationToUserFavorite(storedApplication.getId(), TEST_USER);
    applicationCenterStorage.deleteApplicationFavorite(storedApplication.getId(), TEST_USER);
  }

  @Test
  @SneakyThrows
  void testGetFavoriteApplicationsByUser() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.getFavoriteApplicationsByUser(null));
    List<UserApplication> favoriteApplications = applicationCenterStorage.getFavoriteApplicationsByUser(TEST_USER);
    assertNotNull(favoriteApplications);
    assertEquals(0, favoriteApplications.size());

    FavoriteApplicationEntity favoriteApplicationEntity = mock(FavoriteApplicationEntity.class);
    ApplicationEntity applicationEntity = mock(ApplicationEntity.class);
    when(applicationEntity.getId()).thenReturn(ID);

    when(favoriteApplicationDAO.getFavoriteAppsByUser(TEST_USER)).thenReturn(Collections.singletonList(favoriteApplicationEntity));
    when(favoriteApplicationEntity.getApplication()).thenReturn(applicationEntity);
    when(applicationEntity.isActive()).thenReturn(true);
    assertEquals(0, applicationCenterStorage.getFavoriteApplicationsByUser(TEST_USER).size());
    when(applicationDAO.findById(ID)).thenReturn(Optional.of(applicationEntity));
    assertEquals(1, applicationCenterStorage.getFavoriteApplicationsByUser(TEST_USER).size());

    when(applicationEntity.isMandatory()).thenReturn(true);
    assertEquals(1, applicationCenterStorage.getFavoriteApplicationsByUser(TEST_USER).size());

    when(applicationEntity.isActive()).thenReturn(false);
    assertEquals(0, applicationCenterStorage.getFavoriteApplicationsByUser(TEST_USER).size());
  }

  @Test
  void testGetMandatoryApplicationsByUser() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.getFavoriteApplicationsByUser(null));

    List<UserApplication> mandatoryApplications = applicationCenterStorage.getMandatoryApplications();
    assertNotNull(mandatoryApplications);
    assertEquals(0, mandatoryApplications.size());
    when(applicationDAO.getMandatoryActiveApplicationIds()).thenReturn(Collections.singletonList(ID));
    when(applicationDAO.findById(ID)).thenReturn(Optional.of(applicationEntity(ID)));
    assertEquals(1, applicationCenterStorage.getMandatoryApplications().size());
  }

  @Test
  void testGetApplications() {

    List<Application> applications = applicationCenterStorage.getApplications(null);
    assertNotNull(applications);
    assertEquals(0, applications.size());

    applications = applicationCenterStorage.getApplications(TITLE);
    assertNotNull(applications);
    assertEquals(0, applications.size());

    when(applicationDAO.getApplicationIds()).thenReturn(Arrays.asList(3l, 2l, 5l));
    when(applicationDAO.getApplicationIds(TITLE)).thenReturn(Arrays.asList(3l, 5l));
    when(applicationDAO.getApplicationIds(URL)).thenReturn(Collections.singletonList(3l));

    when(applicationDAO.findById(2l)).thenReturn(Optional.of(applicationEntity(2l)));
    when(applicationDAO.findById(3l)).thenReturn(Optional.of(applicationEntity(3l)));
    when(applicationDAO.findById(5l)).thenReturn(Optional.of(applicationEntity(5l)));

    applications = applicationCenterStorage.getApplications(null);
    assertNotNull(applications);
    assertEquals(3, applications.size());

    applications = applicationCenterStorage.getApplications(TITLE);
    assertNotNull(applications);
    assertEquals(2, applications.size());

    applications = applicationCenterStorage.getApplications(URL);
    assertNotNull(applications);
    assertEquals(1, applications.size());
  }

  @Test
  void testCountApplications() {
    assertEquals(0l, applicationCenterStorage.countApplications());
    when(applicationDAO.count()).thenReturn(1l);
    assertEquals(1l, applicationCenterStorage.countApplications());
  }

  @Test
  @SneakyThrows
  void testIsFavoriteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.isFavoriteApplication(null, null));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.isFavoriteApplication(null, TEST_USER));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.isFavoriteApplication(0L, TEST_USER));
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.isFavoriteApplication(1L, null));
    when(favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(ID,
                                                                 TEST_USER)).thenReturn(mock(FavoriteApplicationEntity.class));
    assertFalse(applicationCenterStorage.isFavoriteApplication(1L, TEST_USER));
    assertTrue(applicationCenterStorage.isFavoriteApplication(ID, TEST_USER));
  }

  @Test
  @SneakyThrows
  void testCountFavorites() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.countFavorites(null));
    assertEquals(0, applicationCenterStorage.countFavorites(TEST_USER));
    when(favoriteApplicationDAO.countFavoritesForUser(TEST_USER)).thenReturn(1l);
    assertEquals(1, applicationCenterStorage.countFavorites(TEST_USER));
  }

  private ApplicationEntity applicationEntity(Long id) {
    return new ApplicationEntity(id,
                                 TITLE + "1",
                                 DESCRIPTION + "1",
                                 ApplicationType.LINK,
                                 "url",
                                 "icon",
                                 HELP_PAGE_URL + "1",
                                 6l,
                                 true,
                                 false,
                                 false,
                                 false,
                                 false,
                                 Collections.singletonList(PERMISSIONS_2),
                                 false);
  }

  private Application application(Long id) {
    return new Application(id,
                           TITLE,
                           URL,
                           HELP_PAGE_URL,
                           DESCRIPTION,
                           ApplicationType.LINK,
                           false,
                           true,
                           false,
                           true,
                           false,
                           Collections.singletonList(PERMISSIONS_1),
                           null,
                           null,
                           "icon",
                           null,
                           0l,
                           false);
  }

}
