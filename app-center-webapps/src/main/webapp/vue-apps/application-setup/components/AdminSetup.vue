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
      <div class="d-flex align-center px-5 pt-5">
        <help-label
          label="appCenter.adminSetupForm.applicationsTitle"
          label-class="text-title"
          tooltip="appCenter.adminSetupForm.whatIsApplications">
          <template #helpContent>
            <p>
              {{ $t('appCenter.adminSetupForm.whatIsApplications.part1') }}
            </p>
            <ul class="reset-style-box">
              <li>{{ $t('appCenter.adminSetupForm.whatIsApplications.part2') }}</li>
              <li>{{ $t('appCenter.adminSetupForm.whatIsApplications.part3') }}</li>
              <li>{{ $t('appCenter.adminSetupForm.whatIsApplications.part4') }}</li>
            </ul>
          </template>
        </help-label>
        <v-btn
          :title="$t('generalSettings.topbar.switchDevicePreview')"
          class="ms-2"
          icon
          @click="$root.mobilePreview = !$root.mobilePreview">
          <v-icon size="20">{{ $root.mobilePreview && 'fa-desktop' || 'fa-mobile-alt' }}</v-icon>
        </v-btn>
      </div>
      <div class="d-flex flex-column flex-sm-row">
        <div class="d-flex flex-column">
          <app-center-admin-toolbar
            ref="toolbar"
            @filter-changed="keyword = $event" />
          <app-center-admin-list
            ref="adminSetupList"
            :keyword="keyword" />
        </div>
        <v-card
          min-width="420"
          class="ma-5"
          flat>
          <app-center-admin-preview />
        </v-card>
      </div>
    </v-main>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    keyword: null,
  }),
};
</script>
