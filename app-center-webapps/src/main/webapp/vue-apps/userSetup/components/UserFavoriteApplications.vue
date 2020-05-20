<template>
  <div class="userFavoriteApplications">
    <div class="favoriteAppTitle">
      {{ $t("appCenter.userSetup.favorite") }}
    </div>

    <v-card
      v-for="(favoriteApp, index) in favoriteApplicationsList"
      :key="index"
      class="favoriteApplication"
      height="65"
      max-width="auto"
      outlined
    >
      <v-list-item>
        <v-list-item-avatar>
          <div class="favoriteAppImage">
            <a :target="favoriteApp.target" :href="favoriteApp.computedUrl">
              <img class="appImage" :src="`/portal/rest/app-center/applications/illustration/${favoriteApp.id}`">
            </a>
          </div>
        </v-list-item-avatar>
        <v-list-item-content>
          <a
            class="favoriteAppUrl"
            :target="favoriteApp.target"
            :href="favoriteApp.computedUrl"
          >
            <h5 class="tooltipContent">
              <div>{{ favoriteApp.title }}</div>
            </h5>
          </a>
        </v-list-item-content>
        <v-spacer></v-spacer>
        <v-list-item-action class="favoriteAppRemove">
          <v-btn
            v-if="!favoriteApp.byDefault"
            icon
            color="red"
            @click.stop="deleteFavoriteApplication(favoriteApp.id)"
          >
            <v-icon>mdi-star</v-icon>
          </v-btn>
        </v-list-item-action>
      </v-list-item>
    </v-card>
    <div v-show="!loading">
      <div v-if="canAddFavorite" class="maxFavorite">
        <v-icon color="#01579B">
          info
        </v-icon>
        <span>{{ $t("appCenter.userSetup.maxFavoriteApps.not.reached", {0: $parent.$children[0].maxFavoriteApps}) }}</span>
      </div>
      <div v-else class="maxFavorite reached">
        <v-icon color="#D84315">
          mdi-alert
        </v-icon>
        <span>{{ $t("appCenter.userSetup.maxFavoriteApps.reached") }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserFavoriteApplications',
  data() {
    return {
      favoriteApplicationsList: [],
      loading: true,
    };
  },

  created() {
    this.getFavoriteApplicationsList();
  },

  methods: {
    getFavoriteApplicationsList() {
      return fetch('/portal/rest/app-center/applications/favorites', {
        method: 'GET',
        credentials: 'include',
      })
        .then(resp => {
          if (resp && resp.ok) {
            return resp.json();
          } else {
            throw new Error('Error when getting the general applications list');
          }
        })
        .then(data => {
          this.favoriteApplicationsList = data && data.applications || [];
          this.favoriteApplicationsList.forEach(app => {
            app.computedUrl = app.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
            app.computedUrl = app.computedUrl.replace('@user@', eXo.env.portal.userName);
            app.target = app.computedUrl.indexOf('/') === 0 ? '_self' : '_blank';
          });
          this.canAddFavorite =
            !this.$parent.$children[0].maxFavoriteApps ||
            this.favoriteApplicationsList.length < this.$parent.$children[0].maxFavoriteApps;
          return this.favoriteApplicationsList;
        }).finally(() => this.loading = false);
    },

    deleteFavoriteApplication(appId) {
      return fetch(`/portal/rest/app-center/applications/favorites/${appId}`, {
        method: 'DELETE',
        credentials: 'include',
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
