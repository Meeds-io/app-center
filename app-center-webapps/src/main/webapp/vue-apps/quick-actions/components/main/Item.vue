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
  <tr>
    <!-- Illustration -->
    <!-- name -->
    <td
      colspan="2"
      align="left"
      class="pe-0">
      <div class="d-flex align-center text-start">
        <v-card
          color="transparent"
          min-width="35"
          class="me-4 d-flex align-center"
          flat>
          <v-icon size="28">{{ quickAction.icon || 'fa-globe' }}</v-icon>
        </v-card>
        <div v-sanitized-html="name" class="text-break"></div>
      </div>
    </td>
    <!-- description -->
    <td
      v-if="!$vuetify.breakpoint.lgAndDown"
      align="left"
      width="50%">
      <div v-sanitized-html="description" class="text-break"></div>
    </td>
    <td
      v-if="!$root.isMobile"
      align="center">
      <v-switch
        v-model="enabled"
        :loading="loading"
        :aria-label="enabled && $t('quickActions.label.disable') || $t('quickActions.label.enable')"
        class="ma-auto pa-0 width-fit-content"
        hide-details
        @click="changeStatus" />
    </td>
  </tr>
</template>
<script>
export default {
  props: {
    quickAction: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    loading: false,
    enabled: false,
  }),
  computed: {
    quickActionId() {
      return this.quickAction?.id;
    },
    disabled() {
      return this.quickAction?.disabled;
    },
    name() {
      return this.$te(this.quickAction?.name) ? this.$t(this.quickAction?.name) : this.quickAction?.name;
    },
    description() {
      return this.$te(this.quickAction?.description) ? this.$t(this.quickAction?.description) : this.quickAction?.description;
    },
    icon() {
      return this.quickAction?.icon;
    },
  },
  watch: {
    disabled: {
      immediate: true,
      handler() {
        this.enabled = !this.disabled;
      },
    },
  },
  methods: {
    async changeStatus() {
      this.$root.$emit('close-alert-message');
      this.loading = true;
      try {
        await this.$quickActionService.setQuickActionEnabled(this.quickActionId, this.enabled);
        this.$root.$emit('alert-message', this.enabled && this.$t('quickActions.status.enabled.success') || this.$t('quickActions.status.disabled.success'), 'success');
      } catch (e) {
        console.error(e);
        this.$root.$emit('alert-message', this.$t('quickActions.status.update.error'), 'error');
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>