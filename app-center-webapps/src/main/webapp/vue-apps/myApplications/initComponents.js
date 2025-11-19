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
import MyApplicationsApp from './components/MyApplicationsApp.vue';
import MyApplicationsList from './components/view/MyApplicationsList.vue';
import MyApplicationsToolbar from './components/view/MyApplicationsToolbar.vue';
import MyApplicationItem from './components/view/MyApplicationItem.vue';
import MyApplicationsSettingsDrawer from './components/settings/MyApplicationsSettingsDrawer.vue';

import * as myApplicationsService from './myApplicationsService.js';

const components = {
  'my-applications-app': MyApplicationsApp,
  'my-applications-list': MyApplicationsList,
  'my-applications-toolbar': MyApplicationsToolbar,
  'my-application-item': MyApplicationItem,
  'my-applications-settings-drawer': MyApplicationsSettingsDrawer
};

for (const key in components) {
  Vue.component(key, components[key]);
}

if (!Vue.prototype.$myApplicationsService) {
  window.Object.defineProperty(Vue.prototype, '$myApplicationsService', {
    value: myApplicationsService,
  });
}

