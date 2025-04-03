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

import './initComponents.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('MyApplications');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env?.portal?.language || 'en';
const url = `/app-center/i18n/locale.portlet.MyApplications?lang=${lang}`;
const vuetify = Vue.prototype.vuetifyOptions;
const appId = 'myApplications';

export function init(settings) {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    Vue.createApp({
      data() {
        return {
          settings: settings
        };
      },
      template: '<my-applications-app />',
      vuetify,
      i18n
    }, `#${appId}`, 'My Application Center');
  });
}

