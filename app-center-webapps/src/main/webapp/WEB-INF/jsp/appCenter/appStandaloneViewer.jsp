<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.exoplatform.portal.application.PortalRequestContext"%>
<%
  String applicationId = PortalRequestContext.getCurrentInstance().getRequest().getParameter("applicationId");
  applicationId = StringUtils.isNumeric(applicationId) ? applicationId : null;
%>
<div class="VuetifyApp">
  <div
    data-app="true"
    class="v-application v-application--is-ltr theme--light"
    id="appStandaloneViewer">
  </div>
  <script>
    window.require(['SHARED/appStandaloneViewerBundle'], app => app.init(<%=applicationId%>));
  </script>
</div>
