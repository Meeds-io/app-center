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
    v-if="!applications.length && !isLoading && !preview"
    class="d-flex flex-column justify-center align-center flex-grow-1">
    <v-icon size="60" class="secondary--text mb-2">
      fas fa-th
    </v-icon>
    <p class="mt-2 mb-0 text-sub-title">
      {{ $t('myApplications.add.application.label') }}
    </p>
  </div>
  <div
    v-else
    class="d-flex flex-column flex-grow-1 full-width">
    <!-- Sized to its apps, the group is placed by its margins: the start and the end follow the page's direction -->
    <div
      :class="groupClass"
      class="d-flex max-width-fit overflow-hidden">
      <card-carousel
        v-if="inRow"
        :parent-class="itemsClass"
        dense>
        <my-application-item
          v-for="application in applications"
          :key="application.id"
          :application="application"
          :display-settings="display"
          :preview="preview"
          class="ma-1"
          @open-portlet="$emit('open-portlet', $event)" />
        <my-applications-see-more
          v-if="showSeeMore"
          :preview="preview"
          class="ma-1" />
      </card-carousel>
      <div
        v-else
        :class="itemsClass"
        class="d-flex">
        <my-application-item
          v-for="application in applications"
          :key="application.id"
          :application="application"
          :display-settings="display"
          :preview="preview"
          :class="isColumn ? 'my-1' : 'ma-1'"
          @open-portlet="$emit('open-portlet', $event)" />
        <my-applications-see-more
          v-if="showSeeMore"
          :preview="preview"
          :class="isColumn ? 'my-1' : 'ma-1'" />
      </div>
    </div>
  </div>
</template>
<script>
const MARGINS = {
  START: {horizontal: 'me-auto', vertical: 'mb-auto'},
  CENTER: {horizontal: 'mx-auto', vertical: 'my-auto'},
  END: {horizontal: 'ms-auto', vertical: 'mt-auto'},
};
const ALIGNMENTS = {
  START: 'start',
  CENTER: 'center',
  END: 'end',
};

export default {
  props: {
    applications: {
      type: Array,
      default: () => []
    },
    // The resolved display settings: a template and each of its settings
    display: {
      type: Object,
      default: null
    },
    isLoading: {
      type: Boolean,
      default: false
    },
    preview: {
      type: Boolean,
      default: false
    },
    // 'See more' ends the list of apps
    showSeeMore: {
      type: Boolean,
      default: false
    },
  },
  computed: {
    isColumn() {
      return this.display.template === 'COLUMN';
    },
    inRow() {
      return !this.isColumn && this.display.listInRow;
    },
    hAlign() {
      return MARGINS[this.display.hAlign] ? this.display.hAlign : 'CENTER';
    },
    vAlign() {
      return MARGINS[this.display.vAlign] ? this.display.vAlign : 'CENTER';
    },
    groupClass() {
      return [MARGINS[this.hAlign].horizontal, MARGINS[this.vAlign].vertical];
    },
    itemsClass() {
      if (this.isColumn) {
        // One app per line: the apps are aligned within the column
        return `flex-column align-${ALIGNMENTS[this.hAlign]}`;
      } else if (this.inRow) {
        return `flex-nowrap align-${ALIGNMENTS[this.vAlign]}`;
      } else {
        // Wrapped lines are aligned as the group is
        return `flex-wrap justify-${ALIGNMENTS[this.hAlign]} align-${ALIGNMENTS[this.vAlign]}`;
      }
    },
  },
};
</script>
