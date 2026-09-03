<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
-->
<template>
  <v-app class="transparent">
    <div
      v-if="error"
      class="d-flex align-center justify-center pa-10 text-h6">
      {{ $t('appCenter.standalone.notAvailable') }}
    </div>
    <div v-else id="appStandaloneViewerContent"></div>
  </v-app>
</template>
<script>
export default {
  props: {
    applicationId: {
      type: Number,
      default: null,
    },
  },
  data: () => ({
    error: false,
    displayed: false,
    resolving: false,
  }),
  created() {
    document.addEventListener('extension-QuickAction-Extension-updated', this.display);
    document.addEventListener('extension-QuickAction-PortletExtension-updated', this.display);
    this.$utils.includeExtensions('QuickActionExtension');
    this.display();
  },
  beforeDestroy() {
    document.removeEventListener('extension-QuickAction-Extension-updated', this.display);
    document.removeEventListener('extension-QuickAction-PortletExtension-updated', this.display);
  },
  methods: {
    async display() {
      if (this.displayed || this.resolving) {
        return;
      }
      if (!this.applicationId) {
        this.error = true;
        return;
      }
      this.resolving = true;
      try {
        const application = await this.$appPlacementService.findApplicationById(this.applicationId).catch(() => null);
        if (!application?.allowDetach) {
          this.error = true;
          return;
        }
        document.title = application.title || document.title;
        if (application.type === 'DRAWER') {
          const quickAction = extensionRegistry.loadExtensions('QuickAction', 'Extension')
            .find(extension => extension.id === application.url);
          if (quickAction?.click) {
            this.displayed = true;
            eXo.env.portal.standaloneAppName = application.url;
            await quickAction.click();
          }
        } else if (application.type === 'PORTLET') {
          const portletQuickAction = extensionRegistry.loadExtensions('QuickAction', 'PortletExtension')?.[0];
          if (portletQuickAction?.render) {
            this.displayed = true;
            await portletQuickAction.render(application.url, '#appStandaloneViewerContent');
          }
        } else {
          this.error = true;
        }
      } finally {
        this.resolving = false;
      }
    },
  },
};
</script>
