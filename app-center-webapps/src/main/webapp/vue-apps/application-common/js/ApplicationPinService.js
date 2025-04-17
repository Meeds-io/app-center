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
const contextName = 'USER';
const contextId = eXo.env.portal.userName;
const scopeName = 'APPLICATION';
const scopeId = 'PinnedApplications';
const key = 'pins';

export async function getPinnedApplications() {
  try {
    const pinnedApplicationIds = await Vue.prototype.$settingService.getSettingValue(contextName, contextId, scopeName, scopeId, key);
    return pinnedApplicationIds?.value && JSON.parse(pinnedApplicationIds.value) || [];
  } catch (e) {
    return [];
  }
}

export async function pinApplication(applicationId) {
  const pinnedApplicationIds = await getPinnedApplications();
  if (!pinnedApplicationIds.includes(applicationId)) {
    pinnedApplicationIds.push(applicationId);
  }
  await savePinnedApplications(pinnedApplicationIds);
}

export async function unpinApplication(applicationId) {
  const pinnedApplicationIds = await getPinnedApplications();
  while (pinnedApplicationIds.includes(applicationId)) {
    pinnedApplicationIds.splice(pinnedApplicationIds.indexOf(applicationId), 1);
  }
  await savePinnedApplications(pinnedApplicationIds);
}

export async function savePinnedApplications(pinnedApplicationIds) {
  await Vue.prototype.$settingService.setSettingValue(contextName, contextId, scopeName, scopeId, key, JSON.stringify(pinnedApplicationIds));
}
