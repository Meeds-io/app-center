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
  <v-app
    id="KudosAdminApp"
    class="applicationsAdmin">
    <main>
      <v-layout>
        <v-flex>
          <v-tabs
            v-model="selectedTab"
            slider-size="4">
            <v-tab key="applications" href="#applications" @click="loadApplicationsList">
              {{ $t('appCenter.adminSetupForm.applications') }}
            </v-tab>
            <v-tab key="settings" href="#settings">
              {{ $t('appCenter.adminSetupForm.generalSettings') }}
            </v-tab>
          </v-tabs>

          <v-tabs-items v-model="selectedTab" class="mt-2">
            <v-tab-item
              id="applications"
              value="applications"
              class="px-4 py-2">
              <adminSetup-list :key="adminSetupListKey" />
            </v-tab-item>
            <v-tab-item
              id="settings"
              value="settings"
              class="px-4 py-2">
              <adminSetup-generalParams />
            </v-tab-item>
          </v-tabs-items>
        </v-flex>
      </v-layout>
    </main>
  </v-app>
</template>
<script>
export default {
  data() {
    return {
      selectedTab: 'applications',
      adminSetupListKey: 0,
    };
  },
  methods: {
    loadApplicationsList() {
      this.adminSetupListKey++;
    }
  },
};
</script>
