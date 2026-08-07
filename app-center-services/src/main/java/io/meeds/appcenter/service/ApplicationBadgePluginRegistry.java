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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;

/**
 * Holds the {@link ApplicationBadgePlugin} contributed by every addon. Beans
 * are collected from the shared Spring context at startup — the preferred
 * plugin-injection mechanism — while {@link #addPlugin(ApplicationBadgePlugin)}
 * stays available for addons still wired through Kernel XML.
 */
@Component
public class ApplicationBadgePluginRegistry {

  private static final Log                          LOG       = ExoLogger.getLogger(ApplicationBadgePluginRegistry.class);

  /**
   * Reserved {@code badgeName} meaning "an administrator turned this
   * application's badge off", as opposed to a blank value which lets the url
   * binding resolve.
   */
  public static final String                        BADGE_DISABLED = "none";

  @Autowired
  private ApplicationContext                        applicationContext;

  private final Map<String, ApplicationBadgePlugin> plugins   = new ConcurrentHashMap<>();

  private volatile boolean                          collected = false;

  /**
   * Beans are collected on first use rather than in {@code @PostConstruct},
   * because a contributing addon's WAR boots <em>after</em> App Center — it
   * depends on it — so an eager scan would silently miss every plugin. The
   * first badge read happens when a user opens a page, long after every context
   * is up.
   */
  private void collectPluginBeans() {
    if (collected) {
      return;
    }
    synchronized (this) {
      if (collected) {
        return;
      }
      applicationContext.getBeansOfType(ApplicationBadgePlugin.class).values().forEach(this::addPlugin);
      collected = true;
    }
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
    collectPluginBeans();
    return plugins.get(badgeName);
  }

  /**
   * @return every registered plugin
   */
  public Collection<ApplicationBadgePlugin> getPlugins() {
    collectPluginBeans();
    return plugins.values();
  }

  /**
   * @return the name of every registered plugin, sorted, for the administration
   *         suggester
   */
  public List<String> getPluginNames() {
    collectPluginBeans();
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
    collectPluginBeans();
    return plugins.values()
                  .stream()
                  .filter(plugin -> StringUtils.equals(url,
                                                       type == ApplicationType.DRAWER ? plugin.getDrawerName() :
                                                                                      plugin.getPortletName()))
                  .map(ApplicationBadgePlugin::getName)
                  .findFirst()
                  .orElse(null);
  }

}
