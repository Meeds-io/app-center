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

// droppedIds: per list setting, the posted ids the server may drop, the hidden
// ones, which may belong to an app or a category deleted since
export function saveSettings(saveSettingsURL, settings, settingName, droppedIds) {
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
      return Promise.resolve(renderedPage);
    }
    return getRenderedPortlet(saveSettingsURL);
  }).then(renderedPage => {
    // A refused portlet action still answers 200: check the settings the portlet was rendered with
    if (!isRenderedWithSettings(renderedPage, settings, settingName, droppedIds)) {
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

function isRenderedWithSettings(renderedPage, settings, settingName, droppedIds) {
  const settingNameIndex = renderedPage ? renderedPage.indexOf(`settingName: '${settingName}'`) : -1;
  if (settingNameIndex < 0) {
    return false;
  }
  const settingsEndIndex = renderedPage.indexOf('}));', settingNameIndex);
  const renderedSettings = renderedPage.substring(settingNameIndex, settingsEndIndex < 0 ? renderedPage.length : settingsEndIndex);
  return Object.keys(settings || {}).every(name => {
    const list = new RegExp(String.raw`${name}: \[([^\]]*)\],`).exec(renderedSettings)?.[1];
    if (typeof list === 'string' && droppedIds?.[name]?.length) {
      return isRenderedList(list, String(settings[name]), droppedIds[name]);
    }
    const value = list ?? new RegExp(String.raw`${name}: '?([^',\s]*)'?,`).exec(renderedSettings)?.[1];
    return value === String(settings[name]);
  });
}

// The rendered list is the posted one, but for droppable ids the server
// dropped: any other difference is a refused save
function isRenderedList(renderedList, postedList, droppableIds) {
  const renderedIds = renderedList ? renderedList.split(',') : [];
  const droppable = droppableIds.map(String);
  const expectedIds = (postedList ? postedList.split(',') : [])
    .filter(id => renderedIds.includes(id) || !droppable.includes(id));
  return expectedIds.join(',') === renderedIds.join(',');
}

export function getListedApplications(settingName, ids, categoryIds) {
  return getApplications('list', settingName, ids, categoryIds);
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

function getApplications(path, settingName, ids, categoryIds) {
  const params = new URLSearchParams({settingName});
  if (categoryIds?.length) {
    params.append('categoryIds', categoryIds.join(','));
  } else if (ids?.length) {
    params.append('ids', ids.join(','));
  } else {
    return Promise.resolve([]);
  }
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
