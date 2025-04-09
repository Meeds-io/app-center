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
  data: () => ({
    applications: [],
  }),
  created() {
    this.$root.$on('app-center-refresh-list', this.refresh);
    this.$root.$on('app-center-refresh-enabled', this.refresh);
    this.refresh();
  },
  beforeDestroy() {
    this.$root.$off('app-center-refresh-list', this.refresh);
    this.$root.$off('app-center-refresh-enabled', this.refresh);
  },
  computed: {
    sortedApplications() {
      const apps = this.applications || [];
      apps.sort((a, b) => {
        if (a.order === null && b.order === null) {
          return this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase());
        } else if (a.order === null) {
          return 1;
        } else if (b.order === null) {
          return -1;
        } else {
          return a.order - b.order;
        }
      });
      return apps;
    },
    filteredApplications() {
      return this.sortedApplications.filter(app => !this.$root.mobilePreview || app.mobile);
    },
    hasApplications() {
      return this.applications?.length;
    },
  },
  methods: {
    refresh() {
      return fetch('/app-center/rest/favorites', {
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
          // manage system apps localized names
          data.applications.forEach(app => {
            const appTitle = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
            if (this.$te(`appCenter.system.application.${appTitle}`)) {
              app.title = this.$t(`appCenter.system.application.${appTitle}`);
            }
          });
          this.applications = [...data.applications];
        }).finally(() => this.loading = false);
    },
  },
};
</script>