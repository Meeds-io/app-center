<template>
	<div class="myTools">
		<div class="myToolsHeader">
			<span>{{ $t("appCenter.userSetup.shortcuts") }}</span>
			<a class="seeAll" href="/portal/intranet/appCenterUserSetup">{{ $t("appCenter.userSetup.seeAll") }}</a>
		</div>
		<ul class="myToolsList">
			<li v-for="favoriteApp in favoriteApplicationsList">
				<a :href="favoriteApp.appUrl" target="_blank">
					<img class="myToolImage" v-if="favoriteApp.appImageFileBody != undefined && favoriteApp.appImageFileBody != ''" :src="favoriteApp.appImageFileBody"/>
  					<span v-tooltip.bottom="favoriteApp.appDescription" class="myToolTitle">
  						<dot :msg="favoriteApp.appTitle" :line="2"></dot>
  					</span>
				</a>
			</li>
		</ul>
		<div v-if="maxFavoriteApps == undefined || favoriteApplicationsList.length < maxFavoriteApps" class="addTool">
      		<a href="/portal/intranet/appCenterUserSetup">
	        	<i class="uiIconPlus uiIconLightGray"></i>
			</a>
		</div>
		
 </div>
</template>

<script>
    import dot from 'vue-text-dot'
	import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);    
    export default {
        name: "myTools",
        components: { dot },
        data(){
            return{
            	favoriteApplicationsList: [],
            	maxFavoriteApps: ''
            }
        },

        created() {
        	this.getFavoriteApplicationsList();
        	this.getMaxFavoriteApps();
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
						throw new Error('Error when getting the favorite applications list');
					}
				}).then(data => {
					this.favoriteApplicationsList = data.applications;
				})
        	},
        	
        	getMaxFavoriteApps() {
        		var getGeneralSettingsUrl = "/rest/appCenter/applications/getGeneralSettings";
				return fetch(getGeneralSettingsUrl, {
					method: 'GET'
				}).then((resp) =>{
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error when getting the general applications list');
					}
				}).then(data => {
					this.maxFavoriteApps = data.maxFavoriteApps;
				})
          	},
    	}
 	}
</script>