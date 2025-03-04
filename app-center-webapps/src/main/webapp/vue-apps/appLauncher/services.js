/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
import * as quickActionService from '../quick-actions/js/QuickActionService.js';
import * as userQuickActionService from '../user-quick-actions/js/UserQuickActionService.js';

if (!Vue.prototype.$quickActionService) {
  window.Object.defineProperty(Vue.prototype, '$quickActionService', {
    value: quickActionService,
  });
}

if (!Vue.prototype.$userQuickActionService) {
  window.Object.defineProperty(Vue.prototype, '$userQuickActionService', {
    value: userQuickActionService,
  });
}
