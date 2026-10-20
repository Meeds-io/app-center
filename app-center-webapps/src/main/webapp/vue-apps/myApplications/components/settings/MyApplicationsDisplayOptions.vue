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
    <template v-for="option in options">
      <!-- The color comes before Outlined, which decides whether it fills or borders the tiles -->
      <div
        v-if="option.key === 'outlined'"
        :key="`${option.key}-color`"
        class="d-flex align-center justify-space-between mt-2">
        <div class="v-label text-color align-start">
          {{ $t('myApplications.color.label') }}
        </div>
        <div class="align-end">
          <color-picker
            :value="display.color"
            :disabled="disabled"
            swatch-size="28px"
            resettable
            @input="update('color', $event)" />
        </div>
      </div>
      <div
        :key="option.key"
        class="d-flex align-center justify-space-between mt-2">
        <div class="v-label text-color align-start">
          {{ $t(option.label) }}
        </div>
        <div class="align-end">
          <v-switch
            :input-value="display[option.key]"
            :disabled="disabled"
            :aria-label="$t(option.label)"
            color="primary"
            class="pa-0 my-auto"
            hide-details
            @change="update(option.key, $event)" />
        </div>
      </div>
      <!-- The size is kept while the icons are hidden -->
      <div
        v-if="option.key === 'showIcon' && display.showIcon"
        :key="`${option.key}-size`"
        class="d-flex align-center justify-space-between mt-2">
        <div class="v-label text-color align-start">
          {{ $t('myApplications.iconSize.label') }}
        </div>
        <!-- number-input reads its value only when created: renewed when the settings are replaced -->
        <number-input
          :key="version"
          :value="display.iconSize"
          :min="minIconSize"
          :max="maxIconSize"
          :step="1"
          :disabled="disabled"
          editable
          @input="update('iconSize', $event)" />
      </div>
    </template>
    <div class="v-label text-color mt-4">
      {{ $t('myApplications.alignment.label') }}
    </div>
    <div class="d-flex">
      <v-radio-group
        v-for="axis in axes"
        :key="axis.key"
        :value="display[axis.key]"
        :disabled="disabled"
        :aria-label="$t(axis.label)"
        class="col-6 mt-2 pa-0"
        hide-details
        mandatory
        @change="update(axis.key, $event)">
        <v-radio
          v-for="alignment in alignments"
          :key="alignment"
          :value="alignment"
          class="ma-0 pa-0">
          <template #label>
            <span class="text-body">{{ $t(axis.alignmentLabels[alignment]) }}</span>
          </template>
        </v-radio>
      </v-radio-group>
    </div>
  </div>
</template>

<script>
import {MAX_ICON_SIZE, MIN_ICON_SIZE} from '../../myApplicationsDisplay.js';

export default {
  props: {
    display: {
      type: Object,
      default: null,
    },
    version: {
      type: Number,
      default: 0,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    minIconSize: MIN_ICON_SIZE,
    maxIconSize: MAX_ICON_SIZE,
    alignments: ['START', 'CENTER', 'END'],
  }),
  computed: {
    options() {
      return [
        {key: 'showSeeMore', label: 'myApplications.showSeeMore.label'},
        // A column lists one app per line
        this.display.template !== 'COLUMN' && {key: 'listInRow', label: 'myApplications.listInRow.label'},
        {key: 'showIcon', label: 'myApplications.showIcons.label'},
        {key: 'showName', label: 'myApplications.showName.label'},
        {key: 'outlined', label: 'myApplications.outlined.label'},
      ].filter(Boolean);
    },
    axes() {
      // The start is the left in a left-to-right page, the right in a right-to-left one
      const start = this.$vuetify.rtl ? 'right' : 'left';
      const end = this.$vuetify.rtl ? 'left' : 'right';
      return [
        {
          key: 'hAlign',
          label: 'myApplications.alignment.horizontal.label',
          alignmentLabels: {
            START: `myApplications.alignment.${start}`,
            CENTER: 'myApplications.alignment.center',
            END: `myApplications.alignment.${end}`,
          },
        },
        {
          key: 'vAlign',
          label: 'myApplications.alignment.vertical.label',
          alignmentLabels: {
            START: 'myApplications.alignment.top',
            CENTER: 'myApplications.alignment.middle',
            END: 'myApplications.alignment.bottom',
          },
        },
      ];
    },
  },
  methods: {
    update(key, value) {
      // number-input emits its min or its max for a size typed beyond them: a size that is not a whole number is not kept
      if (key === 'iconSize' && !(Number.isInteger(value) && value >= MIN_ICON_SIZE && value <= MAX_ICON_SIZE)) {
        return;
      }
      this.$emit('update:display', {...this.display, [key]: value});
    },
  },
};
</script>
