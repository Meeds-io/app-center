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
  <ul class="paginator">
    <li class="paginator-item prev-page">
      <button
        :disabled="isInFirstPage"
        type="button"
        @click="onClickPreviousPage"
      >
        <img src="/app-center/skin/images/paginator/previous-page.svg">
      </button>
    </li>

    <li
      v-for="page in pages"
      :key="page.name"
      class="paginator-item"
    >
      <button
        :class="{ isActive: isPageActive(page.name) }"
        :disabled="page.isDisabled"
        type="button"
        @click="onClickPage(page.name)"
      >
        {{ page.name }}
      </button>
    </li>

    <li class="paginator-item next-page">
      <button
        :disabled="isInLastPage"
        type="button"
        @click="onClickNextPage"
      >
        <img src="/app-center/skin/images/paginator/next-page.svg">
      </button>
    </li>
  </ul>
</template>
<script>
export default {
  name: 'Paginator',
  props: {
    maxVisibleButtons: {
      type: Number,
      required: false,
      default: 10
    },
    totalPages: {
      type: Number,
      required: true
    },
    total: {
      type: Number,
      required: true
    },
    perPage: {
      type: Number,
      required: true
    },
    currentPage: {
      type: Number,
      required: true
    }
  },
  computed: {
    startPage() {
      if (this.currentPage === 1) {
        return 1;
      }

      if (this.currentPage === this.totalPages) {
        if (this.totalPages - this.maxVisibleButtons + 1 < 0) {
          return 1;
        } else {
          return this.totalPages - this.maxVisibleButtons + 1;
        }
      }

      if (this.currentPage < this.maxVisibleButtons) {
        return 1;
      } else if (this.totalPages - this.currentPage < this.maxVisibleButtons) {
        return this.totalPages - this.maxVisibleButtons + 1;
      }

      return this.currentPage;
    },
    endPage() {
      return Math.min(
        this.startPage + this.maxVisibleButtons - 1,
        this.totalPages
      );
    },
    pages() {
      const range = [];

      for (let i = this.startPage; i <= this.endPage; i += 1) {
        range.push({
          name: i,
          isDisabled: i === this.currentPage
        });
      }

      return range;
    },
    isInFirstPage() {
      return this.currentPage === 1;
    },
    isInLastPage() {
      return this.currentPage === this.totalPages;
    }
  },
  methods: {
    onClickFirstPage() {
      this.$emit('pagechanged', 1);
    },
    onClickPreviousPage() {
      this.$emit('pagechanged', this.currentPage - 1);
    },
    onClickPage(page) {
      this.$emit('pagechanged', page);
    },
    onClickNextPage() {
      this.$emit('pagechanged', this.currentPage + 1);
    },
    onClickLastPage() {
      this.$emit('pagechanged', this.totalPages);
    },
    isPageActive(page) {
      return this.currentPage === page;
    }
  }
};
</script>
