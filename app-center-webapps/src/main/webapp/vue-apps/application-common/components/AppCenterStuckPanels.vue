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
      this.refresh();
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
      const placements = this.stuckAllowed && this.$appPlacementService.getPlacements() || null;
      const eligible = placements?.siteEligible && placements || null;
      this.placeStuckApplication(eligible?.left || null, 'left');
      this.placeStuckApplication(eligible?.right || null, 'right');
      this.applySiteOffsets();
    },
    placeStuckApplication(application, side) {
      if (!application) {
        this.$set(this.triggeredDrawerApps, side, null);
        this.releaseAnchor(side);
        return;
      }
      const anchor = this.ensureAnchor(side);
      if (application.type === 'DRAWER') {
        this.cleanRenderedPortlet(side, anchor);
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
          // a drawer docked on this side hands the anchor back before the
          // portlet takes it: its wrapper finishes the cleanup on the same
          // placement event
          const dockedDrawer = this.anchorContent(anchor).querySelector('[data-stuck-app]');
          if (dockedDrawer) {
            document.querySelector('#vuetify-apps')?.appendChild(dockedDrawer);
          }
          this.$set(this.renderedPortletApps, side, application.id);
          // a docked drawer brings its own page-side border: the bare
          // portlet panel draws the same separation line itself
          anchor.style[side === 'left' && 'borderRight' || 'borderLeft'] = '1px solid rgba(0, 0, 0, 0.12)';
          // same header idiom as the exo-drawer: v-list-item structure,
          // text-title title, icon actions, then the divider separator —
          // header stays put while only the portlet content scrolls
          const header = document.createElement('div');
          header.className = 'drawerHeader flex-grow-0';
          const listItem = document.createElement('div');
          listItem.className = 'v-list-item px-0 theme--light';
          const content = document.createElement('div');
          content.className = 'v-list-item__content drawerTitle align-start text-title ps-4';
          const title = document.createElement('div');
          title.className = 'text-truncate full-width';
          title.textContent = application.title || '';
          content.appendChild(title);
          const actions = document.createElement('div');
          actions.className = 'v-list-item__action drawerIcons align-end d-flex flex-row pe-3';
          const unstickButton = document.createElement('button');
          unstickButton.type = 'button';
          unstickButton.title = this.$t && this.$t('appCenter.placement.unstick') || 'Unstick';
          unstickButton.className = 'v-btn v-btn--flat v-btn--icon v-btn--round theme--light v-size--default';
          unstickButton.innerHTML = '<span class="v-btn__content"><i aria-hidden="true" class="v-icon notranslate fas fa-thumbtack icon-default-color" style="font-size: 18px;"></i></span>';
          unstickButton.addEventListener('click', () => this.$appPlacementService.unstickApplication(side));
          actions.appendChild(unstickButton);
          listItem.append(content, actions);
          header.appendChild(listItem);
          const divider = document.createElement('hr');
          divider.setAttribute('role', 'separator');
          divider.setAttribute('aria-orientation', 'horizontal');
          divider.className = 'v-divider theme--light my-0 flex-grow-0';
          const container = document.createElement('div');
          container.id = `stuckPortletPanel-${side}`;
          container.className = 'flex-grow-1 overflow-y-auto';
          container.style.minHeight = '0';
          this.anchorContent(anchor).replaceChildren(header, divider, container);
          portletQuickAction.render(application.url, `#${container.id}`);
        }
      }
    },
    ensureAnchor(side) {
      // the panel lives beside the whole site column (topbar included), as a
      // fixed full height band: the site container shrinks by the same width
      // so the topbar visually ends where the panel begins
      const host = document.querySelector('#ParentSiteRightContainer');
      if (!host) {
        return null;
      }
      const anchorId = `pageBody${side === 'left' && 'Left' || 'Right'}Panel`;
      let anchor = document.querySelector(`#${anchorId}`);
      if (!anchor) {
        anchor = document.createElement('div');
        anchor.id = anchorId;
        host.appendChild(anchor);
      }
      // the anchor lives outside any Vue application root: it carries the
      // Vuetify scoping classes itself so the docked content keeps its skin
      anchor.classList.add('stuck-app-panel');
      anchor.style.position = 'fixed';
      // border-box keeps the page-side border inside the 420px: on a
      // content-box the extra pixel slides under the topbar, which paints
      // over it at topbar height
      anchor.style.boxSizing = 'border-box';
      anchor.style.top = '0';
      anchor.style.bottom = '0';
      anchor.style[side] = '0';
      anchor.style.width = '420px';
      anchor.style.zIndex = '0';
      if (!anchor.querySelector('.v-application--wrap')) {
        // the anchor lives outside any Vue application root: it rebuilds the
        // Vuetify scoping structure so the docked content keeps its skin
        const vuetifyApp = document.createElement('div');
        vuetifyApp.className = 'VuetifyApp full-height';
        const application = document.createElement('div');
        application.className = `v-application ${document.dir === 'rtl' && 'v-application--is-rtl' || 'v-application--is-ltr'} theme--light full-height`;
        // the platform skin forces v-application transparent with !important:
        // the panel paints its own white ground at the same priority so a
        // short application never shows the page through
        application.style.setProperty('background-color', 'white', 'important');
        const wrap = document.createElement('div');
        wrap.className = 'v-application--wrap full-height';
        application.appendChild(wrap);
        vuetifyApp.appendChild(application);
        anchor.replaceChildren(vuetifyApp);
      }
      return anchor;
    },
    anchorContent(anchor) {
      return anchor?.querySelector('.v-application--wrap') || anchor;
    },
    releaseAnchor(side) {
      const anchor = document.querySelector(`#pageBody${side === 'left' && 'Left' || 'Right'}Panel`);
      this.cleanRenderedPortlet(side, anchor);
      if (anchor) {
        // the drawer wrapper moves its shell out on the same event: remove
        // the anchor only once it holds no docked application any more
        window.requestAnimationFrame(() => {
          if (!anchor.querySelector('[data-stuck-app]') && !this.anchorContent(anchor).childElementCount) {
            anchor.remove();
            this.applySiteOffsets();
          }
        });
      }
    },
    applySiteOffsets() {
      const container = document.querySelector('#ParentSiteContainerChildren');
      if (!container) {
        return;
      }
      const leftWidth = document.querySelector('#pageBodyLeftPanel')?.style?.width || null;
      const rightWidth = document.querySelector('#pageBodyRightPanel')?.style?.width || null;
      if (leftWidth || rightWidth) {
        const width = `calc(100% - ${leftWidth || '0px'} - ${rightWidth || '0px'})`;
        container.style.left = leftWidth || '';
        container.style.width = width;
        // the skin clamps the container with min/max-width: 100%
        container.style.minWidth = width;
        container.style.maxWidth = width;
      } else {
        container.style.removeProperty('left');
        container.style.removeProperty('width');
        container.style.removeProperty('min-width');
        container.style.removeProperty('max-width');
      }
    },
    cleanRenderedPortlet(side, anchor) {
      if (this.renderedPortletApps[side] && anchor) {
        this.anchorContent(anchor).replaceChildren();
        anchor.style.removeProperty('border-left');
        anchor.style.removeProperty('border-right');
        this.$set(this.renderedPortletApps, side, null);
      }
    },
  },
};
</script>
