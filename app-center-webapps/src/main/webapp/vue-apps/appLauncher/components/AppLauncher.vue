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
          <div class="appLauncherList">
            <div
              v-for="(application, index) in favoriteApplicationsList"
              :key="index"
              :id="'Pos-' + index"
              class="appLauncherItemContainer"
              v-on:dragover="onDragOver_handler($event)"
              v-on:dragenter="onDragEnter_handler($event, index)"
              v-on:dragleave="onDragLeave_handler($event, index)"
              v-on:drop="onDrop_handler($event, index)">
              <div
                :id="'App-' + index"
                class="appLauncherItem"
                draggable="true"
                v-on:dragstart="onDragStart_handler($event)">
                <a 
                  :target="application.target"
                  :href="application.computedUrl"
                  :id="application.id"
                  draggable="false">
                  <img v-if="application.id" class="appLauncherImage" draggable="false" :src="`/portal/rest/app-center/applications/illustration/${application.id}`">
                  <span class="appLauncherTitle" draggable="false">{{ application.title }}</span>
                </a>
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
      draggedElementIndex: null,
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
      if (!this.appLauncherDrawer) {
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
        });
    },
    navigateTo(link) {
      location.href = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/${link}`;
    },
    onDragStart_handler(event) {
      this.draggedElementIndex = parseInt(event.target.id.substring(4, event.target.id.length));
      event.dataTransfer.setData('text/plain', event.target.id);
    },
    onDragOver_handler(event) {
      event.preventDefault();      
    },
    onDrop_handler(event, index) {
      const id = event.dataTransfer.getData('text');
      const target = event.target;
      if (id.startsWith('App') 
          && this.draggedElementIndex !== index 
          && target.getAttribute('class').startsWith('appLauncherItemContainer') 
          && !target.hasChildNodes()) {        
        // update shifted elements ids
        if (this.draggedElementIndex > index) {
          for (let i = this.draggedElementIndex; i > index; i--) {
            const element = this.getAppElementByIndex(i - 1);
            element.setAttribute('id', `App-${i}`);
          }
        }
        if (this.draggedElementIndex < index) {
          for (let i = this.draggedElementIndex; i < index; i++) {
            const element = this.getAppElementByIndex(i + 1);
            element.setAttribute('id', `App-${i}`);
          }
        }

        // displace moved element
        const draggableElement = document.getElementById(id);
        const dropZone = event.target;
        draggableElement.setAttribute('id', `App-${index}`);
        dropZone.appendChild(draggableElement);

        event.dataTransfer.clearData();
        this.draggedElementIndex = null;
      }
    },
    onDragEnter_handler(event, index) {
      event.preventDefault();
      if (event.target.tagName === 'IMG' || event.target.tagName === 'SPAN') {
        if (this.draggedElementIndex > index) {
          // shift elements to the right
          for (let i = this.draggedElementIndex; i > index; i--) {
            const element = this.getAppElementByIndex(i - 1);
            // element.setAttribute('id', `App-${i}`);
            const appContainer = this.getAppContainerByIndex(i)
            appContainer.appendChild(element);
          }
        }

        if (this.draggedElementIndex < index) {
          // shift elements to the left
          for (let i = this.draggedElementIndex; i < index; i++) {
            const element = this.getAppElementByIndex(i + 1);
            // element.setAttribute('id', `App-${i}`);
            const appContainer = this.getAppContainerByIndex(i)
            appContainer.appendChild(element);
          }
        }
      }
    },
    onDragLeave_handler(event, index) {
      event.preventDefault();
      if (event.target.getAttribute('id') && event.target.getAttribute('id').startsWith('Pos')) {

        if (this.draggedElementIndex > index) {
          // unshift elements to the right
          for (let i = this.draggedElementIndex; i < index; i++) {
            const element = this.getAppElementByIndex(i + 1);
            // element.setAttribute('id', `App-${i}`);
            const appContainer = this.getAppContainerByIndex(i)
            appContainer.appendChild(element);
          }
        }        
      }
    },
    getAppContainerByIndex(index) {
      return document.getElementById(`Pos-${index}`);
    },
    getAppElementByIndex(index) {
      return document.getElementById(`App-${index}`);
    },
  }
};
</script>
