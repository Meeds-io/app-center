<%@page import="org.exoplatform.commons.api.settings.SettingValue"%>
<%@page import="org.exoplatform.commons.api.settings.data.Scope"%>
<%@page import="org.exoplatform.commons.api.settings.data.Context"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@page import="org.exoplatform.commons.api.settings.SettingService"%>
<%
SettingService settingService = ExoContainerContext.getService(SettingService.class);
SettingValue settingValue = settingService.get(Context.GLOBAL.id("QuickActions"), Scope.APPLICATION.id("QuickActions"), "status");
String quickActionsStatus = settingValue == null || settingValue.getValue() == null ? "{}" : settingValue.getValue().toString();

settingValue = settingService.get(Context.USER.id(request.getRemoteUser()), Scope.APPLICATION.id("QuickActions"), "pins");
String pinnedQuickActions = settingValue == null || settingValue.getValue() == null ? "[]" : settingValue.getValue().toString();
%>
<div class="VuetifyApp">
  <div id="userQuickActions">
    <script type="text/javascript">
    window.require(['PORTLET/app-center/UserQuickActions'], app => app.init(`<%=quickActionsStatus%>`, `<%=pinnedQuickActions%>`));
    </script>
  </div>
</div>
