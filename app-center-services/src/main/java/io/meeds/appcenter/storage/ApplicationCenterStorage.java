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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationImage;
import io.meeds.appcenter.model.UserApplication;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;

import lombok.SneakyThrows;

/**
 * Storage service to access / load and save applications. This service will be
 * used , as well, to convert from JPA entity to DTO.
 */
@Component
public class ApplicationCenterStorage {

  private static final String    APPLICATION_NOT_FOUND_MESSAGE       = "Application with id %s doesn't exist";

  private static final String    APPLICATION_ID_IS_MANDATORY_MESSAGE = "applicationId is mandatory";

  private static final String    USERNAME_IS_MANDATORY_MESSAGE       = "username is mandatory";

  public static final String     NAME_SPACE                          = "appCenter";

  public static final Long       DEFAULT_LAST_MODIFIED               = System.currentTimeMillis();

  @Autowired
  private FileService            fileService;

  @Autowired
  private UploadService          uploadService;

  @Autowired
  private ApplicationDAO         applicationDAO;

  @Autowired
  private FavoriteApplicationDAO favoriteApplicationDAO;

  public Application getApplicationByTitle(String title) {
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("title is mandatory");
    }
    ApplicationEntity applicationentity = applicationDAO.getApplicationByTitle(title);
    return toDTO(applicationentity);
  }

  public Application createApplication(ApplicationForm application) {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity.setId(null);
    ApplicationImage applicationImage = createAppImageFileItem(application.getImageUploadId());
    if (applicationImage != null) {
      applicationEntity.setImageFileId(applicationImage.getId());
    }
    applicationEntity = applicationDAO.save(applicationEntity);
    return toDTO(applicationEntity);
  }

  public Application updateApplication(ApplicationForm application) throws ApplicationNotFoundException {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    Long applicationId = application.getId();
    ApplicationEntity storedApplicationEntity = applicationDAO.findById(applicationId).orElse(null);
    if (storedApplicationEntity == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }

    // Avoid changing this flag by UI
    application.setSystem(storedApplicationEntity.isSystem());

    Long oldImageFileId = storedApplicationEntity.getImageFileId();

    boolean imageRemoved = application.getImageFileId() != null
                           && application.getImageFileId() > 0
                           && oldImageFileId != null
                           && oldImageFileId > 0;

    boolean newImageAttached = StringUtils.isNotBlank(application.getImageUploadId());
    // if new image make sure to update it
    if (newImageAttached) {
      ApplicationImage applicationImage = createAppImageFileItem(application.getImageUploadId());
      if (applicationImage != null) {
        application.setImageFileId(applicationImage.getId());
        if (oldImageFileId != null && oldImageFileId > 0) {
          // Cleanup old useless image
          fileService.deleteFile(oldImageFileId);
        }
      }
    } else if (imageRemoved) {
      application.setImageFileId(null);
      // Cleanup old useless image
      fileService.deleteFile(oldImageFileId);
    } else {
      application.setImageFileId(oldImageFileId);
    }

    // if application is mandatory make sure to remove it from users favorites
    if (application.isMandatory()) {
      favoriteApplicationDAO.removeAllFavoritesOfApplication(application.getId());
    }

    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity = applicationDAO.save(applicationEntity);

    return toDTO(applicationEntity);
  }

  public void deleteApplication(long applicationId) throws ApplicationNotFoundException {
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
    if (applicationEntity == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    applicationDAO.delete(applicationEntity);
  }

  public Application getApplicationById(long applicationId) {
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
    return toDTO(applicationEntity);
  }

  public void addApplicationToUserFavorite(long applicationId, String username) throws ApplicationNotFoundException {
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    ApplicationEntity application = applicationDAO.findById(applicationId).orElse(null);
    if (application == null) {
      throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
    }
    favoriteApplicationDAO.save(new FavoriteApplicationEntity(application, username));
  }

  public void updateFavoriteApplicationOrder(long applicationId,
                                             String username,
                                             Long order) throws ApplicationNotFoundException {
    FavoriteApplicationEntity entity = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username);
    if (entity == null) {
      addApplicationToUserFavorite(applicationId, username);
      entity = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username);
    }
    entity.setOrder(order);
    favoriteApplicationDAO.save(entity);
  }

  public void deleteApplicationFavorite(Long applicationId, String username) {
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    FavoriteApplicationEntity applicationFavorite = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId,
                                                                                                            username);
    if (applicationFavorite != null) {
      favoriteApplicationDAO.delete(applicationFavorite);
    }
  }

  public List<UserApplication> getMandatoryApplications() {
    List<ApplicationEntity> applications = applicationDAO.getMandatoryActiveApps();
    return applications.stream()
                       .map(this::toUserApplicationDTO)
                       .toList();
  }

  public List<UserApplication> getFavoriteApplicationsByUser(String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    List<FavoriteApplicationEntity> applications = favoriteApplicationDAO.getFavoriteAppsByUser(username);
    return applications.stream()
                       .map(this::toUserApplicationDTO)
                       .filter(UserApplication::isActive)
                       .toList();
  }

  public List<Application> getSystemApplications() {
    List<ApplicationEntity> applications = applicationDAO.getSystemApplications();
    List<Application> list = new ArrayList<>();
    Application application = null;
    for (ApplicationEntity entity : applications) {
      application = toDTO(entity);
      list.add(application);
    }
    return list;
  }

  public boolean isFavoriteApplication(Long applicationId, String username) {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    return favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username) != null;
  }

  public long countFavorites(String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException(USERNAME_IS_MANDATORY_MESSAGE);
    }
    return favoriteApplicationDAO.countFavoritesForUser(username);
  }

  public ApplicationImage createAppImageFileItem(String uploadId) {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    String location = uploadResource.getStoreLocation();
    return updateAppImageFileItem(null, fileName, fileBody);
  }

  @SneakyThrows
  public Long getApplicationImageLastUpdated(long fileId) {
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null && fileItem.getFileInfo().getUpdatedDate() != null) {
      return fileItem.getFileInfo().getUpdatedDate().getTime();
    }
    return null;
  }

  @SneakyThrows
  public InputStream getApplicationImageInputStream(long fileId) {
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null && fileItem.getAsByte() != null) {
      return new ByteArrayInputStream(fileItem.getAsByte());
    }
    return null;
  }

  @SneakyThrows
  public ApplicationImage getAppImageFile(Long fileId) {
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null) {
      byte[] bytes = fileItem.getAsByte();
      String fileBody = new String(Base64.getEncoder().encode(bytes), Charset.defaultCharset());
      String fileName = fileItem.getFileInfo().getName();
      return new ApplicationImage(fileId, fileName, fileBody);
    }
    return null;
  }

  public List<Application> getApplications(String keyword) {
    List<ApplicationEntity> applications = StringUtils.isBlank(keyword) ? applicationDAO.findAll() :
                                                                        applicationDAO.getApplications(keyword);
    return applications.stream().map(this::toDTO).toList();
  }

  public long countApplications() {
    return applicationDAO.count();
  }

  public List<UserApplication> getMandatoryAndFavoriteApplications(String username, Pageable pageable) {
    return applicationDAO.findFavoriteAndMandatoryApplications(username, pageable).map(this::toUserApplicationDTO).getContent();
  }

  private Application toDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    String imageFileName = null;
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if (applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId() > 0) {
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if (fileInfo != null) {
        imageFileName = fileInfo.getName();
        if (fileInfo.getUpdatedDate() != null) {
          imageLastModified = fileInfo.getUpdatedDate().getTime();
        }
      }
    }
    String[] permissions = StringUtils.split(applicationEntity.getPermissions(), ",");
    Application application = new Application(applicationEntity.getId(),
                                              applicationEntity.getTitle(),
                                              applicationEntity.getUrl(),
                                              applicationEntity.getHelpPageUrl(),
                                              applicationEntity.getImageFileId(),
                                              imageLastModified,
                                              null,
                                              imageFileName,
                                              applicationEntity.getDescription(),
                                              applicationEntity.isSystem(),
                                              applicationEntity.isActive(),
                                              applicationEntity.isMandatory(),
                                              applicationEntity.isMobile(),
                                              applicationEntity.isChangedManually(),
                                              permissions);
    application.setSystem(applicationEntity.isSystem());
    application.setHelpPageURL(applicationEntity.getHelpPageUrl());
    application.setMobile(applicationEntity.isMobile());
    return application;
  }

  private UserApplication toUserApplicationDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    String imageFileName = null;
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if (applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId() > 0) {
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if (fileInfo != null) {
        imageFileName = fileInfo.getName();
        if (fileInfo.getUpdatedDate() != null) {
          imageLastModified = fileInfo.getUpdatedDate().getTime();
        }
      }
    }
    String[] permissions = StringUtils.split(applicationEntity.getPermissions(), ",");
    UserApplication userApplication = new UserApplication(applicationEntity.getId(),
                                                          applicationEntity.getTitle(),
                                                          applicationEntity.getUrl(),
                                                          applicationEntity.getHelpPageUrl(),
                                                          applicationEntity.getImageFileId(),
                                                          imageLastModified,
                                                          null,
                                                          imageFileName,
                                                          applicationEntity.getDescription(),
                                                          applicationEntity.isSystem(),
                                                          applicationEntity.isActive(),
                                                          applicationEntity.isMandatory(),
                                                          applicationEntity.isMobile(),
                                                          false,
                                                          applicationEntity.isChangedManually(),
                                                          permissions);
    userApplication.setSystem(applicationEntity.isSystem());
    userApplication.setHelpPageURL(applicationEntity.getHelpPageUrl());
    userApplication.setMobile(applicationEntity.isMobile());
    return userApplication;
  }

  private UserApplication toUserApplicationDTO(FavoriteApplicationEntity favoriteApplicationEntity) {
    if (favoriteApplicationEntity == null) {
      return null;
    }
    ApplicationEntity applicationEntity = favoriteApplicationEntity.getApplication();
    String imageFileName = null;
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if (applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId() > 0) {
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if (fileInfo != null) {
        imageFileName = fileInfo.getName();
        if (fileInfo.getUpdatedDate() != null) {
          imageLastModified = fileInfo.getUpdatedDate().getTime();
        }
      }
    }
    String[] permissions = StringUtils.split(applicationEntity.getPermissions(), ",");
    UserApplication userApplication = new UserApplication(applicationEntity.getId(),
                                                          applicationEntity.getTitle(),
                                                          applicationEntity.getUrl(),
                                                          applicationEntity.getHelpPageUrl(),
                                                          applicationEntity.getImageFileId(),
                                                          imageLastModified,
                                                          null,
                                                          imageFileName,
                                                          applicationEntity.getDescription(),
                                                          applicationEntity.isSystem(),
                                                          applicationEntity.isActive(),
                                                          applicationEntity.isMandatory(),
                                                          applicationEntity.isMobile(),
                                                          true,
                                                          applicationEntity.isChangedManually(),
                                                          permissions);
    // set UserApplication's order
    userApplication.setOrder(favoriteApplicationEntity.getOrder());
    userApplication.setSystem(applicationEntity.isSystem());
    userApplication.setHelpPageURL(applicationEntity.getHelpPageUrl());
    userApplication.setMobile(applicationEntity.isMobile());
    return userApplication;
  }

  private ApplicationEntity toEntity(Application application) {
    if (application == null) {
      return null;
    }
    ApplicationEntity applicationEntity = new ApplicationEntity(application.getId(),
                                                                application.getTitle(),
                                                                application.getUrl(),
                                                                application.getImageFileId(),
                                                                application.getDescription(),
                                                                application.isActive(),
                                                                application.isMandatory(),
                                                                StringUtils.join(application.getPermissions(), ","),
                                                                application.isChangedManually());
    applicationEntity.setSystem(application.isSystem());
    applicationEntity.setHelpPageUrl(application.getHelpPageURL());
    applicationEntity.setMobile(application.isMobile());
    return applicationEntity;
  }

  @SneakyThrows
  private ApplicationImage updateAppImageFileItem(Long fileId, String fileName, String fileBody) { // NOSONAR
    if (StringUtils.isBlank(fileName) || StringUtils.isBlank(fileBody)) {
      return null;
    }

    String fileContent = fileBody;
    if (fileBody.contains("base64,")) {
      String[] file = fileBody.split("base64,");
      fileContent = file[1];
    }

    byte[] bytesContent = fileContent.getBytes(Charset.defaultCharset().name());
    byte[] decodedBytes = Base64.getDecoder().decode(bytesContent);
    if (decodedBytes != null) {
      bytesContent = decodedBytes;
    }
    FileItem fileItem = new FileItem(fileId,
                                     fileName,
                                     "image/png",
                                     NAME_SPACE,
                                     bytesContent.length,
                                     new Date(),
                                     null,
                                     false,
                                     new ByteArrayInputStream(bytesContent));
    if (fileId != null && fileId > 0) {
      fileItem = fileService.updateFile(fileItem);
    } else {
      fileItem = fileService.writeFile(fileItem);
    }
    Long id = fileItem == null || fileItem.getFileInfo() == null ? null : fileItem.getFileInfo().getId();
    return new ApplicationImage(id, fileName, fileBody);
  }

}
