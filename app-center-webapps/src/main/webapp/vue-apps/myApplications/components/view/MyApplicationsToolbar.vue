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
  <div class="d-flex align-center justify-space-between">
    <div
      v-if="showHeader"
      class="widget-text-header align-start">
      {{ headerLabel }}
    </div>
    <div v-else class="flex-grow-1"></div>
    <div
      class="d-flex align-center ms-auto">
      <template v-if="hasApplications">
        <v-btn
          v-if="!hover || !isAdmin"
          :loading="loading === 'seeMore'"
          color="primary"
          class="pa-0 text-font-size"
          small
          text
          link
          @click="openApplications('seeMore')">
          <span class="primary--text text-none">
            {{ $t('myApplications.seeMore.label') }}
          </span>
        </v-btn>
        <v-tooltip
          v-else
          bottom>
          <template #activator="{ on, attrs }">
            <v-btn
              v-bind="attrs"
              v-on="on"
              :loading="loading === 'seeMoreIcon'"
              color="primary"
              class="ms-2 text-font-size"
              small
              link
              icon
              @click="openApplications('seeMoreIcon')">
              <v-icon
                :size="18"
                class="icon-default-size">
                fas fa-external-link-alt
              </v-icon>
            </v-btn>
          </template>
          {{ $t('myApplications.seeMore.tooltip') }}
        </v-tooltip>
      </template>
      <v-tooltip
        v-if="hover && isAdmin"
        bottom>
        <template #activator="{ on, attrs }">
          <v-btn
            v-bind="attrs"
            v-on="on"
            class="ms-2 text-font-size"
            small
            link
            icon
            @click="$emit('open-settings')">
            <v-icon
              :size="18"
              class="icon-default-size icon-default-color">
              fas fa-cog
            </v-icon>
          </v-btn>
        </template>
        {{ $t('myApplications.editSettings.tooltip') }}
      </v-tooltip>
      <v-tooltip
        v-if="!hasApplications"
        bottom>
        <template #activator="{ on, attrs }">
          <v-btn
            v-bind="attrs"
            v-on="on"
            :loading="loading === 'addAppIcon'"
            class="ms-2 text-font-size"
            small
            link
            icon
            @click="openApplications('seeMoreIcon')">
            <v-icon
              :size="18"
              class="icon-default-color icon-default-size">
              fas fa-plus
            </v-icon>
          </v-btn>
        </template>
        {{ $t('myApplications.add.application.tooltip') }}
      </v-tooltip>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    hasApplications: {
      type: Boolean,
      default: false
    },
    hover: {
      type: Boolean,
      default: false
    },
    isAdmin: {
      type: Boolean,
      default: false
    },
    headerTitle: {
      type: String,
      default: null
    },
    showHeader: {
      type: Boolean,
      default: true
    }
  },
  data: () => ({
    loading: false,
    applicationsDrawer: false,
  }),
  computed: {
    headerLabel() {
      return this.headerTitle || this.$t('myApplications.name.label');
    },
  },
  methods: {
    openApplications(app) {
      this.loading = app;
      this.$nextTick()
        .then(() => {
          document.dispatchEvent(new CustomEvent('CustomEventOpenApplicationLauncherDrawer'));
        })
        .finally(() => this.loading = null);
    },
  },
};
</script>
