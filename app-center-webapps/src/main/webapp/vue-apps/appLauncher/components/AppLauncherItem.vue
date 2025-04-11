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
  <v-hover v-model="hover">
    <v-card
      v-bind="application.type === 'LINK' && {
        href: application.computedUrl,
        target: application.target,
        rel: 'nofollow noreferrer noopener',
      } || {
        loading,
      }"
      v-on="application.type !== 'LINK' && {
        click: () => $emit('open'),
      }"
      class="appLauncherItemContainer transparent fill-height d-flex flex-column align-center justify-center mb-2"
      flat>
      <v-avatar
        class="d-flex align-center justify-center flex-grow-0 flex-shrink-0 my-2"
        size="60"
        tile>
        <img
          v-if="application.imageUrl"
          :src="application.imageUrl"
          class="appLauncherImage"
          referrerpolicy="no-referrer"
          alt="">
        <v-icon
          v-else-if="application.icon"
          size="45"
          class="appLauncherImage d-flex align-center justify-center">
          {{ application.icon }}
        </v-icon>
        <img
          v-else
          class="appLauncherImage"
          referrerpolicy="no-referrer"
          src="/app-center/skin/images/defaultApp.png"
          alt="">
      </v-avatar>
      <div
        :title="application.title"
        class="appLauncherTitle text-truncate-2 flex-grow-1 flex-shrink-1 mt-2 mx-2">
        {{ application.title }}
      </div>
      <v-expand-transition>
        <v-card
          v-if="hover"
          class="d-flex flex-column text-start transition-fast-in-fast-out v-card--reveal mask-color pa-2"
          height="100%"
          flat>
          <div class="text-truncate white--text mb-1">
            {{ application.title }}
          </div>
          <div class="text-font-small-size text-truncate-4 white--text">
            {{ application.description }}
          </div>
          <div class="d-flex justify-end mt-auto">
            <v-btn
              v-if="application.helpPageURL"
              :href="application.helpPageURL"
              small
              icon>
              <v-icon color="white" size="16">fa-question-circle</v-icon>
            </v-btn>
            <v-btn
              :loading="favoriteUpdating"
              :disabled="application.mandatory"
              small
              icon
              @click.prevent.stop="toogleFavorite">
              <v-icon :color="application.favorite && 'yellow'" size="16">
                {{ (application.favorite || application.mandatory) && 'fa-star' || 'far fa-star' }}
              </v-icon>
            </v-btn>
          </div>
        </v-card>
      </v-expand-transition>
    </v-card>
  </v-hover>
</template>
<script>
export default {
  props: {
    application: {
      type: Object,
      default: null,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    hover: false,
    favoriteUpdating: false,
  }),
  methods: {
    toogleFavorite() {
      this.favoriteUpdating = true;
      this.$emit('toogle-favorite');
    },
  },
};
</script>
