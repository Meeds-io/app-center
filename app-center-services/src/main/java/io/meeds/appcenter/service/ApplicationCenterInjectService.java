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
package io.meeds.appcenter.service;

import static io.meeds.appcenter.service.ApplicationCenterService.APP_CENTER_CONTEXT;
import static io.meeds.appcenter.service.ApplicationCenterService.APP_CENTER_SCOPE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationDescriptor;
import io.meeds.appcenter.model.ApplicationDescriptorList;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.common.ContainerTransactional;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * A Service to inject applications at startup time
 */
@Component
public class ApplicationCenterInjectService {

  private static final Log                   LOG                       =
                                                 ExoLogger.getLogger(ApplicationCenterInjectService.class);

  private static final String                APP_CENTER_APPS_VERSION   = "6";

  private static final String                MERGE_MODE                = "merge";

  private static final String                APP_CENTER_SYSTEM_APP_KEY = "systemApps";

  @Autowired
  private ConfigurationManager               configurationManager;

  @Autowired
  private UploadService                      uploadService;

  @Autowired
  private ApplicationCenterService           applicationCenterService;

  @Autowired
  private SettingService                     settingService;

  @Getter
  private Map<String, ApplicationDescriptor> defaultApplications       = new LinkedHashMap<>();

  @PostConstruct
  public void init() {
    CompletableFuture.runAsync(this::initTransactional);
  }

  @ContainerTransactional
  public void initTransactional() {
    injectDefaultApplications();
  }

  /**
   * Inject a default application using IOC {@link ComponentPlugin} using
   * configuration
   *
   * @param applicationPlugin plugin containing application to inject
   */
  public void addApplicationPlugin(ApplicationDescriptor applicationPlugin) {
    if (applicationPlugin == null) {
      throw new IllegalArgumentException("'applicationPlugin' is mandatory");
    }
    if (StringUtils.isBlank(applicationPlugin.getName())) {
      throw new IllegalStateException("'applicationPlugin' name is mandatory");
    }
    this.defaultApplications.put(applicationPlugin.getName(), applicationPlugin);
  }

  /**
   * Delete an injected plugin identified by its name
   *
   * @param pluginName plugin name to delete
   */
  public void removeApplicationPlugin(String pluginName) {
    if (StringUtils.isBlank(pluginName)) {
      throw new IllegalArgumentException("'pluginName' is mandatory");
    }
    this.defaultApplications.remove(pluginName);
  }

  /**
   * Checks whether the application is a system application injected by
   * configuration or not
   *
   * @param application application to check its state
   * @return true if the configuration of the application exists with same title
   *         and URL, else false.
   */
  public boolean isDefaultSystemApplication(Application application) {
    if (application == null) {
      throw new IllegalArgumentException("'application' is mandatory");
    }
    return this.defaultApplications.values()
                                   .stream()
                                   .filter(ApplicationDescriptor::isEnabled)
                                   .anyMatch(app -> StringUtils.equals(app.getApplication().getTitle(), application.getTitle())
                                                    && StringUtils.equals(app.getApplication().getUrl(), application.getUrl()));
  }

  protected void injectDefaultApplications() {
    try {
      readDescriptorsFromFiles();
      deleteRemovedSystemApplications();
      defaultApplications.values()
                         .stream()
                         .filter(ApplicationDescriptor::isEnabled)
                         .forEach(applicationDescriptor -> {
                           try {
                             this.injectDefaultApplication(applicationDescriptor);
                           } catch (Exception e) {
                             LOG.warn("An error occurred while reimporting system application {}",
                                      applicationDescriptor.getName(),
                                      e);
                           }
                         });
      saveStoredApplicationsVersion();
    } catch (Exception e) {
      LOG.warn("An unknown error occurs while reimporting system applications", e);
    }
  }

  private void readDescriptorsFromFiles() throws IOException {
    Enumeration<URL> descriptorFiles = getClass().getClassLoader()
                                                 .getResources("applications.json");
    Collections.list(descriptorFiles)
               .stream()
               .map(this::parseDescriptors)
               .flatMap(List::stream)
               .forEach(d -> defaultApplications.put(d.getName(), d));
  }

  private List<ApplicationDescriptor> parseDescriptors(URL url) {
    try (InputStream inputStream = url.openStream()) {
      String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      ApplicationDescriptorList list = JsonUtils.fromJsonString(content, ApplicationDescriptorList.class);
      return list.getDescriptors();
    } catch (IOException e) {
      LOG.warn("An unkown error happened while parsing application descriptors from url {}", url, e);
      return Collections.emptyList();
    }
  }

  private void deleteRemovedSystemApplications() {
    List<Application> systemApplications = applicationCenterService.getSystemApplications();
    systemApplications.forEach(application -> {
      if (!isDefaultSystemApplication(application)) {
        try {
          LOG.info("Delete application '{}' that was previously injected as system application and that doesn't exist in configuration anymore",
                   application.getTitle());
          applicationCenterService.deleteApplication(application.getId());
        } catch (Exception e) {
          LOG.warn("An unknown error occurs while deleting not found system application '{}' in store",
                   application.getTitle(),
                   e);
        }
      }
    });
  }

