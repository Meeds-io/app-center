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
  <v-app flat>
    <v-container px-0 py-0>
      <v-layout class="transparent">
        <v-btn
          id="appcenterLauncherButton"
          :title="$t('appCenter.appLauncher.topbarIcon.tooltip')"
          icon
          class="text-xs-center"
          @click="toggleDrawer()">
          <v-icon class="appCenterLauncherButtonIcon" size="20">
            fa-th
          </v-icon>
        </v-btn>
      </v-layout>
    </v-container>
    <exo-drawer
      ref="appLauncherDrawer"
      :right="!$vuetify.rtl"
      body-classes="hide-scroll"
      class="appCenterDrawer">
      <template #title>
        {{ applicationsLoaded && $t("appCenter.appLauncher.drawer.title") || '' }}
      </template>
      <template #content>
        <template v-if="$root.quickActions?.length">
          <app-center-launcher-quick-actions />
          <v-divider />
        </template>
        <div v-if="hasApplications" class="content">
          <v-layout v-if="hasApplications" class="favorite appsContainer">
            <component
              :is="$root.isMobile && 'div' || 'draggable'"
              v-model="favoriteApplicationsList"
              class="appLauncherList"
              @start="drag=true"
              @end="drag=false">
              <div
                v-for="(application, index) in favoriteApplicationsList"
                :id="'Pos-' + index"
                :key="index"
                class="appLauncherItemContainer">
                <div
                  :id="'App-' + index"
                  class="appLauncherItem">
                  <a
                    :id="application.id"
                    :target="application.target"
                    :href="application.computedUrl">
                    <img
                      v-if="application.imageUrl"
                      :src="application.imageUrl"
                      class="appLauncherImage"
                      referrerpolicy="no-referrer"
                      alt="">
                    <v-icon
                      v-else-if="application.icon"
                      size="65"
                      class="appLauncherImage d-flex align-center justify-center">
                      {{ application.icon }}
                    </v-icon>
                    <img
                      v-else
                      class="appLauncherImage"
                      referrerpolicy="no-referrer"
                      src="/app-center/skin/images/defaultApp.png"
                      alt="">
                    <span
                      v-exo-tooltip.bottom.body="application.title.length > 22 ? application.title : ''"
                      class="appLauncherTitle text-body">
                      {{ application.title }}
                    </span>
                  </a>
                </div>
              </div>
            </component>
          </v-layout>
        </div>
        <div v-else-if="applicationsLoaded" class="content d-flex align-center justify-center">
          <app-center-launcher-empty class="mt-12" />
        </div>
      </template>
      <template #footer>
        <div v-if="applicationsLoaded && hasApplications">
          <v-card
            flat
            tile
            class="d-flex flex justify-end mx-2 px-1">
            <v-btn
              class="text-uppercase caption primary--text seeAllApplicationsBtn"
              outlined
              small
              :href="appCenterLink">
              {{ $t("appCenter.appLauncher.drawer.viewAll") }}
            </v-btn>
          </v-card>
        </div>
      </template>
    </exo-drawer>
  </v-app>
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
      draggedElementIndex: null,
      appCenterLink: `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup/`,
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
    this.toggleDrawer();
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
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
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
  }
};
</script>
