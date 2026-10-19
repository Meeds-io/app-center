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
      class="text-header align-start">
      <span class="text-truncate">{{ headerLabel }}</span>
    </div>
    <div v-else class="flex-grow-1"></div>
    <div
      class="d-flex align-center ms-auto">
      <template v-if="hasApplications && showSeeMore">
        <my-applications-see-more
          v-if="!seeMoreIconDisplayed" />
        <v-btn
          v-else
          ref="btnSeeMoreIcon"
          :loading="loading === 'seeMoreIcon'"
          :title="$t('myApplications.seeMore.tooltip')"
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
      <!-- The legacy layout adds the cog on hover, a template keeps its room -->
      <my-applications-cog
        v-if="showCog && (hover || !legacy)"
        :displayed="hover"
        class="ms-2"
        @open-settings="$emit('open-settings')" />
      <v-btn
        v-if="!hasApplications && favorites"
        :title="$t('myApplications.add.application.tooltip')"
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
    canEdit: {
      type: Boolean,
      default: false
    },
    // The settings cog is on this line
    showCog: {
      type: Boolean,
      default: false
    },
    // No template: the layout kept until one is picked
    legacy: {
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
    },
    showSeeMore: {
      type: Boolean,
      default: true
    },
    favorites: {
      type: Boolean,
      default: true
    },
  },
  data: () => ({
    loading: false,
    applicationsDrawer: false,
  }),
  computed: {
    headerLabel() {
      return this.headerTitle || this.$t('myApplications.name.label');
    },
    // On hover, the legacy layout shows an editor an icon in place of 'see more'. A template keeps 'see more'
    seeMoreIconDisplayed() {
      return this.legacy && this.hover && this.canEdit;
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
    }
  },
};
</script>