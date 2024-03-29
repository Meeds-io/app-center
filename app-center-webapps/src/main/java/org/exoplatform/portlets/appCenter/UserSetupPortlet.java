/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/appCenter/userSetup/view.jsp");
    dispatcher.forward(request, response);
  }
  
  @Override
  protected void doEdit(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/appCenter/userSetup/edit.jsp");
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
