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
      v-if="!preview"
      :badge-name="application.badgeName"
      :class="horizontal ? 'align-self-start' : 'mx-auto'"
      top-spacing="-5px"
      x-spacing="12px" />
    <template v-if="iconDisplayed">
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
        :class="horizontal && 'flex-shrink-0'"
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
    </template>
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
    iconClass() {
      return this.horizontal ? 'flex-grow-0 flex-shrink-0' : 'mx-auto';
    },
    nameClass() {
      if (!this.template) {
        return ['text-truncate-2', this.iconDisplayed && 'mt-2'];
      }
      // One line, and the whole name on two lines at most while the app is hovered or focused
      const lines = this.expanded ? 'text-truncate-2' : 'text-truncate';
      if (this.horizontal) {
        return [lines, this.iconDisplayed && 'ms-2'];
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
