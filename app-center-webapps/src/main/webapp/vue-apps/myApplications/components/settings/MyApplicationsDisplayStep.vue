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
    <div class="d-flex align-center justify-space-between">
      <label class="v-label text-color align-start">
        {{ $t('myApplications.addHeader.label') }}
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
  </div>
</template>

<script>
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
  },
};
</script>
