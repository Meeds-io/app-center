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

export async function applyApplicationFavicon(application) {
  const href = application?.imageUrl || await renderIconAsDataUrl(application?.icon);
  if (!href) {
    return false;
  }
  let link = document.querySelector('link[rel~="icon"]');
  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }
  link.href = href;
  return true;
}

async function renderIconAsDataUrl(icon) {
  if (!icon) {
    return null;
  }
  const probe = document.createElement('i');
  probe.className = icon.includes(' ') ? icon : `fa ${icon}`;
  probe.style.position = 'absolute';
  probe.style.visibility = 'hidden';
  document.body.appendChild(probe);
  try {
    const style = window.getComputedStyle(probe, '::before');
    const glyph = style.content?.replace(/^["']|["']$/g, '');
    if (!glyph || glyph === 'none') {
      return null;
    }
    const font = `${style.fontWeight} 52px ${style.fontFamily}`;
    await document.fonts.load(font, glyph);
    const canvas = document.createElement('canvas');
    canvas.width = 64;
    canvas.height = 64;
    const context = canvas.getContext('2d');
    context.font = font;
    context.textAlign = 'center';
    context.textBaseline = 'middle';
    context.fillStyle = window.getComputedStyle(document.body).getPropertyValue('--allPagesPrimaryColor')?.trim() || style.color;
    context.fillText(glyph, 32, 34);
    return canvas.toDataURL('image/png');
  } finally {
    probe.remove();
  }
}

function findApplication(predicate) {
  return applicationService.getApplications(false, true)
    .then(data => (data?.applications || []).find(predicate) || null);
}
