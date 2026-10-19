<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <v-hover
    v-if="display"
    :value="hover"
    @input="setHover">
    <v-card
      v-bind="cardBindings"
      v-on="application.type !== 'LINK' && !preview && {
        click: () => openApplication(application.type, application.url),
      }"
      :id="!preview && `App${application.id}`"
      @contextmenu="!preview && $refs.placementMenu?.open?.($event)"
      :title="applicationDescription"
      :class="cardClass"
      :style="cardStyle"
      :width="template ? null : width"
      :max-width="template ? templateMaxWidth : maxWidth"
      :min-width="template === 'CARDS' ? 114 : null"
      :height="template ? null : height"
      :max-height="template ? null : maxHeight"
      :elevation="hover && !lift ? 2 : 0"
      link
      flat
      @keydown.enter="openApplication"
      @mousedown="onMouseDown"
      @focusin="onFocusIn"
      @focusout="onFocusOut">
      <app-center-placement-menu
        v-if="!preview"
        ref="placementMenu"
        :application="application" />
      <!-- While lifted, the face is drawn over the page at the tile's place, the tile keeping its size in the line -->
      <div
        v-if="template"
        ref="face"
        :class="faceClass"
        :style="faceStyle">
        <my-application-item-content
          ref="content"
          :application="application"
          :display-settings="displaySettings"
          :preview="preview"
          :expanded="nameOpen" />
      </div>
      <my-application-item-content
        v-else
        :application="application"
        :preview="preview" />
    </v-card>
  </v-hover>
</template>
<script>
import {getLiftedTileStyle, getTileClass, getTileStyle} from '../../myApplicationsDisplay.js';

