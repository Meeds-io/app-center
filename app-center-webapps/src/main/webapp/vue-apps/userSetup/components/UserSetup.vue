<template>
  <div class="userApplications">
    <v-row dense>
      <v-col class="authorizedApplicationsContainer">
        <user-authorizedApplications />
      </v-col>
      <v-col class="userFavoriteApplicationsContainer" sm="3">
        <user-favoriteApplications />
      </v-col>      
    </v-row>
  </div>
</template>

<script>
export default {
  created() {
    this.pageSize = this.$parent.$data.preferences.pageSize;
  }
};
</script>
