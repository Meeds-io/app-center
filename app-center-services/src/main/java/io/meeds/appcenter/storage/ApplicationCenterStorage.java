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
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.entity.FavoriteApplicationEntity;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationForm;
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

  public Application createApplication(Application application) {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity.setId(null);

    if (application instanceof ApplicationForm applicationForm && StringUtils.isNotBlank(applicationForm.getImageUploadId())) {
      Long imageFileId = saveImageFileItem(null, applicationForm.getImageUploadId());
      applicationEntity.setImageFileId(imageFileId);
    }

    applicationEntity = applicationDAO.save(applicationEntity);
    return getApplication(applicationEntity.getId());
  }

  @CacheEvict(cacheNames = "app-center.application", key = "#p0.getId()")
  public void updateApplication(Application application) throws ApplicationNotFoundException {
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
    boolean imageRemoved = (application.getImageFileId() == null || application.getImageFileId() == 0)
                           && oldImageFileId != null
                           && oldImageFileId > 0;
    application.setImageFileId(oldImageFileId);
    if (imageRemoved) {
      application.setImageFileId(null);
      // Cleanup old useless image
      fileService.deleteFile(oldImageFileId);
    } else if (application instanceof ApplicationForm applicationForm
               && StringUtils.isNotBlank(applicationForm.getImageUploadId())) {
      Long imageFileId = saveImageFileItem(oldImageFileId, applicationForm.getImageUploadId());
      application.setImageFileId(imageFileId);
    }

    // if application is mandatory make sure to remove it from users favorites
    if (application.isMandatory() && !storedApplicationEntity.isMandatory()) {
      favoriteApplicationDAO.removeAllFavoritesOfApplication(application.getId());
    }

    ApplicationEntity applicationEntity = toEntity(application);
    applicationDAO.save(applicationEntity);
  }

  @CacheEvict(cacheNames = "app-center.application", key = "#p0")
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

  @Cacheable("app-center.application")
  public Application getApplication(long applicationId) {
    ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
    return toDTO(applicationEntity);
  }

  public Application findSystemApplicationByUrl(String url) {
    List<ApplicationEntity> list = applicationDAO.findBySystemIsTrueAndUrl(url);
    ApplicationEntity applicationEntity = CollectionUtils.isEmpty(list) ? null : list.get(0);
    return applicationEntity == null ? null : getApplication(applicationEntity.getId());
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

  public List<Application> getSystemApplications() {
    return applicationDAO.getSystemApplicationIds()
                         .stream()
                         .map(this::getApplication)
                         .filter(Objects::nonNull)
                         .toList();
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

  public List<Application> getApplications(String keyword) {
    List<Long> applicationIds = StringUtils.isBlank(keyword) ? applicationDAO.getApplicationIds() :
                                                             applicationDAO.getApplicationIds(keyword);
    return applicationIds.stream()
                         .map(this::getApplication)
                         .filter(Objects::nonNull)
                         .toList();
  }

  public long countApplications() {
    return applicationDAO.count();
  }

  public List<UserApplication> getMandatoryAndFavoriteApplications(String username, Pageable pageable) {
    return applicationDAO.findFavoriteAndMandatoryApplications(username, pageable)
                         .map(this::toUserApplicationDTO)
                         .getContent();
  }

  @SneakyThrows
  private Long saveImageFileItem(Long imageFileId, String uploadId) {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    byte[] bytesContent = IOUtil.getFileContentAsBytes(uploadResource.getStoreLocation());
    FileItem fileItem = new FileItem(imageFileId,
                                     "appCenterIllustration",
                                     "image/png",
                                     NAME_SPACE,
                                     bytesContent.length,
                                     new Date(),
                                     null,
                                     false,
                                     new ByteArrayInputStream(bytesContent));
    if (imageFileId != null && imageFileId > 0) {
      fileItem = fileService.updateFile(fileItem);
    } else {
      fileItem = fileService.writeFile(fileItem);
    }
    return fileItem == null || fileItem.getFileInfo() == null ? null : fileItem.getFileInfo().getId();
  }

  private Application toDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    long imageLastModified = DEFAULT_LAST_MODIFIED;
    if (applicationEntity.getImageFileId() != null && applicationEntity.getImageFileId() > 0) {
      FileInfo fileInfo = fileService.getFileInfo(applicationEntity.getImageFileId());
      if (fileInfo != null && fileInfo.getUpdatedDate() != null) {
        imageLastModified = fileInfo.getUpdatedDate().getTime();
      }
    }
    return new Application(applicationEntity.getId(),
                           applicationEntity.getTitle(),
                           applicationEntity.getUrl(),
                           applicationEntity.getHelpPageUrl(),
                           applicationEntity.getDescription(),
                           applicationEntity.getShortcut(),
                           applicationEntity.getType(),
                           applicationEntity.isActive(),
                           applicationEntity.isMandatory(),
                           applicationEntity.isDefault(),
                           applicationEntity.isMobile(),
                           applicationEntity.isSystem(),
                           applicationEntity.isPwa(),
                           applicationEntity.getPermissions(),
                           null,
                           applicationEntity.getImageFileId(),
                           applicationEntity.getIcon(),
                           getImageUrl(applicationEntity.getImageFileId(), applicationEntity.getId(), imageLastModified),
                           0l,
                           applicationEntity.isChangedManually());
  }

  private UserApplication toUserApplicationDTO(Long applicationId) {
    Application application = getApplication(applicationId);
    return application == null ? null : new UserApplication(application);
  }

  private UserApplication toUserApplicationDTO(FavoriteApplicationEntity favoriteApplicationEntity) {
    if (favoriteApplicationEntity == null || favoriteApplicationEntity.getApplication() == null) {
      return null;
    } else {
      UserApplication userApplication = toUserApplicationDTO(favoriteApplicationEntity.getApplication().getId());
      if (userApplication == null) {
        return null;
      }
      userApplication.setOrder(favoriteApplicationEntity.getOrder());
      userApplication.setFavorite(true);
      return userApplication;
    }
  }

  private ApplicationEntity toEntity(Application application) {
    if (application == null) {
      return null;
    } else {
      return new ApplicationEntity(application.getId(),
                                   application.getTitle(),
                                   application.getDescription(),
                                   application.getType(),
                                   application.getUrl(),
                                   application.getIcon(),
                                   application.getHelpPageURL(),
                                   application.getImageFileId(),
                                   application.isActive(),
                                   application.isDefault(),
                                   application.isMandatory(),
                                   application.isMobile(),
                                   application.isSystem(),
                                   application.isPwa(),
                                   application.getShortcut(),
                                   application.getPermissions(),
                                   application.isChangedManually(),
                                   null);
    }
  }

  private String getImageUrl(Long imageFileId, Long id, long imageLastModified) {
    if (imageFileId == null || imageFileId.longValue() == 0) {
      return null;
    } else {
      return String.format("/app-center/rest/applications/illustration/%s?v=%s", id, imageLastModified);
    }
  }

}
