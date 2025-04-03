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
    <template #title>
      <slot></slot>
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
          name="applicationUrl"
          class="border-box-sizing width-auto pt-0 mb-4"
          type="text"
          hide-details
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
          v-model="application.categoryIds"
          label="appCenter.adminSetupForm.categories"
          label-class="" />
        <div class="mb-2">
          {{ $t('appCenter.adminSetupForm.updateTheIcon') }}
        </div>
        <app-center-image-input
          v-model="application.imageUploadId"
          :application="application"
          class="mb-4"
          @icon="application.icon = $event" />
        <div class="text-header mb-2">
          {{ $t('appCenter.adminSetupForm.advancedOptions') }}
        </div>
        <div class="mb-2">
          <div class="d-flex full-width align-center mb-2">
            <label
              for="applicationMandatory"
              class="text-start flex-grow-1"
              @click="application.mandatory = !application.mandatory">
              {{ $t('appCenter.adminSetupForm.mandatory') }}
            </label>
            <v-switch
              v-model="application.mandatory"
              class="mandatoryLabel ma-0 pa-0"
              name="applicationMandatory"
              hide-details />
          </div>
          <div class="d-flex full-width justify-space-between align-center mb-2">
            <label
              for="applicationDefault"
              class="text-start flex-grow-1"
              @click="application.default = !application.default">{{ $t('appCenter.adminSetupForm.default') }}</label>
            <v-switch
              v-model="application.default"
              class="ma-0 pa-0"
              name="applicationDefault"
              hide-details />
          </div>
          <div class="d-flex full-width justify-space-between align-center mb-2">
            <label
              for="applicationMobile"
              class="text-start flex-grow-1"
              @click="application.mobile = !application.mobile">{{ $t('appCenter.adminSetupForm.mobile') }}</label>
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
          name="applicationHelpPageURL"
          class="border-box-sizing width-auto pt-0 mt-2 mb-4"
          type="text"
          hide-details
          mandatory
          outlined
          dense />
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
  props: {
    appToEditOriginalTitle: {
      type: Object,
      default: null
    },
  },
  data: () => ({
    maxDescriptionLength: 500,
    maxNameLength: 200,
    drawer: false,
    titles: {},
    descriptions: {},
    application: {},
  }),
  computed: {
    disabled() {
      return !this.application.title?.length
        || this.application.description.length > this.maxDescriptionLength
        || !this.validUrl
        || (this.application.helpPageURL?.length && !this.validHelpPageUrl);
    },
    validUrl() {
      return this.application.type !== 'LINK' || this.application.system || this.$utils.toLinkUrl(this.application?.url, {
        urls: true,
        email: true,
        phone: true,
      })?.length;
    },
    validHelpPageUrl() {
      try {
        return this.application.system || this.$utils.toLinkUrl(this.application?.helpPageURL, {
          urls: true,
          email: true,
          phone: true,
        })?.length;
      } catch (e) {
        return false;
      }
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
          v => !!v?.length || ' ',
          v => !!v?.length || !!this.validUrl || this.$t('appCenter.form.url.invalidLink'),
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
  },
  watch: {
    'application.type': {
      handler() {
        this.application.url = null;
      },
    },
  },
  methods: {
    async open(app) {
      this.application = app || {
        icon: null,
        imageUrl: null,
        active: false,
        default: false,
        mandatory: false,
        mobile: false,
        system: false,
        type: 'LINK', // LINK, DRAWER or PORTLET
        permissions: [],
        categoryIds: [],
      };
      this.$refs.formDrawer.open();
      if (app.id) {
        this.titles = await this.$translationService.getTranslations('appCenter', app.id, 'title');
        this.descriptions = await this.$translationService.getTranslations('appCenter', app.id, 'description');
        if (!this.titles || !Object.keys(this.titles).length) {
          this.titles = {};
          this.titles[eXo.env.portal.defaultLanguage] = app.title || '';
        }
        if (!this.descriptions || !Object.keys(this.descriptions).length) {
          this.descriptions = {};
          this.descriptions[eXo.env.portal.defaultLanguage] = app.description || '';
        }
      } else {
        this.titles = {};
        this.descriptions = {};
      }
    },
    close() {
      this.$refs.formDrawer.close();
    },
    save() {
      this.loading = true;
      return fetch('/app-center/rest/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: this.application.id ? 'PUT' : 'POST',
        body: JSON.stringify({
          id: this.application.id,
          title: this.application.title,
          url: this.application.url,
          helpPageURL: this.application.helpPageURL,
          description: this.application.description,
          active: this.application.active,
          default: this.application.default,
          mandatory: this.application.mandatory,
          mobile: this.application.mobile,
          system: this.application.system,
          permissions: this.application.permissions,
          imageFileId: this.application.imageFileId,
        })
      })
        .then(() => this.$emit('initApps'))
        .catch(e => {
          if (e.response.status === 401) {
            this.error = this.$t('appCenter.adminSetupForm.unauthorized');
          } else {
            this.error = this.$t('appCenter.adminSetupForm.error');
          }
        })
        .finally(() => {
          this.loading = false;
          this.close();
        });
    },
    appTitleExists(v) {
      return (v || this.application.title) !== this.appToEditOriginalTitle && this.existingAppNames.includes(v || this.application.title);
    },
  },
};
</script>
