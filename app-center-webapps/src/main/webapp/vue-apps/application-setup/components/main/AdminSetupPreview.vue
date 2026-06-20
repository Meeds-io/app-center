<template>
  <v-card
    class="d-flex flex-column full-width"
    color="transparent"
    min-height="750"
    elevation="2">
    <v-card class="d-flex align-center transparent pa-5" flat>
      <span class="text-title">{{ $t('appCenter.appLauncher.drawer.title') }}</span>
      <v-spacer />
      <v-btn v-if="allowUserPersonalApps" icon small>
        <v-icon size="20">fa-plus</v-icon>
      </v-btn>
      <v-btn icon small>
        <v-icon size="20">fa-expand-alt</v-icon>
      </v-btn>
      <v-btn icon small>
        <v-icon size="20">fa-times</v-icon>
      </v-btn>
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
    <v-divider />
    <div class="pa-5">
      <div class="text-header mb-4">
        {{ $t('appCenter.adminSetupForm.generalSettings') }}
      </div>
      <div class="d-flex align-center justify-space-between">
        <div class="text-start">
          <div class="v-label text-color">
            {{ $t('appCenter.adminSetupForm.allowUserPersonalApps') }}
          </div>
          <div class="caption text--secondary">
            {{ $t('appCenter.adminSetupForm.allowUserPersonalApps.description') }}
          </div>
        </div>
        <v-switch
          v-model="allowUserPersonalApps"
          :disabled="savingSettings"
          :loading="savingSettings"
          color="primary"
          class="pa-0 my-auto ms-4"
          hide-details
          @change="saveSettings" />
      </div>
    </div>
  </v-card>
</template>
<script>
export default {
  data() {
    return {
      allowUserPersonalApps: false,
      savingSettings: false,
    };
  },
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
  created() {
    this.$applicationService.getAppCenterSettings()
      .then(settings => {
        this.allowUserPersonalApps = settings?.allowUserPersonalApps || false;
      });
  },
  methods: {
    saveSettings() {
      this.savingSettings = true;
      this.$applicationService.saveAppCenterSettings({ allowUserPersonalApps: this.allowUserPersonalApps })
        .then(() => {
          this.$root.$emit('alert-message',
            this.$t('appCenter.adminSetupForm.settings.saved.success'),
            'success');
        })
        .catch(() => {
          this.allowUserPersonalApps = !this.allowUserPersonalApps;
          this.$root.$emit('alert-message',
            this.$t('appCenter.adminSetupForm.settings.saved.error'),
            'error');
        })
        .finally(() => {
          this.savingSettings = false;
        });
    },
  },
};
</script>