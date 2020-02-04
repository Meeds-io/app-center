<template>
  <div class="userFavoriteApplications">
    <div class="favoritAppTitle">{{ $t("appCenter.userSetup.favorite") }}</div>
    <div
      v-for="favoriteApp in favoriteApplicationsList"
      :key="favoriteApp.appId"
      class="favoriteApplication">
      <div class="favoriteAppImage">
        <a target="_blank" :href="favoriteApp.appUrl">
          <img
            v-if="favoriteApp.appImageFileBody != undefined && favoriteApp.appImageFileBody != ''"
            class="appImage"
            :src="favoriteApp.appImageFileBody">
          <img
            v-if="favoriteApp.appDefault"
            class="default"
            width="30"
            height="30"
            src="/app-center/skin/images/defaultApp.png">
        </a>
      </div>
      <a
        class="favoriteAppUrl"
        target="_blank"
        :href="favoriteApp.appUrl">
        <h5 v-tooltip.bottom="favoriteApp.appTitle">
          <dot :msg="favoriteApp.appTitle" :line="2" />
        </h5>
      </a>
      <div class="favoriteAppRemove">
        <a
          v-if="!favoriteApp.appDefault"
          v-tooltip.bottom="$t('appCenter.adminSetupForm.delete')"
          class="actionIcon"
          @click.prevent="deleteFavoriteApplication(favoriteApp.appId)">
          <i class="uiIconClose uiIconLightGray"></i>
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
    import dot from 'vue-text-dot'
    import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);   

    export default {
    	name: "UserFavoriteApplications",
    	components: { dot },
    	data(){
        	return{
        		favoriteApplicationsList: []  
        	}
        },

        created() {
        	this.getFavoriteApplicationsList();
        },

        methods:{
        	
        	getFavoriteApplicationsList() {
        		const getFavoriteApplicationsListUrl = "/rest/appCenter/applications/getFavoriteApplicationsList";
				return fetch(getFavoriteApplicationsListUrl, {
					method: 'GET'
				}).then((resp) =>{
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error when getting the general applications list');
					}
				}).then(data => {
					this.favoriteApplicationsList = data.applications;
					this.canAddFavorite = this.$parent.$children[0].maxFavoriteApps == undefined || this.favoriteApplicationsList.length < this.$parent.$children[0].maxFavoriteApps;

				})
          	},
          	
         	deleteFavoriteApplication(appId) {
        		const deleteFavoriteApplicationUrl = `/rest/appCenter/applications/deleteFavoriteApplication/${  appId}`;
				return fetch(deleteFavoriteApplicationUrl, {
					method: 'GET'
				}).then((resp) =>{
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error when deleting a favorite application');
					}
				}).then(data => {
					this.getFavoriteApplicationsList();
					const index = this.$parent.$children[0].authorizedApplicationsList.findIndex(app => app.appId == appId);
					this.$parent.$children[0].authorizedApplicationsList[index].isFavorite = false;
					this.$parent.$children[0].canAddFavorite = this.$parent.$children[0].maxFavoriteApps == undefined || data.applications.length < this.$parent.$children[0].maxFavoriteApps;
				})
          	}
    	}
    }
</script>