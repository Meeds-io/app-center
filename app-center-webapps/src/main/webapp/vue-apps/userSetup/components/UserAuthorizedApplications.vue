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
    <div v-if="loading">
      <v-skeleton-loader
        class="mx-auto"
        type="table-heading"
      >
      </v-skeleton-loader>
    </div>
    <div v-else>    
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
                append-outer-icon="mdi-close"
                hide-details
                @click:append-outer="closeSearch"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-col>
      </v-row>
    </div>
    <div v-if="loading" class="userAuthorizedApplications">
      <div v-for="n in 12" :key="n">
        <v-skeleton-loader
          :key="n"
          class="authorizedApplication"
          type="card"
        ></v-skeleton-loader>
      </div>
    </div>
    <div v-else>    
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
                <a :target="authorizedApp.target" :href="authorizedApp.computedUrl" @click="logOpenApplication(authorizedApp.id)">
                  <img v-if="authorizedApp.imageFileId && authorizedApp.imageFileName" class="appImage" :src="`/portal/rest/app-center/applications/illustration/${authorizedApp.id}`" />
                  <img v-else-if="defaultAppImage.fileBody" class="appImage" :src="`/portal/rest/app-center/applications/illustration/${authorizedApp.id}`" />
                  <img v-else class="appImage" src="/app-center/skin/images/defaultApp.png" />
                </a>
              </div>
              <v-list-item-content>
                <a :target="authorizedApp.target" :href="authorizedApp.computedUrl" @click="logOpenApplication(authorizedApp.id)">
                  <h5 class="tooltipContent">
                    <div 
                      :title="authorizedApp.title.length > 10 ? authorizedApp.title : ''"
                      class="appTitle"
                      :class="!authorizedApp.helpPageURL ? 'noHelpPage' : ''"
                    >
                      {{ authorizedApp.title }}
                    </div>
                  </h5>
                </a>
              </v-list-item-content>
              <template v-if="authorizedApp.helpPageURL">
                <v-list-item-action class="appHelp">
                  <v-btn
                    small
                    icon
                    @click="navigateTo(authorizedApp.helpPageURL)"
                  >
                    <v-icon
                      x-small
                    >
                      mdi-help
                    </v-icon>
                  </v-btn>
                </v-list-item-action>
              </template>
            </v-list-item>
            <v-card-text class="userAppDescription">
              <div 
                :title="authorizedApp.description.length > 105 ? authorizedApp.description : ''"
                class="description"
              >
                {{ authorizedApp.description }}
              </div>
            </v-card-text>
            <v-divider></v-divider>
            <v-card-actions class="applicationActions">
              <a :target="authorizedApp.target" :href="authorizedApp.computedUrl" @click="logOpenApplication(authorizedApp.id)">{{ $t("appCenter.userSetup.authorized.open") }}</a>
              <div 
                :title="getTooltip(authorizedApp)"
              >
                <v-btn
                  v-if="authorizedApp.mandatory"
                  icon
                  disabled
                  class="mandatory"
                >
                  <v-icon
                    small
                    color="red"
                  >
                    mdi-star
                  </v-icon>
                </v-btn>
                <v-btn
                  v-else
                  icon
                  :disabled="authorizedApp.mandatory || (!authorizedApp.favorite && !canAddFavorite)"
                  :class="authorizedApp.mandatory || authorizedApp.favorite ? 'favorite' : ''"
                  @click.stop="addOrDeleteFavoriteApplication(authorizedApp)"
                >
                  <v-icon
                    small
                    color="red"
                  >
                    {{ authorizedApp.mandatory || authorizedApp.favorite ? 'mdi-star' : 'mdi-star-outline' }}
                  </v-icon>
                </v-btn>
              </div>
            </v-card-actions>
          </div>
        </v-card>
      </div>
    </div>
    <v-row class="loadMoreContainer" align="center">
      <v-col>
        <v-btn
          v-if="showPaginator && ! loading"
          class="loadMoreApplicationsBtn"
          :loading="loadingApplications"
          :disabled="loadingApplications"
          block
          @click="loadNextPage"
        >
          {{ $t('appCenter.userSetup.authorized.showMore') }}
        </v-btn>
      </v-col>
    </v-row>
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
    defaultAppImage: {
      type: Object,
      default: function() { return {}; }
    },
  },
  data() {
    return {
      loading: true,
      systemAppNames: [
        'Agenda',
        'Drives',
        'Forum',
        'News',
        'Notes',
        'Tasks',
        'Wallet',
        'Wiki',
      ],
      isMobileDevice: false,
      authorizedApplicationsList: [],
      applicationsListSize: null,
      pageSize: 12,
      offset: 0,
      loadingApplications: true,
      searchText: '',
      searchApp: '',
      searchDelay: 300,
      maxFavoriteApps: '',
      authorizedApplicationsListMsg: this.$t('appCenter.userSetup.loading')
    };
  },
  computed: {
    showPaginator() {
      return parseInt(this.pageSize) === this.applicationsListSize;
    }
  },
  watch: {
    searchText() {
      if (this.searchText && this.searchText.trim().length) {
        document.getElementsByClassName('v-input__icon--append-outer')[0].style.display='block';
        clearTimeout(this.searchApp);
        this.searchApp = setTimeout(() => {
          this.searchAuthorizedApplicationsList();
        }, this.searchDelay);
      } else if (!this.searchText || this.searchText.length !== this.searchText.split(' ').length - 1) {
        document.getElementsByClassName('v-input__icon--append-outer')[0].style.display='none';
        this.getAuthorizedApplicationsList(false, true);
      }
    }
  },
  created() {
    this.isMobileDevice = this.detectMobile();
    this.getMaxFavoriteApps();
    this.getAuthorizedApplicationsList();
  },
  methods: {
    closeSearch(){
      document.getElementsByClassName('v-input__icon--append-outer')[0].style.display='none';
      this.searchText='';
    },
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
    getAuthorizedApplicationsList(searchMode, back) {
      if (back) {
        this.authorizedApplicationsList = [];
        // init offset
        this.offset = 0;
      }
      this.loadingApplications = true;
      let offset = this.offset;
      let limit = this.pageSize;
      if (searchMode) {
        offset = 0;
        limit = 0;
      }
      return fetch(`/portal/rest/app-center/applications/authorized?offset=${offset}&limit=${limit}&keyword=${this.searchText}`, {
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
          const allApplications = [];
          // manage system apps localized names
          data.applications.forEach(app => {
            if (app.system) {
              const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (!this.$t(`appCenter.system.application.${appTitle}`).startsWith('appCenter.system.application')) {
                data.applications[this.getAppIndex(data.applications, app.id)].title = this.$t(`appCenter.system.application.${appTitle}`);
              }
            }
          });
          if (data) {
            if (this.isMobileDevice) {
              allApplications.push(...data.applications.filter(app => app.mobile));
            } else {
              allApplications.push(...data.applications);
            }
          }
          this.authorizedApplicationsList = this.authorizedApplicationsList.concat(allApplications);
          this.authorizedApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.applicationsListSize = data.size;
          this.offset += data.size;
        }).finally(() => {
          this.loadingApplications = false;
          this.loading = false;
        });
    },
    addOrDeleteFavoriteApplication(application) {
      if (!application.favorite) {
        return fetch(`/portal/rest/app-center/applications/favorites/${application.id}`, {
          credentials: 'include',
          method: application.favorite ? 'DELETE' : 'POST',
        }).then(() => {
          application.favorite=!application.favorite;
          return this.$parent.$children[1].getFavoriteApplicationsList();
        });
      } else {
        this.$parent.$children[1].deleteFavoriteApplication(application.id);
      }
    },
    loadNextPage() {
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
      this.getAuthorizedApplicationsList(true);
    },
    logOpenApplication(id) {
      fetch(`/portal/rest/app-center/applications/logClickApplication/${id}`, {
        method: 'GET',
        credentials: 'include',
      });
    },
    navigateTo(link) {
      window.open(link);
    },
    getAppIndex(appList, appId) {
      return appList.findIndex(app => app.id === appId);
    },
    getTooltip(app) {
      if (app.mandatory) {
        return this.$t('appCenter.userSetup.mandatory');
      } else {
        return app.favorite ? this.$t('appCenter.userSetup.remove.from.favorite'): this.$t('appCenter.userSetup.add.to.favorite');
      }
    },
  }
};
</script>
