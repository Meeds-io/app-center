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
      v-bind="application.type === 'LINK' && !readonly && {
        href: computedUrl,
        target: target,
        rel: 'nofollow noreferrer noopener',
      }"
      v-on="application.type === 'LINK' && {
        click: addToRecent,
      } || {
        click: openApp,
      }"
      :min-width="minWidth"
      :max-width="maxWidth"
      :min-height="minHeight"
      :max-height="maxHeight"
      :elevation="elevation"
      :class="[
        $attrs.class,
        {
          'flex-wrap': card,
          'flex-column': !card,
          'border-color': card,
          'transparent': !card,
        }
      ]"
      class="appLauncherItemContainer fill-height d-flex align-center justify-center">
      <v-progress-linear
        v-if="loading"
        class="position-absolute t-0 full-width z-index-two"
        indeterminate />
      <v-avatar
        :size="imageSize"
        :class="card && 'ms-4'"
        class="d-flex align-center justify-center flex-grow-0 flex-shrink-0 my-2"
        tile>
        <v-img
          v-if="application.imageUrl"
          :src="application.imageUrl"
          :max-height="imageSize"
          :max-width="imageSize"
          class="appLauncherImage"
          contain />
        <v-icon
          v-else-if="application.icon"
          :size="iconSize"
          class="appLauncherImage d-flex align-center justify-center">
          {{ application.icon }}
        </v-icon>
        <v-img
          v-else
          :max-height="imageSize"
          :max-width="imageSize"
          src="/app-center/skin/images/defaultApp.png"
          class="appLauncherImage"
          contain />
      </v-avatar>
      <v-card
        v-if="displayName"
        :title="application.title"
        :class="{
          'd-flex justify-start align-center ma-4': card,
          'mt-2 mx-2': !card,
        }"
        min-height="48"
        class="appLauncherTitle transparent text-truncate-2 flex-grow-1 flex-shrink-1"
        flat>
        <span :class="card && 'font-weight-bold'">
          {{ application.title }}
        </span>
        <v-icon
          v-if="application.type === 'LINK' && !application.sameTab"
          :class="card && 'ms-2' || 'ms-1'"
          size="12">
          fa-external-link-alt
        </v-icon>
      </v-card>
      <v-tooltip v-if="displayPinButton" bottom>
        <template #activator="{on, attrs}">
          <div
            :class="$vuetify.rtl && 'l-0' || 'r-0'"
            class="position-absolute z-index-two t-0 mt-1 me-5">
            <v-btn
              v-bind="attrs"
              v-on="on"
              class="white"
              elevation="2"
              x-small
              icon
              @click.stop.prevent="$emit('toogle-pin', !pinnedApplication)">
              <v-icon size="12">fa-thumbtack</v-icon>
            </v-btn>
          </div>
        </template>
        <span>{{ pinnedApplication && $t('appCenter.appLauncher.unpinApplication') || $t('appCenter.appLauncher.pinApplication') }}</span>
      </v-tooltip>
      <v-expand-transition v-if="!$root.isMobile && (displayDescription || card)">
        <v-card
          v-if="hover || card"
          :class="{
            'transition-fast-in-fast-out v-card--reveal mask-color px-2 pt-2 pb-1': !card,
            'px-4 py-2': card,
          }"
          :height="card ? 135 : '100%'"
          width="100%"
          class="d-flex flex-column text-start"
          flat>
          <div v-if="!card" class="text-truncate white--text">
            {{ application.title }}
          </div>
          <div
            :class="{
              'text-font-small-size white--text': !card,
            }"
            class="text-truncate-4">
            {{ application.description }}
          </div>
          <div class="d-flex align-center mt-auto">
            <app-center-shortcut
              v-if="application.shortcut"
              :shortcut="application.shortcut"
              class="align-md-center align-self-end mb-2 mb-md-0"
              small />
            <v-spacer />  
            <v-btn
              v-if="application.helpPageURL"
              :href="application.helpPageURL"
              :title="$t('appCenter.appLauncher.accessHelpPageTooltip')"
              :class="card && 'ms-2'"
              small
              icon
              mouseup.stop="0"
              mousedown.stop="0"
              click.stop="0">
              <v-icon
                :color="!card && 'white'"
                :size="card && 20 || 16">
                fa-question-circle
              </v-icon>
            </v-btn>
            <v-btn
              v-if="card && canPinApps"
              :title="pinnedApplication && $t('appCenter.appLauncher.unpinApplication') || $t('appCenter.appLauncher.pinApplication')"
              class="ms-2"
              small
              icon
              mouseup.stop="0"
              mousedown.stop="0"
              @click.stop.prevent="$emit('toogle-pin', !pinnedApplication)">
              <v-icon
                :class="!pinnedApplication && 'fa-rotate-45'"
                size="20">
                fa-thumbtack
              </v-icon>
            </v-btn>
            <v-btn
              :disabled="application.mandatory"
              :title="application.favorite ? $t('appCenter.appLauncher.removeFavoriteTooltip') : $t('appCenter.appLauncher.addFavoriteTooltip')"
              :class="card && 'ms-2'"
              small
              icon
              @click.prevent.stop="$emit('toogle-favorite')">
              <v-icon
                :color="(application.favorite || readonly) && 'yellow'"
                :size="card && 20 || 16">
                {{ (application.favorite || application.mandatory || readonly) && 'fa-star' || 'far fa-star' }}
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
    displayName: {
      type: Boolean,
      default: false,
    },
    displayDescription: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    minWidth: {
      type: String,
      default: () => 'auto',
    },
    maxWidth: {
      type: String,
      default: () => 'auto',
    },
    minHeight: {
      type: Number,
      default: () => 80,
    },
    maxHeight: {
      type: Number,
      default: () => 137,
    },
    imageSize: {
      type: Number,
      default: () => 60,
    },
    iconSize: {
      type: Number,
      default: () => 45,
    },
    elevate: {
      type: Boolean,
      default: false,
    },
    card: {
      type: Boolean,
      default: false,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    hover: false,
  }),
  computed: {
    computedUrl() {
      if (this.application.type === 'LINK') {
        const computedUrl = this.application.url
          .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`)
          .replace('@user@', eXo.env.portal.userName);
        return this.$utils.toLinkUrl(computedUrl, {
          urls: true,
          email: true,
          phone: true,
        });
      } else {
        return null;
      }
    },
    target() {
      return this.application.sameTab ? '_self' : '_blank';
    },
    elevation() {
      return this.hover && this.elevate ? 2 : 0;
    },
    displayPinButton() {
      return !this.$root.isMobile && this.canPinApps && this.displayDescription && !this.card && (this.hover || this.pinnedApplication);
    },
    pinnedApplication() {
      return !!this.$root.pinnedApplicationIds?.find?.(id => id === this.application?.id);
    },
    canPinApps() {
      return this.$root.canPinApps;
    },
  },
  methods: {
    openApp() {
      this.addToRecent();
      this.$emit('open');
    },
    addToRecent() {
      if (this.readonly) {
        return;
      }
      const recentAppIdsString = window.localStorage.getItem(`meeds-app-center-recent-apps-${eXo.env.portal.userIdentityId}`);
      if (!recentAppIdsString?.length) {
        window.localStorage.setItem(`meeds-app-center-recent-apps-${eXo.env.portal.userIdentityId}`, JSON.stringify([this.application.id]));
      } else {
        const recentAppIds = JSON.parse(recentAppIdsString);
        if (recentAppIds.includes(this.application.id)) {
          recentAppIds.splice(recentAppIds.indexOf(this.application.id), 1);
        }
        recentAppIds.unshift(this.application.id);
        window.localStorage.setItem(`meeds-app-center-recent-apps-${eXo.env.portal.userIdentityId}`, JSON.stringify(recentAppIds));
      }
    },
  },
};
</script>
