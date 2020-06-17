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
  <v-navigation-drawer
    v-model="applicationsDrawer"
    absolute
    right
    stateless
    temporary
    width="420"
    max-width="100vw"
    max-height="100vh"
    class="appCenterDrawer"
  >
    <v-row class="mx-0 title">
      <v-list-item class="applicationsDrawerHeader">
        <v-list-item-content>
          <slot></slot>
        </v-list-item-content>
        <v-list-item-action class="applicationsDrawerIcons">
          <v-btn
            icon
            class="rightIcon"
            @click="$emit('closeDrawer')"
          >
            <v-icon
              large
              class="closeIcon"
            >
              close
            </v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-row>

    <v-divider class="my-0 appHeaderBorder" />

    <v-col class="content">
      <v-row class="applicationForm">
        <v-col>
          <v-label for="title">
            {{ $t('appCenter.adminSetupForm.title') }}
          </v-label>
          <input
            v-model="formArray.title"
            :readonly="formArray.system"
            :placeholder="$t('appCenter.adminSetupForm.titlePlaceholder')"
            type="text"
            name="name"
            class="input-block-level ignore-vuetify-classes my-3"
            maxlength="200"
            required
          />
          <!--          <span class="requiredInput">*</span>-->
          <!--          <p v-if="!formArray.system && !formArray.title" class="errorInput">-->
          <!--            {{ $t("appCenter.adminSetupForm.titleError") }}-->
          <!--          </p>-->
          <v-label for="url">
            {{ $t('appCenter.adminSetupForm.url') }}
          </v-label>
          <input
            v-model="formArray.url"
            type="url"
            :readonly="formArray.system"
            :placeholder="$t('appCenter.adminSetupForm.urlPlaceholder')"
            name="name"
            class="input-block-level ignore-vuetify-classes my-3"
            maxlength="200"
            required
          />
          <!--          <span class="requiredInput">*</span>-->
          <!--          <p v-if="!formArray.system && !validUrl(formArray)" class="errorInput">-->
          <!--            {{ $t("appCenter.adminSetupForm.urlError") }}-->
          <!--          </p>-->
          <v-row>
            <v-col>
              <v-label for="image">
                {{ $t('appCenter.adminSetupForm.image') }}
              </v-label>
            </v-col>
            <v-col>
              <v-btn
                class="text-uppercase caption primary--text seeAllApplicationsBtn"
                outlined
                small
              >
                {{ $t('appCenter.appLauncher.drawer.viewAll') }}
              </v-btn>
            </v-col>
            <v-col>
              Image
            </v-col>
          </v-row>
          <v-label for="description">
            {{ $t('appCenter.adminSetupForm.description') }}
          </v-label>
          <v-textarea
            v-model="formArray.description"
            :placeholder="$t('appCenter.adminSetupForm.description')"
            name="description"
            rows="20"
            maxlength="2000"
            no-resize
          >
          </v-textarea>
          <v-row class="applicationProperties">
            <v-col>
              <v-switch v-model="formArray.isMandatory" :label="$t('appCenter.adminSetupForm.mandatory')"></v-switch>
            </v-col>
            <v-col>
              <v-switch v-model="formArray.active" :label="$t('appCenter.adminSetupForm.active')"></v-switch>
            </v-col>
            <v-col>
              <v-switch v-model="formArray.mobile" :label="$t('appCenter.adminSetupForm.mobile')"></v-switch>
            </v-col>
          </v-row>
          <v-label for="permissions">
            {{ $t('appCenter.adminSetupForm.permissions') }}
          </v-label>
          <input
            :placeholder="$t('appCenter.adminSetupForm.permissionsPlaceHolder')"
            type="text"
            name="name"
            class="input-block-level ignore-vuetify-classes my-3"
            maxlength="200"
          />
          <v-label for="helpPage">
            {{ $t('appCenter.adminSetupForm.helpPage') }}
          </v-label>
          <input
            v-model="formArray.helpPage"
            :placeholder="$t('appCenter.adminSetupForm.helpPagePlaceholder')"
            type="url"
            name="name"
            class="input-block-level ignore-vuetify-classes my-3"
            maxlength="200"
          />
        </v-col>
      </v-row>
    </v-col>

    <v-row class="drawerActions mx-0 px-3">
      <v-card
        flat
        tile
        class="d-flex flex justify-end mx-2"
      >
        <button type="button" class="btn ml-2 applicationsActionBtn" @click="resetForm">
          {{ $t('appCenter.adminSetupForm.cancel') }}
        </button>
        <button type="button" class="btn btn-primary ml-6 applicationsActionBtn" @click="submitForm">
          {{ $t('appCenter.adminSetupForm.save') }}
        </button>
      </v-card>
    </v-row>
  </v-navigation-drawer>
</template>

<script>
export default {
  props: {
    applicationsDrawer: {
      type: Boolean,
      default: false
    },
    formArray: {
      type: Object,
      default: null
    },
  },
  data() {
    return {
      isAppMandatory: false,
    };
  },
  watch: {
    applicationsDrawer() {
      if (this.applicationsDrawer) {
        $('body').addClass('hide-scroll');
        this.$nextTick().then(() => {
          $('#app .v-overlay').click(() => {
            this.$emit('closeDrawer');
          });
        });
      } else {
        $('body').removeClass('hide-scroll');
      }
    }, 
  },
  methods: {
    validUrl(app) {
      const url = app && app.url;
      return app.system || url && (url.indexOf('/portal/') === 0 || url.indexOf('./') === 0 || url.match(/(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}/g));
    },
    addOrEditApplication() {
      return fetch('/portal/rest/app-center/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: this.formArray.id ? 'PUT' : 'POST',
        body: JSON.stringify({
          id: this.formArray.id,
          title: this.formArray.title,
          url: this.formArray.url,
          description: this.formArray.description,
          active: this.formArray.active,
          byDefault: this.formArray.byDefault,
          permissions: this.formArray.permissions,
          imageFileBody: this.formArray.imageFileBody,
          imageFileName: this.formArray.imageFileName,
          imageFileId: this.formArray.imageFileId,
        })
      })
        .then(() => {
          this.$emit('initApps');
          this.resetForm();
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
    submitForm() {
      const MAX_FILE_SIZE = 100000;

      if (
        this.formArray.title &&
        this.formArray.url &&
        this.validUrl(this.formArray)
      ) {
        if (this.$refs.file && this.$refs.file.files.length > 0) {
          const reader = new FileReader();
          reader.onload = e => {
            if (!e.target.result.includes('data:image')) {
              this.formArray.invalidImage = true;
              return;
            }
            if (this.$refs.file.files[0].size > MAX_FILE_SIZE) {
              this.formArray.invalidSize = true;
              return;
            }
            this.formArray.imageFileBody = e.target.result;
            this.addOrEditApplication();
          };
          reader.readAsDataURL(this.$refs.file.files[0]);
        } else {
          this.addOrEditApplication();
          this.$emit('closeDrawer');
        }
      }
    },
    resetForm() {
      this.$emit('resetForm');
    },
  },
};
</script>
