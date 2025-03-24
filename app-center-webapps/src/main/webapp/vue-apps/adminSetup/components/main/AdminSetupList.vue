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
  <div class="listApplications px-5">
    <v-data-table
      :headers="headers"
      :items="sortedApplicationsList"
      :no-data-text="$t('appCenter.adminSetupForm.noApp')"
      disable-pagination
      hide-default-footer
      disable-sort>
      <template #item="props">
        <app-center-admin-item
          :item="props.item"
          @set-enabled="setEnabled(props.item, $event)"
          @edit="showEditApplicationDrawer(props.item)"
          @remove="toDeleteApplicationModal(props.item)" />
      </template>
      <template v-if="hasMore" #footer>
        <v-btn
          class="mx-auto mt-5 border-box-sizing btn"
          block
          @click="loadMore">
          {{ $t('appCenter.adminSetupForm.loadMore') }}
        </v-btn>
      </template>
    </v-data-table>
    <app-center-form-drawer
      ref="appFormDrawer"
      :form-array="formArray"
      :app-permissions="appPermissions"
      :existing-app-names="existingAppNames"
      :app-to-edit-original-title="appToEditOriginalTitle"
      @initApps="getApplicationsList"
      @resetForm="closeDrawer"
      @closeDrawer="closeDrawer">
      <span v-if="addApplication" class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.createNewApp") }}</span>
      <span v-else class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.editApp") }}</span>
    </app-center-form-drawer>
    <app-center-modal
      :open="showDeleteApplicationModal"
      :title="$t('appCenter.adminSetupForm.modal.DeleteApp')"
      :message="$t('appCenter.adminSetupForm.modal.confirmDelete')"
      :ok-label="$t('appCenter.adminSetupForm.modal.delete')"
      :cancel-label="$t('appCenter.adminSetupForm.cancel')"
      @ok="deleteApplication"
      @closed="closeDeleteModal" />
  </div>
</template>
<script>
export default {
  props: {
    keyword: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    initialized: false,
    loading: true,
    defaultAppImage: {
      fileBody: '',
      fileName: '',
      invalidSize: false,
      invalidImage: false,
      invalidImageFormat: false,
    },
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
      invalidImage: false,
      invalidImageFormat: false,
    },
    error: '',
    showDeleteApplicationModal: false,
    displayAppDelay: 200,
    addApplication: true,
    appPermissions: [],
    existingAppNames: [],
    appToEditOriginalTitle: '',
    pageSize: 10,
    limit: 10,
  }),
  computed: {
    sortedApplicationsList() {
      const applicationsList = this.applicationsList?.filter?.(t => t.title) || [];
      applicationsList.sort((a, b) => this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase()));
      return applicationsList.slice(0, this.limit);
    },
    hasMore() {
      return this.applicationsList.length > this.limit;
    },
    headers() {
      return [{
        text: '',
        width: '50px',
        class: 'ps-0'
      }, {
        text: this.$t('appCenter.adminSetupList.name'),
        class: 'pa-0'
      }, {
        text: this.$t('appCenter.adminSetupForm.active'),
        width: '70px',
        align: 'center',
        class: 'px-0'
      }, {
        text: this.$t('appCenter.adminSetupList.actions'),
        width: '70px',
        align: 'center',
        class: 'px-0'
      }];
    },
  },
  watch: {
    keyword() {
      if (this.keyword && this.keyword.trim().length) {
        clearTimeout(this.searchApp);
        this.searchApp = setTimeout(() => {
          this.getApplicationsList();
        }, this.searchDelay);
      } else if (!this.keyword || this.keyword.length !== this.keyword.split(' ').length - 1) {
        this.getApplicationsList();
      }
    }
  },

  created() {
    Promise.all([
      this.getApplicationsList(),
      this.getAppGeneralSettings()
    ]).finally(() => this.$root.$applicationLoaded());
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape' && this && this.closeDeleteModal) {
        this.closeDeleteModal();
      }
    });
  },

  methods: {
    getApplicationsList() {
      const offset = 0;
      const limit = 0;
      return fetch(`/app-center/rest/applications/all?offset=${offset}&limit=${limit}&keyword=${this.keyword || ''}`, {
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
          data.applications.forEach(app => {
            this.existingAppNames.push(app.title);
            // manage system apps localized names
            if (app.system) {
              const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (!this.$t(`appCenter.system.application.${appTitle}`).startsWith('appCenter.system.application')) {
                data.applications[this.getAppIndex(data.applications, app.id)].displayName = this.$t(`appCenter.system.application.${appTitle}`);
              }
            }

            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });

          this.applicationsList = data.applications;
          return this.$nextTick();
        }).finally(() => {
          this.loading = false;
          if (!this.initialized) {
            this.initialized = true;
            /* Differ replacing cached content
             To let Vuetify the time to process
             chevron icons position switch RTL or LTR */
            window.setTimeout(() => {
              this.$root.$emit('application-loaded');
            }, this.displayAppDelay);
          }
        });
    },

    deleteApplication() {
      return fetch(`/app-center/rest/applications/${this.formArray.id}`,{
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
      this.formArray.invalidImageFormat = false;
      this.appToEditOriginalTitle = '';
    },

    showAddApplicationDrawer() {
      this.resetForm();
      this.$refs.appFormDrawer.open();
      $('body').addClass('hide-scroll');
      this.addApplication = true;
      this.formArray.viewMode = true;
    },

    showEditApplicationDrawer(item) {
      this.resetForm();
      this.appToEditOriginalTitle = item.title;
      this.$refs.appFormDrawer.open();
      $('body').addClass('hide-scroll');
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

    closeDeleteModal() {
      this.showDeleteApplicationModal = false;
      this.resetForm();
    },

    validUrl(app) {
      const url = app && app.url;
      return app.system || url && (url.indexOf('/portal/') === 0 || url.indexOf('./') === 0 || url.match(/(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}/g));
    },

    closeDrawer() {
      this.$refs.appFormDrawer.close();
    },

    getAppGeneralSettings() {
      return fetch('/app-center/rest/settings', {
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
    setEnabled(application, enabled) {
      return this.updateOption({
        ...application,
        active: enabled
      });
    },
    updateOption(application) {
      return fetch('/app-center/rest/applications', {
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
          mobile: application.mobile,
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
    },
    loadMore() {
      this.limit += this.pageSize;
    },
  }
};
</script>
