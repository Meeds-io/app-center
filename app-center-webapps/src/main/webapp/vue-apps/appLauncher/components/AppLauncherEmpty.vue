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
  <div class="d-flex flex-column align-center justify-center mx-5">
    <v-icon size="40" class="icon-default-color">fa-th</v-icon>
    <div class="text-sub-title subtitle-1 mt-6">
      {{ $t('appCenter.appLauncher.drawer.placeholder') }}
    </div>
    <v-btn
      :href="appCenterLink"
      class="btn btn-primary mt-6">
      <span class="text-none">{{ $t('appCenter.appLauncher.drawer.placeholder.action') }}</span>
    </v-btn>
    <div v-if="$root.isAdmin" class="mt-10 hidden-xs-only">
      <v-icon size="18" class="icon-default-color me-2">far fa-lightbulb</v-icon>
      <span v-html="appCenterAdminPlaceholder" class="text-sub-title subtitle-1"></span>
    </div>
  </div>
</template>
<script>
export default {
  data: () => ( {
    appCenterLink: `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/appCenterUserSetup`,
    appCenterAdminLink: `${eXo.env.portal.context}/administration/home/general/applicationsCenter`,
  }),
  computed: {
    appCenterAdminPlaceholder() {
      return this.$t('appCenter.appLauncher.drawer.admin.placeholder', {
        0: `<a href="${this.appCenterAdminLink}" target="_blank" class="primary--text font-weight-bold text-no-wrap">`,
        1: '</a>',
      });
    },
  },
};
</script>
