/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.appcenter.plugin;

import org.exoplatform.appcenter.dto.Application;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;

public class ApplicationPlugin extends BaseComponentPlugin {

  private Application application;

  private String      imagePath;

  private boolean     override;

  public ApplicationPlugin(InitParams params) {
    if (params == null || !params.containsKey("application")) {
      throw new IllegalArgumentException("params containing 'application' parameter of plugin is mandatory");
    }

    application = (Application) params.getObjectParam("application").getObject();
    if (application == null) {
      throw new IllegalStateException("'application' init parameter is null");
    }

    if (params.containsKey("imagePath")) {
      this.imagePath = params.getValueParam("imagePath").getValue();
    }

    if (params.containsKey("override")) {
      this.override = Boolean.parseBoolean(params.getValueParam("override").getValue());
    }
  }

  public Application getApplication() {
    return application;
  }

  public String getImagePath() {
    return imagePath;
  }

  public boolean isOverride() {
    return override;
  }
}
