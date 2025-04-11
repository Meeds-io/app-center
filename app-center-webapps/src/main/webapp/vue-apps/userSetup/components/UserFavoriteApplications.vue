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
      <div class="favoriteAppsTitle text-title">
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
        <v-list-item
          v-bind="favoriteApp.type === 'LINK' && {
            href: favoriteApp.computedUrl,
            target: favoriteApp.target,
          } || {
            loading: appLoading === favoriteApp.url,
          }"
          v-on="favoriteApp.type !== 'LINK' && {
            click: () => openApplication(favoriteApp.type, favoriteApp.url),
          }">
          <div class="favoriteAppImage">
            <a>
              <img
                v-if="favoriteApp.imageUrl"
                :src="favoriteApp.imageUrl"
                class="appImage"
                referrerpolicy="no-referrer"
                alt="">
              <v-icon
                v-else-if="favoriteApp.icon"
                size="45"
                class="appImage d-flex align-center justify-center">
                {{ favoriteApp.icon }}
              </v-icon>
              <img
                v-else
                class="appImage"
                referrerpolicy="no-referrer"
                src="/app-center/skin/images/defaultApp.png"
                alt="">
            </a>
          </div>
          <v-list-item-content>
            <a class="favoriteAppUrl">
              <div
                :title="favoriteApp.title.length > 20 ? favoriteApp.title : ''"
                class="favAppTitle text-body">
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
              @click.prevent.stop="deleteFavoriteApplication(favoriteApp.id)">
              <v-icon>mdi-star</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-card>
    </div>
    <div v-show="!loading">
      <div v-if="canAddFavorite" class="maxFavorite text-subtitle">
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
      return fetch('/app-center/rest/favorites', {
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
          this.favoriteApplicationsList = allApplications;

          // check if favorite applications are alphabetically ordered
          this.favoriteApplicationsList.sort((a, b) => {
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

          this.favoriteApplicationsList.forEach(app => {
            if (app.type === 'LINK') {
              app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
              app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
              app.computedUrl = this.$utils.toLinkUrl(app.computedUrl, {
                urls: true,
                email: true,
                phone: true,
              });
              app.target = app.url.indexOf('/') === 0 || app.url.indexOf('./') === 0 || app.computedUrl.indexOf('tel:') === 0 || app.computedUrl.indexOf('mailto:') === 0 ? '_self' : '_blank';
            }
          });
          this.$emit('canAddFavorite', this.canAddFavorite);
          return this.favoriteApplicationsList;
        }).finally(() => this.loading = false);
    },
    deleteFavoriteApplication(appId) {
      return fetch(`/app-center/rest/favorites/${appId}`, {
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
        }).finally(() => document.dispatchEvent(new CustomEvent('app-center-favorite-updated')));
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
