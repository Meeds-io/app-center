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
      <v-col cols="2">
        <a
          class="actionIcon addApplicationButton tooltipContent"
          data-placement="bottom"
          data-container="body"
          @click.stop="showAddApplicationDrawer"
        >
          <i class="uiIconPlus uiIconLightGray"></i>
          <span>{{ $t("appCenter.adminSetupForm.addNewApp") }}</span>
          <span class="tooltiptext">
            {{ $t("appCenter.adminSetupForm.addNewApp") }}
          </span>
        </a>
      </v-col>
      <v-spacer></v-spacer>
      <v-col class="appSearch" cols="3">
        <v-text-field
          v-model="search"
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
      :search="search"
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
          <td class="text-md-center">
            {{ props.item.url }}
          </td>
          <td class="text-md-center">
            {{ props.item.description }}
          </td>
          <td class="text-md-center">
            <h5
              v-for="permission in props.item.permissions"
              :key="permission"
            >
              <span v-if="permission==='any'">*</span>
              <span v-else> {{ permission }}</span>
            </h5>
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
              <v-switch v-model="props.item.mobile" @change="updateOption(props.item)"></v-switch>
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
  props: {
    pageSize: {
      type: Number,
      default: 10,
    },
  },
  data() {
    return {
      headers: [
        { text: `${this.$t('appCenter.adminSetupList.avatar')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupList.application')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.url')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.description')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.permissions')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.mandatory')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.active')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupForm.mobile')}`, align: 'center', filterable: false },
        { text: `${this.$t('appCenter.adminSetupList.actions')}`, align: 'center', filterable: false },
      ],
      defaultAppImage: {
        fileBody: '',
        fileName: '',
        invalidSize: false,
        invalidImage: false
      },
      search: '',
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
      showAddOrEditApplicationModal: false,
      showDeleteApplicationModal: false,
      totalApplications: 0,
      totalPages: 0,
      groups: [],
      openAppDrawer: false,
      addApplication: true,
      appPermissions: [],
    };
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
      return fetch(`/portal/rest/app-center/applications?offset=${offset}&limit=${this.pageSize}&keyword=${''}`, {
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
          this.totalApplications = this.applicationsList.size;
          this.totalPages = Number.parseInt((this.applicationsList.size + this.pageSize - 1) / this.pageSize);
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
      this.showAddOrEditApplicationModal = false;
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
  }
};
</script>
