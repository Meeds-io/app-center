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
              <my-applications-listing-step
                :key="listingStepKey"
                :listing-mode.sync="listingMode"
                :max-apps-to-list.sync="maxAppsToList"
                :selection-mode.sync="selectionMode"
                :applications.sync="applications"
                :categories.sync="categories"
                :setting-name="settingName"
                :loading="!applicationsLoaded || !categoriesLoaded"
                :hidden-count="hiddenApplicationIds.length"
                :hidden-category-count="hiddenCategoryIds.length"
                @add-category="addCategory" />
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
      listingMode: 'FAVORITES',
      selectionMode: 'MANUAL',
      applications: [],
      savedApplications: [],
      applicationsLoaded: false,
      applicationsLoad: 0,
      savedApplicationIds: [],
      hiddenApplicationIds: [],
      categories: [],
      savedCategories: [],
      categoriesLoaded: false,
      categoriesLoad: 0,
      savedCategoryIds: [],
      hiddenCategoryIds: [],
      listingStepKey: 0,
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
      // Never post a list that could not be read, and never an empty SELECTED listing
      if (!this.applicationsLoaded || !this.categoriesLoaded) {
        return false;
      }
      if (this.listingMode === 'SELECTED'
          && !(this.selectionMode === 'MANUAL' && this.postedApplicationIds.length)
          && !(this.selectionMode === 'CATEGORY' && this.postedCategoryIds.length)) {
        return false;
      }
      return this.settings.listingMode !== this.listingMode
          || this.settings.selectionMode !== this.selectionMode
          || this.savedApplicationIds.join(',') !== this.postedApplicationIds.join(',')
          || this.savedCategoryIds.join(',') !== this.postedCategoryIds.join(',')
          || this.settings.showHeader !== this.showHeader || this.savedMaxAppsToList !== this.maxAppsToList
          || JSON.stringify(this.currentTranslations) !== JSON.stringify(this.translations);
    },
    savedMaxAppsToList() {
      // A value stored before the 1–100 bounds is shown, and compared, clamped
      const value = Number(this.settings.maxAppsToList);
      return Number.isNaN(value) ? 4 : Math.min(Math.max(value, 1), 100);
    },
    applicationIds() {
      return this.applications.map(application => application.id);
    },
    postedApplicationIds() {
      // The stored apps this editor may not see keep their place: only the visible ones move or go
      const visibleIds = [...this.applicationIds];
      const ids = this.savedApplicationIds.map(id => (this.hiddenApplicationIds.includes(id) ? id : visibleIds.shift()))
        .filter(id => id);
      return [...ids, ...visibleIds];
    },
    postedCategoryIds() {
      // As for the apps: the stored categories this editor may not read keep their place
      const visibleIds = this.categories.map(category => category.id);
      const ids = this.savedCategoryIds.map(id => (this.hiddenCategoryIds.includes(id) ? id : visibleIds.shift()))
        .filter(id => id);
      return [...ids, ...visibleIds];
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
      this.loadApplications();
      this.loadCategories();
      // number-input reads its value only when created, and the drawer keeps its content once opened
      this.listingStepKey++;
      this.$refs.myApplicationsSettingsDrawer.open();
    },
    close() {
      this.$refs.myApplicationsSettingsDrawer.close();
    },
    async save() {
      const settings = {
        listingMode: this.listingMode,
        selectionMode: this.selectionMode,
        applicationIds: this.postedApplicationIds.join(','),
        categoryIds: this.postedCategoryIds.join(','),
        maxAppsToList: this.maxAppsToList,
        showHeader: this.showHeader
      };
      this.isSaving = true;
      try {
        await this.$myApplicationsService.saveSettings(this.saveSettingsUrl, settings, this.settingName, {
          applicationIds: this.hiddenApplicationIds,
          categoryIds: this.hiddenCategoryIds,
        });
        await this.saveHeaderTranslations();
        this.savedApplications = [...this.applications];
        this.savedApplicationIds = [...this.postedApplicationIds];
        this.savedCategories = [...this.categories];
        this.savedCategoryIds = [...this.postedCategoryIds];
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
    async loadApplications() {
      const load = ++this.applicationsLoad;
      this.applicationsLoaded = false;
      try {
        const applications = await this.$myApplicationsService.getContextApplications(this.settingName, this.settings.applicationIds);
        if (load === this.applicationsLoad) {
          const visibleIds = applications.map(application => application.id);
          this.savedApplications = applications;
          this.savedApplicationIds = [...(this.settings.applicationIds || [])];
          this.hiddenApplicationIds = this.savedApplicationIds.filter(id => !visibleIds.includes(id));
          this.applications = [...applications];
          this.applicationsLoaded = true;
        }
      } catch (e) {
        // Save stays disabled: a list that could not be read is never posted
        if (load === this.applicationsLoad) {
          this.$root.$emit('alert-message', this.$t('myApplications.listedApps.error.message'), 'error');
        }
      }
    },
    async loadCategories() {
      const load = ++this.categoriesLoad;
      this.categoriesLoaded = false;
      const ids = [...(this.settings.categoryIds || [])];
      // A category this editor may not read, or deleted since, is kept hidden in place
      const categories = await Promise.all(ids.map(id => this.$categoryService.getCategory(id).catch(() => null)));
      if (load === this.categoriesLoad) {
        this.savedCategories = categories.filter(Boolean);
        this.savedCategoryIds = ids;
        this.hiddenCategoryIds = ids.filter((id, index) => !categories[index]);
        this.categories = [...this.savedCategories];
        this.categoriesLoaded = true;
      }
    },
    addCategory(category) {
      if (this.categories.length + this.hiddenCategoryIds.length < 100
          && !this.categories.some(c => c.id === category.id)
          && !this.hiddenCategoryIds.includes(category.id)) {
        this.categories = [...this.categories, category];
      }
    },
    restoreSavedSettings() {
      this.stepper = 1;
      this.listingMode = this.settings.listingMode || 'FAVORITES';
      this.selectionMode = this.settings.selectionMode || 'MANUAL';
      this.applications = [...this.savedApplications];
      this.categories = [...this.savedCategories];
      this.maxAppsToList = this.savedMaxAppsToList;
      this.showHeader = this.settings.showHeader;
    }
  }
};
</script>
