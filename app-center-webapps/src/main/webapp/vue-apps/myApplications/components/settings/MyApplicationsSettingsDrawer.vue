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
                :loading="!applicationsLoaded || !categoriesLoaded || isSaving"
                :hidden-count="hiddenApplicationIds.length"
                :hidden-category-count="hiddenCategoryIds.length"
                :max-applications="maxApplications"
                @add-category="addCategory"
                @create-application="$emit('create-application')"
                @edit-application="$emit('edit-application', $event)" />
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
                :display.sync="display"
                :version="displayVersion"
                :preview-applications="previewApplications"
                :setting-name="settingName"
                :object-type="objectType"
                :field-name="fieldName"
                :header-title="displayedValue"
                :disabled="isSaving"
                @update:header-title="updateFieldValue"
                @translations-updated="translationUpdated"
                @select-template="selectTemplate" />
            </div>
          </v-slide-y-transition>
        </div>
      </v-stepper>
    </template>
    <template #footer>
      <div class="d-flex align-center">
        <v-btn
          :disabled="isSaving"
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
import {getDisplaySettings, getTemplateDisplaySettings, getTemplateShowHeader, isSameDisplay} from '../../myApplicationsDisplay.js';

export default {
  data() {
    return {
      stepper: 1,
      isSaving: false,
      showHeader: true,
      // The template and its settings, null while the legacy layout is kept
      display: null,
      // Renewed whenever the display settings are replaced rather than edited
      displayVersion: 0,
      maxAppsToList: 4,
      listingMode: 'FAVORITES',
      selectionMode: 'MANUAL',
      applications: [],
      savedApplications: [],
      applicationsLoaded: false,
      applicationsLoad: 0,
      savedApplicationIds: [],
      hiddenApplicationIds: [],
      // Created from these settings since their last save: listed nowhere until saved
      createdApplications: [],
      // The creations the form has not reported yet, by number: the settings action count when each started.
      // A refused creation is never reported, and its start never matched
      creationStarts: [],
      // Created while the list was at its cap, until the form reports the creation
      unlistedApplicationIds: [],
      // Counts the starts and ends of a Save, and the Cancels: a report made since one of them stays silent
      settingsActions: 0,
      // Counts the Cancels, the close after a Save included: an app whose creation started before one is not this session's
      settingsResets: 0,
      // The server's MAX_LISTED_APPLICATIONS, hidden apps included
      maxApplications: 100,
      closedWhileSaving: false,
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
    },
    // The apps the portlet displays, previewed in the display step
    displayedApplications: {
      type: Array,
      default: () => []
    },
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
          || this.settings.showHeader !== this.showHeader
          // Against the stored value, not its clamped display: a value stored out of the bounds can be saved back within them
          || Number(this.settings.maxAppsToList) !== this.maxAppsToList
          || !isSameDisplay(this.savedDisplay, this.display)
          || JSON.stringify(this.currentTranslations) !== JSON.stringify(this.translations);
    },
    savedDisplay() {
      return getDisplaySettings(this.settings);
    },
    previewApplications() {
      // The apps picked in the first step are those the portlet will list
      const applications = this.listingMode === 'SELECTED' && this.selectionMode === 'MANUAL' ? this.applications : this.displayedApplications;
      return applications.slice(0, 5);
    },
    savedMaxAppsToList() {
      // A value stored before the 1–100 bounds is shown clamped
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
    // Each template comes with its own configuration, which every pick applies
    selectTemplate(template) {
      this.display = getTemplateDisplaySettings(template);
      this.showHeader = getTemplateShowHeader(template);
      this.displayVersion++;
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
      // Read before the save's waits: what is posted is what becomes saved
      const applications = [...this.applications];
      const categories = [...this.categories];
      const postedApplicationIds = this.postedApplicationIds;
      const postedCategoryIds = this.postedCategoryIds;
      const createdApplications = this.createdApplications;
      // The apps this portlet created that the last save listed and this one removes: deleted once it is stored
      const removedApplications = this.savedApplications.filter(application => application.contextual
                                                                               && !postedApplicationIds.includes(application.id));
      const translations = this.translations;
      const headerTitle = this.displayedValue;
      const settings = {
        listingMode: this.listingMode,
        selectionMode: this.selectionMode,
        applicationIds: postedApplicationIds.join(','),
        categoryIds: postedCategoryIds.join(','),
        maxAppsToList: this.maxAppsToList,
        showHeader: this.showHeader,
        // No template picked: the legacy layout is kept
        ...this.display,
      };
      this.isSaving = true;
      this.settingsActions++;
      try {
        await this.$myApplicationsService.saveSettings(this.saveSettingsUrl, settings, this.settingName, {
          applicationIds: this.hiddenApplicationIds,
          categoryIds: this.hiddenCategoryIds,
        });
      } catch (e) {
        this.isSaving = false;
        this.settingsActions++;
        this.$root.$emit('alert-message', this.$t('myApplications.settings.save.error.message'), 'error');
        if (this.closedWhileSaving) {
          // Nothing saved and the drawer closed meanwhile: a Cancel
          this.closedWhileSaving = false;
          this.deleteCreatedApplications();
        }
        return;
      }
      try {
        // Stored, whatever follows: the created apps now listed stay, those removed before saving are deleted
        this.createdApplications = this.createdApplications.filter(application => !createdApplications.includes(application));
        this.savedApplications = applications;
        this.savedApplicationIds = [...postedApplicationIds];
        this.savedCategories = categories;
        this.savedCategoryIds = [...postedCategoryIds];
        const headerSaved = await this.saveHeaderTranslations(settings.showHeader, translations).then(() => true, () => false);
        const notDeleted = await this.deleteApplications([
          ...createdApplications.filter(application => !postedApplicationIds.includes(application.id)),
          ...removedApplications,
        ]);
        // Closed while saving: the apps created meanwhile, which the save did not post, go as on a Cancel.
        // Read after the last wait but this one, and reported with the others
        if (this.closedWhileSaving) {
          const createdMeanwhile = this.createdApplications;
          this.createdApplications = [];
          notDeleted.push(...await this.deleteApplications(createdMeanwhile));
        }
        this.$emit('settings-updated', settings, headerSaved ? headerTitle : this.settings.headerTitle);
        // One message, as a message replaces the previous one
        if (notDeleted.length) {
          this.reportNotDeleted(notDeleted);
        } else if (!headerSaved) {
          this.$root.$emit('alert-message', this.$t('myApplications.settings.save.error.message'), 'error');
        } else {
          this.$root.$emit('alert-message', this.$t('myApplications.settings.save.success.message'), 'success');
        }
      } finally {
        this.isSaving = false;
        this.closedWhileSaving = false;
        this.settingsActions++;
      }
    },
    async saveHeaderTranslations(showHeader, translations) {
      if (showHeader) {
        await this.$translationService.saveTranslations(this.objectType, this.settingName, this.fieldName, translations);
        this.currentTranslations = structuredClone(translations);
      }
    },
    reset() {
      this.settingsActions++;
      this.settingsResets++;
      if (this.isSaving) {
        // The save in progress decides what becomes of the created apps
        this.closedWhileSaving = true;
      } else {
        this.deleteCreatedApplications();
      }
      this.restoreSavedSettings();
      this.close();
    },
    creationStarted(creationId) {
      this.creationStarts = [...this.creationStarts, {creationId, settingsActions: this.settingsActions, settingsResets: this.settingsResets}];
    },
    applicationCreated(application, creationId) {
      const start = this.creationStarts.find(started => started.creationId === creationId);
      if (start && start.settingsResets !== this.settingsResets) {
        // Cancelled, or closed, while the request ran: deleted as the session's other created apps were
        this.deleteApplications([{id: application.id, title: application.title}]).then(this.reportNotDeleted);
        return;
      }
      this.createdApplications = [...this.createdApplications, {id: application.id, title: application.title}];
      // The list can reach the cap while the creation runs: the form closed, an app picked meanwhile
      if (this.applications.length + this.hiddenApplicationIds.length < this.maxApplications) {
        this.applications = [...this.applications, application];
      } else {
        this.unlistedApplicationIds = [...this.unlistedApplicationIds, application.id];
      }
    },
    creationCompleted({application, translationsSaved, creationId}) {
      const start = this.creationStarts.find(started => started.creationId === creationId);
      this.creationStarts = this.creationStarts.filter(started => started !== start);
      const listed = !this.unlistedApplicationIds.includes(application.id);
      this.unlistedApplicationIds = this.unlistedApplicationIds.filter(id => id !== application.id);
      // A Save or a Cancel ran since the creation started, the request included: its own message stands
      if (start?.settingsActions !== this.settingsActions) {
        return;
      }
      // One message: the labels' failure first, as an app left out can still be listed
      if (!translationsSaved) {
        this.$root.$emit('alert-message', this.$t('appCenter.contextual.translationsNotSaved'), 'warning');
      } else if (!listed) {
        this.$root.$emit('alert-message', this.$t('myApplications.createdApps.capReached', {0: application.title, 1: this.maxApplications}), 'warning');
      } else {
        this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationCreatedSuccessfully'), 'success');
      }
    },
    // Updated by the form at once, as any form save: listed with its new values
    applicationUpdated(application) {
      const update = applications => applications.map(listed => (listed.id === application.id ? {...listed, ...application} : listed));
      this.applications = update(this.applications);
      this.savedApplications = update(this.savedApplications);
    },
    deleteCreatedApplications() {
      // Cleared first: Cancel runs this again when the drawer closes
      const createdApplications = this.createdApplications;
      this.createdApplications = [];
      this.deleteApplications(createdApplications).then(this.reportNotDeleted);
    },
    // Best effort: an app left behind is listed nowhere, the administration aside.
    // Resolves with each app not deleted and why
    deleteApplications(applications) {
      return Promise.all(applications.map(application => this.$applicationService.deleteContextApplication(this.settingName, application.id)
        .then(() => null)
        .catch(e => `${application.title} (${this.getDeleteErrorReason(e)})`)))
        .then(failures => failures.filter(Boolean));
    },
    getDeleteErrorReason(error) {
      if (error?.message && this.$te(error.message)) {
        return this.$t(error.message);
      } else if (error?.status === 403) {
        return this.$t('myApplications.createdApps.deleteError.forbidden');
      } else if (error?.status === 404) {
        return this.$t('myApplications.createdApps.deleteError.notFound');
      }
      return this.$t('myApplications.createdApps.deleteError.unknownReason');
    },
    reportNotDeleted(notDeleted) {
      if (notDeleted.length) {
        this.$root.$emit('alert-message', this.$t('myApplications.createdApps.deleteError.message', {0: notDeleted.join(', ')}), 'error');
      }
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
      this.display = this.savedDisplay && {...this.savedDisplay};
      this.displayVersion++;
    }
  }
};
</script>
