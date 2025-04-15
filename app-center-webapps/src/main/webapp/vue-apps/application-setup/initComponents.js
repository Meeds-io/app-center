/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
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

import AdminSetupApp from './components/AdminSetup.vue';

import AdminSetupToolbar from './components/main/AdminSetupToolbar.vue';
import AdminSetupList from './components/main/AdminSetupList.vue';
import AdminSetupPreview from './components/main/AdminSetupPreview.vue';
import AdminSetupItem from './components/main/AdminSetupItem.vue';
import AdminSetupMenu from './components/main/AdminSetupMenu.vue';

import ApplicationFormDrawer from './components/drawer/ApplicationFormDrawer.vue';
import ApplicationPermissions from './components/form/ApplicationPermissions.vue';
import ApplicationImageInput from './components/form/ApplicationImageInput.vue';
import QuickActionSuggester from './components/form/QuickActionSuggester.vue';
import PortletInstanceSuggester from './components/form/PortletInstanceSuggester.vue';

import ApplicationIcon from './components/view/ApplicationIcon.vue';

const components = {
  'app-center-admin-setup': AdminSetupApp,
  'app-center-admin-toolbar': AdminSetupToolbar,
  'app-center-admin-item': AdminSetupItem,
  'app-center-admin-menu': AdminSetupMenu,
  'app-center-admin-list': AdminSetupList,
  'app-center-admin-preview': AdminSetupPreview,
  'app-center-form-drawer': ApplicationFormDrawer,
  'app-center-permissions': ApplicationPermissions,
  'app-center-image-input': ApplicationImageInput,
  'app-center-icon': ApplicationIcon,
  'quick-action-suggester': QuickActionSuggester,
  'portlet-instance-suggester': PortletInstanceSuggester,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
