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
          'border-color': card,
          'transparent': !card && !hover,
          'light-grey-background-color': !card && hover,
        }
      ]"
      :title="application.description"
      class="appLauncherItemContainer fill-height d-flex flex-column align-center justify-center position-relative overflow-hidden border-box-sizing"
      tabindex="0"
      @keydown.space.stop="$emit('toogle-pin', !pinnedApplication)"
      @keydown.enter="openApp"
      @focusin="hover = true"
      @focusout="onFocusOut">
      <v-progress-linear
        v-if="loading"
        class="position-absolute t-0 full-width z-index-two"
        indeterminate />
      <div
        :class="card && 'flex-row align-self-start' || 'flex-column align-self-center align-center'"
        class="d-flex full-width">
        <v-avatar
          :size="imageSize"
          :class="card && 'ms-4'"
          class="d-flex align-center justify-center flex-grow-0 flex-shrink-0 my-1 pa-1"
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
          :class="{
            'align-center justify-start mx-4 my-auto': card,
            'align-center justify-center mt-2 mx-2': !card,
          }"
          min-height="48"
          width="100%"
          class="appLauncherTitle transparent flex-grow-1 flex-shrink-1 d-flex"
          flat>
          <div
            :class="card && 'align-self-center' || 'align-self-start'"
            class="d-flex">
            <span :class="card && 'text-title text-start'" class="text-truncate-2">
              {{ application.title }}
            </span>
            <v-icon
              v-if="application.type === 'LINK' && !application.sameTab"
              :class="card && 'ms-2' || 'ms-1'"
              :size="card && 16 || 12">
              fa-external-link-alt
            </v-icon>
          </div>
        </v-card>
      </div>
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
      <v-card
        v-if="card && !mobile"
        :class="{
          'transition-fast-in-fast-out v-card--reveal mask-color px-2 pt-2 pb-1': !card,
          'px-4 my-2': card,
        }"
        :height="card ? 135 : '100%'"
        width="100%"
        class="d-flex flex-column text-start border-box-sizing"
        flat>
        <div v-if="!card" class="text-truncate white--text">
          {{ application.title }}
        </div>
        <div
          :class="{
            'text-font-small-size white--text': !card,
          }"
          class="text-truncate-3">
          {{ application.description }}
        </div>
        <div 
          class="d-flex align-center mt-auto mb-1 flex-wrap justify-space-between overflow-hidden border-box-sizing"
          role="group">
          <app-center-shortcut
            v-if="application.shortcut && !$root.isMobile && hover"
            :shortcut="application.shortcut"
            class="align-self-md-center align-self-end mb-2 mb-md-0 flex-shrink-0 overflow-hidden" />
          <div class="d-flex align-center justify-end flex-shrink-0 flex-nowrap ms-auto">
            <v-btn
              v-if="application.helpPageURL"
              :title="$t('appCenter.appLauncher.accessHelpPageTooltip')"
              :class="card && 'ms-2'"
              small
              icon
              mouseup.stop="0"
              mousedown.stop="0"
              @click.stop="openHelpPageURL(application.helpPageURL)">
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
              :aria-pressed="pinnedApplication ? 'true' : 'false'"
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
              class="ms-2"
              :aria-pressed="application.favorite ? 'true' : 'false'"
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
        </div>
      </v-card>
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
    mobile: {
      type: Boolean,
      default: false,
    },
    tooltip: {
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
          .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`)
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
      return !this.$root.isMobile && this.canPinApps && this.displayName && !this.card && (this.hover || this.pinnedApplication);
    },
    pinnedApplication() {
      return !!this.$root.pinnedApplicationIds?.find?.(id => id === this.application?.id);
    },
    canPinApps() {
      return this.$root.canPinApps && !this.$root.isMobile;
    },
  },
  methods: {
    openApp(event) {
      if (document.activeElement === event.currentTarget) {
        this.addToRecent();
        this.$emit('open');
      }
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
    onFocusOut(event) {
      if (!this.$el.contains(event.relatedTarget)) {
        this.hover = false;
      }
    },
    openHelpPageURL(helpPageURL) {
      window.open(helpPageURL, '_blank', 'noopener');
    }
  },
};
</script>
