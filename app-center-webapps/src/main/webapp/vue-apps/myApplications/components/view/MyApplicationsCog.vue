<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
  <!-- Hidden, it keeps its room, so that nothing moves when the portlet is hovered -->
  <v-btn
    :title="$t('myApplications.editSettings.tooltip')"
    :aria-label="$t('myApplications.editSettings.tooltip')"
    :class="!displayed && 'invisible'"
    class="text-font-size"
    small
    link
    icon
    @click="$emit('open-settings')">
    <v-icon
      :size="18"
      class="icon-default-size icon-default-color">
      fas fa-cog
    </v-icon>
  </v-btn>
</template>
<script>
export default {
  props: {
    // The settings cog is displayed on hover only
    displayed: {
      type: Boolean,
      default: false
    },
  },
};
</script>
