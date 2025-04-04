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
      disable-sort
      dense>
      <template #item="props">
        <app-center-admin-item
          :item="props.item"
          @set-enabled="setEnabled(props.item, $event)"
          @edit="editApplication(props.item)"
          @remove="applicationToDelete = props.item" />
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
    <app-center-form-drawer ref="appFormDrawer" />
    <exo-confirm-dialog
      ref="deleteConfirmDialog"
      :title="$t('appCenter.adminSetupForm.modal.DeleteApp')"
      :message="$t('appCenter.adminSetupForm.modal.confirmDelete')"
      :ok-label="$t('appCenter.adminSetupForm.modal.delete')"
      :cancel-label="$t('appCenter.adminSetupForm.cancel')"
      @ok="deleteApplication"
      @closed="deleteApplication = null" />
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
    loading: true,
    applicationsList: [],
    applicationToDelete: null,
    showDeleteApplicationModal: false,
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
        width: '30px',
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
      this.getApplicationsList();
    },
    deleteApplication() {
      if (this.deleteApplication) {
        this.$refs.deleteConfirmDialog.open();
      } else {
        this.$refs.deleteConfirmDialog.close();
      }
    },
  },
  created() {
    this.$root.$on('app-center-refresh-list', this.getApplicationsList);
    this.init();
  },
  beforeDestroy() {
    this.$root.$off('app-center-refresh-list', this.getApplicationsList);
  },
  methods: {
    init() {
      return this.getApplicationsList()
        .finally(() => this.$root.$applicationLoaded());
    },
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
            throw new Error('Error when getting the favorite applications list');
          }
        })
        .then(data => {
          this.applicationsList = [];
          data.applications.forEach(app => {
            // manage system apps localized names
            if (app.system) {
              const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (this.$te(`appCenter.system.application.${appTitle}`)) {
                data.applications[this.getAppIndex(data.applications, app.id)].displayName = this.$t(`appCenter.system.application.${appTitle}`);
              }
            }
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.applicationsList = data.applications;
        }).finally(() => this.loading = false);
    },
    deleteApplication() {
      return fetch(`/app-center/rest/applications/${this.applicationToDelete.id}`,{
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
        .then(() => this.getApplicationsList())
        .catch(() => this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.errorDeletingApplication'), 'error'))
        .finally(() => this.applicationToDelete = null);
    },
    updateApplication(application) {
      return fetch('/app-center/rest/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: 'PUT',
        body: JSON.stringify(application)
      })
        .catch(e => {
          const UNAUTHORIZED_ERROR_CODE = 401;
          if (e.response.status === UNAUTHORIZED_ERROR_CODE) {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.unauthorized'), 'error');
          } else {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.error'), 'error');
          }
        });      
    },
    editApplication(item) {
      this.$refs.appFormDrawer.open(item);
    },
    setEnabled(application, enabled) {
      return this.updateApplication({
        ...application,
        active: enabled
      })
        .then(() => {
          if (enabled) {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationEnabledSuccessfully'), 'success');
          } else {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationDisabledSuccessfully'), 'success');
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
