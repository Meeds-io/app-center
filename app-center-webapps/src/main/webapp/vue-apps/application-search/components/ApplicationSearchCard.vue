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
  <v-card class="searchApplicationCard d-flex flex-column" outlined>
    <div class="image mx-auto">
      <a 
        :target="targetUrl"
        :href="applicationUrl"
        @click="logOpenApplication(id)">
        <v-img
          class="appImage"
          :src="imageUrl"
          width="148"
          height="148" />
      </a>
    </div>
    <div class="mx-auto">
      <a
        class="headline"
        :title="result.title"
        :target="targetUrl"
        :href="applicationUrl"
        @click="logOpenApplication(id)">
        {{ result.title }}
      </a>
    </div>
    <div
      :title="result.description"
      class="mx-auto text-sub-title pt-2 pb-4">
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
  computed: {
    imageUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.rest}/app-center/applications/illustration/${this.result.id}?v=${this.result.imageLastModified}`;
    },
    applicationUrl() {
      const computedUrl = this.result.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
      return computedUrl.replace('@user@', eXo.env.portal.userName);
    },
    targetUrl() {
      return this.applicationUrl.indexOf('/') === 0 ? '_self' : '_blank';
    },
  },
  methods: {
    logOpenApplication() {
      fetch(`/portal/rest/app-center/applications/logClickApplication/${this.result.id}`, {
        method: 'GET',
        credentials: 'include',
      });
    },
  }
};
</script>
