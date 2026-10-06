<%@page import="org.apache.commons.text.StringEscapeUtils"%>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="org.exoplatform.commons.utils.CommonsUtils" %>
<%@ page import="org.exoplatform.portal.localization.LocaleContextInfoUtils" %>
<%@ page import="io.meeds.social.translation.service.TranslationService" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<portlet:defineObjects/>
<portlet:actionURL var="saveSettingsUrl" />
<%
  String objectType = "myApplicationsPortlet";
  String fieldName  = "headerTitle";
  Object settingNameParam = request.getAttribute("settingName");
  String settingName = (settingNameParam instanceof String[]) ? ((String[]) settingNameParam)[0]
          : (String) settingNameParam;
  boolean canEdit = Boolean.TRUE.equals(request.getAttribute("canEdit"));

  PortletPreferences preferences = renderRequest.getPreferences();
  int maxAppsToList = Integer.parseInt(preferences.getValue("maxAppsToList", "4"));
  boolean showHeader = Boolean.parseBoolean(preferences.getValue("showHeader", "true"));
  String headerTitle = CommonsUtils.getService(TranslationService.class).getTranslationLabelOrDefault(objectType,
          settingName, fieldName, LocaleContextInfoUtils.getUserLocale(request.getRemoteUser()));
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="myApplications">
    <%-- myApplicationsService.js#isRenderedWithSettings parses this init block to confirm a save: keep settingName first and one "key: value," per line --%>
    <script type="text/javascript">
      require(['PORTLET/app-center/AppCenterMyApplicationsPortlet'], app => app.init({
        settingName: '<%=StringEscapeUtils.escapeEcmaScript(settingName)%>',
        maxAppsToList: '<%=maxAppsToList%>',
        showHeader: <%=showHeader%>,
        headerTitle: <%=headerTitle == null ? null : String.format("'%s'", StringEscapeUtils.escapeEcmaScript(headerTitle))%>,
        canEdit: <%=canEdit%>,
        saveSettingsUrl: '<%=saveSettingsUrl%>'
      }));
    </script>
  </div>
</div>
