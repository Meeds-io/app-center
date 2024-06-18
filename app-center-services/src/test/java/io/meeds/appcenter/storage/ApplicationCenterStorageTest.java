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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;

import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationImage;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterStorage.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterStorageTest {

  private static final long        IMAGE_FILE_ID       = 5l;

  private static final long        IMAGE_LAST_MODIFIED = 588l;

  private static final String      HELP_PAGE_URL       = "helpPageUrl";

  private static final String      URL                 = "url";

  private static final String      PERMISSIONS_2       = "permissions2";

  private static final String      PERMISSIONS_1       = "permissions1";

  private static final String      DESCRIPTION         = "description";

  private static final String      TITLE               = "title";

  private static final String      TEST_USER           = "testuser";

  private static final String      FILE_CONTENT        = "fileContent";

  private static final Long        ID                  = 2l;

  @MockBean
  private FileService              fileService;

  @MockBean
  private ApplicationDAO           applicationDAO;

  @MockBean
  private FavoriteApplicationDAO   favoriteApplicationDAO;

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
    Application application = new Application(null,
                                              TITLE,
                                              URL,
                                              "",
                                              0L,
                                              0L,
                                              null,
                                              null,
                                              DESCRIPTION,
                                              false,
                                              true,
                                              false,
                                              false,
                                              false,
                                              PERMISSIONS_1,
                                              PERMISSIONS_2);

    Application storedApplication = applicationCenterStorage.createApplication(application);
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
  void testUpdateApplication() {
    ApplicationEntity existingApplication = new ApplicationEntity(ID,
                                                                  TITLE + "1",
                                                                  URL + "1",
                                                                  HELP_PAGE_URL + "1",
                                                                  6l,
                                                                  DESCRIPTION + "1",
                                                                  true,
                                                                  false,
                                                                  false,
                                                                  false,
                                                                  PERMISSIONS_2,
                                                                  false,
                                                                  null);
    when(applicationDAO.findById(ID)).thenReturn(Optional.of(existingApplication));

    Application application = application(ID);
    Application storedApplication = applicationCenterStorage.updateApplication(application);

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
  void testDeleteApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.deleteApplication(0));
    assertThrows(ApplicationNotFoundException.class, () -> applicationCenterStorage.deleteApplication(5000L));

    Application application = new Application(null,
                                              TITLE,
                                              URL,
                                              "",
                                              0L,
                                              0L,
                                              null,
                                              null,
                                              DESCRIPTION,
                                              false,
                                              true,
                                              false,
                                              false,
                                              false,
                                              PERMISSIONS_1,
                                              PERMISSIONS_2);

    Application storedApplication = applicationCenterStorage.createApplication(application);
    applicationCenterStorage.deleteApplication(storedApplication.getId());
    assertNull(applicationCenterStorage.getApplicationById(storedApplication.getId()));
  }

  @Test
  @SneakyThrows
  void testGetApplication() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.getApplicationById(0));
    assertNull(applicationCenterStorage.getApplicationById(50000));

    Application application = new Application(null,
                                              TITLE,
                                              URL,
                                              "",
                                              0L,
                                              0L,
                                              null,
                                              null,
                                              DESCRIPTION,
                                              false,
                                              true,
                                              false,
                                              false,
                                              false,
                                              PERMISSIONS_1,
                                              PERMISSIONS_2);

    Application storedApplication = applicationCenterStorage.createApplication(application);
    storedApplication = applicationCenterStorage.getApplicationById(storedApplication.getId());
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
  void testGetApplicationByTitleOrURL() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.getApplicationByTitle(null));
    assertNull(applicationCenterStorage.getApplicationByTitle(TITLE));

    when(applicationDAO.getApplicationByTitle(TITLE)).thenReturn(applicationEntity(ID));
    Application storedApplication = applicationCenterStorage.getApplicationByTitle(TITLE);
    assertNotNull(storedApplication);
  }

  @Test
  @SneakyThrows
  void testAddApplicationToUserFavorite() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterStorage.addApplicationToUserFavorite(0, TEST_USER));
    assertThrows(ApplicationNotFoundException.class,
                 () -> applicationCenterStorage.addApplicationToUserFavorite(50000, TEST_USER));

    Application application = new Application(null,
                                              TITLE,
                                              URL,
                                              "",
                                              0L,
                                              0L,
                                              null,
                                              null,
                                              DESCRIPTION,
                                              false,
                                              true,
                                              false,
                                              false,
                                              false,
                                              PERMISSIONS_1,
                                              PERMISSIONS_2);

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
    Application application = new Application(null,
                                              TITLE,
                                              URL,
                                              "",
                                              0L,
                                              0L,
                                              null,
                                              null,
                                              DESCRIPTION,
                                              false,
                                              true,
                                              false,
                                              false,
                                              false,
                                              PERMISSIONS_1,
                                              PERMISSIONS_2);
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
    when(favoriteApplicationDAO.getFavoriteAppsByUser(TEST_USER)).thenReturn(Collections.singletonList(favoriteApplicationEntity));
    when(favoriteApplicationEntity.getApplication()).thenReturn(applicationEntity);
    when(applicationEntity.isActive()).thenReturn(true);
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
    when(applicationDAO.getMandatoryActiveApps()).thenReturn(Collections.singletonList(applicationEntity()));
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

    when(applicationDAO.findAll()).thenReturn(Arrays.asList(applicationEntity(3l),
                                                            applicationEntity(2l),
                                                            applicationEntity(5l)));
    when(applicationDAO.getApplications(TITLE)).thenReturn(Arrays.asList(applicationEntity(3l),
                                                                         applicationEntity(5l)));
    when(applicationDAO.getApplications(URL)).thenReturn(Arrays.asList(applicationEntity(3l)));

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

  @Test
  void testCreateAppImageFileItem() {
    assertNull(applicationCenterStorage.createAppImageFileItem(null, null));
    assertNull(applicationCenterStorage.createAppImageFileItem("name", null));
    assertNull(applicationCenterStorage.createAppImageFileItem(null, FILE_CONTENT));
    ApplicationImage applicationImage = applicationCenterStorage.createAppImageFileItem("name", FILE_CONTENT);
    assertNotNull(applicationImage);
    assertNotNull(applicationImage.getFileName());
    assertNotNull(applicationImage.getFileBody());
  }

  @Test
  @SneakyThrows
  void testGetAppImageFile() {
    ApplicationImage applicationImage = applicationCenterStorage.createAppImageFileItem("name", FILE_CONTENT);
    assertNotNull(applicationImage);

    applicationImage = applicationCenterStorage.getAppImageFile(ID);
    assertNull(applicationImage);

    FileItem fileItem = mock(FileItem.class);
    FileInfo fileInfo = mock(FileInfo.class);
    when(fileItem.getAsByte()).thenReturn(FILE_CONTENT.getBytes());
    when(fileItem.getFileInfo()).thenReturn(fileInfo);
    when(fileInfo.getName()).thenReturn("filename");
    when(fileService.getFile(IMAGE_FILE_ID)).thenReturn(fileItem);

    applicationImage = applicationCenterStorage.getAppImageFile(IMAGE_FILE_ID);
    assertNotNull(applicationImage);
    assertNotNull(applicationImage.getFileName());
    assertNotNull(applicationImage.getFileBody());
  }

  private ApplicationEntity applicationEntity() {
    return applicationEntity(null);
  }

  private ApplicationEntity applicationEntity(Long id) {
    return new ApplicationEntity(id,
                                 TITLE + "1",
                                 URL + "1",
                                 HELP_PAGE_URL + "1",
                                 6l,
                                 DESCRIPTION + "1",
                                 true,
                                 false,
                                 false,
                                 false,
                                 PERMISSIONS_2,
                                 false,
                                 null);
  }

  private Application application(Long id) {
    return new Application(id,
                           "titre",
                           URL,
                           HELP_PAGE_URL,
                           IMAGE_FILE_ID,
                           IMAGE_LAST_MODIFIED,
                           "",
                           "",
                           DESCRIPTION,
                           false,
                           true,
                           false,
                           true,
                           false,
                           PERMISSIONS_1);
  }

}
