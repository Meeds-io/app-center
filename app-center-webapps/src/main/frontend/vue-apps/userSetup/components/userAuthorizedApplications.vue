<template>
	<div class="authorizedApplications">
		<div class="appSearch">
			<input @input="searchAuthorizedApplicationsList()" v-model="keyword" :placeholder="$t('appCenter.adminSetupList.search')" type="text">
		</div>
		<div class="userAuthorizedApplications">
			<div :key="authorizedApp.appId" class="authorizedApplication" v-for="(authorizedApp, index) in authorizedApplicationsList">
				<div class="authorisedAppContent">
					<div class="applicationHeader">
				   		<div class="image">
							<a target="_blank" :href="authorizedApp.appUrl">
								<img class="appImage" v-if="authorizedApp.appImageFileBody != undefined && authorizedApp.appImageFileBody != ''" :src="authorizedApp.appImageFileBody"/>
								<img class="default" v-if="authorizedApp.appDefault" width="30" height="30" src="/app-center/skin/images/defaultApp.png"/>
							</a>
						</div>
						<div class="title">
							<a target="_blank" :href="authorizedApp.appUrl">
								<h5 v-tooltip.bottom="authorizedApp.appTitle">
									<dot :msg="authorizedApp.appTitle" :line="2"></dot>
								</h5>
							</a>
						</div>
					</div>
					<div class="userAppDescription" v-tooltip.bottom="authorizedApp.appDescription">
						<dot :msg="authorizedApp.appDescription" :line="4"></dot>
					</div>
					<div class="applicationButton">
						<a target="_blank" :href="authorizedApp.appUrl">{{ $t("appCenter.userSetup.authorized.access") }}</a>
						<button :disabled="authorizedApp.appDefault || !authorizedApp.isFavorite && !canAddFavorite" @click.prevent="addDeleteFavoriteApplication(authorizedApp.appId, index)" :class="authorizedApp.appDefault || authorizedApp.isFavorite ? 'favorite' : ''">{{ $t("appCenter.userSetup.authorized.favorite") }}</button>
					</div>
				</div>
			</div>
			<div class="noApp" v-if="authorizedApplicationsListMsg != ''">{{ authorizedApplicationsListMsg }}</div>
		</div>
		<div class="appPaginator">
			<button v-if="showPaginator" @click="nextPage()">{{ $t('appCenter.userSetup.authorized.displayMore') }}</button>
		</div>	
	</div>
</template>

<script>

    import axios from 'axios';
    import dot from 'vue-text-dot'
	import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);   

    export default {
    	name: "userAuthorizedApplications",
    	components: { dot },
    	data(){
        	return{
        		authorizedApplicationsList: [],
        		showPaginator: false,
        		currentPage: 1,
        		keyword: '',
        		maxFavoriteApps: '',
        		authorizedApplicationsListMsg: this.$t("appCenter.userSetup.loading")
        	}
        },

        created() {
       		this.pageSize = this.$parent.pageSize;
        	this.getAuthorizedApplicationsList();
        	this.getMaxFavoriteApps();
        },
        
        methods:{
        	
        	getAuthorizedApplicationsList() {
        		var getAuthorizedApplicationsListUrl = "/rest/appCenter/applications/getAuthorizedApplicationsList?offset="+ (this.currentPage - 1) +"&limit=" + this.pageSize + "&keyword=" + this.keyword;
	        	axios.get(getAuthorizedApplicationsListUrl)
	            .then(response => {
	            	this.authorizedApplicationsList = this.authorizedApplicationsList.concat(response.data.applications);
	            	this.totalApplications = response.data.totalApplications;
	            	this.canAddFavorite = response.data.canAddFavorite;
	            	if (this.currentPage * this.pageSize < this.totalApplications) {
	            		this.showPaginator = true;
	            	}
	            	else {
	            		this.showPaginator = false;
	            	}
	            	this.authorizedApplicationsListMsg = this.totalApplications == 0 ? this.$t("appCenter.adminSetupForm.noApp") : "";
	           	}).catch(e => {
	            })
          	},
          	
         	addDeleteFavoriteApplication(appId, index) {
         		if (!this.authorizedApplicationsList[index].isFavorite) {
	        		var addFavoriteApplicationUrl = "/rest/appCenter/applications/addFavoriteApplication/" + appId;
		        	axios.get(addFavoriteApplicationUrl)
		            .then(response => {
		           		this.$parent.$children[1].getFavoriteApplicationsList();
		           		this.canAddFavorite = this.maxFavoriteApps == undefined || response.data.applications.length < this.maxFavoriteApps;
		           		this.authorizedApplicationsList[index].isFavorite = true;
		           		
		           	}).catch(e => {
		            })
		    	}
		    	else {
		    		this.$parent.$children[1].deleteFavoriteApplication(appId);
		    	}
          	},
          	
          	nextPage() {
          		this.currentPage++;
          		this.getAuthorizedApplicationsList(); 
          	},
          	
          	getMaxFavoriteApps() {
        		var getGeneralSettingsUrl = "/rest/appCenter/applications/getGeneralSettings";
	        	axios.get(getGeneralSettingsUrl)
	            .then(response => {
	            	this.maxFavoriteApps = response.data.maxFavoriteApps;
	           	}).catch(e => {
	            })
          	},
          	
          	searchAuthorizedApplicationsList() {
          		this.authorizedApplicationsList = [];
          		this.getAuthorizedApplicationsList();
          		this.currentPage = 1;
          	}
    	}
    }
</script>
