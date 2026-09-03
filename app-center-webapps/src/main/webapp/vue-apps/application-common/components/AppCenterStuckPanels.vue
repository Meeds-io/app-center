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
  <div>
    <app-center-portlet-instance-drawer
      v-if="leftPortletApp"
      ref="leftPortletPanel" />
    <app-center-portlet-instance-drawer
      v-if="rightPortletApp"
      ref="rightPortletPanel" />
  </div>
</template>
<script>
export default {
  data: () => ({
    leftPortletApp: null,
    rightPortletApp: null,
  }),
  created() {
    document.addEventListener('app-placement-changed', this.refresh);
    this.refresh();
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refresh);
  },
  methods: {
    refresh() {
      if (this.$vuetify?.breakpoint?.smAndDown) {
        return;
      }
      this.$appPlacementService.getPlacements(true)
        .then(placements => {
          if (!placements?.enabled || !placements?.siteEligible) {
            return;
          }
          this.openStuckApplication(placements.left, 'left');
          this.openStuckApplication(placements.right, 'right');
        });
    },
    openStuckApplication(applicationId, side) {
      if (!applicationId) {
        this[`${side}PortletApp`] = null;
        return;
      }
      this.$appPlacementService.findApplicationById(applicationId)
        .then(application => {
          if (!application) {
            return null;
          } else if (application.type === 'DRAWER') {
            const quickAction = extensionRegistry.loadExtensions('QuickAction', 'Extension')
              .find(extension => extension.id === application.url);
            return quickAction?.click?.();
          } else if (application.type === 'PORTLET') {
            this[`${side}PortletApp`] = application;
            return this.$nextTick().then(() => this.$refs[`${side}PortletPanel`]?.open?.(application.url));
          }
          return null;
        });
    },
  },
};
</script>
