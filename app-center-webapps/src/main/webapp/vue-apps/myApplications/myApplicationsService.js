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

export function saveSettings(saveSettingsURL, settings, settingName) {
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
    return resp.text();
  }).then(renderedPage => {
    // A page drawn by the page layout renders its body portlets apart from the page:
    // read this window's own rendering when the page does not carry it
    if (renderedPage?.includes?.(`settingName: '${settingName}'`)) {
      return renderedPage;
    }
    return getRenderedPortlet(saveSettingsURL);
  }).then(renderedPage => {
    // A refused portlet action still answers 200: check the settings the portlet was rendered with
    if (!isRenderedWithSettings(renderedPage, settings, settingName)) {
      throw new Error('My applications settings were not saved');
    }
  });
}

function getRenderedPortlet(saveSettingsURL) {
  const actionUrl = new URL(saveSettingsURL.replaceAll('&amp;', '&'), window.location.origin);
  const portletId = actionUrl.searchParams.get('portal:componentId');
  if (!portletId) {
    return Promise.resolve(null);
  }
  const params = new URLSearchParams({
    maximizedPortletId: portletId,
    showMaxWindow: true,
    hideSharedLayout: true,
    maximizedPortletMode: 'VIEW',
  });
  return fetch(`${actionUrl.pathname}?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => resp?.ok && resp.text() || null);
}

function isRenderedWithSettings(renderedPage, settings, settingName) {
  const settingNameIndex = renderedPage ? renderedPage.indexOf(`settingName: '${settingName}'`) : -1;
  if (settingNameIndex < 0) {
    return false;
  }
  const settingsEndIndex = renderedPage.indexOf('}));', settingNameIndex);
  const renderedSettings = renderedPage.substring(settingNameIndex, settingsEndIndex < 0 ? renderedPage.length : settingsEndIndex);
  return Object.keys(settings || {}).every(name => {
    const value = new RegExp(`${name}: \\[([^\\]]*)\\],`).exec(renderedSettings)?.[1]
               ?? new RegExp(`${name}: '?([^',\\s]*)'?,`).exec(renderedSettings)?.[1];
    return value === String(settings[name]);
  });
}

export function getListedApplications(settingName, ids) {
  return getApplications('list', settingName, ids);
}

export function getContextApplications(settingName, ids) {
  return getApplications('contextual/list', settingName, ids);
}

export function getContextSuggestions(settingName, keyword, excludedIds, limit) {
  const params = new URLSearchParams({settingName, limit: limit || 20});
  if (keyword) {
    params.append('keyword', keyword);
  }
  if (excludedIds?.length) {
    params.append('excludedIds', excludedIds.join(','));
  }
  return fetch(`/app-center/rest/applications/contextual/suggest?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error while suggesting applications');
    }
    return resp.json();
  });
}

function getApplications(path, settingName, ids) {
  if (!ids?.length) {
    return Promise.resolve([]);
  }
  const params = new URLSearchParams({settingName, ids: ids.join(',')});
  return fetch(`/app-center/rest/applications/${path}?${params}`, {
    method: 'GET',
    credentials: 'include',
  }).then(resp => {
    if (!resp?.ok) {
      throw new Error('Error while retrieving the listed applications');
    }
    return resp.json();
  });
}
