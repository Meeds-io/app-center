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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import java.util.Base64;

import org.exoplatform.appcenter.dao.ApplicationDAO;
import org.exoplatform.appcenter.dao.FavoriteApplicationDAO;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationImage;
import org.exoplatform.appcenter.dto.UserApplication;
import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.appcenter.service.ApplicationNotFoundException;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;

/**
 * Storage service to access / load and save applications. This service will be
 * used , as well, to convert from JPA entity to DTO.
 */
public class ApplicationCenterStorage {

  public static final String     NAME_SPACE            = "appCenter";

  public static final Long       DEFAULT_LAST_MODIFIED = System.currentTimeMillis();

  private FileService            fileService;

  private ApplicationDAO         applicationDAO;

  private FavoriteApplicationDAO favoriteApplicationDAO;

  public ApplicationCenterStorage(ApplicationDAO applicationDAO,
                                  FavoriteApplicationDAO favoriteApplicationDAO,
                                  FileService fileService) {
    this.applicationDAO = applicationDAO;
    this.favoriteApplicationDAO = favoriteApplicationDAO;
    this.fileService = fileService;
  }

  public Application getApplicationByTitle(String title) {
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("title is mandatory");
    }
    ApplicationEntity applicationentity = applicationDAO.getApplicationByTitle(title);
    return toDTO(applicationentity);
  }

  public Application createApplication(Application applicationForm) throws Exception {
    if (applicationForm == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    ApplicationEntity applicationEntity = toEntity(applicationForm);
    applicationEntity.setId(null);
    ApplicationImage applicationImage = createAppImageFileItem(applicationForm.getImageFileName(),
                                                               applicationForm.getImageFileBody());
    if (applicationImage != null) {
      applicationEntity.setImageFileId(applicationImage.getId());
    }
    applicationEntity = applicationDAO.create(applicationEntity);
    return toDTO(applicationEntity);
  }

  public Application updateApplication(Application application) throws Exception {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    Long applicationId = application.getId();
    ApplicationEntity storedApplicationEntity = applicationDAO.find(applicationId);
    if (storedApplicationEntity == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found");
    }

    // Avoid changing this flag by UI
    application.setSystem(storedApplicationEntity.isSystem());

    Long oldImageFileId = storedApplicationEntity.getImageFileId();

    boolean imageRemoved = StringUtils.isBlank(application.getImageFileBody())
        && StringUtils.isBlank(application.getImageFileName()) && oldImageFileId != null && oldImageFileId > 0;

    boolean newImageAttached = StringUtils.isNotBlank(application.getImageFileBody())
        && StringUtils.isNotBlank(application.getImageFileName());
    // if new image make sure to update it
    if (newImageAttached) {
      ApplicationImage applicationImage = createAppImageFileItem(application.getImageFileName(), application.getImageFileBody());
      if (applicationImage != null) {
        application.setImageFileId(applicationImage.getId());
        if (oldImageFileId != null && oldImageFileId > 0) {
          // Cleanup old useless image
          fileService.deleteFile(oldImageFileId);
        }
      }
    } else {
      application.setImageFileId(oldImageFileId);
    }

    // if image was unset remove it
    if (imageRemoved) {
      application.setImageFileId(null);
      // Cleanup old useless image
      fileService.deleteFile(oldImageFileId);
    }

    // if application is mandatory make sure to remove it from users favorites
    if (application.isMandatory()) {
      favoriteApplicationDAO.removeAllFavoritesOfApplication(application.getId());
    }

    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity = applicationDAO.update(applicationEntity);

    return toDTO(applicationEntity);
  }

  public void deleteApplication(long applicationId) throws ApplicationNotFoundException {
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    ApplicationEntity applicationEntity = applicationDAO.find(applicationId);
    if (applicationEntity == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " not found");
    }
    applicationDAO.delete(applicationEntity);
  }

  public Application getApplicationById(long applicationId) {
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    ApplicationEntity applicationEntity = applicationDAO.find(applicationId);
    return toDTO(applicationEntity);
  }

  public void addApplicationToUserFavorite(long applicationId, String username) throws ApplicationNotFoundException {
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    ApplicationEntity application = applicationDAO.find(applicationId);
    if (application == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found in store");
    }
    favoriteApplicationDAO.create(new FavoriteApplicationEntity(application, username));
  }

  public void updateFavoriteApplicationOrder(long applicationId, String username, Long order) {
    FavoriteApplicationEntity entity = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username);
    // check if it is a favorite application and not a system application
    if (entity != null && !entity.getApplication().isMandatory()) {
      entity.setOrder(order);
      favoriteApplicationDAO.update(entity);
    }
  }

  public void deleteApplicationFavorite(Long applicationId, String username) {
    if (applicationId <= 0) {
      throw new IllegalArgumentException("applicationId must be a positive integer");
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
                       .collect(Collectors.toList());
  }

  public List<UserApplication> getFavoriteApplicationsByUser(String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    List<FavoriteApplicationEntity> applications = favoriteApplicationDAO.getFavoriteAppsByUser(username);
    return applications.stream()
                       .map(this::toUserApplicationDTO)
                       .filter(userApplication -> userApplication.isActive() && !userApplication.isMandatory())
                       .collect(Collectors.toList());
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
      throw new IllegalArgumentException("applicationId must be a positive integer");
    }
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    return favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username) != null;
  }

  public long countFavorites(String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    return favoriteApplicationDAO.countFavoritesForUser(username);
  }

  public ApplicationImage saveAppImageFileItem(ApplicationImage defaultAppImage) throws Exception {
    if (defaultAppImage == null) {
      throw new IllegalArgumentException("Application image is mandatory");
    }
    if (defaultAppImage.getId() == null || defaultAppImage.getId() <= 0) {
      return createAppImageFileItem(defaultAppImage.getFileName(), defaultAppImage.getFileBody());
    } else {
      return updateAppImageFileItem(defaultAppImage.getId(), defaultAppImage.getFileName(), defaultAppImage.getFileBody());
    }
  }

  public ApplicationImage createAppImageFileItem(String fileName, String fileBody) throws Exception {
    return updateAppImageFileItem(null, fileName, fileBody);
  }

  public Long getApplicationImageLastUpdated(long fileId) throws FileStorageException {
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null && fileItem.getFileInfo().getUpdatedDate() != null) {
      return fileItem.getFileInfo().getUpdatedDate().getTime();
    }
    return null;
  }

  public InputStream getApplicationImageInputStream(long fileId) throws FileStorageException, IOException { // NOSONAR
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null && fileItem.getAsByte() != null) {
      return new ByteArrayInputStream(fileItem.getAsByte());
    }
    return null;
  }

  public ApplicationImage getAppImageFile(Long fileId) throws FileStorageException {
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
    List<ApplicationEntity> applications = applicationDAO.getApplications(keyword);
    List<Application> list = new ArrayList<>();
    for (ApplicationEntity entity : applications) {
      Application application = toDTO(entity);
      list.add(application);
    }
    return list;
  }

  public long countApplications() {
    return applicationDAO.count();
  }

  private Application toDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    String imageFileName = null;
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if(applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId()>0){
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if(fileInfo != null){
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
    application.setIsMobile(applicationEntity.isMobile());
    return application;
  }

  private UserApplication toUserApplicationDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    String imageFileName = null;
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if(applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId()>0){
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if(fileInfo != null){
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
    userApplication.setIsMobile(applicationEntity.isMobile());
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
    userApplication.setIsMobile(applicationEntity.isMobile());
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
    applicationEntity.setIsMobile(application.isMobile());
    return applicationEntity;
  }

  private ApplicationImage updateAppImageFileItem(Long fileId, String fileName, String fileBody) throws Exception { // NOSONAR
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
