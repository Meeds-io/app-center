<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
    <app-center-user-settings-window
      v-if="displayDetails"
      @back="close" />
    <div v-else class="application-body">
      <v-list two-line>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-title">
              {{ $t('appCenter.userSettings.shortcuts.title') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
              small
              icon
              @click="open">
              <v-icon size="18" class="icon-default-color">fas fa-eye</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </div>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    id: `Settings${parseInt(Math.random() * 10000)
      .toString()
      .toString()}`,
    language: eXo.env.portal.language,
    displayDetails: false,
    displayed: true,
  }),
  watch: {
    displayed() {
      if (this.displayed) {
        this.$nextTick().then(() => this.$root.$emit('application-cache'));
      }
      this.$root.$updateApplicationVisibility(this.displayed);
    },
  },
  created() {
    document.addEventListener('showSettingsApps', this.showSettingsApps);
    document.addEventListener('hideSettingsApps', this.hideSettingsApps);
    if (window.location.hash === '#apps-shortcuts') {
      window.setTimeout(() => {
        this.open();
      }, 500);
    }
  },
  mounted() {
    this.$nextTick().then(() => {
      this.$root.$applicationLoaded();
    });
    this.$root.$updateApplicationVisibility(this.displayed);
  },
  beforeDestroy() {
    document.removeEventListener('showSettingsApps', this.showSettingsApps);
    document.removeEventListener('hideSettingsApps', this.hideSettingsApps);
  },
  methods: {
    open() {
      document.dispatchEvent(new CustomEvent('hideSettingsApps', { detail: this.id }));
      this.displayDetails = true;
      window.history.replaceState(
        null,
        this.$t('appCenter.userSettings.shortcuts.title'),
        `${window.location.pathname}#apps-shortcuts`
      );
    },
    close() {
      this.displayDetails = false;
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      if (window.location.hash === '#apps-shortcuts') {
        window.history.replaceState('', document.title, window.location.pathname + window.location.search);
      }
    },
    hideSettingsApps(event) {
      if (event?.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    },
    showSettingsApps() {
      this.displayed = true;
    },
  },
};
</script>