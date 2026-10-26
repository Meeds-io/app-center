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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.social.util.JsonUtils;

/**
 * Stores the ordered list of applications a user pinned to the topbar as a
 * JSON array of their ids, under {@link Context#USER},
 * {@code APPLICATION:PinnedApplications}, {@code pins}: the key of the
 * existing pins, which are read as they are.
 */
@Component
public class ApplicationPinStorage {

  public static final Scope  PINS_SCOPE              = Scope.APPLICATION.id("PinnedApplications");

  public static final String PINS_KEY                = "pins";

  /**
   * Bound of the list, far above what the topbar and the launcher display:
   * the value of the setting can also be written around this storage.
   */
  public static final int    MAX_PINNED_APPLICATIONS = 100;

  private static final Log   LOG                     = ExoLogger.getLogger(ApplicationPinStorage.class);

  @Autowired
  private SettingService     settingService;

  /**
   * @param username user login
   * @return the first {@link #MAX_PINNED_APPLICATIONS} stored application ids,
   *         in pin order, without duplicates or non-positive values; an empty
   *         list when nothing is stored or the stored value is not a JSON array
   *         of numbers
   */
  public List<Long> getPinnedApplicationIds(String username) {
    SettingValue<?> value = settingService.get(Context.USER.id(username), PINS_SCOPE, PINS_KEY);
    if (value == null || value.getValue() == null || StringUtils.isBlank(value.getValue().toString())) {
      return Collections.emptyList();
    }
    try {
      Long[] applicationIds = JsonUtils.fromJsonString(value.getValue().toString(), Long[].class);
      return applicationIds == null ? Collections.emptyList()
                                    : Arrays.stream(applicationIds)
                                            .filter(Objects::nonNull)
                                            .filter(id -> id > 0)
                                            .distinct()
                                            .limit(MAX_PINNED_APPLICATIONS)
                                            .toList();
    } catch (Exception e) {
      LOG.debug("Pinned applications of user {} aren't a JSON array of ids, read as none", username, e);
      return Collections.emptyList();
    }
  }

  public void setPinnedApplicationIds(String username, List<Long> applicationIds) {
    settingService.set(Context.USER.id(username),
                       PINS_SCOPE,
                       PINS_KEY,
                       SettingValue.create(JsonUtils.toJsonString(applicationIds)));
  }

}
