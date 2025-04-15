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
    <div class="d-flex flex-wrap ma-5">
      <div
        v-for="application in filteredApplications"
        :key="application.id"
        class="col-4 pa-0 mt-4">
        <v-hover>
          <v-card
            slot-scope="{ hover }"
            :flat="!hover"
            :class="{'z-index-one': hover}"
            class="d-flex flex-column align-center fill-height">
            <v-img
              v-if="application.imageUrl"
              :src="application.imageUrl"
              class="mt-2"
              max-height="65"
              max-width="65"
              width="65" />
            <v-icon
              v-else-if="application.icon"
              class="d-flex align-center justify-center mt-2"
              size="65">
              {{ application.icon }}
            </v-icon>
            <v-img
              v-else
              src="/app-center/skin/images/defaultApp.png"
              max-height="65"
              max-width="65"
              class="mt-2"
              width="65" />
            <span class="text-body mt-2">{{ application.title }}</span>
          </v-card>
        </v-hover>
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