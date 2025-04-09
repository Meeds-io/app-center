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
  <v-card
    v-bind="result.type === 'LINK' && {
      href: applicationUrl,
      target: targetUrl,
    } || {
      loading,
    }"
    v-on="result.type !== 'LINK' && {
      click: openApplication,
    }"
    class="searchApplicationCard d-flex flex-column"
    outlined>
    <div class="image mx-auto">
      <a>
        <v-img
          class="appImage"
          :src="imageUrl"
          width="148"
          height="148">
          <v-icon
            v-if="!imageUrl && result.icon"
            class="ma-auto fill-height full-width"
            size="120">
            {{ result.icon }}
          </v-icon>
        </v-img>
      </a>
    </div>
    <div
      :title="result.title"
      class="mx-auto">
      <a class="headline">
        {{ result.title }}
      </a>
    </div>
    <div
      :title="result.description"
      class="mx-auto text-subtitle text-truncate-2 px-2 pt-2 pb-4">
      {{ result.description }}
    </div>
  </v-card>
</template>
<script>
export default {
  name: 'ApplicationSearchCard',
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    loading: false,
  }),
  computed: {
    imageUrl() {
      return this.result.imageUrl;
    },
    applicationUrl() {
      let computedUrl = this.result.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
      computedUrl = computedUrl.replace('@user@', eXo.env.portal.userName);
      computedUrl = this.$utils.toLinkUrl(computedUrl, {
        urls: true,
        email: true,
        phone: true,
      });
      return computedUrl;
    },
    targetUrl() {
      return this.result.url.indexOf('/') === 0 || this.result.url.indexOf('./') === 0 || this.applicationUrl.indexOf('tel:') === 0 || this.applicationUrl.indexOf('mailto:') === 0 ? '_self' : '_blank';
    },
  },
  methods: {
    openApplication() {
      window.require(['SHARED/QuickActionExtensions'], () => {
        const appType = this.result.type;
        const appUrl = this.result.url;
        if (appType === 'DRAWER') {
          this.$utils.includeExtensions('QuickActionExtension');
          const quickActionExtensions = extensionRegistry.loadExtensions('QuickAction', 'Extension');
          const ext = quickActionExtensions.find(ext => ext.id === appUrl);
          if (ext?.click) {
            this.loading = true;
            try {
              ext.click();
            } finally {
              window.setTimeout(() => this.loading = false, 500);
            }
          }
        }
      });
    },
  },
};
</script>
