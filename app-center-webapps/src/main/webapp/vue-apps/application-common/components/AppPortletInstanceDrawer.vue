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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    :loading="loading"
    allow-expand
    @closed="$emit('closed')">
    <template #title>
      {{ applicationTitle }}
    </template>
    <template #content>
      <div v-if="drawer">
        <div id="appLauncherPortletViewer"></div>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    loading: false,
    applicationTitle: null,
    portletInstanceQuickAction: null,
  }),
  created() {
    document.addEventListener('extension-QuickAction-PortletExtension-updated', this.refreshQuickActions);
    this.refreshQuickActions();
    this.$utils.includeExtensions('QuickActionExtension');
  },
  beforeDestroy() {
    document.removeEventListener('extension-QuickAction-PortletExtension-updated', this.refreshQuickActions);
  },
  methods: {
    async open(portletInstanceId) {
      this.drawer = false;
      this.applicationTitle = null;
      await this.$nextTick();
      this.drawer = true;
      this.loading = true;
      this.applicationTitle = await this.portletInstanceQuickAction.getName(portletInstanceId);
      await this.$nextTick();
      window.setTimeout(async () => {
        await this.portletInstanceQuickAction.render(portletInstanceId, '#appLauncherPortletViewer');
        window.setTimeout(() => this.loading = false, 300);
      }, 200);
    },
    refreshQuickActions() {
      const portletInstanceQuickActions = extensionRegistry.loadExtensions('QuickAction', 'PortletExtension');
      this.portletInstanceQuickAction = portletInstanceQuickActions?.[0];
    },
  },
};
</script>