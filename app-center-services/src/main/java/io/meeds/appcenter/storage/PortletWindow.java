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

import java.util.Map;

/**
 * A portlet window's preferences as committed, within a task of
 * {@link PortletWindowStorage#callOnWindow}: read and written on the storage's
 * thread, in the task's request, under the window's lock.
 */
public class PortletWindow {

  private final PortletWindowStorage storage;

  private final String               storageId;

  PortletWindow(PortletWindowStorage storage, String storageId) {
    this.storage = storage;
    this.storageId = storageId;
  }

  /**
   * @return the window's storage id
   */
  public String getStorageId() {
    return storageId;
  }

  /**
   * @return the window's preferences as its row holds them, by name
   */
  public Map<String, String> getPreferences() {
    return storage.readPreferences(storageId);
  }

  /**
   * Sets preferences over the committed ones, the others kept.
   *
   * @param values the preferences to set, by name
   */
  public void savePreferences(Map<String, String> values) {
    storage.writePreferences(storageId, values);
  }

}
