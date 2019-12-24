<template>
	<div class="userSetup">
  		<div class="userApplicationTitle">
  			{{ $t("appCenter.userSetup.appDirectory") }}
  			<a v-if="isAdmin" target="_blank" href="/portal/g/:platform:administrators/appCenterAdminSetup">
  				<i class="uiIconPLF24x24Setup"></i>
  			</a>
  		</div>
  		<div class="userApplicationContent">
			<user-authorizedApplications></user-authorizedApplications>
	  		<user-favoriteApplications></user-favoriteApplications>	
		</div>
  	</div>
</template>

<script>
	export default {
	
		data(){
        	return{
        		isAdmin : eXo.env.portal.isAdmin,
        	}
        },
	
		created() {
	    	this.pageSize = this.$parent.$data.preferences.pageSize;
		}
	}
</script>
