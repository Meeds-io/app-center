<%@page import="io.meeds.portal.navigation.service.NavigationConfigurationService"%>
<%@page import="io.meeds.appcenter.service.ApplicationCenterService"%>
<%@page import="io.meeds.social.util.JsonUtils"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%
  ApplicationCenterService appCenterService = ExoContainerContext.getService(ApplicationCenterService.class);
  String pinnedApplicationIds = JsonUtils.toJsonString(appCenterService.getPinnedApplicationIds(request.getRemoteUser()));

  NavigationConfigurationService navigationConfigurationService = ExoContainerContext.getService(NavigationConfigurationService.class);
  long topbarAppsCount = navigationConfigurationService.getTopbarConfiguration(request.getRemoteUser(), request.getLocale()).getApplications().stream().filter(a -> a.isEnabled() || a.getId().contains("mandatory-")).count();
%>
<div class="VuetifyApp">
  <div id="userPinnedApplications">
    <script type="text/javascript">
    window.require(['PORTLET/app-center/UserPinnedApplications'], app => app.init(<%=pinnedApplicationIds%>, <%=topbarAppsCount%>));
    </script>
  </div>
</div>
