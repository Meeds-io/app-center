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
  <div class="authorizedApplications">
    <v-row class="authorizedApplicationsHeader">
      <v-col sm="8" class="applicationCenterTitle">
        <div class="userApplicationTitle">
          {{ $t("appCenter.userSetup.appCenter") }}
        </div>
      </v-col>
      <v-col class="applicationCenterActions">
        <v-row>
          <v-col class="appSearch">
            <v-text-field
              v-model="searchText"
              :placeholder="`${$t('appCenter.adminSetupList.filter')} ...`"
              prepend-inner-icon="mdi-filter"
              hide-details
            ></v-text-field>
          </v-col>
          <v-col class="adminIcon" cols="1">
            <div id="appCenterAdminSetup">
              <a
                v-if="isAdmin"
                href="/portal/g/:platform:administrators/appCenterAdminSetup"
              >
                <i class="uiIconPLF24x24Setup"></i>
              </a>
            </div>            
          </v-col>
        </v-row>
      </v-col>
    </v-row>
    <div class="userAuthorizedApplications">
      <div v-if="!authorizedApplicationsList || !authorizedApplicationsList.length" class="noApp">
        {{ $t("appCenter.adminSetupForm.noApp") }}
      </div>      
      <v-card
        v-for="(authorizedApp) in authorizedApplicationsList"
        :key="authorizedApp.id"
        class="authorizedApplication"
        outlined
      >
        <div class="authorisedAppContent">
          <v-list-item class="applicationHeader">
            <div class="image">
              <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">
                <img class="appImage" :src="`/portal/rest/app-center/applications/illustration/${authorizedApp.id}`">
              </a>
            </div>
            <v-list-item-content>
              <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">
                <h5 class="tooltipContent">
                  <div 
                    v-exo-tooltip.bottom.body="authorizedApp.title.length > 10 ? authorizedApp.title : ''"
                    class="appTitle"
                  >
                    {{ authorizedApp.title }}
                  </div>
                </h5>
              </a>
            </v-list-item-content>
            <v-list-item-action class="appHelp">
              <v-btn
                small
                icon
              >
                <v-icon 
                  x-small
                >
                  mdi-help
                </v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-card-text class="userAppDescription">
            <div 
              v-exo-tooltip.bottom.body="authorizedApp.description.length > 105 ? authorizedApp.description : ''"
              class="description"
            >
              {{ authorizedApp.description }}
            </div>
          </v-card-text>
          <v-divider></v-divider>
          <v-card-actions class="applicationActions">
            <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">{{ $t("appCenter.userSetup.authorized.open") }}</a>
            <v-btn
              icon
              :disabled="authorizedApp.byDefault || (!authorizedApp.favorite && !canAddFavorite)"
              :class="authorizedApp.byDefault || authorizedApp.favorite ? 'favorite' : ''"
              @click.stop="addOrDeleteFavoriteApplication(authorizedApp)"
            >
              <v-icon
                small
                color="red"
              >
                {{ authorizedApp.byDefault || authorizedApp.favorite ? 'mdi-star' : 'mdi-star-outline' }}
              </v-icon>
            </v-btn>
          </v-card-actions>
        </div>
      </v-card>
    </div>
    <div class="appPaginator">
      <button v-if="showPaginator" @click="nextPage()">
        {{ $t("appCenter.userSetup.authorized.displayMore") }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserAuthorizedApplications',
  props: {
    canAddFavorite: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      isAdmin: eXo.env.portal.isAdmin,
      authorizedApplicationsList: [],
      showPaginator: false,
      currentPage: 1,
      searchText: '',
      searchApp: '',
      searchDelay: 300,
      maxFavoriteApps: '',
      authorizedApplicationsListMsg: this.$t('appCenter.userSetup.loading')
    };
  },
  computed: {
    canAddFavorite() {
      const countFavoriteApps = this.authorizedApplicationsList.filter(app => !app.byDefault && app.favorite).length;
      return countFavoriteApps < this.maxFavoriteApps;
    }
  },
  watch: {
    searchText() {
      if (this.searchText && this.searchText.trim().length) {
        clearTimeout(this.searchApp);
        this.searchApp = setTimeout(() => {
          this.searchAuthorizedApplicationsList();
        }, this.searchDelay);
      } else {
        this.searchAuthorizedApplicationsList();
      }
    }
  },
  created() {
    this.pageSize = this.$parent.pageSize;
    this.getMaxFavoriteApps();
    this.getAuthorizedApplicationsList();
  },
  methods: {
    getAuthorizedApplicationsList() {
      const offset = this.currentPage - 1;
      return fetch(`/portal/rest/app-center/applications/authorized?offset=${offset}&limit=${this.pageSize}&keyword=${this.searchText}`, {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error when getting authorized applications list');
          }
        })
        .then(data => {
          this.authorizedApplicationsList = this.authorizedApplicationsList.concat(data.applications);
          this.authorizedApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          if (this.currentPage * this.pageSize < data.size) {
            this.showPaginator = true;
          } else {
            this.showPaginator = false;
          }
          this.$emit('canAddFavorite', this.canAddFavorite);
        });
    },
    addOrDeleteFavoriteApplication(application) {
      return fetch(`/portal/rest/app-center/applications/favorites/${application.id}`, {
        credentials: 'include',
        method: application.favorite ? 'DELETE' : 'POST',
      })
        .then(() => {
          return this.$parent.$children[1].getFavoriteApplicationsList();
        })
        .then(() => {
          application.favorite = !application.favorite;
          if (!application.favorite) {
            this.$parent.$children[1].deleteFavoriteApplication(application.id);
          }
          this.$emit('canAddFavorite', this.canAddFavorite);
        });
    },
    nextPage() {
      this.currentPage++;
      this.getAuthorizedApplicationsList();
    },
    getMaxFavoriteApps() {
      return fetch('/portal/rest/app-center/settings', {
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
          this.maxFavoriteApps = data.maxFavoriteApps;
        });
    },
    searchAuthorizedApplicationsList() {
      this.authorizedApplicationsList = [];
      this.getAuthorizedApplicationsList();
      this.currentPage = 1;
    }
  }
};
</script>
