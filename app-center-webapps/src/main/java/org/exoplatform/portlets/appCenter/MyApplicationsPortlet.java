/**
 * 
 */
package org.exoplatform.portlets.appCenter;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;


/**
 * @author Ayoub Zayati
 */
public class MyApplicationsPortlet extends GenericPortlet {
  @Override
  protected void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/jsp/appCenter/myApplications/view.jsp");
    dispatcher.forward(request, response);
  }

}
