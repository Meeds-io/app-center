/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;

/**
 * Runs against a real cache manager on purpose: the point of this test is that
 * the {@code @Cacheable} and {@code @CacheEvict} annotations agree on cache
 * name and key. A mock-based test would pass even if they didn't, and the
 * eviction would silently never take effect.
 */
@SpringBootTest(classes = { ApplicationBadgePortletStorage.class,
  ApplicationBadgePortletStorageTest.CacheTestConfiguration.class })
@ExtendWith(MockitoExtension.class)
class ApplicationBadgePortletStorageTest {

  private static final String            APP2 = "agenda/AgendaTimeline";

  private static final String            APP1 = "agenda/Agenda";

  @MockitoBean
  private PortletInstanceService         portletInstanceService;

  @Autowired
  private ApplicationBadgePortletStorage badgePortletStorage;

  @Configuration
  @EnableCaching
  static class CacheTestConfiguration {
    @Bean
    ConcurrentMapCacheManager cacheManager() {
      return new ConcurrentMapCacheManager(ApplicationBadgePortletStorage.CACHE_NAME);
    }
  }

  /**
   * The cache manager is a singleton of the shared test context, so an entry
   * left by one test would leak into the next.
   */
  @BeforeEach
  void setup() {
    badgePortletStorage.clearCache();
  }

  @Test
  void mapsInstanceIdToContentIdAndReadsThroughTheCache() {
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(portletInstance(12, APP1),
                                                                          portletInstance(13, APP2),
                                                                          portletInstance(14, null)));

    assertEquals(APP1, badgePortletStorage.getPortletContentId("12"));
    assertEquals(APP2, badgePortletStorage.getPortletContentId("13"));
    // A blank content id is skipped rather than mapped to an empty value
    assertNull(badgePortletStorage.getPortletContentId("14"));
    assertNull(badgePortletStorage.getPortletContentId("404"));
    assertNull(badgePortletStorage.getPortletContentId(null));

    // Every read after the first is served from the cache
    verify(portletInstanceService, times(1)).getPortletInstances();
  }

  @Test
  void clearCacheEvictsWhatTheCacheableMethodStored() {
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(portletInstance(12, APP1)));
    assertEquals(APP1, badgePortletStorage.getPortletContentId("12"));
    verify(portletInstanceService, times(1)).getPortletInstances();

    badgePortletStorage.clearCache();

    // Reloaded: without a matching key the eviction would be a silent no-op and
    // an administrator's new portlet instance would stay invisible until the
    // TTL
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(portletInstance(12, APP1),
                                                                          portletInstance(99, "glpi/Glpi")));
    assertEquals("glpi/Glpi", badgePortletStorage.getPortletContentId("99"));
    verify(portletInstanceService, times(2)).getPortletInstances();
  }

  @Test
  void reverseLookupFindsTheInstancesOfDeclaredPortlets() {
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(portletInstance(12, APP1),
                                                                          portletInstance(13, APP2),
                                                                          portletInstance(20, "other/Something")));

    assertEquals(List.of("12"), badgePortletStorage.getPortletInstanceUrls(List.of(APP1)));
    // A bare portlet name resolves too
    assertEquals(List.of("13"), badgePortletStorage.getPortletInstanceUrls(List.of("AgendaTimeline")));
    assertTrue(badgePortletStorage.getPortletInstanceUrls(List.of("unknown/Portlet")).isEmpty());
    assertTrue(badgePortletStorage.getPortletInstanceUrls(List.of()).isEmpty());
    assertTrue(badgePortletStorage.getPortletInstanceUrls(null).isEmpty());
  }

  @Test
  void cachedMappingIsNotMutableByCallers() {
    when(portletInstanceService.getPortletInstances()).thenReturn(List.of(portletInstance(12, APP1)));

    assertThrows(UnsupportedOperationException.class, // NOSONAR
                 () -> badgePortletStorage.getPortletContentIdsByInstanceId().put("13", "forged/ContentId"));
  }

  @Test
  void toleratesAnEmptyOrMissingPortletInstanceList() {
    when(portletInstanceService.getPortletInstances()).thenReturn(null);
    assertNull(badgePortletStorage.getPortletContentId("12"));
  }

  private PortletInstance portletInstance(long id, String contentId) {
    PortletInstance portletInstance = new PortletInstance();
    portletInstance.setId(id);
    portletInstance.setContentId(contentId);
    return portletInstance;
  }

}
