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
    triggeredDrawerApps: {},
  }),
  computed: {
    stuckAllowed() {
      return (this.$vuetify?.breakpoint?.width || 0) >= (this.$vuetify?.breakpoint?.thresholds?.lg || 1264);
    },
  },
  watch: {
    stuckAllowed() {
      if (this.stuckAllowed) {
        this.refresh();
      }
    },
  },
  created() {
    document.addEventListener('app-placement-changed', this.refresh);
    document.addEventListener('extension-QuickAction-Extension-updated', this.refresh);
    document.addEventListener('extension-QuickAction-PortletExtension-updated', this.refresh);
    this.$utils.includeExtensions('QuickActionExtension');
    this.refresh();
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refresh);
    document.removeEventListener('extension-QuickAction-Extension-updated', this.refresh);
    document.removeEventListener('extension-QuickAction-PortletExtension-updated', this.refresh);
  },
  methods: {
    refresh() {
      if (!this.stuckAllowed) {
        return;
      }
      this.$appPlacementService.getPlacements(true)
        .then(placements => {
          if (!placements?.enabled || !placements?.siteEligible) {
            return;
          }
          this.openStuckApplication(placements.left, 'left');
          this.openStuckApplication(placements.right, 'right');
        })
        .catch(() => null);
    },
    openStuckApplication(applicationId, side) {
      if (!applicationId) {
        this[`${side}PortletApp`] = null;
        this.$set(this.triggeredDrawerApps, side, null);
        return;
      }
      this.$appPlacementService.findApplicationById(applicationId)
        .then(application => {
          if (!application) {
            return null;
          } else if (application.type === 'DRAWER') {
            if (this.triggeredDrawerApps[side] === application.id
                || document.querySelector(`.stuck-app-container [data-stuck-app="${window.CSS.escape(application.url)}"]`)) {
              return null;
            }
            const quickAction = extensionRegistry.loadExtensions('QuickAction', 'Extension')
              .find(extension => extension.id === application.url);
            if (quickAction?.click) {
              this.$set(this.triggeredDrawerApps, side, application.id);
              return quickAction.click();
            }
            return null;
          } else if (application.type === 'PORTLET') {
            if (this[`${side}PortletApp`]?.id === application.id) {
              return null;
            }
            this[`${side}PortletApp`] = application;
            return this.$nextTick().then(() => this.$refs[`${side}PortletPanel`]?.open?.(application.url));
          }
          return null;
        });
    },
  },
};
</script>
