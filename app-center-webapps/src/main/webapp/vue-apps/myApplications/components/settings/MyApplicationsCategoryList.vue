<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <div>
    <!-- Not offered while the stored categories load: their arrival would replace a pick -->
    <category-suggester
      v-if="!loading && !capReached"
      v-model="pickedCategoryId"
      object-type="appCenter"
      class="mb-2" />
    <div v-else-if="capReached" class="caption text-sub-title mb-2">
      {{ $t('myApplications.searchCategory.capReached', {0: maxCategories}) }}
    </div>
    <div
      v-for="(category, index) in categories"
      :key="category.id"
      class="d-flex align-center mb-1">
      <span class="text-truncate text-color">{{ category.name }}</span>
      <div class="d-flex flex-shrink-0 ms-auto">
        <v-btn
          :style="!index && 'visibility: hidden'"
          :disabled="loading || !index"
          :title="$t('myApplications.moveUp.tooltip')"
          :aria-label="$t('myApplications.moveUp.tooltip')"
          icon
          small
          @click="move(index, -1)">
          <v-icon size="16" class="icon-default-color">fas fa-arrow-up</v-icon>
        </v-btn>
        <v-btn
          :style="index === categories.length - 1 && 'visibility: hidden'"
          :disabled="loading || index === categories.length - 1"
          :title="$t('myApplications.moveDown.tooltip')"
          :aria-label="$t('myApplications.moveDown.tooltip')"
          icon
          small
          @click="move(index, 1)">
          <v-icon size="16" class="icon-default-color">fas fa-arrow-down</v-icon>
        </v-btn>
        <v-btn
          :title="$t('myApplications.removeCategory.tooltip')"
          :disabled="loading"
          :aria-label="$t('myApplications.removeCategory.tooltip')"
          icon
          small
          @click="remove(index)">
          <v-icon size="16" class="icon-default-color">fas fa-times</v-icon>
        </v-btn>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    categories: {
      type: Array,
      default: () => [],
    },
    hiddenCount: {
      type: Number,
      default: 0,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    maxCategories: 100,
    pickedCategoryId: null,
  }),
  computed: {
    capReached() {
      return this.categories.length + this.hiddenCount >= this.maxCategories;
    },
  },
  watch: {
    pickedCategoryId(categoryId) {
      if (categoryId) {
        this.add(categoryId);
        // Empties the suggester for the next pick
        this.pickedCategoryId = null;
      }
    },
  },
  methods: {
    async add(categoryId) {
      const category = await this.$categoryService.getCategory(categoryId).catch(() => null);
      if (category) {
        // The drawer appends it, once, within the cap: picks read concurrently
        // would overwrite each other through this component's prop
        this.$emit('add', category);
      }
    },
    move(index, offset) {
      const categories = [...this.categories];
      categories.splice(index + offset, 0, categories.splice(index, 1)[0]);
      this.$emit('update:categories', categories);
    },
    remove(index) {
      const categories = [...this.categories];
      categories.splice(index, 1);
      this.$emit('update:categories', categories);
    },
  },
};
</script>
