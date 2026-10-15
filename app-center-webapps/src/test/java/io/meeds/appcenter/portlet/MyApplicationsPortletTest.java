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

import static io.meeds.social.portlet.CMSPortlet.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.ApplicationState;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.webui.Utils;

import io.meeds.appcenter.constant.WindowKind;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.appcenter.service.MyApplicationsCopyService;
import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.cms.service.CMSService;

@ExtendWith(MockitoExtension.class)
class MyApplicationsPortletTest {

  private static final String                 SETTING_TYPE       = MyApplicationsSettingsService.SETTING_TYPE;

  private static final String                 VIEW_PATH          = "/html/myApplications.html";

  private static final String                 STORAGE_ID         = "12";

  private static final String                 PAGE_REFERENCE     = "portal::dw::home";

  private static final long                   SPACE_ID           = 3L;

  private static final String                 VIEWER_IDENTITY_ID = "7";

  private static final String                 USERNAME           = "john";

  private static final String                 SECTION_DRAFT_PAGE = "portal::global::section_draft_7_john";

  private static final String                 PAYLOAD            = "{\"applications\":[{\"id\":1}]}";

  /** A site rendered with its own layout, as any site that is not displayed */
  private static final SiteKey                REQUESTED_SITE     = SiteKey.portal("public");

  @Mock
  private MyApplicationsSettingsService       settingsService;

  @Mock
  private CMSService                          cmsService;

  @Mock
  private MyApplicationsCopyService           copyService;

  @Mock
  private ApplicationCenterService            applicationCenterService;

  @Mock
  private LayoutService                       layoutService;

  @Mock
  private Application                         applicationModel;

  @Mock
  private ApplicationState                    applicationState;

  @Mock
  private PortletConfig                       portletConfig;

  @Mock
  private PortletContext                      portletContext;

  @Mock
  private PortletRequestDispatcher            requestDispatcher;

  @Mock
  private RenderRequest                       renderRequest;

  @Mock
  private RenderResponse                      renderResponse;

  @Mock
  private PortletPreferences                  preferences;

  @Mock
  private PortalRequestContext                portalRequestContext;

  @Mock
  private UIPortlet                           uiPortlet;

  @Mock
  private UIPage                              uiPage;

  @Mock
  private UIPortal                            uiPortal;

  /**
   * The values of the request preferences: a value set during the render is
   * the one read afterwards in that render, as the portlet container's
   * preferences of a render request do
   */
  private final Map<String, String>           preferenceValues   = new HashMap<>();

  /** The window's preferences, as stored in the layout */
  private final Portlet                       storedPreferences  = new Portlet();

  private MockedStatic<ExoContainerContext>   containerContext;

  private MockedStatic<UIPortlet>             currentPortlet;

  private MockedStatic<Util>                  portalUtil;

  private MockedStatic<SpaceUtils>            spaceUtils;

  private MockedStatic<Utils>                 socialUtils;

  private MyApplicationsPortlet               portlet;

