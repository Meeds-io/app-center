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

<template>
  <div
    :class="contentClass"
    class="d-flex align-center full-width">
    <app-center-badge
      v-if="!preview && !cornerBadge"
      :badge-name="application.badgeName"
      :class="horizontal ? 'align-self-start' : 'mx-auto'"
      top-spacing="-5px"
      x-spacing="12px" />
    <!-- With no icon, or one too narrow to carry it, the badge lies on the tile's top end corner, over its edges, so
         that it never covers the name: the corner of the tile, or of its lifted face, whichever is drawn -->
    <div
      v-else-if="!preview"
      class="position-absolute t-0"
      style="inset-inline-end: 0">
      <app-center-badge
        :badge-name="application.badgeName"
        :x-spacing="cornerBadgeXSpacing"
        top-spacing="-5px" />
    </div>
    <div
      v-if="iconDisplayed"
      :style="iconBoxStyle"
      class="d-flex align-center justify-center flex-shrink-0">
      <v-img
        v-if="application.imageUrl"
        :src="application.imageUrl"
        :class="iconClass"
        :max-width="iconPixels"
        :max-height="iconPixels"
        referrerpolicy="no-referrer"
        contain />
      <v-icon
        v-else-if="application.icon"
        :size="iconPixels"
        class="d-flex align-center justify-center">
        {{ application.icon }}
      </v-icon>
      <v-img
        v-else
        :class="iconClass"
        :max-width="iconPixels"
        :max-height="iconPixels"
        src="/app-center/skin/images/defaultApp.png"
        contain
        referrerpolicy="no-referrer" />
    </div>
    <v-tooltip
      v-if="nameDisplayed"
      bottom>
      <template #activator="{ on, attrs }">
        <span
          ref="name"
          :class="[nameClass, nameColorClass]"
          v-bind="attrs"
          v-on="on">
          {{ application.title }}
        </span>
      </template>
      <span>{{ application.title }}</span>
    </v-tooltip>
  </div>
</template>
<script>
import {isFilledTile} from '../../myApplicationsDisplay.js';

const LINE_ALIGNMENTS = {
  START: 'justify-start',
  CENTER: 'justify-center',
  END: 'justify-end',
};

export default {
  props: {
    application: {
      type: Object,
      default: null
    },
    // The resolved display settings of a template, null for the legacy tile
    displaySettings: {
      type: Object,
      default: null
    },
    preview: {
      type: Boolean,
      default: false
    },
    // The name opens on two lines at most: its app is hovered or focused, it does not fit on one, and, in Row
    // or Cards, the app is wholly in view
    expanded: {
      type: Boolean,
      default: false
    },
    // Drawn over the page as the page's menus are
    lifted: {
      type: Boolean,
      default: false
    },
  },
  computed: {
    // Lifted, a tile with no fill of its own lies on the ground of the page's menus, so its name takes their text
    // color, the dark grey of their buttons
    nameColorClass() {
      return this.lifted && !isFilledTile(this.displaySettings) ? 'dark-grey-color' : 'text-color';
    },
    template() {
      return this.displaySettings?.template;
    },
    horizontal() {
      return this.template === 'ROW' || this.template === 'COLUMN';
    },
    // A column's app spans its line, and its icon and name are placed in it as Horizontal alignment says
    contentClass() {
      if (this.template === 'COLUMN') {
        return ['flex-row', LINE_ALIGNMENTS[this.displaySettings.hAlign] || 'justify-start'];
      }
      return this.horizontal ? 'flex-row' : 'flex-column justify-center';
    },
    iconDisplayed() {
      return !this.template || this.displaySettings.showIcon;
    },
    nameDisplayed() {
      return !this.template || this.displaySettings.showName;
    },
    iconPixels() {
      return this.template ? this.displaySettings.iconSize : 45;
    },
    // In Row and Column, a glyph narrower than its size, or an image smaller than it, is centered in a box of the
    // icon's size, so that the name never starts before the size plus its 8px margin
    iconBoxStyle() {
      return this.horizontal && {minWidth: `${this.iconPixels}px`};
    },
    // On the icon's corner, the 20px badge spans 12px to 32px past the icon's start: in Row and Column, an icon
    // under 24px would leave it over the name
    cornerBadge() {
      return !this.iconDisplayed || (this.horizontal && this.iconPixels < 24);
    },
    // Past the tile's 1px border, the 20px badge goes 4px over its top and its end, the room the tile's margin leaves
    // before the list clips. A column's app spans the list, which clips at its end: its badge ends on the tile's end
    cornerBadgeXSpacing() {
      return this.template === 'COLUMN' ? '-19px' : '-15px';
    },
    iconClass() {
      return this.horizontal ? 'flex-grow-0' : 'mx-auto';
    },
    nameClass() {
      if (!this.template) {
        return ['text-truncate-2', this.iconDisplayed && 'mt-2'];
      }
      // One line, and the whole name on two lines at most while the app is hovered or focused
      const lines = this.expanded ? 'text-truncate-2' : 'text-truncate';
      if (this.horizontal) {
        // With the badge on the tile's corner, every name of a column ends before the badge's room on its tile's end
        return [lines, this.iconDisplayed && 'ms-2', this.cornerBadge && this.template === 'COLUMN' && 'me-3'];
      }
      // Centered in a column, a single line is as wide as its text unless bounded by the tile
      return [lines, 'max-width-fit', this.iconDisplayed && 'mt-2'];
    },
  },
  methods: {
    nameTruncated() {
      const name = this.$refs.name;
      return !!name && name.scrollWidth > name.clientWidth;
    },
  },
};
</script>
