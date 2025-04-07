<!--

  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <exo-drawer
    ref="formDrawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    :loading="loading"
    body-classes="hide-scroll"
    class="appCenterDrawer">
    <template v-if="drawer" #title>
      <span class="appLauncherDrawerTitle">{{ drawerTitle }}</span>
    </template>
    <template v-if="drawer" #content>
      <v-form
        ref="form"
        autocomplete="off"
        class="pa-4"
        @submit.prevent.stop="0">
        <v-label for="applicationName">
          {{ $t('appCenter.adminSetupForm.title') }}
        </v-label>
        <translation-text-field
          ref="applicationName"
          id="applicationName"
          v-model="titles"
          :rules="rules.name"
          :placeholder="$t('appCenter.adminSetupForm.titlePlaceholder')"
          :maxlength="maxNameLength"
          name="applicationName"
          drawer-title="appCenter.adminSetupForm.titleTranslation"
          class="width-auto flex-grow-1 mt-2 mb-4"
          no-expand-icon
          back-icon
          autofocus
          required />
        <v-label for="applicationDescription">
          {{ $t('appCenter.adminSetupForm.description') }}
        </v-label>
        <translation-text-field
          ref="applicationDescription"
          id="applicationDescription"
          v-model="descriptions"
          :rules="rules.description"
          :placeholder="$t('appCenter.adminSetupForm.descriptionPlaceHolder')"
          :maxlength="maxNameLength"
          name="applicationDescription"
          drawer-title="appCenter.adminSetupForm.descriptionTranslation"
          class="width-auto flex-grow-1 mt-2 mb-4"
          no-expand-icon
          back-icon
          autofocus
          required />
        <div class="mb-2">
          {{ $t('appCenter.adminSetupForm.application') }}
        </div>
        <v-radio-group
          v-model="application.type"
          class="mt-0 pa-0"
          mandatory>
          <v-radio value="LINK">
            <template #label>
              <span class="ms-1">{{ $t('appCenter.adminSetupForm.link') }}</span>
            </template>
          </v-radio>
          <v-radio value="DRAWER">
            <template #label>
              <span class="ms-1">{{ $t('appCenter.adminSetupForm.drawer') }}</span>
            </template>
          </v-radio>
          <v-radio value="PORTLET">
            <template #label>
              <span class="ms-1">{{ $t('appCenter.adminSetupForm.portlet') }}</span>
            </template>
          </v-radio>
        </v-radio-group>
        <v-text-field
          v-if="application.type === 'LINK'"
          ref="applicationUrl"
          id="applicationUrl"
          v-model="application.url"
          :placeholder="$t('appCenter.adminSetupForm.urlPlaceholder')"
          :rules="rules.url"
          name="applicationUrl"
          class="border-box-sizing width-auto pt-0 mb-3"
          type="text"
          mandatory
          outlined
          dense />
        <quick-action-suggester
          v-else-if="application.type === 'DRAWER'"
          ref="applicationUrl"
          id="applicationUrl"
          v-model="application.url"
          name="applicationUrl"
          class="mb-4" />
        <portlet-instance-suggester
          v-else-if="application.type === 'PORTLET'"
          ref="applicationUrl"
          id="applicationUrl"
          v-model="application.url"
          name="applicationUrl"
          class="mb-4" />
        <category-input
          v-model="newCategoryIds"
          label="appCenter.adminSetupForm.categories"
          label-class="" />
        <div class="mb-2">
          {{ $t('appCenter.adminSetupForm.updateTheIcon') }}
        </div>
        <app-center-image-input
          v-model="application.imageUploadId"
          :application="application"
          class="mb-4"
          @icon="application.icon = $event"
          @reset="resetImage" />
        <div class="text-header mb-2">
          {{ $t('appCenter.adminSetupForm.advancedOptions') }}
        </div>
        <div class="mb-2">
          <div class="d-flex full-width align-center mb-2">
            <v-card
              class="text-start flex-grow-1 clickable transparent"
              flat
              @click="application.mandatory = !application.mandatory">
              {{ $t('appCenter.adminSetupForm.mandatory') }}
            </v-card>
            <v-switch
              v-model="application.mandatory"
              class="ma-0 pa-0"
              name="applicationMandatory"
              hide-details />
          </div>
          <div class="d-flex full-width justify-space-between align-center mb-2">
            <v-card
              class="text-start flex-grow-1 clickable transparent"
              flat
              @click="application.default = !application.default">
              {{ $t('appCenter.adminSetupForm.default') }}
            </v-card>
            <v-switch
              v-model="application.default"
              class="ma-0 pa-0"
              name="applicationDefault"
              hide-details />
          </div>
          <div class="d-flex full-width justify-space-between align-center mb-2">
            <v-card
              class="text-start flex-grow-1 clickable transparent"
              flat
              @click="application.mobile = !application.mobile">
              {{ $t('appCenter.adminSetupForm.mobile') }}
            </v-card>
            <v-switch
              v-model="application.mobile"
              class="ma-0 pa-0"
              name="applicationMobile"
              hide-details />
          </div>
        </div>
        <div class="mb-2">
          {{ $t('appCenter.adminSetupForm.permissions') }}
        </div>
        <app-center-permissions
          v-model="application.permissions"
          class="mb-4" />
        <v-label for="applicationHelpPageURL">
          {{ $t('appCenter.adminSetupForm.helpPage') }}
        </v-label>
        <v-text-field
          ref="applicationHelpPageURL"
          id="applicationHelpPageURL"
          v-model="application.helpPageURL"
          :placeholder="$t('appCenter.adminSetupForm.helpPagePlaceholder')"
          :rules="rules.helpUrl"
          name="applicationHelpPageURL"
          class="border-box-sizing width-auto pt-0 mt-2 mb-3"
          type="text"
          outlined
          dense />
        <v-label for="applicationShortcut">
          {{ $t('appCenter.adminSetupForm.shortcut') }}
        </v-label>
        <v-text-field
          ref="applicationShortcut"
          id="applicationShortcut"
          v-model="application.shortcut"
          :placeholder="$t('appCenter.adminSetupForm.shortcutPlaceholder')"
          :rules="rules.shortcut"
          name="applicationShortcut"
          class="border-box-sizing width-auto pt-0 mt-2 mb-4"
          type="text"
          maxlength="1"
          outlined
          dense>
          <template #prepend-inner>
            <div class="d-flex align-center mt-n1 ms-n1">
              <v-card
                class="fill-height grey-lighten1-background white--text px-5 py-2"
                flat>
                Ctrl
              </v-card>
              <v-icon class="mx-2" size="24">fa-plus</v-icon>
              <v-card
                class="fill-height grey-lighten1-background white--text px-5 py-2"
                flat>
                Alt
              </v-card>
              <v-icon class="mx-2" size="24">fa-plus</v-icon>
            </div>
          </template>
        </v-text-field>
        <div class="d-flex full-width align-center mb-2">
          <v-card
            class="text-start flex-grow-1 clickable transparent"
            flat
            @click="application.mandatory = !application.mandatory">
            {{ $t('appCenter.adminSetupForm.pwa') }}
          </v-card>
          <v-switch
            v-model="application.pwa"
            class="ma-0 pa-0"
            name="applicationPwa"
            hide-details />
        </div>
      </v-form>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          class="btn ms-auto applicationsActionBtn"
          @click="close">
          {{ $t('appCenter.adminSetupForm.cancel') }}
        </v-btn>
        <v-btn
          :disabled="disabled"
          class="btn btn-primary ms-6 applicationsActionBtn"
          @click="save">
          {{ $t('appCenter.adminSetupForm.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    maxDescriptionLength: 500,
    maxNameLength: 200,
    drawer: false,
    titles: {},
    descriptions: {},
    application: {},
    oldCategoryIds: [],
    newCategoryIds: [],
  }),
  computed: {
    drawerTitle() {
      return this.application?.id ? this.$t('appCenter.adminSetupForm.createNewApp') : this.$t('appCenter.adminSetupForm.editApp');
    },
    validUrl() {
      return (
        this.application?.type === 'LINK'
        && this.$utils.toLinkUrl(this.application?.url, {
          urls: true,
          email: true,
          phone: true,
        })?.length
      ) || (
        this.application?.type !== 'LINK'
        && this.application?.url
      );
    },
    validHelpPageUrl() {
      return !this.application?.helpPageURL
        || this.$utils.toLinkUrl(this.application?.helpPageURL, {
          urls: true,
          email: true,
          phone: true,
        })?.length;
    },
    rules() {
      return {
        name: [
          v => !!v?.length || ' ',
          v => !v?.length || v.length < this.maxNameLength || this.$t('appCenter.form.name.exceedsMaxLength', {
            0: this.maxNameLength,
          }),
        ],
        description: [
          v => !v?.length || v.length < this.maxDescriptionLength || this.$t('appCenter.form.description.exceedsMaxLength', {
            0: this.maxDescriptionLength,
          }),
        ],
        url: [
          () => !!this.application?.url?.length || ' ',
          () => !!this.validUrl || this.$t('appCenter.form.url.invalidLink'),
        ],
        helpUrl: [
          () => !!this.validHelpPageUrl || this.$t('appCenter.form.url.invalidLink'),
        ],
        shortcut: [
          () => !this.application.shortcut?.length || this.application.shortcut.length === 1 || this.$t('appCenter.adminSetupForm.shortcutInvalidLength'),
          v => !this.shortcutExists(v) || this.$t('appCenter.adminSetupForm.shortcutAlreadyInUse'),
        ],
      };
    },
    permissionSuggesterLabels() {
      return {
        searchPlaceholder: this.$t('appCenter.adminSetupForm.permissionsPlaceHolder'),
        placeholder: this.$t('appCenter.adminSetupForm.permissionsPlaceHolder'),
        noDataLabel: this.$t('appCenter.adminSetupForm.permissionsNoResult'),
      };
    },
    title() {
      return this.titles[eXo.env.portal.defaultLanguage];
    },
    description() {
      return this.descriptions[eXo.env.portal.defaultLanguage];
    },
    disabled() {
      return !this.title?.length
        || !!(this.description?.length && this.description?.length > this.maxDescriptionLength)
        || !this.validUrl
        || !this.validHelpPageUrl
        || !!(this.application?.shortcut?.length && this.shortcutExists(this.application.shortcut));
    },
  },
  watch: {
    'application.type': {
      handler(newVal, oldVal) {
        if (this.drawer && newVal && oldVal) {
          this.application.url = null;
        }
      },
    },
    title(newVal) {
      if (this.drawer) {
        this.application.title = newVal;
      }
    },
    description(newVal) {
      if (this.drawer) {
        this.application.description = newVal;
      }
    },
  },
  created() {
    this.$root.$on('app-center-drawer-open', this.open);
  },
  beforeDestroy() {
    this.$root.$off('app-center-drawer-open', this.open);
  },
  methods: {
    async open(app) {
      this.$root.$emit('close-alert-message');
      this.application = app && JSON.parse(JSON.stringify(app)) || {
        icon: null,
        imageUrl: null,
        url: null,
        helpPageURL: null,
        shortcut: null,
        active: true,
        default: false,
        mandatory: false,
        mobile: false,
        system: false,
        type: 'LINK', // LINK, DRAWER or PORTLET
        permissions: [],
        categoryIds: [],
      };
      this.oldCategoryIds = app?.categoryIds?.slice?.() || [];
      this.newCategoryIds = this.oldCategoryIds.slice();
      if (app?.id) {
        this.titles = await this.$translationService.getTranslations('appCenter', app.id, 'title');
        this.descriptions = await this.$translationService.getTranslations('appCenter', app.id, 'description');
        if (!this.titles || !Object.keys(this.titles).length || !this.titles[eXo.env.portal.defaultLanguage]?.length) {
          this.titles = this.titles || {};
          this.titles[eXo.env.portal.defaultLanguage] = app.title || '';
        }
        if (!this.descriptions || !Object.keys(this.descriptions).length || !this.descriptions[eXo.env.portal.defaultLanguage]?.length) {
          this.descriptions = this.descriptions || {};
          this.descriptions[eXo.env.portal.defaultLanguage] = app.description || '';
        }
      } else {
        this.titles = {};
        this.descriptions = {};
      }
      await this.$nextTick();
      this.$refs.formDrawer.open();
    },
    resetImage() {
      this.application.imageUrl = null;
      this.application.imageFileId = null;
    },
    close() {
      this.$refs.formDrawer.close();
    },
    save() {
      const isNew = !this.application.id;
      this.loading = true;
      return fetch('/app-center/rest/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: isNew ? 'POST' : 'PUT',
        body: JSON.stringify(this.application)
      })
        .then(resp => {
          if (resp?.ok) {
            if (isNew) {
              return resp.json();
            } else {
              return this.application;
            }
          } else {
            throw new Error();
          }
        })
        .then(async app => {
          await this.$translationService.saveTranslations('appCenter', app.id, 'title', this.titles);
          await this.$translationService.saveTranslations('appCenter', app.id, 'description', this.descriptions);
          await this.$applicationCategoryService.updateCategories(app.id, this.oldCategoryIds, this.newCategoryIds);
          this.$root.$emit('app-center-refresh-list');
          if (isNew) {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationCreatedSuccessfully'), 'success');
          } else {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationUpdatedSuccessfully'), 'success');
          }
          this.close();
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.errorSavingApplication'), 'error'))
        .finally(() => this.loading = false);
    },
    shortcutExists(c) {
      return !!this.$root.applications.find(a => a.shortcut === c && a.id !== this.application.id);
    },
  },
};
</script>
