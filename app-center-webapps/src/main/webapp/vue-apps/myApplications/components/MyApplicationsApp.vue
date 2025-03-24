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
  <v-app>
    <v-card
      class="application-body position-static pa-5"
      flat>
      <div class="d-flex">
        <h4 class="widget-text-header my-auto me-auto">
          {{ $t('myApplications.name.label') }}
        </h4>
        <v-tooltip bottom>
          <template #activator="{ on, attrs }">
            <v-btn
              :href="userAppSetupUrl"
              target="_self"
              class="ms-auto"
              link
              icon
              v-bind="attrs"
              v-on="on">
              <v-icon class="icon-default-color icon-default-size">
                fas fa-plus
              </v-icon>
            </v-btn>
          </template>
          {{ $t('myApplications.add.application.tooltip') }}
        </v-tooltip>
      </div>
      <my-applications-list
        :applications-list="favoriteApplications" />
    </v-card>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      userAppSetupUrl: '/portal/intranet/appCenterUserSetup',
      favoriteApplications: []
    };
  }
};
</script>
