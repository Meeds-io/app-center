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
import './initComponents.js';

const appCenterApplicationsFetch = Vue.prototype.$applicationService.getApplications();

export function init(parentElementId, topbarApplication) {
  Vue.createApp({
    data: () => ({
      topbarApplication,
      application: null,
      quickActionExtensions: [],
    }),
    computed: {
      applicationId() {
        return Number(this.topbarApplication?.properties?.applicationId);
      },
      quickActions() {
        const quickActions = {};
        this.quickActionExtensions.forEach(ext => quickActions[ext.id] = ext);
        return quickActions;
      },
    },
    created() {
      document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      this.refreshQuickActions();
      this.$utils.includeExtensions('QuickActionExtension');
      this.init();
    },
    beforeDestroy() {
      document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
    },
    methods: {
      init() {
        appCenterApplicationsFetch
          .then(data => this.application = data?.applications?.find?.(app => app.id === this.applicationId));
      },
      refreshQuickActions() {
        this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
      },
    },
    template: `
      <div id="${parentElementId}" class="VuetifyApp">
        <v-app>
          <app-center-topbar-application :application="application" />
        </v-app>
      </div>
    `,
    vuetify: Vue.prototype.vuetifyOptions,
    i18n: exoi18n.i18n,
  }, `#${parentElementId}`, 'Application Center Drawer');
}
