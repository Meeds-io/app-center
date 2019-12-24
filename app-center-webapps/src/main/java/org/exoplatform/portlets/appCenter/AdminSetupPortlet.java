/**
 * 
 */
package org.exoplatform.portlets.appCenter;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.PortletPreferences;


/**
 * @author Ayoub Zayati
 */
public class AdminSetupPortlet extends GenericPortlet {
  @Override
  protected void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/appCenter/adminSetup/view.jsp");
    dispatcher.forward(request, response);
  }

  @Override
  protected void doEdit(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/appCenter/adminSetup/edit.jsp");
    dispatcher.forward(request, response);
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws IOException, PortletException {
    PortletPreferences preferences = request.getPreferences();
    String pageSize = request.getParameter("pageSize");
    preferences.setValue("pageSize", pageSize);
    response.setRenderParameter("pageSize", pageSize);
    preferences.store();
  }
}
