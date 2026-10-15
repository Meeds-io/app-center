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
      <div
        :class="horizontal ? 'flex-row' : 'flex-column justify-center'"
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
              :class="nameClass"
              class="text-color"
              v-bind="attrs"
              v-on="on">
              {{ applicationTitle }}
            </span>
          </template>
          <span>{{ applicationTitle }}</span>
        </v-tooltip>
      </div>
    </v-card>
  </v-hover>
</template>
<script>
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
    // ROW, CARDS or COLUMN, null for the legacy tile, which the props below leave as it is
    template: {
      type: String,
      default: null
    },
    showIcon: {
      type: Boolean,
      default: true
    },
    iconSize: {
      type: Number,
      default: 45
    },
    showName: {
      type: Boolean,
      default: true
    },
    outlined: {
      type: Boolean,
      default: false
    },
    // Empty for the skin's light grey
    color: {
      type: String,
      default: ''
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
    applicationTitle() {
      return this.application?.title;
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
    horizontal() {
      return this.template === 'ROW' || this.template === 'COLUMN';
    },
    iconDisplayed() {
      return !this.template || this.showIcon;
    },
    nameDisplayed() {
      return !this.template || this.showName;
    },
    iconPixels() {
      return this.template ? this.iconSize : 45;
    },
    iconClass() {
      return this.horizontal ? 'flex-grow-0 flex-shrink-0' : 'mx-auto';
    },
    nameClass() {
      if (!this.horizontal) {
        return ['text-truncate-2', this.iconDisplayed && 'mt-2'];
      }
      return ['text-truncate', this.iconDisplayed && 'ms-2'];
    },
    templateMaxWidth() {
      return this.template === 'CARDS' ? 114 : 240;
    },
    cardClass() {
      if (!this.template) {
        return {
          'border-color-black': this.isFocused,
          'transparent': !this.hover,
          'light-grey-background-color': this.hover,
        };
      }
      return {
        'rounded-pill px-4 py-2': this.template === 'ROW',
        'pa-2': this.template === 'CARDS',
        'px-2 py-1': this.template === 'COLUMN',
        'border-color-black': this.isFocused,
        // Outlined: no fill. Filled: the color, else the skin's light grey
        'transparent': this.outlined,
        'light-grey-background-color': !this.outlined && !this.color,
      };
    },
    cardStyle() {
      if (!this.template) {
        return null;
      } else if (this.outlined) {
        return {border: `thin solid ${this.color || 'var(--allPagesBorderColor)'}`};
      } else {
        return this.color && {backgroundColor: this.color} || null;
      }
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
