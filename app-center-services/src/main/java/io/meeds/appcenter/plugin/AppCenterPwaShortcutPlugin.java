/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.ResourceBundleService;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.pwa.model.PwaShortcut;
import io.meeds.pwa.model.PwaShortcutIcon;
import io.meeds.pwa.plugin.PwaShortcutPlugin;

@Service
public class AppCenterPwaShortcutPlugin implements PwaShortcutPlugin {

  public static final String      PWA_SMALL_ICON_BASE_PATH    = "/pwa/rest/manifest/smallIcon?v="; // NOSONAR

  private static final String     PWA_SHORTCUT_IMG_DIMENSIONS = "192x192";

  @Autowired
  private PortalContainer         container;

  @Autowired
  private UserPortalConfigService portalConfigService;

  @Override
  public List<PwaShortcut> getShortcuts(String username) {
    List<Application> applications = container.getComponentInstanceOfType(ApplicationCenterService.class)
                                              .getMandatoryAndFavoriteApplications(Pageable.unpaged(),
                                                                                   username,
                                                                                   getUserLocale(username))
                                              .getApplications()
                                              .stream()
                                              .filter(Application::isPwa)
                                              .toList();
    if (applications.isEmpty()) {
      return Collections.emptyList();
    } else {
      String defaultPortal = getDefaultPortal(username);
      return applications.stream()
                         .map(app -> toShortcut(app, String.format("/portal/%s", defaultPortal), username))
                         .toList();
    }
  }

  private PwaShortcut toShortcut(Application application, String defaultPortalPath, String username) {
    return new PwaShortcut(application.getTitle(),
                           application.getTitle(),
                           application.getDescription(),
                           getUrl(application, defaultPortalPath, username),
                           getShortcutIcons(application));
  }

  private List<PwaShortcutIcon> getShortcutIcons(Application application) {
    return StringUtils.isBlank(application.getImageUrl()) ?
                                                          Collections.singletonList(new PwaShortcutIcon(String.format("%s%s&sizes=%s",
                                                                                                                      PWA_SMALL_ICON_BASE_PATH,
                                                                                                                      application.hashCode(),
                                                                                                                      PWA_SHORTCUT_IMG_DIMENSIONS),
                                                                                                        PWA_SHORTCUT_IMG_DIMENSIONS,
                                                                                                        null,
                                                                                                        null)) :
                                                          Collections.singletonList(new PwaShortcutIcon(String.format("%s&sizes=%s",
                                                                                                                      getFullImageUrl(application),
                                                                                                                      PWA_SHORTCUT_IMG_DIMENSIONS),
                                                                                                        PWA_SHORTCUT_IMG_DIMENSIONS,
                                                                                                        null,
                                                                                                        null));
  }

  private String getUrl(Application application, String defaultPortalPath, String username) {
    ApplicationType type = application.getType() == null ? ApplicationType.LINK : application.getType();
    return switch (type) {
    case LINK: {
      yield application.getUrl()
                       .replace("./", defaultPortalPath + "/")
                       .replace("@user@", username == null ? "" : username);
    }
    case DRAWER: {
      yield String.format("%s?appCenterDrawer=%s", defaultPortalPath, application.getUrl());
    }
    case PORTLET: {
      yield String.format("%s?appCenterPortlet=%s", defaultPortalPath, application.getUrl());
    }
    default:
      yield application.getUrl();
    };
  }

  private String getDefaultPortal(String username) {
    PortalConfig defaultSite = portalConfigService.getDefaultSite(username);
    return defaultSite == null ? portalConfigService.getMetaPortal() : defaultSite.getName();
  }

  private String getFullImageUrl(Application application) {
    return CommonsUtils.getCurrentDomain() + application.getImageUrl();
  }

  private Locale getUserLocale(String username) {
    try {
      return LocaleContextInfoUtils.getUserLocale(username);
    } catch (Exception e) {
      return ResourceBundleService.DEFAULT_CROWDIN_LOCALE;
    }
  }

}
