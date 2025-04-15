<template>
  <v-card
    class="d-flex flex-column full-width"
    color="transparent"
    min-height="750"
    elevation="2">
    <v-card class="d-flex align-center transparent pa-5" flat>
      <span class="text-title">{{ $t('appCenter.appLauncher.drawer.title') }}</span>
      <v-spacer />
      <v-icon size="20">fa-plus</v-icon>
      <v-icon size="20" class="ms-5">fa-times</v-icon>
    </v-card>
    <v-divider />
    <div class="d-flex flex-wrap my-4 ms-4 me-1">
      <div class="appLauncherList d-flex flex-wrap border-box-sizing">
        <div
          v-for="application in filteredApplications"
          :key="application.id"
          class="flex-grow-1 flex-shrink-0 col-4 mb-3 pa-0">
          <app-center-item
            :application="application"
            class="me-3"
            display-description
            display-name
            readonly />
        </div>
      </div>
    </div>
  </v-card>
</template>
<script>
export default {
  computed: {
    favoriteApplications() {
      return this.$root.applications?.filter?.(app => app.default || app.mandatory) || [];
    },
    filteredApplications() {
      return this.favoriteApplications.filter(app => !this.$root.mobilePreview || app.mobile);
    },
    hasApplications() {
      return this.filteredApplications?.length;
    },
  },
};
</script>