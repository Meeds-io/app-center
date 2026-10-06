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
    :loading="isSaving"
    @closed="reset">
    <template #title>
      {{ $t('myApplications.edit.shortcutsList.title') }}
    </template>
    <template #content>
      <v-stepper
        v-model="stepper"
        class="ma-0 pa-4 d-flex flex-column"
        vertical
        flat>
        <div class="flex-shrink-0">
          <v-stepper-step
            :step="1"
            width="100%"
            class="ma-0 pa-0"
            editable>
            <div class="text-header text-truncate">
              {{ $t('myApplications.appsListing.label') }}
            </div>
          </v-stepper-step>
          <v-slide-y-transition>
            <div v-show="stepper === 1" class="mt-4">
              <my-applications-listing-step v-model="maxAppsToList" />
            </div>
          </v-slide-y-transition>
        </div>
        <div class="flex-shrink-0 mt-4">
          <v-stepper-step
            :step="2"
            class="ma-0 pa-0"
            editable>
            <div class="text-header text-truncate">
              {{ $t('myApplications.displayOptions.label') }}
            </div>
          </v-stepper-step>
          <v-slide-y-transition>
            <div v-show="stepper === 2" class="mt-4">
              <my-applications-display-step
                :show-header.sync="showHeader"
                :setting-name="settingName"
                :object-type="objectType"
                :field-name="fieldName"
                :header-title="displayedValue"
                :disabled="isSaving"
                @update:header-title="updateFieldValue"
                @translations-updated="translationUpdated" />
            </div>
          </v-slide-y-transition>
        </div>
      </v-stepper>
    </template>
    <template #footer>
      <div class="d-flex align-center">
        <v-btn
          class="btn ms-auto me-2"
          @click="reset">
          {{ $t('myApplications.settings.cancel.label') }}
        </v-btn>
        <v-btn
          :disabled="!saveEnabled"
          :loading="isSaving"
          class="btn btn-primary"
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
      stepper: 1,
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
    settingName() {
      return this.settings.settingName;
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
    async save() {
      const settings = {
        maxAppsToList: this.maxAppsToList,
        showHeader: this.showHeader
      };
      this.isSaving = true;
      try {
        await this.$myApplicationsService.saveSettings(this.saveSettingsUrl, settings, this.settingName);
        await this.saveHeaderTranslations();
        this.$emit('settings-updated', settings, this.displayedValue);
        this.$root.$emit('alert-message', this.$t('myApplications.settings.save.success.message'), 'success');
      } catch (e) {
        this.$root.$emit('alert-message', this.$t('myApplications.settings.save.error.message'), 'error');
      } finally {
        this.isSaving = false;
      }
    },
    async saveHeaderTranslations() {
      if (this.showHeader) {
        await this.$translationService.saveTranslations(this.objectType, this.settingName, this.fieldName, this.translations);
        this.currentTranslations = structuredClone(this.translations);
      }
    },
    reset() {
      this.restoreSavedSettings();
      this.close();
    },
    restoreSavedSettings() {
      this.stepper = 1;
      this.maxAppsToList = Number(this.settings.maxAppsToList);
      this.showHeader = this.settings.showHeader;
    }
  }
};
</script>
