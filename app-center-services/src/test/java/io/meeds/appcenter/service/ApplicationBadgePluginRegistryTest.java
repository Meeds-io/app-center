/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.appcenter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.storage.ApplicationBadgePortletStorage;

class ApplicationBadgePluginRegistryTest {

  private static final String            DRAWER2      = "aDrawer";

  private static final String            DRAWER1      = "someDrawer";

  private static final String            APP2         = "agenda/AgendaTimeline";

  private static final String            APP1         = "agenda/Agenda";

  private static final String            APP3         = "aPortlet";

  private static final String            BADGE_NAME   = "agendaPendingInvitations";

  /** What a PORTLET application actually stores: a portlet instance id */
  private static final String            CALENDAR_URL = "12";

  private static final String            TIMELINE_URL = "13";

  private ApplicationBadgePluginRegistry registry;

  private ApplicationBadgePortletStorage badgePortletStorage;

  @BeforeEach
  void setup() {
    registry = new ApplicationBadgePluginRegistry();
    badgePortletStorage = mock(ApplicationBadgePortletStorage.class);
    ReflectionTestUtils.setField(registry, "badgePortletStorage", badgePortletStorage);
    lenient().when(badgePortletStorage.getPortletContentId(CALENDAR_URL)).thenReturn(APP1);
    lenient().when(badgePortletStorage.getPortletContentId(TIMELINE_URL)).thenReturn(APP2);
    registry.addPlugin(plugin(BADGE_NAME, List.of(), List.of(APP1, APP2)));
  }

  @Test
  void addPluginIgnoresAnUnnamedPlugin() {
    registry.addPlugin(null);
    registry.addPlugin(plugin("  ", List.of(), List.of()));
    registry.addPlugin(plugin(null, List.of(), List.of()));

    assertEquals(1, registry.getPlugins().size());
    assertNull(registry.getPlugin("  "));
  }

  @Test
  void getPluginNamesIsSorted() {
    registry.addPlugin(plugin("aFirstBadge", List.of(DRAWER1), List.of()));

    assertEquals(List.of("aFirstBadge", BADGE_NAME), registry.getPluginNames());
  }

  @Test
  void resolveMapsAPortletInstanceIdToItsContentId() {
    // A PORTLET entry stores the instance id, never the portlet name: without
    // that mapping no portlet application could ever carry a badge
    assertEquals(BADGE_NAME, registry.resolveBadgeName(application(ApplicationType.PORTLET, CALENDAR_URL)));
  }

  @Test
  void resolveMatchesEveryPortletDeclaredByOnePlugin() {
    // The calendar and its timeline are two catalog entries reporting the very
    // same counter
    assertEquals(BADGE_NAME, registry.resolveBadgeName(application(ApplicationType.PORTLET, CALENDAR_URL)));
    assertEquals(BADGE_NAME, registry.resolveBadgeName(application(ApplicationType.PORTLET, TIMELINE_URL)));
  }

  @Test
  void resolveAlsoAcceptsABarePortletName() {
    registry.addPlugin(plugin("bareNameBadge", List.of(), List.of("SomePortlet")));
    when(badgePortletStorage.getPortletContentId("99")).thenReturn("someWebapp/SomePortlet");

    assertEquals("bareNameBadge", registry.resolveBadgeName(application(ApplicationType.PORTLET, "99")));
  }

  @Test
  void resolveIgnoresAUrlDeclaredForAnotherType() {
    // The portlet content ids must not leak into drawer matching
    assertNull(registry.resolveBadgeName(application(ApplicationType.DRAWER, APP1)));
    assertNull(registry.resolveBadgeName(application(ApplicationType.DRAWER, CALENDAR_URL)));
  }

  @Test
  void resolveIgnoresAnUnmappedInstanceAndALink() {
    assertNull(registry.resolveBadgeName(application(ApplicationType.PORTLET, "404")));
    assertNull(registry.resolveBadgeName(application(ApplicationType.LINK, CALENDAR_URL)));
    assertNull(registry.resolveBadgeName(application(null, CALENDAR_URL)));
    assertNull(registry.resolveBadgeName(application(ApplicationType.PORTLET, null)));
    assertNull(registry.resolveBadgeName(null));
  }

  @Test
  void resolvePrefersTheExplicitBinding() {
    Application application = application(ApplicationType.PORTLET, CALENDAR_URL);
    application.setBadgeName("someOtherBadge");

    assertEquals("someOtherBadge", registry.resolveBadgeName(application));
  }

  @Test
  void resolveReturnsNothingWhenTheAdministratorTurnedTheBadgeOff() {
    // Distinct from a blank value, which lets the url binding resolve again
    Application application = application(ApplicationType.PORTLET, CALENDAR_URL);
    application.setBadgeName(ApplicationBadgePluginRegistry.BADGE_DISABLED);

    assertNull(registry.resolveBadgeName(application));
  }

  @Test
  void getBoundUrlsTranslatesDeclaredPortletsBackToInstanceIds() {
    // The ACL reverse lookup queries on stored urls, so declared portlets have
    // to become instance ids again
    when(badgePortletStorage.getPortletInstanceUrls(List.of(APP1, APP2)))
                                                                         .thenReturn(List.of(CALENDAR_URL,
                                                                                             TIMELINE_URL));
    registry.addPlugin(plugin("withDrawer", List.of(DRAWER1), List.of()));

    assertEquals(List.of(CALENDAR_URL, TIMELINE_URL), registry.getBoundUrls(registry.getPlugin(BADGE_NAME)));
    assertEquals(List.of(DRAWER1), registry.getBoundUrls(registry.getPlugin("withDrawer")));
    assertTrue(registry.getBoundUrls(null).isEmpty());
  }

  @Test
  void matchesAcceptsBothAContentIdAndABarePortletName() {
    assertTrue(ApplicationBadgePortletStorage.matches(APP1, APP1));
    assertTrue(ApplicationBadgePortletStorage.matches("Agenda", APP1));
    assertFalse(ApplicationBadgePortletStorage.matches("other/Agenda", APP1));
    assertFalse(ApplicationBadgePortletStorage.matches("agenda", APP1));
    assertFalse(ApplicationBadgePortletStorage.matches(null, APP1));
  }

  @Test
  void declaredUrlsMergesBothTypesAndDropsBlanks() {
    ApplicationBadgePlugin plugin = plugin("mixed", List.of(DRAWER2, "  "), List.of(APP3, DRAWER2));

    assertEquals(List.of(DRAWER2, APP3), plugin.getDeclaredUrls());
    assertTrue(plugin.getDeclaredUrls(ApplicationType.LINK).isEmpty());
    assertNotNull(plugin.getDeclaredUrls(null));
  }

  private Application application(ApplicationType type, String url) {
    Application application = new Application();
    application.setId(1L);
    application.setType(type);
    application.setUrl(url);
    return application;
  }

  private ApplicationBadgePlugin plugin(String name, List<String> drawerNames, List<String> portletNames) {
    return new ApplicationBadgePlugin() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public long countBadge(String username) {
        return 0;
      }

      @Override
      public List<String> getDrawerNames() {
        return drawerNames;
      }

      @Override
      public List<String> getPortletNames() {
        return portletNames;
      }
    };
  }

}
