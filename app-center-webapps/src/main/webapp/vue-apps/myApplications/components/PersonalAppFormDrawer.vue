<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <exo-drawer
    id="personalAppFormDrawer"
    ref="personalAppFormDrawer"
    :right="!$vuetify.rtl"
    @closed="reset">
    <template #title>
      <div class="text-truncate text-header-title font-weight-bold text-color">
        {{ editMode ? $t('appCenter.personalApp.drawer.edit.title') : $t('appCenter.personalApp.drawer.title') }}
      </div>
    </template>
    <template #content>
      <v-form ref="form" class="pa-5">
        <div class="mb-4">
          <label class="v-label text-color font-weight-bold">
            {{ $t('appCenter.personalApp.form.title.label') }}
            <span class="error--text">*</span>
          </label>
          <v-text-field
            v-model="title"
            :placeholder="$t('appCenter.personalApp.form.title.placeholder')"
            :rules="[titleRequired, titleMaxLength]"
            :maxlength="200"
            outlined
            dense
            class="mt-1" />
        </div>
        <div class="mb-4">
          <label class="v-label text-color font-weight-bold">
            {{ $t('appCenter.personalApp.form.url.label') }}
            <span class="error--text">*</span>
          </label>
          <v-text-field
            v-model="url"
            :placeholder="$t('appCenter.personalApp.form.url.placeholder')"
            :rules="[urlRequired, urlValid]"
            outlined
            dense
            class="mt-1" />
        </div>
        <div class="d-flex align-center justify-space-between mt-2">
          <label class="v-label text-color">
            {{ $t('appCenter.personalApp.form.sameTab.label') }}
          </label>
          <v-switch
            v-model="sameTab"
            color="primary"
            class="pa-0 my-auto"
            hide-details />
        </div>
      </v-form>
    </template>
    <template #footer>
      <div class="d-flex width-fit-content ms-auto">
        <v-btn
          class="btn me-5"
          @click="close">
          {{ $t('appCenter.adminSetupForm.cancel') }}
        </v-btn>
        <v-btn
          :disabled="!canSave"
          :loading="saving"
          class="btn btn-primary"
          @click="save">
          {{ $t('appCenter.adminSetupForm.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data() {
    return {
      title: '',
      url: '',
      sameTab: false,
      saving: false,
      editedApp: null,
    };
  },
  computed: {
    editMode() {
      return !!this.editedApp;
    },
    canSave() {
      return this.title?.trim().length > 0 && this.url?.trim().length > 0;
    },
    titleRequired() {
      return v => !!v?.trim() || this.$t('appCenter.adminSetupForm.emptyTitle');
    },
    titleMaxLength() {
      return v => !v || v.length <= 200 || this.$t('appCenter.form.name.exceedsMaxLength', [200]);
    },
    urlRequired() {
      return v => !!v?.trim() || this.$t('appCenter.adminSetupForm.emptyUrl');
    },
    urlValid() {
      return v => {
        if (!v?.trim()) {
          return true;
        }
        try {
          new URL(v);
          return true;
        } catch {
          return this.$t('appCenter.form.url.invalidLink');
        }
      };
    },
  },
  methods: {
    open(app) {
      this.editedApp = app || null;
      if (app) {
        this.title = app.title || '';
        this.url = app.url || '';
        this.sameTab = app.sameTab || false;
      }
      this.$refs.personalAppFormDrawer.open();
    },
    close() {
      this.$refs.personalAppFormDrawer.close();
    },
    reset() {
      this.title = '';
      this.url = '';
      this.sameTab = false;
      this.saving = false;
      this.editedApp = null;
      this.$refs.form?.reset();
    },
    save() {
      if (!this.$refs.form.validate()) {
        return;
      }
      this.saving = true;
      const application = {
        id: this.editedApp?.id,
        title: this.title.trim(),
        url: this.url.trim(),
        sameTab: this.sameTab,
        type: 'LINK',
        active: true,
      };
      const promise = this.editMode
        ? this.$applicationService.updatePersonalApp(application)
        : this.$applicationService.createPersonalApp(application);
      promise
        .then(() => {
          this.$root.$emit('alert-message',
            this.$t(this.editMode ? 'appCenter.personalApp.update.success' : 'appCenter.personalApp.save.success'),
            'success');
          this.$emit('saved');
          this.close();
        })
        .catch(() => {
          this.$root.$emit('alert-message', this.$t('appCenter.personalApp.save.error'), 'error');
        })
        .finally(() => {
          this.saving = false;
        });
    },
  },
};
</script>
