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
const contextName = 'GLOBAL';
const contextId = 'QuickActions';
const scopeName = 'APPLICATION';
const scopeId = 'QuickActions';
const key = 'status';

export async function getQuickActionStatus() {
  try {
    const quickActionsStatus = await Vue.prototype.$settingService.getSettingValue(contextName, contextId, scopeName, scopeId, key);
    return quickActionsStatus?.value && JSON.parse(quickActionsStatus.value) || {};
  } catch (e) {
    return {};
  }
}

export async function saveQuickActionsStatus(quickActionsStatus) {
  await Vue.prototype.$settingService.setSettingValue(contextName, contextId, scopeName, scopeId, key, JSON.stringify(quickActionsStatus));
}

export async function setQuickActionEnabled(quickAction, enabled) {
  const quickActionsStatus = await getQuickActionStatus();
  quickActionsStatus[quickAction] = !enabled;
  await saveQuickActionsStatus(quickActionsStatus);
}
