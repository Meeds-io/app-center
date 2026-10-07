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
    <v-radio-group
      v-if="listingMode === 'SELECTED'"
      :value="selectionMode"
      class="mt-0 ms-8 pa-0"
      hide-details
      mandatory
      @change="$emit('update:selection-mode', $event)">
      <v-radio value="MANUAL">
        <template #label>
          <span class="ms-1 text-color">{{ $t('myApplications.pickApps.label') }}</span>
        </template>
      </v-radio>
      <template v-if="selectionMode === 'MANUAL'">
        <my-applications-application-suggester
          :setting-name="settingName"
          :selected-ids="applicationIds"
          :disabled="loading || capReached"
          :cap-reached="capReached"
          :max-applications="maxApplications"
          class="mb-2"
          @add="add" />
        <div
          v-for="(application, index) in applications"
          :key="application.id"
          class="d-flex align-center mb-1">
          <span class="text-truncate text-color">{{ application.title }}</span>
          <div class="d-flex flex-shrink-0 ms-auto">
            <v-btn
              :style="!index && 'visibility: hidden'"
              :disabled="loading || !index"
              :title="$t('myApplications.moveUp.tooltip')"
              :aria-label="$t('myApplications.moveUp.tooltip')"
              icon
              small
              @click="move(index, -1)">
              <v-icon size="16" class="icon-default-color">fas fa-arrow-up</v-icon>
            </v-btn>
            <v-btn
              :style="index === applications.length - 1 && 'visibility: hidden'"
              :disabled="loading || index === applications.length - 1"
              :title="$t('myApplications.moveDown.tooltip')"
              :aria-label="$t('myApplications.moveDown.tooltip')"
              icon
              small
              @click="move(index, 1)">
              <v-icon size="16" class="icon-default-color">fas fa-arrow-down</v-icon>
            </v-btn>
            <v-btn
              :title="$t('myApplications.removeApp.tooltip')"
              :disabled="loading"
              :aria-label="$t('myApplications.removeApp.tooltip')"
              icon
              small
              @click="remove(index)">
              <v-icon size="16" class="icon-default-color">fas fa-times</v-icon>
            </v-btn>
          </div>
        </div>
      </template>
      <v-radio value="CATEGORY">
        <template #label>
          <span class="ms-1 text-color">{{ $t('myApplications.categoryApps.label') }}</span>
        </template>
      </v-radio>
    </v-radio-group>
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
    selectionMode: {
      type: String,
      default: 'MANUAL',
    },
    applications: {
      type: Array,
      default: () => [],
    },
    settingName: {
      type: String,
      default: null,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    hiddenCount: {
      type: Number,
      default: 0,
    },
  },
  data: () => ({
    maxApplications: 100,
  }),
  computed: {
    applicationIds() {
      return this.applications.map(application => application.id);
    },
    capReached() {
      return this.applications.length + this.hiddenCount >= this.maxApplications;
    },
  },
  methods: {
    add(application) {
      if (!this.capReached && !this.applicationIds.includes(application.id)) {
        this.$emit('update:applications', [...this.applications, application]);
      }
    },
    move(index, offset) {
      const applications = [...this.applications];
      applications.splice(index + offset, 0, applications.splice(index, 1)[0]);
      this.$emit('update:applications', applications);
    },
    remove(index) {
      const applications = [...this.applications];
      applications.splice(index, 1);
      this.$emit('update:applications', applications);
    },
  },
};
</script>
