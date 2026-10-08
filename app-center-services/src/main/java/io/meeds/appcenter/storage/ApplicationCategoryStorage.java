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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import io.meeds.appcenter.plugin.ApplicationCategoryPlugin;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryService;

/**
 * The applications linked to a category or to one of its sub-categories, as
 * the Shortcuts portlet lists them, cached per category: reading a subtree's
 * links costs one query per category of the subtree.
 */
@Component
public class ApplicationCategoryStorage {

  public static final String       CACHE_NAME   = "app-center.category.apps";

  private static final List<String> OBJECT_TYPES = List.of(ApplicationCategoryPlugin.OBJECT_TYPE);

  private static final Pattern      ID           = Pattern.compile("^\\d{1,18}$");

  @Autowired
  private CategoryService           categoryService;

  @Autowired
  private CategoryLinkService       categoryLinkService;

  /**
   * Reads the ids of the applications linked to a category or to any of its
   * sub-categories, whatever their depth: the category's own links first, then
   * its sub-categories' in the order the category service returns them, each
   * application once. Nothing in the value depends on the viewer: access,
   * activation and order are decided by the caller on each read. The links of
   * applications are stored under the application id as is, since
   * {@link ApplicationCategoryPlugin} does not rewrite the linked object, so
   * the list read of links needs no plugin normalisation.
   *
   * @param categoryId the selected category
   * @return the linked application ids, never null, not modifiable
   */
  @Cacheable(cacheNames = CACHE_NAME, key = "#p0", sync = true)
  public List<Long> getApplicationIds(long categoryId) {
    List<Long> categoryIds = new ArrayList<>();
    categoryIds.add(categoryId);
    // Database read, with no per-viewer filter and no index lag
    categoryIds.addAll(categoryService.getSubcategoryIds(categoryId, 0, 0, -1));
    Set<Long> applicationIds = new LinkedHashSet<>();
    categoryIds.forEach(id -> categoryLinkService.getLinkedObjects(id, OBJECT_TYPES, 0, 0)
                                                 .stream()
                                                 .map(CategoryObject::getId)
                                                 .filter(objectId -> objectId != null && ID.matcher(objectId).matches())
                                                 .map(Long::valueOf)
                                                 .forEach(applicationIds::add));
    return List.copyOf(applicationIds);
  }

  /**
   * Clears every category's applications: a link, a category creation, move or
   * deletion can change any subtree.
   */
  @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
  public void clearCache() {
    // Evicted by the annotation
  }

}
