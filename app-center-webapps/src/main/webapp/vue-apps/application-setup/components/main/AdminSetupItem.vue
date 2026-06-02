<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
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
  <tr>
    <td colspan="2" class="px-1 py-2">
      <div class="d-flex">
        <v-card
          class="transparent d-flex align-center justify-center"
          min-width="30"
          flat>
          <app-center-icon
            :icon-url="item.imageUrl"
            :icon="item.icon"
            class="flex-grow-0 flex-shrink-0" />
        </v-card>
        <span :title="item.title" class="tableAppTitle ms-2 text-truncate-2 d-flex align-center text-start">
          {{ displayName }}
        </span>
      </div>
    </td>
    <td class="pa-0" width="88px">
      <div class="d-flex justify-center">
        <v-card
          class="d-flex"
          width="88"
          flat>
          <v-btn
            v-if="index > 0"
            :title="$t('appCenter.adminSetupList.moveUp')"
            :loading="movingUp"
            class="ms-1 me-auto"
            icon
            @click="$emit('move-up')">
            <v-icon size="20">fa-arrow-up</v-icon>
          </v-btn>
          <v-btn
            v-if="index < (length - 1)"
            :title="$t('appCenter.adminSetupList.moveDown')"
            :loading="movingDown"
            class="me-1 ms-auto"
            icon
            @click="$emit('move-down')">
            <v-icon size="20">fa-arrow-down</v-icon>
          </v-btn>
        </v-card>
      </div>
    </td>
    <td class="text-center position-relative">
      <v-switch
        v-model="item.active"
        class="ma-0 pa-0 absolute-all-center"
        hide-details
        @change="$emit('set-enabled', $event)" />
    </td>
    <td class="text-center">
      <app-center-admin-menu
        :item="item"
        class="ma-auto"
        @edit="$emit('edit')"
        @remove="$emit('remove')" />
    </td>
  </tr>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      default: null,
    },
    index: {
      type: Number,
      default: () => 0,
    },
    length: {
      type: Number,
      default: () => 0,
    },
    movingUp: {
      type: Boolean,
      default: false,
    },
    movingDown: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    movingUpIndex: -1,
    movingDownIndex: -1,
    defaultAppImg: '/app-center/skin/images/defaultApp.png',
  }),
  computed: {
    displayName() {
      return this.item.displayName || this.item.title;
    },
  },
};
</script>
