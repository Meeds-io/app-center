<template>
	<div class="userFavoriteApplications">
		<div class="favoritAppTitle">{{ $t("appCenter.userSetup.favorite") }}</div>
		<div :key="favoriteApp.appId" class="favoriteApplication" v-for="favoriteApp in favoriteApplicationsList">
			<div class="favoriteAppImage">
				<a target="_blank" :href="favoriteApp.appUrl">
					<img class="appImage" v-if="favoriteApp.appImageFileBody != undefined && favoriteApp.appImageFileBody != ''" :src="favoriteApp.appImageFileBody"/>
					<img class="default" v-if="favoriteApp.appDefault" width="30" height="30" src="/app-center/skin/images/defaultApp.png"/>
				</a>
			</div>
			<a class="favoriteAppUrl" target="_blank" :href="favoriteApp.appUrl">
				<h5 class="tooltipContent">
					<dot :msg="favoriteApp.appTitle" :line="2"></dot>
					<span class="tooltiptext">{{ favoriteApp.appTitle }}</span>
				</h5>
			</a>
			<div class="favoriteAppRemove">
				<a v-if="!favoriteApp.appDefault" @click.stop="deleteFavoriteApplication(favoriteApp.appId)" class="actionIcon tooltipContent" >
					<i class="uiIconClose uiIconLightGray"></i>
					<span class="tooltiptext tooltiptextIcon">{{ $t('appCenter.adminSetupForm.delete') }}</span>
				</a>
			</div>
		</div>
		<div class="maxFavoriteReached" v-if="!canAddFavorite">
			<img width="13" height="13" src="/app-center/skin/images/Info tooltip.png"/>
			{{ $t("appCenter.userSetup.maxFavoriteApps.reached") }}
		</div>
	</div>	
</template>

<script>
    import dot from 'vue-text-dot'

    export default {
    	name: "userFavoriteApplications",
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
        		var getFavoriteApplicationsListUrl = "/rest/appCenter/applications/getFavoriteApplicationsList";
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
        		var deleteFavoriteApplicationUrl = "/rest/appCenter/applications/deleteFavoriteApplication/" + appId;
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
					var index = this.$parent.$children[0].authorizedApplicationsList.findIndex(app => app.appId == appId);
					this.$parent.$children[0].authorizedApplicationsList[index].isFavorite = false;
					this.$parent.$children[0].canAddFavorite = this.$parent.$children[0].maxFavoriteApps == undefined || data.applications.length < this.$parent.$children[0].maxFavoriteApps;
				})
          	}
    	}
    }
</script>