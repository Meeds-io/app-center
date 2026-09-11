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
  <div></div>
</template>
<script>
export default {
  data: () => ({
    triggeredDrawerApps: {},
    renderedPortletApps: {},
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
    document.addEventListener('page-layout-rendered', this.refresh);
    document.addEventListener('extension-QuickAction-Extension-updated', this.refresh);
    document.addEventListener('extension-QuickAction-PortletExtension-updated', this.refresh);
    this.$utils.includeExtensions('QuickActionExtension');
    this.refresh();
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refresh);
    document.removeEventListener('page-layout-rendered', this.refresh);
    document.removeEventListener('extension-QuickAction-Extension-updated', this.refresh);
    document.removeEventListener('extension-QuickAction-PortletExtension-updated', this.refresh);
  },
  methods: {
    refresh() {
      if (!this.stuckAllowed) {
        return;
      }
      const placements = this.$appPlacementService.getPlacements();
      if (!placements?.siteEligible) {
        return;
      }
      this.openStuckApplication(placements.left, 'left');
      this.openStuckApplication(placements.right, 'right');
    },
    openStuckApplication(application, side) {
      const anchor = document.querySelector(`#pageBody${side === 'left' && 'Left' || 'Right'}Panel`);
      if (!application) {
        this.$set(this.triggeredDrawerApps, side, null);
        this.cleanRenderedPortlet(side, anchor);
        return;
      }
      if (application.type === 'DRAWER') {
        if (this.triggeredDrawerApps[side] === application.id
            || document.querySelector(`.stuck-app-panel [data-stuck-app="${window.CSS.escape(application.url)}"]`)) {
          return;
        }
        const quickAction = extensionRegistry.loadExtensions('QuickAction', 'Extension')
          .find(extension => extension.id === application.url);
        if (quickAction?.click) {
          this.$set(this.triggeredDrawerApps, side, application.id);
          quickAction.click();
        }
      } else if (application.type === 'PORTLET' && anchor) {
        if (this.renderedPortletApps[side] === application.id) {
          return;
        }
        const portletQuickAction = extensionRegistry.loadExtensions('QuickAction', 'PortletExtension')?.[0];
        if (portletQuickAction?.render) {
          this.$set(this.renderedPortletApps, side, application.id);
          anchor.classList.add('stuck-app-panel', 'white', 'overflow-y-auto');
          const container = document.createElement('div');
          container.id = `stuckPortletPanel-${side}`;
          anchor.replaceChildren(container);
          portletQuickAction.render(application.url, `#${container.id}`);
        }
      }
    },
    cleanRenderedPortlet(side, anchor) {
      if (this.renderedPortletApps[side] && anchor) {
        anchor.replaceChildren();
        anchor.classList.remove('stuck-app-panel', 'white', 'overflow-y-auto');
        this.$set(this.renderedPortletApps, side, null);
      }
    },
  },
};
</script>
