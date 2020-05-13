<template>
  <v-app id="appLauncher" flat>
    <v-container px-0 py-0>
      <v-layout class="transparent">
        <v-btn icon small color="transparent" class="uiApplicationIconButton">
          <v-icon class="text-xs-center uiIcon uiApplicationIcon" @click="toggleDrawer()" />
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
      class="appCenterDrawer">
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
              @click="toggleDrawer()"></i>
          </v-list-item-action>
        </v-list-item>
      </v-row>
      
      <v-divider class="my-0 appHeaderBorder" />

      <div class="content">
        <v-layout class="mx-0 px-3">
          <div>
            <div v-if="loading">
              <v-skeleton-loader
                class="mx-auto"
                type="table-cell@3">
              </v-skeleton-loader>
              <v-skeleton-loader
                class="mx-auto"
                type="table-cell@3">
              </v-skeleton-loader>
              <v-skeleton-loader
                class="mx-auto"
                type="table-cell@3">
              </v-skeleton-loader>
              <v-skeleton-loader
                class="mx-auto"
                type="table-cell@3">
              </v-skeleton-loader>
            </div>
            <div v-else class="appLauncherList">
              <div
                v-for="(application, index) in favoriteApplicationsList"
                :key="index"
                :id="'Pos-' + index"
                class="appLauncherItemContainer">
                <div
                  :id="'App-' + index"
                  class="appLauncherItem">
                  <a
                    :target="application.target"
                    :href="application.computedUrl"
                    :id="application.id">
                    <img v-if="application.id" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`">
                    <span class="appLauncherTitle">{{ application.title }}</span>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </v-layout>
      </div>
      
      <v-row class="drawerActions mx-0 px-3">
        <v-card
          flat
          tile
          class="d-flex flex justify-end mx-2">
          <v-btn
            class="text-uppercase caption primary--text seeAllApplicationsBtn"
            outlined
            small
            @click="navigateTo('appCenterUserSetup/')">
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
      appLauncherDrawer: null,
      favoriteApplicationsList: [],
      appCenterUserSetupLink: "",
      loading: true,
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
  },
  created() {
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape') {
        this.appLauncherDrawer = false;
      }
    });
  },
  methods: {
    toggleDrawer() {
      if (!this.appLauncherDrawer) {
        this.getFavoriteApplicationsList();
        //only when opening the appLauncherDrawer
        fetch("/portal/rest/app-center/applications/logOpenDrawer", {
          method: "GET",
          credentials: 'include',
        });
      }

      this.appLauncherDrawer = !this.appLauncherDrawer;
    },
    getFavoriteApplicationsList() {
      return fetch("/portal/rest/app-center/applications/favorites", {
        method: "GET",
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error("Error getting favorite applications list");
          }
        })
        .then(data => {
          // sort favorite applications alphabetical by default
          this.favoriteApplicationsList = data.applications.sort((a, b) => {
            if (a.title < b.title) {
              return -1;
            }
            
            if (a.title > b.title) {
              return 1;
            }
            
            return 0;
          });
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          return this.favoriteApplicationsList;
        }).finally(() => this.loading = false);
    },
    navigateTo(link) {
      location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/${link}`;
    },
  }
};
</script>
