<template>
  <div class="userApplications">
    <v-row dense>
      <v-col xl="6">
        <user-authorizedApplications />
      </v-col>
      <v-col sm="3" xl="6">
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
