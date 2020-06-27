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
  <div class="listApplications">
    <v-row>
      <v-col cols="3">
        <v-btn class="addApplicationBtn" depressed @click="showAddApplicationDrawer">
          <v-icon left>
            mdi-plus
          </v-icon>
          {{ $t("appCenter.adminSetupForm.addNewApp") }}
        </v-btn>
      </v-col>
      <v-spacer></v-spacer>
      <v-col class="appSearch pb-5" cols="3">
        <v-text-field
          v-model="searchText"
          :placeholder="`${$t('appCenter.adminSetupList.filter')} ...`"
          prepend-inner-icon="mdi-filter"
          hide-details
        ></v-text-field>
      </v-col>
    </v-row>
    <v-divider></v-divider>
    <v-data-table
      :headers="headers"
      :items="applicationsList"
      :footer-props="{
        itemsPerPageText: `${$t('appCenter.adminSetupForm.table.footer.text')}:`,        
      }"
      disable-sort
    >
      <template slot="item" slot-scope="props">
        <tr>
          <td class="text-md-start">
            <img v-if="props.item.imageFileId" :src="`/portal/rest/app-center/applications/illustration/${props.item.id}`" />
            <img v-else-if="defaultAppImage.fileBody" :src="`/portal/rest/app-center/applications/illustration/${props.item.id}`" />
            <img v-else src="/app-center/skin/images/defaultApp.png" />
          </td>
          <td class="text-md-center">
            {{ props.item.title }}
          </td>
          <td 
            v-exo-tooltip.bottom.body="props.item.url.length > 39 ? props.item.url : ''"
            class="text-md-center appUrl"
          >
            {{ props.item.url }}
          </td>
          <td
            v-exo-tooltip.bottom.body="props.item.description.length > 79 ? props.item.description : ''"
            class="text-md-center"
          >
            <div class="tableAppDescription">
              {{ props.item.description }}              
            </div>
          </td>
          <td class="text-md-center">
            <div
              v-exo-tooltip.bottom.body="props.item.permissions.length > 3 ? props.item.permissions : ''" 
              class="tableAppPermissions"
            >
              <div
                v-for="permission in props.item.permissions"
                :key="permission"
                v-exo-tooltip.bottom.body="permission.length > 22 && props.item.permissions.length <= 3 ? permission : ''"
                class="permission"
              >
                <span v-if="permission==='any'">*</span>
                <span v-else> {{ permission }}</span>
              </div>
            </div>
          </td>
          <td class="text-md-center">
            <v-row justify="center">
              <v-switch v-model="props.item.mandatory" @change="updateOption(props.item)"></v-switch>              
            </v-row>
          </td>
          <td class="text-md-center">
            <v-row justify="center">
              <v-switch v-model="props.item.active" @change="updateOption(props.item)"></v-switch>
            </v-row>
          </td>
          <td class="text-md-center">
            <v-row justify="center">
              <v-switch 
                v-model="props.item.mobile"
                v-exo-tooltip.bottom.body="$t('appCenter.adminSetupForm.table.switch.mobile.tooltip')"
                @change="updateOption(props.item)"
              ></v-switch>
            </v-row>
          </td>
          <td class="text-md-center">
            <v-row justify="center">
              <v-btn
                icon
                class="actionsBtn"
                @click="showEditApplicationDrawer(props.item)"
              >
                <v-icon
                  medium
                >
                  mdi-pencil
                </v-icon>
              </v-btn>
              <v-btn
                icon
                class="actionsBtn"
                @click="toDeleteApplicationModal(props.item)"
              >
                <v-icon
                  medium
                >
                  mdi-delete
                </v-icon>
              </v-btn>
            </v-row>
          </td>
        </tr>
      </template>
    </v-data-table>

    <div v-if="!applicationsList.length" class="noApp">
      {{ $t("appCenter.adminSetupForm.noApp") }}
    </div>
    
    <exo-app-center-drawer
      :applications-drawer="openAppDrawer"
      :form-array="formArray"
      :app-permissions="appPermissions"
      @initApps="getApplicationsList"
      @resetForm="closeDrawer"
      @closeDrawer="closeDrawer"
    >
      <span v-if="addApplication" class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.createNewApp") }}</span>
      <span v-else class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.editApp") }}</span>
    </exo-app-center-drawer>

    <transition name="fade">
      <exo-app-center-modal
        v-show="showDeleteApplicationModal"
        :title="$t('appCenter.adminSetupForm.DeleteApp')"
        @modal-closed="closeDeleteModal()"
      >
        <div class="deleteApplication">
          <h3>
            {{ $t("appCenter.adminSetupForm.confirmDelete", {0: formArray.title}) }}
          </h3>
          <div class="form-group application-buttons pt-2">
            <button class="ignore-vuetify-classes btn btn-primary form-submit" @click.stop="deleteApplication()">
              <i class="uiTrashIcon"></i>
              {{ $t("appCenter.adminSetupForm.delete") }}
            </button>
            <button
              class="ignore-vuetify-classes btn form-reset"
              @click.stop="showDeleteApplicationModal = false"
            >
              <i class="uiCloseIcon"></i>
              {{ $t("appCenter.adminSetupForm.cancel") }}
            </button>
          </div>
          <div v-if="error != ''" class="error">
            <span>{{ error }}</span>
          </div>
        </div>
      </exo-app-center-modal>
    </transition>
  </div>
</template>

<script>

