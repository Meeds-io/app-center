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
  <div class="userApplications">
    <v-row dense>
      <v-col class="authorizedApplicationsContainer">
        <user-authorizedApplications :can-add-favorite="canAddFavorite" :default-app-image="defaultAppImage"></user-authorizedApplications>
      </v-col>
      <v-col class="userFavoriteApplicationsContainer" sm="3">
        <user-favoriteApplications :default-app-image="defaultAppImage" @canAddFavorite="setCanAddFavorite"></user-favoriteApplications>
      </v-col>      
    </v-row>
  </div>
</template>

<script>
export default {
  data() {
    return {
      defaultAppImage: {
        fileBody: '',
        fileName: '',
        invalidSize: false,
        invalidImage: false
      },
      canAddFavorite: false,
    };
  },
  created() {
    this.getAppGeneralSettings();
    this.pageSize = this.$parent.$data.preferences.pageSize;
  },
  methods: {
    setCanAddFavorite(canAddFavorite) {
      this.canAddFavorite = canAddFavorite;
    },
    getAppGeneralSettings() {
      return fetch('/portal/rest/app-center/settings', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error getting favorite applications list');
          }
        })
        .then(data => {
          Object.assign(this.defaultAppImage, data && data.defaultApplicationImage);
        });
    },
  },
};
</script>
