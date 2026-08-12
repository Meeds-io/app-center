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
  <v-btn
    v-if="!hidden"
    v-bind="application?.type === 'LINK' && !readonly && {
      href: computedUrl,
      target: target,
      rel: 'nofollow noreferrer noopener',
    }"
    v-on="application?.type !== 'LINK' && {
      click: openApplication,
    }"
    :title="titleTooltip"
    :loading="loading"
    icon>
    <app-center-badge
      :badge-name="application.badgeName"
      top-spacing="-20px"
      x-spacing="9px" />
    <v-icon
      v-if="application?.icon && !application?.imageUrl"
      size="20"
      class="d-flex align-center justify-center line-height-normal">
      {{ application.icon }}
    </v-icon>
    <v-card
      v-else
      class="d-flex align-center justify-center"
      color="transparent"
      height="20"
      width="20"
      flat>
      <img
        v-if="application?.imageUrl"
        :src="application?.imageUrl"
        class="max-width-fit max-height-fit"
        height="auto"
        width="auto"
        alt="">
    </v-card>
    <app-center-portlet-instance-drawer
      v-if="portletDrawer"
      ref="portletInstanceDrawer" />
  </v-btn>
</template>
<script>
export default {
  props: {
    application: {
      type: Object,
      default: null,
    },
    hidden: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    loading: false,
    portletDrawer: false,
  }),
  watch: {
    application: {
      immediate: true,
      handler() {
        this.registerAsTopbarDisplayed();
      },
    },
    hidden() {
      this.registerAsTopbarDisplayed();
    },
  },
  computed: {
    computedUrl() {
      return this.$applicationUrlService.computeApplicationUrl(this.application);
    },
    target() {
      return this.application?.sameTab ? '_self' : '_blank';
    },
    appType() {
      return this.application?.type;
    },
    appUrl() {
      return this.application?.url;
    },
    titleTooltip() {
      return this.application?.shortcut ? `${this.application.title} (${this.$t('appCenter.adminSetupForm.ctrl')} + ${this.$t('appCenter.adminSetupForm.shift')} + ${this.application.shortcut})` : this.application?.title;
    },
  },
  methods: {
    registerAsTopbarDisplayed() {
      if (this.hidden || !this.application?.url) {
        return;
      }
      // Announces that this application is displayed in the topbar by the
      // administrator, so the Application Center disables pinning it: pinned,
      // the same icon would appear twice
      const topbarDisplayedApps = eXo.env.portal.topbarDisplayedApps = eXo.env.portal.topbarDisplayedApps || [];
      if (!topbarDisplayedApps.includes(this.application.url)) {
        topbarDisplayedApps.push(this.application.url);
        // Unlike the JSP registrations, this one runs after Vue apps mounted —
        // the application resolves asynchronously from the catalog — so late
        // readers holding a snapshot are told to refresh it
        document.dispatchEvent(new CustomEvent('topbar-displayed-apps-updated'));
      }
    },
    async openApplication() {
      if (this.appType === 'PORTLET') {
        this.loading = true;
        try {
          this.portletDrawer = true;
          await this.$nextTick();
          await this.$refs.portletInstanceDrawer.open(this.appUrl);
        } finally {
          window.setTimeout(() => this.loading = false, 500);
        }
      } else if (this.appType === 'DRAWER' && this.$root.quickActions[this.appUrl]) {
        this.loading = true;
        try {
          await this.$root.quickActions[this.appUrl].click();
        } finally {
          window.setTimeout(() => this.loading = false, 500);
        }
      }
    },
  },
};
</script>
