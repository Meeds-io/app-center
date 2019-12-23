package org.exoplatform.appCenter.services.entity.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author Ayoub Zayati
 */
@Entity(name = "FavoriteApplicationEntity")
@ExoEntity
@Table(name = "FAVORITE_APPLICATION")
@NamedQueries({
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteApps", query = "SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp "
        + "WHERE favoriteApp.userName= :userName"),
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteAppByUserNameAndAppId", query = "SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp "
        + "WHERE favoriteApp.application.id= :applicationId AND favoriteApp.userName= :userName"),
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteAppsByAppId", query = "SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp "
        + "WHERE favoriteApp.application.id= :applicationId"),

})
public class FavoriteApplication {

  @Id
  @SequenceGenerator(name = "SEQ_FAVORITE_APPLICATION_ID", sequenceName = "SEQFAVORITE_APPLICATION_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FAVORITE_APPLICATION_ID")
  @Column(name = "ID")
  private Long        id;

  @ManyToOne
  @JoinColumn(name = "APPLICATION_ID")
  private Application application;

  @Column(name = "USER_NAME")
  private String      userName;

  public FavoriteApplication() {
  }

  public FavoriteApplication(Application application, String userName) {
    this.application = application;
    this.userName = userName;
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
   * @return the application
   */
  public Application getApplication() {
    return application;
  }

  /**
   * @param application the application to set
   */
  public void setApplication(Application application) {
    this.application = application;
  }

  /**
   * @return the userName
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @param userName the userName to set
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }
}
