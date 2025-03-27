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
  <div class="d-flex align-center justify-space-between">
    <div class="widget-text-header align-start">
      {{ $t('myApplications.name.label') }}
    </div>
    <v-tooltip
      :disabled="hasApplications"
      bottom>
      <template #activator="{ on, attrs }">
        <v-btn
          :href="userAppSetupUrl"
          :icon="!hasApplications"
          :text="hasApplications"
          :class="{'pa-0': hasApplications}"
          :color="hasApplications && 'primary'"
          target="_self"
          class="align-end text-font-size"
          small
          link
          v-bind="attrs"
          v-on="on">
          <v-icon
            v-if="!hasApplications"
            :size="18"
            class="icon-default-color icon-default-size">
            fas fa-plus
          </v-icon>
          <span
            v-else
            class="primary--text text-none">
            {{ $t('myApplications.seeMore.label') }}
          </span>
        </v-btn>
      </template>
      {{ $t('myApplications.add.application.tooltip') }}
    </v-tooltip>
  </div>
</template>

<script>
export default {
  data() {
    return {
      userAppSetupUrl: `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`,
    };
  },
  props: {
    hasApplications: {
      type: Boolean,
      default: false
    }
  }
};
</script>
