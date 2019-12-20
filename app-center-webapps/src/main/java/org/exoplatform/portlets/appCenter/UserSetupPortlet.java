/**
 * 
 */
package org.exoplatform.portlets.appCenter;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * @author Ayoub Zayati
 *
 */
public class UserSetupPortlet extends GenericPortlet {
  @Override
  protected void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/appCenter/userSetup/view.jsp");
    dispatcher.forward(request, response);
  }
  
  @Override
  protected void doEdit(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/appCenter/userSetup/edit.jsp");
    dispatcher.forward(request, response);
  }

  @Override
  public void processAction(ActionRequest request, ActionResponse response) throws IOException, PortletException {
    PortletPreferences preferences = request.getPreferences();
    String maxApps = request.getParameter("maxApps");
    preferences.setValue("maxApps", maxApps);
    response.setRenderParameter("maxApps", maxApps);
    preferences.store();
  }
}
