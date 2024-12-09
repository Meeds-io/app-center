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
    <div class="v-application--wrap d-none">
      <div class="container px-0 py-0">
        <div class="layout transparent">
          <v-tooltip bottom>
            <template #activator="{on, attrs}">
              <v-btn
                id="appcenterLauncherButton"
                v-on="on"
                v-bind="attrs"
                aria-label="<%=tooltip%>"
                icon
                class="text-xs-center"
                @click="Vue.startApp('SHARED/appLauncherBundle', 'init', <%=isAdmin%>)">
                <v-icon class="appCenterLauncherButtonIcon icon-default-color" size="20">
                  fa-th
                </v-icon>
              </v-btn>
            </template>
            <span><%=tooltip%></span>
          </v-tooltip>
        </div>
      </div>
    </div>
    <script type="text/javascript">
      require(['SHARED/commonVueComponents', 'SHARED/eXoVueI18n'], () => {
        new Vue({
          el: '#appLauncher',
          vuetify: Vue.prototype.vuetifyOptions,
          mounted() {
            document.querySelector('#appLauncher .v-application--wrap').classList.remove('d-none');
          },
        });
      });
    </script>
  </div>
</div>