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
  <div class="listApplications">
    <div class="applicationListHeader">
      <a
        class="actionIcon addApplicationButton tooltipContent"
        data-placement="bottom"
        data-container="body"
        @click.stop="showAddApplicationDrawer"
      >
        <i class="uiIconPlus uiIconLightGray"></i>
        <span>{{ $t("appCenter.adminSetupForm.addNewApp") }}</span>
        <span class="tooltiptext">{{
          $t("appCenter.adminSetupForm.addNewApp")
        }}</span>
      </a>
      <input
        v-model="keyword"
        :placeholder="$t('appCenter.adminSetupList.search')"
        type="text"
        @input="
          currentPage = 1;
          getApplicationsList();
        "
      >
    </div>

    <table class="uiGrid table table-hover table-striped">
      <tr>
        <th>
          {{ $t("appCenter.adminSetupList.picto") }}
        </th>
        <th>
          {{ $t("appCenter.adminSetupList.application") }}
        </th>
        <th class="d-none d-md-table-cell">
          {{ $t("appCenter.adminSetupForm.url") }}
        </th>
        <th class="d-none d-md-table-cell">
          {{ $t("appCenter.adminSetupForm.description") }}
        </th>
        <th class="d-none d-md-table-cell">
          {{ $t("appCenter.adminSetupForm.permissions") }}
        </th>
        <th class="d-none d-sm-table-cell">
          {{ $t("appCenter.adminSetupForm.isMandatory") }}
        </th>
        <th class="d-none d-sm-table-cell">
          {{ $t("appCenter.adminSetupForm.active") }}
        </th>
        <th class="actions">
          {{ $t("appCenter.adminSetupList.actions") }}
        </th>
      </tr>
      <tr v-for="application in applicationsList" :key="application.id">
        <td>
          <img :src="`/portal/rest/app-center/applications/illustration/${application.id}`" />
        </td>
        <td>
          <h5>{{ application.title }}</h5>
        </td>
        <td class="d-none d-md-table-cell">
          <h5>{{ application.url }}</h5>
        </td>
        <td class="d-none d-md-table-cell">
          <h5>{{ application.description }}</h5>
        </td>
        <td class="d-none d-md-table-cell">
          <h5
            v-for="permission in application.permissions"
            :key="permission"
          >
            <span v-if="permission==='any'">*</span>
            <span v-else> {{ permission }}</span>
          </h5>
        </td>
        <td class="d-none d-sm-table-cell">
          <input
            v-model="application.isMandatory"
            disabled="disabled"
            type="checkbox"
          >
        </td>
        <td class="d-none d-sm-table-cell">
          <input
            v-model="application.active"
            disabled="disabled"
            type="checkbox"
          >
        </td>
        <td>
          <a
            class="actionIcon tooltipContent"
            data-placement="bottom"
            data-container="body"
            @click.stop="showEditApplicationDrawer(application)"
          >
            <i class="uiIconEdit uiIconLightGray"></i>
            <span class="tooltiptext tooltiptextIcon">{{
              $t("appCenter.adminSetupForm.edit")
            }}</span>
          </a>
  
          <a
            v-if="!application.system"
            class="actionIcon tooltipContent"
            data-placement="bottom"
            data-container="body"
            @click.stop="toDeleteApplicationModal(application)"
          >
            <i class="uiIconRemove uiIconLightGray"></i>
            <span class="tooltiptext tooltiptextIcon">{{
              $t("appCenter.adminSetupList.remove")
            }}</span>
          </a>
        </td>
      </tr>
    </table>

    <div v-if="!applicationsList.length" class="noApp">
      {{ $t("appCenter.adminSetupForm.noApp") }}
    </div>
    <div
      v-if="totalPages > 1"
      class="applicationsPaginator"
    >
      <paginator
        :current-page="currentPage"
        :per-page="pageSize"
        :total="totalApplications"
        :total-pages="totalPages"
        @pagechanged="onPageChange"
      />
    </div>
    
    <exo-app-center-drawer :applications-drawer="openAppDrawer" :form-array="formArray" @initApps="getApplicationsList" @resetForm="closeDrawer" @closeDrawer="closeDrawer">
      <span v-if="addApplication" class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.createNewApp") }}</span>
      <span v-else class="appLauncherDrawerTitle">{{ $t("appCenter.adminSetupForm.editApp") }}</span>
    </exo-app-center-drawer>

    <transition name="fade">
      <exo-app-center-modal
        v-show="showAddOrEditApplicationModal"
        :title="
          formArray.viewMode
            ? $t('appCenter.adminSetupForm.createNewApp')
            : $t('appCenter.adminSetupForm.editApp')
        "
        @modal-closed="resetForm()"
      >
        <div class="addApplication">
          <div class="form-container appCenter-form">
            <div class="row row-form-items">
              <form>
                <table class="applicationTable">
                  <tr>
                    <td>
                      <span>{{ $t("appCenter.adminSetupForm.title") }}</span>
                    </td>
                    <td>
                      <input
                        v-model="formArray.title"
                        type="text"
                        :readonly="formArray.system"
                        :placeholder="
                          $t('appCenter.adminSetupForm.titlePlaceholder')
                        "
                      >
                      <span class="requiredInput">*</span>
                      <p v-if="!formArray.system && !formArray.title" class="errorInput">
                        {{ $t("appCenter.adminSetupForm.titleError") }}
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <span>{{ $t("appCenter.adminSetupForm.url") }}</span>
                    </td>
                    <td>
                      <input
                        v-model="formArray.url"
                        type="url"
                        :readonly="formArray.system"
                        :placeholder="
                          $t('appCenter.adminSetupForm.urlPlaceholder')
                        "
                      >
                      <span class="requiredInput">*</span>
                      <p v-if="!formArray.system && !validUrl(formArray)" class="errorInput">
                        {{ $t("appCenter.adminSetupForm.urlError") }}
                      </p>
                    </td>
                  </tr>
                  <tr class="uploadImage">
                    <td>
                      <span>{{ $t("appCenter.adminSetupForm.image") }}</span>
                    </td>
                    <td>
                      <label for="file" class="custom-file-upload">
                        <i class="uiDownloadIcon download-icon"></i>
                        {{ $t("appCenter.adminSetupForm.browse") }}
                      </label>
                      <input
                        id="file"
                        ref="file"
                        type="file"
                        accept="image/*"
                        @change="handleFileUpload()"
                      >
                      <div
                        v-if="
                          formArray.imageFileName != undefined &&
                            formArray.imageFileName != ''
                        "
                        class="file-listing"
                      >
                        {{ formArray.imageFileName }}
                        <span class="remove-file" @click="removeFile()">
                          <i class="uiCloseIcon"></i>
                        </span>
                      </div>
                      <p
                        :class="
                          'sizeInfo' + (formArray.invalidSize ? ' error' : '')
                        "
                      >
                        <img
                          width="13"
                          height="13"
                          src="/app-center/skin/images/Info tooltip.png"
                        >
                        {{ $t("appCenter.adminSetupForm.sizeError") }}
                      </p>
                      <p v-if="formArray.invalidImage" class="errorInput">
                        {{ $t("appCenter.adminSetupForm.imageError") }}
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <span>{{
                        $t("appCenter.adminSetupForm.description")
                      }}</span>
                    </td>
                    <td>
                      <textarea
                        v-model="formArray.description"
                        type="text"
                        :placeholder="
                          $t('appCenter.adminSetupForm.description')
                        "
                      ></textarea>
                    </td>
                  </tr>
                  <tr class="application-checkbox">
                    <td>
                      <span>{{
                        $t("appCenter.adminSetupForm.isMandatory")
                      }}</span>
                    </td>
                    <td>
                      <input
                        id="byDefault"
                        v-model="formArray.isMandatory"
                        :disabled="!formArray.active"
                        type="checkbox"
                      >
                      <label for="byDefault"></label>
                    </td>
                  </tr>
                  <tr class="application-checkbox">
                    <td>
                      <span>{{ $t("appCenter.adminSetupForm.active") }}</span>
                    </td>
                    <td>
                      <input
                        id="active"
                        v-model="formArray.active"
                        type="checkbox"
                        @change="onActiveChange()"
                      >
                      <label for="active"></label>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <span>{{
                        $t("appCenter.adminSetupForm.permissions")
                      }}</span>
                    </td>
                    <td>
                      <input id="permissions-suggester" type="text">
                    </td>
                  </tr>
                </table>

                <div class="form-group application-buttons pt-2">
                  <button class="ignore-vuetify-classes btn btn-primary form-submit" @click.stop="submitForm()">
                    {{ $t("appCenter.adminSetupForm.save") }}
                  </button>
                  <button class="ignore-vuetify-classes btn form-reset" @click.stop="resetForm()">
                    {{ $t("appCenter.adminSetupForm.cancel") }}
                  </button>
                </div>
                <div class="requiredField">
                  <span>{{
                    $t("appCenter.adminSetupForm.requiredField")
                  }}</span>
                </div>
                <div v-if="error != ''" class="error">
                  <span>{{ error }}</span>
                </div>
              </form>
            </div>
          </div>
        </div>
      </exo-app-center-modal>
    </transition>

    <transition name="fade">
      <exo-app-center-modal
        v-show="showDeleteApplicationModal"
        :title="$t('appCenter.adminSetupForm.DeleteApp')"
        @modal-closed="closeDeleteModal()"
      >
        <div class="deleteApplication">
          <h3>
            {{ $t("appCenter.adminSetupForm.confirmDelete", {0: formArray.title}) }}
          </h3>
          <div class="form-group application-buttons pt-2">
            <button class="ignore-vuetify-classes btn btn-primary form-submit" @click.stop="deleteApplication()">
              <i class="uiTrashIcon"></i>
              {{ $t("appCenter.adminSetupForm.delete") }}
            </button>
            <button
              class="ignore-vuetify-classes btn form-reset"
              @click.stop="showDeleteApplicationModal = false"
            >
              <i class="uiCloseIcon"></i>
              {{ $t("appCenter.adminSetupForm.cancel") }}
            </button>
          </div>
          <div v-if="error != ''" class="error">
            <span>{{ error }}</span>
          </div>
        </div>
      </exo-app-center-modal>
    </transition>
  </div>
