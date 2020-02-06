<template>
	<div class="listApplications" v-esc="closeModals">
  		<div class="applicationListHeader">
      		<a @click.stop="showAddApplicationModal()"  
      			class="actionIcon addApplicationButton" 
      			v-tooltip.bottom='$t("appCenter.adminSetupForm.addNewApp")' 
         		data-placement="bottom" 
         		data-container="body">
	        	<i class="uiIconPlus uiIconLightGray"></i>
	        	<span>{{ $t("appCenter.adminSetupForm.addNewApp") }}</span>
			</a>
      		<input @input="currentPage = 1;getApplicationsList()" v-model="keyword" :placeholder="$t('appCenter.adminSetupList.search')" type="text">
		</div>
		
		<table class="uiGrid table table-hover table-striped">
        	<tr>          
            	<th>
              		{{ $t('appCenter.adminSetupList.picto') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupList.application') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupForm.url') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupForm.description') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupForm.permissions') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupForm.byDefault') }}
            	</th>
            	<th>
              		{{ $t('appCenter.adminSetupForm.active') }}
            	</th>
            	<th class="actions">
              		{{ $t('appCenter.adminSetupList.actions') }}
            	</th>
			</tr>
          	<tr :key="application.appId" v-for="application in applicationsList">
          		<td>
              		<img v-if="application.appImageFileBody != undefined && application.appImageFileBody != ''" :src="application.appImageFileBody"/>
            	</td>
            	<td>
              		<h5>{{ application.appTitle }}</h5>
            	</td>
            	<td>
              		<h5>{{ application.appUrl }}</h5>
            	</td>
            	<td>
              		<h5>{{ application.appDescription }}</h5>
            	</td>
            	<td>
              		<h5 v-for="permission in application.appPermissions">{{ permission }}</h5>
            	</td>
            	<td>
              		<input disabled="disabled" type="checkbox" v-model="application.appDefault"/>
            	</td>
            	<td>
              		<input disabled="disabled" type="checkbox" v-model="application.appActive"/>
            	</td>
            	<td>
	            	<a @click.stop="showEditApplicationModal(application)" 
	            		class="actionIcon" 
	            		v-tooltip.bottom='$t("appCenter.adminSetupForm.edit")' 
	            		data-placement="bottom" 
	            		data-container="body">
	              			<i class="uiIconEdit uiIconLightGray"></i>
	            	</a>
	            
	          		<a @click.stop="toDeleteApplicationModal(application)" 
	          			class="actionIcon" 
	          			v-tooltip.bottom='$t("appCenter.adminSetupList.remove")' 
	          			data-placement="bottom" 
	          			data-container="body">
	              			<i class="uiIconRemove uiIconLightGray"></i>
	            	</a>
            	</td>
			</tr>
		</table>
    	<div class="noApp" v-if="applicationsList.length == 0">{{ $t("appCenter.adminSetupForm.noApp") }}</div>
		<div v-if="applicationsList != '' && totalPages != '' && totalPages != '0' && totalPages != '1'" class="applicationsPaginator">
	     	<paginator
	        	:current-page='currentPage'
	        	:per-page='pageSize'
	        	:total='totalApplications'
	        	:total-pages='totalPages'
	        	@pagechanged="onPageChange">
			</paginator>
		</div>
	
		<transition name="fade">
			<exo-modal v-show="showAddEditApplicationModal" :title="formArray.viewMode ? $t('appCenter.adminSetupForm.createNewApp') : $t('appCenter.adminSetupForm.editApp')" @modal-closed="resetForm()">
	        	<div class="addApplication">
	        		<div class="form-container appCenter-form">
	          			<div class="row row-form-items">
	              			<form>
	                			<table class="applicationTable">
	                  				<tr>
	                    				<td>  
	                    					<span>{{ $t("appCenter.adminSetupForm.title") }}</span>
	                    				</td>
	                    				<td>  
	                    					<input type="text" v-model="formArray.title" :placeholder="$t('appCenter.adminSetupForm.titlePlaceholder')">
	                    					<span class="requiredInput">*</span>
	                        				<p v-if="formArray.title == ''" class="errorInput">{{ $t("appCenter.adminSetupForm.titleError") }}</p>
	                    				</td>
	                  				</tr>
	                  				<tr>
					                    <td>  
					                    	<span>{{ $t("appCenter.adminSetupForm.url") }}</span>
					                    </td>
					                    <td>  
					                    	<input type="url" 
					                        	v-model="formArray.url" 
					                         	:placeholder="$t('appCenter.adminSetupForm.urlPlaceholder')">
					                      	<span class="requiredInput">*</span>
					                      	<p v-if="!validUrl(formArray.url) || formArray.url == ''" class="errorInput">{{ $t('appCenter.adminSetupForm.urlError') }}</p>
	                   					</td>
	                  				</tr>
				                  	<tr class="uploadImage">
				                    	<td>  
				                    		<span>{{ $t("appCenter.adminSetupForm.image") }}</span>
				                    	</td>
				                    	<td>  
				                    		<label for="file" class="custom-file-upload">
				                        		<font-awesome-icon icon="download" class="download-icon"/> {{ $t("appCenter.adminSetupForm.browse") }}
				                            </label>
				                        	<input id="file" type="file" accept="image/*" ref="file" @change="handleFileUpload()" />
				                           	<div v-if="formArray.imageFileName != undefined && formArray.imageFileName != ''" class="file-listing">
				                            	{{ formArray.imageFileName }}
				                              	<span class="remove-file" @click="removeFile()">
				                                	<font-awesome-icon icon="times"/>
				                                </span>
				                         	</div>
				                         	<p :class="'sizeInfo' + (formArray.invalidSize ? ' error' : '')">
				                         		<img width="13" height="13" src="/app-center/skin/images/Info tooltip.png"/>
				                         		{{ $t('appCenter.adminSetupForm.sizeError') }}
				                         	</p>
				                         	<p v-if="formArray.invalidImage" class="errorInput">{{ $t('appCenter.adminSetupForm.imageError') }}</p>
				                  		</td>
				         			</tr>
				                    <tr>
				                    	<td>  
				                    		<span>{{ $t("appCenter.adminSetupForm.description") }}</span>
				                    	</td>
				                      	<td>  
				                      		<textarea type="text" v-model="formArray.description" :placeholder="$t('appCenter.adminSetupForm.description')" ></textarea>
				                      	</td>
				                   	</tr>
				                    <tr class="application-checkbox">
				                    	<td>
				                    		<span>{{ $t("appCenter.adminSetupForm.byDefault") }}</span>
				                    	</td>
				                      	<td>
				                        	<input :disabled="!formArray.active" type="checkbox" id="byDefault" v-model="formArray.byDefault">
				                        	<label for="byDefault"></label>
				                      	</td>
				                 	</tr>
				                  	<tr class="application-checkbox">
				                    	<td>
				                    		<span>{{ $t("appCenter.adminSetupForm.active") }}</span>
				                    	</td>
				                     	<td>  
				                        	<input type="checkbox" id="active" @change="onActiveChange()" v-model="formArray.active">
				                       		<label for="active"></label>
				                    	</td>
				                	</tr>
				                    <tr>
				                    	<td>  
				                    		<span>{{ $t("appCenter.adminSetupForm.permissions") }}</span>
				                    	</td>
				                    	<td>
										    <input type="text" id="permissions-suggester">
				                    	</td>
				                 	</tr>
	                			</table>
	                
		                       	<div class="form-group application-buttons">
		                       		<button @click.stop="submitForm()" class="form-submit">{{ $t("appCenter.adminSetupForm.save") }}</button>
		                        	<button @click.stop="resetForm()" class="form-reset">{{ $t("appCenter.adminSetupForm.cancel") }}</button>
		                       	</div>
		                       	<div class="requiredField"><span>{{ $t("appCenter.adminSetupForm.requiredField") }}</span></div>
		                       <div class="error" v-if="error != ''">
		                      		<span>{{ error }}</span>
		                       </div>
							</form>
	            		</div>
	        		</div>
				</div>  
	      	</exo-modal>
		</transition>

		<transition name="fade">    
	    	<exo-modal v-show="showDeleteApplicationModal" :title="$t('appCenter.adminSetupForm.DeleteApp')" @modal-closed="closeDeleteModal()">
	      		<div class="deleteApplication">
		        	<h3>{{ $t("appCenter.adminSetupForm.confirmDelete") }}<span>{{ formArray.title }}</span> ?</h3>
		         	<div class="form-group application-buttons">
		            	<button @click.stop="deleteApplication()" class="form-submit"><font-awesome-icon icon="trash-alt"/> {{ $t("appCenter.adminSetupForm.delete") }}</button>
		            	<button @click.stop="showDeleteApplicationModal = false" class="form-reset"><font-awesome-icon icon="times"/> {{ $t("appCenter.adminSetupForm.cancel") }}</button>
					</div>
					<div class="error" v-if="error != ''">
		                  <span>{{ error }}</span>
		            </div>
				</div>
			</exo-modal>
		</transition>
	</div>  
