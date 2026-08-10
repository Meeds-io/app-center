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
package io.meeds.appcenter.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.storage.ApplicationBadgePortletStorage;

/**
 * Holds the {@link ApplicationBadgePlugin} contributed by every addon, and
 * resolves which badge a catalog entry displays.
 * <p>
 * Each plugin registers itself from its own module's {@code @PostConstruct}, so
 * registration does not depend on the order in which the WARs boot.
 */
@Service
public class ApplicationBadgePluginRegistry {

  private static final Log                          LOG       = ExoLogger.getLogger(ApplicationBadgePluginRegistry.class);

  /**
   * Reserved {@code badgeName} meaning "an administrator turned this
   * application's badge off", as opposed to a blank value which lets the url
   * binding resolve.
   */
  public static final String                        BADGE_DISABLED = "none";

  @Autowired
  private ApplicationBadgePortletStorage            badgePortletStorage;

  private final Map<String, ApplicationBadgePlugin> plugins   = new ConcurrentHashMap<>();

  /**
   * Resolves the urls a badge is bound to, in the form actually stored on the
   * applications: a {@code PORTLET} entry stores a portlet instance id, so the
   * declared portlet names have to be translated back to those ids.
   *
   * @param  plugin the badge plugin
   * @return        the stored urls this badge binds to, never null
   */
  public List<String> getBoundUrls(ApplicationBadgePlugin plugin) {
    if (plugin == null) {
      return List.of();
    }
    return Stream.concat(plugin.getDeclaredUrls(ApplicationType.DRAWER).stream(),
                         badgePortletStorage.getPortletInstanceUrls(plugin.getDeclaredUrls(ApplicationType.PORTLET)).stream())
                 .filter(StringUtils::isNotBlank)
                 .distinct()
                 .toList();
  }

  /**
   * Registers a badge plugin, replacing any previously registered plugin with
   * the same name.
   *
   * @param plugin the plugin to register
   */
  public void addPlugin(ApplicationBadgePlugin plugin) {
    if (plugin == null || StringUtils.isBlank(plugin.getName())) {
      LOG.warn("Ignoring an application badge plugin without a name: {}", plugin);
      return;
    }
    plugins.put(plugin.getName(), plugin);
  }

  /**
   * @param  badgeName the badge identifier
   * @return           the plugin registered under that name, or {@code null}
   */
  public ApplicationBadgePlugin getPlugin(String badgeName) {
    if (StringUtils.isBlank(badgeName)) {
      return null;
    }
    return plugins.get(badgeName);
  }

  /**
   * @return every registered plugin
   */
  public Collection<ApplicationBadgePlugin> getPlugins() {
    return plugins.values();
  }

  /**
   * @return the name of every registered plugin, sorted, for the administration
   *         suggester
   */
  public List<String> getPluginNames() {
    return plugins.keySet().stream().sorted().toList();
  }

  /**
   * Resolves which badge an application displays: an explicitly bound name
   * wins, otherwise a Drawer or Portlet entry matches the url a plugin
   * declares.
   * <p>
   * Lives here rather than in {@code ApplicationBadgeService} so that
   * {@code ApplicationCenterService} can decorate the applications it returns
   * without the two services depending on each other.
   *
   * @param  application the catalog entry
   * @return             the badge identifier, or null when the application
   *                       carries none or was explicitly opted out
   */
  public String resolveBadgeName(Application application) {
    if (application == null) {
      return null;
    }
    String boundName = application.getBadgeName();
    if (StringUtils.isNotBlank(boundName)) {
      return BADGE_DISABLED.equals(boundName) ? null : boundName;
    }
    String url = application.getUrl();
    ApplicationType type = application.getType();
    if (StringUtils.isBlank(url) || type == null || type == ApplicationType.LINK) {
      return null;
    }
    if (type == ApplicationType.DRAWER) {
      // A DRAWER entry stores the drawer name directly
      return plugins.values()
                    .stream()
                    .filter(plugin -> plugin.getDeclaredUrls(type).contains(url))
                    .map(ApplicationBadgePlugin::getName)
                    .findFirst()
                    .orElse(null);
    }
    // A PORTLET entry stores a portlet instance id, so it has to be mapped to
    // that instance's content id before matching what a plugin declares
    String contentId = badgePortletStorage.getPortletContentId(url);
    if (StringUtils.isBlank(contentId)) {
      return null;
    }
    return plugins.values()
                  .stream()
                  .filter(plugin -> plugin.getDeclaredUrls(type)
                                          .stream()
                                          .anyMatch(declared -> ApplicationBadgePortletStorage.matches(declared, contentId)))
                  .map(ApplicationBadgePlugin::getName)
                  .findFirst()
                  .orElse(null);
  }

}
