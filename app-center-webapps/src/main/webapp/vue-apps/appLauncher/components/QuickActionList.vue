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
  <div class="content d-flex flex-wrap ma-5 gap-5">
    <v-card
      v-for="action in $root.quickActions"
      :key="action.id"
      :loading="loading[action.id]"
      min-height="100"
      max-height="100"
      min-width="100"
      max-width="100"
      class="position-relative border-color d-flex align-center justify-center"
      flat
      @click="() => action.click()">
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <div
            v-on="on"
            v-bind="attrs"
            class="d-flex flex-column align-center justify-center pa-2">
            <v-icon size="25">{{ action.icon }}</v-icon>
            <div class="my-2"></div>
            <div class="text-truncate-2">{{ $t(action.name) }}</div>
          </div>
        </template>
        <span>{{ $t(action.description) }}</span>
      </v-tooltip>
      <v-tooltip bottom>
        <template #activator="{on, attrs}">
          <v-btn
            v-bind="attrs"
            v-on="on"
            :loading="pinLoading[action.id]"
            :class="$vuetify.rtl && 'l-0' || 'r-0'"
            :color="action.pinned && 'primary'"
            class="position-absolute ignore-vuetify-classes t-0"
            elevation="0"
            x-small
            icon
            @click.stop.prevent="pinApp(action)">
            <v-icon size="12">fa-map-pin</v-icon>
          </v-btn>
        </template>
        <span>{{ action.pinned && $t('quickActions.pinApp') || $t('quickActions.unpinApp') }}</span>
      </v-tooltip>
    </v-card>
  </div>
</template>
<script>
export default {
  data: () => ({
    loading: {},
    pinLoading: {},
  }),
  methods: {
    async pinApp(action) {
      this.$set(this.pinLoading, action.id, true);
      try {
        if (action.pinned) {
          await this.$userQuickActionService.unpinQuickAction(action.id);
          action.pinned = false;
          this.$root.$emit('alert-message', this.$t('quickActions.unpinedApp.success'), 'success');
          document.dispatchEvent(new CustomEvent('QuickActionUnpinned', {detail: action}));
        } else {
          await this.$userQuickActionService.pinQuickAction(action.id);
          action.pinned = true;
          this.$root.$emit('alert-message', this.$t('quickActions.pinedApp.success'), 'success');
          document.dispatchEvent(new CustomEvent('QuickActionPinned', {detail: action}));
        }
      } finally {
        window.setTimeout(() => this.$set(this.pinLoading, action.id, false), 200);
      }
    },
  }
};
</script>
