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
  <v-app>
    <div class="userApplications application-body">
      <v-row dense>
        <v-col class="authorizedApplicationsContainer application-background-color application-border application-border-radius">
          <user-authorizedApplications :can-add-favorite="canAddFavorite" />
        </v-col>
        <v-col class="userFavoriteApplicationsContainer application-background-color application-border application-border-radius" sm="3">
          <user-favoriteApplications @canAddFavorite="setCanAddFavorite" />
        </v-col>      
      </v-row>
    </div>
  </v-app>
</template>
<script>
export default {
  props: {
    preferences: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      canAddFavorite: false,
    };
  },
  created() {
    this.pageSize = this.preferences.pageSize;
  },
  methods: {
    setCanAddFavorite(canAddFavorite) {
      this.canAddFavorite = canAddFavorite;
    },
  },
};
</script>
