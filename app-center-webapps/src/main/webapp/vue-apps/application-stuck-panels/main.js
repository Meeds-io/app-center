/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2026 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
const appId = 'appCenterStuckPanels';

export async function init() {
  if (document.querySelector(`#${appId}`)) {
    return;
  }
  const lang = eXo?.env?.portal?.language || 'en';
  const urls = [`/app-center/i18n/locale.addon.appcenter?lang=${lang}`];
  const i18n = await exoi18n.loadLanguageAsync(lang, urls);
  const appElement = document.createElement('div');
  appElement.id = appId;
  document.querySelector('#vuetify-apps').appendChild(appElement);
  Vue.createApp({
    template: `<app-center-stuck-panels id="${appId}" />`,
    i18n,
    vuetify: Vue.prototype.vuetifyOptions,
  }, appElement, 'Stuck Applications');
}
