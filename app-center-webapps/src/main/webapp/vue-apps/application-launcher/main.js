
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

//should expose the locale ressources as REST API
const appId = 'appLauncher';

let initialized = false;

export async function init({
  isAdmin,
  pinnedApplicationIds,
  autoInitDrawerId,
  autoInitPortletId,
}, noAutoOpen, shortcuts, shortcut) {
  if (initialized) {
    return;
  }
  initialized = true;
  let appElement = document.querySelector(`#${appId}`);
  const hideApp = !appElement;
  if (!appElement) {
    appElement = document.querySelector('#appShortcuts');
  }

  const lang = eXo?.env?.portal?.language || 'en';
  const urls = [
    `/app-center/i18n/locale.addon.appcenter?lang=${lang}`,
    `/app-center/i18n/locale.portlet.QuickActions?lang=${lang}`
  ];
  //getting locale ressources
  const i18n = await exoi18n.loadLanguageAsync(lang, urls);
  try {
    await Vue.createApp({
      data: () => ({
        isAdmin,
        noAutoOpen,
        hideApp,
        shortcut,
        shortcuts: shortcuts?.filter?.(c => c?.length)?.map?.(c => c.toLowerCase()) || [],
        pinnedApplicationIds,
        autoInitDrawerId,
        autoInitPortletId,
        canPinApps: !!document.querySelector('#userPinnedApplications'),
        quickActionExtensions: [],
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
      },
      created() {
        document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
        document.addEventListener('app-center-application-unpinned', this.refreshPinnedApplications);
        document.addEventListener('app-center-application-pinned', this.refreshPinnedApplications);
        this.refreshQuickActions();
        this.$utils.includeExtensions('QuickActionExtension');
      },
      beforeDestroy() {
        document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
        document.removeEventListener('app-center-application-unpinned', this.refreshPinnedApplications);
        document.removeEventListener('app-center-application-pinned', this.refreshPinnedApplications);
      },
      methods: {
        refreshQuickActions() {
          this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
        },
        async refreshPinnedApplications() {
          this.pinnedApplicationIds = await this.$applicationPinService.getPinnedApplications();
        },
      },
      template: `<app-center-launcher id="${appId}" />`,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n: i18n,
    }, appElement, 'Application Center Drawer');
  } finally {
    Vue.prototype.$utils.includeExtensions('QuickActionExtension');
  }
}
