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
      v-on="!$root.isMobile && {
        end: onDragEnd
      }"
      :is="$root.isMobile && 'div' || 'draggable'"
      v-model="applicationsList"
      item-key="id"
      class="d-flex flex-wrap flex-grow-1">
      <my-application-item
        v-for="application in applicationsList"
        :key="application.id"
        :application="application"
        :width="114"
        :max-width="114"
        :height="132"
        :max-height="132"
        class="d-flex mt-5 me-1"
        @open-portlet="$emit('open-portlet', $event)" />
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
    isLoading: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    onDragEnd() {
      this.$emit('list-updated', this.applicationsList);
    }
  }
};
</script>

