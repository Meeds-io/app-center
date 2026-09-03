/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
import * as applicationService from './ApplicationService.js';

let placementsPromise = null;

export function getPlacements(useCache) {
  if (!useCache || !placementsPromise) {
    placementsPromise = fetch(`/app-center/rest/applications/placements?siteName=${eXo.env.portal.portalName || ''}`, {
      method: 'GET',
      credentials: 'include',
    })
      .then(resp => {
        if (resp?.ok) {
          return resp.json();
        } else {
          throw new Error('Error when getting application placements');
        }
      })
      .catch(error => {
        placementsPromise = null;
        throw error;
      });
  }
  return placementsPromise;
}

export function stickApplication(applicationId, side) {
  return fetch(`/app-center/rest/applications/placements/${side}?applicationId=${applicationId}`, {
    method: 'PUT',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when sticking application');
      }
      placementsPromise = null;
      if (window.require) {
        window.require(['SHARED/appStuckPanelsBundle'], app => app.init());
      }
      document.dispatchEvent(new CustomEvent('app-placement-changed'));
    });
}

export function unstickApplication(side) {
  return fetch(`/app-center/rest/applications/placements/${side}`, {
    method: 'DELETE',
    credentials: 'include',
  })
    .then(resp => {
      if (!resp?.ok) {
        throw new Error('Error when unsticking application');
      }
      placementsPromise = null;
      document.dispatchEvent(new CustomEvent('app-placement-changed'));
    });
}

export function findApplicationByDrawer(drawerName) {
  return findApplication(application => application.type === 'DRAWER' && application.url === drawerName);
}

export function findApplicationByPortletInstance(portletInstanceId) {
  return findApplication(application => application.type === 'PORTLET' && `${application.url}` === `${portletInstanceId}`);
}

export function findApplicationById(applicationId) {
  return findApplication(application => `${application.id}` === `${applicationId}`);
}

export function findStuckApplication(appType, appUrl) {
  return getPlacements(true).then(placements => {
    if (!placements?.enabled || (!placements.left && !placements.right)) {
      return null;
    }
    return findApplication(application => application.type === appType
      && `${application.url}` === `${appUrl}`
      && (`${application.id}` === `${placements.left}` || `${application.id}` === `${placements.right}`));
  });
}

export function alertWhenStuck(appType, appUrl, message) {
  return findStuckApplication(appType, appUrl)
    .catch(() => null)
    .then(stuckApplication => {
      if (stuckApplication) {
        document.dispatchEvent(new CustomEvent('alert-message', {detail: {
          alertType: 'info',
          alertMessage: message,
        }}));
        return true;
      }
      return false;
    });
}

export function getDetachUrl(application) {
  return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName || eXo.env.portal.portalName}/app-viewer?applicationId=${application.id}`;
}

export function openDetached(application) {
  window.open(getDetachUrl(application), `ac-app-${application.id}`);
}

function findApplication(predicate) {
  return applicationService.getApplications(false, true)
    .then(data => (data?.applications || []).find(predicate) || null);
}
