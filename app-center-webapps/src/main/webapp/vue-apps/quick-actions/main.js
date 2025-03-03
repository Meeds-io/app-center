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
import './extensions.js';
import './services.js';

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('QuickActions');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo?.env.portal.language || 'en';
const url = `/app-center/i18n/locale.portlet.QuickActions?lang=${lang}`;

const appId = 'quickActions';
export async function init() {
  const i18n = await exoi18n.loadLanguageAsync(lang, url);
  try {
    await Vue.createApp({
      template: `<quick-actions id="${appId}"/>`,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
      data: () => ({
        quickActionExtensions: [],
        quickActionsStatus: {},
        collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
      }),
      computed: {
        isMobile() {
          return this.$vuetify.breakpoint.smAndDown;
        },
        quickActions() {
          const quickActions = this.quickActionExtensions
            .filter(ext => ext.id)
            .map(ext => ({
              id: ext.id,
              icon: ext.icon,
              name: this.$te(ext.name) ? this.$t(ext.name) : ext.name,
              description: this.$te(ext.description) ? this.$t(ext.description) : ext.description,
              disabled: this.quickActionsStatus[ext.id] || false,
            }));
          quickActions.sort((a, b) => this.collator.compare(a.name.toLowerCase(), b.name.toLowerCase()));
          return quickActions;
        },
      },
      async created() {
        this.quickActionsStatus = await this.$quickActionService.getQuickActionStatus();
        document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
        this.refreshQuickActions();
      },
      beforeDestroy() {
        document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      },
      methods: {
        refreshQuickActions() {
          this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
        },
      },
    }, `#${appId}`, 'Quick Actions');
  } finally {
    Vue.prototype.$utils.includeExtensions('QuickActionExtension');
  }
}
