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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import io.meeds.layout.model.PortletInstance;
import io.meeds.layout.service.PortletInstanceService;

/**
 * Translates between a {@code PORTLET} application's stored url and the portlet
 * name a badge plugin declares.
 * <p>
 * A {@code PORTLET} catalog entry stores the <strong>portlet instance
 * id</strong> chosen in the administration suggester, not the
 * {@code portlet-name} from {@code portlet.xml}. A plugin can only declare the
 * latter — instance ids are generated per deployment — so the two have to be
 * mapped.
 * <p>
 * The whole mapping is cached as a single entry, deliberately: it is read for
 * every application of every launcher render, and resolving one instance at a
 * time would turn that into a query per application.
 */
@Component
public class ApplicationBadgePortletStorage {

  public static final String             CACHE_NAME = "app-center.badge.portlet";

  private static final String            SINGLE_KEY = "'portletContentIdsByInstanceId'";

  @Autowired
  private PortletInstanceService         portletInstanceService;

  /**
   * Self reference, so that the derived lookups below go through the Spring
   * proxy. Calling the {@code @Cacheable} method on {@code this} would bypass
   * the caching aspect entirely and re-read every portlet instance on every
   * lookup — which is the very thing this cache exists to avoid.
   */
  @Autowired
  @Lazy
  private ApplicationBadgePortletStorage self;

  /**
   * @return the {@code contentId} of every portlet instance, keyed by instance
   *         id as stored in an application's url
   */
  @Cacheable(cacheNames = CACHE_NAME, key = SINGLE_KEY, sync = true)
  public Map<String, String> getPortletContentIdsByInstanceId() {
    List<PortletInstance> portletInstances = portletInstanceService.getPortletInstances();
    Map<String, String> contentIds = new HashMap<>();
    if (portletInstances != null) {
      portletInstances.forEach(portletInstance -> {
        if (StringUtils.isNotBlank(portletInstance.getContentId())) {
          contentIds.put(String.valueOf(portletInstance.getId()), portletInstance.getContentId());
        }
      });
    }
    // Unmodifiable: this instance is what sits in the cache, so a caller
    // mutating it would corrupt the entry for everyone
    return Collections.unmodifiableMap(contentIds);
  }

  /**
   * @param url an application url holding a portlet instance id
   * @return the corresponding {@code contentId}, or null when unknown
   */
  public String getPortletContentId(String url) {
    return StringUtils.isBlank(url) ? null : self.getPortletContentIdsByInstanceId().get(url);
  }

  /**
   * Reverse lookup, used to find which applications a badge is bound to.
   *
   * @param declaredPortlets the portlet values a plugin declares, either a
   *          {@code contentId} or a bare {@code portlet-name}
   * @return the urls of the matching portlet instances
   */
  public List<String> getPortletInstanceUrls(Collection<String> declaredPortlets) {
    if (declaredPortlets == null || declaredPortlets.isEmpty()) {
      return List.of();
    }
    return self.getPortletContentIdsByInstanceId()
               .entrySet()
               .stream()
               .filter(entry -> declaredPortlets.stream().anyMatch(declared -> matches(declared, entry.getValue())))
               .map(Map.Entry::getKey)
               .toList();
  }

  /**
   * A plugin may declare either the full {@code applicationName/portletName}
   * content id — unambiguous, and what the administration form matches on — or
   * just the portlet name, as Layout's own portlet lookup also tolerates.
   *
   * @param declared the value declared by a badge plugin
   * @param contentId the content id of a portlet instance
   * @return whether they designate the same portlet
   */
  public static boolean matches(String declared, String contentId) {
    return StringUtils.isNotBlank(declared)
           && (StringUtils.equals(declared, contentId)
               || StringUtils.equals(declared, StringUtils.substringAfterLast(contentId, "/")));
  }

  /**
   * Drops the mapping, so that a newly created or removed portlet instance is
   * picked up before the cache would have expired on its own.
   */
  @CacheEvict(cacheNames = CACHE_NAME, key = SINGLE_KEY)
  public void clearCache() {
    // Cache eviction only
  }

}
