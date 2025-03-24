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
    <v-main class="application-body pb-5">
      <h4 class="text-title px-5 pt-5 ma-0">
        {{ $t('appCenter.adminSetupForm.applications') }}
      </h4>
      <div class="d-flex flex-column flex-sm-row">
        <div class="d-flex flex-column">
          <app-center-admin-toolbar
            ref="toolbar"
            @filter-changed="keyword = $event"
            @add="addApp" />
          <app-center-admin-setup-list
            ref="adminSetupList"
            :keyword="keyword" />
        </div>
        <v-divider vertical />
        <div class="ma-5">
          Preview
        </div>
      </div>
    </v-main>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    keyword: null,
  }),
  methods: {
    addApp() {
      this.$refs.adminSetupList.showAddApplicationDrawer();
    },
  },
};
</script>