</template>

<script>
import Paginator from './Paginator.vue';

export default {
  name: 'AdminSetup',
  components: {
    Paginator
  },
  props: {
    pageSize: {
      type: Number,
      default: 10,
    },
  },
  data() {
    return {
      keyword: '',
      applicationsList: [],
      formArray: {
        id: 0,
        title: '',
        url: '',
        helpPageURL: '',
        description: '',
        active: true,
        isMandatory: false,
        system: false,
        permissions: [],
        imageFileBody: '',
        imageFileName: '',
        isMobile: true,
        viewMode: true,
        invalidSize: false,
        invalidImage: false
      },
      error: '',
      showAddOrEditApplicationModal: false,
      showDeleteApplicationModal: false,
      currentPage: 1,
      totalApplications: 0,
      totalPages: 0,
      groups: [],
      openAppDrawer: false,
      addApplication: true,
    };
  },

  created() {
    this.getApplicationsList();
    $(document).on('keydown', (event) => {
      if (event.key === 'Escape' && this && this.closeModals) {
        this.closeModals();
      }
    });
  },

  methods: {
    getApplicationsList() {
      const offset = this.currentPage - 1;
      return fetch(`/portal/rest/app-center/applications?offset=${offset}&limit=${this.pageSize}&keyword=${this.keyword}`, {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error(
              'Error when getting the favorite applications list'
            );
          }
        })
        .then(data => {
          this.applicationsList = [];
          this.totalApplications = this.applicationsList.size;
          this.totalPages = Number.parseInt((this.applicationsList.size + this.pageSize - 1) / this.pageSize);
          data.applications.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });

          // A trick to force retrieving img URL again to update illustration
          const REFRESH_TIMEOUT=20;
          window.setTimeout(() => this.applicationsList = data.applications, REFRESH_TIMEOUT);
        });
    },

    onPageChange(page) {
      this.currentPage = page;
      this.getApplicationsList();
    },

    submitForm() {
      const MAX_FILE_SIZE = 100000;

      if (
        this.formArray.title &&
        this.formArray.url &&
        this.validUrl(this.formArray)
      ) {
        if (this.$refs.file && this.$refs.file.files.length > 0) {
          const reader = new FileReader();
          reader.onload = e => {
            if (!e.target.result.includes('data:image')) {
              this.formArray.invalidImage = true;
              return;
            }
            if (this.$refs.file.files[0].size > MAX_FILE_SIZE) {
              this.formArray.invalidSize = true;
              return;
            }
            this.formArray.imageFileBody = e.target.result;
            this.addOrEditApplication();
          };
          reader.readAsDataURL(this.$refs.file.files[0]);
        } else {
          this.addOrEditApplication();
        }
      }
    },

    addOrEditApplication() {
      return fetch('/portal/rest/app-center/applications', {
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        method: this.formArray.id ? 'PUT' : 'POST',
        body: JSON.stringify({
          id: this.formArray.id,
          title: this.formArray.title,
          url: this.formArray.url,
          description: this.formArray.description,
          active: this.formArray.active,
          isMandatory: this.formArray.isMandatory,
          permissions: this.formArray.permissions,
          imageFileBody: this.formArray.imageFileBody,
          imageFileName: this.formArray.imageFileName,
          imageFileId: this.formArray.imageFileId,
        })
      })
        .then(() => {
          this.getApplicationsList();
          this.resetForm();
        })
        .catch(e => {
          const UNAUTHORIZED_ERROR_CODE = 401;
          if (e.response.status === UNAUTHORIZED_ERROR_CODE) {
            this.error = this.$t('appCenter.adminSetupForm.unauthorized');
          } else {
            this.error = this.$t('appCenter.adminSetupForm.error');
          }
        });
    },

    deleteApplication() {
      return fetch(`/portal/rest/app-center/applications/${this.formArray.id}`,{
        method: 'DELETE',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json;
          } else {
            throw new Error('Error when deleting application by id');
          }
        })
        .then(() => {
          this.closeDeleteModal();
          this.currentPage = 1;
          this.getApplicationsList();
        });
    },

    resetForm() {
      this.error = '';
      this.formArray.id = '';
      this.formArray.title = '';
      this.formArray.url = '';
      this.formArray.helpPageURL = '';
      this.formArray.imageFileName = '';
      this.formArray.imageFileBody = '';
      this.formArray.description = '';
      this.formArray.isMandatory = false;
      this.formArray.system = false;
      this.formArray.active = true;
      this.formArray.isMobile = true;
      this.formArray.permissions = [];
      this.formArray.invalidSize = false;
      this.formArray.invalidImage = false;
      this.showAddOrEditApplicationModal = false;
    },

    handleFileUpload() {
      if (this.$refs.file.files.length > 0) {
        this.formArray.imageFileName = this.$refs.file.files[0].name;
        this.formArray.invalidSize = false;
        this.formArray.invalidImage = false;
      } else {
        this.removeFile();
      }
    },

    removeFile() {
      this.formArray.imageFileName = '';
      this.formArray.imageFileBody = '';
      this.formArray.invalidSize = false;
      this.formArray.invalidImage = false;
    },

    showAddApplicationDrawer() {
      this.openAppDrawer = true;
      this.addApplication = true;
      this.formArray.viewMode = true;
      this.initPermissionsSuggester();
    },

    showEditApplicationDrawer(item) {
      this.openAppDrawer = true;
      this.addApplication = false;
      Object.assign(this.formArray, item);
      this.initPermissionsSuggester();
    },

    toDeleteApplicationModal(item) {
      this.showDeleteApplicationModal = true;
      this.formArray.id = item.id;
      this.formArray.title = item.title;
    },

    closeModals() {
      this.closeDeleteModal();
      this.resetForm();
    },

    closeDeleteModal() {
      this.formArray.id = '';
      this.formArray.title = '';
      this.showDeleteApplicationModal = false;
    },
    validUrl(app) {
      const url = app && app.url;
      return app.system || url && (url.indexOf('/portal/') === 0 || url.indexOf('./') === 0 || url.match(/(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\\+~#=]{2,256}/g));
    },
    onActiveChange() {
      if (!this.formArray.active) {
        this.formArray.isMandatory = false;
      }
    },
    closeDrawer() {
      this.resetForm();
      this.openAppDrawer = false;
    },
    initPermissionsSuggester() {
      const permissionsSuggester = $('#permissions-suggester');
      if (permissionsSuggester && permissionsSuggester.length) {
        const component = this;
        const suggesterData = {
          type: 'tag',
          plugins: ['remove_button', 'restore_on_backspace'],
          create: false,
          createOnBlur: false,
          highlight: false,
          openOnFocus: false,
          sourceProviders: ['adminSetup'],
          valueField: 'text',
          labelField: 'text',
          searchField: ['text'],
          closeAfterSelect: true,
          dropdownParent: 'body',
          hideSelected: true,
          renderMenuItem(item, escape) {
            return component.renderMenuItem(item, escape);
          },
          renderItem(item) {
            if (item.text === 'any') {
              return '<div class="item">*</div>';
            } else {
              return `<div class="item">${item.text}</div>`;
            }
          },
          onItemAdd(item) {
            component.addSuggestedItem(item);
          },
          onItemRemove(item) {
            component.removeSuggestedItem(item);
          },
          sortField: [{ field: 'order' }, { field: '$score' }],
          providers: {
            adminSetup: component.findGroups
          }
        };
        permissionsSuggester.suggester(suggesterData);
        $('#permissions-suggester')[0].selectize.clear();
        if (this.formArray.permissions && this.formArray.permissions !== null) {
          for (const permission of this.formArray.permissions) {
            permissionsSuggester[0].selectize.addOption({ text: permission });
            permissionsSuggester[0].selectize.addItem(permission);
          }
        }
      }
    },

    addSuggestedItem(item) {
      if (
        $('#permissions-suggester') &&
        $('#permissions-suggester').length &&
        $('#permissions-suggester')[0].selectize
      ) {
        const selectize = $('#permissions-suggester')[0].selectize;
        item = selectize.options[item];
      }
      if (
        !this.formArray.permissions.find(permission => permission === item.text)
      ) {
        this.formArray.permissions.push(item.text);
      }
    },

    removeSuggestedItem(item) {
      const permissionsSuggester = $('#permissions-suggester');
      for (let i = this.formArray.permissions.length - 1; i >= 0; i--) {
        if (this.formArray.permissions[i] === item) {
          this.formArray.permissions.splice(i, 1);
          permissionsSuggester[0].selectize.removeOption(item);
          permissionsSuggester[0].selectize.removeItem(item);
        }
      }
    },
    findGroups(query, callback) {
      if (!query.length) {
        return callback();
      }
      fetch(`/portal/rest/v1/groups?q=${query}`, { credentials: 'include' })
        .then(resp => resp.json())
        .then(data => {
          const groups = [];
          for (const group of data) {
            groups.push({
              avatarUrl: null,
              text: `*:${group.id}`,
              value: `*:${group.id}`,
              type: 'group'
            });
          }
          callback(groups);
        });
    },
    renderMenuItem(item, escape) {
      return `<div class="item">${escape(item.value)}</div>`;
    }
  }
};
</script>
