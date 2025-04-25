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
  <div class="d-flex flex-nowrap align-center">
    <app-center-icon
      :icon-size="iconSize"
      :icon-url="iconUrl"
      :icon="icon"
      class="flex-grow-0 flex-shrink-0 me-4" />
    <font-icon-input
      v-model="icon"
      class="my-auto me-4"
      button-label="appCenter.adminSetupForm.button.chooseIcon"
      no-label
      no-icon />
    <div class="position-relative overflow-hidden">
      <v-file-input
        v-if="!resetInput"
        id="iconFileInput"
        ref="iconFileInput"
        accept="image/*"
        class="position-absolute t-0 l-0 full-width pa-0 ma-0"
        prepend-icon=""
        hide-details
        hide-input
        @change="uploadFile" />
      <v-btn
        :loading="sending"
        class="position-relative z-index-two btn primary"
        border
        outlined
        @click="openFileUpload">
        {{ $t('appCenter.adminSetupForm.upload') }}
      </v-btn>
    </div>
    <v-btn
      v-if="!sending && !isDefault"
      :title="$t('appCenter.adminSetupForm.resetDefault')"
      class="ms-2"
      icon
      @click="reset">
      <v-icon size="24">fa-undo</v-icon>
    </v-btn>
  </div>
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
    imageData: null,
    iconSize: 34,
    maxFileSize: 102400,
    sending: false,
    resetInput: false,
    icon: null,
    initialized: false,
  }),
  computed: {
    iconUrl() {
      if (this.imageData) {
        return this.$utils.convertImageDataAsSrc(this.imageData);
      } else {
        return this.application?.imageUrl;
      }
    },
    isDefault() {
      return !this.application?.iconSrc && !this.application?.iconUrl;
    },
  },
  watch: {
    icon() {
      if (this.initialized) {
        if (this.icon) {
          this.reset();
        }
        this.$emit('icon', this.icon);
      }
    },
  },
  created() {
    if (this.application.imageUrl) {
      this.$emit('icon', null);
    }
  },
  async mounted() {
    this.icon = this.application.icon;
    await this.$nextTick();
    this.initialized = true;
  },
  methods: {
    reset() {
      this.$emit('reset');
      this.sending = false;
      if (this.$refs.iconFileInput) {
        this.$emit('input', null);
        this.imageData = null;
        this.resetInput = true;
        this.$nextTick().then(() => this.resetInput = false);
      }
    },
    openFileUpload() {
      this.$refs.iconFileInput.$el.querySelector('input').click();
    },
    uploadFile(file) {
      this.$root.$emit('close-alert-message');
      if (file && file.size) {
        if (file.size > this.maxFileSize) {
          this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.tooBigFile.label'), 'error');
          return;
        }
        this.sending = true;
        const self = this;
        return this.$uploadService.upload(file)
          .then(uploadId => {
            if (uploadId) {
              this.$emit('input', uploadId);
              const reader = new FileReader();
              reader.onload = (e) => {
                self.imageData = e.target.result;
                self.$forceUpdate();
              };
              reader.readAsDataURL(file);
            } else {
              this.$root.$emit('alert-message', this.$t('appCenter.adminSetupForm.uploadingError'), 'error');
            }
          })
          .catch(error => this.$root.$emit('alert-message', this.$t(String(error)), 'error'))
          .finally(() => this.sending = false);
      }
    },
  },
};
</script>