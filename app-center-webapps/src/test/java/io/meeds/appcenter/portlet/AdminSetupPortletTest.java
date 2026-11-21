/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.appcenter.portlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletPreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AdminSetupPortletTest {

  private static final Path JSP_FOLDER = Path.of("src/main/webapp/WEB-INF/jsp/appCenter/adminSetup");

  @Test
  void aPositivePageSizeIsStored() throws Exception {
    PortletPreferences preferences = mock(PortletPreferences.class);
    ActionResponse response = mock(ActionResponse.class);
    new AdminSetupPortlet().processAction(actionRequest(" 25 ", preferences), response);
    verify(preferences).setValue("pageSize", "25");
    verify(response).setRenderParameter("pageSize", "25");
    verify(preferences).store();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = { "", "0", "-5", "abc", "12'", "10\"</script>", "99999999999" })
  void anyOtherPageSizeIsNotStored(String pageSize) throws Exception {
    PortletPreferences preferences = mock(PortletPreferences.class);
    ActionResponse response = mock(ActionResponse.class);
    new AdminSetupPortlet().processAction(actionRequest(pageSize, preferences), response);
    verify(preferences, never()).setValue(nullable(String.class), nullable(String.class));
    verify(response, never()).setRenderParameter(nullable(String.class), nullable(String.class));
    verify(preferences, never()).store();
  }

  @Test
  void aStoredPageSizeIsReadAsAPositiveNumber() {
    assertEquals("25", AdminSetupPortlet.getPageSize("25"));
    assertEquals("", AdminSetupPortlet.getPageSize(null));
    assertEquals("", AdminSetupPortlet.getPageSize("0"));
    assertEquals("", AdminSetupPortlet.getPageSize("10'"));
    assertEquals("", AdminSetupPortlet.getPageSize("<!--<script>"));
  }

  @Test
  void theViewsPrintTheParsedPageSize() throws Exception {
    String view = Files.readString(JSP_FOLDER.resolve("view.jsp"), StandardCharsets.UTF_8);
    assertTrue(view.contains("String pageSize = AdminSetupPortlet.getPageSize(preferences.getValue(\"pageSize\", null));"));
    assertTrue(view.contains("pageSize: <%=Utils.toScriptString(pageSize)%>"));
    String edit = Files.readString(JSP_FOLDER.resolve("edit.jsp"), StandardCharsets.UTF_8);
    assertTrue(edit.contains("String pageSize = AdminSetupPortlet.getPageSize("));
    assertFalse(edit.contains("pageSize = renderRequest"));
    assertFalse(edit.contains("pageSize = preferences"));
    assertTrue(edit.contains("<input name=\"pageSize\" type=\"text\" value=\"<%=pageSize%>\">"));
    // the parsed page size is the only one the form prints
    assertEquals(1, edit.split("<%=pageSize", -1).length - 1);
    assertFalse(edit.contains("<%=pageSizeParameter"));
    assertFalse(edit.contains("<%=preferences"));
  }

  private ActionRequest actionRequest(String pageSize, PortletPreferences preferences) {
    ActionRequest request = mock(ActionRequest.class);
    when(request.getPreferences()).thenReturn(preferences);
    when(request.getParameter("pageSize")).thenReturn(pageSize);
    return request;
  }

}
