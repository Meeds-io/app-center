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
      :items="filteredApplicationsList"
      :no-data-text="$t('appCenter.adminSetupForm.noApp')"
      item-key="id"
      hide-default-footer
      disable-pagination
      disable-sort
      dense>
      <template #item="props">
        <app-center-admin-item
          :item="props.item"
          :index="props.index"
          :length="filteredApplications.length"
          :moving-up="movingUpId === props.item.id"
          :moving-down="movingDownId === props.item.id"
          @set-enabled="setEnabled(props.item, $event)"
          @edit="editApplication(props.item)"
          @remove="applicationToDelete = props.item"
          @move-up="moveUp(props.item)"
          @move-down="moveDown(props.item)" />
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
    <confirm-dialog
      ref="deleteConfirmDialog"
      :title="$t('appCenter.adminSetupForm.modal.DeleteApp')"
      :message="$t('appCenter.adminSetupForm.modal.confirmDelete')"
      :ok-label="$t('appCenter.adminSetupForm.modal.delete')"
      :cancel-label="$t('appCenter.adminSetupForm.cancel')"
      @ok="deleteApplication"
      @closed="applicationToDelete = null" />
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
    movingUpId: null,
    movingDownId: null,
    applicationToDelete: null,
    showDeleteApplicationModal: false,
    pageSize: 20,
    limit: 20,
  }),
  computed: {
    applications() {
      return this.$root.applications || [];
    },
    filteredApplications() {
      return this.keyword
       && this.applications.filter(a => a.displayName.toLowerCase().includes(this.keyword.trim().toLowerCase()))
       || this.applications;
    },
    filteredApplicationsList() {
      return this.filteredApplications.slice(0, this.limit);
    },
    hasMore() {
      return this.filteredApplications.length > this.limit;
    },
    headers() {
      return this.$root.isMobile && [{
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
      }] || [{
        text: '',
        width: '30px',
        class: 'ps-0'
      }, {
        text: this.$t('appCenter.adminSetupList.name'),
        class: 'pa-0'
      }, {
        text: this.$t('appCenter.adminSetupList.move'),
        width: '88px',
        align: 'center',
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
    applicationToDelete() {
      if (this.applicationToDelete) {
        this.$refs.deleteConfirmDialog.open();
      }
    },
  },
  created() {
    this.$root.$on('app-center-refresh-list', this.getApplications);
    this.init();
  },
  beforeDestroy() {
    this.$root.$off('app-center-refresh-list', this.getApplications);
  },
  methods: {
    init() {
      return this.getApplications()
        .finally(() => this.$root.$applicationLoaded());
    },
    getApplications() {
      return this.$applicationService.getApplications(true)
        .then(data => {
          data.applications.forEach(app => {
            if (app.system) {
              const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (this.$te(`appCenter.system.application.${appTitle}`)) {
                app.displayName = this.$t(`appCenter.system.application.${appTitle}`);
                if (this.$te(`appCenter.system.application.${appTitle}.description`) && !app.description?.length) {
                  app.description = this.$t(`appCenter.system.application.${appTitle}.description`);
                }
              }
            }
            if (!app.displayName) {
              app.displayName = app.title;
            }
          });
          const applications = data?.applications?.filter?.(t => t.title) || [];
          this.sortApplicationsByOrder(applications);
          applications.forEach((app, index) => app.order = index);
          this.$root.applications = applications;
        }).finally(() => this.loading = false);
    },
    deleteApplication() {
      return this.$applicationService.deleteApplication(this.applicationToDelete.id)
        .then(() => {
          this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationDeletedSuccessfully'), 'success');
          this.$root.$emit('app-center-refresh-list');
          this.applicationToDelete = null;
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.errorDeletingApplication'), 'error'));
    },
    updateApplication(application) {
      return this.$applicationService.updateApplication(application)
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
          this.$root.$emit('app-center-refresh-enabled', application, enabled);
        });
    },
    loadMore() {
      this.limit += this.pageSize;
    },
    async moveDown(app) {
      const index = this.$root.applications.indexOf(app);
      if (index >= 0) {
        this.movingDownId = app.id;
        try {
          await this.updateItemOrder(this.$root.applications[index], index + 1);
          await this.updateItemOrder(this.$root.applications[index + 1], index);
          await this.getApplications();
        } finally {
          this.movingDownId = null;
        }
      }
    },
    async moveUp(app) {
      const index = this.$root.applications.indexOf(app);
      if (index >= 0) {
        this.movingUpId = app.id;
        try {
          await this.updateItemOrder(this.$root.applications[index], index - 1);
          await this.updateItemOrder(this.$root.applications[index - 1], index);
          await this.getApplications();
        } finally {
          this.movingUpId = null;
        }
      }
    },
    updateItemOrder(app, order) {
      return this.$applicationService.updateApplication({
        ...app,
        order,
      });
    },
    sortApplicationsByOrder(apps) {
      apps.sort((a, b) => {
        if (a.order === null && b.order === null) {
          return this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase());
        } else if (a.order === null) {
          return 1;
        } else if (b.order === null) {
          return -1;
        } else {
          return a.order - b.order;
        }
      });
    },
  }
};
</script>
