<%@ page import="javax.portlet.PortletPreferences" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects/>
<%
    PortletPreferences preferences = renderRequest.getPreferences();
    String pageSize = preferences.getValue("pageSize", "");
    if (pageSize == null) {
    	pageSize = "";
    }
    
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application applicationsAdmin v-application--is-ltr theme--light"
    id="adminSetup">
    <v-cacheable-dom-app cache-id="adminSetup"></v-cacheable-dom-app>
    <script>
  		require(['SHARED/adminSetupPortletBundle'], function(adminSetupPortletApp) {
  			var preferences = {
  				pageSize: "<%=pageSize%>"
  			};
  			adminSetupPortletApp.init(preferences);
	    });
  	</script>
  </div>
</div>