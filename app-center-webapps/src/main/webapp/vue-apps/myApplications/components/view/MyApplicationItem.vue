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
  <v-hover v-slot="{hover}">
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
      :class="{'background-grey-primary': hover}"
      :width="width"
      :max-width="maxWidth"
      :min-height="minHeight"
      :max-height="maxHeight"
      link
      flat>
      <div class="pa-4 d-flex flex-column align-center full-width">
        <v-img
          v-if="application.imageUrl"
          :src="application.imageUrl"
          :alt="applicationTitle"
          referrerpolicy="no-referrer"
          class="mx-auto"
          max-width="60"
          max-height="60"
          contain />
        <v-icon
          v-else-if="application.icon"
          size="60"
          class="d-flex align-center justify-center">
          {{ application.icon }}
        </v-icon>
        <v-img
          v-else
          :alt="applicationTitle"
          src="/app-center/skin/images/defaultApp.png"
          max-width="60"
          max-height="60"
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
    minHeight: {
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
    }
  },
  methods: {
    async openApplication() {
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
  },
};
</script>
