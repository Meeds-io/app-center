/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

export function getFavorites(size) {
  return fetch(`/app-center/rest/favorites?size=${size || 0}`, {
    method: 'GET',
    credentials: 'include',
  })
    .then(resp => {
      if (resp?.ok) {
        return resp.json();
      } else {
        throw new Error('Error when getting user favorite applications');
      }
    });
}

export function deleteFavorite(id) {
  return fetch(`/app-center/rest/favorites/${id}`,{
    method: 'DELETE',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when deleting favorite application by id');
      }
    });
}

export function addFavorite(id) {
  return fetch(`/app-center/rest/favorites/${id}`,{
    method: 'POST',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when marking application as favorite');
      }
    });      
}

export function updateFavoritesOrder(applicationsOrder) {
  return fetch('/app-center/rest/favorites', {
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    method: 'PUT',
    body: JSON.stringify(applicationsOrder)
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when updating favorite applications order');
      }
    });
}
