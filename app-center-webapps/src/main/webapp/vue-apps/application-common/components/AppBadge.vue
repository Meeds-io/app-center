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
  <div v-if="display" class="position-relative z-index-two">
    <!-- An addon may replace the default rendering for its own application -->
    <extension-registry-components
      v-if="hasExtension"
      :params="params"
      name="AppCenterAppBadge"
      type="badge"
      strict-type />
    <v-badge
      v-else
      :aria-label="ariaLabel"
      :content="displayedCount"
      :style="heightStyle"
      class="badge-display position-absolute"
      color="var(--allPagesBadgePrimaryColor, #d32a2a)"
      overlap
      dense
      flat />
  </div>
</template>
<script>
export default {
  props: {
    badgeName: {
      type: String,
      default: null,
    },
    size: {
      type: Number,
      default: () => 20,
    },
    topSpacing: {
      type: String,
      default: () => '-19px',
    },
    xSpacing: {
      type: String,
      default: () => '-3px',
    },
  },
  data: () => ({
    hasExtension: false,
  }),
  computed: {
    // Read straight from the shared observable rather than mirrored into local
    // state: the store already updates every mounted badge at once, and there
    // is no per-mount listener to register — nor to forget to remove
    count() {
      return this.$applicationBadgeService.getBadge(this.badgeName);
    },
    heightStyle() {
      return {
        '--badge-x-spacing': this.xSpacing,
        '--badge-top-spacing': this.topSpacing,
        '--badge-min-width': `${this.size}px`,
        '--badge-height': `${this.size}px`,
      };
    },
    display() {
      return !!this.badgeName && (this.hasExtension || this.count > 0);
    },
    displayedCount() {
      return this.count > 99 ? '99+' : this.count;
    },
    // The count must reach assistive technologies, never colour alone
    ariaLabel() {
      return this.$t('appCenter.badge.unreadItems', [this.count]);
    },
    params() {
      return {
        badgeName: this.badgeName,
        topSpacing: this.topSpacing,
        xSpacing: this.xSpacing,
        count: this.count,
        size: this.size,
      };
    },
  },
  created() {
    if (!this.badgeName) {
      return;
    }
    this.refreshExtension();
    // An addon may register its rendering after this component mounted — the
    // override point would otherwise only ever be honoured for extensions
    // registered before the first badge appeared
    document.addEventListener('extension-AppCenterAppBadge-updated', this.refreshExtension);
    this.$applicationBadgeService.init();
    this.$applicationBadgeService.loadBadge(this.badgeName);
  },
  beforeDestroy() {
    document.removeEventListener('extension-AppCenterAppBadge-updated', this.refreshExtension);
  },
  methods: {
    refreshExtension() {
      this.hasExtension = !!extensionRegistry.loadComponents('AppCenterAppBadge')
        ?.filter(component => component.componentName === 'badge'
                              && component.componentOptions?.badgeName === this.badgeName)?.length;
    },
  },
};
</script>
