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
      :id="`App${application.id}`"
      :title="applicationDescription"
      :href="applicationUrl"
      :target="applicationUrlTarget"
      :class="{'background-grey-primary': hover}"
      class="pa-4 d-flex flex-column align-center"
      :width="$attrs.width"
      :max-width="$attrs['max-width']"
      :min-height="$attrs['min-height']"
      :max-height="$attrs['max-height']"
      link
      flat>
      <v-img
        v-if="application.imageUrl"
        :src="application.imageUrl"
        :alt="applicationTitle"
        max-width="60"
        max-height="60"
        contain
        class="mx-auto"
        referrerpolicy="no-referrer" />
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
    </v-card>
  </v-hover>
</template>

<script>
export default {
  data() {
    return {
      illustrationBaseUrl: '/app-center/rest/applications/illustration/'
    };
  },
  props: {
    application: {
      type: Object,
      default: null
    },
  },
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
  }
};
</script>
