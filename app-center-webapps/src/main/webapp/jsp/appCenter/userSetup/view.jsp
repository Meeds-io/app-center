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
  <div id="userSetup">
  	<script>
  		require(['SHARED/userSetupPortletBundle'], function(userSetupPortletApp) {
  			var preferences = {
  				pageSize: "<%=pageSize%>"
  			};
  			userSetupPortletApp.init(preferences);
  	    });
  	</script>
  </div>
</div>