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
    <td colspan="2" class="ps-0 py-2">
      <div class="d-flex">
        <v-card
          class="transparent"
          min-width="50"
          flat>
          <img
            v-if="item.imageFileId && item.imageFileName"
            :src="`/app-center/rest/applications/illustration/${item.id}?v=${item.imageLastModified}`"
            referrerpolicy="no-referrer"
            class="flex-grow-0 flex-shrink-0">
          <img v-else :src="defaultAppImg">
        </v-card>
        <span :title="item.title" class="tableAppTitle ms-4 text-truncate-2 d-flex align-center text-start">
          {{ displayName }}
        </span>
      </div>
    </td>
    <td class="text-center position-relative">
      <v-switch
        v-model="active"
        class="ma-0 absolute-all-center"
        hide-details
        @change="$emit('set-enabled', active)" />
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
    }
  },
  data: () => ({
    active: true,
    defaultAppImg: '/app-center/skin/images/defaultApp.png',
  }),
  computed: {
    displayName() {
      return this.item.displayName || this.item.title;
    },
  },
  created() {
    this.active = this.item.active;
  },
};
</script>
