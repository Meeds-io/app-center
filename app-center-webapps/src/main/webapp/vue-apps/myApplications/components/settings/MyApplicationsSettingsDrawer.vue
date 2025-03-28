<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
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
    id="myApplicationsSettingsDrawer"
    ref="myApplicationsSettingsDrawer"
    :right="!$vuetify.rtl"
    @closed="reset">
    <template #title>
      <div class="align-center text-truncate text-header-title font-weight-bold text-color">
        {{ $t('myApplications.edit.settings.title') }}
      </div>
    </template>
    <template #content>
      <v-form ref="form">
        <div class="pa-5">
          <div class="text-header">
            {{ $t('myApplications.displayOptions.label') }}
          </div>
          <div class="d-flex align-center">
            <label class="v-label mt-2 text-color">
              {{ $t('myApplications.numberToList.label') }}
            </label>
            <div class="ms-auto">
              <number-input
                v-model="maxAppsToList"
                :min="1"
                :max="100"
                :step="1"
                editable />
            </div>
          </div>
          <div class="d-flex mt-1 align-center justify-space-between">
            <label class="v-label text-color align-start">
              {{ $t('myApplications.addHeader.label') }}
            </label>
            <div class="align-end">
              <v-switch
                v-model="showHeader"
                :disabled="isSaving"
                color="primary"
                class="pa-0 my-auto switch-input" />
            </div>
          </div>
          <translation-text-field
            v-if="showHeader"
            :object-id="applicationId"
            :object-type="objectType"
            :field-name="fieldName"
            :field-value="displayedValue"
            :drawer-title="$t('myApplications.header.translation.title')"
            class="mt-2"
            no-expand-icon
            back-icon
            required
            @update:field-value="updateFieldValue"
            @input="translationUpdated" />
        </div>
      </v-form>
    </template>
    <template #footer>
      <div class="d-flex width-fit-content ms-auto">
        <v-btn
          class="btn me-5"
          @click="reset">
          {{ $t('myApplications.settings.cancel.label') }}
        </v-btn>
        <v-btn
          class="btn btn-primary"
          :disabled="!saveEnabled"
          :loading="isSaving"
          @click="save">
          {{ $t('myApplications.settings.save.label') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data() {
    return {
      isSaving: false,
      showHeader: true,
      maxAppsToList: 4,
      objectType: 'myApplicationsPortlet',
      fieldName: 'headerTitle',
      translations: [],
      userLocale: eXo.env.portal.language,
      defaultLangValue: this.$t('myApplications.name.label'),
      translationsInitialized: false,
      currentTranslations: []
    };
  },
  props: {
    settings: {
      type: Object,
      default: null
    }
  },
  computed: {
    saveEnabled() {
      return this.settings.showHeader !== this.showHeader || Number(this.settings.maxAppsToList) !== this.maxAppsToList
          || JSON.stringify(this.currentTranslations) !== JSON.stringify(this.translations);
    },
    applicationId() {
      return this.settings.applicationId;
    },
    saveSettingsUrl() {
      return this.settings?.saveSettingsUrl;
    },
    displayedValue() {
      return this.translations?.[this.userLocale] || this.defaultLangValue;
    },
  },
  methods: {
    translationUpdated(translations) {
      this.translations = translations;
      if (!this.translationsInitialized) {
        this.currentTranslations = structuredClone(this.translations);
        this.translationsInitialized = true;
      }
    },
    updateFieldValue(value) {
      this.defaultLangValue = value;
    },
    open() {
      this.restoreSavedSettings();
      this.$refs.myApplicationsSettingsDrawer.open();
    },
    close() {
      this.$refs.myApplicationsSettingsDrawer.close();
    },
    save() {
      const settings = {
        maxAppsToList: this.maxAppsToList,
        showHeader: this.showHeader
      };
      this.$myApplicationsService.saveSettings(this.saveSettingsUrl, settings).then(() => {
        this.saveHeaderTranslations();
        this.$emit('settings-updated', settings, this.displayedValue);
        this.$root.$emit('alert-message', this.$t('myApplications.settings.save.success.message'), 'success');
      }).catch(() => {
        this.$root.$emit('alert-message', this.$t('myApplications.settings.save.error.message'), 'error');
      });
    },
    async saveHeaderTranslations() {
      if (this.showHeader) {
        await this.$translationService.saveTranslations(this.objectType, this.applicationId, this.fieldName, this.translations);
        this.currentTranslations = structuredClone(this.translations);
      }
    },
    reset() {
      this.restoreSavedSettings();
      this.close();
    },
    restoreSavedSettings() {
      this.maxAppsToList = this.settings.maxAppsToList;
      this.showHeader = this.settings.showHeader;
    }
  }
};
</script>
