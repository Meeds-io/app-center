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
import './initComponents.js';
import '../application-common/initComponents.js';
import './services.js';

//should expose the locale ressources as REST API
const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || 'en';
const urls = [
  `/app-center/i18n/locale.addon.appcenter?lang=${lang}`,
  `/app-center/i18n/locale.portlet.QuickActions?lang=${lang}`
];

const appId = 'adminSetup';

export function init() {
  exoi18n.loadLanguageAsync(lang, urls).then(i18n => {
    Vue.createApp({
      template: `<app-center-admin-setup id="${appId}" />`,
      data: () => ({
        applications: [],
        portletInstances: [],
        quickActionExtensions: [],
        mobilePreview: false,
        collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
      }),
      computed: {
        isMobile() {
          return this.$vuetify.breakpoint.smAndDown;
        },
        quickActions() {
          return this.quickActionExtensions
            .filter(ext => ext.id)
            .map(ext => ({
              id: ext.id,
              icon: ext.icon,
              name: this.$te(ext.name) ? this.$t(ext.name) : ext.name,
              description: this.$te(ext.description) ? this.$t(ext.description) : ext.description,
            }));
        },
      },
      created() {
        document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
        this.refreshQuickActions();
        Vue.prototype.$utils.includeExtensions('QuickActionExtension');
      },
      beforeDestroy() {
        document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      },
      methods: {
        refreshQuickActions() {
          const quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
          quickActionExtensions.sort((a, b) => this.collator.compare(this.$t(a.name).toLowerCase(), this.$t(b.name).toLowerCase()));
          this.quickActionExtensions = quickActionExtensions;
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Application Center Administration');
  });
}
