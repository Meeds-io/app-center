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
package io.meeds.appcenter.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.appcenter.storage.ApplicationCategoryStorage;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;

import jakarta.annotation.PostConstruct;

@SpringBootTest(classes = { ApplicationCategoryCacheListener.class, ApplicationCategoryStorage.class,
  ApplicationCategoryCacheListenerTest.CacheTestConfiguration.class })
@ExtendWith(MockitoExtension.class)
class ApplicationCategoryCacheListenerTest {

  private static final List<String>        OBJECT_TYPES = List.of(ApplicationCategoryPlugin.OBJECT_TYPE);

  /** Not reset: the registration happens once, when the context starts */
  @MockitoBean(reset = MockReset.NONE)
  private ListenerService                  listenerService;

  @MockitoBean
  private ApplicationCenterService         applicationCenterService;

  @MockitoBean
  private CategoryService                  categoryService;

  @MockitoBean
  private CategoryLinkService              categoryLinkService;

  @Autowired
  private ApplicationCategoryStorage       categoryStorage;

  @Autowired
  private ApplicationCategoryCacheListener listener;

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(ApplicationCategoryStorage.CACHE_NAME);
    }
  }

  @BeforeEach
  void setup() {
    categoryStorage.clearCache();
    // The service delegates the eviction to the storage
    doAnswer(invocation -> {
      categoryStorage.clearCache();
      return null;
    }).when(applicationCenterService).clearCategoryApplications();
  }

  /**
   * Also fails the mutants removing {@link PostConstruct} from init or making
   * the listener {@link Asynchronous}, which would let a read on this node
   * serve the list from before the change
   */
  @Test
  void listensSynchronouslyToEveryCategoryChange() throws NoSuchMethodException {
    assertEquals(List.of("category.link.added",
                         "category.link.removed",
                         "social.category.created",
                         "social.category.updated",
                         "social.category.deleted"),
                 ApplicationCategoryCacheListener.EVENT_NAMES);
    ApplicationCategoryCacheListener.EVENT_NAMES.forEach(eventName -> verify(listenerService).addListener(eq(eventName),
                                                                                                         eq(listener)));
    assertTrue(ApplicationCategoryCacheListener.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    assertTrue(!ApplicationCategoryCacheListener.class.isAnnotationPresent(Asynchronous.class));
  }

  @Test
  void everyCategoryEventClearsTheCachedApplications() throws Exception {
    for (String eventName : ApplicationCategoryCacheListener.EVENT_NAMES) {
      when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3")));
      assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l), eventName);

      when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3"), app("4")));
      assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l), eventName);
      listener.onEvent(new Event<>(eventName, 10l, app("4")));
      assertEquals(List.of(3l, 4l), categoryStorage.getApplicationIds(10l), eventName);
      categoryStorage.clearCache();
    }
  }

  @Test
  void aLinkOfAnotherObjectTypeKeepsTheCachedApplications() throws Exception {
    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3")));
    assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l));
    when(categoryLinkService.getLinkedObjects(10l, OBJECT_TYPES, 0, 0)).thenReturn(List.of(app("3"), app("4")));

    listener.onEvent(new Event<>("category.link.added", 10l, new CategoryObject("activity", "8", 0)));
    listener.onEvent(new Event<>("category.link.removed", 10l, new CategoryObject("space", "2", 0)));
    listener.onEvent(new Event<>("category.link.added", 10l, null));
    assertEquals(List.of(3l), categoryStorage.getApplicationIds(10l));

    // A category change clears it whatever its data
    listener.onEvent(new Event<>("social.category.updated", null, null));
    assertEquals(List.of(3l, 4l), categoryStorage.getApplicationIds(10l));
  }

  private CategoryObject app(String id) {
    return new CategoryObject(ApplicationCategoryPlugin.OBJECT_TYPE, id, 0);
  }

}
