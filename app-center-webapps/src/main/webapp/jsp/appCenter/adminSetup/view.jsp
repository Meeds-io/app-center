<%@ page import="javax.portlet.PortletPreferences" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects/>
<%
    PortletPreferences preferences = renderRequest.getPreferences();
    String maxApps = preferences.getValue("maxApps", "");
    if (maxApps == null) {
    	maxApps = "";
    }
    
%>
<div id="adminSetup">
	<script>
		require(['SHARED/adminSetupPortletBundle'], function(adminSetupPortletApp) {
			var preferences = {
				maxApps: "<%=maxApps%>"
			};
			adminSetupPortletApp.init(preferences);
	    });
	</script>
</div>