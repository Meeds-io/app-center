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
          <v-layout v-if="favoriteApplicationsList.length > 0" class="favorite appsContainer">
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
                      v-if="application.imageFileId && application.imageFileName"
                      class="appLauncherImage"
                      referrerpolicy="no-referrer"
                      :src="`/app-center/rest/applications/illustration/${application.id}?v=${application.imageLastModified}`"
                      alt="">
                    <img
                      v-else-if="defaultAppImage.fileBody"
                      class="appLauncherImage"
                      referrerpolicy="no-referrer"
                      :src="`/app-center/rest/applications/illustration/${application.id}?v=${application.imageLastModified}`"
                      alt="">
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
      defaultAppImage: {
        fileBody: '',
        fileName: '',
        invalidSize: false,
        invalidImage: false
      },
      isMobileDevice: false,
      applicationsLoaded: false,
      favoriteApplicationsList: [],
      applicationsOrder: null,
      appCenterUserSetupLink: '',
      loading: true,
      draggedElementIndex: null,
      alphabeticalOrder: true,
      appCenterLink: `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup/`,
    };
  },
  watch: {
    favoriteApplicationsList() {
      // check if still alphabetically ordered
      if (this.alphabeticalOrder) {
        const alphabeticalOrder = {};
        this.favoriteApplicationsList.forEach(app => {
          alphabeticalOrder[`${app.id}`] = this.favoriteApplicationsList.indexOf(app);
        });
        if (JSON.stringify(this.applicationsOrder) !== JSON.stringify(alphabeticalOrder)) {
          this.alphabeticalOrder = false;
        }        
      }
      // update an store applications order if no more alphabetically ordered
      if (!this.alphabeticalOrder) {
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
          this.updateApplicationsOrder(applicationsOrder);
        }
      }
    },
  },
  computed: {
    hasApplications() {
      return this.favoriteApplicationsList?.length;
    },
  },
  created() {
    document.addEventListener('app-center-favorite-updated', this.getMandatoryAndFavoriteApplications);
    this.isMobileDevice = this.detectMobile();
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;

    this.applicationsLoaded = false;
    this.getAppGeneralSettings()
      .then(() => this.getMandatoryAndFavoriteApplications())
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
          // sort favorite applications alphabetically by default
          if (this.favoriteApplicationsList.some(app => app.order !== null)) {
            this.alphabeticalOrder = false;
          }

          this.favoriteApplicationsList.sort((a, b) => {
            if (a.order === null && b.order === null) {
              if (a.title < b.title) {
                return -1;
              }
              if (a.title > b.title) {
                return 1;
              }
              return 0;
            }
            if (a.order && b.order === null) {
              return 1;
            }
            if (a.order === null && b.order) {
              return -1;
            }
            if (a.order === b.order) {
              if (a.title < b.title) {
                return -1;
              }
              if (a.title > b.title) {

                return 1;
              }
              return 0;
            }
            return a.order - b.order;

          });

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
    updateApplicationsOrder(applicationsOrder) {
      return fetch('/app-center/rest/favorites', {
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        method: 'PUT',
        body: JSON.stringify(applicationsOrder)
      });
    },
    getAppGeneralSettings() {
      return fetch('/app-center/rest/settings', {
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
          Object.assign(this.defaultAppImage, data && data.defaultApplicationImage);
        });
    },
    getAppIndex(appList, appId) {
      return appList.findIndex(app => app.id === appId);
    },
  }
};
</script>
