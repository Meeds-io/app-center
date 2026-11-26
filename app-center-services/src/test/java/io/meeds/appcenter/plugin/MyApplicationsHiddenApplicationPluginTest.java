/**
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ListResourceBundle;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.appcenter.service.MyApplicationsSettingsService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.translation.service.TranslationService;

@ExtendWith(MockitoExtension.class)
class MyApplicationsHiddenApplicationPluginTest {

  private static final String                   NAME           = "Untitled-12-abc";

  private static final String                   PAGE_REFERENCE = "portal::dw::overview";

  @Mock
  private CMSService                            cmsService;

  @Mock
  private MyApplicationsSettingsService         myApplicationsSettingsService;

  @Mock
  private TranslationService                    translationService;

  @Mock
  private ResourceBundleService                 resourceBundleService;

  @InjectMocks
  private MyApplicationsHiddenApplicationPlugin plugin;

  @BeforeEach
  void setUp() {
    lenient().when(resourceBundleService.getResourceBundle(eq(MyApplicationsHiddenApplicationPlugin.MY_APPLICATIONS_BUNDLE_NAME),
                                                           any(Locale.class)))
             .thenReturn(new ListResourceBundle() {
               @Override
               protected Object[][] getContents() {
                 return new Object[][] { { MyApplicationsHiddenApplicationPlugin.DEFAULT_NAME_KEY, "Shortcuts" } };
               }
             });
  }

  @Test
  void getApplicationOfAnUnknownWindowIsNull() {
    assertNull(plugin.getApplication(NAME, "fr"));
  }

  @Test
  void getApplicationTitleIsTheHeaderOfTheWindow() {
    mockSetting();
    when(myApplicationsSettingsService.isHideAllowed(NAME)).thenReturn(true);
    when(translationService.getTranslationLabelOrDefault(MyApplicationsSettingsService.SETTING_TYPE,
                                                         NAME,
                                                         MyApplicationsSettingsService.HEADER_TITLE_FIELD,
                                                         Locale.FRENCH)).thenReturn("Mes raccourcis");

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals(MyApplicationsSettingsService.APPLICATION_TYPE, application.getType());
    assertEquals("Mes raccourcis", application.getTitle());
    assertEquals(PAGE_REFERENCE, application.getPageReference());
    assertTrue(application.isAllowHide());
  }

  @Test
  void getApplicationTitleWithoutHeaderIsTheDefaultName() {
    mockSetting();

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals("Shortcuts", application.getTitle());
    assertFalse(application.isAllowHide());
  }

  @Test
  void canViewIsTheAccessPermissionOfTheWindow() {
    Identity identity = new Identity("user");
    when(cmsService.hasAccessPermission(identity, MyApplicationsSettingsService.SETTING_TYPE, NAME)).thenReturn(true);
    assertTrue(plugin.canView(NAME, identity));
    assertFalse(plugin.canView(NAME, new Identity("other")));
  }

  @Test
  void sharedLayoutWindowHasNoPageAndShowsToEveryUser() {
    mockSetting();
    when(myApplicationsSettingsService.isSharedLayoutWindow(NAME, PAGE_REFERENCE)).thenReturn(true);

    assertNull(plugin.getApplication(NAME, "fr").getPageReference());
    assertTrue(plugin.canView(NAME, new Identity("user")));
    assertFalse(plugin.canView(NAME, new Identity(org.exoplatform.services.security.IdentityConstants.ANONIM)));
    assertFalse(plugin.canView(NAME, null));
  }

  @Test
  void siteLayoutWindowHasNoPageAndShowsToTheUsersOfItsSite() {
    mockSetting();
    when(myApplicationsSettingsService.isSiteLayoutWindow(NAME, PAGE_REFERENCE)).thenReturn(true);
    Identity siteUser = new Identity("siteUser");
    when(myApplicationsSettingsService.canViewSiteLayoutWindow(PAGE_REFERENCE, siteUser)).thenReturn(true);

    assertNull(plugin.getApplication(NAME, "fr").getPageReference(),
               "A site-layout window is in no page: Hidden content would drop it for its recorded page");
    assertTrue(plugin.canView(NAME, siteUser));
    assertFalse(plugin.canView(NAME, new Identity("other")));
  }

  private void mockSetting() {
    when(cmsService.getSetting(MyApplicationsSettingsService.SETTING_TYPE, NAME)).thenReturn(new CMSSetting(MyApplicationsSettingsService.SETTING_TYPE,
                                                                                                            NAME,
                                                                                                            PAGE_REFERENCE,
                                                                                                            0));
  }

}
