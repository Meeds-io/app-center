<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="io.meeds.appcenter.portlet.AdminSetupPortlet" %>
<%@ page import="org.exoplatform.social.webui.Utils" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<portlet:defineObjects/>
<%
  PortletPreferences preferences = renderRequest.getPreferences();
  String pageSize = AdminSetupPortlet.getPageSize(preferences.getValue("pageSize", null));
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application applicationsAdmin v-application--is-ltr theme--light"
    id="adminSetup">
    <v-cacheable-dom-app cache-id="adminSetup"></v-cacheable-dom-app>
    <script>
  		require(['SHARED/adminSetupPortletBundle'], function(adminSetupPortletApp) {
  			var preferences = {
  				pageSize: <%=Utils.toScriptString(pageSize)%>
  			};
  			adminSetupPortletApp.init(preferences);
	    });
  	</script>
  </div>
</div>