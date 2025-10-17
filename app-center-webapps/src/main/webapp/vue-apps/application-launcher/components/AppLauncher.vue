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
  <v-app flat>
    <v-container
      v-if="!$root.hideApp"
      px-0
      py-0>
      <v-layout class="transparent">
        <v-btn
          id="appcenterLauncherButton"
          :title="tooltip"
          icon
          class="text-xs-center"
          @click="$refs.appDrawer.open()">
          <v-icon class="appCenterLauncherButtonIcon" size="20">
            fa-th
          </v-icon>
        </v-btn>
      </v-layout>
    </v-container>
    <app-center-launcher-drawer
      ref="appDrawer"
      :app-loading="appLoading"
      @open-app="openApplication"
      @apps-loaded="applications = $event" />
    <app-center-portlet-instance-drawer
      ref="portletInstanceDrawer" />
  </v-app>
</template>
<script>
export default {
  data: () => ({
    applications: null,
    appLoading: null,
  }),
  computed: {
    tooltip() {
      return `${this.$t('appCenter.appLauncher.topbarIcon.tooltip')} ${this.$t('appCenter.appLauncher.topbarIcon.tooltip.shortcut')}`;
    },
  },
  created() {
    if (this.$root.shortcuts?.length) {
      window.addEventListener('keydown', this.openApplicationByShortcutEvent);
      this.$utils.removeShortcutsListener(this.$root.shortcuts);
    }
  },
  mounted() {
    if (this.$root.shortcut && this.$root.shortcut?.toLowerCase?.() !== 'a') {
      this.openApplicationByShortcut(this.$root.shortcut);
    } else if (!this.$root.noAutoOpen || this.$root.shortcut?.toLowerCase?.() === 'a') {
      this.openDrawer();
    } else if (this.$root.autoInitDrawerId) {
      this.openApplication('DRAWER', this.$root.autoInitDrawerId);
    } else if (this.$root.autoInitPortletId) {
      this.openApplication('PORTLET', this.$root.autoInitPortletId);
    }
  },
  beforeDestroy() {
    window.removeEventListener('keydown', this.openApplicationByShortcutEvent);
  },
  methods: {
    openDrawer() {
      this.$refs.appDrawer.open();
    },
    openApplicationByShortcutEvent(e) {
      if (e.ctrlKey
          && e.shiftKey
          && e.key) {
        if (e.key?.toLowerCase?.() === 'a') {
          this.openDrawer();
        } else if (this.$root.shortcuts?.includes?.(e.key.toLowerCase())) {
          window.setTimeout(() => this.openApplicationByShortcut(e.key), 10);
        } else {
          return;
        }
        e.stopPropagation();
        e.preventDefault();
      }
    },
    async openApplicationByShortcut(shortcut) {
      if (!shortcut || !this.$root.shortcuts?.includes?.(shortcut.toLowerCase())) {
        return;
      }
      if (!this.applications?.length) {
        await this.$refs.appDrawer.init();
        await this.$nextTick();
      }
      const application = this.applications.find(a => a.shortcut && a.shortcut.toLowerCase() === shortcut.toLowerCase());
      if (application?.type === 'LINK') {
        const computedUrl = application.url
          .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`)
          .replace('@user@', eXo.env.portal.userName);
        const url = this.$utils.toLinkUrl(computedUrl, {
          urls: true,
          email: true,
          phone: true,
        });
        if (application.sameTab) {
          if (url?.startsWith?.('/')) {
            window.location.href = `${window.location.origin}${url}`;
          } else {
            window.location.href = url;
          }
        } else  if (url?.startsWith?.('/')) {
          window.open(`${window.location.origin}${url}`);
        } else {
          window.open(url);
        }
      } else {
        this.openApplication(application?.type, application?.url);
      }
    },
    async openApplication(appType, appUrl) {
      if (appType === 'PORTLET') {
        this.appLoading = appUrl;
        try {
          if (this.$refs.portletInstanceDrawer) {
            await this.$refs.portletInstanceDrawer.open(appUrl);
          } else {
            const interval = window.setInterval(() => {
              if (this.$refs.portletInstanceDrawer) {
                window.clearInterval(interval);
                this.$refs.portletInstanceDrawer.open(appUrl);
              }
            }, 200);
          }
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
      } else if (appType === 'DRAWER' && this.$root.quickActions[appUrl]) {
        this.appLoading = appUrl;
        try {
          await this.$root.quickActions[appUrl].click();
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
      }
    },
  },
};
</script>
