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
  <div class="userFavoriteApplications">
    <div v-if="loading" class="favoriteAppsTitle">
      <v-skeleton-loader
        class="mx-auto"
        type="card-heading" />
    </div>
    <div v-else>
      <div class="favoriteAppsTitle">
        {{ $t("appCenter.userSetup.favorite") }}
      </div>
    </div>

    <div v-if="loading">
      <div v-for="n in 8" :key="n">
        <v-skeleton-loader
          class="mx-auto"
          type="table-heading" />
      </div>      
    </div>
    <div v-else>
      <v-card
        v-for="(favoriteApp, index) in favoriteApplicationsList"
        :key="index"
        class="favoriteApplication"
        height="65"
        max-width="auto"
        outlined>
        <v-list-item>
          <div class="favoriteAppImage">
            <a
              :target="favoriteApp.target"
              :href="favoriteApp.computedUrl"
              @click="logOpenApplication(favoriteApp.id)">
              <img
                v-if="favoriteApp.imageFileId && favoriteApp.imageFileName"
                class="appImage"
                referrerpolicy="no-referrer"
                :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}?v=${favoriteApp.imageLastModified}`">
              <img
                v-else-if="defaultAppImage.fileBody"
                class="appImage"
                referrerpolicy="no-referrer"
                :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}?v=${favoriteApp.imageLastModified}`">
              <img
                v-else
                class="appImage"
                referrerpolicy="no-referrer"
                src="/app-center/skin/images/defaultApp.png">
            </a>
          </div>
          <v-list-item-content>
            <a
              class="favoriteAppUrl"
              :target="favoriteApp.target"
              :href="favoriteApp.computedUrl"
              @click="logOpenApplication(favoriteApp.id)">
              <div
                :title="favoriteApp.title.length > 20 ? favoriteApp.title : ''"
                class="favAppTitle">
                {{ favoriteApp.title }}
              </div>
            </a>
          </v-list-item-content>
          <v-list-item-action
            :title="favoriteApp.mandatory ? $t('appCenter.userSetup.mandatory') : $t('appCenter.userSetup.remove.from.favorite')"
            class="favoriteAppRemove">
            <v-btn
              :disabled="favoriteApp.mandatory"
              :class="favoriteApp.mandatory ? 'mandatory' : ''"
              icon
              @click.stop="deleteFavoriteApplication(favoriteApp.id)">
              <v-icon>mdi-star</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-card>
    </div>
    <div v-show="!loading">
      <div v-if="canAddFavorite" class="maxFavorite">
        <v-icon class="notReached">
          info
        </v-icon>
        <span>{{ $t("appCenter.userSetup.maxFavoriteApps.not.reached", {0: $parent.$children[0].maxFavoriteApps}) }}</span>
      </div>
      <div v-else class="maxFavorite reached">
        <v-icon>
          mdi-alert
        </v-icon>
        <span>{{ $t("appCenter.userSetup.maxFavoriteApps.reached") }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserFavoriteApplications',
  props: {
    defaultAppImage: {
      type: Object,
      default: function() { return {}; }
    },
  },
  data() {
    return {
      isMobileDevice: false,
      favoriteApplicationsList: [],
      loading: true,
      canAddFavorite: false,
    };
  },
  created() {
    this.isMobileDevice = this.detectMobile();
    this.getFavoriteApplicationsList();
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
    getFavoriteApplicationsList() {
      return fetch('/portal/rest/app-center/applications/favorites', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error when getting the general applications list');
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
          this.canAddFavorite = data.canAddFavorite;
          const allApplications = [];
          if (data) {
            if (this.isMobileDevice) {
              allApplications.push(...data.applications.filter(app => app.mobile));
            } else {
              allApplications.push(...data.applications);
            }
          }
          const mandatoryApps = allApplications.filter(app => app.mandatory && !app.favorite);
          const favoriteApps = allApplications.filter(app => app.favorite && !app.mandatory);
          mandatoryApps.sort((a, b) => {
            if (a.title < b.title) {
              return -1;
            }

            if (a.title > b.title) {
              return 1;
            }

            return 0;
          });
          // check if favorite applications are alphabetically ordered
          if (!favoriteApps.some(app => app.order !== null)) {
            favoriteApps.sort((a, b) => {
              if (a.title < b.title) {
                return -1;
              }

              if (a.title > b.title) {
                return 1;
              }

              return 0;
            });
          }
          this.favoriteApplicationsList = [];
          this.favoriteApplicationsList.push(...mandatoryApps);
          this.favoriteApplicationsList.push(...favoriteApps);
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.$emit('canAddFavorite', this.canAddFavorite);
          return this.favoriteApplicationsList;
        }).finally(() => this.loading = false);
    },
    logOpenApplication(id) {
      fetch(`/portal/rest/app-center/applications/logClickApplication/${id}`, {
        method: 'GET',
        credentials: 'include',
      });
    },
    deleteFavoriteApplication(appId) {
      return fetch(`/portal/rest/app-center/applications/favorites/${appId}`, {
        method: 'DELETE',
        credentials: 'include',
      })
        .then(() => {
          return this.getFavoriteApplicationsList();
        })
        .then(() => {
          const index = this.$parent.$children[0].authorizedApplicationsList.findIndex(
            app => app.id === appId
          );
          this.$parent.$children[0].authorizedApplicationsList[index].favorite = false;
        });
    },
    getAppIndex(appList, appId) {
      return appList.findIndex(app => app.id === appId);
    },
  }
};
</script>
