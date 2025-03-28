<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="org.exoplatform.commons.utils.CommonsUtils" %>
<%@ page import="org.exoplatform.portal.localization.LocaleContextInfoUtils" %>
<%@ page import="io.meeds.social.translation.service.TranslationService" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects/>
<portlet:actionURL var="saveSettingsUrl" />
<%
  long applicationId;
  String objectType = "myApplicationsPortlet";
  String fieldName  = "headerTitle";
  Object applicationIdParam = request.getAttribute("applicationId");
  applicationId = Long.parseLong((applicationIdParam instanceof String[]) ? ((String[]) applicationIdParam)[0]
          : (String) applicationIdParam);
  boolean isAdmin = (boolean) request.getAttribute("isAdmin");

  PortletPreferences preferences = renderRequest.getPreferences();
  int maxAppsToList = Integer.parseInt(preferences.getValue("maxAppsToList", "4"));
  boolean showHeader = Boolean.parseBoolean(preferences.getValue("showHeader", "true"));
  String headerTitle = CommonsUtils.getService(TranslationService.class).getTranslationLabelOrDefault(objectType,
          applicationId, fieldName, LocaleContextInfoUtils.getUserLocale(request.getRemoteUser()));
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="myApplications">
    <script type="text/javascript">
      require(['PORTLET/app-center/AppCenterMyApplicationsPortlet'], app => app.init({
        applicationId: '<%=applicationId%>',
        maxAppsToList: '<%=maxAppsToList%>',
        showHeader: <%=showHeader%>,
        headerTitle: '<%=headerTitle%>' !== 'null' ? '<%=headerTitle%>' : null,
        isAdmin: <%=isAdmin%>,
        saveSettingsUrl: '<%=saveSettingsUrl%>'
      }));
    </script>
  </div>
</div>
