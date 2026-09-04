/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter.entity;

import io.meeds.common.persistence.PortableSequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "FavoriteApplicationEntity")
@Table(name = "AC_FAVORITE_APPLICATION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteApplicationEntity {

  @Id
  @PortableSequence(name = "SEQFAVORITE_APPLICATION_ID")
  @Column(name = "ID")
  private Long              id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "APPLICATION_ID")
  private ApplicationEntity application;

  @Column(name = "USER_NAME")
  private String            userName;

  @Column(name = "APPLICATION_ORDER")
  private Long              order;

  @Column(name = "FAVORITE")
  private Boolean           favorite;

}
