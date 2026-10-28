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
    ref="line"
    class="d-flex flex-column flex-grow-1 full-width">
    <!-- Sized to its apps, the group is placed by its margins: the start and the end follow the page's direction. A
         column spans the portlet's width, and ends before the cog laid over the portlet's top end while it shows -->
    <div
      :class="groupClass"
      class="d-flex max-width-fit overflow-hidden position-relative">
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
          v-if="seeMoreDisplayed"
          :preview="preview"
          class="ma-1 align-self-center" />
      </card-carousel>
      <div
        v-else
        ref="items"
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
          v-if="seeMoreDisplayed"
          :preview="preview"
          :class="seeMoreClass" />
        <!-- Shown over a list that needs its room, it ends the last line, or opens a line of its own when that one is
             full -->
        <my-applications-cog
          v-if="cogDisplayed"
          ref="wrappedCog"
          :displayed="hover"
          :class="cogOverLine ? 'absolute-vertical-center' : 'ma-1 align-self-center'"
          :style="cogOverLine && 'inset-inline-end: 0'"
          @focused="cogFocused = $event"
          @open-settings="$emit('open-settings')" />
      </div>
      <!-- After the carousel rather than in it, so that it is reached however far the line is scrolled -->
      <my-applications-cog
        v-if="inRow && cogDisplayed"
        :displayed="hover"
        :class="cogOverLine ? 'absolute-vertical-center' : 'ma-1 align-self-center'"
        :style="cogOverLine && 'inset-inline-end: 0'"
        @focused="cogFocused = $event"
        @open-settings="$emit('open-settings')" />
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
// The room the cog takes after the apps: its 28px and its margins
const COG_ROOM = 36;

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
    // The settings cog ends the list of apps, after 'see more'
    showCog: {
      type: Boolean,
      default: false
    },
    // The portlet's hover state, which the pointer and the keyboard focus both set: it displays the cog
    hover: {
      type: Boolean,
      default: false
    },
    // The settings cog laid over the portlet's top end shows: the apps end before it
    topCogShown: {
      type: Boolean,
      default: false
    },
  },
  data: () => ({
    // The apps of a Row or Cards list do not fit on one line along with the cog's room
    lineFull: false,
    // The cog holds the focus, which shows it whatever the hover
    cogFocused: false,
  }),
  computed: {
    // 'See more' ends a list of apps, so none while the apps load
    seeMoreDisplayed() {
      return this.showSeeMore && this.applications.length > 0;
    },
    cogDisplayed() {
      return this.showCog && this.applications.length > 0;
    },
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
      if (this.isColumn) {
        // The room of the cog and its gap, the cog being 28px wide
        return [MARGINS[this.vAlign].vertical, 'full-width', this.topCogShown && 'pe-8'];
      }
      return [MARGINS[this.hAlign].horizontal, MARGINS[this.vAlign].vertical];
    },
    itemsClass() {
      if (this.isColumn) {
        // One app per line, as wide as the column: each app is aligned within its line
        return 'flex-column align-stretch full-width';
      } else if (this.inRow) {
        // The apps of a line share one height, the tallest one's: an app whose name opens on two lines is lifted
        // over the line instead of growing it
        return 'flex-nowrap align-stretch';
      } else {
        // Wrapped lines are aligned as the group is, and the apps of a line share one height
        return `flex-wrap justify-${ALIGNMENTS[this.hAlign]} align-stretch`;
      }
    },
    // In a column, placed in its line as the apps are
    seeMoreClass() {
      return this.isColumn ? `my-1 align-self-${ALIGNMENTS[this.hAlign]}` : 'ma-1 align-self-center';
    },
    // The cog of a Row or Cards list whose apps fit on one line along with it keeps its room after them, so that
    // nothing moves when it shows. Where they need that room, the hidden cog lies out of the flow over the group's end,
    // so that they take the whole line or keep their lines, and takes its room when it shows: the carousel narrows, or
    // the wrapped lines make room for it
    observedList() {
      if (this.isColumn || !this.cogDisplayed) {
        return null;
      }
      return this.inRow ? 'CAROUSEL' : 'WRAPPED';
    },
    cogOverLine() {
      return this.lineFull && !this.hover && !this.cogFocused;
    },
  },
  watch: {
    observedList() {
      // After the render: the list to observe is the one the new mode renders
      this.$nextTick(this.observeLine);
    },
  },
  mounted() {
    this.observeLine();
  },
  beforeDestroy() {
    this.lineObserver?.disconnect();
  },
  methods: {
    observeLine() {
      this.lineObserver?.disconnect();
      this.lineObserver = null;
      this.lineFull = false;
      const apps = this.appsElement();
      if (apps && window.ResizeObserver) {
        // The line's width and the apps', whichever way the cog lies
        this.lineObserver = new ResizeObserver(this.computeLineFull);
        this.lineObserver.observe(this.$refs.line);
        this.lineObserver.observe(apps);
      }
    },
    computeLineFull() {
      const appsWidth = this.appsWidth();
      // Fractional widths, as the lines wrap on them
      this.lineFull = appsWidth !== null && appsWidth + COG_ROOM > this.$refs.line.getBoundingClientRect().width;
    },
    // What holds the apps: the carousel's scrolled content, or the wrapped lines
    appsElement() {
      if (this.observedList === 'CAROUSEL') {
        // The row of social's CardCarousel.vue that holds the slotted apps
        return this.$refs.line?.querySelector('.carousel-last-parent');
      }
      return this.observedList === 'WRAPPED' ? this.$refs.items : null;
    },
    // The width of the apps and 'see more' on one line with their margins, but the cog's, whether they overflow the
    // carousel or not
    appsWidth() {
      const apps = this.appsElement();
      if (!apps) {
        return null;
      }
      const cog = this.$refs.wrappedCog?.$el;
      return Array.from(apps.children)
        .filter(child => child !== cog)
        .reduce((width, child) => {
          const style = window.getComputedStyle(child);
          return width + child.getBoundingClientRect().width + parseFloat(style.marginLeft) + parseFloat(style.marginRight);
        }, 0);
    },
  },
};
</script>
