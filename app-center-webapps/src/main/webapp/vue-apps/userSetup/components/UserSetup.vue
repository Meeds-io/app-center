<template>
  <div class="userApplications">
    <v-row dense>
      <v-col xl="6" sm="9">
        <user-authorizedApplications />
      </v-col>
      <v-col xl="3">
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
