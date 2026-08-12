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
  <v-card
    :loading="loading"
    class="application-body"
    flat>
    <v-toolbar
      class="border-box-sizing"
      flat>
      <v-btn
        height="36"
        width="36"
        icon
        @click="$emit('back')">
        <v-icon size="20">
          {{ $vuetify.rtl && 'fa fa-arrow-right' || 'fa fa-arrow-left' }}
        </v-icon>
      </v-btn>
      <v-toolbar-title class="ps-2 text-title">
        {{ $t('appCenter.userSettings.shortcuts.title') }}
      </v-toolbar-title>
    </v-toolbar>
    <div v-if="topbarApplications?.length" class="px-4 pb-5">
      <div class="text-header">
        {{ $t('appCenter.userSettings.shortcuts.topbarApps') }}
      </div>
      <app-center-user-settings-item
        v-for="app in staticTopbarApplications"
        :key="app.id"
        :application="app"
        class="mt-2" />
      <app-center-user-settings-item
        v-for="app in topbarApplications"
        :key="app.id"
        :application="app"
        class="mt-2" />
    </div>
    <div v-if="defaultApplications?.length" class="px-4 pb-5">
      <div class="text-header">
        {{ $t('appCenter.userSettings.shortcuts.defaultApps') }}
      </div>
      <app-center-user-settings-item
        v-for="app in defaultApplications"
        :key="app.id"
        :application="app"
        class="mt-2" />
    </div>
    <div v-if="otherApplications?.length" class="px-4 pb-5">
      <div class="text-header">
        {{ $t('appCenter.userSettings.shortcuts.otherApps') }}
      </div>
      <app-center-user-settings-item
        v-for="app in otherApplications"
        :key="app.id"
        :application="app"
        class="mt-2" />
    </div>
    <div v-if="editorApplications?.length" class="px-4 pb-5">
      <div class="text-header">
        {{ $t('appCenter.userSettings.shortcuts.editorApps') }}
      </div>
      <app-center-user-settings-item
        v-for="app in editorApplications"
        :key="app.id"
        :application="app"
        class="mt-2"
        editor />
    </div>
  </v-card>
</template>
<script>
export default {
  data: () => ({
    loading: false,
    applications: null,
    editorLinkPlugins: null,
    editorExtensionPlugins: null,
    topbarConfiguration: null,
  }),
  computed: {
    topbarApplicationIds() {
      return this.topbarConfiguration?.applications
        ?.map?.(app => app?.properties?.applicationId && Number(app?.properties?.applicationId))
        ?.filter?.(id => id)
        || [];
    },
    shortcutApplications() {
      const apps = this.applications?.slice?.() || [];
      apps.forEach(app => {
        if (app.system) {
          const title = /\s/.test(app.title) ? app.title.replace(/ /g,'.').toLowerCase() : app.title.toLowerCase();
          if (this.$te(`appCenter.system.application.${title}`)) {
            app.title = this.$t(`appCenter.system.application.${title}`);
            if (this.$te(`appCenter.system.application.${title}.description`) && !app.description?.length) {
              app.description = this.$t(`appCenter.system.application.${title}.description`);
            }
          }
        }
      });
      apps.push({
        title: this.$t('appCenter.system.application.search'),
        icon: 'fa-search',
        shortcut: 'f',
      });
      apps.push({
        title: this.$t('appCenter.system.application.notifications'),
        icon: 'fa-bell',
        shortcut: 'e',
      });
      apps.push({
        title: this.$t('appCenter.system.application.appcenter'),
        icon: 'fa-th',
        shortcut: 'a',
      });
      apps.sort((a, b) => this.$root.collator.compare(a.title.toLowerCase(), b.title.toLowerCase()));
      return apps.filter(app => app.shortcut?.length);
    },
    topbarApplications() {
      return this.shortcutApplications?.filter?.(app => !app.id || this.topbarApplicationIds.includes(app.id));
    },
    defaultApplications() {
      return this.shortcutApplications?.filter?.(app => app.id && app.default && !this.topbarApplicationIds.includes(app.id));
    },
    otherApplications() {
      return this.shortcutApplications?.filter?.(app => app.id && !app.default && !this.topbarApplicationIds.includes(app.id));
    },
    editorApplications() {
      return [
        ...(this.editorLinkPlugins || []),
        ...(this.editorExtensionPlugins || []),
      ]
        .sort(this.editorComparator)
        .map(l => ({
          title: this.$t(l.titleKey),
          icon: l.icon,
          shortcut: l.command,
        }));
    },
  },
  created() {
    this.init();
  },
  methods: {
    async init() {
      await Promise.all([
        this.refreshApplications(),
        this.refreshTopbarConfiguration(),
        this.refreshEditorLinkPlugins(),
      ]);
    },
    async refreshApplications() {
      const data = await this.$applicationService.getApplications(false, true);
      if (data?.applications) {
        this.applications = data?.applications || [];
      } else {
        this.applications = [];
      }
    },
    async refreshTopbarConfiguration() {
      this.topbarConfiguration = await this.getTopbarConfiguration();
    },
    refreshEditorLinkPlugins() {
      return new Promise(resolve => {
        window.require(['SHARED/ContentLink'], () => {
          this.$contentLinkService.getExtensions()
            .then(extensions => this.editorLinkPlugins = extensions)
            .then(() => this.$utils.includeExtensions('ContentLinkExtension'))
            .then(() => this.editorExtensionPlugins = extensionRegistry.loadExtensions('ContentLink', 'InsertContentExtension'))
            .then(resolve);
        });
      });
    },
    getTopbarConfiguration() {
      return fetch('/social/rest/navigation/settings/topbar', {
        method: 'GET',
        credentials: 'include',
      }).then((resp) => {
        if (resp?.ok) {
          return resp.json();
        } else {
          throw new Error('Error when retrieving Topbar configuration');
        }
      });
    },
    editorComparator(a, b) {
      return this.$root.collator.compare(this.$t(a.titleKey), this.$t(b.titleKey));
    },
  }
};
</script>