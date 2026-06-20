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
import AppLauncher from './components/AppLauncher.vue';
import AppLauncherEmpty from './components/AppLauncherEmpty.vue';
import AppLauncherDrawer from './components/AppLauncherDrawer.vue';
import AppLauncherMobileDrawer from './components/AppLauncherMobileDrawer.vue';
import PersonalAppFormDrawer from '../myApplications/components/PersonalAppFormDrawer.vue';
import ApplicationIcon from '../application-setup/components/view/ApplicationIcon.vue';
import ApplicationImageInput from '../application-setup/components/form/ApplicationImageInput.vue';

const components = {
  'app-center-launcher': AppLauncher,
  'app-center-launcher-empty': AppLauncherEmpty,
  'app-center-launcher-drawer': AppLauncherDrawer,
  'app-center-launcher-mobile-drawer': AppLauncherMobileDrawer,
  'personal-app-form-drawer': PersonalAppFormDrawer,
  'app-center-icon': ApplicationIcon,
  'app-center-image-input': ApplicationImageInput,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
