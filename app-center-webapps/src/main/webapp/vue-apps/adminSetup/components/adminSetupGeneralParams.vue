<template>
	<div class="generalParams">
		<div class="form-container appCenter-form">
			<div class="maxFavoriteApps">
				<span>{{ $t("appCenter.adminSetupForm.maxFavoriteApps") }}</span>
				<span v-if="isMaxFavoriteAppsView && maxFavoriteApps != ''">
					{{ maxFavoriteApps }}
				</span>
					
				<input v-if="!isMaxFavoriteAppsView" type="number" min="0" onkeypress="return event.charCode >= 48 && event.charCode <= 57" v-model="maxFavoriteApps">

				<a v-if="isMaxFavoriteAppsView" @click.prevent="isMaxFavoriteAppsView = false" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.edit")' data-placement="bottom" data-container="body">
			     	<i class="uiIconEdit uiIconLightGray"></i>
			   	</a>
			  	<a v-if="!isMaxFavoriteAppsView" @click.prevent="setMaxFavoriteApps()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.save")' data-placement="bottom" data-container="body">
			   		<i class="uiIconSave uiIconLightGray"></i>
			    </a>
			    <a v-if="!isMaxFavoriteAppsView" @click.prevent="isMaxFavoriteAppsView = true" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.cancel")' data-placement="bottom" data-container="body">
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
			  	<a v-if="defaultAppImage.isView" @click.prevent="defaultAppImage.isView = false" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.edit")' data-placement="bottom" data-container="body">
			   		<i class="uiIconEdit uiIconLightGray"></i>
			   	</a>
			   	<a v-if="!defaultAppImage.isView" @click.prevent="submitDefaultAppImage()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.save")' data-placement="bottom" data-container="body">
			   		<i class="uiIconSave uiIconLightGray"></i>
			   	</a>
			   	<a v-if="!defaultAppImage.isView" @click.prevent="resetDefaultAppImage()" class="actionIcon" v-tooltip.bottom='$t("appCenter.adminSetupForm.cancel")' data-placement="bottom" data-container="body">
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

    import axios from 'axios';
    import jq from 'jquery';
   	import { library } from '@fortawesome/fontawesome-svg-core'
    import { faExclamationCircle, faDownload, faTimes } from '@fortawesome/free-solid-svg-icons'
    import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
    import BootstrapVue from 'bootstrap-vue';
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
	        	axios.get(getGeneralSettingsUrl)
	            .then(response => {
	            	if (response.data.maxFavoriteApps != undefined) {
	            		this.maxFavoriteApps = response.data.maxFavoriteApps;
	            	}
	            	if (response.data.defaultAppImageName != undefined) {
	            		this.defaultAppImage.fileName = response.data.defaultAppImageName;
	            	}
	            	if (response.data.defaultAppImageBody != undefined) {
	            		this.defaultAppImage.fileBody = response.data.defaultAppImageBody;
	            	}
	           	}).catch(e => {
	            })
          	},
          	
          	setMaxFavoriteApps() {
		    	var setMaxFavoriteAppsUrl = "/rest/appCenter/applications/setMaxFavorite";
		    	if (this.maxFavoriteApps != '') {
		    		setMaxFavoriteAppsUrl += "?number=" + this.maxFavoriteApps;
		    	}
	           	axios.get(setMaxFavoriteAppsUrl)
	            .then(response => {
	            	this.isMaxFavoriteAppsView = true;
	           	}).catch(e => {
	           		
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
				axios.post(setDefaultAppImageUrl,this.defaultAppImage, {
					headers: {
						"Content-Type": "application/json; charset=utf-8"
					}
				})
			   	.then(response => {
			    	this.defaultAppImage.isView = true;
			    	this.getAppGeneralSettings();
			  	}).catch(e => {
			    })
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
