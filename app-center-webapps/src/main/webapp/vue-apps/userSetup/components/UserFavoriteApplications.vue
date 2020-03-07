<template>
  <div class="userFavoriteApplications">
    <div class="favoritAppTitle">{{ $t("appCenter.userSetup.favorite") }}</div>
    <div
      v-for="favoriteApp in favoriteApplicationsList"
      :key="favoriteApp.id"
      class="favoriteApplication">
      <div class="favoriteAppImage">
        <a :target="favoriteApp.target" :href="favoriteApp.computedUrl">
          <img class="appImage" :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}`">
        </a>
      </div>
      <a
        class="favoriteAppUrl"
        :target="favoriteApp.target"
        :href="favoriteApp.computedUrl">
        <h5 class="tooltipContent">
          <dot :msg="favoriteApp.title" :line="2" />
          <span class="tooltiptext">{{ favoriteApp.title }}</span>
        </h5>
      </a>
      <div class="favoriteAppRemove">
        <a
          v-if="!favoriteApp.byDefault"
          class="actionIcon tooltipContent"
          @click.stop="deleteFavoriteApplication(favoriteApp.id)">
          <i class="uiIconClose uiIconLightGray"></i>
          <span class="tooltiptext tooltiptextIcon">{{
            $t("appCenter.adminSetupForm.delete")
          }}</span>
        </a>
      </div>
    </div>
    <div v-if="!canAddFavorite" class="maxFavoriteReached">
      <img
        width="13"
        height="13"
        src="/app-center/skin/images/Info tooltip.png">
      {{ $t("appCenter.userSetup.maxFavoriteApps.reached") }}
    </div>
  </div>
</template>

<script>
import dot from "vue-text-dot";

export default {
  name: "UserFavoriteApplications",
  components: { dot },
  data() {
    return {
      favoriteApplicationsList: []
    };
  },

  created() {
    this.getFavoriteApplicationsList();
  },

  methods: {
    getFavoriteApplicationsList() {
      return fetch('/portal/rest/app-center/applications/favorites', {
        method: "GET"
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error("Error when getting the general applications list");
          }
        })
        .then(data => {
          this.favoriteApplicationsList = (data && data.applications) || [];
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.canAddFavorite =
            !this.$parent.$children[0].maxFavoriteApps ||
            this.favoriteApplicationsList.length < this.$parent.$children[0].maxFavoriteApps;
          return this.favoriteApplicationsList;
        });
    },

    deleteFavoriteApplication(appId) {
      return fetch(`/portal/rest/app-center/applications/favorites/${appId}`, {
        method: 'DELETE'
      })
        .then(() => {
          return this.getFavoriteApplicationsList();
        })
        .then(data => {
          const index = this.$parent.$children[0].authorizedApplicationsList.findIndex(
            app => app.id === appId
          );
          this.$parent.$children[0].authorizedApplicationsList[
            index
          ].favorite = false;
          this.$parent.$children[0].canAddFavorite =
            !this.$parent.$children[0].maxFavoriteApps ||
            data.length <
              this.$parent.$children[0].maxFavoriteApps;
        });
    }
  }
};
</script>
