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
  <v-hover v-if="display" v-model="hover">
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
      :elevation="hover ? 2 : 0"
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
      <my-application-item-content
        :application="application"
        :display-settings="displaySettings"
        :preview="preview"
        :expanded="hover" />
    </v-card>
  </v-hover>
</template>
<script>
import {getTileClass, getTileStyle} from '../../myApplicationsDisplay.js';

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
    cardClass() {
      return getTileClass(this.displaySettings, this.hover, this.isFocused);
    },
    cardStyle() {
      return getTileStyle(this.displaySettings);
    },
  },
  methods: {
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
        this.hover = false;
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
      this.hover = true;
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
