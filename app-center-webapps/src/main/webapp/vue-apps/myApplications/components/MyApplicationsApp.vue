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
          :can-edit="canEdit"
          :show-header="showHeader"
          :header-title="headerTitle"
          :has-applications="hasApplications"
          :favorites="!selectedListing"
          @open-settings="openSettingsDrawer" />
        <div
          v-if="canEdit && selectedListing && !hasDisplayedApplications && !isLoading"
          class="d-flex flex-column justify-center align-center flex-grow-1 py-4">
          <p class="mb-2 text-sub-title">
            {{ $t('myApplications.noApps.label') }}
          </p>
          <v-btn
            class="btn btn-primary"
            @click="openSettingsDrawer">
            {{ $t('myApplications.addApps.label') }}
          </v-btn>
        </div>
        <my-applications-list
          v-else
          :applications-list="filteredApplications"
          :is-loading="isLoading"
          :sortable="!selectedListing"
          @list-updated="handleListOrderUpdate"
          @open-portlet="$refs.portletInstanceDrawer.open($event)" />
      </widget-wrapper>
    </v-hover>
    <my-applications-settings-drawer
      v-if="canEdit"
      :settings="$root.settings"
      ref="settingsDrawer"
      @settings-updated="settingsUpdated" />
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
      listingLoaded: false,
    };
  },
  computed: {
    canEdit() {
      return this.$root.settings?.canEdit;
    },
    filteredApplications() {
      return this.$root.isMobile && this.favoriteApplications.filter(application => application.mobile)
                                 || this.favoriteApplications;
    },
    hasDisplayedApplications() {
      return this.filteredApplications?.length > 0;
    },
    portletVisible() {
      // A selected list none of whose apps can be shown is hidden, but from whoever may edit it
      return !this.selectedListing || this.hasDisplayedApplications || this.canEdit;
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
    },
    selectedListing() {
      return this.$root.settings?.listingMode === 'SELECTED';
    },
    settingName() {
      return this.$root.settings?.settingName;
    },
  },
  watch: {
    portletVisible(visible) {
      if (this.listingLoaded) {
        this.$root.$updateApplicationVisibility(visible);
      }
    },
  },
  created() {
    this.getFavoriteApplications();
    this.$root.isLoading = true;
  },
  methods: {
    openSettingsDrawer() {
      this.$refs.settingsDrawer.open();
    },
    settingsUpdated(settings, headerTitle) {
      const updateList = Number(this.maxAppsToList) !== settings.maxAppsToList
          || this.$root.settings.listingMode !== settings.listingMode
          || this.$root.settings.selectionMode !== settings.selectionMode
          || this.$root.settings.applicationIds?.join(',') !== settings.applicationIds;
      this.$root.settings.listingMode = settings.listingMode;
      this.$root.settings.selectionMode = settings.selectionMode;
      this.$root.settings.applicationIds = settings.applicationIds ? settings.applicationIds.split(',').map(Number) : [];
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
      if (this.selectedListing) {
        return this.getListedApplications();
      }
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
    getListedApplications() {
      const applicationIds = this.$root.settings.selectionMode === 'MANUAL' && this.$root.settings.applicationIds || [];
      return this.$myApplicationsService.getListedApplications(this.settingName, applicationIds)
        .catch(() => [])
        .then(applications => {
          this.favoriteApplications = applications
            .map(app => this.mapApplication(app))
            .filter(app => app);
          this.listingLoaded = true;
          this.$root.$updateApplicationVisibility(this.portletVisible);
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
        return {
          ...app,
          computedUrl: this.$applicationUrlService.computeApplicationUrl(app),
          target: app.sameTab ? '_self' : '_blank',
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