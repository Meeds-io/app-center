<template>
  <v-app
    id="app-center-launcher"
    class="transparent"
    flat>
    <v-container
      px-0
      py-0
      class="transparent">
      <v-layout class="transparent">
        <v-icon
          icon
          class="text-xs-center uiApplicationIcon"
          @click="toggleDrawer()" />
        <v-navigation-drawer
          v-model="appLauncherDrawer"
          right
          absolute
          temporary
          width="420"
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
          <v-row class="seeAllApplications mx-0">
            <v-card
              flat
              tile
              class="d-flex flex justify-end mx-2">
              <a
                class="text-uppercase caption primary--text seeAllApplicationsBtn"
                :href="appCenterUserSetupLink">{{ $t("appCenter.appLauncher.drawer.viewAll") }}</a>
            </v-card>
          </v-row>
        </v-navigation-drawer>
      </v-layout>
    </v-container>
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
  created() {
    this.getFavoriteApplicationsList();
    window.require(["SHARED/appCenterBundle"], function(appCenterBundle) {
      appCenterBundle.init();
    });
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;
  },
  methods: {
    toggleDrawer() {
      this.appLauncherDrawer = !this.appLauncherDrawer;
    },
    getFavoriteApplicationsList() {
      return fetch("/portal/rest/app-center/applications/favorites", {
        method: "GET"
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error("Error getting favorite applications list");
          }
        })
        .then(data => {
          this.favoriteApplicationsList = (data && data.applications) || [];
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/$\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.url.indexOf('/') === 0 ? '_self' : '_blank';
          });
          return this.favoriteApplicationsList;
        });
    }
  }
};
</script>
