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
  <v-app v-if="hasPinnedApps">
    <v-btn
      v-for="application in $root.pinnedApplications"
      :key="application.id"
      v-bind="application?.type === 'LINK' && !readonly && {
        href: computedUrl,
        target: target,
        rel: 'nofollow noreferrer noopener',
      }"
      v-on="application?.type !== 'LINK' && {
        click: () => openApplication(application),
      }"
      :title="application?.title"
      :loading="loading[application?.id]"
      icon>
      <v-icon
        v-if="application?.icon && !application?.imageUrl"
        size="20"
        class="d-flex align-center justify-center">
        {{ application.icon }}
      </v-icon>
      <v-card
        v-else
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
    </v-btn>
    <app-center-portlet-instance-drawer
      v-if="openPortletDrawer"
      ref="portletInstanceDrawer" />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    loading: {},
    openPortletDrawer: false,
  }),
  computed: {
    hasPinnedApps() {
      return this.$root.pinnedApplications?.length && !this.$root.isMobile;
    },
  },
  methods: {
    async openApplication(application) {
      this.$set(this.loading, application.id, true);
      try {
        const appType = this.result.type;
        const appUrl = this.result.url;
        if (appType === 'PORTLET') {
          this.openPortletDrawer = true;
          await this.$nextTick();
          await this.$refs?.portletInstanceDrawer?.open?.(appUrl);
        } else if (appType === 'DRAWER') {
          await this.$root.quickActions[appUrl].click();
        }
      } finally {
        window.setTimeout(() => this.$set(this.loading, application.id, false), 200);
      }
    },
  },
};
</script>
