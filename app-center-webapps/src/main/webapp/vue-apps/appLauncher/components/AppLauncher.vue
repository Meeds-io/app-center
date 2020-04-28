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
      <v-row class="mx-0">
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
      <v-divider :inset="inset" class="my-0 appHeaderBorder" />

      <v-row class="mx-0 px-3">
        <div class="appLauncherList">
          <div
            v-for="(application, index) in favoriteApplicationsList"
            :key="index"
            class="appLauncherItem">
            <a :target="application.target" :href="application.computedUrl">
              <img v-if="application.id" class="appLauncherImage" :src="`/portal/rest/app-center/applications/illustration/${application.id}`">
              <span class="appLauncherTitle">{{ application.title }}</span>
            </a>
          </div>
        </div>
      </v-row>
      
      <v-row class="seeAllApplications mx-0 py-3">
        <v-card
          flat
          tile
          class="d-flex flex justify-center mx-2">
          <a
            class="text-uppercase caption primary--text seeAllApplicationsBtn"
            :href="appCenterUserSetupLink">{{ $t("appCenter.appLauncher.drawer.viewAll") }}</a>
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
      appCenterUserSetupLink: ""
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
    this.getFavoriteApplicationsList();
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape') {
        this.appLauncherDrawer = false;
      }
    });
  },
  methods: {
    toggleDrawer() {
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
          this.favoriteApplicationsList = data.applications;
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          return this.favoriteApplicationsList;
        });
    }
  }
};
</script>
