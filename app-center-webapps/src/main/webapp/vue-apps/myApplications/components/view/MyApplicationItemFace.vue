<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

<!-- The content of a template's tile. While lifted, it is drawn over the page at the tile's place, the tile
     keeping its size in the line: the tile is this element's parent -->
<template>
  <div :class="faceClass" :style="faceStyle">
    <my-application-item-content
      ref="content"
      :application="application"
      :display-settings="displaySettings"
      :preview="preview"
      :expanded="nameOpen"
      :lifted="!!lift" />
  </div>
</template>
<script>
import {getLiftedTileStyle, getTileClass} from '../../myApplicationsDisplay.js';

export default {
  props: {
    application: {
      type: Object,
      default: null
    },
    // The resolved display settings of a template
    displaySettings: {
      type: Object,
      default: null
    },
    preview: {
      type: Boolean,
      default: false
    },
    focused: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    // A name that does not fit on one line opens on two while its app is hovered or focused
    nameOpen: false,
    // The place and size of the lifted face, null while it lies in its tile
    lift: null,
  }),
  computed: {
    // A Row or Cards tile is lifted instead of growing, so that the other apps of its line keep their height
    liftable() {
      return this.displaySettings.template === 'ROW' || this.displaySettings.template === 'CARDS';
    },
    faceClass() {
      return this.lift ? [getTileClass(this.displaySettings, true, this.focused), 'elevation-2'] : 'full-width';
    },
    faceStyle() {
      // At the carousel arrows' level, which come later in the page and so stay above it
      return this.lift && {
        ...getLiftedTileStyle(this.displaySettings),
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
    this.close();
  },
  methods: {
    // Called when the tile gets hovered or focused, measured before the name opens: a name that fits keeps its
    // line, so that its tile keeps its size
    open() {
      if (!this.$refs.content.nameTruncated()) {
        return;
      } else if (this.liftable) {
        window.addEventListener('scroll', this.follow, true);
        window.addEventListener('resize', this.follow);
        this.follow();
      } else {
        this.nameOpen = true;
      }
    },
    close() {
      this.nameOpen = false;
      this.setLift(null);
      window.removeEventListener('scroll', this.follow, true);
      window.removeEventListener('resize', this.follow);
    },
    // While the tile is hovered, and on every scroll and resize (scroll events come once per frame): lifted with
    // its name open when wholly in view, put back in its line with its name closed when cut by the edge of what
    // holds it, as a scrolled carousel, out of which its face would come
    follow() {
      if (!this.whollyInView()) {
        this.nameOpen = false;
        this.setLift(null);
      } else if (!this.lift) {
        // Measured while the name is on one line, then placed again once drawn
        this.setLift(this.liftedRect());
        this.nameOpen = true;
        this.$nextTick(this.place);
      } else {
        this.place();
      }
    },
    // The origin is read from where the face is drawn: a transformed ancestor moves it as the page scrolls
    place() {
      if (this.lift) {
        const face = this.$el.getBoundingClientRect();
        this.liftOrigin = {x: face.left - this.lift.left, y: face.top - this.lift.top};
        this.setLift(this.liftedRect());
      }
    },
    setLift(lift) {
      this.lift = lift;
      this.$emit('lift', lift);
    },
    liftedRect() {
      const rect = this.$el.parentElement.getBoundingClientRect();
      return {
        top: rect.top - this.liftOrigin.y,
        left: rect.left - this.liftOrigin.x,
        width: rect.width,
        height: rect.height,
      };
    },
    whollyInView() {
      const tile = this.$el.parentElement.getBoundingClientRect();
      for (let element = this.$el.parentElement.parentElement; element && element !== document.body; element = element.parentElement) {
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
  },
};
</script>
