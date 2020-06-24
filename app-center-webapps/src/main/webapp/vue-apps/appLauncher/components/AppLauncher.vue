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
  <v-app id="appLauncher" flat>
    <v-container px-0 py-0>
      <v-layout class="transparent">
        <v-btn icon class="text-xs-center" @click="toggleDrawer()">
          <v-icon class="grey-color">
            mdi-apps
          </v-icon>
        </v-btn>
      </v-layout>
    </v-container>
    <v-navigation-drawer
      v-model="appLauncherDrawer"
      absolute
      right
      stateless
      temporary
      width="420"
      max-width="100vw"
      max-height="100vh"
      class="appCenterDrawer"
    >
      <v-row class="mx-0 title">
        <v-list-item class="appLauncherDrawerHeader">
          <v-list-item-content>
            <span class="appLauncherDrawerTitle">{{
              $t("appCenter.appLauncher.drawer.title")
            }}</span>
          </v-list-item-content>
          <v-list-item-action class="appLauncherDrawerIcons">
            <i
              class="uiCloseIcon appLauncherDrawerClose"
              @click="toggleDrawer()"
            ></i>
          </v-list-item-action>
        </v-list-item>
      </v-row>
      
      <v-divider class="my-0 appHeaderBorder" />

      <div class="content">
        <v-row v-if="mandatoryApplicationsList.length > 0" class="mandatory appsContainer">
          <v-col v-model="mandatoryApplicationsList" class="appLauncherList">
            <div
              v-for="(application, index) in mandatoryApplicationsList"
              :id="'Pos-' + index"
              :key="index"
              class="appLauncherItemContainer"
            >
              <div
                :id="'App-' + index"
                class="appLauncherItem"
              >
                <a
                  :id="application.id"
                  :target="application.target"
                  :href="application.computedUrl"
                >
                  <img v-if="application.imageFileId" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`" />
                  <img v-else-if="defaultAppImage.fileBody" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`" />
                  <img v-else class="appLauncherImage" src="/app-center/skin/images/defaultApp.png" />
                  <span
                    v-exo-tooltip.bottom.body="application.title.length > 22 ? application.title : ''"
                    class="appLauncherTitle"
                  >
                    {{ application.title }}
                  </span>
                </a>
              </div>
            </div>
          </v-col>
        </v-row>
        <v-row v-if="favoriteApplicationsList.length > 0 && mandatoryApplicationsList.length > 0" class="appsContainer">
          <v-divider></v-divider>
        </v-row>
        <v-layout class="favorite appsContainer">
          <draggable v-model="favoriteApplicationsList" class="appLauncherList" @start="drag=true" @end="drag=false">
            <div
              v-for="(application, index) in favoriteApplicationsList"
              :id="'Pos-' + index"
              :key="index"
              class="appLauncherItemContainer"
            >
              <div
                :id="'App-' + index"
                class="appLauncherItem"
              >
                <a
                  :id="application.id"
                  :target="application.target"
                  :href="application.computedUrl"
                >
                  <img v-if="application.imageFileId" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`" />
                  <img v-else-if="defaultAppImage.fileBody" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`" />
                  <img v-else class="appLauncherImage" src="/app-center/skin/images/defaultApp.png" />
                  <span 
                    v-exo-tooltip.bottom.body="application.title.length > 22 ? application.title : ''"
                    class="appLauncherTitle"
                  >
                    {{ application.title }}
                  </span>
                </a>
              </div>
            </div>
          </draggable>
        </v-layout>
      </div>
      
      <v-row class="drawerActions mx-0">
        <v-card
          flat
          tile
          class="d-flex flex justify-end mx-2 px-1"
        >
          <v-btn
            class="text-uppercase caption primary--text seeAllApplicationsBtn"
            outlined
            small
            @click="navigateTo('appCenterUserSetup/')"
          >
            {{ $t("appCenter.appLauncher.drawer.viewAll") }}
          </v-btn>
        </v-card>
      </v-row>
    </v-navigation-drawer>
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
      appLauncherDrawer: null,
      mandatoryApplicationsList: [],
      favoriteApplicationsList: [],
      applicationsOrder: null,
      appCenterUserSetupLink: '',
      loading: true,
      draggedElementIndex: null,
      alphabeticalOrder: true,
    };
  },
  watch: {
    appLauncherDrawer() {
      if (this.appLauncherDrawer) {
        $('body').addClass('hide-scroll');

        this.$nextTick().then(() => {
          $('#appLauncher .v-overlay').click(() => {
            this.appLauncherDrawer = false;
          });
        });
      } else {
        $('body').removeClass('hide-scroll');
      }
    },
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
        if (applicationsToUpdateOrder && applicationsToUpdateOrder.length > 0) {
          const applicationsOrder = applicationsToUpdateOrder.map(app => {
            return {id: app.id, order: this.applicationsOrder[`${app.id}`]};
          });
          this.updateApplicationsOrder(applicationsOrder);
        }
      }
    },
  },
  created() {
    this.isMobileDevice = this.detectMobile();
    this.getAppGeneralSettings();
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape') {
        this.appLauncherDrawer = false;
      }
    });
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
      if (!this.appLauncherDrawer) {
        this.getMandatoryAndFavoriteApplications();
        //only when opening the appLauncherDrawer
        fetch('/portal/rest/app-center/applications/logOpenDrawer', {
          method: 'GET',
          credentials: 'include',
        });
      }

      this.appLauncherDrawer = !this.appLauncherDrawer;
    },
    getMandatoryAndFavoriteApplications() {
      return fetch('/portal/rest/app-center/applications/favorites', {
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
          const applications = [];
          if (this.isMobileDevice) {
            applications.push(...data.applications.filter(app => app.mobile));
          } else {
            applications.push(...data.applications);
          }
          this.mandatoryApplicationsList = applications.filter(app => app.mandatory && !app.favorite);
          // sort mandatory applications alphabetical
          this.mandatoryApplicationsList.sort((a, b) => {
            if (a.title < b.title) {
              return -1;
            }

            if (a.title > b.title) {
              return 1;
            }

            return 0;
          });
          this.favoriteApplicationsList = applications.filter(app => app.favorite && !app.mandatory);
          // sort favorite applications alphabetically by default
          if (this.favoriteApplicationsList.some(app => app.order !== null)) {
            this.alphabeticalOrder = false;
          } else {
            this.favoriteApplicationsList.sort((a, b) => {
              if (a.title < b.title) {
                return -1;
              }

              if (a.title > b.title) {
                return 1;
              }

              return 0;
            });            
          }
          // store favorite applications order
          this.applicationsOrder = {};
          this.favoriteApplicationsList.forEach(app => {
            this.applicationsOrder[`${app.id}`] = this.favoriteApplicationsList.indexOf(app);
          });
          
          this.mandatoryApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
        }).finally(() => this.loading = false);
    },
    updateApplicationsOrder(applicationsOrder) {
      return fetch('/portal/rest/app-center/applications/favorites', {
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        method: 'PUT',
        body: JSON.stringify(applicationsOrder)
      });
    },
    navigateTo(link) {
      if (link==='appCenterUserSetup/') {
        fetch('/portal/rest/app-center/applications/logClickAllApplications', {
          method: 'GET',
          credentials: 'include',
        });
      }
      location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/${link}`;
    },
    getAppGeneralSettings() {
      return fetch('/portal/rest/app-center/settings', {
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
  }
};
</script>
