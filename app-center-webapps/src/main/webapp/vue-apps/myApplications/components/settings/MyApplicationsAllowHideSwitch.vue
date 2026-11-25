<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2026 Meeds Association contact@meeds.io

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
  <div class="d-flex align-center justify-space-between mt-2">
    <div class="v-label text-color align-start">
      {{ $t('myApplications.allowHide.label') }}
    </div>
    <div class="align-end">
      <v-switch
        id="myApplicationsAllowHideSwitch"
        :input-value="value"
        :disabled="disabled"
        :aria-label="$t('myApplications.allowHide.label')"
        color="primary"
        class="pa-0 my-auto"
        hide-details
        @change="$emit('input', !!$event)" />
    </div>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
