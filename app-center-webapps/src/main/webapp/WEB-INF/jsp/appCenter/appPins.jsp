<%@page import="io.meeds.portal.navigation.service.NavigationConfigurationService"%>
<%@page import="org.exoplatform.commons.api.settings.SettingValue"%>
<%@page import="org.exoplatform.commons.api.settings.data.Scope"%>
<%@page import="org.exoplatform.commons.api.settings.data.Context"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@page import="org.exoplatform.commons.api.settings.SettingService"%>
<%
  SettingService settingService = ExoContainerContext.getService(SettingService.class);
  SettingValue settingValue = settingService.get(Context.USER.id(request.getRemoteUser()), Scope.APPLICATION.id("PinnedApplications"), "pins");
  String pinnedApplicationIds = settingValue == null || settingValue.getValue() == null ? "[]" : settingValue.getValue().toString();

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
