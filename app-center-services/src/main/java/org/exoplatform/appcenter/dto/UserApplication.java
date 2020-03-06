/**
 * 
 */
package org.exoplatform.appcenter.dto;

public class UserApplication extends Application {

  private static final long serialVersionUID = -2451635329141517005L;

  private boolean           favorite;

  public UserApplication(Application app) {
    super(app.getId(),
          app.getTitle(),
          app.getUrl(),
          app.getImageFileId(),
          app.getImageFileBody(),
          app.getImageFileName(),
          app.getDescription(),
          app.isActive(),
          app.isByDefault(),
          app.getPermissions());
  }

  public UserApplication(Long id,
                             String title,
                             String url,
                             Long imageFileId,
                             String imageFileBody,
                             String imageFileName,
                             String description,
                             boolean active,
                             boolean byDefault,
                             boolean favorite,
                             String... permissions) {
    super(id, title, url, imageFileId, imageFileBody, imageFileName, description, active, byDefault, permissions);
    this.favorite = favorite;
  }

  public boolean isFavorite() {
    return favorite;
  }

  public void setFavorite(boolean favorite) {
    this.favorite = favorite;
  }
}
