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
<%@page import="org.exoplatform.commons.api.settings.SettingService"%>
<%@page import="org.exoplatform.commons.api.settings.SettingValue"%>
<%@page import="org.exoplatform.commons.api.settings.data.Scope"%>
<%@page import="org.exoplatform.commons.api.settings.data.Context"%>
<%
  ResourceBundle bundle;
  try {
    bundle = ExoContainerContext.getService(ResourceBundleService.class).getResourceBundle("locale.addon.appcenter", request.getLocale());
  } catch (Exception e) {
    bundle = ExoContainerContext.getService(ResourceBundleService.class).getResourceBundle("locale.addon.appcenter", Locale.ENGLISH);
  }
  String tooltip = bundle.getString("appCenter.appLauncher.topbarIcon.tooltip");
  String tooltipShortcut = bundle.getString("appCenter.appLauncher.topbarIcon.tooltip.shortcut");
  boolean isAdmin = ConversationState.getCurrent().getIdentity().isMemberOf("/platform/administrators");
  String appCenterDrawer = PortalRequestContext.getCurrentInstance().getRequest().getParameter("appCenterDrawer");
  String appCenterPortlet = PortalRequestContext.getCurrentInstance().getRequest().getParameter("appCenterPortlet");
  boolean autoInit = appCenterDrawer != null || appCenterPortlet != null;
  List<String> shortcuts = ExoContainerContext.getService(ApplicationCenterService.class).getApplicationShortcuts(PortalRequestContext.getCurrentInstance().getRemoteUser());
  String shortcutListString = CollectionUtils.isEmpty(shortcuts) ? "['a']" : String.format("['%s','a']", StringUtils.join(shortcuts, "', '"));

  SettingService settingService = ExoContainerContext.getService(SettingService.class);
  SettingValue settingValue = settingService.get(Context.USER.id(request.getRemoteUser()), Scope.APPLICATION.id("PinnedApplications"), "pins");
  String pinnedApplicationIds = settingValue == null || settingValue.getValue() == null ? "[]" : settingValue.getValue().toString().replace("\"", "`");
%>
<div class="VuetifyApp">
  <div
    data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="appLauncher">
    <div class="v-application--wrap">
      <div class="container px-0 py-0">
        <div class="layout transparent">
          <button
            type="button"
            title="<%=tooltip%> <%=tooltipShortcut%>"
            class="v-btn v-btn--flat v-btn--icon v-btn--round theme--light v-size--default"
            id="appcenterLauncherButton"
            onclick="Vue.startApp('SHARED/appLauncherBundle', 'init', {isAdmin: <%=isAdmin%>, pinnedApplicationIds: <%=pinnedApplicationIds%>})">
            <span class="v-btn__content">
              <i aria-hidden="true"
                class="v-icon notranslate appCenterLauncherButtonIcon icon-default-color fa fa-th theme--light"
                style="font-size: 20px;"></i>
            </span>
          </button>
            <script type="text/javascript">
            <% if (autoInit) { %>
            window.require(['SHARED/appLauncherBundle'], app => app.init({isAdmin: <%=isAdmin%>, pinnedApplicationIds: <%=pinnedApplicationIds%>, autoInitDrawerId: '<%=appCenterDrawer == null ? "" : appCenterDrawer%>', autoInitPortletId: '<%=appCenterPortlet == null ? "" : appCenterPortlet%>'}, true, <%=shortcutListString%>));
            <% } else { %>
            window.require(['SHARED/commonVueComponents'], () => Vue.prototype.$utils.addShortcutsListener(<%=shortcutListString%>, shortcut => window.require(['SHARED/appLauncherBundle'], app => app.init({isAdmin: <%=isAdmin%>, pinnedApplicationIds: <%=pinnedApplicationIds%>}, true, <%=shortcutListString%>, shortcut))));
            <% } %>
            </script>
        </div>
      </div>
    </div>
  </div>
</div>