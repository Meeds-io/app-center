/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import * as applicationService from './js/ApplicationService.js';
import * as applicationFavoriteService from './js/ApplicationFavoriteService.js';
import * as applicationPinService from './js/ApplicationPinService.js';
import * as applicationUrlService from './js/ApplicationUrlService.js';
import * as applicationBadgeService from './js/ApplicationBadgeService.js';

if (!Vue.prototype.$applicationService) {
  window.Object.defineProperty(Vue.prototype, '$applicationService', {
    value: applicationService,
  });
}

if (!Vue.prototype.$applicationFavoriteService) {
  window.Object.defineProperty(Vue.prototype, '$applicationFavoriteService', {
    value: applicationFavoriteService,
  });
}

if (!Vue.prototype.$applicationPinService) {
  window.Object.defineProperty(Vue.prototype, '$applicationPinService', {
    value: applicationPinService,
  });
}

if (!Vue.prototype.$applicationUrlService) {
  window.Object.defineProperty(Vue.prototype, '$applicationUrlService', {
    value: applicationUrlService,
  });
}

if (!Vue.prototype.$applicationBadgeService) {
  window.Object.defineProperty(Vue.prototype, '$applicationBadgeService', {
    value: applicationBadgeService,
  });
}
