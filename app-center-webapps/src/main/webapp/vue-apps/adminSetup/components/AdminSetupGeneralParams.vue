<template>
	<div class="generalParams">
		<div class="form-container appCenter-form">
			<div class="maxFavoriteApps">
				<span>{{ $t("appCenter.adminSetupForm.maxFavoriteApps") }}</span>
				<span v-if="isMaxFavoriteAppsView && maxFavoriteApps != ''">
					{{ maxFavoriteApps }}
				</span>
					
				<input v-if="!isMaxFavoriteAppsView" type="number" min="0" onkeypress="return event.charCode >= 48 && event.charCode <= 57" v-model="maxFavoriteApps">

				<a v-if="isMaxFavoriteAppsView" @click.stop="isMaxFavoriteAppsView = false" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.edit")' data-placement="bottom" data-container="body">
			     	<i class="uiIconEdit uiIconLightGray"></i>
			   	</a>
			  	<a v-if="!isMaxFavoriteAppsView" @click.stop="setMaxFavoriteApps()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.save")' data-placement="bottom" data-container="body">
			   		<i class="uiIconSave uiIconLightGray"></i>
			    </a>
			    <a v-if="!isMaxFavoriteAppsView" @click.stop="isMaxFavoriteAppsView = true" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.cancel")' data-placement="bottom" data-container="body">
			    	<i class="uiIconClose uiIconLightGray"></i>
			 	</a>
			</div>
				
			<div class="defaultAppImage">
				<span>{{ $t("appCenter.adminSetupForm.defaultAppImage") }}</span>
				<img class="appImage" v-if="defaultAppImage.isView && defaultAppImage.fileBody != ''" :src="defaultAppImage.fileBody"/>
					
				<label v-if="!defaultAppImage.isView" for="defaultAppImageFile" class="custom-file-upload">
			  		<font-awesome-icon icon="download" class="download-icon"/> {{ $t("appCenter.adminSetupForm.browse") }}
			  	</label>
			  	<input v-if="!defaultAppImage.isView" id="defaultAppImageFile" type="file" accept="image/*" ref="defaultAppImageFile" @change="handleDefaultAppImageFileUpload()" />
				<div v-if="!defaultAppImage.isView && defaultAppImage.fileName != undefined && defaultAppImage.fileName != ''" class="file-listing">
					{{ defaultAppImage.fileName }}
					<span class="remove-file" @click="removeDefaultAppImageFile()">
				   		<font-awesome-icon icon="times"/>
				  	</span>
			  	</div>
			  	<a v-if="defaultAppImage.isView" @click.stop="defaultAppImage.isView = false" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.edit")' data-placement="bottom" data-container="body">
			   		<i class="uiIconEdit uiIconLightGray"></i>
			   	</a>
			   	<a v-if="!defaultAppImage.isView" @click.stop="submitDefaultAppImage()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.save")' data-placement="bottom" data-container="body">
			   		<i class="uiIconSave uiIconLightGray"></i>
			   	</a>
			   	<a v-if="!defaultAppImage.isView" @click.stop="resetDefaultAppImage()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.cancel")' data-placement="bottom" data-container="body">
			    	<i class="uiIconClose uiIconLightGray"></i>
			  	</a>
			  	<p :class="'errorInput' + (defaultAppImage.invalidSize ? '' : ' sizeInfo')">
					<img width="13" height="13" src="/app-center/skin/images/Info tooltip.png"/>
					{{ $t('appCenter.adminSetupForm.sizeError') }}
				</p>
				<p v-if="defaultAppImage.invalidImage" class="errorInput">{{ $t('appCenter.adminSetupForm.imageError') }}</p>
			</div>
		</div>
	</div>	
</template>

