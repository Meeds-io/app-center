<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
-->
<template>
  <v-menu
    v-model="menu"
    :position-x="x"
    :position-y="y"
    absolute
    offset-y>
    <v-list dense>
      <v-list-item v-if="canDetach" @click="openInNewTab">
        <v-list-item-title>{{ $t('appCenter.placement.openInNewTab') }}</v-list-item-title>
      </v-list-item>
      <v-list-item v-if="canStick && stuckSide !== 'right'" @click="stickTo('right')">
        <v-list-item-title>{{ $t('appCenter.placement.stickRight') }}</v-list-item-title>
      </v-list-item>
      <v-list-item v-if="canStick && stuckSide !== 'left'" @click="stickTo('left')">
        <v-list-item-title>{{ $t('appCenter.placement.stickLeft') }}</v-list-item-title>
      </v-list-item>
      <v-list-item v-if="stuckSide" @click="unstick">
        <v-list-item-title>{{ $t('appCenter.placement.unstick') }}</v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>
<script>
export default {
  props: {
    application: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    menu: false,
    x: 0,
    y: 0,
    placements: null,
  }),
  computed: {
    canDetach() {
      return this.placements?.enabled && this.application?.allowDetach || false;
    },
    canStick() {
      return this.placements?.enabled && this.placements?.siteEligible && this.application?.allowStick || false;
    },
    stuckSide() {
      if (!this.placements?.enabled || !this.application) {
        return null;
      } else if (`${this.placements.left}` === `${this.application.id}`) {
        return 'left';
      } else if (`${this.placements.right}` === `${this.application.id}`) {
        return 'right';
      }
      return null;
    },
    hasActions() {
      return this.canDetach || this.canStick || !!this.stuckSide;
    },
  },
  created() {
    document.addEventListener('app-placement-changed', this.refreshPlacements);
    this.refreshPlacements();
  },
  beforeDestroy() {
    document.removeEventListener('app-placement-changed', this.refreshPlacements);
  },
  methods: {
    refreshPlacements() {
      if (this.$appPlacementService) {
        this.$appPlacementService.getPlacements(true)
          .then(placements => this.placements = placements);
      }
    },
    open(event) {
      if (!this.hasActions) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      this.x = event.clientX;
      this.y = event.clientY;
      this.menu = true;
    },
    openInNewTab() {
      this.$appPlacementService.openDetached(this.application);
    },
    stickTo(side) {
      this.$appPlacementService.stickApplication(this.application.id, side)
        .catch(this.dispatchPlacementError);
    },
    unstick() {
      this.$appPlacementService.unstickApplication(this.stuckSide)
        .catch(this.dispatchPlacementError);
    },
    dispatchPlacementError() {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertType: 'error',
        alertMessage: this.$t('appCenter.placement.actionError'),
      }}));
    },
  },
};
</script>
