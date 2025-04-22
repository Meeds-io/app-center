<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->
<template>
  <div class="d-flex flex-column flex-md-row align-center">
    <v-card
      class="d-flex align-center align-self-start mb-2 mb-md-0"
      min-width="fit-content"
      width="380"
      max-width="30%"
      flat>
      <v-avatar
        class="d-flex align-center justify-center flex-grow-0 flex-shrink-0 me-3"
        size="22"
        tile>
        <v-img
          v-if="application.imageUrl"
          :src="application.imageUrl"
          max-height="20"
          max-width="20"
          contain />
        <v-icon
          v-else-if="application.icon"
          size="20"
          class="d-flex align-center justify-center">
          {{ application.icon }}
        </v-icon>
      </v-avatar>
      {{ application.title }}
    </v-card>
    <app-center-shortcut
      v-if="application.shortcut"
      :shortcut="application.shortcut"
      class="align-self-md-center align-self-end mb-2 mb-md-0" />
  </div>
</template>
<script>
export default {
  props: {
    application: {
      type: String,
      default: null,
    },
  },
};
</script>