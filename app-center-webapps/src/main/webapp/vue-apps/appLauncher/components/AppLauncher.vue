<template>
  <v-app
    id="app-center-launcher"
    class="VuetifyApp transparent"
    flat>
    <v-container
      px-0
      py-0
      class="transparent">
        <v-layout
          class="transparent">
            <v-icon
              icon
              class="text-xs-center uiApplicationIcon"
              @click="toggleDrawer()" >
            </v-icon>
            <v-navigation-drawer
                    v-model="appLauncherDrawer"
                    right
                    absolute
                    temporary
                    width="380"
                    class="appCenterDrawer">
                <v-row class="mx-0">
                    <v-list-item class="appLauncherDrawerHeader">
                        <v-list-item-content>
                            <span class="appLauncherDrawerTitle">{{ $t('appCenter.appLauncher.drawer.title') }}</span>
                        </v-list-item-content>
                        <v-list-item-action class="appLauncherDrawerIcons">
                            <i class="uiCloseIcon appLauncherDrawerClose" @click="toggleDrawer()"></i>
                        </v-list-item-action>
                    </v-list-item>
                </v-row>
                <v-divider
                   :inset="inset"
                   class="my-0 appHeaderBorder"/>

                <v-row class="mx-0 px-3">
                    <div class="appLauncherList">
                        <div class="appLauncherItem" v-for="(application, index) in favoriteApplicationsList">
                            <a target="_blank" :href="application.appUrl">
                                <img class="appLauncherImage" v-if="application.appImageFileBody != undefined && application.appImageFileBody != ''" :src="application.appImageFileBody"/>
                                <span class="appLauncherTitle">{{ application.appTitle }}</span>
                            </a>
                        </div>
                    </div>
                </v-row>
                <v-row class="seeAllApplications mx-0">
                    <v-card
                            flat
                            tile
                            class="d-flex flex justify-center mx-2">
                        <a
                                class="primary--text seeAllApplicationsBtn"
                                :href="appCenterUserSetupLink"
                                >{{ $t('appCenter.appLauncher.drawer.viewAll') }}</a>
                    </v-card>
                </v-row>
            </v-navigation-drawer>
         </v-layout>
        </v-container>
    </v-app>
</template>
<script>
    export default {
        data () {
            return {
                appLauncherDrawer : null,
                favoriteApplicationsList: [],
                appCenterUserSetupLink: ''
            }
        },
        created() {
            this.getFavoriteApplicationsList();
            window.require(['SHARED/appCenterBundle'], function (appCenterBundle) {
                appCenterBundle.init();
            });
            this.appCenterUserSetupLink = eXo.env.portal.context + "/" + eXo.env.portal.portalName + "/appCenterUserSetup";
        },
        methods : {
            toggleDrawer() {
                this.appLauncherDrawer = !this.appLauncherDrawer;
            },
            getFavoriteApplicationsList() {
                return fetch('/rest/appCenter/applications/getFavoriteApplicationsList', {
                    method: 'GET',
                }).then((resp) => {
                    if(resp && resp.ok) {
                        return resp.json();
                    } else {
                        throw new Error('Error getting favorite applications list');
                    }
                }).then(data => {
                    this.favoriteApplicationsList = data.applications;
                })
            }
        }
    }
</script>