<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3 of the License, or (at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.
You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <exo-drawer
    ref="appLauncherDrawer"
    :right="!$vuetify.rtl"
    body-classes="hide-scroll"
    class="appCenterDrawer">
    <template #title>
      {{ applicationsLoaded && $t("appCenter.appLauncher.drawer.title") || '' }}
    </template>
    <template #titleIcons>
      <v-btn
        :href="$root.appCenterLink"
        :title="$t('appCenter.appLauncher.addAppPlaceHolder')"
        icon
        class="text-xs-center">
        <v-icon size="20">fa-plus</v-icon>
      </v-btn>
    </template>
    <template #content>
      <div v-if="hasApplications" class="content">
        <v-layout v-if="hasApplications" class="favorite appsContainer mt-4">
          <component
            :is="$root.isMobile && 'div' || 'draggable'"
            v-model="favoriteApplicationsList"
            class="appLauncherList"
            @start="drag=true"
            @end="drag=false">
            <v-hover
              v-for="application in favoriteApplicationsList"
              :key="application.id">
              <v-card
                slot-scope="{ hover }"
                v-bind="application.type === 'LINK' && {
                  href: application.computedUrl,
                  target: application.target,
                  rel: 'nofollow noreferrer noopener',
                } || {
                  loading: appLoading === application.url,
                }"
                v-on="application.type !== 'LINK' && {
                  click: () => openApplication(application.type, application.url),
                }"
                :elevation="hover && 2 || 0"
                class="appLauncherItemContainer transparent fill-height d-flex flex-column align-center justify-center mb-2">
                <v-avatar
                  class="d-flex align-center justify-center flex-grow-0 flex-shrink-0 my-2"
                  size="60"
                  tile>
                  <img
                    v-if="application.imageUrl"
                    :src="application.imageUrl"
                    class="appLauncherImage"
                    referrerpolicy="no-referrer"
                    alt="">
                  <v-icon
                    v-else-if="application.icon"
                    size="45"
                    class="appLauncherImage d-flex align-center justify-center">
                    {{ application.icon }}
                  </v-icon>
                  <img
                    v-else
                    class="appLauncherImage"
                    referrerpolicy="no-referrer"
                    src="/app-center/skin/images/defaultApp.png"
                    alt="">
                </v-avatar>
                <div
                  :title="application.title"
                  class="appLauncherTitle text-truncate-2 flex-grow-1 flex-shrink-1 mt-2 mx-2">
                  {{ application.title }}
                </div>
              </v-card>
            </v-hover>
          </component>
        </v-layout>
      </div>
      <div v-else-if="applicationsLoaded" class="content d-flex align-center justify-center">
        <app-center-launcher-empty class="mt-12" />
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data() {
    return {
      isMobileDevice: false,
      applicationsLoaded: false,
      favoriteApplicationsList: [],
      applicationsOrder: null,
      appCenterUserSetupLink: '',
      drag: false,
      loading: true,
      appLoading: null,
      draggedElementIndex: null,
    };
  },
  computed: {
    sortedApplicationsList() {
      const apps = this.favoriteApplicationsList || [];
      apps.sort((a, b) => {
        if (a.order === null && b.order === null) {
          return this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase());
        } else if (a.order === null) {
          return 1;
        } else if (b.order === null) {
          return -1;
        } else {
          return a.order - b.order;
        }
      });
      return apps;
    },
    hasApplications() {
      return this.favoriteApplicationsList?.length;
    },
  },
  watch: {
    drag() {
      if (!this.drag) {
        this.updateApplicationsOrder();
      }
    },
  },
  created() {
    document.addEventListener('app-center-favorite-updated', this.getMandatoryAndFavoriteApplications);
    this.isMobileDevice = this.detectMobile();
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;

    this.applicationsLoaded = false;
    this.getMandatoryAndFavoriteApplications()
      .finally(() => {
        this.applicationsLoaded = true;
        this.$root.$applicationLoaded();
        if (this.$refs.appLauncherDrawer) {
          this.$refs.appLauncherDrawer.endLoading();
        }
      });
  },
  mounted() {
    if (!this.applicationsLoaded) {
      this.$refs.appLauncherDrawer.startLoading();
    }
    if (!this.$root.noAutoOpen) {
      this.toggleDrawer();
    } else {
      if (this.$utils.getQueryParam('appCenterDrawer')) {
        this.openApplication('DRAWER', this.$utils.getQueryParam('appCenterDrawer'));
      }
    }
  },
  methods: {
    detectMobile() {
      const toMatch = [
        /Android/i,
        /webOS/i,
        /iPhone/i,
        /iPad/i,
        /iPod/i,
        /BlackBerry/i,
        /Windows Phone/i
      ];

      return toMatch.some((toMatchItem) => {
        return navigator.userAgent.match(toMatchItem);
      });
    },
    toggleDrawer() {
      this.$refs.appLauncherDrawer.open();
    },
    getMandatoryAndFavoriteApplications() {
      return fetch('/app-center/rest/favorites', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error getting favorite applications list');
          }
        })
        .then(data => {
          // manage system apps localized names
          data.applications.forEach(app => {
            if (app.system) {
              const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (!this.$t(`appCenter.system.application.${appTitle}`).startsWith('appCenter.system.application')) {
                data.applications[this.getAppIndex(data.applications, app.id)].title = this.$t(`appCenter.system.application.${appTitle}`);
              }
            }
          });
          const applications = [];
          if (this.isMobileDevice) {
            applications.push(...data.applications.filter(app => app.mobile));
          } else {
            applications.push(...data.applications);
          }
          this.favoriteApplicationsList = applications.filter(app => app.favorite || app.mandatory);
          this.favoriteApplicationsList = this.sortedApplicationsList.slice();

          // store favorite applications order
          this.applicationsOrder = {};
          this.favoriteApplicationsList.forEach(app => {
            this.applicationsOrder[`${app.id}`] = this.favoriteApplicationsList.indexOf(app);
          });
          this.favoriteApplicationsList.forEach(app => {
            if (app.type === 'LINK') {
              app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
              app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
              app.computedUrl = this.$utils.toLinkUrl(app.computedUrl, {
                urls: true,
                email: true,
                phone: true,
              });
              app.target = app.sameTab ? '_self' : '_blank';
            }
          });
        }).finally(() => this.loading = false);
    },
    updateApplicationsOrder() {
      const applicationsToUpdateOrder = [];
      // check applications order
      this.favoriteApplicationsList.forEach(app => {
        if (this.applicationsOrder[`${app.id}`] !== this.favoriteApplicationsList.indexOf(app)) {
          applicationsToUpdateOrder.push(app);
          this.applicationsOrder[`${app.id}`] = this.favoriteApplicationsList.indexOf(app);
        }
      });
      if (applicationsToUpdateOrder.length) {
        const applicationsOrder = applicationsToUpdateOrder.map(app => {
          return {id: app.id, order: this.applicationsOrder[`${app.id}`]};
        });
        return fetch('/app-center/rest/favorites', {
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          method: 'PUT',
          body: JSON.stringify(applicationsOrder)
        });
      }
    },
    getAppIndex(appList, appId) {
      return appList.findIndex(app => app.id === appId);
    },
    async openApplication(appType, appUrl) {
      if (appType === 'DRAWER' && this.$root.quickActions[appUrl]) {
        this.appLoading = appUrl;
        try {
          await this.$root.quickActions[appUrl].click();
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
        
      }
    },
  }
};
</script>
