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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    :loading="loading"
    class="appCenterMobileDrawer">
    <template v-if="drawer" #content>
      <v-flex class="mx-0 drawerHeader flex-grow-0">
        <v-list-item class="ps-1 pe-0">
          <v-list-item-action class="drawerIcons me-2">
            <v-btn icon @click="goBack">
              <v-icon size="20">
                {{ $vuetify.rtl && 'fa fa-arrow-right' || 'fa fa-arrow-left' }}
              </v-icon>
            </v-btn>
          </v-list-item-action>
          <v-list-item-content v-if="filter" class="drawerTitle align-start text-title py-2 px-5">
            <div class="text-truncate full-width">
              <v-text-field
                v-model="keyword"
                :placeholder="$t('appCenter.appLauncher.filterPlaceholder')"
                :prepend-inner-icon="keyword && 'fa-filter primary--text' || 'fa-filter icon-default-color'"
                class="flex-grow-1 full-height pa-0"
                clear-icon="fa-times fa-1x primary--text position-absolute absolute-vertical-center"
                height="36"
                autocomplete="off"
                hide-details
                clearable
                v-on="on" />
            </div>
          </v-list-item-content>
          <template v-else>
            <v-list-item-content class="drawerTitle align-start text-title">
              <div class="text-truncate full-width">
                {{ $t("appCenter.appLauncher.drawer.title") }}
              </div>
            </v-list-item-content>
            <v-list-item-action class="drawerIcons align-end d-flex flex-row">
              <v-btn
                :title="$t('appCenter.userSettings.mobile.filterTitle')"
                icon
                @click="toogleFilter">
                <v-icon size="20">fa-filter</v-icon>
              </v-btn>
              <v-btn
                :title="$t('label.close')"
                icon
                @click="close">
                <v-icon size="20">fa-times</v-icon>
              </v-btn>
            </v-list-item-action>
          </template>
        </v-list-item>
      </v-flex>
      <v-divider class="my-0" />
      <div v-if="loading" class="position-relative z-index-two">
        <v-progress-linear
          indeterminate
          color="primary"
          class="position-absolute" />
      </div>
      <div class="d-flex flex-column">
        <v-layout
          v-if="hasApplications"
          class="d-flex flex-column favorite appsContainer px-4 mt-4">
          <div class="appLauncherList d-flex flex-wrap">
            <div
              v-for="application in filteredApplications"
              :key="application.id"
              class="flex-grow-1 flex-shrink-0 col-12 col-sm-6 pa-0 mb-4">
              <app-center-item
                :application="application"
                :loading="appLoading === application.url"
                min-height="227"
                max-height="227"
                display-name
                display-description
                card
                elevate
                @open="$emit('open', application.type, application.url)"
                @toogle-favorite="$emit('toogle-favorite', application)"
                @edit="$emit('edit', application)" />
            </div>
          </div>
        </v-layout>
        <div v-else-if="!loading" class="content d-flex align-center justify-center">
          <app-center-launcher-empty
            has-applications
            class="mt-12"
            @reset="resetFilter" />
        </div>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  props: {
    applications: {
      type: Array,
      default: null,
    },
  },
  data: () => ({
    drawer: false,
    loading: false,
    filter: false,
    keyword: null,
    appLoading: null,
  }),
  computed: {
    mobileApplications() {
      return this.applications?.filter?.(a => a.mobile);
    },
    filteredApplications() {
      return this.keyword
        && this.mobileApplications
          .filter(a => !this.keyword
            || a.title.toLowerCase().includes(this.keyword.trim().toLowerCase())
            || a.description?.toLowerCase?.()?.includes?.(this.keyword.trim().toLowerCase()))
          .filter(a => !this.categoryId
              || a.categoryIds?.includes?.(this.categoryId)
              || a.categoryIds?.find?.(id => this.subCategoryIds?.includes?.(id)))
        || this.mobileApplications;
    },
    hasApplications() {
      return this.filteredApplications?.length;
    },
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    toogleFilter() {
      this.filter = !this.filter;
    },
    resetFilter() {
      if (this.keyword) {
        this.keyword = null;
      } else {
        this.close();
      }
    },
    goBack() {
      if (this.filter) {
        this.filter = false;
      } else {
        this.close();
      }
    },
  }
};
</script>
