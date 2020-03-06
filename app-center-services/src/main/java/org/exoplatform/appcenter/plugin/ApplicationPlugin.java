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
