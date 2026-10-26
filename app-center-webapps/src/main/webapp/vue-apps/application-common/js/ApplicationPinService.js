/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// The server rewrites the whole list of pins on each pin and unpin: the
// requests of the page are sent one after the other, so that two quick clicks
// can't overwrite each other, and a refresh never reads an older list than
// the last save
let pendingRequest = Promise.resolve();

function enqueue(request) {
  const result = pendingRequest.then(request);
  pendingRequest = result.catch(() => null);
  return result;
}

export function getPinnedApplications() {
  return enqueue(() => fetch('/app-center/rest/pins', {
    method: 'GET',
    credentials: 'include',
  })
    .then(resp => {
      if (resp?.ok) {
        return resp.json();
      } else {
        throw new Error('Error when getting pinned applications');
      }
    }))
    .catch(() => []);
}

export function pinApplication(applicationId) {
  return enqueue(() => fetch(`/app-center/rest/pins/${applicationId}`, {
    method: 'PUT',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when pinning application');
      }
    }));
}

export function unpinApplication(applicationId) {
  return enqueue(() => fetch(`/app-center/rest/pins/${applicationId}`, {
    method: 'DELETE',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when unpinning application');
      }
    }));
}
