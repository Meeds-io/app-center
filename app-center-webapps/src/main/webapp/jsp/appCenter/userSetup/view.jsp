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
<div id="userSetup">
	<script>
		require(['SHARED/userSetupPortletBundle'], function(userSetupPortletApp) {
			var preferences = {
				maxApps: "<%=maxApps%>"
			};
			userSetupPortletApp.init(preferences);
	    });
	</script>
</div>