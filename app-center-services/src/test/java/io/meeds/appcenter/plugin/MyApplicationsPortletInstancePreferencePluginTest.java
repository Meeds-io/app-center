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
package io.meeds.appcenter.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.layout.model.PortletInstanceContext;
import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.service.PortletInstanceService;

import jakarta.annotation.PostConstruct;

@SpringBootTest(classes = { MyApplicationsPortletInstancePreferencePlugin.class })
@ActiveProfiles("layout")
class MyApplicationsPortletInstancePreferencePluginTest {

  /** Not reset: the registration happens once, when the context starts */
  @MockitoBean(reset = MockReset.NONE)
  private PortletInstanceService                         portletInstanceService;

  @MockitoBean
  private MyApplicationsCopyService                      copyService;

  @Autowired
  private MyApplicationsPortletInstancePreferencePlugin plugin;

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * Layout collects only its own context's plugins: fails the mutants
   * removing the registration or its {@link PostConstruct}, where every copy
   * would carry the source window's setting and none of its applications
   */
  @Test
  void registersOnStartupForTheShortcutsPortlet() throws NoSuchMethodException {
    verify(portletInstanceService).addPortletInstancePreferencePlugin(plugin);
    assertTrue(MyApplicationsPortletInstancePreferencePlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    assertEquals("AppCenterMyApplicationsPortlet", plugin.getPortletName());
  }

  @Test
  void generatePreferencesHandsTheWindowPreferencesAndTheCopyingUserOver() {
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    Map<String, String> copyPreferences = new LinkedHashMap<>();
    copyPreferences.put("applicationIds", "1,2");
    copyPreferences.put("data.init", "{}");
    when(copyService.getCopyPreferences(Map.of("name", "Untitled-12-a", "applicationIds", "1,2"), false, "john"))
                                                                                                           .thenReturn(copyPreferences);

    List<PortletInstancePreference> preferences = plugin.generatePreferences(new Application(),
                                                                             portlet("name", "Untitled-12-a", "applicationIds", "1,2"),
                                                                             new PortletInstanceContext(false, null));

    assertEquals(List.of(new PortletInstancePreference("applicationIds", "1,2"), new PortletInstancePreference("data.init", "{}")),
                 preferences);
  }

  /**
   * Fails the mutant ignoring the draft property, where the site layout
   * editor's draft would turn every window into a copy of itself
   */
  @Test
  void generatePreferencesTellsADraftOfTheSameWindow() {
    plugin.generatePreferences(new Application(), portlet(), new PortletInstanceContext(false, Map.of("draft", "true")));

    verify(copyService).getCopyPreferences(Map.of(), true, null);
  }

  @Test
  void generatePreferencesTakesAnyOtherContextForACopy() {
    plugin.generatePreferences(new Application(), portlet(), null);
    plugin.generatePreferences(new Application(), portlet(), new PortletInstanceContext(true, null));
    plugin.generatePreferences(new Application(), portlet(), new PortletInstanceContext(false, Map.of("draft", "false")));

    verify(copyService, times(3)).getCopyPreferences(Map.of(), false, null);
  }

  @Test
  void generatePreferencesOfAWindowWithoutPreferences() {
    plugin.generatePreferences(new Application(), null, null);

    verify(copyService).getCopyPreferences(isNull(), eq(false), isNull());
    verify(copyService, never()).getCopyPreferences(eq(Map.of()), anyBoolean(), isNull());
  }

  private Portlet portlet(String... namesAndValues) {
    Portlet portlet = new Portlet();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      portlet.setValue(namesAndValues[i], namesAndValues[i + 1]);
    }
    return portlet;
  }

}
