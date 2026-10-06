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
  <v-radio-group
    :value="listingMode"
    class="mt-0 pa-0"
    hide-details
    mandatory
    @change="$emit('update:listing-mode', $event)">
    <v-radio value="FAVORITES">
      <template #label>
        <span class="ms-1 text-color">{{ $t('myApplications.listFavorites.label') }}</span>
      </template>
    </v-radio>
    <div v-if="listingMode === 'FAVORITES'" class="d-flex align-center mb-2">
      <label class="v-label text-color">
        {{ $t('myApplications.numberToList.label') }}
      </label>
      <div class="ms-auto">
        <number-input
          :value="maxAppsToList"
          :min="1"
          :max="100"
          :step="1"
          editable
          @input="$emit('update:max-apps-to-list', $event)" />
      </div>
    </div>
    <v-radio value="SELECTED">
      <template #label>
        <span class="ms-1 text-color">{{ $t('myApplications.selectApps.label') }}</span>
      </template>
    </v-radio>
  </v-radio-group>
</template>

<script>
export default {
  props: {
    listingMode: {
      type: String,
      default: 'FAVORITES',
    },
    maxAppsToList: {
      type: Number,
      default: 4,
    },
  },
};
</script>
