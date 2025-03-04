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
const scopeId = 'QuickActions';
const key = 'pins';

export async function getQuickActionPins() {
  try {
    const quickActionsPins = await Vue.prototype.$settingService.getSettingValue(contextName, contextId, scopeName, scopeId, key);
    return quickActionsPins?.value && JSON.parse(quickActionsPins.value) || [];
  } catch (e) {
    return [];
  }
}

export async function saveQuickActionPins(quickActionPins) {
  await Vue.prototype.$settingService.setSettingValue(contextName, contextId, scopeName, scopeId, key, JSON.stringify(quickActionPins));
}

export async function pinQuickAction(quickAction) {
  const quickActionPins = await getQuickActionPins();
  if (!quickActionPins.includes(quickAction)) {
    quickActionPins.push(quickAction);
  }
  await saveQuickActionPins(quickActionPins);
}

export async function unpinQuickAction(quickAction) {
  const quickActionPins = await getQuickActionPins();
  while (quickActionPins.includes(quickAction)) {
    quickActionPins.splice(quickActionPins.indexOf(quickAction), 1);
  }
  await saveQuickActionPins(quickActionPins);
}
