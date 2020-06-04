<template>
  <div class="userApplications">
    <v-row dense>
      <v-col class="authorizedApplicationsContainer">
        <user-authorizedApplications :can-add-favorite="canAddFavorite"></user-authorizedApplications>
      </v-col>
      <v-col class="userFavoriteApplicationsContainer" sm="3">
        <user-favoriteApplications @canAddFavorite="setCanAddFavorite"></user-favoriteApplications>
      </v-col>      
    </v-row>
  </div>
</template>

<script>
export default {
  data() {
    return {
      canAddFavorite: false,
    };
  },
  created() {
    this.pageSize = this.$parent.$data.preferences.pageSize;
  },
  methods: {
    setCanAddFavorite(canAddFavorite) {
      this.canAddFavorite = canAddFavorite;
    },
  },
};
</script>
