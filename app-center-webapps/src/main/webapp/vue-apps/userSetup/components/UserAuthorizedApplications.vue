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
        type="table-heading" />
    </div>
    <div v-else>    
      <v-row class="authorizedApplicationsHeader">
        <v-col sm="8" class="applicationCenterTitle">
          <div class="userApplicationTitle text-title">
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
                @click:append-outer="closeSearch" />
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
          type="card" />
      </div>
    </div>
    <div v-else>    
      <div class="userAuthorizedApplications">
        <div v-if="!authorizedApplicationsList || !authorizedApplicationsList.length" class="noApp text-header">
          {{ $t("appCenter.adminSetupForm.noApp") }}
        </div>
        <v-row no-gutters>
          <v-col
            v-for="(authorizedApp) in authorizedApplicationsList"
            :key="authorizedApp.id"
            cols="12"
            sm="6"
            lg="4"
            xl="4">
            <v-card
              v-bind="authorizedApp.type === 'LINK' && {
                href: authorizedApp.computedUrl,
                target: authorizedApp.target,
              } || {
                loading: appLoading === authorizedApp.url,
              }"
              v-on="authorizedApp.type !== 'LINK' && {
                click: () => openApplication(authorizedApp.type, authorizedApp.url),
              }"
              class="authorizedApplication"
              height="180"
              max-height="180"
              outlined
              hover>
              <div class="authorisedAppContent border-box-sizing pt-3">
                <div class="flex flex-column align-center justify-center flex-grow-1">
                  <div class="applicationHeader">
                    <div class="image">
                      <a>
                        <img
                          v-if="authorizedApp.imageUrl"
                          :src="authorizedApp.imageUrl"
                          class="appImage"
                          referrerpolicy="no-referrer"
                          alt="">
                        <v-icon
                          v-else-if="authorizedApp.icon"
                          size="45"
                          class="appImage full-width d-flex align-center justify-center mb-2">
                          {{ authorizedApp.icon }}
                        </v-icon>
                        <img
                          v-else
                          class="appImage"
                          referrerpolicy="no-referrer"
                          src="/app-center/skin/images/defaultApp.png"
                          alt="">
                      </a>
                    </div>
                    <div>
                      <a>
                        <div class="tooltipContent">
                          <div
                            :title="authorizedApp.title.length > 10 ? authorizedApp.title : ''"
                            class="appTitle text-body"
                            :class="!authorizedApp.helpPageURL ? 'noHelpPage' : ''">
                            {{ authorizedApp.title }}
                          </div>
                        </div>
                      </a>
                    </div>
                  </div>
                  <v-card-text class="userAppDescription">
                    <div
                      :title="authorizedApp.description.length > 105 ? authorizedApp.description : ''"
                      class="description text-subtitle">
                      {{ authorizedApp.description }}
                    </div>
                  </v-card-text>
                </div>
                <v-divider />
                <v-card-actions class="applicationActions">
                  <a>{{ $t("appCenter.userSetup.authorized.open") }}</a>
                  <div class="actionsBtn">
                    <v-btn
                      v-if="authorizedApp.helpPageURL"
                      class="appHelp"
                      x-small
                      icon
                      @click.prevent.stop="navigateTo(authorizedApp.helpPageURL)">
                      <v-icon
                        x-small>
                        mdi-help
                      </v-icon>
                    </v-btn>
                    <div :title="getTooltip(authorizedApp)">
                      <v-btn
                        v-if="authorizedApp.mandatory"
                        x-small
                        icon
                        disabled
                        class="mandatory">
                        <v-icon
                          small
                          color="red">
                          mdi-star
                        </v-icon>
                      </v-btn>
                      <v-btn
                        v-else
                        x-small
                        icon
                        :disabled="authorizedApp.mandatory || (!authorizedApp.favorite && !canAddFavorite)"
                        :class="authorizedApp.mandatory || authorizedApp.favorite ? 'favorite' : ''"
                        @click.prevent.stop="addOrDeleteFavoriteApplication(authorizedApp)">
                        <v-icon
                          small
                          color="red">
                          {{ authorizedApp.mandatory || authorizedApp.favorite ? 'mdi-star' : 'mdi-star-outline' }}
                        </v-icon>
                      </v-btn>
                    </div>
                  </div>
                </v-card-actions>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </div>
    <v-row class="loadMoreContainer" align="center">
      <v-col>
        <v-btn
          v-if="showPaginator"
          class="loadMoreApplicationsBtn"
          :loading="loadingApplications"
          :disabled="loadingApplications"
          block
          @click="loadNextPage">
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
  },
  data() {
    return {
      loading: true,
      appLoading: null,
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
      return parseInt(this.offset) < this.applicationsListSize && !this.loading;
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
    Promise.all([
      this.getMaxFavoriteApps(),
      this.getAuthorizedApplicationsList()
    ]).finally(() => this.$root.$applicationLoaded());
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
      return fetch(`/app-center/rest/applications?offset=${offset}&limit=${limit}&keyword=${this.searchText}`, {
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
              if (this.$te(`appCenter.system.application.${appTitle}`)) {
                app.title = this.$t(`appCenter.system.application.${appTitle}`);
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
          this.authorizedApplicationsList = this.authorizedApplicationsList.sort((a, b) => {
            return this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase());
          });
          this.authorizedApplicationsList.forEach(app => {
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
          this.applicationsListSize = data.size;
          this.offset += data.applications.length;
        }).finally(() => {
          this.loadingApplications = false;
          this.loading = false;
        });
    },
    addOrDeleteFavoriteApplication(application) {
      if (!application.favorite) {
        return fetch(`/app-center/rest/favorites/${application.id}`, {
          credentials: 'include',
          method: application.favorite ? 'DELETE' : 'POST',
        }).then(() => {
          application.favorite=!application.favorite;
          return this.$parent.$children[1].getFavoriteApplicationsList();
        }).finally(() => document.dispatchEvent(new CustomEvent('app-center-favorite-updated')));
      } else {
        this.$parent.$children[1].deleteFavoriteApplication(application.id);
      }
    },
    loadNextPage() {
      this.getAuthorizedApplicationsList();
    },
    getMaxFavoriteApps() {
      return fetch('/app-center/rest/settings', {
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
