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

const appId = 'userPinnedApplications';

export async function init(pinnedApplicationIds, topbarAppsCount) {
  const lang = eXo.env.portal.language || 'en';
  const url = `/app-center/i18n/locale.addon.appcenter?lang=${lang}`;
  const i18n = await exoi18n.loadLanguageAsync(lang, url);
  await Vue.createApp({
    template: `<app-center-pinned-applications id="${appId}"/>`,
    vuetify: Vue.prototype.vuetifyOptions,
    i18n,
    data: () => ({
      resizeObserver: null,
      maxTopbarApps: 10,
      topbarAppsCount,
      pinnedApplicationIds,
      quickActionExtensions: [],
      applications: null,
      topbarParentElement: document.querySelector('#middle-topNavigation-container'),
      topbarMaxWidth: 324,
      topbarElementWidth: 36,
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
      limit() {
        return Math.max(0, this.maxTopbarApps - this.topbarAppsCount);
      },
    },
    watch: {
      topbarAppsCount: {
        immediate: true,
        handler() {
          eXo.env.portal.topbarAppsCount = this.topbarAppsCount;
        },
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
    mounted() {
      this.resizeObserver = new ResizeObserver(this.updateMaxApps).observe(this.topbarParentElement);
    },
    beforeDestroy() {
      document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      document.removeEventListener('app-center-application-unpinned', this.refreshPinnedApplications);
      document.removeEventListener('app-center-application-pinned', this.refreshPinnedApplications);
      this.resizeObserver?.disconnect?.();
    },
    methods: {
      updateMaxApps() {
        const topbarAppsWidth = this.topbarParentElement.offsetWidth - this.$el.offsetWidth;
        this.topbarAppsCount = parseInt(topbarAppsWidth / this.topbarElementWidth) + 1;
      },
      async init() {
        if (this.pinnedApplicationIds?.length) {
          const data = await this.$applicationService.getApplications();
          this.applications = data?.applications || [];
          this.applications.forEach(app => {
            if (app.system) {
              const title = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (this.$te(`appCenter.system.application.${title}`)) {
                app.title = this.$t(`appCenter.system.application.${title}`);
                if (this.$te(`appCenter.system.application.${title}.description`) && !app.description?.length) {
                  app.description = this.$t(`appCenter.system.application.${title}.description`);
                }
              }
            }
            if (app.type === 'LINK' && app.url) {
              const normalizedUrl = app.url.replace(/\\\\/g, '\\');
              const ESC = '__BACKSLASH__';
              const escaped = (normalizedUrl || '').replace(/\\/g, ESC);
              const computedUrl = escaped
                .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`)
                .replace('@user@', eXo.env.portal.userName);
              app.computedUrl = this.$utils.toLinkUrl(computedUrl, {
                urls: true,
                email: true,
                phone: true,
              }).replace(new RegExp(ESC, 'g'), '\\');
            }
          });
        }
      },
      refreshQuickActions() {
        this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
      },
      async refreshPinnedApplications() {
        this.pinnedApplicationIds = await this.$applicationPinService.getPinnedApplications();
        await this.init();
      },
    },
  }, `#${appId}`, 'User Pinned Applications');
}
