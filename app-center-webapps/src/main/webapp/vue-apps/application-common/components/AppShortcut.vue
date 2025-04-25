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
  <div class="d-flex align-md-center content-box-sizing">
    <v-card
      height="20"
      width="36"
      class="text-subtitle-font-size grey-lighten1-background white--text d-flex align-center justify-center line-height-normal"
      flat>
      {{ $t('appCenter.adminSetupForm.ctrl') }}
    </v-card>
    <v-icon
      size="12"
      class="mx-2">
      fa-plus
    </v-icon>
    <v-card
      height="20"
      width="36"
      class="text-subtitle-font-size grey-lighten1-background white--text d-flex align-center justify-center line-height-normal"
      flat>
      {{ $t('appCenter.adminSetupForm.shift') }}
    </v-card>
    <v-icon
      size="12"
      class="mx-2">
      fa-plus
    </v-icon>
    <v-card
      height="20"
      width="20"
      class="text-subtitle-font-size grey-lighten1-background white--text d-flex align-center justify-center line-height-normal"
      flat>
      {{ shortcut }}
    </v-card>
  </div>
</template>
<script>
export default {
  props: {
    shortcut: {
      type: String,
      default: null,
    },
  },
};
</script>