export default {
  name: 'AdminSetup',
  data() {
    return {
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
      headers: [
        { text: `${this.$t('appCenter.adminSetupList.avatar')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupList.application')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.url')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.description')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.permissions')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.mandatory')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.active')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupForm.mobile')}`, align: 'center' },
        { text: `${this.$t('appCenter.adminSetupList.actions')}`, align: 'center' },
      ],
      defaultAppImage: {
        fileBody: '',
        fileName: '',
        invalidSize: false,
        invalidImage: false
      },
      searchText: '',
      searchApp: '',
      searchDelay: 300,
      applicationsList: [],
      formArray: {
        id: 0,
        title: '',
        url: '',
        helpPageURL: '',
        description: '',
        active: true,
        mandatory: false,
        mobile: true,
        system: false,
        permissions: [],
        imageFileBody: '',
        imageFileName: '',
        imageFileId: '',
        viewMode: true,
        invalidSize: false,
        invalidImage: false
      },
      error: '',
      showDeleteApplicationModal: false,
      openAppDrawer: false,
      addApplication: true,
      appPermissions: [],
    };
  },
  watch: {
    searchText() {
      if (this.searchText && this.searchText.trim().length) {
        clearTimeout(this.searchApp);
        this.searchApp = setTimeout(() => {
          this.getApplicationsList();
        }, this.searchDelay);
      } else if (!this.searchText || this.searchText.length !== this.searchText.split(' ').length - 1) {
        this.getApplicationsList();
      }
    }
  },

  created() {
    this.getApplicationsList();
    this.getAppGeneralSettings();
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape' && this && this.closeModals) {
        this.closeModals();
      }
    });
  },

  methods: {
    getApplicationsList() {
      const offset = 0;
      const limit = 0;
      return fetch(`/portal/rest/app-center/applications?offset=${offset}&limit=${limit}&keyword=${this.searchText}`, {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error(
              'Error when getting the favorite applications list'
            );
          }
        })
        .then(data => {
          this.applicationsList = [];
          // manage system apps localized names
          data.applications.forEach(app => {
            if (this.systemAppNames.includes(app.title)) {
              data.applications[this.getAppIndex(data.applications, app.id)].title = this.$t(`appCenter.system.application.${app.title.toLowerCase()}`);
            } else if (app.title === 'Perk store') {
              data.applications[this.getAppIndex(data.applications, app.id)].title = this.$t('appCenter.system.application.perkStore');
            }
          });
          data.applications.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });

          // A trick to force retrieving img URL again to update illustration
          const REFRESH_TIMEOUT=20;
          window.setTimeout(() => this.applicationsList = data.applications, REFRESH_TIMEOUT);
        });
    },

    deleteApplication() {
      return fetch(`/portal/rest/app-center/applications/${this.formArray.id}`,{
        method: 'DELETE',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json;
          } else {
            throw new Error('Error when deleting application by id');
          }
        })
        .then(() => {
          this.closeDeleteModal();
          this.getApplicationsList();
        });
    },

    resetForm() {
      this.error = '';
      this.formArray.id = '';
      this.formArray.title = '';
      this.formArray.url = '';
      this.formArray.helpPageURL = '';
      this.formArray.imageFileName = '';
      this.formArray.imageFileBody = '';
      this.formArray.description = '';
      this.formArray.mandatory = false;
      this.formArray.system = false;
      this.formArray.active = true;
      this.formArray.mobile = true;
      this.formArray.permissions = [];
      this.formArray.invalidSize = false;
      this.formArray.invalidImage = false;
    },

    showAddApplicationDrawer() {
      this.openAppDrawer = true;
      this.addApplication = true;
      this.formArray.viewMode = true;
    },

    showEditApplicationDrawer(item) {
      this.openAppDrawer = true;
      this.addApplication = false;
      Object.assign(this.formArray, item);
      this.appPermissions = [];
      const allOffset = 2;
      for (const permission of this.formArray.permissions) {
        const groupId = permission.startsWith('*:') ? permission.substr(allOffset, permission.length - allOffset) : permission;
        this.appPermissions.push({
          id: groupId,
          name: groupId,
        });
      }
    },

    toDeleteApplicationModal(item) {
      this.showDeleteApplicationModal = true;
      this.formArray.id = item.id;
      this.formArray.title = item.title;
    },

    closeModals() {
      this.closeDeleteModal();
      this.resetForm();
    },

    closeDeleteModal() {
      this.formArray.id = '';
      this.formArray.title = '';
      this.showDeleteApplicationModal = false;
    },
    validUrl(app) {
      const url = app && app.url;
      return app.system || url && (url.indexOf('/portal/') === 0 || url.indexOf('./') === 0 || url.match(/(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}/g));
    },
    closeDrawer() {
      this.resetForm();
      this.openAppDrawer = false;
    },
    getAppGeneralSettings() {
      return fetch('/portal/rest/app-center/settings', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error getting favorite applications list');
          }
        })
        .then(data => {
          Object.assign(this.defaultAppImage, data && data.defaultApplicationImage);
        });
    },
    updateOption(application) {
      return fetch('/portal/rest/app-center/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: 'PUT',
        body: JSON.stringify({
          id: application.id,
          title: application.title,
          url: application.url,
          helpPageURL: application.helpPageURL,
          description: application.description,
          active: application.active,
          mandatory: application.mandatory,
          isMobile: application.mobile,
          system: application.system,
          permissions: application.permissions,
          imageFileBody: application.imageFileBody,
          imageFileName: application.imageFileName,
          imageFileId: application.imageFileId,
        })
      })
        .catch(e => {
          const UNAUTHORIZED_ERROR_CODE = 401;
          if (e.response.status === UNAUTHORIZED_ERROR_CODE) {
            this.error = this.$t('appCenter.adminSetupForm.unauthorized');
          } else {
            this.error = this.$t('appCenter.adminSetupForm.error');
          }
        });      
    },
    getAppIndex(appList, appId) {
      return appList.findIndex(app => app.id === appId);
    }
  }
};
</script>
