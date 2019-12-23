/**
 * 
 */
package org.exoplatform.appCenter.services.entity;

/**
 * @author Ayoub Zayati
 *
 */
public class ApplicationForm {
  
  private Long id;
  
  private String title;
  
  private String url;
  
  private String description;
  
  private boolean active;
  
  private boolean byDefault;

  private String[] permissions;
  
  private String imageFileBody;

  private String imageFileName;
  
  public ApplicationForm() {
  }

  public ApplicationForm(String title,
                         String url,
                         String description,
                         boolean active,
                         boolean byDefault,
                         String[] permissions,
                         String imageFileBody,
                         String imageFileName) {
    this.title = title;
    this.url = url;
    this.description = description;
    this.active = active;
    this.byDefault = byDefault;
    this.permissions = permissions;
    this.imageFileBody = imageFileBody;
    this.imageFileName = imageFileName;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active the active to set
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * @return the byDefault
   */
  public boolean isByDefault() {
    return byDefault;
  }

  /**
   * @param byDefault the byDefault to set
   */
  public void setByDefault(boolean byDefault) {
    this.byDefault = byDefault;
  }

  /**
   * @return the permissions
   */
  public String[] getPermissions() {
    return permissions;
  }

  /**
   * @param permissions the permissions to set
   */
  public void setPermissions(String[] permissions) {
    this.permissions = permissions;
  }

  /**
   * @return the imageFileBody
   */
  public String getImageFileBody() {
    return imageFileBody;
  }

  /**
   * @param imageFileBody the imageFileBody to set
   */
  public void setImageFileBody(String imageFileBody) {
    this.imageFileBody = imageFileBody;
  }

  /**
   * @return the imageFileName
   */
  public String getImageFileName() {
    return imageFileName;
  }

  /**
   * @param imageFileName the imageFileName to set
   */
  public void setImageFileName(String imageFileName) {
    this.imageFileName = imageFileName;
  }
}
