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
    <v-icon
      v-if="application?.icon && !application?.imageUrl"
      size="20"
      class="d-flex align-center justify-center">
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
  computed: {
    computedUrl() {
      if (this.application?.type === 'LINK') {
        const computedUrl = this.application.url
          .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`)
          .replace('@user@', eXo.env.portal.userName);
        return this.$utils.toLinkUrl(computedUrl, {
          urls: true,
          email: true,
          phone: true,
        });
      } else {
        return null;
      }
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
