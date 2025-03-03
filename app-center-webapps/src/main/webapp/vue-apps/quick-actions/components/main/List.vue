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
  <v-data-table
    :headers="headers"
    :items="filteredQuickActions"
    :disable-sort="$root.isMobile"
    :hide-default-header="$root.isMobile"
    disable-pagination
    hide-default-footer
    class="application-body quickActionsTable px-5">
    <template slot="item" slot-scope="props">
      <quick-actions-item
        :key="props.item.id"
        :quick-action="props.item" />
    </template>
  </v-data-table>
</template>
<script>
export default {
  props: {
    keyword: {
      type: String,
      default: null,
    },
  },
  computed: {
    headers() {
      return (this.$root.isMobile && [
        {
          text: this.$t('quickActions.label.name'),
          value: 'name',
          align: 'left',
          sortable: false,
          class: 'quick-actions-name-header',
          width: '100%'
        },
        {
          text: this.$t('quickActions.label.status'),
          value: 'enabled',
          align: 'center',
          sortable: false,
          class: 'quick-actions-status-header text-no-wrap',
          width: '90px'
        },
      ]) || (this.$vuetify.breakpoint.lgAndDown && [
        {
          text: '',
          value: 'icon',
          align: 'left',
          sortable: false,
          class: 'quick-actions-illustration-header',
          width: '35px'
        },
        {
          text: this.$t('quickActions.label.name'),
          value: 'name',
          align: 'left',
          sortable: false,
          class: 'quick-actions-name-header ps-0',
          width: 'auto'
        },
        {
          text: this.$t('quickActions.label.status'),
          value: 'enabled',
          align: 'center',
          sortable: false,
          class: 'quick-actions-status-header',
          width: '75px'
        },
      ]) || [
        {
          text: '',
          value: 'icon',
          align: 'center',
          sortable: false,
          class: 'quick-actions-illustration-header',
          width: '35px'
        },
        {
          text: this.$t('quickActions.label.name'),
          value: 'name',
          align: 'left',
          sortable: false,
          class: 'quick-actions-name-header ps-0',
          width: 'auto'
        },
        {
          text: this.$t('quickActions.label.description'),
          value: 'description',
          align: 'left',
          sortable: false,
          class: 'quick-actions-description-header',
          width: '50%'
        },
        {
          text: this.$t('quickActions.label.status'),
          value: 'enabled',
          align: 'center',
          sortable: false,
          class: 'quick-actions-status-header text-no-wrap',
          width: '90px'
        },
      ];
    },
    quickActions() {
      return this.$root.quickActions;
    },
    filteredQuickActions() {
      return this.keyword?.length && this.quickActions.filter(t => {
        const name = this.$te(t.name) ? this.$t(t.name) : t.name;
        const description = this.$te(t.description) ? this.$t(t.description) : t.description;
        return name?.toLowerCase?.()?.includes(this.keyword.toLowerCase())
          || this.$utils.htmlToText(description)?.toLowerCase?.()?.includes(this.keyword.toLowerCase());
      }) || this.quickActions || [];
    },
  },
};
</script>
