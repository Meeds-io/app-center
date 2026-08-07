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
      v-bind="application.type === 'LINK' && {
        href: applicationUrl,
        target: applicationUrlTarget,
        rel: 'nofollow noreferrer noopener',
      } || {
        loading,
      }"
      v-on="application.type !== 'LINK' && {
        click: () => openApplication(application.type, application.url),
      }"
      :id="`App${application.id}`"
      :title="applicationDescription"
      :class="[
        $attrs.class,
        {
          'border-color': card,
          'border-color-black': isFocused,
          'transparent': !hover,
          'light-grey-background-color': hover,
        }
      ]"
      :width="width"
      :max-width="maxWidth"
      :height="height"
      :max-height="maxHeight"
      :elevation="hover ? 2 : 0"
      link
      flat
      @keydown.enter="openApplication"
      @mousedown="onMouseDown"
      @focusin="onFocusIn"
      @focusout="onFocusOut">
      <app-center-badge :badge-name="application.badgeName" absolute />
      <div class="d-flex flex-column justify-center align-center full-width">
        <v-img
          v-if="application.imageUrl"
          :src="application.imageUrl"
          referrerpolicy="no-referrer"
          class="mx-auto"
          max-width="45"
          max-height="45"
          contain />
        <v-icon
          v-else-if="application.icon"
          size="45"
          class="d-flex align-center justify-center">
          {{ application.icon }}
        </v-icon>
        <v-img
          v-else
          src="/app-center/skin/images/defaultApp.png"
          max-width="45"
          max-height="45"
          contain
          class="mx-auto"
          referrerpolicy="no-referrer" />
        <v-tooltip
          bottom>
          <template #activator="{ on, attrs }">
            <span
              class="text-truncate-2 mt-2 text-color"
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
  },
  methods: {
    async openApplication(event) {
      this.onFocusOut(event);
      const appType = this.application.type;
      const appUrl = this.application.url;
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
