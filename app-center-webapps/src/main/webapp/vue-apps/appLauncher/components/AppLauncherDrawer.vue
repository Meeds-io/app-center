<!--
This file is part of the Meeds project (https://meeds.io/).
Copyright (C) 2020 Meeds Association
contact@meeds.io
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
    :allow-expand="expanded"
    :loading="drawerLoading"
    body-classes="hide-scroll"
    class="appCenterDrawer"
    @expand-updated="expanded = $event">
    <template #title>
      {{ applicationsLoaded && $t("appCenter.appLauncher.drawer.title") || '' }}
    </template>
    <template v-if="!expanded && !$root.isMobile" #titleIcons>
      <v-btn
        :title="$t('appCenter.appLauncher.addAppPlaceHolder')"
        icon
        class="text-xs-center"
        @click="expandDrawer">
        <v-icon size="20">fa-plus</v-icon>
      </v-btn>
    </template>
    <template v-if="drawer" #content>
      <div v-if="cardDisplay" class="d-flex justify-space-between">
        <div class="col-6 pa-0">
          <div class="ms-4">
            <categories-filter v-model="categoryId" class="mt-4" />
          </div>
        </div>
        <div class="col-6 pa-0">
          <application-toolbar
            id="appLauncherToolbar"
            :right-text-filter="{
              minCharacters: 1,
              placeholder: $t('appCenter.appLauncher.filterPlaceholder'),
              tooltip: $t('appCenter.appLauncher.filterPlaceholder'),
              minWidth: 'min(calc(100% - 100px), 275px)',
            }"
            :right-select-box="{
              selected: filter,
              items: filters,
            }"
            class="border-box-sizing px-1 flex-grow-0"
            hide-left
            hide-center
            @filter-text-input="keyword = $event"
            @filter-select-change="filter = $event" />
        </div>
      </div>
      <v-expand-transition v-else-if="hasRecentApplications">
        <div v-show="!expanded">
          <v-layout class="d-flex flex-column flex-wrap px-4 mt-4">
            <div class="text-header mb-2">
              {{ $t('appCenter.appLauncher.recentApps') }}
            </div>
            <card-carousel class="d-flex max-width-fit">
              <v-tooltip
                v-for="application in recentApplications"
                :key="application.id"
                bottom>
                <template #activator="{on, attrs}">
                  <div
                    v-on="on"
                    v-bind="attrs"
                    class="border-color border-radius me-4">
                    <app-center-launcher-item
                      :application="application"
                      :loading="appLoading === application.url"
                      min-width="60"
                      max-width="60"
                      max-height="60"
                      min-height="60"
                      image-size="24"
                      icon-size="24"
                      elevate
                      @open="openApplication(application.type, application.url)" />
                  </div>
                </template>
                <span>{{ application.title }}</span>
              </v-tooltip>
            </card-carousel>
            <div class="text-header mb-n2">
              {{ $t('appCenter.appLauncher.favoriteApps') }}
            </div>
          </v-layout>
        </div>
      </v-expand-transition>
      <v-layout
        v-if="hasApplications"
        :class="!expanded && 'mt-4'"
        class="d-flex flex-column favorite appsContainer px-4">
        <component
          :is="!$root.isMobile && !expanded && 'draggable' || 'div'"
          v-model="favoriteApplications"
          class="appLauncherList d-flex flex-wrap me-n2"
          @start="drag=true"
          @end="drag=false">
          <div
            v-for="application in filteredApplications"
            :key="application.id"
            :class="cardDisplay && 'mb-4' || 'mb-3'"
            class="flex-grow-1 flex-shrink-0 col-4 pa-0">
            <app-center-launcher-item
              :application="application"
              :loading="appLoading === application.url"
              :card="cardDisplay"
              :min-height="cardDisplay && 227 || 'auto'"
              :max-height="cardDisplay && 227 || 'auto'"
              :class="cardDisplay && 'me-4' || 'me-3'"
              display-name
              display-description
              @open="openApplication(application.type, application.url)"
              @toogle-favorite="toogleFavorite(application)" />
          </div>
        </component>
      </v-layout>
      <div v-else-if="!drawerLoading" class="content d-flex align-center justify-center">
        <app-center-launcher-empty class="mt-12" />
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    expanded: false,
    loading: false,
    applicationsLoaded: false,
    recentApplicationIds: [],
    availableApplications: [],
    favoriteApplications: [],
    applicationsOrder: null,
    drawer: false,
    drag: false,
    filter: 'ALL',
    categoryId: null,
    subCategoryIds: null,
    appLoading: null,
    keyword: null,
    draggedElementIndex: null,
  }),
  computed: {
    isFavoriteFilter() {
      return !this.expanded || this.filter === 'FAVORITES';
    },
    applications() {
      return (this.isFavoriteFilter ? this.favoriteApplications : this.availableApplications) || [];
    },
    sortedApplicationsList() {
      return this.$root.isMobile ? this.applications.filter(app => app.mobile) : this.applications;
    },
    filteredApplications() {
      return this.expanded && (this.keyword || this.categoryId)
        && this.sortedApplicationsList
          .filter(c => !this.keyword
            || c.title.toLowerCase().includes(this.keyword.trim().toLowerCase())
            || c.description?.toLowerCase?.()?.includes?.(this.keyword.trim().toLowerCase()))
          .filter(c => !this.categoryId
              || c.categoryIds?.includes?.(this.categoryId)
              || c.categoryIds?.find?.(id => this.subCategoryIds?.includes?.(id)))
        || this.sortedApplicationsList;
    },
    drawerLoading() {
      return this.loading || !this.applicationsLoaded;
    },
    cardDisplay() {
      return this.expanded && !this.$root.isMobile;
    },
    hasApplications() {
      return this.applications?.length;
    },
    recentApplications() {
      const recentApplications = this.favoriteApplications?.filter?.(a => this.recentApplicationIds.includes(a.id));
      recentApplications.sort((a, b) => this.recentApplicationIds.indexOf(a.id) - this.recentApplicationIds.indexOf(b.id));
      return recentApplications;
    },
    hasRecentApplications() {
      return this.recentApplications?.length;
    },
    filters() {
      return [{
        text: this.$t('appCenter.appLauncher.filter.all'),
        value: 'ALL',
      },{
        text: this.$t('appCenter.appLauncher.filter.favorites'),
        value: 'FAVORITES',
      }];
    },
  },
  watch: {
    drag() {
      if (!this.drag) {
        this.updateApplicationsOrder();
      }
    },
    drawer() {
      if (this.drawer) {
        const recentAppIdsString = window.localStorage.getItem('meeds-app-center-recent-apps');
        if (!recentAppIdsString?.length) {
          this.recentApplicationIds = [];
        } else {
          this.recentApplicationIds = JSON.parse(recentAppIdsString);
        }
      }
    },
    expanded() {
      if (!this.expanded) {
        this.keyword = null;
        this.filter = 'ALL';
      }
      this.retrieveApplications();
    },
    filter() {
      this.retrieveApplications();
    },
    categoryId() {
      this.retrieveSubCategoryIds();
    },
  },
  created() {
    this.$root.$on('app-center-add-app', this.expandDrawer);
    this.init();
  },
  mounted() {
    if (!this.$root.noAutoOpen) {
      this.open();
    } else if (this.$utils.getQueryParam('appCenterDrawer')) {
      this.openApplication('DRAWER', this.$utils.getQueryParam('appCenterDrawer'));
    }
  },
  beforeDestroy() {
    this.$root.$off('app-center-add-app', this.expandDrawer);
  },
  methods: {
    init() {
      this.applicationsLoaded = false;
      this.retrieveApplications()
        .finally(() => {
          this.applicationsLoaded = true;
          this.$root.$applicationLoaded();
        });
    },
    retrieveApplications() {
      this.loading = true;
      return this.getApplications(this.isFavoriteFilter)
        .then(data => {
          const applications = data.applications;
          // manage system apps localized names
          applications.forEach(app => {
            if (app.system) {
              const title = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
              if (this.$te(`appCenter.system.application.${title}`)) {
                app.title = this.$t(`appCenter.system.application.${title}`);
              }
            }
          });
          if (this.expanded) {
            this.sortApplicationsByTitle(applications);
          } else {
            this.sortApplicationsByOrder(applications);
          }
          if (this.isFavoriteFilter) {
            this.favoriteApplications = applications;
          } else {
            this.availableApplications = applications;
          }
        }).finally(() => this.loading = false);
    },
    getApplications(favorites) {
      return fetch(favorites && '/app-center/rest/favorites' || '/app-center/rest/applications', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error getting favorite applications list');
          }
        });
    },
    updateApplicationsOrder() {
      const applicationsOrder = this.favoriteApplications.map((app, index) => ({
        id: app.id,
        order: index,
      }));
      if (applicationsOrder.length) {
        return fetch('/app-center/rest/favorites', {
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          method: 'PUT',
          body: JSON.stringify(applicationsOrder)
        });
      }
    },
    async openApplication(appType, appUrl) {
      if (appType === 'DRAWER' && this.$root.quickActions[appUrl]) {
        this.appLoading = appUrl;
        try {
          await this.$root.quickActions[appUrl].click();
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
        
      }
    },
    sortApplicationsByOrder(apps) {
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
    },
    sortApplicationsByTitle(apps) {
      apps.sort((a, b) => this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase()));
    },
    toogleFavorite(application) {
      this.loading = true;
      return fetch(`/app-center/rest/favorites/${application.id}`, {
        credentials: 'include',
        method: application.favorite ? 'DELETE' : 'POST',
      })
        .then(() => this.retrieveApplications())
        .then(() => {
          if (application.favorite) {
            this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.favoriteRemoved'), 'success');
          } else {
            this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.favoriteAdded'), 'success');
          }
        })
        .finally(() => this.loading = false);
    },
    async expandDrawer() {
      this.expanded = true;
      await this.$nextTick();
      this.$refs.drawer.toogleExpand();
    },
    open() {
      this.$refs.drawer.open();
    },
    async retrieveSubCategoryIds() {
      if (!this.categoryId) {
        this.subCategoryIds = [];
      } else {
        const subcategoyIds = await this.$categoryService.getSubcategoryIds(this.categoryId, {
          offset: 0,
          limit: -1,
          depth: 10,
        });
        const subcategoyIdsFlat = subcategoyIds.flatMap(s => s);
        subcategoyIdsFlat.push(this.categoryId);
        this.subCategoryIds = [...new Set(subcategoyIdsFlat)];
      }
    }
  }
};
</script>
