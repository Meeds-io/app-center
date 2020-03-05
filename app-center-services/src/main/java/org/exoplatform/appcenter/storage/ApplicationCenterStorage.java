package org.exoplatform.appcenter.storage;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.appcenter.dao.ApplicationDAO;
import org.exoplatform.appcenter.dao.FavoriteApplicationDAO;
import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.appcenter.dto.ApplicationImage;
import org.exoplatform.appcenter.entity.ApplicationEntity;
import org.exoplatform.appcenter.entity.FavoriteApplicationEntity;
import org.exoplatform.appcenter.service.ApplicationNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.file.services.FileStorageException;

/**
 * Storage service to access / load and save applications. This service will be
 * used , as well, to convert from JPA entity to DTO.
 */
public class ApplicationCenterStorage {

  public static final String     NAME_SPACE = "appCenter";

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

  public Application getApplicationByTitleOrURL(String title, String url) {
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("title is mandatory");
    }
    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException("url is mandatory");
    }
    ApplicationEntity applicationentity = applicationDAO.getApplicationByTitleOrUrl(title, url);
    return toDTO(applicationentity);
  }

  public Application createApplication(Application applicationForm) throws Exception {
    if (applicationForm == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    ApplicationEntity application = toEntity(applicationForm);
    ApplicationImage applicationImage = createAppImageFileItem(applicationForm.getImageFileName(),
                                                               applicationForm.getImageFileBody());
    if (applicationImage != null) {
      application.setImageFileId(applicationImage.getId());
    }
    application = applicationDAO.create(application);
    return toDTO(application);
  }

  public Application updateApplication(Application application) throws Exception {
    if (application == null) {
      throw new IllegalArgumentException("application is mandatory");
    }
    Long applicationId = application.getId();
    ApplicationEntity storedApplicationentity = applicationDAO.find(applicationId);
    if (storedApplicationentity == null) {
      throw new ApplicationNotFoundException("Application with id " + applicationId + " wasn't found");
    }
    Long oldImageFileId = storedApplicationentity.getImageFileId();
    Long newImageFileId = application.getImageFileId();
    boolean newImageExists = application.getImageFileId() != null && application.getImageFileId() > 0;
    boolean oldImageFileExists = oldImageFileId != null && oldImageFileId > 0;
    boolean attachNewImage = false;
    boolean deleteOldImage = false;
    if (newImageExists) {
      if (oldImageFileExists && oldImageFileId.longValue() != newImageFileId.longValue()) {
        // we should update new image
        attachNewImage = true;
        // Delete old attached image to application
        deleteOldImage = true;
      }
    } else if (oldImageFileExists) {
      /*
       * Delete old attached image to application FIXME : For now, we keep the
       * old file as it is // NOSONAR deleteOldImage = true;
       */ // NOSONAR
    }
    if (attachNewImage) {
      ApplicationImage applicationImage = createAppImageFileItem(application.getImageFileName(), application.getImageFileBody());
      if (applicationImage != null) {
        application.setImageFileId(applicationImage.getId());
      }
    }
    ApplicationEntity applicationEntity = toEntity(application);
    applicationEntity = applicationDAO.update(applicationEntity);

    // Cleanup old useless image
    if (deleteOldImage) {
      fileService.deleteFile(oldImageFileId);
    }
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

  public List<Application> getFavoriteApplicationsByUser(String username) {
    if (StringUtils.isBlank(username)) {
      throw new IllegalArgumentException("username is mandatory");
    }
    List<FavoriteApplicationEntity> applications = favoriteApplicationDAO.getFavoriteApps(username);
    return applications.stream()
                       .map(FavoriteApplicationEntity::getApplication)
                       .map(this::toDTO)
                       .collect(Collectors.toList());
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

  public ApplicationImage getAppImageFile(Long fileId) throws FileStorageException {
    FileItem fileItem = fileService.getFile(fileId);
    if (fileItem != null) {
      byte[] bytes = fileItem.getAsByte();
      String fileBody = new String(bytes, Charset.defaultCharset());
      String fileMime = fileItem.getFileInfo().getMimetype();
      String fileName = fileItem.getFileInfo().getName();

      return new ApplicationImage(fileId,
                                  fileName,
                                  "data:" + fileMime + ";base64," + fileBody);
    }
    return null;
  }

  public List<Application> getApplications(String keyword, int offset, int limit) {
    List<ApplicationEntity> applicatiions = applicationDAO.getApplications(keyword, offset, limit);
    return applicatiions.stream().map(this::toDTO).collect(Collectors.toList());
  }

  public long countApplications() {
    return applicationDAO.count();
  }

  private Application toDTO(ApplicationEntity applicationEntity) {
    if (applicationEntity == null) {
      return null;
    }
    String[] permissions = StringUtils.split(applicationEntity.getPermissions(), ",");
    return new Application(applicationEntity.getId(),
                           applicationEntity.getTitle(),
                           applicationEntity.getUrl(),
                           applicationEntity.getImageFileId(),
                           null,
                           null,
                           applicationEntity.getDescription(),
                           applicationEntity.isActive(),
                           applicationEntity.isByDefault(),
                           permissions);
  }

  private ApplicationEntity toEntity(Application application) {
    if (application == null) {
      return null;
    }
    return new ApplicationEntity(application.getId(),
                                 application.getTitle(),
                                 application.getUrl(),
                                 application.getImageFileId(),
                                 application.getDescription(),
                                 application.isActive(),
                                 application.isByDefault(),
                                 StringUtils.join(application.getPermissions(), ","));
  }

  private ApplicationImage updateAppImageFileItem(Long fileId, String fileName, String fileBody) throws Exception { // NOSONAR
    if (StringUtils.isBlank(fileName) || StringUtils.isBlank(fileBody)) {
      return null;
    }

    String fileContent = fileBody;
    String fileMime = "image/png";
    if (fileBody.contains(",")) {
      String[] file = fileBody.split(",");
      fileMime = file[0].replace("data:", "").replace(";base64", "");
      fileContent = file[1];
    }

    Date currentDate = new Date();
    long size = 0;
    FileItem fileItem = new FileItem(fileId,
                                     fileName,
                                     fileMime,
                                     NAME_SPACE,
                                     size,
                                     currentDate,
                                     null,
                                     false,
                                     new ByteArrayInputStream(fileContent.getBytes(Charset.defaultCharset().name())));
    if (fileId != null && fileId > 0) {
      fileItem = fileService.updateFile(fileItem);
    } else {
      fileItem = fileService.writeFile(fileItem);
    }
    Long id = fileItem == null || fileItem.getFileInfo() == null ? null : fileItem.getFileInfo().getId();
    return new ApplicationImage(id, fileName, fileBody);
  }

}
