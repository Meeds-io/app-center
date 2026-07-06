/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.appcenter.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.mcp.model.AppModel;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * MCP tools exposing the App-Center add-on to the AI agent (EVA). Every method
 * acts as the current user (resolved via {@link #getCurrentUserName()}) and
 * passes that username to {@link ApplicationCenterService}, so access rights,
 * favorite ownership and personal-app ownership are all enforced by the
 * service. Admin-only catalog management is intentionally not exposed here.
 */
@Service
@Profile("mcp-server")
public class AppCenterMcpTool implements McpToolPlugin {

  private static final int              DEFAULT_LIST_LIMIT = 20;

  private static final int              MAX_LIST_LIMIT     = 100;

  private final ApplicationCenterService applicationCenterService;

  public AppCenterMcpTool(ApplicationCenterService applicationCenterService) {
    this.applicationCenterService = applicationCenterService;
  }

  /**
   * Lists the applications the current user is authorized to use (active apps
   * they may access), optionally filtered by a keyword on title/url, with each
   * app flagged as favorite or not.
   */
  public List<AppModel> listMyApps(String keyword, Integer offset, Integer limit) {
    String currentUser = getCurrentUserName();
    Locale locale = getCurrentUserLocale();
    ApplicationList list = applicationCenterService.getActiveApplications(clampOffset(offset),
                                                                          clampLimit(limit),
                                                                          StringUtils.trimToNull(keyword),
                                                                          locale,
                                                                          currentUser);
    return toModels(list);
  }

  /**
   * Lists the current user's launcher drawer: the mandatory apps plus the apps
   * they marked as favorites.
   */
  public List<AppModel> listMyAppDrawer() {
    String currentUser = getCurrentUserName();
    Locale locale = getCurrentUserLocale();
    ApplicationList list = applicationCenterService.getMandatoryAndFavoriteApplications(Pageable.unpaged(),
                                                                                        currentUser,
                                                                                        locale);
    return toModels(list);
  }

  /**
   * Adds an application to the current user's favorites.
   */
  public String addFavoriteApp(Long applicationId) throws ObjectNotFoundException {
    long id = requirePositiveId(applicationId);
    String currentUser = getCurrentUserName();
    try {
      applicationCenterService.addFavoriteApplication(id, currentUser);
    } catch (ApplicationNotFoundException e) {
      throw notFound(id);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("You are not allowed to add application " + id
          + " to your favorites. Only applications you can access can be favorited.");
    }
    return "Application " + id + " has been added to your favorites.";
  }

  /**
   * Removes an application from the current user's favorites.
   */
  public String removeFavoriteApp(Long applicationId) {
    long id = requirePositiveId(applicationId);
    String currentUser = getCurrentUserName();
    applicationCenterService.deleteFavoriteApplication(id, currentUser);
    return "Application " + id + " has been removed from your favorites.";
  }

  /**
   * Reorders the current user's favorite applications. Pass the favorite
   * application ids in the desired display order (first id shown first). Any id
   * that is not yet a favorite gets added while being ordered.
   */
  public String reorderFavoriteApps(List<Long> applicationIds) throws ObjectNotFoundException {
    if (CollectionUtils.isEmpty(applicationIds)) {
      throw new IllegalArgumentException("application_ids is required: pass the favorite app ids in the desired order.");
    }
    String currentUser = getCurrentUserName();
    long order = 0;
    for (Long applicationId : applicationIds) {
      long id = requirePositiveId(applicationId);
      try {
        applicationCenterService.updateFavoriteApplicationOrder(new ApplicationOrder(id, order++), currentUser);
      } catch (ApplicationNotFoundException e) {
        throw notFound(id);
      }
    }
    return "Reordered " + applicationIds.size() + " favorite application(s).";
  }

  /**
   * Creates a personal URL application (a shortcut to an external URL) owned by
   * the current user. Requires the personal-apps feature to be enabled on the
   * instance.
   */
  public AppModel createPersonalApp(String title, String url, String icon) {
    if (!applicationCenterService.isUserPersonalAppsEnabled()) {
      throw new IllegalStateException("Personal apps are disabled on this instance.");
    }
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("title is required for a personal app.");
    }
    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException("url is required for a personal app (the external link it opens).");
    }
    String currentUser = getCurrentUserName();
    Application application = new Application();
    application.setTitle(title.trim());
    application.setUrl(url.trim());
    application.setType(ApplicationType.LINK);
    if (StringUtils.isNotBlank(icon)) {
      application.setIcon(icon.trim());
    }
    try {
      Application created = applicationCenterService.createPersonalApplication(application, currentUser);
      return toModel(created);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("You are not allowed to create a personal app: " + e.getMessage());
    }
  }

  /**
   * Updates a personal URL application owned by the current user. Only the
   * supplied fields are changed (title, url and/or icon).
   */
  public AppModel updatePersonalApp(Long applicationId, String title, String url, String icon) throws ObjectNotFoundException {
    long id = requirePositiveId(applicationId);
    if (StringUtils.isBlank(title) && StringUtils.isBlank(url) && StringUtils.isBlank(icon)) {
      throw new IllegalArgumentException("Nothing to update: provide at least one of title, url or icon.");
    }
    String currentUser = getCurrentUserName();
    Application application = applicationCenterService.getApplication(id);
    if (application == null) {
      throw notFound(id);
    }
    if (StringUtils.isNotBlank(title)) {
      application.setTitle(title.trim());
    }
    if (StringUtils.isNotBlank(url)) {
      application.setUrl(url.trim());
    }
    if (StringUtils.isNotBlank(icon)) {
      application.setIcon(icon.trim());
    }
    try {
      applicationCenterService.updatePersonalApplication(application, currentUser);
    } catch (ApplicationNotFoundException e) {
      throw notFound(id);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("You are not allowed to update application " + id
          + ": it must be a personal app that you own, and the personal-apps feature must be enabled.");
    }
    return toModel(applicationCenterService.getApplication(id));
  }

  /**
   * Deletes a personal URL application owned by the current user.
   */
  public String deletePersonalApp(Long applicationId) throws ObjectNotFoundException {
    long id = requirePositiveId(applicationId);
    String currentUser = getCurrentUserName();
    try {
      applicationCenterService.deletePersonalApplication(id, currentUser);
    } catch (ApplicationNotFoundException e) {
      throw notFound(id);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("You are not allowed to delete application " + id
          + ": it must be a personal app that you own, and the personal-apps feature must be enabled.");
    }
    return "Personal application " + id + " has been deleted.";
  }

  private long requirePositiveId(Long applicationId) {
    if (applicationId == null || applicationId <= 0) {
      throw new IllegalArgumentException("application_id is required and must be a positive number.");
    }
    return applicationId;
  }

  private ObjectNotFoundException notFound(long id) {
    return new ObjectNotFoundException("No application found with id " + id
        + ". List the current user's apps with list_my_apps to find a valid id.");
  }

  private int clampOffset(Integer offset) {
    return (offset == null || offset < 0) ? 0 : offset;
  }

  private int clampLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIST_LIMIT;
    }
    return Math.min(limit, MAX_LIST_LIMIT);
  }

  private List<AppModel> toModels(ApplicationList list) {
    List<AppModel> models = new ArrayList<>();
    if (list != null && CollectionUtils.isNotEmpty(list.getApplications())) {
      list.getApplications().forEach(application -> models.add(toModel(application)));
    }
    return models;
  }

  private AppModel toModel(Application application) {
    if (application == null) {
      return null;
    }
    boolean favorite = application.getOrder() != null;
    if (application instanceof io.meeds.appcenter.model.UserApplication userApplication) {
      favorite = favorite || userApplication.isFavorite();
    }
    return new AppModel(application.getId(),
                        application.getTitle(),
                        application.getUrl(),
                        application.getIcon(),
                        application.getImageFileId(),
                        application.getType() == null ? null : application.getType().name(),
                        favorite,
                        application.isMandatory(),
                        application.isPersonal());
  }

}
