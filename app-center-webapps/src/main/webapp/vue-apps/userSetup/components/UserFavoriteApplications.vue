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
    <div class="favoriteAppsTitle">
      {{ $t("appCenter.userSetup.favorite") }}
    </div>

    <v-card
      v-for="(favoriteApp, index) in favoriteApplicationsList"
      :key="index"
      class="favoriteApplication"
      height="65"
      max-width="auto"
      outlined
    >
      <v-list-item>
        <div class="favoriteAppImage">
          <a :target="favoriteApp.target" :href="favoriteApp.computedUrl">
            <img class="appImage" :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}`">
          </a>
        </div>
        <v-list-item-content>
          <a
            class="favoriteAppUrl"
            :target="favoriteApp.target"
            :href="favoriteApp.computedUrl"
          >
            <div
              v-exo-tooltip.bottom.body="favoriteApp.title.length > 20 ? favoriteApp.title : ''"
              class="favAppTitle"
            >
              {{ favoriteApp.title }}
            </div>
          </a>
        </v-list-item-content>
        <v-list-item-action
          v-exo-tooltip.bottom.body="favoriteApp.byDefault ? 'Test Tooltip' : ''"
          class="favoriteAppRemove"
        >
          <v-btn
            :disabled="favoriteApp.byDefault"
            :class="favoriteApp.byDefault ? 'mandatory' : ''"
            icon
            @click.stop="deleteFavoriteApplication(favoriteApp.id)"
          >
            <v-icon>mdi-star</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-card>
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
  data() {
    return {
      favoriteApplicationsList: [],
      loading: true,
      canAddFavorite: false,
    };
  },
  created() {
    this.getFavoriteApplicationsList();
  },
  methods: {
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
          this.canAddFavorite = data.canAddFavorite;
          const allApplications = data && data.applications || [];
          const mandatoryApps = allApplications.filter(app => app.byDefault && !app.favorite);
          const favoriteApps = allApplications.filter(app => app.favorite && !app.byDefault);
          mandatoryApps.sort((a, b) => {
            if (a.title < b.title) {
              return -1;
            }

            if (a.title > b.title) {
              return 1;
            }

            return 0;
          });
          if (favoriteApps.some(app => app.order === null)) {
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
    }
  }
};
</script>
