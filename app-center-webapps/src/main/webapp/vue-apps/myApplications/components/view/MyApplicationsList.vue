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
  <div
    v-if="!applicationsList.length && !isLoading"
    class="d-flex flex-column justify-center align-center flex-grow-1">
    <v-icon size="60" class="secondary--text mb-2">
      fas fa-th
    </v-icon>
    <p class="mt-2 mb-0 text-sub-title">
      {{ $t('myApplications.add.application.label') }}
    </p>
  </div>
  <v-layout
    v-else>
    <component
      :is="isMobile && 'div' || 'draggable'"
      v-model="applicationsList"
      :item-key="'id'"
      class="d-flex flex-wrap flex-grow-0 justify-start"
      v-bind="isMobile ? {} : {
        onStart: onDragStart,
        onEnd: onDragEnd
      }">
      <my-application-item
        v-for="application in applicationsList"
        :key="application.id"
        :application="application"
        :default-app-image="defaultAppImage"
        :width="125"
        :max-width="125"
        :min-height="125"
        :max-height="150"
        class="d-flex ma-0 flex-grow-1" />
    </component>
  </v-layout>
</template>

<script>
export default {
  props: {
    applicationsList: {
      type: Array,
      default: () => []
    },
    defaultAppImage: {
      type: Object,
      default: null
    },
    isLoading: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    isMobile() {
      return this.$vuetify.breakpoint.smAndDown;
    }
  },
  methods: {
    onDragEnd() {
      this.$emit('list-updated', this.applicationsList);
    }
  }
};
</script>

