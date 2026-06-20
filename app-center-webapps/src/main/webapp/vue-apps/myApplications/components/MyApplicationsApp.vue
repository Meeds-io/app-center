<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <v-app id="myApplications">
    <v-hover v-model="hover">
      <widget-wrapper
        ref="widget"
        :loading="isLoading"
        :tabindex="tabindex"
        extra-class="application-body position-static border-box-sizing"
        @focusin="onFocusIn" 
        @focusout="onFocusOut">
        <my-applications-toolbar
          v-if="!isLoading"
          ref="myApplicationsToolbar"
          :hover="hover"
          :is-admin="isAdmin"
          :show-header="showHeader"
          :header-title="headerTitle"
          :has-applications="hasApplications"
          :allow-personal-apps="allowPersonalApps"
          @open-settings="openSettingsDrawer"
          @open-personal-app-form="openPersonalAppForm" />
        <my-applications-list
          :applications-list="filteredApplications"
          :is-loading="isLoading"
          @list-updated="handleListOrderUpdate"
          @open-portlet="$refs.portletInstanceDrawer.open($event)" />
      </widget-wrapper>
    </v-hover>
    <my-applications-settings-drawer
      v-if="isAdmin"
      :settings="$root.settings"
      ref="settingsDrawer"
      @settings-updated="settingsUpdated" />
    <personal-app-form-drawer
      ref="personalAppFormDrawer"
      @saved="getFavoriteApplications" />
    <app-center-portlet-instance-drawer
      ref="portletInstanceDrawer" />
  </v-app>
</template>

<script>

export default {
  data() {
    return {
      favoriteApplications: [],
      applicationsOrder: {},
      isLoading: false,
      alphabeticalOrder: true,
      currentUser: eXo.env.portal.userName,
      initialized: false,
      hover: false,
      tabindex: '0',
      allowPersonalApps: false,
    };
  },
  computed: {
    isAdmin() {
      return this.$root.settings?.isAdmin;
    },
    filteredApplications() {
      return this.$root.isMobile && this.favoriteApplications.filter(application => application.mobile)
                                 || this.favoriteApplications;
    },
    hasApplications() {
      return this.favoriteApplications?.length > 0;
    },
    showHeader() {
      return this.$root.settings?.showHeader;
    },
    headerTitle() {
      return this.$root.settings?.headerTitle;
    },
    maxAppsToList() {
      return this.$root.settings.maxAppsToList;
    }
  },
  created() {
    this.getFavoriteApplications();
    this.loadAppCenterSettings();
    this.$root.isLoading = true;
  },
  methods: {
    loadAppCenterSettings() {
      this.$applicationService.getAppCenterSettings()
        .then(settings => {
          this.allowPersonalApps = settings?.allowUserPersonalApps || false;
        });
    },
    openPersonalAppForm(app) {
      this.$refs.personalAppFormDrawer.open(app || null);
    },
    openSettingsDrawer() {
      this.$refs.settingsDrawer.open();
    },
    settingsUpdated(settings, headerTitle) {
      const updateList = Number(this.maxAppsToList) !== settings.maxAppsToList;
      this.$root.settings.maxAppsToList = settings.maxAppsToList;
      this.$root.settings.showHeader = settings.showHeader;
      this.$root.settings.headerTitle = headerTitle;
      this.$refs.settingsDrawer.close();
      if (updateList) {
        this.getFavoriteApplications();
      }
    },
    getFavoriteApplications() {
      this.isLoading = true;
      return this.$applicationFavoriteService.getFavorites(this.maxAppsToList)
        .then((data) => {
          this.favoriteApplications = (data?.applications || [])
            .map(app => this.mapApplication(app))
            .filter(app => app);
          this.sortAndStoreApplicationsOrder();
        })
        .finally(() => {
          this.isLoading = false;
          this.initialized = true;
        });
    },
    mapApplication(app) {
      if (!app) {
        return null;
      }
      const computedApp = this.computeApplicationUrl(app);
      this.i18nSystemApplicationTitle(computedApp);
      return computedApp;
    },
    sortAndStoreApplicationsOrder() {
      this.alphabeticalOrder = !this.favoriteApplications.some(app => app.order !== null);

      this.favoriteApplications.sort((a, b) => {
        const orderDiff = (a.order ?? Infinity) - (b.order ?? Infinity);
        return orderDiff || a.title.localeCompare(b.title);
      });

      this.applicationsOrder = this.favoriteApplications.reduce((orderMap, app, index) => {
        if (app && app.id) {
          orderMap[app.id] = index;
        }
        return orderMap;
      }, {});
    },
    computeApplicationUrl(app) {
      if (app.type === 'LINK') {
        const normalizedUrl = app.url.replace(/\\\\/g, '\\');
        const ESC = '__BACKSLASH__';
        const escaped = (normalizedUrl || '').replace(/\\/g, ESC);
        let computedUrl = escaped.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`);
        computedUrl = computedUrl.replace('@user@', eXo.env.portal.userName);
        computedUrl = this.$utils.toLinkUrl(computedUrl, {
          urls: true,
          email: true,
          phone: true,
        }).replace(new RegExp(ESC, 'g'), '\\');
        const target = app.sameTab ? '_self' : '_blank';
        return {
          ...app,
          computedUrl,
          target
        };
      } else {
        return app;
      }
    },
    i18nSystemApplicationTitle(app) {
      if (app.system) {
        const title = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
        if (this.$te(`appCenter.system.application.${title}`)) {
          app.title = this.$t(`appCenter.system.application.${title}`);
          if (this.$te(`appCenter.system.application.${title}.description`) && !app.description?.length) {
            app.description = this.$t(`appCenter.system.application.${title}.description`);
          }
        }
      }
    },
    async updateApplicationsOrder(applicationList) {
      const newApplicationsOrders = [];
      for (const [index, app] of applicationList.entries()) {
        const currentOrder = this.applicationsOrder[`${app.id}`];
        if (currentOrder !== index) {
          this.applicationsOrder[`${app.id}`] = index;
          newApplicationsOrders.push({id: app.id, order: index});
        }
      }
      if (newApplicationsOrders.length) {
        await this.$applicationFavoriteService.updateFavoritesOrder(newApplicationsOrders);
        this.favoriteApplications = [...applicationList];
      }
    },
    handleListOrderUpdate(applicationList) {
      if (!this.initialized) {
        return;
      }
      this.updateApplicationsOrder(applicationList);
    },
    onFocusIn(event) {
      this.hover =true;
      this.$nextTick(() => {
        const activeElement = document.activeElement;
        const header = this.$refs.myApplicationsToolbar?.$refs?.btnSeeMoreIcon?.$el;
        if (header && activeElement === this.$refs.widget?.$el && event.relatedTarget) {
          header.focus();
        }
        if (this.tabindex === '0') {
          this.tabindex = '-1';
        }
      });
    },
    onFocusOut(event) {
      const root = this.$el;
      const next = event.relatedTarget;
      if (next && root.contains(next)) {
        return;
      }
      this.tabindex = '0';
      this.hover = false;
    },
  }
};
</script>