package org.exoplatform.appCenter.services.entity.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author Ayoub Zayati
 */
@Entity(name = "ApplicationEntity")
@ExoEntity
@Table(name = "AC_APPLICATION")
@NamedQueries({
    @NamedQuery(name = "ApplicationEntity.getAuthorizedApplications", query = "SELECT app FROM ApplicationEntity app "
        + "WHERE app.active = true AND (app.permissions LIKE :permissionPattern1 OR app.permissions LIKE :permissionPattern2)"),
    @NamedQuery(name = "ApplicationEntity.getDefaultApplications", query = "SELECT app FROM ApplicationEntity app "
        + "WHERE app.byDefault = true AND (app.permissions LIKE :permissionPattern1 OR app.permissions LIKE :permissionPattern2)"),
    @NamedQuery(name = "ApplicationEntity.getAppByTitleOrUrl", query = "SELECT app FROM ApplicationEntity app "
        + "WHERE app.title = :title OR app.url = :url") })
public class Application {

  @Id
  @SequenceGenerator(name = "SEQ_APPLICATION_ID", sequenceName = "SEQ_APPLICATION_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_APPLICATION_ID")
  @Column(name = "ID")
  private Long    id;

  @Column(name = "TITLE")
  private String  title;

  @Column(name = "URL")
  private String  url;

  @Column(name = "IMAGE_FILE_ID")
  private Long    imageFileId;

  @Column(name = "DESCRIPTION")
  private String  description;

  @Column(name = "ACTIVE")
  private boolean active;

  @Column(name = "BY_DEFAULT")
  private boolean byDefault;

  @Column(name = "PERMISSIONS")
  private String  permissions;

  public Application() {
  }

  public Application(String title,
                     String url,
                     Long imageFileId,
                     String description,
                     boolean active,
                     boolean byDefault,
                     String permissions) {
    this.title = title;
    this.url = url;
    this.imageFileId = imageFileId;
    this.description = description;
    this.active = active;
    this.byDefault = byDefault;
    this.permissions = permissions;
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
   * @return the imageFileId
   */
  public Long getImageFileId() {
    return imageFileId;
  }

  /**
   * @param imageFileId the imageFileId to set
   */
  public void setImageFileId(Long imageFileId) {
    this.imageFileId = imageFileId;
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
  public String getPermissions() {
    return permissions;
  }

  /**
   * @param permissions the permissions to set
   */
  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }
}
