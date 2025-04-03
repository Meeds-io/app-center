/*
* This file is part of the Meeds project (https://meeds.io/).
*
* Copyright (C) 2025 Meeds Association contact@meeds.io
*
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
* Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

export function getFavoriteApplications(size) {
  return fetch(`/app-center/rest/favorites?size=${size}`, {
    credentials: 'include',
    method: 'GET'
  })
    .then(resp => {
      if (resp?.ok) {
        return resp.json();
      } else {
        throw new Error(
          'Error when getting the favorite applications list'
        );
      }
    });
}

export async function updateApplicationsOrder(applicationsOrder) {
  try {
    const response = await fetch('/app-center/rest/favorites', {
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      method: 'PUT',
      body: JSON.stringify(applicationsOrder),
    });
    if (!response.ok) {
      console.error(`Failed to update applications order, status: ${response.status}`);
    }
    return true;
  } catch (error) {
    console.error('Error updating applications order:', error);
    return false;
  }
}

export function saveSettings(saveSettingsURL, settings) {
  const formData = new FormData();
  if (settings) {
    Object.keys(settings).forEach(name => {
      formData.append(name, settings[name]);
    });
  }
  return fetch(saveSettingsURL.replaceAll('&amp;', '&'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams(formData).toString(),
  }).then(resp => {
    if (!resp.ok) {
      throw new Error('Error while saving my applications settings');
    }
  });
}
