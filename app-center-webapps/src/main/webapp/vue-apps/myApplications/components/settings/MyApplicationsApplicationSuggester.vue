<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <v-autocomplete
    ref="suggester"
    v-model="selected"
    :search-input.sync="keyword"
    :items="items"
    :loading="loading || searching"
    :disabled="disabled"
    :placeholder="$t('myApplications.searchApp.placeholder')"
    :no-data-text="$t('myApplications.searchApp.noData')"
    :hint="capReached && $t('myApplications.searchApp.capReached', {0: maxApplications}) || ''"
    :persistent-hint="capReached"
    :hide-details="!capReached"
    item-value="id"
    item-text="title"
    class="pa-0"
    return-object
    no-filter
    outlined
    dense
    @focus="!suggestions.length && search()"
    @change="add" />
</template>

<script>
export default {
  props: {
    settingName: {
      type: String,
      default: null,
    },
    selectedIds: {
      type: Array,
      default: () => [],
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    capReached: {
      type: Boolean,
      default: false,
    },
    maxApplications: {
      type: Number,
      default: 100,
    },
  },
  data: () => ({
    selected: null,
    keyword: null,
    suggestions: [],
    searching: false,
    searchTimeout: null,
  }),
  computed: {
    items() {
      return this.suggestions.filter(application => !this.selectedIds.includes(application.id));
    },
  },
  watch: {
    keyword() {
      window.clearTimeout(this.searchTimeout);
      this.searchTimeout = window.setTimeout(() => this.search(), 300);
    },
  },
  beforeDestroy() {
    window.clearTimeout(this.searchTimeout);
  },
  methods: {
    async search() {
      const keyword = this.keyword;
      this.searching = true;
      try {
        const suggestions = await this.$myApplicationsService.getContextSuggestions(this.settingName, keyword, this.selectedIds);
        if (keyword === this.keyword) {
          this.suggestions = suggestions;
        }
      } catch (e) {
        this.suggestions = [];
      } finally {
        this.searching = false;
      }
    },
    add(application) {
      if (application) {
        this.$emit('add', application);
      }
      this.$nextTick(() => {
        this.selected = null;
        this.keyword = null;
      });
    },
  },
};
</script>
