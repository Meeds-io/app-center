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
  <v-autocomplete
    ref="selectAutoComplete"
    v-model="portletInstanceId"
    :placeholder="$t('appCenter.adminSetupForm.portletPlaceholder')"
    :loading="loading"
    :items="items"
    item-value="id"
    item-text="label"
    append-icon=""
    class="no-box-shadow no-border pa-0"
    width="100%"
    max-width="100%"
    hide-selected
    hide-details
    outlined
    chips
    dense />
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    loading: false,
    portletInstanceId: null,
  }),
  computed: {
    items() {
      return this.$root.portletInstances.map(item => ({
        id: item.id,
        label: this.$t(item.name),
      }));
    },
  },
  watch: {
    portletInstanceId() {
      this.$emit('input', this.portletInstanceId);
    },
  },
  async created() {
    this.portletInstanceId = this.value && Number(this.value) || null;
    if (!this.$root.portletInstances?.length) {
      this.loading = true;
      try {
        this.$root.portletInstances = await this.getPortletInstances();
      } finally {
        this.loading = false;
      }
    }
    this.initialized = true;
  },
  methods: {
    getPortletInstances() {
      return fetch('/layout/rest/portlet/instances', {
        method: 'GET',
        credentials: 'include',
      }).then(resp => {
        if (!resp?.ok) {
          throw new Error('Error when retrieving portlet instances');
        } else {
          return resp.json();
        }
      });
    },
  }
};
</script>
