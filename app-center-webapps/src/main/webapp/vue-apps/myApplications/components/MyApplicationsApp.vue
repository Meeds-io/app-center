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
    <v-card
      class="d-flex flex-column application-body position-static pa-5 border-box-sizing"
      :loading="isLoading"
      flat>
      <my-applications-toolbar
        v-if="!isLoading"
        :has-applications="hasApplications" />
      <my-applications-list
        :applications-list="filteredApplications"
        :default-app-image="defaultAppImage"
        @list-updated="handleListOrderUpdate" />
    </v-card>
  </v-app>
</template>

<script>

export default {
  data() {
    return {
      favoriteApplications: [],
      applicationsOrder: {},
      defaultAppImage: null,
      isLoading: false,
      alphabeticalOrder: true,
      baseUrl: `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`,
      currentUser: eXo.env.portal.userName,
      initialized: false
    };
  },
  computed: {
    filteredApplications() {
      return this.isMobileDevice && this.favoriteApplications.filter(application => application.mobile)
                                 || this.favoriteApplications;
    },
    isMobileDevice() {
      return /Android|webOS|iPhone|iPad|iPod|BlackBerry|Windows Phone/i.test(navigator.userAgent);
    },
    hasApplications() {
      return this.favoriteApplications?.length > 0;
    }
  },
  created() {
    this.getFavoriteApplications();
  },
  methods: {
    getFavoriteApplications() {
      this.isLoading = true;
      return this.$appCenterService.getFavoriteApplications()
        .then((data) => {
          this.favoriteApplications = (data?.applications || [])
            .map(app => this.mapApplication(app))
            .filter(app => !!app);
          this.defaultAppImage = data?.defaultApplicationImage;
          this.sortAndStoreApplicationsOrder();
        })
        .finally(() => {
          this.isLoading = false;
          this.initialized = true;
        });
    },
    mapApplication(app) {
      if (!app) {return null;}
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
      const computedUrl = app.url.replace(/^\.\//, this.baseUrl)
        .replace('@user@', this.currentUser);
      const target = computedUrl.startsWith('/') ? '_self' : '_blank';
      return { ...app, computedUrl, target };
    },
    i18nSystemApplicationTitle(app) {
      if (app.system) {
        const appTitleKey = app.title.includes(' ')
          ? app.title.replace(/ /g, '.').toLowerCase()
          : app.title.toLowerCase();

        const localizedTitle = this.$t(`appCenter.system.application.${appTitleKey}`);
        if (!localizedTitle.startsWith('appCenter.system.application')) {
          app.title = localizedTitle;
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
        return await this.$appCenterService.updateApplicationsOrder(newApplicationsOrders);
      }
    },
    handleListOrderUpdate(applicationList) {
      if (!this.initialized) {
        return;
      }
      this.updateApplicationsOrder(applicationList);
    }
  }
};
</script>
