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
    <template v-if="drawer && application" #content>
      <v-form
        ref="form"
        autocomplete="off"
        class="pa-4"
        @submit.prevent.stop="0">
        <div v-if="!personal" class="text-header mb-4">
          {{ $t('appCenter.adminSetupForm.display') }}
        </div>
        <v-label for="applicationName">
          {{ $t('appCenter.adminSetupForm.title') }}
        </v-label>
        <translation-text-field
          v-if="!personal"
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
          required />
        <v-text-field
          v-else
          id="applicationName"
          v-model="application.title"
          :placeholder="$t('appCenter.adminSetupForm.titlePlaceholder')"
          :rules="rules.personalTitle"
          :maxlength="maxNameLength"
          name="applicationName"
          class="width-auto flex-grow-1 mt-2 pt-0 mb-4"
          outlined
          dense
          required />
        <template v-if="!personal">
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
            required />
        </template>
        <template v-if="!personal">
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
            <template v-if="application.type === 'LINK'">
              <v-text-field
                ref="applicationUrl"
                id="applicationUrl"
                v-model="application.url"
                :placeholder="$t('appCenter.adminSetupForm.urlPlaceholder')"
                :rules="rules.url"
                name="applicationUrl"
                class="border-box-sizing width-auto pt-0"
                type="text"
                mandatory
                outlined
                dense />
              <div class="d-flex full-width justify-space-between align-center mb-3">
                <v-card
                  class="text-start flex-grow-1 clickable transparent"
                  flat
                  @click="application.sameTab = !application.sameTab">
                  {{ $t('appCenter.adminSetupForm.openInSameTab') }}
                </v-card>
                <v-switch
                  v-model="application.sameTab"
                  class="my-0 me-n2 pa-0"
                  name="applicationSameTabSwitch"
                  hide-details />
              </div>
            </template>
            <v-radio value="DRAWER">
              <template #label>
                <span class="ms-1">{{ $t('appCenter.adminSetupForm.drawer') }}</span>
              </template>
            </v-radio>
            <quick-action-suggester
              v-if="application.type === 'DRAWER'"
              ref="applicationUrl"
              id="applicationUrl"
              v-model="application.url"
              :quick-actions="quickActions"
              name="applicationUrl"
              class="mb-4" />
            <v-radio value="PORTLET">
              <template #label>
                <span class="ms-1">{{ $t('appCenter.adminSetupForm.portlet') }}</span>
              </template>
            </v-radio>
            <portlet-instance-suggester
              v-if="application.type === 'PORTLET'"
              ref="applicationUrl"
              id="applicationUrl"
              v-model="application.url"
              :portlet-instances.sync="portletInstances"
              name="applicationUrl"
              class="mb-4" />
          </v-radio-group>
          <template v-if="displayBadgeField">
            <div class="mb-2">
              {{ $t('appCenter.adminSetupForm.badge.label') }}
            </div>
            <v-chip
              v-if="resolvedBadgeProvider"
              class="mb-3"
              outlined>
              {{ resolvedBadgeProviderLabel }}
            </v-chip>
            <v-autocomplete
              v-else
              v-model="application.badgeName"
              :items="badgeProviders"
              :placeholder="$t('appCenter.adminSetupForm.badge.placeholder')"
              class="border-box-sizing width-auto pt-0 mb-3"
              name="applicationBadgeName"
              item-value="value"
              item-text="text"
              deletable-chips
              outlined
              attach
              chips
              dense />
            <div class="d-flex full-width justify-space-between align-center mb-3">
              <v-card
                class="text-start flex-grow-1 clickable transparent"
                flat
                @click="badgeDisplayed = !badgeDisplayed">
                {{ $t('appCenter.adminSetupForm.badge.display') }}
              </v-card>
              <v-switch
                v-model="badgeDisplayed"
                class="my-0 me-n2 pa-0"
                name="applicationBadgeSwitch"
                hide-details />
            </div>
          </template>
          <category-input
            v-model="newCategoryIds"
            label="appCenter.adminSetupForm.categories"
            label-class="" />
        </template>
        <template v-else>
          <v-label for="personalApplicationUrl">
            {{ $t('appCenter.personalApp.form.url.label') }}
          </v-label>
          <v-text-field
            ref="personalApplicationUrl"
            id="personalApplicationUrl"
            v-model="application.url"
            :placeholder="$t('appCenter.personalApp.form.url.placeholder')"
            :rules="rules.personalUrl"
            name="personalApplicationUrl"
            class="border-box-sizing width-auto pt-0 mt-2"
            type="text"
            outlined
            dense />
          <div class="d-flex full-width justify-space-between align-center mb-3">
            <v-card
              class="text-start flex-grow-1 clickable transparent"
              flat
              @click="application.sameTab = !application.sameTab">
              {{ $t('appCenter.adminSetupForm.openInSameTab') }}
            </v-card>
            <v-switch
              v-model="application.sameTab"
              class="my-0 me-n2 pa-0"
              name="applicationSameTabSwitch"
              hide-details />
          </div>
        </template>
        <div class="mb-2">
          {{ $t('appCenter.adminSetupForm.updateTheIcon') }}
        </div>
        <app-center-image-input
          v-model="uploadedImage"
          :application="application"
          :accept="imageTypes"
          class="mb-4"
          @icon="application.icon = $event"
          @reset="resetImage" />
        <template v-if="!personal">
          <div class="text-header mb-2">
            {{ $t('appCenter.adminSetupForm.advancedOptions') }}
          </div>
          <div class="mb-2">
            <div
              v-if="!contextual"
              :title="forcedFieldTitle"
              class="d-flex full-width align-center mb-2">
              <v-card
                :class="forcedFieldsEditable && 'clickable'"
                class="text-start flex-grow-1 transparent"
                flat
                v-on="forcedFieldsEditable && {click: () => application.mandatory = !application.mandatory}">
                {{ $t('appCenter.adminSetupForm.mandatory') }}
              </v-card>
              <v-switch
                v-model="application.mandatory"
                :disabled="!forcedFieldsEditable"
                class="ma-0 pa-0"
                name="applicationMandatory"
                hide-details />
            </div>
            <div
              v-if="!contextual"
              :title="forcedFieldTitle"
              class="d-flex full-width justify-space-between align-center mb-2">
              <v-card
                :class="forcedFieldsEditable && 'clickable'"
                class="text-start flex-grow-1 transparent"
                flat
                v-on="forcedFieldsEditable && {click: () => application.default = !application.default}">
                {{ $t('appCenter.adminSetupForm.default') }}
              </v-card>
              <v-switch
                v-model="application.default"
                :disabled="!forcedFieldsEditable"
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
          <template v-if="!contextual">
            <div class="d-flex full-width justify-space-between align-center mb-2">
              <v-card
                class="text-start flex-grow-1 clickable transparent"
                flat
                @click="hasPermissions = !hasPermissions">
                {{ $t('appCenter.adminSetupForm.permissions') }}
              </v-card>
              <v-switch
                v-model="hasPermissions"
                class="ma-0 pa-0"
                name="applicationPermissionsSwitch"
                hide-details />
            </div>
            <app-center-permissions
              v-if="hasPermissions"
              v-model="application.permissions"
              class="mb-4" />
          </template>
          <div class="d-flex full-width justify-space-between align-center mb-2">
            <v-card
              class="text-start flex-grow-1 clickable transparent"
              flat
              @click="hasHelpUrl = !hasHelpUrl">
              {{ $t('appCenter.adminSetupForm.helpPage') }}
            </v-card>
            <v-switch
              v-model="hasHelpUrl"
              class="ma-0 pa-0"
              name="applicationHelpUrlSwitch"
              hide-details />
          </div>
          <v-text-field
            v-if="hasHelpUrl"
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
          <template v-if="!contextual">
            <div class="d-flex full-width justify-space-between align-center mb-2">
              <v-card
                :title="shortcutTitle"
                class="text-start flex-grow-1 clickable transparent"
                flat
                v-on="shortcutEditable && {
                  click: () => hasShortcut = !hasShortcut
                }">
                {{ $t('appCenter.adminSetupForm.shortcut') }}
              </v-card>
              <div :title="shortcutTitle">
                <v-switch
                  v-model="hasShortcut"
                  :disabled="!shortcutEditable"
                  class="ma-0 pa-0"
                  name="applicationShortcutSwitch"
                  hide-details />
              </div>
            </div>
            <div :title="shortcutTitle">
              <v-text-field
                v-if="hasShortcut"
                ref="applicationShortcut"
                id="applicationShortcut"
                v-model="application.shortcut"
                :disabled="!shortcutEditable"
                :placeholder="$t('appCenter.adminSetupForm.shortcutPlaceholder')"
                :rules="rules.shortcut"
                name="applicationShortcut"
                class="border-box-sizing width-auto pt-0 mt-2 mb-3"
                type="text"
                maxlength="1"
                outlined
                dense>
                <template #prepend-inner>
                  <div class="d-flex align-center mt-n1 ms-n1">
                    <v-card
                      class="fill-height grey-lighten1-background white--text px-5 py-2"
                      flat>
                      {{ $t('appCenter.adminSetupForm.ctrl') }}
                    </v-card>
                    <v-icon class="mx-2" size="24">fa-plus</v-icon>
                    <v-card
                      class="fill-height grey-lighten1-background white--text px-5 py-2"
                      flat>
                      {{ $t('appCenter.adminSetupForm.shift') }}
                    </v-card>
                    <v-icon class="mx-2" size="24">fa-plus</v-icon>
                  </div>
                </template>
              </v-text-field>
            </div>
            <div :title="forcedFieldTitle" class="d-flex full-width align-center mb-2">
              <v-card
                :class="forcedFieldsEditable && 'clickable'"
                class="text-start flex-grow-1 transparent"
                flat
                v-on="forcedFieldsEditable && {click: () => application.pwa = !application.pwa}">
                {{ $t('appCenter.adminSetupForm.pwa') }}
              </v-card>
              <v-switch
                v-model="application.pwa"
                :disabled="!forcedFieldsEditable"
                class="ma-0 pa-0"
                name="applicationPwa"
                hide-details />
            </div>
          </template>
          <template v-if="displayPlacementFields">
            <div
              v-if="!contextual"
              :title="forcedFieldTitle"
              class="d-flex full-width justify-space-between align-center mb-2">
              <v-card
                :class="forcedFieldsEditable && 'clickable'"
                class="text-start flex-grow-1 transparent"
                flat
                v-on="forcedFieldsEditable && {click: () => application.allowStick = !application.allowStick}">
                {{ $t('appCenter.adminSetupForm.allowStick') }}
              </v-card>
              <v-switch
                v-model="application.allowStick"
                :disabled="!forcedFieldsEditable"
                class="ma-0 pa-0"
                name="applicationAllowStick"
                hide-details />
            </div>
            <div class="d-flex full-width justify-space-between align-center mb-2">
              <v-card
                class="text-start flex-grow-1 clickable transparent"
                flat
                @click="application.allowDetach = !application.allowDetach">
                {{ $t('appCenter.adminSetupForm.allowDetach') }}
              </v-card>
              <v-switch
                v-model="application.allowDetach"
                class="ma-0 pa-0"
                name="applicationAllowDetach"
                hide-details />
            </div>
          </template>
        </template>
      </v-form>
    </template>
    <template #footer>
      <div class="d-flex align-center">
        <v-btn
          v-if="editingPersonalApp"
          :disabled="loading"
          color="error"
          outlined
          elevation="0"
          class="ignore-vuetify-classes"
          @click="$refs.deleteConfirmDialog.open()">
          <span class="text-none">{{ $t('appCenter.adminSetupForm.modal.delete') }}</span>
        </v-btn>
        <v-btn
          class="btn ms-auto applicationsActionBtn"
          @click="close">
          {{ $t('appCenter.adminSetupForm.cancel') }}
        </v-btn>
        <v-btn
          :disabled="disabled"
          class="btn btn-primary ms-6 applicationsActionBtn"
          @click="save">
          {{ submitLabel }}
        </v-btn>
        <confirm-dialog
          v-if="editingPersonalApp"
          ref="deleteConfirmDialog"
          :title="$t('appCenter.personalApp.delete.confirm.title')"
          :message="$t('appCenter.personalApp.delete.confirm.message')"
          :ok-label="$t('appCenter.adminSetupForm.modal.delete')"
          :cancel-label="$t('appCenter.adminSetupForm.cancel')"
          @ok="deletePersonalApp" />
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    personal: {
      type: Boolean,
      default: false,
    },
    // Creates an application only the Shortcuts portlet of settingName lists:
    // the options the server forces on such an application are not offered
    contextual: {
      type: Boolean,
      default: false,
    },
    settingName: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    maxDescriptionLength: 500,
    maxNameLength: 200,
    contextualMaxUrlLength: 500,
    // The illustration types the server accepts for a Shortcuts portlet's application
    contextualImageTypes: 'image/png,image/jpeg,image/gif,image/webp',
    drawer: false,
    titles: {},
    descriptions: {},
    application: {},
    originalApplication: {},
    hasPermissions: false,
    hasHelpUrl: false,
    hasShortcut: false,
    oldCategoryIds: [],
    newCategoryIds: [],
    uploadedImage: {
      uploadId: 0,
      mimeType: ''
    },
    badgeProviderList: [],
    // Null until the administrator touches the switch, so that the displayed
    // state follows what is stored; true/false once they decided.
    badgeDisabledByAdmin: null,
    badgeProvidersRequested: false,
    quickActionExtensions: [],
    portletInstances: [],
    loading: false
  }),
  computed: {
    drawerTitle() {
      if (this.personal) {
        return this.application?.id
          ? this.$t('appCenter.personalApp.drawer.edit.title')
          : this.$t('appCenter.personalApp.drawer.title');
      }
      return this.application?.id ? this.$t('appCenter.adminSetupForm.editApp') : this.$t('appCenter.adminSetupForm.createNewApp');
    },
    editingPersonalApp() {
      return this.personal && !!this.application?.id;
    },
    submitLabel() {
      return this.editingPersonalApp
        ? this.$t('appCenter.personalApp.form.apply')
        : this.$t('appCenter.adminSetupForm.save');
    },
    validUrl() {
      if (this.contextual && this.application?.type === 'LINK') {
        return this.isValidContextLink(this.application?.url);
      }
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
    normalizedPersonalUrl() {
      return this.$applicationUrlService.normalizePersonalUrl(this.application?.url);
    },
    validHelpPageUrl() {
      if (this.contextual && this.application?.helpPageURL) {
        return this.isValidContextLink(this.application.helpPageURL);
      }
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
        personalTitle: [
          v => !!v?.trim() || ' ',
          v => !v || v.length <= this.maxNameLength || this.$t('appCenter.form.name.exceedsMaxLength', {
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
          () => !this.isContextLinkTooLong(this.application?.url) || this.$t('appCenter.contextual.urlTooLong'),
          () => !!this.validUrl || this.$t('appCenter.form.url.invalidLink'),
        ],
        personalUrl: [
          () => !!this.application?.url?.trim()?.length || ' ',
          () => !!this.normalizedPersonalUrl || this.$t('appCenter.form.url.invalidLink'),
        ],
        helpUrl: [
          () => !this.isContextLinkTooLong(this.application?.helpPageURL) || this.$t('appCenter.contextual.helpPageURLTooLong'),
          () => !!this.validHelpPageUrl || this.$t('appCenter.form.url.invalidLink'),
        ],
        shortcut: [
          () => !this.application?.shortcut?.length || this.application?.shortcut.length === 1 || this.$t('appCenter.adminSetupForm.shortcutInvalidLength'),
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
      return this.personal ? this.application?.title : this.titles[eXo.env.portal.defaultLanguage];
    },
    description() {
      return this.personal ? null : this.descriptions[eXo.env.portal.defaultLanguage];
    },
    type() {
      return this.application?.type;
    },
    badgeProviders() {
      return this.badgeProviderList.map(provider => ({
        value: provider.name,
        text: this.$te(`appCenter.adminSetupForm.badge.${provider.name}`) ? this.$t(`appCenter.adminSetupForm.badge.${provider.name}`) : provider.name,
      }));
    },
    selectedPortletInstance() {
      if (this.type === 'PORTLET' && this.application?.url && this.portletInstances?.length) {
        return this.portletInstances.find(p => `${p.id}` === `${this.application?.url}`);
      } else {
        return null;
      }
    },
    selectedPortletContentId() {
      return this.selectedPortletInstance?.contentId;
    },
    resolvedBadgeProvider() {
      let url = this.application?.url;
      if (this.personal || (this.type !== 'DRAWER' && this.type !== 'PORTLET')) {
        return null;
      } else if (this.type === 'PORTLET') {
        url = this.selectedPortletContentId;
      }
      if (!url) {
        return null;
      }
      const propName = this.type === 'DRAWER' ? 'drawerNames' : 'portletNames';
      return this.badgeProviderList.find(provider => provider[propName]?.includes(url))?.name || null;
    },
    resolvedBadgeProviderLabel() {
      if (this.resolvedBadgeProvider) {
        return this.$te(`appCenter.adminSetupForm.badge.${this.resolvedBadgeProvider}`) ? this.$t(`appCenter.adminSetupForm.badge.${this.resolvedBadgeProvider}`) : this.resolvedBadgeProvider;
      } else {
        return null;
      }
    },
    // Hidden entirely for an internal application no provider matches: an
    // empty disabled field reads as broken.
    displayBadgeField() {
      if (this.personal || !this.badgeProviderList.length) {
        return false;
      }
      return this.type === 'LINK' || !!this.resolvedBadgeProvider;
    },
    displayPlacementFields() {
      return !this.personal && (this.type === 'DRAWER' || this.type === 'PORTLET');
    },
    // The payload carries the binding as *resolved*, which reports "turned off"
    // and "not bound" identically. A provider the url matches while the payload
    // carries no badge can only mean the stored value is the reserved 'none',
    // since that is the one value resolution refuses. A Link resolves to itself,
    // so it has no such distinction to recover — and needs none, having nothing
    // to re-resolve.
    badgeDisplayed: {
      get() {
        if (this.badgeDisabledByAdmin !== null) {
          return !this.badgeDisabledByAdmin;
        }
        return !(this.resolvedBadgeProvider && !this.application?.badgeName);
      },
      set(value) {
        this.badgeDisabledByAdmin = !value;
      },
    },
    // What actually gets stored, as opposed to what the form displays: the
    // resolved provider is never written back, so an automatic binding keeps
    // following its plugin instead of being frozen on the first edit.
    badgeNameToSave() {
      if (!this.displayBadgeField) {
        return this.application?.badgeName || null;
      } else if (!this.badgeDisplayed) {
        // A reserved marker, distinct from a blank value which would simply let
        // the url binding resolve again
        return 'none';
      }
      return this.resolvedBadgeProvider ? null : (this.application?.badgeName || null);
    },
    // The same computation applied to the loaded application, so that comparing
    // the two compares stored values. Comparing the resolved name against what
    // would be stored would report every internal application as modified as
    // soon as its drawer opens.
    originalBadgeNameToSave() {
      if (!this.displayBadgeField) {
        return this.originalApplication?.badgeName || null;
      } else if (this.resolvedBadgeProvider) {
        return this.originalApplication?.badgeName ? null : 'none';
      }
      return this.originalApplication?.badgeName || null;
    },
    applicationToSave() {
      if (this.personal) {
        return { ...this.application };
      }
      return {
        ...this.application,
        badgeName: this.badgeNameToSave,
        title: JSON.parse(JSON.stringify(this.titles)),
        description: JSON.parse(JSON.stringify(this.descriptions)),
        categoryIds: this.newCategoryIds,
      };
    },
    modified() {
      // The badge is compared apart: both sides hold a resolved name, which is
      // not what either would store
      return this.badgeNameToSave !== this.originalBadgeNameToSave
        || JSON.stringify({ ...this.applicationToSave, badgeName: null })
          !== JSON.stringify({ ...this.originalApplication, badgeName: null });
    },
    shortcutEditable() {
      // the shortcut of a default product application is defined by the product
      // itself, so the whole option stays read only for a system application
      return !this.application?.system && this.forcedFieldsEditable;
    },
    shortcutTitle() {
      if (!this.forcedFieldsEditable) {
        return this.forcedFieldTitle;
      }
      return !this.shortcutEditable && this.$t('appCenter.userSettings.shortcuts.productShortcutNotEditable') || null;
    },
    // The server forces mandatory, default, PWA, stick and shortcut off on an
    // application created from a Shortcuts portlet, on every write: not offered
    // in that portlet's form, read only in the administration's
    forcedFieldsEditable() {
      return !this.contextual && !this.application?.contextual;
    },
    forcedFieldTitle() {
      return !this.forcedFieldsEditable && this.$t('appCenter.adminSetupForm.contextualOptionNotEditable') || null;
    },
    imageTypes() {
      return this.contextual ? this.contextualImageTypes : 'image/*';
    },
    quickActions() {
      return this.quickActionExtensions
        .filter(ext => ext.id)
        .map(ext => ({
          id: ext.id,
          icon: ext.icon,
          name: this.$te(ext.name) ? this.$t(ext.name) : ext.name,
        }))
        .sort((a, b) => a.name.localeCompare(b.name, eXo.env.portal.language, {numeric: true, sensitivity: 'base'}));
    },
    disabled() {
      if (this.personal) {
        return this.loading || !this.modified
          || !this.application?.title?.trim()?.length
          || !this.normalizedPersonalUrl;
      }
      return this.loading || !this.modified
        || !this.title?.length
        || !!(this.description?.length && this.description?.length > this.maxDescriptionLength)
        || !this.validUrl
        || !this.validHelpPageUrl
        // an emptied shortcut field is an unfinished edit, not a change: the
        // shortcut is removed by switching the option off, not by clearing it
        || (this.hasShortcut && !this.application?.shortcut?.length)
        || !!(this.application?.shortcut?.length && this.shortcutExists(this.application?.shortcut));
    },
  },
  watch: {
    uploadedImage: {
      handler() {
        this.$set(this.application, 'imageUploadId', this.uploadedImage.uploadId);
        this.$set(this.application, 'illustrationMimeType', this.uploadedImage.mimeType);
      },
      deep: true,
    },
    type(newVal, oldVal) {
      if (this.drawer && newVal && oldVal && this.application) {
        this.application.url = null;
        // The binding follows the url: leaving it behind would keep a stale
        // provider bound after switching Portlet -> Link
        this.application.badgeName = null;
        this.badgeDisabledByAdmin = null;
        this.application.allowStick = newVal === 'DRAWER' && this.forcedFieldsEditable;
        this.application.allowDetach = newVal === 'DRAWER';
      }
    },
    title(newVal) {
      if (this.drawer && this.application && !this.personal) {
        this.application.title = newVal;
      }
    },
    description(newVal) {
      if (this.drawer && this.application && !this.personal) {
        this.application.description = newVal;
      }
    },
    hasPermissions() {
      if (!this.hasPermissions && this.application) {
        this.application.permissions = null;
      }
    },
    hasHelpUrl() {
      if (!this.hasHelpUrl && this.application) {
        this.application.helpPageURL = null;
      }
    },
    hasShortcut() {
      if (!this.hasShortcut && this.application && this.shortcutEditable) {
        this.application.shortcut = null;
      }
    },
  },
  created() {
    this.$root.$on('app-center-drawer-open', this.open);
    if (!this.personal) {
      document.addEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
      this.refreshQuickActions();
      this.$utils.includeExtensions('QuickActionExtension');
    }
  },
  beforeDestroy() {
    this.$root.$off('app-center-drawer-open', this.open);
    document.removeEventListener('extension-QuickAction-Extension-updated', this.refreshQuickActions);
  },
  methods: {
    async open(app) {
      this.$root.$emit('close-alert-message');
      this.application = null;
      if (this.personal) {
        this.application = app?.id ? { ...app } : {
          icon: 'fa-link',
          imageUrl: null,
          url: null,
          sameTab: false,
          active: true,
          type: 'LINK',
          personal: true,
        };
        this.originalApplication = { ...this.application };
      } else {
        if (!this.badgeProvidersRequested) {
          // Once, when the form is first opened: the form is created on pages
          // where it may never be, and only administrators may read the providers
          this.badgeProvidersRequested = true;
          this.$applicationBadgeService.getBadgeProviders()
            .then(providers => this.badgeProviderList = providers || []);
        }
        if (app?.id && !this.contextual) {
          const data = await this.$applicationService.getApplications(true);
          this.application = data?.applications?.find?.(a => a.id === app.id);
          if (this.application) {
            // Clone to a different object
            this.application = {...this.application};
          }
        }
        if (!this.application) {
          this.application = {
            icon: 'fa-dot-circle',
            imageUrl: null,
            url: null,
            order: 1,
            sameTab: true,
            helpPageURL: null,
            shortcut: null,
            active: true,
            default: false,
            mandatory: false,
            mobile: true,
            system: false,
            type: 'LINK', // LINK, DRAWER or PORTLET
            permissions: [],
            categoryIds: [],
            allowStick: false,
            allowDetach: false,
          };
        }
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
          this.descriptions = Object.fromEntries(
            Object.entries(this.descriptions).map(([key, value]) => [
              key,
              this.$utils.htmlToText(value)
            ]));
        } else {
          this.titles = {};
          this.descriptions = {};
        }
        this.hasPermissions = !!this.application?.permissions?.length;
        this.hasHelpUrl = !!this.application?.helpPageURL?.length;
        this.hasShortcut = this.application?.shortcut?.length;
        // Cleared on every open: the drawer is a single instance, so a
        // previously edited application would otherwise carry its switch over.
        // Null means "follow what is stored", which badgeDisplayed infers.
        this.badgeDisabledByAdmin = null;
        this.originalApplication = {
          ...this.application,
          title: JSON.parse(JSON.stringify(this.titles)),
          description: JSON.parse(JSON.stringify(this.descriptions)),
          categoryIds: this.newCategoryIds,
        };
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
    // The server's rule for a Shortcuts portlet's application: an http(s) or
    // portal-relative link, of at most 500 characters once normalized
    isValidContextLink(url) {
      const normalizedUrl = this.$applicationUrlService.normalizePersonalUrl(url);
      return !!normalizedUrl && normalizedUrl.length <= this.contextualMaxUrlLength;
    },
    isContextLinkTooLong(url) {
      return this.contextual && this.$applicationUrlService.normalizePersonalUrl(url)?.length > this.contextualMaxUrlLength;
    },
    shortcutExists(c) {
      return !!this.$root.applications?.find?.(a => a.shortcut === c && a.id !== this.application?.id);
    },
    refreshQuickActions() {
      this.quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension') || [];
    },
    async save() {
      if (this.loading) {
        return;
      }
      const isNew = !this.application.id;
      this.loading = true;
      try {
        if (this.contextual) {
          await this.saveContextApplication();
        } else if (this.personal) {
          this.application.url = this.normalizedPersonalUrl;
          let app;
          if (isNew) {
            app = await this.$applicationService.createPersonalApp(this.application);
          } else {
            app = await this.$applicationService.updatePersonalApp(this.application);
          }
          this.close();
          this.$root.$emit('alert-message', this.$t(isNew ? 'appCenter.personalApp.save.success' : 'appCenter.personalApp.update.success'), 'success');
          this.$emit('saved', app, isNew);
        } else {
          if (!this.application?.permissions?.length) {
            this.application.permissions = null;
          }
          let app = this.application;
          if (isNew) {
            app = await this.$applicationService.createApplication(this.application);
          } else {
            await this.$applicationService.updateApplication(this.application);
          }
          await this.$translationService.saveTranslations('appCenter', app.id, 'title', this.titles);
          await this.$translationService.saveRichTranslations('appCenter', app.id, 'description', this.descriptions);
          await this.$applicationCategoryService.updateCategories(app.id, this.oldCategoryIds, this.newCategoryIds);
          this.$root.$emit('app-center-refresh-list');
          if (isNew) {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationCreatedSuccessfully'), 'success');
          } else {
            this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.applicationUpdatedSuccessfully'), 'success');
          }
          this.close();
        }
      } catch (e) {
        // the drawer stays open so that the user doesn't lose what was entered
        this.$root.$emit('alert-message', this.$t(this.saveErrorMessageKey(isNew, e)), 'error');
      } finally {
        this.loading = false;
      }
    },
    async saveContextApplication() {
      // Read before any wait: the form can be opened again, and emptied, meanwhile
      const {titles, descriptions} = this;
      const app = await this.$applicationService.createContextApplication(this.settingName, {
        ...this.application,
        badgeName: this.badgeNameToSave,
        categoryIds: this.newCategoryIds,
      });
      // Listed at once and the drawer closed: the application exists, so
      // nothing that follows may let a second save create it again
      this.$emit('saved', app, true);
      this.close();
      const translationsSaved = await this.$translationService.saveTranslations('appCenter', app.id, 'title', titles)
        .then(() => this.$translationService.saveRichTranslations('appCenter', app.id, 'description', descriptions))
        .then(() => true, () => false);
      // Reported by the host, which knows whether it listed the app: one message, as a message replaces the previous one
      this.$emit('creation-completed', {application: app, translationsSaved});
    },
    saveErrorMessageKey(isNew, error) {
      // A refusal of the Shortcuts portlet's endpoint names its cause with a label code
      if (this.contextual && error?.message && this.$te(error.message)) {
        return error.message;
      }
      if (!this.personal) {
        return 'appCenter.adminSetupForm.errorSavingApplication';
      }
      return isNew ? 'appCenter.personalApp.save.error' : 'appCenter.personalApp.update.error';
    },
    async deletePersonalApp() {
      if (this.loading) {
        return;
      }
      this.loading = true;
      try {
        await this.$applicationService.deletePersonalApp(this.application.id);
        this.close();
        this.$root.$emit('alert-message', this.$t('appCenter.personalApp.delete.success'), 'success');
        this.$emit('deleted', this.application);
      } catch (e) {
        this.$root.$emit('alert-message', this.$t('appCenter.personalApp.delete.error'), 'error');
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
