package org.exoplatform.appcenter.dto;

import java.io.Serializable;

public class GeneralSettings implements Serializable {

  private static final long serialVersionUID = -7944257841117045698L;

  private long              maxFavoriteApps;

  private ApplicationImage  defaultApplicationImage;

  public long getMaxFavoriteApps() {
    return maxFavoriteApps;
  }

  public void setMaxFavoriteApps(long maxFavoriteApps) {
    this.maxFavoriteApps = maxFavoriteApps;
  }

  public ApplicationImage getDefaultApplicationImage() {
    return defaultApplicationImage;
  }

  public void setDefaultApplicationImage(ApplicationImage defaultApplicationImage) {
    this.defaultApplicationImage = defaultApplicationImage;
  }

}