export default {
  props: {
    application: {
      type: Object,
      default: null
    },
    width: {
      type: String,
      default: null
    },
    maxWidth: {
      type: String,
      default: null
    },
    height: {
      type: String,
      default: null
    },
    maxHeight: {
      type: String,
      default: null
    },
    // The resolved display settings of a template, null for the legacy tile, left as it is
    displaySettings: {
      type: Object,
      default: null
    },
    // Displayed only, as in the settings' preview: opens nothing
    preview: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    illustrationBaseUrl: '/app-center/rest/applications/illustration/',
    loading: false,
    hover: false,
    isFocused: false,
    ignoreNextFocus: false,
    // A name that does not fit on one line opens on two while its app is hovered or focused
    nameOpen: false,
    // The place and size of the lifted tile, null while it lies in its line
    lift: null,
  }),
  computed: {
    applicationUrl() {
      return this.application?.computedUrl;
    },
    applicationUrlTarget() {
      return this.application?.target;
    },
    applicationDescription() {
      return this.application?.description;
    },
    display() {
      return this.application.type !== 'DRAWER' || this.$root.quickActions[this.application.url];
    },
    cardBindings() {
      if (this.preview) {
        return {tabindex: -1};
      } else if (this.application.type === 'LINK') {
        return {
          href: this.applicationUrl,
          target: this.applicationUrlTarget,
          rel: 'nofollow noreferrer noopener',
        };
      } else {
        return {loading: this.loading};
      }
    },
    template() {
      return this.displaySettings?.template;
    },
    templateMaxWidth() {
      return this.template === 'CARDS' ? 114 : 240;
    },
    // A Row or Cards tile is lifted instead of growing, so that the other apps of its line keep their height
    liftable() {
      return this.template === 'ROW' || this.template === 'CARDS';
    },
    cardClass() {
      return this.lift ? null : getTileClass(this.displaySettings, this.hover, this.isFocused);
    },
    cardStyle() {
      if (this.lift) {
        return {
          width: `${this.lift.width}px`,
          height: `${this.lift.height}px`,
          border: 'thin solid transparent',
          backgroundColor: 'transparent',
        };
      }
      return getTileStyle(this.displaySettings);
    },
    faceClass() {
      return this.lift ? [getTileClass(this.displaySettings, this.hover, this.isFocused), 'elevation-2'] : 'full-width';
    },
    faceStyle() {
      // At the carousel arrows' level, which come later in the page and so stay above it
      return this.lift && {
        ...getLiftedTileStyle(this.displaySettings, this.lift.ground),
        position: 'fixed',
        top: `${this.lift.top}px`,
        left: `${this.lift.left}px`,
        width: `${this.lift.width}px`,
        minHeight: `${this.lift.height}px`,
        zIndex: 1,
      };
    },
  },
  created() {
    // Where a fixed element's coordinates start, in the viewport: its corner, unless a transformed ancestor
    // such as a drawer holds the tile
    this.liftOrigin = {x: 0, y: 0};
  },
  beforeDestroy() {
    this.dropTile();
  },
  methods: {
    setHover(hover) {
      // Measured before the name opens: a name that fits keeps its line, so that its tile keeps its size
      if (hover && !this.hover && this.template) {
        this.nameOpen = this.fullyVisible() && !!this.$refs.content?.nameTruncated();
        if (this.nameOpen && this.liftable) {
          this.liftTile();
        }
      } else if (!hover) {
        this.nameOpen = false;
        this.dropTile();
      }
      this.hover = hover;
    },
    liftTile() {
      this.lift = {...this.liftedRect(), ground: this.groundColor()};
      window.addEventListener('scroll', this.followTile, true);
      window.addEventListener('resize', this.followTile);
      // Ordered after the render: the face is measured where it was drawn
      this.$nextTick(this.followTile);
    },
    dropTile() {
      if (this.lift) {
        this.lift = null;
        window.removeEventListener('scroll', this.followTile, true);
        window.removeEventListener('resize', this.followTile);
      }
    },
    liftedRect() {
      const rect = this.$el.getBoundingClientRect();
      return {
        top: rect.top - this.liftOrigin.y,
        left: rect.left - this.liftOrigin.x,
        width: rect.width,
        height: rect.height,
      };
    },
    // Places the lifted face on its tile again, once drawn and whenever the page or the line scrolls (scroll
    // events come once per frame). The origin is read from where the face is drawn: a transformed ancestor
    // moves it as the page scrolls
    followTile() {
      if (this.lift) {
        const face = this.$refs.face.getBoundingClientRect();
        this.liftOrigin = {x: face.left - this.lift.left, y: face.top - this.lift.top};
        this.lift = {...this.liftedRect(), ground: this.lift.ground};
      }
    },
    // A tile cut by the edge of what holds it, as a scrolled carousel, is not opened: its face would come out of it
    fullyVisible() {
      const tile = this.$el.getBoundingClientRect();
      for (let element = this.$el.parentElement; element && element !== document.body; element = element.parentElement) {
        const style = window.getComputedStyle(element);
        if (style.overflowX !== 'visible' || style.overflowY !== 'visible') {
          const box = element.getBoundingClientRect();
          if (tile.left < box.left - 0.5 || tile.right > box.right + 0.5 || tile.top < box.top - 0.5 || tile.bottom > box.bottom + 0.5) {
            return false;
          }
        }
      }
      return true;
    },
    // The color the tile lies on: its first ancestor painted with an opaque one, white over an image or nothing
    groundColor() {
      for (let element = this.$el.parentElement; element; element = element.parentElement) {
        const color = window.getComputedStyle(element).backgroundColor;
        if (color.startsWith('rgb(')) {
          return color;
        }
      }
      return '#FFFFFF';
    },
    async openApplication(event) {
      if (this.preview) {
        return;
      }
      this.onFocusOut(event);
      const appType = this.application.type;
      const appUrl = this.application.url;
      if (appType !== 'LINK'
          && await this.$appPlacementService.alertWhenStuck(appType, appUrl, this.$t('appCenter.placement.alreadyStuck'))) {
        return;
      }
      if (appType === 'PORTLET') {
        this.appLoading = appUrl;
        try {
          this.$emit('open-portlet', appUrl);
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
      } else if (appType === 'DRAWER' && this.$root.quickActions[appUrl]) {
        this.loading = true;
        try {
          await this.$root.quickActions[appUrl].click();
        } finally {
          window.setTimeout(() => this.loading = false, 500);
        }
      }
    },
    onFocusOut(event) {
      if (!this.$el.contains(event.relatedTarget)) {
        this.setHover(false);
        this.isFocused = false;
      }
    },
    onFocusIn() {
      if (this.ignoreNextFocus) {
        this.isFocused = false;
        this.ignoreNextFocus = false;
      } else {
        this.isFocused = true;
      }
      this.setHover(true);
    },
    onMouseDown() {
      this.ignoreNextFocus = true;
      setTimeout(() => {
        this.ignoreNextFocus = false;
      }, 100);
    },
  },
};
</script>
