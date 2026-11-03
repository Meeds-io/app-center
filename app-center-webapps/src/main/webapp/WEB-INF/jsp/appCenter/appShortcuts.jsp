<%@page import="org.apache.commons.text.StringEscapeUtils"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.apache.commons.collections4.CollectionUtils"%>
<%@page import="io.meeds.appcenter.service.ApplicationCenterService"%>
<%@page import="java.util.List"%>
<%@page import="org.exoplatform.portal.application.PortalRequestContext"%>
<%@page import="org.exoplatform.services.security.ConversationState"%>
<%@page import="java.util.Locale"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="io.meeds.social.util.JsonUtils"%>
<%
  boolean isAdmin = ConversationState.getCurrent().getIdentity().isMemberOf("/platform/administrators");
  String appCenterDrawer = PortalRequestContext.getCurrentInstance().getRequest().getParameter("appCenterDrawer");
  String appCenterPortlet = PortalRequestContext.getCurrentInstance().getRequest().getParameter("appCenterPortlet");
  boolean autoInit = appCenterDrawer != null || appCenterPortlet != null;
  ApplicationCenterService appCenterService = ExoContainerContext.getService(ApplicationCenterService.class);
  List<String> shortcuts = appCenterService.getApplicationShortcuts(PortalRequestContext.getCurrentInstance().getRemoteUser())
                                           .stream()
                                           .map(StringEscapeUtils::escapeEcmaScript)
                                           .toList();
  String shortcutListString = CollectionUtils.isEmpty(shortcuts) ? "['a']" : String.format("['%s','a']", StringUtils.join(shortcuts, "', '"));

  String pinnedApplicationIds = JsonUtils.toJsonString(appCenterService.getPinnedApplicationIds(request.getRemoteUser()));
%>
<div class="VuetifyApp">
  <div
    data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="appShortcuts">
    <div class="v-application--wrap">
      <div class="container px-0 py-0">
        <div class="layout transparent">
          <script type="text/javascript">
          <% if (autoInit) { %>
          window.require(['SHARED/appLauncherBundle'], app => app.init({isAdmin: <%=isAdmin%>, pinnedApplicationIds: <%=pinnedApplicationIds%>, autoInitDrawerId: '<%=appCenterDrawer == null ? "" : StringEscapeUtils.escapeEcmaScript(appCenterDrawer)%>', autoInitPortletId: '<%=appCenterPortlet == null ? "" : StringEscapeUtils.escapeEcmaScript(appCenterPortlet)%>'}, true, <%=shortcutListString%>));
          <% } else { %>
          window.require(['SHARED/commonVueComponents'], () => Vue.prototype.$utils.addShortcutsListener(<%=shortcutListString%>, shortcut => window.require(['SHARED/appLauncherBundle'], app => app.init({isAdmin: <%=isAdmin%>, pinnedApplicationIds: <%=pinnedApplicationIds%>}, true, <%=shortcutListString%>, shortcut))));
          <% } %>
          </script>
        </div>
      </div>
    </div>
  </div>
</div>