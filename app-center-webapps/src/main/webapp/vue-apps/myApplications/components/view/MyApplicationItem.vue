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
      :max-width="$attrs.maxWidth"
      :min-height="$attrs.minHeight"
      :max-height="$attrs.maxHeight"
      link
      flat>
      <v-img
        :src="appImageUrl"
        :alt="applicationTitle"
        max-width="65"
        max-height="65"
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
    defaultAppImage: {
      type: Object,
      default: null
    }
  },
  computed: {
    appImageUrl() {
      const { imageFileId, imageFileName, id, imageLastModified } = this.application;
      return (imageFileId && imageFileName) || this.defaultAppImage?.fileBody
        ? `${this.illustrationBaseUrl}${id}?v=${imageLastModified}`
        : '/app-center/skin/images/defaultApp.png';
    },
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
