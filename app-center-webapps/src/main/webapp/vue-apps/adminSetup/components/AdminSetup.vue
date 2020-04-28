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
    <div class="uiTabNormal uiTabInPage white">
      <ul class="nav nav-tabs px-0">
        <li :class="{ active: activeTab === 1 }" @click="activeTab = 1">
          <a href="#globalParams" data-toggle="tab">{{
            $t("appCenter.adminSetupForm.generalSettings")
          }}</a>
        </li>
        <li :class="{ active: activeTab === 2 }" @click="activeTab = 2">
          <a href="#list" data-toggle="tab">{{
            $t("appCenter.adminSetupForm.applications")
          }}</a>
        </li>
      </ul>
      <div class="px-3 pb-2">
        <div
          v-if="activeTab === 1"
          id="globalParams"
          class="tab-pane fade in active"
        >
          <adminSetup-generalParams />
        </div>
        <div
          v-if="activeTab === 2"
          id="list"
          class="tab-pane fade in active"
        >
          <adminSetup-list :page-size="pageSize" />
        </div>
      </div>
    </div>
  </v-app>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 1,
      pageSize: 10,
    };
  },
  created() {
    this.pageSize = this.$parent.$data.preferences.pageSize && Number(this.$parent.$data.preferences.pageSize) || this.pageSize;
  },
  mounted() {
    const windowLocationHash = window.location.hash;
    if (windowLocationHash === '#list') {
      this.activeTab = 2;
    } else {
      this.activeTab = 1;
    }
  }
};
</script>
