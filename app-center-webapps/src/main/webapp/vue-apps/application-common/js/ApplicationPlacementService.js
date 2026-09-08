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

export function getPlacements() {
  return window.eXo?.env?.portal?.appPlacements || null;
}

export function refreshPlacements() {
  return fetch(`/app-center/rest/applications/placements?siteName=${eXo.env.portal.portalName || ''}`, {
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
    .then(placements => {
      eXo.env.portal.appPlacements = placements;
      document.dispatchEvent(new CustomEvent('app-placement-changed'));
      return placements;
    });
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
      if (window.require) {
        window.require(['SHARED/appStuckPanelsBundle'], app => app.init());
      }
      return refreshPlacements();
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
      return refreshPlacements();
    });
}

export function getEligibility(appName) {
  return findApplicationByDrawer(appName)
    .then(application => application && {
      applicationId: application.id,
      allowStick: application.allowStick,
      allowDetach: application.allowDetach,
    } || null);
}

export function findApplicationById(applicationId) {
  return findApplication(application => `${application.id}` === `${applicationId}`);
}

export function findApplicationByDrawer(drawerName) {
  return findApplication(application => application.type === 'DRAWER' && application.url === drawerName);
}

export function findApplicationByPortletInstance(portletInstanceId) {
  return findApplication(application => application.type === 'PORTLET' && `${application.url}` === `${portletInstanceId}`);
}

export function findStuckApplication(appType, appUrl) {
  const placements = getPlacements();
  if (!placements?.siteEligible || window.innerWidth < 1264) {
    return null;
  }
  return [placements.left, placements.right]
    .find(application => application
      && application.type === appType
      && `${application.url}` === `${appUrl}`) || null;
}

export function alertWhenStuck(appType, appUrl, message) {
  const stuckApplication = findStuckApplication(appType, appUrl);
  if (stuckApplication) {
    document.dispatchEvent(new CustomEvent('alert-message', {detail: {
      alertType: 'info',
      alertMessage: message,
    }}));
    return true;
  }
  return false;
}

export function getDetachUrl(applicationId) {
  return `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName || eXo.env.portal.portalName}/app-viewer?applicationId=${applicationId}`;
}

export function openDetached(applicationId) {
  window.open(getDetachUrl(applicationId), `ac-app-${applicationId}`);
}

function findApplication(predicate) {
  return applicationService.getApplications(false, true)
    .then(data => (data?.applications || []).find(predicate) || null);
}
