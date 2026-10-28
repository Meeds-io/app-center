<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
  <div>
    <select
      v-model="template"
      :disabled="disabled"
      :aria-label="$t('myApplications.template.label')"
      class="full-width ignore-vuetify-classes py-2 height-auto text-truncate">
      <!-- A window saved before the templates keeps its legacy layout until one is picked -->
      <option
        v-if="!display"
        value=""
        disabled>
        {{ $t('myApplications.template.placeholder') }}
      </option>
      <option
        v-for="templateName in templates"
        :key="templateName"
        :value="templateName">
        {{ $t(`myApplications.template.${templateName}`) }}
      </option>
    </select>
    <v-card
      v-if="display"
      class="mt-4 pa-2 border-box-sizing overflow-hidden"
      outlined
      flat>
      <!-- The header line as the portlet draws it: its title, then See more, whatever the template, when it is shown
           and apps are listed -->
      <div
        v-if="showHeader"
        class="d-flex align-center mb-2">
        <span class="text-header text-truncate">
          {{ headerTitle }}
        </span>
        <my-applications-see-more
          v-if="seeMorePlacement === 'HEADER' && previewApplications.length"
          class="ms-auto ps-2"
          preview />
      </div>
      <my-applications-template-list
        v-if="previewApplications.length"
        :applications="previewApplications"
        :display="display"
        :show-see-more="seeMorePlacement === 'LIST'"
        preview />
      <div v-else class="text-sub-title text-center py-2">
        {{ $t('myApplications.noApps.label') }}
      </div>
    </v-card>
    <div class="d-flex align-center justify-space-between mt-4">
      <label class="v-label text-color align-start">
        {{ $t('myApplications.showHeader.label') }}
      </label>
      <div class="align-end">
        <v-switch
          :input-value="showHeader"
          :disabled="disabled"
          color="primary"
          class="pa-0 my-auto"
          hide-details
          @change="$emit('update:show-header', $event)" />
      </div>
    </div>
    <translation-text-field
      v-if="showHeader"
      :object-id="settingName"
      :object-type="objectType"
      :field-name="fieldName"
      :field-value="headerTitle"
      :drawer-title="$t('myApplications.header.translation.title')"
      class="mt-2"
      no-expand-icon
      back-icon
      required
      @update:field-value="$emit('update:header-title', $event)"
      @input="$emit('translations-updated', $event)" />
    <my-applications-display-options
      v-if="display"
      :display="display"
      :version="version"
      :disabled="disabled"
      @update:display="$emit('update:display', $event)" />
  </div>
</template>

<script>
import {TEMPLATES, getSeeMorePlacement} from '../../myApplicationsDisplay.js';

export default {
  props: {
    showHeader: {
      type: Boolean,
      default: true,
    },
    settingName: {
      type: String,
      default: null,
    },
    objectType: {
      type: String,
      default: null,
    },
    fieldName: {
      type: String,
      default: null,
    },
    headerTitle: {
      type: String,
      default: null,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    // The template and its settings being edited, null while the legacy layout is kept
    display: {
      type: Object,
      default: null,
    },
    previewApplications: {
      type: Array,
      default: () => [],
    },
    // Changes whenever the display settings are replaced rather than edited
    version: {
      type: Number,
      default: 0,
    },
  },
  data: () => ({
    templates: TEMPLATES,
  }),
  computed: {
    seeMorePlacement() {
      return getSeeMorePlacement(this.display, this.showHeader);
    },
    template: {
      get() {
        return this.display ? this.display.template : '';
      },
      // A pick replaces every display setting by the template's own
      set(template) {
        this.$emit('select-template', template);
      },
    },
  },
};
</script>
