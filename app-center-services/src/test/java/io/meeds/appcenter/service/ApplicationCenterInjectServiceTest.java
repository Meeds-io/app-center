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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationDescriptor;

import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationCenterInjectService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationCenterInjectServiceTest {

  private static final String            TEST_APPLICATION_TITLE = "test-application-injection";

  private static final String            SHORTCUT               = "G";

  private static final Random            RANDOM                 = new Random();

  private static final long              IMAGE_FILE_ID          = 5l;

  private static final String            HELP_PAGE_URL          = "./helpPageUrl";

  private static final String            URL                    = "./url";

  private static final String            PERMISSIONS_1          = "/permissions1";

  private static final String            DESCRIPTION            = "description";

  private static final String            TITLE                  = "title";

  private static final Long              ID                     = 2l;

  @MockBean
  private ConfigurationManager           configurationManager;

  @MockBean
  private SettingService                 settingService;

  @MockBean
  private ApplicationCenterService       applicationCenterService;

  @MockBean
  private UploadService                  uploadService;

  @Autowired
  private ApplicationCenterInjectService applicationCenterInjectService;

  @Test
  @SneakyThrows
  void injectDefaultApplications() {
    assertThrows(IllegalArgumentException.class, () -> applicationCenterInjectService.addApplicationPlugin(null));

    applicationCenterInjectService.injectDefaultApplications();
    assertTrue(applicationCenterInjectService.getDefaultApplications().size() >= 1);

    String pluginName = "testapp";

    Application application = application();
    ApplicationDescriptor applicationPlugin1 = new ApplicationDescriptor(null, application);
    assertThrows(IllegalStateException.class, () -> applicationCenterInjectService.addApplicationPlugin(applicationPlugin1));
    applicationPlugin1.setName(pluginName);
    applicationCenterInjectService.addApplicationPlugin(applicationPlugin1);
    applicationCenterInjectService.injectDefaultApplications();
    verify(applicationCenterService,
           never()).createApplication(argThat(app -> app.getTitle().equals(TEST_APPLICATION_TITLE)));

    when(applicationCenterService.createApplication(argThat(app -> app.getTitle()
                                                                      .equals(TITLE)))).thenAnswer(invocation -> {
                                                                        Application app = invocation.getArgument(0);
                                                                        app.setId(RANDOM.nextLong());
                                                                        return app;
                                                                      });

    applicationPlugin1.setEnabled(true);
    try {
      applicationCenterInjectService.addApplicationPlugin(applicationPlugin1);
      applicationCenterInjectService.injectDefaultApplications();
      verify(applicationCenterService).createApplication(argThat(app -> app.getTitle().equals(TITLE)));
    } finally {
      applicationCenterInjectService.removeApplicationPlugin(pluginName);
    }

    ApplicationDescriptor applicationPlugin2 = new ApplicationDescriptor(pluginName,
                                                                         application,
                                                                         "jar:/test.png",
                                                                         "write",
                                                                         true,
                                                                         true);
    try {
      applicationCenterInjectService.addApplicationPlugin(applicationPlugin2);
      when(applicationCenterService.findSystemApplicationByUrl(application.getUrl())).thenReturn(application);
      application.setChangedManually(true);
      applicationCenterInjectService.injectDefaultApplications();
      verify(applicationCenterService).updateApplication(any());
    } finally {
      applicationCenterInjectService.removeApplicationPlugin(pluginName);
    }

    // Third start with file attached and override-mode is merge
    applicationPlugin2 = new ApplicationDescriptor(pluginName, application, "jar:/test.png", "merge", false, true);
    applicationPlugin2.setName(pluginName);
    try {
      applicationCenterInjectService.addApplicationPlugin(applicationPlugin2);
      applicationCenterInjectService.injectDefaultApplications();
      verify(configurationManager, atLeast(1)).getURL(applicationPlugin2.getImagePath());
    } finally {
      applicationCenterInjectService.removeApplicationPlugin(pluginName);
    }
  }

  private Application application() {
    return application(ID);
  }

  private Application application(Long id) {
    return new Application(id,
                           TITLE,
                           URL,
                           HELP_PAGE_URL,
                           DESCRIPTION,
                           SHORTCUT,
                           ApplicationType.LINK,
                           false,
                           true,
                           false,
                           true,
                           false,
                           true,
                           Collections.singletonList(PERMISSIONS_1),
                           null,
                           IMAGE_FILE_ID,
                           "icon",
                           null,
                           0l,
                           false);
  }

}
