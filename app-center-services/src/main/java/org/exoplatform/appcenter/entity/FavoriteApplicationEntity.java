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
package org.exoplatform.appcenter.entity;

import javax.persistence.*;

import org.exoplatform.commons.api.persistence.ExoEntity;

/**
 * @author Ayoub Zayati
 */
@Entity(name = "FavoriteApplicationEntity")
@ExoEntity
@Table(name = "AC_FAVORITE_APPLICATION")
@NamedQueries({
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteAppByUserNameAndAppId", query = "SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp "
        + " WHERE favoriteApp.application.id = :applicationId AND favoriteApp.userName = :userName"),
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteAppsByAppId", query = "SELECT distinct(favoriteApp) FROM FavoriteApplicationEntity favoriteApp "
        + " WHERE favoriteApp.application.id = :applicationId"),
    @NamedQuery(name = "FavoriteApplicationEntity.countFavoritesByUser", query = "SELECT count(*) FROM FavoriteApplicationEntity favoriteApp "
        + " WHERE favoriteApp.userName = :userName"),
    @NamedQuery(name = "FavoriteApplicationEntity.getFavoriteAppsByUser", query = "SELECT favoriteApp FROM FavoriteApplicationEntity favoriteApp"
        + " WHERE favoriteApp.userName = :userName ORDER BY favoriteApp.order NULLS LAST"),

})
public class FavoriteApplicationEntity {

  @Id
  @SequenceGenerator(name = "SEQ_FAVORITE_APPLICATION_ID", sequenceName = "SEQFAVORITE_APPLICATION_ID")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FAVORITE_APPLICATION_ID")
  @Column(name = "ID")
  private Long              id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "APPLICATION_ID")
  private ApplicationEntity application;

  @Column(name = "USER_NAME")
  private String            userName;

  @Column(name = "APPLICATION_ORDER")
  private Long              order;

  public FavoriteApplicationEntity() {
  }

  public FavoriteApplicationEntity(ApplicationEntity application, String userName) {
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
  public ApplicationEntity getApplication() {
    return application;
  }

  /**
   * @param application the application to set
   */
  public void setApplication(ApplicationEntity application) {
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

  /**
   * @return the application's order
   */
  public Long getOrder() {
    return order;
  }

  /**
   * @param order the application's order
   */
  public void setOrder(Long order) {
    this.order = order;
  }
}
