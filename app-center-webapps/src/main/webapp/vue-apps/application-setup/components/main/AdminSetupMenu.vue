<!--
  This file is part of the Meeds project (https://meeds.io/).

  Copyright (C) 2020 -2025 Meeds Association contact@meeds.io

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
  <component
    v-model="menu"
    :is="$root.isMobile && 'v-bottom-sheet' || 'v-menu'"
    :left="!$vuetify.rtl"
    :right="$vuetify.rtl"
    :content-class="menuId"
    offset-y>
    <template #activator="{ on, attrs }">
      <v-btn
        :aria-label="$t('items.menu.open')"
        icon
        small
        class="mx-auto"
        v-bind="attrs"
        v-on="on">
        <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
      </v-btn>
    </template>
    <v-hover v-if="menu" @input="hoverMenu = $event">
      <v-list
        class="pa-0 fffddd"
        dense
        @mouseout="menu = false"
        @focusout="menu = false">
        <v-subheader v-if="$root.isMobile">
          <div class="d-flex full-width">
            <div class="d-flex flex-grow-1 flex-shrink-1 align-center subtitle-1 text-truncate">
            </div>
            <div class="flex-shrink-0">
              <v-btn
                :aria-label="$t('item.label.closeMenu')"
                icon
                @click="menu = false">
                <v-icon>fa-times</v-icon>
              </v-btn>
            </div>
          </div>
        </v-subheader>
        <v-list-item-group v-model="listItem">
          <v-list-item
            rel="opener"
            dense
            @click="$emit('edit')">
            <v-card
              color="transparent"
              min-width="15"
              class="me-2"
              flat>
              <v-icon size="13">fa-edit</v-icon>
            </v-card>
            <v-list-item-title>
              {{ $t('appCenter.adminSetupForm.edit') }}
            </v-list-item-title>
          </v-list-item>
          <v-tooltip :disabled="!item.system" bottom>
            <template #activator="{ on, attrs }">
              <div
                v-on="on"
                v-bind="attrs">
                <v-list-item
                  :disabled="item.system"
                  dense
                  v-on="!item.system && {
                    click: () => $emit('remove')
                  }">
                  <v-card
                    color="transparent"
                    min-width="15"
                    class="me-2"
                    flat>
                    <v-icon
                      :class="!item.system && 'error--text' || 'disabled--text'"
                      size="13">
                      fa-trash
                    </v-icon>
                  </v-card>
                  <v-list-item-title>
                    <span :class="!item.system && 'error--text' || 'disabled--text'">{{ $t('appCenter.adminSetupForm.modal.delete') }}</span>
                  </v-list-item-title>
                </v-list-item>
              </div>
            </template>
            <span>{{ $t('appCenter.adminSetupForm.modal.system.delete') }}</span>
          </v-tooltip>
        </v-list-item-group>
      </v-list>
    </v-hover>
  </component>
</template>
<script>
export default {
  props: {
    item: {
      type: Object,
      default: null,
    },
  },
  data: () => ({
    menu: false,
    hoverMenu: false,
    listItem: null,
    menuId: `ItemMenu${parseInt(Math.random() * 10000)}`,
  }),
  computed: {
    itemId() {
      return this.item?.id;
    },
    name() {
      return this.$te(this.item?.name) ? this.$t(this.item?.name) : this.item?.name;
    },
  },
  watch: {
    listItem() {
      if (this.menu) {
        this.menu = false;
        this.listItem = null;
      }
    },
    menu() {
      if (this.menu) {
        this.$root.$emit('app-center-admin-menu-opened', this.itemId);
      } else {
        this.$root.$emit('app-center-admin-menu-closed', this.itemId);
      }
    },
    hoverMenu() {
      if (!this.hoverMenu) {
        window.setTimeout(() => {
          if (!this.hoverMenu) {
            this.menu = false;
          }
        }, 200);
      }
    },
  },
  created() {
    this.$root.$on('app-center-admin-menu-opened', this.checkMenuStatus);
    document.addEventListener('click', this.closeMenuOnClick);
  },
  beforeDestroy() {
    this.$root.$off('app-center-admin-menu-opened', this.checkMenuStatus);
    document.removeEventListener('click', this.closeMenuOnClick);
  },
  methods: {
    closeMenuOnClick(e) {
      if (e.target && !e.target.closest(`.${this.menuId}`)) {
        this.menu = false;
      }
    },
    checkMenuStatus(templateId) {
      if (this.menu && templateId !== this.item.id) {
        this.menu = false;
      }
    },
  },
};
</script>