<script>
   	import { library } from '@fortawesome/fontawesome-svg-core'
    import { faExclamationCircle, faDownload, faTimes } from '@fortawesome/free-solid-svg-icons'
    import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
	import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);

    library.add(faExclamationCircle, faDownload, faTimes)

    Vue.component('font-awesome-icon', FontAwesomeIcon)
    
    export default {
    	name: "adminSetupGeneralParams",
    	data(){
        	return{
              	maxFavoriteApps: '',
              	isMaxFavoriteAppsView: true,
              	defaultAppImage: {
              		fileBody: '',
                	fileName: '',
                	isView: true,
                	invalidSize: false,
                	invalidImage: false
              	}
            }
        },

        created() {
        	this.getAppGeneralSettings();
        },

        methods:{
        	getAppGeneralSettings() {
        		var getGeneralSettingsUrl = "/rest/appCenter/applications/getGeneralSettings";
				return fetch(getGeneralSettingsUrl, {
					method: 'GET',
				}).then((resp) => {
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error getting favorite applications list');
					}
				}).then(data => {
					if (data.maxFavoriteApps != undefined) {
						this.maxFavoriteApps = data.maxFavoriteApps;
					}
					if (data.defaultAppImageName != undefined) {
						this.defaultAppImage.fileName = data.defaultAppImageName;
					}
					if (data.defaultAppImageBody != undefined) {
						this.defaultAppImage.fileBody = data.defaultAppImageBody;
					}
				})
          	},
          	
          	setMaxFavoriteApps() {
		    	var setMaxFavoriteAppsUrl = "/rest/appCenter/applications/setMaxFavorite";
		    	if (this.maxFavoriteApps != '') {
		    		setMaxFavoriteAppsUrl += "?number=" + this.maxFavoriteApps;
		    	}
				return fetch(setMaxFavoriteAppsUrl, {
					method: 'GET'
				}).then(() =>{
					this.isMaxFavoriteAppsView = true;
				})
          	},
          	
          	submitDefaultAppImage() {
          		if (this.defaultAppImage.fileBody == "" && this.defaultAppImage.fileName == "") {
          			this.setDefaultAppImage();
          		}
          		else {
	          		if (this.$refs.defaultAppImageFile.files.length > 0) {
		            	var reader = new FileReader();
				    	reader.onload = (e) => {
				    		if(!e.target.result.includes("data:image")) {
	                    		this.defaultAppImage.invalidImage = true;
	                    		return;
	                    	}
	                    	if(this.$refs.defaultAppImageFile.files[0].size > 100000) {
	                    		this.defaultAppImage.invalidSize = true;
	                    		return;
	                    	}
					    	this.defaultAppImage.fileBody = e.target.result;
	                   		this.setDefaultAppImage();
					   	};
				    	reader.readAsDataURL(this.$refs.defaultAppImageFile.files[0]);
					}
					else {
						this.setDefaultAppImage();
					}
				}
          	},
          	
          	setDefaultAppImage() {
				var setDefaultAppImageUrl = "/rest/appCenter/applications/setDefaultImage";
				return fetch(setDefaultAppImageUrl, {
					headers: {
						'content-Type': 'application/json'
					},
					method: 'POST',
					body: JSON.stringify(this.defaultAppImage)
				}).then(() => {
					this.defaultAppImage.isView = true;
					this.getAppGeneralSettings();
				}).catch(e => {
					throw new Error('Error when setting the default application image ' + e)
				});
          	},
          	
          	handleDefaultAppImageFileUpload(){
          		if (this.$refs.defaultAppImageFile.files.length > 0) {
	   				this.defaultAppImage.fileName = this.$refs.defaultAppImageFile.files[0].name;
	   				this.defaultAppImage.invalidSize = false;
            		this.defaultAppImage.invalidImage = false;
	   			}
	   			else {
	   				this.removeDefaultAppImageFile();
	   			}
	      	},
	      	
          	removeDefaultAppImageFile(){
            	this.defaultAppImage.fileName = '';
            	this.defaultAppImage.fileBody = '';
            	this.defaultAppImage.invalidSize = false;
            	this.defaultAppImage.invalidImage = false;
          	},
          	
          	resetDefaultAppImage(){
          		this.defaultAppImage.isView = true;
            	this.defaultAppImage.invalidSize = false;
            	this.defaultAppImage.invalidImage = false;
          		this.getAppGeneralSettings();
          	}
        }
    }
</script>
