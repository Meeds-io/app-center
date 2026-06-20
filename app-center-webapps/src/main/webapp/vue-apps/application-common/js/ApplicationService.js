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

export function getApplications(includeDisabled) {
  return fetch(includeDisabled ? '/app-center/rest/applications/all' : '/app-center/rest/applications', {
    method: 'GET',
    credentials: 'include',
  })
    .then(resp => {
      if (resp?.ok) {
        return resp.json();
      } else {
        throw new Error('Error when getting applications');
      }
    });
}

export function deleteApplication(id) {
  return fetch(`/app-center/rest/applications/${id}`,{
    method: 'DELETE',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when deleting application by id');
      }
    });
}

export function createApplication(application) {
  return fetch('/app-center/rest/applications', {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'POST',
    credentials: 'include',
    body: JSON.stringify(application)
  })
    .then(resp => {
      if (resp?.ok) {
        return resp.json();
      } else {
        throw new Error('Error when creating application');
      }
    });      
}

export function updateApplication(application) {
  return fetch('/app-center/rest/applications', {
    headers: {
      'Content-Type': 'application/json'
    },
    method: 'PUT',
    credentials: 'include',
    body: JSON.stringify(application)
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when updating application');
      }
    });
}

export function getAppCenterSettings() {
  return fetch('/app-center/rest/applications/settings', {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    }
    throw new Error('Error when getting app-center settings');
  });
}

export function saveAppCenterSettings(settings) {
  return fetch('/app-center/rest/applications/settings', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when saving app-center settings');
    }
  });
}

export function createPersonalApp(application) {
  return fetch('/app-center/rest/applications/personal', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(application),
  }).then(resp => {
    if (resp?.ok) {
      return resp.json();
    }
    throw new Error('Error when creating personal application');
  });
}

export function updatePersonalApp(application) {
  return fetch(`/app-center/rest/applications/personal/${application.id}`, {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(application),
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when updating personal application');
    }
  });
}

export function deletePersonalApp(id) {
  return fetch(`/app-center/rest/applications/personal/${id}`, {
    method: 'DELETE',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error when deleting personal application');
    }
  });
}