</template>

<script>
    import { library } from '@fortawesome/fontawesome-svg-core'
    import { faExclamationCircle, faDownload, faTimes, faTrashAlt } from '@fortawesome/free-solid-svg-icons'
    import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
    import Paginator from './Paginator.vue';
    import VueEsc from 'vue-esc';
    import VTooltip from 'v-tooltip'
	 
	Vue.use(VTooltip);

    library.add(faExclamationCircle, faDownload, faTimes, faTrashAlt);

    Vue.component('font-awesome-icon', FontAwesomeIcon);
    Vue.use(VueEsc);
    
    export default {
    	name: "adminSetup",
   		components: {
        	Paginator
    	},
    	data(){
        	return{
            	keyword: '',
            	applicationsList: [],
              	formArray:{
                	id:'',
                  	title:'',
                  	url:'',
                  	imageFileBody: '',
                  	imageFileName: '',
                  	description:'',
                  	byDefault: false,
                  	active: true,
                  	permissions:[],
                  	viewMode: true,
                  	invalidSize: false,
                  	invalidImage: false
                },
                editArray: [],
                error: '',
		        showAddEditApplicationModal: false,
		        showDeleteApplicationModal: false,
		        currentPage: 1,
		        totalApplications: '',
		        totalPages: '',
		        groups: []
            }
        },
        
        created() {
        	this.pageSize = this.$parent.pageSize;
       		this.getApplicationsList();
        },

        methods:{
       		getApplicationsList() {
       			
            	var getApplicationsListUrl = "/rest/appCenter/applications/getApplicationsList?offset="+ (this.currentPage - 1) +"&limit=" + this.pageSize + "&keyword=" + this.keyword;

				return fetch(getApplicationsListUrl, {
					method: 'GET',
				}).then((resp) => {
					if(resp && resp.ok) {
						return resp.json();
					} else {
						throw new Error('Error when getting the favorite applications list');
					}
				}).then(data => {
					this.applicationsList = data.applications;
					this.totalApplications = data.totalApplications;
					this.totalPages = data.totalPages;
				})
            },
            
            onPageChange(page) {
				this.currentPage = page;
				this.getApplicationsList();
			},
            
          	submitForm() {
            	if(this.formArray.title && this.formArray.url && this.validUrl(this.formArray.url)) {
            		if (this.formArray.imageFileBody == "" && this.formArray.imageFileName == "") {
	          			this.addEditApplication();
	          		}
          			else {
	                	if (this.$refs.file.files.length > 0) {
	                  		var reader = new FileReader();
	                  		reader.onload = (e) => {
	                  			console.log(e.target.result);
	                    		this.formArray.imageFileBody = e.target.result;
	                    		if(!this.formArray.imageFileBody.includes("data:image")) {
	                    			this.formArray.invalidImage = true;
	                    			return;
	                    		}
	                    		if(this.$refs.file.files[0].size > 100000) {
	                    			this.formArray.invalidSize = true;
	                    			return;
	                    		}
	                    		this.addEditApplication();
	                		};
	                  		reader.readAsDataURL(this.$refs.file.files[0]);
	            		}
	            		else {
	            			this.addEditApplication();
	            		}
			    	}
            	}
            },
            
            addEditApplication() {
		    	var addEditApplication = this.formArray.viewMode ? "/rest/appCenter/applications/addApplication" : "/rest/appCenter/applications/editApplication";
		        return fetch(addEditApplication, {
		        	headers: {
		        		'content-Type': 'application/json'
					},
					method: 'POST',
					body: JSON.stringify(this.formArray)
				}).then(response => {
					this.getApplicationsList();
					this.resetForm();
				}).catch(e => {
					if (e.response.status == 401) {
						this.error = this.$t("appCenter.adminSetupForm.unauthorized");
					}
					else {
						this.error = this.$t('appCenter.adminSetupForm.error');
					}
				});
      		},

            deleteApplication() {
				return fetch("/rest/appCenter/applications/deleteApplication/" + this.formArray.id, {
					method: 'GET',
				}).then( (resp) => {
					if(resp && resp.ok) {
						return resp.json;
					} else {
						throw new Error('Error when deleting application by id');
					}
				}).then(() => {
					this.closeDeleteModal();
					this.currentPage = 1;
					this.getApplicationsList();
				})
            },
            
          	resetForm() {
            	this.error = '';
            	this.formArray.id = '';
            	this.formArray.title = '';
            	this.formArray.description = '';
            	this.formArray.url = '';
            	this.formArray.permissions = [];
            	this.formArray.active = true;
            	this.formArray.byDefault = false;
            	this.formArray.imageFileName = '';
            	this.formArray.imageFileBody = '';
            	this.formArray.invalidSize = false;
            	this.formArray.invalidImage = false;
            	this.showAddEditApplicationModal = false;
          	},
          
          	handleFileUpload(){
          		if (this.$refs.file.files.length > 0) {
	   				this.formArray.imageFileName = this.$refs.file.files[0].name;
            		this.formArray.invalidSize = false;
            		this.formArray.invalidImage = false
	   			}
	   			else {
	   				this.removeFile();
	   			}
          	},
         	 
          	removeFile(){
            	this.formArray.imageFileName = '';
            	this.formArray.imageFileBody = '';
            	this.formArray.invalidSize = false;
            	this.formArray.invalidImage = false
          	},
          	
          	showAddApplicationModal() {
            	this.showAddEditApplicationModal = true;
            	this.formArray.viewMode = true;
            	this.initPermissionsSuggester();
          	},
        
          	showEditApplicationModal(item) {
            	this.showAddEditApplicationModal = true;
            	this.formArray.id = item.appId;
            	this.formArray.title = item.appTitle;
            	this.formArray.description = item.appDescription;
            	this.formArray.url = item.appUrl;
            	this.formArray.permissions = Array.from(item.appPermissions);
            	this.formArray.active = item.appActive;
            	this.formArray.byDefault = item.appDefault;
            	this.formArray.imageFileBody = item.appImageFileBody;
            	this.formArray.imageFileName = item.appImageFileName;
            	this.formArray.viewMode = false;
            	this.initPermissionsSuggester();
          	},
        
        	toDeleteApplicationModal(item) {
            	this.showDeleteApplicationModal = true;
            	this.formArray.id = item.appId;
            	this.formArray.title = item.appTitle;
         	},
         	
         	closeModals(e) {
         		this.closeDeleteModal();
         		this.resetForm();
         	},
         	
         	closeDeleteModal() {
         		this.formArray.id = '';
            	this.formArray.title = '';
         		this.showDeleteApplicationModal = false;
         	},
            
       		validUrl(url) {
       			return url.match(/(http(s)?:\/\/.)[-a-zA-Z0-9@:%._\+~#=]{2,256}/g);
          	},
          	
			onActiveChange() {
				if(!this.formArray.active) {
					this.formArray.byDefault = false;
				}
			},
			          	
          	initPermissionsSuggester() {
		    	var permissionsSuggester = jq('#permissions-suggester');
		      	if(permissionsSuggester && permissionsSuggester.length) {
					
		        	const component = this;
		        	const suggesterData = {
		          		type: 'tag',
		          		plugins: ['remove_button', 'restore_on_backspace'],
		          		create: false,
		          		createOnBlur: false,
		          		highlight: false,
		          		openOnFocus: false,
		          		sourceProviders: ['adminSetup'],
		          		valueField: 'text',
		          		labelField: 'text',
		          		searchField: ['text'],
		          		closeAfterSelect: true,
		          		dropdownParent: 'body',
		          		hideSelected: true,
			          	renderMenuItem (item, escape) {
			            	return component.renderMenuItem(item, escape);
			          	},
			          	renderItem(item) {
			            	return `<div class="item">${item.text}</div>`;
			          	},
			          	onItemAdd(item) {
			            	component.addSuggestedItem(item);
			          	},
			          	onItemRemove(item) {
			            	component.removeSuggestedItem(item);
			          	},
			          	sortField: [{field: 'order'}, {field: '$score'}],
			          	providers: {
			            	'adminSetup': component.findGroups
			          	}
		        	};
		        	permissionsSuggester.suggester(suggesterData);
		        	jq('#permissions-suggester')[0].selectize.clear();
		        	if(this.formArray.permissions && this.formArray.permissions !== null) {
		          		for(const permission of this.formArray.permissions) {
		            		permissionsSuggester[0].selectize.addOption({text: permission});
		            		permissionsSuggester[0].selectize.addItem(permission);
		          		}
		        	}     
    			}
      		},
      		
      		addSuggestedItem(item) {
		    	if(jq('#permissions-suggester') && jq('#permissions-suggester').length && jq('#permissions-suggester')[0].selectize) {
		        	const selectize = jq('#permissions-suggester')[0].selectize;
		        	item = selectize.options[item];
		      	}
		      	if(!this.formArray.permissions.find(permission => permission === item.text)) {
		        	this.formArray.permissions.push(item.text);
		      	}
		    },
		    
    		removeSuggestedItem(item) {
      			const permissionsSuggester = jq('#permissions-suggester');
      			for(let i=this.formArray.permissions.length-1; i>=0; i--) {
        			if(this.formArray.permissions[i] === item) {
          				this.formArray.permissions.splice(i, 1);
          				permissionsSuggester[0].selectize.removeOption(item);
          				permissionsSuggester[0].selectize.removeItem(item);
        			}
      			}
    		},
    		
    		findGroups (query, callback) {
		    	if (!query.length) {
		        	return callback(); 
		      	}
		      	fetch('/rest/v1/groups?q=' + query, {credentials: 'include'}).then(resp => resp.json()).then(data => {
		        	const groups = [];
		        	for(const group of data) {
		          		groups.push({
		            		avatarUrl: null,
		            		text: `*:${group.id}`,
		            		value: `*:${group.id}`,
		            		type: 'group'
		          		});
		        	}
		        	callback(groups);
		      	});
    		},
    		
			renderMenuItem (item, escape) {
		    	return `
		        	<div class="item">${escape(item.value)}</div>
		      `	;
		    },
		}
	}
</script>

<style>
	.fade-enter-active, .fade-leave-active {
		transition: opacity .5s ease-out;
	}
	
	.fade-enter, .fade-leave-to {
		opacity:0;
	}
</style>