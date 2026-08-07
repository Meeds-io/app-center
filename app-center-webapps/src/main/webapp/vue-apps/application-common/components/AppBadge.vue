<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

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
  <!-- No badge at all when the application carries none, or when nothing is unread -->
  <div v-if="display" :class="positionClass">
    <!-- An addon may replace the default rendering for its own application -->
    <extension-registry-components
      v-if="hasExtension"
      :params="params"
      name="AppCenterAppBadge"
      type="badge"
      strict-type />
    <v-chip
      v-else
      :aria-label="ariaLabel"
      color="error-color-background"
      min-width="22"
      height="22"
      dark>
      {{ displayedCount }}
    </v-chip>
  </div>
</template>
<script>
export default {
  props: {
    // Badge identifier resolved server side; null when the app carries none
    badgeName: {
      type: String,
      default: null,
    },
    // Overlays the badge on the top-end corner of the tile it decorates
    absolute: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    hasExtension: false,
  }),
  computed: {
    count() {
      return this.$applicationBadgeService.badges[this.badgeName] || 0;
    },
    display() {
      return !!this.badgeName && this.count > 0;
    },
    displayedCount() {
      return this.count > 99 ? '99+' : this.count;
    },
    positionClass() {
      return this.absolute ? 'position-absolute t-0 z-index-two mt-1 ms-1' : 'd-flex align-center';
    },
    // The count must reach assistive technologies, never colour alone
    ariaLabel() {
      return this.$t('appCenter.badge.unreadItems', [this.count]);
    },
    params() {
      return {
        badgeName: this.badgeName,
        count: this.count,
      };
    },
  },
  created() {
    if (!this.badgeName) {
      return;
    }
    this.hasExtension = !!extensionRegistry.loadComponents('AppCenterAppBadge')
      ?.filter(component => component.componentName === 'badge')?.length;
    this.$applicationBadgeService.init();
    this.$applicationBadgeService.loadBadge(this.badgeName);
  },
};
</script>
