<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletMode" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ page import="java.util.ResourceBundle"%>

<portlet:defineObjects/>
<%
    ResourceBundle resource = portletConfig.getResourceBundle(renderRequest.getLocale());


	PortletURL actionURL = renderResponse.createActionURL();
	PortletPreferences preferences = renderRequest.getPreferences();
	String maxApps = renderRequest.getParameter("maxApps");
	if (maxApps == null) {
	  maxApps = preferences.getValue("maxApps", "");
		if (maxApps == null) {
		  maxApps = "";
		}
	}
%>

<div class="editpref-form UIFormWithTitle">
	<form class="formLogin" action="<%=actionURL%>" method="post">
		<div class="uiContentBox">
			<div class="form-horizontal">
				<div class="control-group">
					<label class="control-label portlet-form-label">
						<%=resource.getString("editpref.form.maxApps")%>
					</label>
					<div class="controls portlet-input-field">
						<input name="maxApps" type="text" value="<%=maxApps%>">
					</div>
				</div>
			</div>
			<div class="uiAction uiActionBorder">
				<button type="submit" class="btn"><%=resource.getString("editpref.form.save")%></button>
			</div>
		</div>
	</form>
</div>