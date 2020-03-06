<template>
  <div class="myTools">
    <div class="myToolsHeader">
      <span>{{ $t("appCenter.userSetup.shortcuts") }}</span>
      <a class="seeAll" href="/portal/intranet/appCenterUserSetup">{{
        $t("appCenter.userSetup.seeAll")
      }}</a>
    </div>
    <ul class="myToolsList">
      <li v-for="favoriteApp in favoriteApplicationsList" :key="favoriteApp.id">
        <a :href="favoriteApp.url" target="_blank">
          <img class="myToolImage" :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}`">
          <span class="myToolTitle tooltipContent">
            <dot :msg="favoriteApp.title" :line="2" />
            <span class="tooltiptext">{{ favoriteApp.description }}</span>
          </span>
        </a>
      </li>
    </ul>
    <div
      v-if="
        !maxFavoriteApps || favoriteApplicationsList.length < maxFavoriteApps
      "
      class="addTool">
      <a :href="appCenterUserSetupLink">
        <i class="uiIconPlus uiIconLightGray"></i>
      </a>
    </div>
  </div>
</template>

<script>
import dot from "vue-text-dot";
export default {
  name: "MyTools",
  components: { dot },
  data() {
    return {
      favoriteApplicationsList: [],
      maxFavoriteApps: "",
      appCenterUserSetupLink: ""
    };
  },

  created() {
    this.getFavoriteApplicationsList();
    this.getMaxFavoriteApps();
    this.appCenterUserSetupLink = `${eXo.env.portal.context}/${eXo.env.portal.portalName}/appCenterUserSetup`;
  },

  methods: {
    getFavoriteApplicationsList() {
      return fetch('/portal/rest/app-center/applications/favorites', {
        credentials: "include",
        method: "GET"
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error(
              "Error when getting the favorite applications list"
            );
          }
        })
        .then(data => {
          this.favoriteApplicationsList = (data && data.applications) || [];
        });
    },

    getMaxFavoriteApps() {
      return fetch('/portal/rest/app-center/settings', {
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
          this.maxFavoriteApps = data.maxFavoriteApps;
        });
    }
  }
};
</script>