  @BeforeEach
  void setUp() throws Exception {
    containerContext = mockStatic(ExoContainerContext.class);
    containerContext.when(() -> ExoContainerContext.getService(MyApplicationsSettingsService.class)).thenReturn(settingsService);
    containerContext.when(() -> ExoContainerContext.getService(CMSService.class)).thenReturn(cmsService);
    containerContext.when(() -> ExoContainerContext.getService(LayoutService.class)).thenReturn(layoutService);
    containerContext.when(() -> ExoContainerContext.getService(MyApplicationsCopyService.class)).thenReturn(copyService);
    containerContext.when(() -> ExoContainerContext.getService(ApplicationCenterService.class)).thenReturn(applicationCenterService);
    lenient().when(copyService.getOrStoreSettingName(anyString(), any()))
             .thenAnswer(invocation -> invocation.<Supplier<String>> getArgument(1).get());
    currentPortlet = mockStatic(UIPortlet.class);
    currentPortlet.when(UIPortlet::getCurrentUIPortlet).thenReturn(uiPortlet);
    portalUtil = mockStatic(Util.class);
    portalUtil.when(Util::getUIPage).thenReturn(uiPage);
    portalUtil.when(Util::getUIPortal).thenReturn(uiPortal);
    portalUtil.when(Util::getPortalRequestContext).thenReturn(portalRequestContext);
    Space space = new Space();
    space.setId(SPACE_ID);
    spaceUtils = mockStatic(SpaceUtils.class);
    spaceUtils.when(SpaceUtils::getSpaceByContext).thenReturn(space);
    socialUtils = mockStatic(Utils.class);
    socialUtils.when(Utils::getViewerIdentityId).thenReturn(VIEWER_IDENTITY_ID);

    lenient().when(portletConfig.getInitParameter("portlet-view-dispatched-file-path")).thenReturn(VIEW_PATH);
    lenient().when(portletConfig.getPortletContext()).thenReturn(portletContext);
    lenient().when(portletContext.getRequestDispatcher(VIEW_PATH)).thenReturn(requestDispatcher);
    lenient().when(renderRequest.getPreferences()).thenReturn(preferences);
    lenient().when(preferences.getValue(anyString(), any()))
             .thenAnswer(invocation -> preferenceValues.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
    lenient().doAnswer(invocation -> preferenceValues.put(invocation.getArgument(0), invocation.getArgument(1)))
             .when(preferences)
             .setValue(anyString(), anyString());
    lenient().when(uiPortlet.getStorageId()).thenReturn(STORAGE_ID);
    lenient().when(uiPage.getPageId()).thenReturn(PAGE_REFERENCE);
    lenient().when(layoutService.getApplicationModel(STORAGE_ID)).thenReturn(applicationModel);
    lenient().when(applicationModel.getState()).thenReturn(applicationState);
    lenient().when(layoutService.load(applicationState)).thenReturn(storedPreferences);
    lenient().when(settingsService.getContextSpaceId(anyString(), anyString())).thenReturn(SPACE_ID);
    lenient().when(settingsService.getSettingPageReference(anyString(), anyString(), any()))
             .thenAnswer(invocation -> invocation.getArgument(1));
    lenient().when(portalRequestContext.getSiteKey()).thenReturn(REQUESTED_SITE);
    lenient().when(portalRequestContext.getPortalConfig()).thenReturn(new PortalConfig());
    lenient().when(portalRequestContext.getMetaPortal()).thenReturn("meeds");

    portlet = new MyApplicationsPortlet();
    portlet.init(portletConfig);
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    socialUtils.close();
    spaceUtils.close();
    portalUtil.close();
    currentPortlet.close();
    containerContext.close();
  }

  /**
   * The new name is set on the preferences of the render request, the ones
   * passed to CMSPortlet#doView, which reads the name from them: fails the
   * mutant dropping that setValue, where CMSPortlet initializes and renders
   * the window with the name it gave up. The window's committed name is the
   * one it gives up, as in production: fails the mutant generating the new
   * name through generateRandomId, which adopts the committed name and would
   * keep the copied one
   */
  @Test
  void doViewRendersAWindowWithTheNameItGetsInPlaceOfOneItMustNotKeep() throws Exception {
    givenAPageWindow();
    String copiedName = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    lenient().when(copyService.getOrStoreSettingName(eq(STORAGE_ID), any())).thenReturn(copiedName);
    String newName = "Untitled-12-" + UUID.randomUUID();
    when(settingsService.generateSettingName(STORAGE_ID, WindowKind.PAGE)).thenReturn(newName);

    portlet.doView(renderRequest, renderResponse);

    verify(copyService).saveWindowPreference(STORAGE_ID, NAME, newName);
    verify(settingsService).copyHeaderTitle(copiedName, newName);
    verify(cmsService).saveSettingName(SETTING_TYPE, newName, PAGE_REFERENCE, SPACE_ID, Long.parseLong(VIEWER_IDENTITY_ID));
    InOrder inOrder = inOrder(preferences, renderRequest, requestDispatcher);
    inOrder.verify(preferences).setValue(NAME, newName);
    inOrder.verify(renderRequest).setAttribute("settingName", newName);
    inOrder.verify(requestDispatcher).include(renderRequest, renderResponse);
  }

  @Test
  void doViewKeepsTheNameOfAWindowThatKeepsIt() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(settingsService.keepsSettingName(eq(name),
                                          eq(STORAGE_ID),
                                          eq(WindowKind.PAGE),
                                          eq(PAGE_REFERENCE),
                                          any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(preferences, never()).setValue(anyString(), anyString());
    verify(copyService, never()).saveWindowPreference(any(), any(), any());
    verify(settingsService, never()).generateSettingName(anyString(), any());
    verify(renderRequest).setAttribute("settingName", name);
  }

  @Test
  void doViewChecksTheNameOfAWindowByItsKind() throws Exception {
    String name = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(settingsService.keepsSettingName(eq(name), eq(STORAGE_ID), any(), eq(PAGE_REFERENCE), any()))
                                                                                                                  .thenReturn(true);

    givenAPageWindow();
    portlet.doView(renderRequest, renderResponse);
    givenASiteLayoutWindow();
    portlet.doView(renderRequest, renderResponse);
    givenASharedLayoutWindow();
    portlet.doView(renderRequest, renderResponse);

    InOrder inOrder = inOrder(settingsService);
    inOrder.verify(settingsService).keepsSettingName(eq(name), any(), eq(WindowKind.PAGE), any(), any());
    inOrder.verify(settingsService).keepsSettingName(eq(name), any(), eq(WindowKind.SITE_LAYOUT), any(), any());
    inOrder.verify(settingsService).keepsSettingName(eq(name), any(), eq(WindowKind.SHARED_LAYOUT), any(), any());
  }

  /**
   * Fails the mutant dropping the scan of the site layout: a site-layout
   * window would keep a name another window of the site layout carries
   */
  @Test
  void doViewComparesASiteLayoutWindowWithTheOtherWindowsOfTheSiteAndOfThePage() throws Exception {
    givenASiteLayoutWindow();
    String name = givenTheWindowCarries("Untitled-99-site-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);
    givenTheWindows(uiPortal, window(STORAGE_ID, name), window("13", "site-window"), window("14", null));
    givenTheWindows(uiPage, window("15", "page-window"));

    portlet.doView(renderRequest, renderResponse);

    assertEquals(List.of("site-window", "page-window"), List.copyOf(otherWindowNames(name, WindowKind.SITE_LAYOUT)));
  }

  @Test
  void doViewComparesAPageWindowWithTheOtherWindowsOfThePageOnly() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);
    lenient().doAnswer(invocation -> {
      invocation.<List<UIPortlet>> getArgument(0).add(window("13", "site-window"));
      return null;
    }).when(uiPortal).findComponentOfType(any(), eq(UIPortlet.class));
    givenTheWindows(uiPage, window(STORAGE_ID, name), window("15", "page-window"));

    portlet.doView(renderRequest, renderResponse);

    assertEquals(List.of("page-window"), List.copyOf(otherWindowNames(name, WindowKind.PAGE)));
  }

  @Test
  void doViewNeverChecksTheNameOnADraftSite() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(portalRequestContext.isDraftSite()).thenReturn(true);
    // Were the name checked, it would be kept: the verification below is what fails
    lenient().when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService, never()).keepsSettingName(any(), any(), any(), any(), any());
    verify(preferences, never()).setValue(anyString(), anyString());
    verify(renderRequest).setAttribute("settingName", name);
  }

  @Test
  void doViewNeverChecksAWindowWithoutStorageId() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(uiPortlet.getStorageId()).thenReturn(null);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService, never()).keepsSettingName(any(), any(), any(), any(), any());
    verify(renderRequest).setAttribute("settingName", name);
  }

  /**
   * A configured shared layout gives its windows no storage id, so their names
   * must be configured: fails the mutant checking such a window, whose new
   * name could not be stored
   */
  @Test
  void doViewLeavesAWindowOfAConfiguredSharedLayoutWithItsConfiguredName() throws Exception {
    givenASharedLayoutWindow();
    when(uiPortlet.getStorageId()).thenReturn(null);
    String name = givenTheWindowCarries("shared-shortcuts");
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    // Were the name checked, it would be kept: the verification below is what fails
    lenient().when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService, never()).keepsSettingName(any(), any(), any(), any(), any());
    verify(preferences, never()).setValue(anyString(), anyString());
    verify(renderRequest).setAttribute("settingName", name);
  }

  /**
   * Fails the mutant marking the name of a window without a storage id, as a
   * window of a configured shared layout: CMSPortlet generates it
   */
  @Test
  void generateRandomIdOfAWindowWithoutStorageIdLeavesTheNameToCMSPortlet() {
    givenASharedLayoutWindow();
    when(uiPortlet.getStorageId()).thenReturn(null);

    String name = portlet.generateRandomId();

    verify(settingsService, never()).generateSettingName(any(), any());
    assertTrue(name.startsWith("Untitled-"));
    assertFalse(name.contains("-shared-"));
  }

  @Test
  void doViewTakesThePageADraftPageIsADraftOf() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-99-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(uiPage.getPageId()).thenReturn(PAGE_REFERENCE + "_draft_john");
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService).keepsSettingName(eq(name), eq(STORAGE_ID), eq(WindowKind.PAGE), eq(PAGE_REFERENCE), any());
  }

  @Test
  void generateRandomIdGeneratesANameNoSettingHolds() {
    givenASiteLayoutWindow();
    String takenName = "Untitled-12-site-" + UUID.randomUUID();
    String freeName = "Untitled-12-site-" + UUID.randomUUID();
    when(settingsService.generateSettingName(STORAGE_ID, WindowKind.SITE_LAYOUT)).thenReturn(takenName, freeName);
    when(cmsService.isSettingNameExists(SETTING_TYPE, takenName)).thenReturn(true);

    assertEquals(freeName, portlet.generateRandomId());
  }

  @Test
  void generateRandomIdOutsideAWindowGeneratesAnUnmarkedName() {
    currentPortlet.when(UIPortlet::getCurrentUIPortlet).thenReturn(null);

    assertTrue(portlet.generateRandomId().startsWith("Untitled--"));
    verify(settingsService, never()).generateSettingName(any(), any());
  }

  /**
   * CMSPortlet names a window it renders with no name: fails the mutant
   * generating the name here, which two first renders of a copy would do
   * twice, each with its own view of the window
   */
  @Test
  void generateRandomIdNamesTheWindowFromItsCommittedPreferences() {
    givenAPageWindow();
    when(copyService.getOrStoreSettingName(eq(STORAGE_ID), any())).thenReturn("Untitled-12-other");

    assertEquals("Untitled-12-other", portlet.generateRandomId());
    verify(settingsService, never()).generateSettingName(any(), any());
  }

  @Test
  void generateRandomIdOnASectionTemplateDraftPageMarksTheTemplate() {
    givenASectionTemplateDraftWindow();
    String name = "Untitled-12-sdraft-7-" + UUID.randomUUID();
    when(settingsService.generateSectionTemplateDraftSettingName(STORAGE_ID, 7L)).thenReturn(name);

    assertEquals(name, portlet.generateRandomId());
    verify(settingsService, never()).generateSettingName(any(), any());
  }

  /**
   * CMSPortlet's own write loads the window as the render first read it:
   * fails the mutant writing through it, which restores what another render
   * committed since
   */
  @Test
  void savePreferenceWritesOverTheCommittedPreferences() {
    givenAPageWindow();

    portlet.savePreference(NAME, "Untitled-12-a");

    verify(copyService).saveWindowPreference(STORAGE_ID, NAME, "Untitled-12-a");
    verify(layoutService, never()).save(any(ApplicationState.class), any(Portlet.class));
  }

  /**
   * Fails the mutant recording the page CMSPortlet hands over, cut at
   * _draft_, which names neither the template nor the administrator whose
   * earlier sessions the window cleans up
   */
  @Test
  void saveSettingNameRecordsTheSectionTemplateDraftPageUncut() throws Exception {
    givenASectionTemplateDraftWindow();
    String name = "Untitled-12-sdraft-7-" + UUID.randomUUID();
    when(settingsService.getContextSpaceId(name, SECTION_DRAFT_PAGE)).thenReturn(0L);

    portlet.saveSettingName(name, "portal::global::section", SPACE_ID);

    verify(cmsService).saveSettingName(SETTING_TYPE, name, SECTION_DRAFT_PAGE, 0L, Long.parseLong(VIEWER_IDENTITY_ID));
  }

  @Test
  void setViewRequestAttributesConsumesTheCopyAndRendersTheListItGot() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    preferenceValues.put("data.init", PAYLOAD);
    when(copyService.consumeWindowCopy(STORAGE_ID, name)).thenReturn(Map.of(MyApplicationsSettingsService.APPLICATION_IDS,
                                                                            "41,2",
                                                                            "data.init",
                                                                            ""));

    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);

    assertEquals("41,2", preferenceValues.get(MyApplicationsSettingsService.APPLICATION_IDS));
    assertEquals("", preferenceValues.get("data.init"));
  }

  /**
   * Consumption runs at each render until it succeeds, not at the name's
   * initialization only: fails the mutant skipping a window already named,
   * whose payload a failed first attempt would leave for good
   */
  @Test
  void setViewRequestAttributesConsumesTheCopyOfAWindowNamedBefore() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    preferenceValues.put("data.init", PAYLOAD);
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(copyService).consumeWindowCopy(STORAGE_ID, name);
    verify(cmsService, never()).saveSettingName(any(), any(), any(), anyLong(), anyLong());
  }

  @Test
  void setViewRequestAttributesOfAWindowCarryingNothingConsumesNothing() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());

    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);

    verify(copyService, never()).consumeWindowCopy(any(), any());
  }

  /**
   * The site layout editor renders the draft site: fails the mutant dropping
   * the draft site test, where a window would create applications in the
   * draft
   */
  @Test
  void setViewRequestAttributesConsumesNothingOnADraftSite() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    preferenceValues.put("data.init", PAYLOAD);
    when(portalRequestContext.isDraftSite()).thenReturn(true);

    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);

    verify(copyService, never()).consumeWindowCopy(any(), any());
  }

  /**
   * Fails the mutant letting a failed consumption fail the render, and the
   * mutant changing the list when another render is consuming the window
   */
  @Test
  void setViewRequestAttributesRendersTheListAsItIsWhenNothingIsConsumed() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    preferenceValues.put("data.init", PAYLOAD);
    preferenceValues.put(MyApplicationsSettingsService.APPLICATION_IDS, "1,2");
    when(copyService.consumeWindowCopy(STORAGE_ID, name)).thenReturn(null)
                                                         .thenThrow(new IllegalStateException("database unreachable"));

    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);
    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);

    assertEquals("1,2", preferenceValues.get(MyApplicationsSettingsService.APPLICATION_IDS));
    assertEquals(PAYLOAD, preferenceValues.get("data.init"));
  }

  @Test
  void postSettingInitOfASectionTemplateDraftWindowDeletesTheEarlierSessionsLeftovers() throws Exception {
    givenASectionTemplateDraftWindow();
    String name = "Untitled-12-sdraft-7-" + UUID.randomUUID();
    givenTheWindows(uiPage, window(STORAGE_ID, name), window("13", null));

    portlet.postSettingInit(preferences, name);

    verify(applicationCenterService).deleteSectionTemplateDraftLeftovers(name, SECTION_DRAFT_PAGE, List.of(STORAGE_ID, "13"));
  }

  @Test
  void postSettingInitOfAPageWindowDeletesNothing() {
    givenAPageWindow();

    portlet.postSettingInit(preferences, "Untitled-12-" + UUID.randomUUID());

    verify(applicationCenterService, never()).deleteSectionTemplateDraftLeftovers(any(), any(), any());
  }

  /**
   * Fails the mutant letting a failed cleanup fail the render, after which
   * the window's name is initialized and the cleanup never runs again
   */
  @Test
  void postSettingInitGoesOnWhenTheLeftoversCannotBeDeleted() throws Exception {
    givenASectionTemplateDraftWindow();
    String name = "Untitled-12-sdraft-7-" + UUID.randomUUID();
    givenTheWindows(uiPage, window(STORAGE_ID, name));
    doThrow(new IllegalStateException("database unreachable")).when(applicationCenterService)
                                                               .deleteSectionTemplateDraftLeftovers(any(), any(), any());

    portlet.postSettingInit(preferences, name);

    verify(applicationCenterService).deleteSectionTemplateDraftLeftovers(any(), any(), any());
  }

  /**
   * Fails the mutants recording, or taking the space of, the page CMSPortlet
   * hands over instead of the reference of the window's setting, and the
   * mutant taking a site-layout window for a shared-layout one
   */
  @Test
  void saveSettingNameRecordsTheReferenceAndTheSpaceOfTheWindowContext() throws Exception {
    givenASiteLayoutWindow();
    String name = "Untitled-12-site-" + UUID.randomUUID();
    when(settingsService.getSettingPageReference(name, "portal::global::profile", REQUESTED_SITE)).thenReturn("portal::meeds::profile");
    when(settingsService.getContextSpaceId(name, "portal::meeds::profile")).thenReturn(0L);

    portlet.saveSettingName(name, "portal::global::profile", SPACE_ID);

    verify(cmsService).saveSettingName(SETTING_TYPE, name, "portal::meeds::profile", 0L, Long.parseLong(VIEWER_IDENTITY_ID));
  }

  /**
   * A window of a configured shared layout has no storage id, so no generated
   * name to tell its kind: fails the mutant recording the page of its first
   * render, whose right and space its settings and applications would take
   */
  @Test
  void saveSettingNameRecordsTheSharedLayoutReferenceForAWindowOfASharedLayout() throws Exception {
    givenASharedLayoutWindow();
    String name = "shared-shortcuts";
    when(settingsService.getContextSpaceId(name, MyApplicationsSettingsService.SHARED_LAYOUT_REFERENCE)).thenReturn(0L);

    portlet.saveSettingName(name, PAGE_REFERENCE, SPACE_ID);

    verify(cmsService).saveSettingName(SETTING_TYPE,
                                       name,
                                       MyApplicationsSettingsService.SHARED_LAYOUT_REFERENCE,
                                       0L,
                                       Long.parseLong(VIEWER_IDENTITY_ID));
  }

  /**
   * Fails the mutant taking the requested site as the site of the layout: a
   * displayed site is rendered with the meta portal's layout
   */
  @Test
  void doViewTakesTheMetaPortalAsTheLayoutSiteOfADisplayedSite() throws Exception {
    givenASiteLayoutWindow();
    String name = givenTheWindowCarries("Untitled-12-site-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    PortalConfig displayedSite = new PortalConfig();
    displayedSite.setDisplayed(true);
    when(portalRequestContext.getPortalConfig()).thenReturn(displayedSite);
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService).getSettingPageReference(name, PAGE_REFERENCE, SiteKey.portal("meeds"));
  }

  /**
   * Fails the mutant comparing the rendered page: a site-layout window shown
   * with a page of another site, or under another displayed site, would give
   * up its name
   */
  @Test
  void doViewChecksTheNameOfAWindowAgainstTheReferenceItsSettingRecords() throws Exception {
    givenASiteLayoutWindow();
    String name = givenTheWindowCarries("Untitled-99-site-" + UUID.randomUUID());
    when(cmsService.isSettingNameExists(SETTING_TYPE, name)).thenReturn(true);
    when(uiPage.getPageId()).thenReturn("portal::global::activity");
    when(settingsService.getSettingPageReference(name, "portal::global::activity", REQUESTED_SITE)).thenReturn("portal::meeds::activity");
    when(settingsService.keepsSettingName(any(), any(), any(), any(), any())).thenReturn(true);

    portlet.doView(renderRequest, renderResponse);

    verify(settingsService).keepsSettingName(eq(name),
                                             eq(STORAGE_ID),
                                             eq(WindowKind.SITE_LAYOUT),
                                             eq("portal::meeds::activity"),
                                             any());
  }

  /**
   * Fails the mutant dropping the draft site test of canEdit: the site layout
   * editor renders the draft site, where the settings drawer is not offered
   */
  @Test
  void canEditIsRefusedOnADraftSite() {
    Identity identity = new Identity(USERNAME);
    when(settingsService.canEditSettings("name", USERNAME)).thenReturn(true);

    assertTrue(portlet.canEdit("name", identity));
    when(portalRequestContext.isDraftSite()).thenReturn(true);
    assertFalse(portlet.canEdit("name", identity));
  }

  @Test
  void setViewRequestAttributesCopiesALegacyHeaderTitleOnce() throws Exception {
    givenAPageWindow();
    String name = givenTheWindowCarries("Untitled-12-" + UUID.randomUUID());
    preferenceValues.put("applicationId", "4242");

    portlet.setViewRequestAttributes(name, renderRequest, renderResponse);

    verify(settingsService).copyHeaderTitle("4242", name);
    verify(copyService).saveWindowPreference(STORAGE_ID, "applicationId", "");
  }

  @Test
  void processActionStoresTheSettingsTheUserMayWrite() throws Exception {
    ActionRequest request = actionRequest(Map.of(MyApplicationsSettingsService.LISTING_MODE, "SELECTED"));
    PortletPreferences actionPreferences = request.getPreferences();
    when(settingsService.getSettingsToStore("Untitled-12-name", USERNAME, Map.of(MyApplicationsSettingsService.LISTING_MODE, "SELECTED")))
                                                                                                                         .thenReturn(Map.of(MyApplicationsSettingsService.LISTING_MODE,
                                                                                                                                            "SELECTED"));

    portlet.processAction(request, mock(ActionResponse.class));

    InOrder inOrder = inOrder(actionPreferences);
    inOrder.verify(actionPreferences).setValue(MyApplicationsSettingsService.LISTING_MODE, "SELECTED");
    inOrder.verify(actionPreferences).setValue("data.init", "");
    inOrder.verify(actionPreferences).store();
  }

  @Test
  void processActionRefusesTheSettingsTheServiceRefuses() throws Exception {
    ActionRequest request = actionRequest(Map.of());
    PortletPreferences actionPreferences = request.getPreferences();
    doThrow(new IllegalAccessException("not allowed")).when(settingsService).getSettingsToStore(any(), any(), any());
    assertThrows(PortletException.class, () -> portlet.processAction(request, mock(ActionResponse.class)));

    doThrow(new IllegalArgumentException("refused")).when(settingsService).getSettingsToStore(any(), any(), any());
    assertThrows(PortletException.class, () -> portlet.processAction(request, mock(ActionResponse.class)));
    verify(actionPreferences, never()).store();
  }

  /**
   * Fails the mutant dropping the draft site test of processAction
   */
  @Test
  void processActionIsRefusedOnADraftSite() throws Exception {
    ActionRequest request = actionRequest(Map.of(MyApplicationsSettingsService.LISTING_MODE, "SELECTED"));
    PortletPreferences actionPreferences = request.getPreferences();
    when(portalRequestContext.isDraftSite()).thenReturn(true);

    assertThrows(PortletException.class, () -> portlet.processAction(request, mock(ActionResponse.class)));
    verify(settingsService, never()).getSettingsToStore(any(), any(), any());
    verify(actionPreferences, never()).store();
  }

  private void givenASectionTemplateDraftWindow() {
    givenAPageWindow();
    lenient().when(uiPage.getPageId()).thenReturn(SECTION_DRAFT_PAGE);
    lenient().when(settingsService.getSectionTemplateId(SECTION_DRAFT_PAGE)).thenReturn(7L);
  }

  private void givenAPageWindow() {
    lenient().when(uiPortlet.getAncestorOfType(UIPage.class)).thenReturn(uiPage);
    lenient().when(uiPortlet.getAncestorOfType(UIPortal.class)).thenReturn(uiPortal);
  }

  private void givenASiteLayoutWindow() {
    lenient().when(uiPortlet.getAncestorOfType(UIPage.class)).thenReturn(null);
    lenient().when(uiPortlet.getAncestorOfType(UIPortal.class)).thenReturn(uiPortal);
  }

  private void givenASharedLayoutWindow() {
    lenient().when(uiPortlet.getAncestorOfType(UIPage.class)).thenReturn(null);
    lenient().when(uiPortlet.getAncestorOfType(UIPortal.class)).thenReturn(null);
  }

  private String givenTheWindowCarries(String name) {
    preferenceValues.put(NAME, name);
    return name;
  }

  private void givenTheWindows(org.exoplatform.webui.core.UIContainer container, UIPortlet... windows) {
    doAnswer(invocation -> {
      Collections.addAll(invocation.<List<UIPortlet>> getArgument(0), windows);
      return null;
    }).when(container).findComponentOfType(any(), eq(UIPortlet.class));
  }

  private UIPortlet window(String storageId, String name) throws Exception {
    UIPortlet window = mock(UIPortlet.class);
    lenient().when(window.getStorageId()).thenReturn(storageId);
    Portlet windowPreferences = new Portlet();
    if (name != null) {
      windowPreferences.setValue(NAME, name);
    }
    lenient().when(window.getPreferences()).thenReturn(windowPreferences);
    return window;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> otherWindowNames(String name, WindowKind windowKind) {
    ArgumentCaptor<Supplier<Collection<String>>> otherWindowNames = ArgumentCaptor.forClass(Supplier.class);
    verify(settingsService).keepsSettingName(eq(name),
                                             eq(STORAGE_ID),
                                             eq(windowKind),
                                             any(),
                                             otherWindowNames.capture());
    return otherWindowNames.getValue().get();
  }

  private ActionRequest actionRequest(Map<String, String> parameters) throws Exception {
    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
    ActionRequest request = mock(ActionRequest.class);
    PortletPreferences actionPreferences = mock(PortletPreferences.class);
    lenient().when(request.getPreferences()).thenReturn(actionPreferences);
    lenient().when(actionPreferences.getValue(NAME, null)).thenReturn("Untitled-12-name");
    lenient().when(request.getParameterNames()).thenReturn(Collections.enumeration(parameters.keySet()));
    lenient().when(request.getParameter(anyString())).thenAnswer(invocation -> parameters.get(invocation.getArgument(0)));
    return request;
  }

}
