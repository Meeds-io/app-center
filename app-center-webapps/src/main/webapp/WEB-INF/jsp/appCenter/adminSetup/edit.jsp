<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletMode" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="io.meeds.appcenter.portlet.AdminSetupPortlet" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ page import="java.util.ResourceBundle"%>

<portlet:defineObjects/>
<%
  ResourceBundle resource = portletConfig.getResourceBundle(renderRequest.getLocale());
	PortletURL actionURL = renderResponse.createActionURL();
	PortletPreferences preferences = renderRequest.getPreferences();
	String pageSizeParameter = renderRequest.getParameter("pageSize");
	String pageSize = AdminSetupPortlet.getPageSize(pageSizeParameter == null ? preferences.getValue("pageSize", null) : pageSizeParameter);
%>
<div class="editpref-form UIFormWithTitle">
	<form class="formLogin" action="<%=actionURL%>" method="post">
		<div class="uiContentBox">
			<div class="form-horizontal">
				<div class="control-group">
					<label class="control-label portlet-form-label">
						<%=resource.getString("appCenter.editpref.form.pageSize")%>
					</label>
					<div class="controls portlet-input-field">
						<input name="pageSize" type="text" value="<%=pageSize%>">
					</div>
				</div>
			</div>
			<div class="uiAction uiActionBorder">
				<button type="submit" class="btn"><%=resource.getString("appCenter.editpref.form.save")%></button>
			</div>
		</div>
	</form>
</div>
