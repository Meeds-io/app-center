/**
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
package io.meeds.appcenter.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.upload.UploadService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.dao.ApplicationDAO;
import io.meeds.appcenter.dao.FavoriteApplicationDAO;
import io.meeds.appcenter.entity.ApplicationEntity;
import io.meeds.appcenter.model.Application;

/**
 * The update of an application a Shortcuts portlet created, against a real
 * cache manager: a key drift of its eviction is a silent no-op a mocked
 * storage cannot see.
 */
@SpringBootTest(classes = { ApplicationCenterStorage.class, ApplicationCenterStorageCacheTest.CacheTestConfiguration.class })
@ExtendWith(MockitoExtension.class)
class ApplicationCenterStorageCacheTest {

  private static final String      CACHE_NAME   = "app-center.application";

  private static final Long        ID           = 2L;

  private static final String      SETTING_NAME = "Untitled-12";

  @MockitoBean
  private FileService              fileService;

  @MockitoBean
  private ApplicationDAO           applicationDAO;

  @MockitoBean
  private FavoriteApplicationDAO   favoriteApplicationDAO;

  @MockitoBean
  private UploadService            uploadService;

  @Autowired
  private ApplicationCenterStorage applicationCenterStorage;

  @Autowired
  private CacheManager             cacheManager;

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(CACHE_NAME);
    }
  }

  /**
   * The cache manager is a singleton of the shared test context, so an entry
   * left by one test would leak into the next.
   */
  @BeforeEach
  void setup() {
    cacheManager.getCache(CACHE_NAME).clear();
  }

  /**
   * Fails the mutants dropping the eviction or keying it on anything but the
   * application id: every later read by id, the drawer's listing and the
   * portlet's included, would serve the application as it was before the edit
   */
  @Test
  void updateContextApplicationEvictsTheApplicationReadById() throws Exception {
    when(applicationDAO.findById(ID)).thenReturn(Optional.of(contextApplicationEntity("Before")));
    when(applicationDAO.save(any())).thenAnswer(invocation -> {
      ApplicationEntity saved = invocation.getArgument(0);
      when(applicationDAO.findById(ID)).thenReturn(Optional.of(saved));
      return saved;
    });
    assertEquals("Before", applicationCenterStorage.getApplication(ID).getTitle());
    assertEquals("Before", applicationCenterStorage.getApplication(ID).getTitle());
    verify(applicationDAO, times(1)).findById(ID);

    Application application = new Application(applicationCenterStorage.getApplication(ID));
    application.setTitle("After");
    applicationCenterStorage.updateContextApplication(application);

    assertEquals("After", applicationCenterStorage.getApplication(ID).getTitle());
  }

  private ApplicationEntity contextApplicationEntity(String title) {
    ApplicationEntity applicationEntity = new ApplicationEntity(ID,
                                                                title,
                                                                "description",
                                                                ApplicationType.LINK,
                                                                "https://meeds.io",
                                                                true,
                                                                "fa-star",
                                                                null,
                                                                null,
                                                                true,
                                                                false,
                                                                false,
                                                                false,
                                                                false,
                                                                true,
                                                                null,
                                                                Collections.singletonList("*:/spaces/team"),
                                                                false,
                                                                null,
                                                                false,
                                                                null,
                                                                false,
                                                                false,
                                                                null,
                                                                null,
                                                                null);
    applicationEntity.setContextSettingName(SETTING_NAME);
    return applicationEntity;
  }

}