  private void injectDefaultApplication(ApplicationDescriptor applicationDescriptor) { // NOSONAR
    Application application = applicationDescriptor.getApplication();
    String pluginName = applicationDescriptor.getName();
    if (application == null) {
      LOG.warn("An application plugin '{}' holds an empty application", pluginName);
      return;
    }

    String title = application.getTitle();
    if (StringUtils.isBlank(title)) {
      LOG.warn("Plugin '{}' has an application with empty title, it will not be injected", pluginName);
      return;
    }

    String url = application.getUrl();
    if (StringUtils.isBlank(url)) {
      LOG.warn("Plugin '{}' has an application with empty url, it will not be injected", pluginName);
      return;
    }

    long storedAppId = getStoredApplicationId(pluginName);
    Application storedApplication = storedAppId == 0l ? null : applicationCenterService.getApplication(storedAppId);
    if (storedApplication == null) {
      storedApplication = applicationCenterService.findSystemApplicationByUrl(url);
    }

    String version = getStoredApplicationsVersion();
    if (storedApplication != null
        && !applicationDescriptor.isOverride()
        && StringUtils.equals(APP_CENTER_APPS_VERSION, version)
        && storedApplication.isChangedManually()
        && (MERGE_MODE.equals(applicationDescriptor.getOverrideMode()) || applicationDescriptor.getOverrideMode() == null)) {
      LOG.info("Ignore updating system application '{}', override flag is turned off", application.getTitle());
      return;
    }

    if (application.getType() == null) {
      application.setType(ApplicationType.LINK);
    }

    ApplicationForm applicationForm = new ApplicationForm(application);
    String imagePath = applicationDescriptor.getImagePath();
    if (StringUtils.isNotBlank(imagePath)) {
      String uploadId = UUID.randomUUID().toString();
      try {
        URL resource = configurationManager.getURL(imagePath);
        if (resource == null) {
          LOG.warn("Application Image with path {} doesn't exist", imagePath);
        } else {
          File file = new File(resource.getFile());
          UploadResource uploadResource = new UploadResource(uploadId,
                                                             file.getName(),
                                                             "image/png",
                                                             file.getAbsolutePath(),
                                                             0,
                                                             0,
                                                             UploadResource.UPLOADED_STATUS);
          uploadService.createUploadResource(uploadResource);
          applicationForm.setImageUploadId(uploadId);
        }
      } catch (Exception e) {
        LOG.warn("Error reading image from file {}. Application will be injected without image", imagePath, e);
      }
    }

    if (storedApplication == null) {
      try {
        LOG.info("Create system application '{}'", applicationForm.getTitle());
        applicationForm.setSystem(true);
        applicationForm.setChangedManually(false);
        applicationForm.setImageFileId(null);
        storedApplication = applicationCenterService.createApplication(applicationForm);
        if (storedApplication != null) {
          saveStoredApplicationId(pluginName, storedApplication.getId());
        }
      } catch (Exception e) {
        LOG.error("Error creating application {}", applicationForm, e);
      }
    } else {
      try {
        LOG.info("Update system application '{}'", applicationForm.getTitle());
        applicationForm.setSystem(true);
        applicationForm.setChangedManually(false);
        applicationForm.setId(storedApplication.getId());
        applicationForm.setImageFileId(storedApplication.getImageFileId());
        applicationCenterService.updateApplication(applicationForm);
        saveStoredApplicationId(pluginName, storedApplication.getId());
      } catch (Exception e) {
        LOG.error("Error updating application {}", applicationForm, e);
      }
    }
  }

  private long getStoredApplicationId(String pluginName) {
    SettingValue<?> settingValue = settingService.get(APP_CENTER_CONTEXT,
                                                      APP_CENTER_SCOPE,
                                                      APP_CENTER_SYSTEM_APP_KEY + pluginName);
    String appId = settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
    return StringUtils.isBlank(appId) ? 0l : Long.parseLong(appId);
  }

  private void saveStoredApplicationId(String pluginName, long appId) {
    settingService.set(APP_CENTER_CONTEXT,
                       APP_CENTER_SCOPE,
                       APP_CENTER_SYSTEM_APP_KEY + pluginName,
                       SettingValue.create(appId));
  }

  private String getStoredApplicationsVersion() {
    SettingValue<?> settingValue = settingService.get(APP_CENTER_CONTEXT,
                                                      APP_CENTER_SCOPE,
                                                      APP_CENTER_SYSTEM_APP_KEY);
    return settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
  }

  private void saveStoredApplicationsVersion() {
    settingService.set(APP_CENTER_CONTEXT,
                       APP_CENTER_SCOPE,
                       APP_CENTER_SYSTEM_APP_KEY,
                       SettingValue.create(APP_CENTER_APPS_VERSION));
  }

}
