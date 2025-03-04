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
  <v-app v-if="hasPinnedApps">
    <div class="d-flex align-center justify-center">
      <v-tooltip
        v-for="action in $root.pinnedQuickActions"
        :key="action.id"
        bottom>
        <template #activator="{on, attrs}">
          <v-btn
            v-on="on"
            v-bind="attrs"
            icon
            @click="clickQuickAction(action)">
            <v-icon size="20">{{ action.icon }}</v-icon>
          </v-btn>
        </template>
        <span>{{ $t(action.name) }}</span>
      </v-tooltip>
    </div>
  </v-app>
</template>
<script>
export default {
  computed: {
    hasPinnedApps() {
      return this.$root.pinnedQuickActions?.length;
    },
  },
  methods: {
    clickQuickAction(action) {
      action.click();
    },
  },
};
</script>
