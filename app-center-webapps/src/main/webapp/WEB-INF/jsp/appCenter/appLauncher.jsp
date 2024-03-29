<%@page import="org.exoplatform.services.security.ConversationState"%>
<%@page import="java.util.Locale"%>
<%@page import="org.exoplatform.container.ExoContainerContext"%>
<%@page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@page import="java.util.ResourceBundle"%>
<%
  ResourceBundle bundle;
  try {
    bundle = ExoContainerContext.getService(ResourceBundleService.class).getResourceBundle("locale.addon.appcenter", request.getLocale());
  } catch (Exception e) {
    bundle = ExoContainerContext.getService(ResourceBundleService.class).getResourceBundle("locale.addon.appcenter", Locale.ENGLISH);
  }
  String tooltip = bundle.getString("appCenter.appLauncher.topbarIcon.tooltip");
  boolean isAdmin = ConversationState.getCurrent().getIdentity().isMemberOf("/platform/administrators");
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
            title="<%=tooltip%>"
            class="text-xs-center v-btn v-btn--flat v-btn--icon v-btn--round theme--light v-size--default"
            id="appcenterLauncherButton"
            onclick="Vue.startApp('SHARED/appLauncherBundle', 'init', <%=isAdmin%>)">
            <span class="v-btn__content"><i aria-hidden="true"
              class="v-icon notranslate appCenterLauncherButtonIcon mdi mdi-dots-grid icon-large-size icon-default-color theme--light"></i></span>
          </button>
        </div>
      </div>
    </div>
  </div>
</div>