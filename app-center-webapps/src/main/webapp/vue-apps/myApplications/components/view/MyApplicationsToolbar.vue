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
    <!-- Holds the cog's height while the cog lies out of the flow: the line does not move when it shows -->
    <div
      :style="showCog && 'min-height: 28px'"
      class="d-flex align-center position-relative ms-auto">
      <template v-if="hasApplications && showSeeMore">
        <my-applications-see-more
          v-if="!seeMoreIconDisplayed" />
        <!-- Drawn as the Links portlet draws it on hover -->
        <v-btn
          v-else
          ref="btnSeeMoreIcon"
          :loading="loading === 'seeMoreIcon'"
          :title="$t('myApplications.seeMore.tooltip')"
          class="ms-2 text-font-size"
          small
          link
          outlined
          icon
          @click="openApplications('seeMoreIcon')">
          <v-icon
            :size="18"
            class="primary--text">
            fas fa-external-link-alt
          </v-icon>
        </v-btn>
      </template>
      <!-- The cog shows with the portlet's hover state, as in the platform's other widgets. Until then it lies hidden
           over the line's end, out of the flow: 'see more' ends the line, and the cog keeps its place in the tab order -->
      <my-applications-cog
        v-if="showCog"
        :displayed="hover"
        :class="hover ? 'ms-2' : 'position-absolute'"
        :style="!hover && 'inset-inline-end: 0'"
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
    // With the portlet's hover state, an editor sees an icon in place of 'see more', next to the cog, as in the
    // platform's other widgets
    seeMoreIconDisplayed() {
      return this.hover && this.canEdit;
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