<%@page import="org.apache.commons.text.StringEscapeUtils"%>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="org.exoplatform.commons.utils.CommonsUtils" %>
<%@ page import="org.exoplatform.portal.localization.LocaleContextInfoUtils" %>
<%@ page import="io.meeds.social.translation.service.TranslationService" %>
<%@ page import="io.meeds.appcenter.service.MyApplicationsSettingsService" %>
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
  String listingMode = MyApplicationsSettingsService.getListingMode(preferences.getValue(MyApplicationsSettingsService.LISTING_MODE, null));
  String selectionMode = MyApplicationsSettingsService.getSelectionMode(preferences.getValue(MyApplicationsSettingsService.SELECTION_MODE, null));
  String applicationIds = MyApplicationsSettingsService.getApplicationIds(preferences.getValue(MyApplicationsSettingsService.APPLICATION_IDS, null)).toString().replace(" ", "");
  String categoryIds = MyApplicationsSettingsService.getCategoryIds(preferences.getValue(MyApplicationsSettingsService.CATEGORY_IDS, null)).toString().replace(" ", "");
  // Each display setting is printed from its parsed value, never as stored: null when unset or invalid, the color empty
  String template = MyApplicationsSettingsService.getTemplate(preferences.getValue(MyApplicationsSettingsService.TEMPLATE, null));
  Boolean showSeeMore = MyApplicationsSettingsService.getDisplayFlag(preferences.getValue(MyApplicationsSettingsService.SHOW_SEE_MORE, null));
  Boolean listInRow = MyApplicationsSettingsService.getDisplayFlag(preferences.getValue(MyApplicationsSettingsService.LIST_IN_ROW, null));
  Boolean showIcon = MyApplicationsSettingsService.getDisplayFlag(preferences.getValue(MyApplicationsSettingsService.SHOW_ICON, null));
  Integer iconSize = MyApplicationsSettingsService.getIconSize(preferences.getValue(MyApplicationsSettingsService.ICON_SIZE, null));
  Boolean showName = MyApplicationsSettingsService.getDisplayFlag(preferences.getValue(MyApplicationsSettingsService.SHOW_NAME, null));
  String color = MyApplicationsSettingsService.getColor(preferences.getValue(MyApplicationsSettingsService.COLOR, null));
  Boolean outlined = MyApplicationsSettingsService.getDisplayFlag(preferences.getValue(MyApplicationsSettingsService.OUTLINED, null));
  String hAlign = MyApplicationsSettingsService.getAlignment(preferences.getValue(MyApplicationsSettingsService.H_ALIGN, null));
  String vAlign = MyApplicationsSettingsService.getAlignment(preferences.getValue(MyApplicationsSettingsService.V_ALIGN, null));
  String headerTitle = CommonsUtils.getService(TranslationService.class).getTranslationLabelOrDefault(objectType,
          settingName, fieldName, LocaleContextInfoUtils.getUserLocale(request.getRemoteUser()));
  // One mount point per window: several Shortcuts portlets can share a page
  String appId = "myApplications" + renderRequest.getWindowID();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="<%=StringEscapeUtils.escapeHtml4(appId)%>">
    <%-- myApplicationsService.js#isRenderedWithSettings parses this init block to confirm a save: keep settingName first and one "key: value," per line, a list printed as [a,b] --%>
    <script type="text/javascript">
      require(['PORTLET/app-center/AppCenterMyApplicationsPortlet'], app => app.init('<%=StringEscapeUtils.escapeEcmaScript(appId)%>', {
        settingName: '<%=StringEscapeUtils.escapeEcmaScript(settingName)%>',
        maxAppsToList: '<%=maxAppsToList%>',
        listingMode: '<%=listingMode%>',
        selectionMode: '<%=selectionMode%>',
        applicationIds: <%=applicationIds%>,
        categoryIds: <%=categoryIds%>,
        showHeader: <%=showHeader%>,
        template: <%=template == null ? null : String.format("'%s'", template)%>,
        showSeeMore: <%=showSeeMore%>,
        listInRow: <%=listInRow%>,
        showIcon: <%=showIcon%>,
        iconSize: <%=iconSize%>,
        showName: <%=showName%>,
        color: '<%=StringEscapeUtils.escapeEcmaScript(color)%>',
        outlined: <%=outlined%>,
        hAlign: <%=hAlign == null ? null : String.format("'%s'", hAlign)%>,
        vAlign: <%=vAlign == null ? null : String.format("'%s'", vAlign)%>,
        headerTitle: <%=headerTitle == null ? null : String.format("'%s'", StringEscapeUtils.escapeEcmaScript(headerTitle))%>,
        canEdit: <%=canEdit%>,
        saveSettingsUrl: '<%=saveSettingsUrl%>'
      }));
    </script>
  </div>
</div>
