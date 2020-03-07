<template>
  <div class="authorizedApplications">
    <div class="appSearch">
      <input
        v-model="searchText"
        :placeholder="$t('appCenter.adminSetupList.search')"
        type="text">
    </div>
    <div class="userAuthorizedApplications">
      <div
        v-for="(authorizedApp, index) in authorizedApplicationsList"
        :key="authorizedApp.id"
        class="authorizedApplication">
        <div class="authorisedAppContent">
          <div class="applicationHeader">
            <div class="image">
              <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">
                <img class="appImage" :src="`/portal/rest/app-center/applications/illustration/${authorizedApp.id}`">
              </a>
            </div>
            <div class="title">
              <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">
                <h5 class="tooltipContent">
                  <dot :msg="authorizedApp.title" :line="2" />
                  <span class="tooltiptext">{{ authorizedApp.title }}</span>
                </h5>
              </a>
            </div>
          </div>
          <div class="userAppDescription tooltipContent">
            <dot :msg="authorizedApp.description" :line="4" />
            <span class="tooltiptext">{{ authorizedApp.description }}</span>
          </div>
          <div class="applicationButton">
            <a :target="authorizedApp.target" :href="authorizedApp.computedUrl">{{
              $t("appCenter.userSetup.authorized.access")
            }}</a>
            <button
              :disabled="authorizedApp.byDefault || (!authorizedApp.favorite && !canAddFavorite)"
              :class="authorizedApp.byDefault || authorizedApp.favorite ? 'favorite' : ''"
              @click.stop="
                addOrDeleteFavoriteApplication(authorizedApp)
              ">
              {{ $t("appCenter.userSetup.authorized.favorite") }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="!authorizedApplicationsList || authorizedApplicationsList.length" class="noApp">
        {{ $t("appCenter.adminSetupForm.noApp") }}
      </div>
    </div>
    <div class="appPaginator">
      <button v-if="showPaginator" @click="nextPage()">
        {{ $t("appCenter.userSetup.authorized.displayMore") }}
      </button>
    </div>
  </div>
</template>

<script>
import dot from "vue-text-dot";

export default {
  name: "UserAuthorizedApplications",
  components: { dot },
  data() {
    return {
      authorizedApplicationsList: [],
      showPaginator: false,
      currentPage: 1,
      searchText: "",
      searchApp: "",
      searchDelay: 300,
      maxFavoriteApps: "",
      authorizedApplicationsListMsg: this.$t("appCenter.userSetup.loading")
    };
  },

  watch: {
    searchText() {
      if (this.searchText && this.searchText.trim().length) {
        clearTimeout(this.searchApp);
        this.searchApp = setTimeout(() => {
          this.searchAuthorizedApplicationsList();
        }, this.searchDelay);
      } else {
        this.searchAuthorizedApplicationsList();
      }
    }
  },

  created() {
    this.pageSize = this.$parent.pageSize;
    this.getAuthorizedApplicationsList();
    this.getMaxFavoriteApps();
  },

  methods: {
    getAuthorizedApplicationsList() {
      const offset = this.currentPage - 1;
      return fetch(`/portal/rest/app-center/applications/authorized?offset=${offset}&limit=${this.pageSize}&keyword=${this.searchText}`, {
        method: 'GET'
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error("Error when getting authorized applications list");
          }
        })
        .then(data => {
          this.authorizedApplicationsList = this.authorizedApplicationsList.concat(data.applications);
          this.authorizedApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.canAddFavorite = data.canAddFavorite;
          if (this.currentPage * this.pageSize < data.size) {
            this.showPaginator = true;
          } else {
            this.showPaginator = false;
          }
        });
    },
    addOrDeleteFavoriteApplication(application) {
      return fetch(`/portal/rest/app-center/applications/favorites/${application.id}`, {
        credentials: 'include',
        method: application.favorite ? 'DELETE' : 'POST',
      })
        .then(() => {
          return this.$parent.$children[1].getFavoriteApplicationsList();
        })
        .then(data => {
          const applications = (data && data.applications && data.applications.length) || [];
          this.canAddFavorite = !this.maxFavoriteApps || applications.length < this.maxFavoriteApps;
          application.favorite = !application.favorite;
          if (!application.favorite) {
            this.$parent.$children[1].deleteFavoriteApplication(application.id);
          }
        });
    },
    nextPage() {
      this.currentPage++;
      this.getAuthorizedApplicationsList();
    },
    getMaxFavoriteApps() {
      return fetch('/portal/rest/app-center/settings', {
        method: 'GET'
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error("Error when getting the general applications list");
          }
        })
        .then(data => {
          this.maxFavoriteApps = data.maxFavoriteApps;
        });
    },
    searchAuthorizedApplicationsList() {
      this.authorizedApplicationsList = [];
      this.getAuthorizedApplicationsList();
      this.currentPage = 1;
    }
  }
};
</script>
