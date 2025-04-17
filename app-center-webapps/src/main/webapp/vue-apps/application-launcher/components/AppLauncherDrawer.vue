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
            ref="appLauncherToolbar"
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
                    <app-center-item
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
          class="appLauncherList d-flex flex-wrap me-n3"
          @start="drag=true"
          @end="drag=false">
          <div
            v-for="application in filteredApplications"
            :key="application.id"
            :class="cardDisplay && 'mb-4' || 'mb-3'"
            class="flex-grow-1 flex-shrink-0 col-4 pa-0">
            <app-center-item
              :application="application"
              :loading="appLoading === application.url"
              :card="cardDisplay"
              :min-height="cardDisplay && 227 || 'auto'"
              :max-height="cardDisplay && 227 || 'auto'"
              :class="cardDisplay && 'me-4' || 'me-3'"
              display-name
              display-description
              @open="openApplication(application.type, application.url)"
              @toogle-favorite="toogleFavorite(application)"
              @toogle-pin="tooglePin(application, $event)" />
          </div>
        </component>
      </v-layout>
      <div v-else-if="!drawerLoading" class="content d-flex align-center justify-center">
        <app-center-launcher-empty
          :has-applications="hasAvailableApplications"
          class="mt-12"
          @expand="expandDrawer"
          @reset="resetFilter" />
      </div>
      <app-center-portlet-instance-drawer
        ref="portletInstanceDrawer" />
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
    isPinFilter() {
      return !this.expanded || this.filter === 'PINNED';
    },
    applications() {
      if (this.isFavoriteFilter) {
        return this.favoriteApplications;
      } else if (this.isPinFilter) {
        return this.pinnedApplications;
      } else {
        return this.availableApplications;
      }
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
      return this.filteredApplications?.length;
    },
    hasAvailableApplications() {
      return this.availableApplications?.length;
    },
    pinnedApplications() {
      return this.$root.pinnedApplicationIds?.map?.(id => this.availableApplications?.find?.(app => app.id === id)) || [];
    },
    recentApplications() {
      const recentApplications = this.favoriteApplications?.filter?.(a => this.recentApplicationIds.includes(a.id) && (!this.$root.isMobile || !this.$root.pinnedApplicationIds.includes(a.id)));
      recentApplications.sort((a, b) => this.recentApplicationIds.indexOf(a.id) - this.recentApplicationIds.indexOf(b.id));
      return !this.$root.isMobile ? recentApplications : [...this.pinnedApplications, ...recentApplications];
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
      },{
        text: this.$t('appCenter.appLauncher.filter.pinned'),
        value: 'PINNED',
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
        this.init();

        const recentAppIdsString = window.localStorage.getItem(`meeds-app-center-recent-apps-${eXo.env.portal.userIdentityId}`);
        if (!recentAppIdsString?.length) {
          this.recentApplicationIds = [];
        } else {
          this.recentApplicationIds = JSON.parse(recentAppIdsString);
        }
      }
    },
    expanded() {
      if (!this.expanded) {
        this.resetFilter();
      }
      this.retrieveApplications(this.isFavoriteFilter);
    },
    filter() {
      this.retrieveApplications(this.isFavoriteFilter);
    },
    categoryId() {
      this.retrieveSubCategoryIds();
    },
  },
  created() {
    window.addEventListener('keypress', this.openApplicationByShortcutEvent);
  },
  mounted() {
    if (this.$root.shortcut) {
      this.openApplicationByShortcut(this.$root.shortcut);
    } else if (!this.$root.noAutoOpen) {
      this.open();
    } else if (this.$utils.getQueryParam('appCenterDrawer')) {
      this.openApplication('DRAWER', this.$utils.getQueryParam('appCenterDrawer'));
    } else if (this.$utils.getQueryParam('appCenterPortlet')) {
      this.openApplication('PORTLET', this.$utils.getQueryParam('appCenterPortlet'));
    }
  },
  beforeDestroy() {
    window.removeEventListener('keypress', this.openApplicationByShortcutEvent);
  },
  methods: {
    async init() {
      this.applicationsLoaded = false;
      try {
        await this.refresh();
      } finally {
        this.applicationsLoaded = true;
        this.$root.$applicationLoaded();
      }
    },
    async refresh() {
      await Promise.all([
        this.retrieveApplications(true),
        this.retrieveApplications(false)
      ]);
    },
    retrieveApplications(isFavoriteFilter) {
      this.loading = true;
      return this.getApplications(isFavoriteFilter)
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
          if (isFavoriteFilter) {
            this.sortApplicationsByOrder(applications);
            this.favoriteApplications = applications;
          } else {
            this.sortApplicationsByTitle(applications);
            this.availableApplications = applications;
          }
          this.$root.shortcuts = applications.filter(a => a.shortcut).map(a => a.shortcut);
        }).finally(() => this.loading = false);
    },
    getApplications(favorites) {
      return favorites ? this.$applicationFavoriteService.getFavorites() : this.$applicationService.getApplications(false);
    },
    updateApplicationsOrder() {
      const applicationsOrder = this.favoriteApplications.map((app, index) => ({
        id: app.id,
        order: index,
      }));
      return this.$applicationFavoriteService.updateFavoritesOrder(applicationsOrder);
    },
    openApplicationByShortcutEvent(e) {
      if (e.ctrlKey && e.shiftKey && e.key) {
        window.setTimeout(() => this.openApplicationByShortcut(e.key), 10);
      }
    },
    async openApplicationByShortcut(shortcut) {
      if (!this.$root.shortcuts?.includes?.(shortcut) && !this.$root.shortcuts?.includes?.(shortcut.toLowerCase())) {
        return;
      }
      if (!this.applications?.length) {
        await this.retrieveApplications(this.isFavoriteFilter);
      }
      const application = this.applications.find(a => a.shortcut && a.shortcut.toLowerCase() === shortcut.toLowerCase());
      if (application?.type === 'LINK') {
        const computedUrl = application.url
          .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`)
          .replace('@user@', eXo.env.portal.userName);
        const url = this.$utils.toLinkUrl(computedUrl, {
          urls: true,
          email: true,
          phone: true,
        });
        if (application.sameTab) {
          if (url?.startsWith?.('/')) {
            window.location.href = `${window.location.origin}${url}`;
          } else {
            window.location.href = url;
          }
        } else  if (url?.startsWith?.('/')) {
          window.open(`${window.location.origin}${url}`);
        } else {
          window.open(url);
        }
      } else {
        this.openApplication(application?.type, application?.url);
      }
    },
    async openApplication(appType, appUrl) {
      if (appType === 'PORTLET') {
        this.appLoading = appUrl;
        try {
          await this.$refs.portletInstanceDrawer.open(appUrl);
        } finally {
          window.setTimeout(() => this.appLoading = null, 500);
        }
      } else if (appType === 'DRAWER' && this.$root.quickActions[appUrl]) {
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
    async toogleFavorite(application) {
      this.loading = true;
      try {
        if (application.favorite) {
          await this.$applicationFavoriteService.deleteFavorite(application.id);
        } else {
          await this.$applicationFavoriteService.addFavorite(application.id);
        }
        if (application.favorite) {
          this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.favoriteRemoved'), 'success');
        } else {
          this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.favoriteAdded'), 'success');
        }
        await this.init();
      } finally {
        this.loading = false;
      }
    },
    async tooglePin(application, pin) {
      this.loading = true;
      try {
        if (pin) {
          await this.$applicationPinService.pinApplication(application.id);
          this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.pinAdded'), 'success');
          document.dispatchEvent(new CustomEvent('app-center-application-unpinned', {detail: this.application}));
        } else {
          await this.$applicationPinService.unpinApplication(application.id);
          this.$root.$emit('alert-message', this.$t('appCenter.appLauncher.pinRemoved'), 'success');
          document.dispatchEvent(new CustomEvent('app-center-application-pinned', {detail: this.application}));
        }
        await this.refresh();
      } finally {
        this.loading = false;
      }
    },
    resetFilter() {
      this.filter = 'ALL';
      this.keyword = null;
      if (this.$refs?.appLauncherToolbar) {
        this.$refs.appLauncherToolbar.reset();
      }
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
