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

import './initComponents.js';
import './services.js';

const appId = 'userPinnedApplications';
export async function init(pinnedApplicationIds) {
  await Vue.createApp({
    template: `<app-center-pinned-applications id="${appId}"/>`,
    vuetify: Vue.prototype.vuetifyOptions,
    i18n: exoi18n.i18n,
    data: () => ({
      pinnedApplicationIds,
      quickActionExtensions: [],
      applications: null,
      collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
    }),
    computed: {
      isMobile() {
        return this.$vuetify.breakpoint.smAndDown;
      },
      quickActions() {
        const quickActions = {};
        this.quickActionExtensions.forEach(ext => quickActions[ext.id] = ext);
        return quickActions;
      },
      pinnedApplications() {
        return this.pinnedApplicationIds
          .map(id => this.applications?.find?.(app => app.id === id))
          .filter(app => app);
      },
    },
    created() {
      document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      document.addEventListener('app-center-application-unpinned', this.refreshPinnedApplications);
      document.addEventListener('app-center-application-pinned', this.refreshPinnedApplications);
      this.$utils.includeExtensions('QuickActionExtension');
      this.refreshQuickActions();
      this.init();
    },
    beforeDestroy() {
      document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      document.removeEventListener('app-center-application-unpinned', this.refreshPinnedApplications);
      document.removeEventListener('app-center-application-pinned', this.refreshPinnedApplications);
    },
    methods: {
      async init() {
        if (this.pinnedApplicationIds?.length) {
          this.applications = await this.$applicationService.getApplications();
        }
      },
      refreshQuickActions() {
        this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
      },
      async refreshPinnedApplications() {
        this.applications = await this.$applicationService.getApplications();
        this.pinnedApplicationIds = await this.$applicationPinService.getPinnedApplications();
      },
    },
  }, `#${appId}`, 'User Pinned Applications');
}
