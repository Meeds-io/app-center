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
        :key="authorizedApp.appId"
        class="authorizedApplication">
        <div class="authorisedAppContent">
          <div class="applicationHeader">
            <div class="image">
              <a target="_blank" :href="authorizedApp.appUrl">
                <img
                  v-if="authorizedApp.appImageFileBody != undefined && authorizedApp.appImageFileBody != ''"
                  class="appImage"
                  :src="authorizedApp.appImageFileBody">
                <img
                  v-if="authorizedApp.appDefault"
                  class="default"
                  width="30"
                  height="30"
                  src="/app-center/skin/images/defaultApp.png">
              </a>
            </div>
            <div class="title">
              <a target="_blank" :href="authorizedApp.appUrl">
                <h5 v-tooltip.bottom="authorizedApp.appTitle">
                  <dot :msg="authorizedApp.appTitle" :line="2" />
                </h5>
              </a>
            </div>
          </div>
          <div v-tooltip.bottom="authorizedApp.appDescription" class="userAppDescription">
            <dot :msg="authorizedApp.appDescription" :line="4" />
          </div>
          <div class="applicationButton">
            <a target="_blank" :href="authorizedApp.appUrl">{{ $t("appCenter.userSetup.authorized.access") }}</a>
            <button
              :disabled="authorizedApp.appDefault || !authorizedApp.isFavorite && !canAddFavorite"
              :class="authorizedApp.appDefault || authorizedApp.isFavorite ? 'favorite' : ''"
              @click.prevent="addDeleteFavoriteApplication(authorizedApp.appId, index)">
              {{ $t("appCenter.userSetup.authorized.favorite") }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="authorizedApplicationsListMsg != ''" class="noApp">{{ authorizedApplicationsListMsg }}</div>
    </div>
    <div class="appPaginator">
      <button v-if="showPaginator" @click="nextPage()">{{ $t('appCenter.userSetup.authorized.displayMore') }}</button>
    </div>	
  </div>
</template>

<script>
    import dot from 'vue-text-dot'
	import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);   

    export default {
    	name: "UserAuthorizedApplications",
    	components: { dot },
    	data(){
        	return{
        	  authorizedApplicationsList: [],
			  showPaginator: false,
			  currentPage: 1,
			  searchText: '',
			  searchApp: '',
			  searchDelay: 300,
			  maxFavoriteApps: '',
			  authorizedApplicationsListMsg: this.$t("appCenter.userSetup.loading")
        	}
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
        
        methods:{
        	
        	getAuthorizedApplicationsList() {
        		const getAuthorizedApplicationsListUrl = `/rest/appCenter/applications/getAuthorizedApplicationsList?offset=${ this.currentPage - 1 }&limit=${  this.pageSize  }&keyword=${  this.searchText}`;
				return fetch(getAuthorizedApplicationsListUrl, {
					method: 'GET'
				}).then((resp) =>{
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error when getting authorized applications list');
					}
				}).then(data => {
					this.authorizedApplicationsList = this.authorizedApplicationsList.concat(data.applications);
					this.totalApplications = data.totalApplications;
					this.canAddFavorite = data.canAddFavorite;
					if (this.currentPage * this.pageSize < this.totalApplications) {
						this.showPaginator = true;
					}
					else {
						this.showPaginator = false;
					}
					this.authorizedApplicationsListMsg = this.totalApplications == 0 ? this.$t("appCenter.adminSetupForm.noApp") : "";
				})
          	},
          	
         	addDeleteFavoriteApplication(appId, index) {
         		if (!this.authorizedApplicationsList[index].isFavorite) {
	        		const addFavoriteApplicationUrl = `/rest/appCenter/applications/addFavoriteApplication/${  appId}`;
					return fetch(addFavoriteApplicationUrl, {
						method: 'GET'
					}).then((resp) =>{
						if(resp && resp.ok) {
							return resp.json();
						} else {
							throw new Error('Error when deleting a favorite application');
						}
					}).then(data => {
						this.$parent.$children[1].getFavoriteApplicationsList();
						this.canAddFavorite = this.maxFavoriteApps == undefined || data.applications.length < this.maxFavoriteApps;
						this.authorizedApplicationsList[index].isFavorite = true;
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
        		const getGeneralSettingsUrl = "/rest/appCenter/applications/getGeneralSettings";
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
          	
          	searchAuthorizedApplicationsList() {
          		this.authorizedApplicationsList = [];
          		this.getAuthorizedApplicationsList();
          		this.currentPage = 1;
          	}
    	}
    }
</script>
