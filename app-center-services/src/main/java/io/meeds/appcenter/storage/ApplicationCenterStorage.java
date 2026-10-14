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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.security.codec.CodecInitializer;

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

  public static final String     NAME_SPACE                          = "appCenter";

  public static final Long       DEFAULT_LAST_MODIFIED               = System.currentTimeMillis();

  private static final Log       LOG                                 = ExoLogger.getLogger(ApplicationCenterStorage.class);

  private static final String    APPLICATION_NOT_FOUND_MESSAGE       = "Application with id %s doesn't exist";

  private static final String    ILLUSTRATION_TOKEN_PROVIDER         = "appcenter";

  private static final String    ILLUSTRATION_TOKEN_TYPE             = "icon";

  private static final String    APPLICATION_ID_IS_MANDATORY_MESSAGE = "applicationId is mandatory";

  private static final String    USERNAME_IS_MANDATORY_MESSAGE       = "username is mandatory";

  @Autowired
  private FileService            fileService;

  @Autowired
  private UploadService          uploadService;

  @Autowired
  private ApplicationDAO         applicationDAO;

  @Autowired
  private FavoriteApplicationDAO favoriteApplicationDAO;

  @Autowired(required = false)
  private CodecInitializer       codecInitializer;

  /**
   * Self reference, so that a lookup derived from a {@code @Cacheable} method
   * of this same bean goes through the Spring proxy. Calling it on {@code this}
   * bypasses the caching aspect entirely: correct results, silently no cache.
   */
  @Autowired
  @Lazy
  private ApplicationCenterStorage self;

  /**
   * Creates an application of the Application Center: whatever the
   * application carries, it is owned by no Shortcuts portlet window.
   *
   * @param application the application to create
   * @return the created application
   */
  public Application createApplication(Application application) {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity.setId(null);
    applicationEntity.setContextSettingName(null);
    applicationEntity.setContextSpaceId(null);

    if (application instanceof ApplicationForm applicationForm && StringUtils.isNotBlank(applicationForm.getImageUploadId())) {
      Long imageFileId = saveImageFileItem(null, applicationForm.getImageUploadId(), applicationForm.getIllustrationMimeType());
      applicationEntity.setImageFileId(imageFileId);
    }

    applicationEntity = applicationDAO.save(applicationEntity);
    return getApplication(applicationEntity.getId());
  }

  /**
   * Creates an application owned by a Shortcuts portlet window. The
   * application is stored as given, its illustration file id included: the
   * caller decides every field, no upload is read here. When the database
   * refuses the application, its illustration is deleted.
   *
   * @param application the application to create
   * @param contextSettingName the CMS setting name of the owning window
   * @param contextSpaceId the space of the window's context, 0 for the
   *          platform context
   * @return the created application
   */
  public Application createContextApplication(Application application, String contextSettingName, long contextSpaceId) {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    } else if (StringUtils.isBlank(contextSettingName)) {
      throw new IllegalArgumentException("contextSettingName is mandatory");
    }
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity.setId(null);
    applicationEntity.setContextSettingName(contextSettingName);
    applicationEntity.setContextSpaceId(contextSpaceId);
    try {
      applicationEntity = applicationDAO.save(applicationEntity);
    } catch (RuntimeException e) {
      // No illustration is left behind an application the database refused
      if (application.getImageFileId() != null) {
        fileService.deleteFile(application.getImageFileId());
      }
      throw e;
    }
    return getApplication(applicationEntity.getId());
  }

  /**
   * Updates an application owned by a Shortcuts portlet window. The
   * application is stored as given, its illustration file id included: the
   * caller decides every field, no upload is read here. Its owner and system
   * flag stay the stored ones. The illustration it replaces or removes is
   * deleted once the application is saved; when the database refuses the
   * application, the new illustration is deleted instead.
   *
   * @param application the application to update
   */
  @CacheEvict(cacheNames = "app-center.application", key = "#p0.getId()")
  public void updateContextApplication(Application application) {
    ApplicationEntity storedApplicationEntity = applicationDAO.findById(application.getId()).orElseThrow();
    Long oldImageFileId = storedApplicationEntity.getImageFileId();
    Long imageFileId = application.getImageFileId();
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity.setSystem(storedApplicationEntity.isSystem());
    applicationEntity.setContextSettingName(storedApplicationEntity.getContextSettingName());
    applicationEntity.setContextSpaceId(storedApplicationEntity.getContextSpaceId());
    try {
      applicationDAO.save(applicationEntity);
    } catch (RuntimeException e) {
      if (imageFileId != null && !imageFileId.equals(oldImageFileId)) {
        fileService.deleteFile(imageFileId);
      }
      throw e;
    }
    if (oldImageFileId != null && !oldImageFileId.equals(imageFileId)) {
      try {
        fileService.deleteFile(oldImageFileId);
      } catch (RuntimeException e) {
        // The application is updated: its former illustration is only left behind
        LOG.warn("Unable to delete the former illustration {} of application {}", oldImageFileId, application.getId(), e);
      }
    }
  }

  @CacheEvict(cacheNames = "app-center.application", key = "#p0.getId()")
  public void updateApplication(Application application) {
    Long applicationId = application.getId();
    ApplicationEntity storedApplicationEntity = applicationDAO.findById(applicationId).orElseThrow();
    // Avoid changing these fields by UI
    application.setSystem(storedApplicationEntity.isSystem());
    application.setContextSettingName(storedApplicationEntity.getContextSettingName());
    application.setContextSpaceId(storedApplicationEntity.getContextSpaceId());
    Long oldImageFileId = storedApplicationEntity.getImageFileId();
    boolean imageRemoved = (application.getImageFileId() == null || application.getImageFileId() == 0)
                           && oldImageFileId != null
                           && oldImageFileId > 0;
    application.setImageFileId(oldImageFileId);
    if (imageRemoved) {
      application.setImageFileId(null);
      // Cleanup old useless image
      fileService.deleteFile(oldImageFileId);
    }
    if (application instanceof ApplicationForm applicationForm
        && StringUtils.isNotBlank(applicationForm.getImageUploadId())) {
      Long imageFileId = saveImageFileItem(oldImageFileId, applicationForm.getImageUploadId(), applicationForm.getIllustrationMimeType());
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

  /**
   * Retrieves the applications a badge is bound to, either explicitly through
   * their stored badge name, or implicitly through a url the plugin declares.
   *
   * @param  badgeName the badge identifier
   * @param  urls      the drawer and/or portlet names declared by the plugin,
   *                     possibly empty
   * @return           the bound applications, never null
   */
  public List<Application> getApplicationsByBadge(String badgeName, List<String> urls) {
    List<Long> applicationIds = CollectionUtils.isEmpty(urls) ? applicationDAO.getApplicationIdsByBadgeName(badgeName)
                                                              : applicationDAO.getApplicationIdsByBadge(badgeName, urls);
    // Through the proxy, otherwise the @Cacheable on getApplication never runs:
    // this is the badge ACL check, on the read path of a badge that every user
    // hits on every page, and it would cost a findById plus a file lookup per
    // bound application every time. Callers here only read the applications,
    // never mutate them, so serving the cached instances is safe.
    return applicationIds.stream().map(self::getApplication).filter(Objects::nonNull).toList();
  }

  public Application findSystemApplicationByUrl(String url) {
    List<ApplicationEntity> list = applicationDAO.findBySystemIsTrueAndUrl(url);
    ApplicationEntity applicationEntity = CollectionUtils.isEmpty(list) ? null : list.get(0);
    return applicationEntity == null ? null : getApplication(applicationEntity.getId());
  }

  public FavoriteApplicationEntity addApplicationToUserFavorite(long applicationId,
                                                                String username) throws ApplicationNotFoundException {
    if (applicationId <= 0) {
      throw new IllegalArgumentException(APPLICATION_ID_IS_MANDATORY_MESSAGE);
    }
    FavoriteApplicationEntity applicationFavorite = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId,
                                                                                                            username);
    if (applicationFavorite == null) {
      ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
      if (applicationEntity == null) {
        throw new ApplicationNotFoundException(String.format(APPLICATION_NOT_FOUND_MESSAGE, applicationId));
      }
      applicationFavorite =
                          favoriteApplicationDAO.save(new FavoriteApplicationEntity(null, applicationEntity, username, 0l, true));
    } else if (applicationFavorite.getFavorite() == null || !applicationFavorite.getFavorite().booleanValue()) {
      applicationFavorite.setFavorite(true);
      applicationFavorite = favoriteApplicationDAO.save(applicationFavorite);
    }
    return applicationFavorite;
  }

  public void updateFavoriteApplicationOrder(long applicationId,
                                             String username,
                                             Long order) throws ApplicationNotFoundException {
    FavoriteApplicationEntity entity = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId, username);
    if (entity == null) {
      entity = addApplicationToUserFavorite(applicationId, username);
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
    if (applicationFavorite == null) {
      ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
      if (applicationEntity != null) {
        favoriteApplicationDAO.save(new FavoriteApplicationEntity(null, applicationEntity, username, 0l, false));
      }
    } else {
      applicationFavorite.setFavorite(false);
      favoriteApplicationDAO.save(applicationFavorite);
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
    FavoriteApplicationEntity applicationFavorite = favoriteApplicationDAO.getFavoriteAppByUserNameAndAppId(applicationId,
                                                                                                            username);
    if (applicationFavorite != null) {
      return applicationFavorite.getFavorite().booleanValue();
    } else {
      ApplicationEntity applicationEntity = applicationDAO.findById(applicationId).orElse(null);
      return applicationEntity != null && applicationEntity.isDefault();
    }
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

  /**
   * @param id the application's id
   * @param token the token of an illustration URL
   * @param imageLastModified the version of the illustration the URL names
   * @return true when the token is the one the application's illustration URL
   *         carries for that version
   */
  public boolean isIllustrationTokenValid(long id, String token, long imageLastModified) {
    return LinkProvider.isAttachmentTokenValid(token,
                                               ILLUSTRATION_TOKEN_PROVIDER,
                                               String.valueOf(id),
                                               ILLUSTRATION_TOKEN_TYPE,
                                               String.valueOf(imageLastModified));
  }

  /**
   * @param  keyword matched in title and url, all applications when blank
   * @return         the ids of the matching applications, sorted by title
   */
  public List<Long> getApplicationIds(String keyword) {
    return StringUtils.isBlank(keyword) ? applicationDAO.getApplicationIds() : applicationDAO.getApplicationIds(keyword);
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

  /**
   * @param spaceId a space id
   * @return the ids of the applications created by Shortcuts portlet windows
   *         in that space's context
   */
  public List<Long> getContextApplicationIds(long spaceId) {
    return applicationDAO.findIdsByContextSpaceId(spaceId);
  }

  /**
   * Reads the start of an upload, at most one byte more than the given bound,
   * so that a caller tells an upload above the bound without reading it whole.
   *
   * @param uploadId the upload id
   * @param maxBytes the bound
   * @return at most maxBytes + 1 bytes of the upload, null when no completed
   *         upload has that id
   */
  public byte[] readUpload(String uploadId, int maxBytes) {
    UploadResource uploadResource = StringUtils.isBlank(uploadId) ? null : uploadService.getUploadResource(uploadId);
    if (uploadResource == null
        || uploadResource.getStatus() != UploadResource.UPLOADED_STATUS
        || StringUtils.isBlank(uploadResource.getStoreLocation())) {
      return null; // NOSONAR
    }
    Path storeLocation = Path.of(uploadResource.getStoreLocation());
    if (!Files.isRegularFile(storeLocation)) {
      return null; // NOSONAR
    }
    try (InputStream inputStream = Files.newInputStream(storeLocation)) {
      return inputStream.readNBytes(maxBytes + 1);
    } catch (IOException e) {
      LOG.debug("Unable to read upload {}", uploadId, e);
      return null; // NOSONAR
    }
  }

  /**
   * Removes an upload once its content is stored.
   *
   * @param uploadId the upload id
   */
  public void removeUpload(String uploadId) {
    if (StringUtils.isNotBlank(uploadId)) {
      uploadService.removeUploadResource(uploadId);
    }
  }

  /**
   * Stores an application illustration.
   *
   * @param content the image content
   * @param mimeType the image type, decided from the content
   * @return the id of the stored file
   */
  public Long saveIllustration(byte[] content, String mimeType) {
    return saveImageFileItem(null, content, mimeType);
  }

  /**
   * Deletes an application illustration that no application uses.
   *
   * @param fileId the id of the stored file
   */
  public void deleteIllustration(long fileId) {
    fileService.deleteFile(fileId);
  }

  public List<UserApplication> getMandatoryAndFavoriteApplications(String username, Pageable pageable) {
    return applicationDAO.findFavoriteAndMandatoryApplications(username, pageable)
                         .map(this::toUserApplicationDTO)
                         .getContent();
  }

  @SneakyThrows
  private Long saveImageFileItem(Long imageFileId, String uploadId, String mimeType) {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    byte[] bytesContent = IOUtil.getFileContentAsBytes(uploadResource.getStoreLocation());
    return saveImageFileItem(imageFileId, bytesContent, mimeType);
  }

  @SneakyThrows
  private Long saveImageFileItem(Long imageFileId, byte[] bytesContent, String mimeType) {
    FileItem fileItem = new FileItem(imageFileId,
                                     "appCenterIllustration",
                                     mimeType,
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
                           applicationEntity.isSameTab(),
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
                           applicationEntity.getOrder(),
                           applicationEntity.isChangedManually(),
                           applicationEntity.isPersonal(),
                           applicationEntity.getBadgeName(),
                           applicationEntity.isAllowStick(),
                           applicationEntity.isAllowDetach(),
                           applicationEntity.getContextSettingName(),
                           applicationEntity.getContextSpaceId());
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
      if (favoriteApplicationEntity.getOrder() != null) {
        userApplication.setOrder(favoriteApplicationEntity.getOrder());
      }
      userApplication.setFavorite(favoriteApplicationEntity.getFavorite()
          == null ? favoriteApplicationEntity.getApplication().isDefault() :
                  favoriteApplicationEntity.getFavorite().booleanValue());
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
                                   application.isSameTab(),
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
                                   application.getOrder(),
                                   application.isPersonal(),
                                   application.getBadgeName(),
                                   application.isAllowStick(),
                                   application.isAllowDetach(),
                                   application.getContextSettingName(),
                                   application.getContextSpaceId(),
                                   null);
    }
  }

  private String getImageUrl(Long imageFileId, Long id, long imageLastModified) {
    if (imageFileId == null || imageFileId.longValue() == 0) {
      return null;
    } else {
      return String.format("/app-center/rest/applications/illustration/%s?v=%s&r=%s",
                           id,
                           imageLastModified,
                           generateToken(id, imageLastModified));
    }
  }

  private String generateToken(Long id, long imageLastModified) {
    if (codecInitializer == null) {
      return StringUtils.EMPTY;
    } else {
      return URLEncoder.encode(LinkProvider.generateAttachmentToken(ILLUSTRATION_TOKEN_PROVIDER,
                                                                    String.valueOf(id),
                                                                    ILLUSTRATION_TOKEN_TYPE,
                                                                    String.valueOf(imageLastModified)),
                               StandardCharsets.UTF_8);
    }
  }

}
