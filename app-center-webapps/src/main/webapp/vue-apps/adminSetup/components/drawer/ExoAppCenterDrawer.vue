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
  <v-navigation-drawer
    v-model="applicationsDrawer"
    absolute
    right
    stateless
    temporary
    width="420"
    max-width="100vw"
    max-height="100vh"
    class="appCenterDrawer"
  >
    <v-row class="mx-0 title">
      <v-list-item class="applicationsDrawerHeader">
        <v-list-item-content>
          <slot></slot>
        </v-list-item-content>
        <v-list-item-action class="applicationsDrawerIcons">
          <v-btn
            icon
            class="rightIcon"
            @click="$emit('closeDrawer')"
          >
            <v-icon
              large
              class="closeIcon"
            >
              close
            </v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-row>

    <v-divider class="my-0 appHeaderBorder" />

    <div class="content">
    </div>

    <v-row class="drawerActions mx-0 px-3">
      <v-card
        flat
        tile
        class="d-flex flex justify-end mx-2"
      >
        <button type="button" class="btn ml-2 applicationsActionBtn">
          {{ $t('appCenter.adminSetupForm.cancel') }}
        </button>
        <button type="button" class="btn btn-primary ml-6 applicationsActionBtn">
          {{ $t('appCenter.adminSetupForm.save') }}
        </button>
      </v-card>
    </v-row>
  </v-navigation-drawer>
</template>

<script>
export default {
  props: {
    applicationsDrawer: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    modalClass: {
      type: String,
      default: ''
    }
  },
  watch: {
    applicationsDrawer() {
      if (this.applicationsDrawer) {
        $('body').addClass('hide-scroll');
        this.$nextTick().then(() => {
          $('#app .v-overlay').click(() => {
            this.$emit('closeDrawer');
          });
        });
      } else {
        $('body').removeClass('hide-scroll');
      }
    }, 
  },
  methods: {
  },
};
</script>
