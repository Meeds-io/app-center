<template>
  <div class="userApplications">
    <v-row dense>
      <v-col>
        <user-authorizedApplications />
      </v-col>
      <v-col sm="3">
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
