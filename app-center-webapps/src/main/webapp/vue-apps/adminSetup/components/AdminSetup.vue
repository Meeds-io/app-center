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
  <v-app class="applicationsAdmin">
    <v-row class="uiTabNormal uiTabInPage white">
      <v-col>
        <v-tabs
          class="appAdminTabs"
          background-color="transparent">
          <v-tab @click="loadApplicationsList">
            {{ $t('appCenter.adminSetupForm.applications') }}
          </v-tab>
          <v-tab>
            {{ $t('appCenter.adminSetupForm.generalSettings') }}
          </v-tab>

          <v-tab-item class="px-4 py-2">
            <adminSetup-list :key="adminSetupListKey" />
          </v-tab-item>
          <v-tab-item class="px-4 py-2">
            <adminSetup-generalParams />
          </v-tab-item>
        </v-tabs>        
      </v-col>
    </v-row>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
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
