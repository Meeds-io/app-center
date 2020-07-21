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
  <div class="generalParams">
    <div class="form-container appCenter-form">
      <v-row class="px-12 py-0">
        <v-list-item dense>
          <v-list-item-content>
            <span>
              {{ `${$t("appCenter.adminSetupForm.maxFavoriteApps")} : ${maxFavoriteApps}` }}
            </span>
          </v-list-item-content>
          <v-spacer></v-spacer>
          <v-list-item-action class="setMaxFavorite">
            <v-slider
              v-if="!isMaxFavoriteAppsView"
              v-model="maxFavoriteApps"
              color="blue"
              track-color="grey"
              always-dirty
              min="0"
              max="50"
            >
              <template v-slot:prepend>
                <v-icon
                  color="blue"
                  @click="decrement"
                >
                  mdi-minus
                </v-icon>
              </template>
              <template v-slot:append>
                <v-icon
                  color="blue"
                  @click="increment"
                >
                  mdi-plus
                </v-icon>
              </template>
            </v-slider>
          </v-list-item-action>
          <v-list-item-action class="editMaxFavorite">
            <div
              v-exo-tooltip.bottom.body="$t('appCenter.adminSetupForm.save')"
              class="saveChanges"
            >
              <a
                v-if="!isMaxFavoriteAppsView"
                data-placement="bottom"
                data-container="body"
                @click.stop="setMaxFavoriteApps()"
              >
                <i class="uiIconSave uiIconLightGray"></i>
              </a>
            </div>
            <div
              v-exo-tooltip.bottom.body="$t('appCenter.adminSetupForm.cancel')"
              class="cancelEdit"
            >
              <a
                v-if="!isMaxFavoriteAppsView"
                data-placement="bottom"
                data-container="body"
                @click.stop="isMaxFavoriteAppsView = true"
              >
                <v-icon class="iconClose">
                  close
                </v-icon>
              </a>
            </div>
            <v-btn
              v-if="isMaxFavoriteAppsView"
              icon
              @click.stop="isMaxFavoriteAppsView = false"
            >
              <v-icon>mdi-pencil</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>        
      </v-row>
      <v-row class="px-6 my-2">
        <v-divider></v-divider>
      </v-row>
      <v-row class="px-12 py-0">
        <v-list-item dense>
          <v-list-item-content class="defaultAppImage">
            <span>
              {{ $t("appCenter.adminSetupForm.defaultAppImage") }}
              <img v-if="defaultAppImage.fileBody" class="appImage" :src="`data:image/png;base64,${defaultAppImage.fileBody}`" />
              <img v-else class="appImage" src="/app-center/skin/images/defaultApp.png" />
            </span>
            <p v-if="defaultAppImage.invalidImage" class="errorInput">
              {{ $t("appCenter.adminSetupForm.imageError") }}
            </p>
          </v-list-item-content>
          <v-spacer></v-spacer>
          <v-list-item-action class="setDefaultAppImage">
            <div v-show="!defaultAppImageViewMode
              && !defaultAppImage.fileName &&
              !defaultAppImage.fileBody"
            >
              <label
                v-if="!defaultAppImageViewMode"
                for="defaultAppImageFile"
                class="custom-file-upload"
              >
                <i class="uiDownloadIcon download-icon"></i>{{ $t("appCenter.adminSetupForm.browse") }}
              </label>
              <input
                v-if="!defaultAppImageViewMode"
                id="defaultAppImageFile"
                ref="defaultAppImageFile"
                type="file"
                accept="image/*"
                @change="handleDefaultAppImageFileUpload()"
              >
            </div>
            <div v-show="!defaultAppImageViewMode && defaultAppImage.fileName && defaultAppImage.fileBody">
              <span>
                {{ defaultAppImage.fileName }}
              </span>
              <v-btn
                class="remove-file"
                icon
                @click="removeDefaultAppImageFile"
              >
                <v-icon small>
                  mdi-delete
                </v-icon>
              </v-btn>
            </div>
          </v-list-item-action>
          <v-list-item-action class="editDefaultImage">
            <div
              v-exo-tooltip.bottom.body="$t('appCenter.adminSetupForm.save')"
              class="saveChanges"
            >
              <a
                v-if="!defaultAppImageViewMode"
                data-placement="bottom"
                data-container="body"
                @click.stop="submitDefaultAppImage()"
              >
                <i class="uiIconSave uiIconLightGray"></i>
              </a>
            </div>
            <div
              v-exo-tooltip.bottom.body="$t('appCenter.adminSetupForm.cancel')"
              class="cancelEdit"
            >
              <a
                v-if="!defaultAppImageViewMode"
                data-placement="bottom"
                data-container="body"
                @click.stop="resetDefaultAppImage()"
              >
                <v-icon class="iconClose">
                  close
                </v-icon>
              </a>
            </div>
            <v-btn
              v-if="defaultAppImageViewMode"
              icon
              @click.stop="defaultAppImageViewMode = false"
            >
              <v-icon>mdi-pencil</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>        
      </v-row>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminSetupGeneralParams',
  data() {
    return {
      maxFavoriteApps: 0,
      isMaxFavoriteAppsView: true,
      defaultAppImageViewMode: true,
      defaultAppImage: {
        fileBody: '',
        fileName: '',
        invalidSize: false,
        invalidImage: false
      }
    };
  },
  watch: {
    isMaxFavoriteAppsView() {
      if (this.isMaxFavoriteAppsView) {
        document.getElementsByClassName('tooltip')[0].remove();
      }
    },
    defaultAppImageViewMode() {
      if (this.defaultAppImageViewMode) {
        document.getElementsByClassName('tooltip')[0].remove();
      }
    }
  },
  created() {
    this.getAppGeneralSettings();
  },
  methods: {
    getAppGeneralSettings() {
      return fetch('/portal/rest/app-center/settings', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error getting favorite applications list');
          }
        })
        .then(data => {
          this.maxFavoriteApps = data && data.maxFavoriteApps;
          Object.assign(this.defaultAppImage, data && data.defaultApplicationImage);
        });
    },

    setMaxFavoriteApps() {
      this.$nextTick()
        .then(fetch(`/portal/rest/app-center/settings/maxFavorites?number=${this.maxFavoriteApps}`, {
          method: 'PATCH',
          credentials: 'include',
        })).then(() => {
          this.isMaxFavoriteAppsView = true;
        });
    },

    submitDefaultAppImage() {
      const MAX_FILE_SIZE = 100000;

      if (
        this.defaultAppImage.fileBody === '' &&
        this.defaultAppImage.fileName === ''
      ) {
        this.setDefaultAppImage();
      } else {
        if (this.$refs.defaultAppImageFile.files.length > 0) {
          const reader = new FileReader();
          reader.onload = e => {
            if (!e.target.result.includes('data:image')) {
              this.defaultAppImage.invalidImage = true;
              return;
            }
            if (this.$refs.defaultAppImageFile.files[0].size > MAX_FILE_SIZE) {
              this.defaultAppImage.invalidSize = true;
              return;
            }
            this.defaultAppImage.fileBody = e.target.result;
            this.setDefaultAppImage();
          };
          reader.readAsDataURL(this.$refs.defaultAppImageFile.files[0]);
        } else {
          this.setDefaultAppImage();
        }
      }
    },

    setDefaultAppImage() {
      const setDefaultAppImageUrl = '/portal/rest/app-center/settings/image';
      return fetch(setDefaultAppImageUrl, {
        method: 'PATCH',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          id: this.defaultAppImage && this.defaultAppImage.id,
          fileName: this.defaultAppImage && this.defaultAppImage.fileName,
          fileBody: this.defaultAppImage && this.defaultAppImage.fileBody
        })
      })
        .then(() => {
          this.defaultAppImageViewMode = true;
          this.getAppGeneralSettings();
        })
        .catch(e => {
          throw new Error(
            `Error when setting the default application image ${e}`
          );
        });
    },

    handleDefaultAppImageFileUpload() {
      if (this.$refs.defaultAppImageFile.files.length > 0) {
        this.defaultAppImage.fileName = this.$refs.defaultAppImageFile.files[0].name;
        this.defaultAppImage.invalidSize = false;
        this.defaultAppImage.invalidImage = false;
      } else {
        this.removeDefaultAppImageFile();
      }
    },

    removeDefaultAppImageFile() {
      this.defaultAppImage.fileName = '';
      this.defaultAppImage.fileBody = '';
      this.defaultAppImage.invalidSize = false;
      this.defaultAppImage.invalidImage = false;
      if (this.$refs.defaultAppImageFile.files.length > 0) {
        // remove file from the input
        document.getElementById('defaultAppImageFile').value = '';
      }
    },

    resetDefaultAppImage() {
      this.defaultAppImageViewMode = true;
      this.removeDefaultAppImageFile();
      this.getAppGeneralSettings();
    },
    increment() {
      this.maxFavoriteApps++;
    },
    decrement() {
      this.maxFavoriteApps--;
    },
  }
};
</script>
