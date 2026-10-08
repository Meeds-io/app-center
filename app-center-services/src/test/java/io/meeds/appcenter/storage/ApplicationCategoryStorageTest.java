/*
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;

@SpringBootTest(classes = { ApplicationCategoryStorage.class, ApplicationCategoryStorageTest.CacheTestConfiguration.class })
@ExtendWith(MockitoExtension.class)
class ApplicationCategoryStorageTest {

  private static final List<String>   OBJECT_TYPES = List.of(ApplicationCategoryPlugin.OBJECT_TYPE);

  @MockitoBean
  private CategoryService             categoryService;

  @MockitoBean
  private CategoryLinkService         categoryLinkService;

  @Autowired
  private ApplicationCategoryStorage  categoryStorage;

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(ApplicationCategoryStorage.CACHE_NAME);
    }
  }

  /**
   * The cache manager is a singleton of the shared test context, so an entry
   * left by one test would leak into the next.
   */
  @BeforeEach
  void setup() {
    categoryStorage.clearCache();
  }

  @Test
  void getApplicationIdsReadsTheWholeSubtreeOnce() {
    when(categoryService.getSubcategoryIds(10l, 0, 0, -1)).thenReturn(List.of(11l, 12l));
    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3")));
    when(categoryLinkService.getLinkedObjects(11l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("1"), app("3")));
    when(categoryLinkService.getLinkedObjects(12l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("x"), app("2")));

    assertEquals(List.of(3l, 1l, 2l), categoryStorage.getApplicationIds(10l));
    assertEquals(List.of(3l, 1l, 2l), categoryStorage.getApplicationIds(10l));

    verify(categoryService, times(1)).getSubcategoryIds(10l, 0, 0, -1);
    verify(categoryLinkService, times(1)).getLinkedObjects(12l, OBJECT_TYPES, 0, 0);
    List<Long> ids = categoryStorage.getApplicationIds(10l);
    assertThrows(UnsupportedOperationException.class, () -> ids.add(4l));
  }

  @Test
  void getApplicationIdsIsCachedPerCategory() {
    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3")));
    when(categoryLinkService.getLinkedObjects(20l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("5")));

    assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l));
    assertEquals(List.of(5l), categoryStorage.getApplicationIds(20l));
    assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l));
    verify(categoryLinkService, times(1)).getLinkedObjects(10l, OBJECT_TYPES, 0, 0);
  }

  @Test
  void clearCacheEvictsEveryCategory() {
    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3")));
    when(categoryLinkService.getLinkedObjects(20l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("5")));
    categoryStorage.getApplicationIds(10l);
    categoryStorage.getApplicationIds(20l);

    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3"), app("4")));
    categoryStorage.clearCache();

    assertEquals(List.of(3l, 4l), categoryStorage.getApplicationIds(10l));
    categoryStorage.getApplicationIds(20l);
    verify(categoryLinkService, times(2)).getLinkedObjects(20l, OBJECT_TYPES, 0, 0);
  }

  private CategoryObject app(String id) {
    return new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, id, 0);
  }

}
