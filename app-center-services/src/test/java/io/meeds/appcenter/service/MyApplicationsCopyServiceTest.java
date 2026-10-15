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
package io.meeds.appcenter.service;

import static io.meeds.appcenter.service.MyApplicationsCopyService.APPLICATION_ID_PREFERENCE;
import static io.meeds.appcenter.service.MyApplicationsCopyService.DATA_INIT_PREFERENCE;
import static io.meeds.appcenter.service.MyApplicationsCopyService.NAME_PREFERENCE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileStorageException;

import org.apache.commons.lang3.StringUtils;

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCopy;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationListCopy;
import io.meeds.appcenter.model.exception.WindowPreferencesTimeoutException;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.PortletWindow;
import io.meeds.appcenter.storage.PortletWindowStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

@SpringBootTest(classes = { MyApplicationsCopyService.class })
@ExtendWith(MockitoExtension.class)
class MyApplicationsCopyServiceTest {

  private static final String      SETTING_NAME = "Untitled-12-1b4e28ba";

  private static final String      COPY_NAME    = "Untitled-34-2c5f39cb";

  private static final String      OTHER_NAME   = "Untitled-56-3d6a4adc";

  private static final String      USERNAME     = "john";

  private static final String      STORAGE_ID   = "34";

  private static final String      APP_TYPE     = ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE;

  private static final String      HEADER_TYPE  = MyApplicationsSettingsService.SETTING_TYPE;

  private static final String      HEADER_FIELD = MyApplicationsSettingsService.HEADER_TITLE_FIELD;

  private static final byte[]      PNG          = new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0 };

  private static final List<String> SPACE_AUDIENCE = List.of("*:/spaces/team");

  @MockitoBean
  private ApplicationCenterService      applicationCenterService;

  @MockitoBean
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @MockitoBean
  private TranslationService            translationService;

  @MockitoBean
  private PortletWindowStorage          windowStorage;

  /** The window's preferences as committed, which the storage reads and writes */
  private final Map<String, String> committed = new HashMap<>();

  /** The window a task of the storage is given */
  private final PortletWindow       window    = mock(PortletWindow.class);

  /** The end of the last window task, run by the storage once the task ended */
  private Runnable                  taskEnd;

  @Autowired
  private MyApplicationsCopyService copyService;

  @BeforeEach
  void setUp() throws Exception {
    lenient().when(translationService.getTranslationField(anyString(), anyString(), anyString()))
             .thenThrow(new ObjectNotFoundException("no labels"));
    lenient().when(applicationCenterService.canAccess(any(), anyString())).thenReturn(true);
    // The rule itself, which reads the application only
    lenient().when(applicationCenterService.isShown(any(), any(), any(), anyBoolean())).thenCallRealMethod();
    // The service is the Spring context's, shared by the tests: no claim
    // outlives the test that took it
    ((Set<?>) ReflectionTestUtils.getField(copyService, "consumingWindows")).clear();
    lenient().when(windowStorage.getPreferences(STORAGE_ID)).thenAnswer(invocation -> new HashMap<>(committed));
    lenient().doAnswer(invocation -> {
      committed.putAll(invocation.getArgument(1));
      return null;
    }).when(windowStorage).savePreferences(eq(STORAGE_ID), any());
    lenient().when(window.getPreferences()).thenAnswer(invocation -> new HashMap<>(committed));
    lenient().doAnswer(invocation -> {
      committed.putAll(invocation.getArgument(0));
      return null;
    }).when(window).savePreferences(any());
    lenient().when(windowStorage.callOnWindow(eq(STORAGE_ID), any(), any())).thenAnswer(invocation -> {
      taskEnd = invocation.getArgument(2);
      try {
        return invocation.<Function<PortletWindow, ?>> getArgument(1).apply(window);
      } finally {
        taskEnd.run();
      }
    });
    lenient().when(windowStorage.getOrSetPreference(eq(STORAGE_ID), anyString(), anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(1);
      return StringUtils.isNotBlank(committed.get(name)) ? committed.get(name)
                                                         : committed.merge(name, invocation.getArgument(2), (a, b) -> b);
    });
  }

  @Test
  void getCopyPreferencesCarriesTheWindowApplicationsAndHeaderTitleInPlaceOfItsSetting() throws Exception {
    Application owned = contextApplication(1L, SETTING_NAME);
    owned.setImageFileId(9L);
    when(applicationCenterService.getApplicationImageInputStream(1L)).thenReturn(new ByteArrayInputStream(PNG));
    givenLabels(APP_TYPE, "1", "title", Map.of(Locale.FRENCH, "Wiki fr", Locale.ENGLISH, "Wiki"));
    givenLabels(APP_TYPE, "1", "description", Map.of(Locale.ENGLISH, "<b>Wiki</b>"));
    givenLabels(HEADER_TYPE, SETTING_NAME, HEADER_FIELD, Map.of(Locale.ENGLISH, "Shortcuts"));
    appCenterApplication(2L);
    contextApplication(3L, OTHER_NAME);

    Map<String, String> copyPreferences = copyService.getCopyPreferences(preferences(SETTING_NAME, "1,2,3", null), false, USERNAME);

    assertFalse(copyPreferences.containsKey(NAME_PREFERENCE));
    assertFalse(copyPreferences.containsKey(APPLICATION_ID_PREFERENCE));
    assertEquals("SELECTED", copyPreferences.get(MyApplicationsSettingsService.LISTING_MODE));
    assertEquals("1,2,3", copyPreferences.get(MyApplicationsSettingsService.APPLICATION_IDS));
    ApplicationListCopy listCopy = JsonUtils.fromJsonString(copyPreferences.get(DATA_INIT_PREFERENCE), ApplicationListCopy.class);
    assertEquals(Map.of("en", "Shortcuts"), listCopy.getHeaderTitles());
    assertEquals(1, listCopy.getApplications().size());
    ApplicationCopy copy = listCopy.getApplications().get(0);
    assertEquals(1L, copy.getId());
    assertEquals("Wiki 1", copy.getTitle());
    assertEquals(ApplicationType.LINK, copy.getType());
    assertEquals("https://wiki.example.org/1", copy.getUrl());
    assertTrue(copy.isSameTab());
    assertEquals(Map.of("fr", "Wiki fr", "en", "Wiki"), copy.getTitles());
    assertEquals(Map.of("en", "<b>Wiki</b>"), copy.getDescriptions());
    assertArrayEquals(PNG, Base64.getDecoder().decode(copy.getIllustration()));
    // Every application is in the user's audience: no settings right to read
    verify(myApplicationsSettingsService, never()).canEditSettings(any(), any());
  }

  /**
   * A viewer's read, such as layout's impersonated page read, carries no
   * application outside the viewer's audience, even one the window manages:
   * fails the mutant dropping the access filter, and the one carrying the
   * window's managed applications without the settings right
   */
  @Test
  void getCopyPreferencesForAViewerCarriesOnlyTheActiveApplicationsTheViewerAccesses() throws Exception {
    Application hidden = contextApplication(1L, SETTING_NAME);
    hidden.setPermissions(SPACE_AUDIENCE);
    when(applicationCenterService.canAccess(hidden, USERNAME)).thenReturn(false);
    Application inactive = contextApplication(2L, SETTING_NAME);
    inactive.setActive(false);
    contextApplication(3L, SETTING_NAME);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, USERNAME)).thenReturn(false);

    assertEquals(List.of(3L), copiedIds(copyService.getCopyPreferences(preferences(SETTING_NAME, "1,2,3", null), false, USERNAME)));
  }

  /**
   * A user who may edit the window's settings gets a copy carrying what the
   * drawer lists them, the window's applications outside their audience
   * included, except one whose audience an administrator emptied (PO,
   * 2026-10-15): fails the mutant dropping the editor's branch, and the one
   * dropping its managed-application condition
   */
  @Test
  void getCopyPreferencesForAnEditorCarriesTheApplicationsTheWindowManagesOutsideTheirAudience() throws Exception {
    Application outsideAudience = contextApplication(1L, SETTING_NAME);
    outsideAudience.setPermissions(SPACE_AUDIENCE);
    when(applicationCenterService.canAccess(outsideAudience, USERNAME)).thenReturn(false);
    Application emptiedAudience = contextApplication(2L, SETTING_NAME);
    when(applicationCenterService.canAccess(emptiedAudience, USERNAME)).thenReturn(false);
    contextApplication(3L, SETTING_NAME);
    when(myApplicationsSettingsService.canEditSettings(SETTING_NAME, USERNAME)).thenReturn(true);

    assertEquals(List.of(1L, 3L),
                 copiedIds(copyService.getCopyPreferences(preferences(SETTING_NAME, "1,2,3", null), false, USERNAME)));
  }

  /**
   * With no user, as at startup or in a job, every application is copied:
   * fails the mutant checking access for a null user
   */
  @Test
  void getCopyPreferencesWithNoUserCarriesEveryApplication() throws Exception {
    contextApplication(1L, SETTING_NAME);
    contextApplication(2L, SETTING_NAME);

    assertEquals(List.of(1L, 2L), copiedIds(copyService.getCopyPreferences(preferences(SETTING_NAME, "1,2", null), false, null)));
    verify(applicationCenterService, never()).canAccess(any(), any());
    verify(myApplicationsSettingsService, never()).canEditSettings(any(), any());
  }

  /**
   * A copy not rendered yet is copied again with the payload it carries:
   * fails the mutant reading the window's applications again, a section
   * template being saved, then instantiated, before any render
   */
  @Test
  void getCopyPreferencesPassesAnUnconsumedPayloadThroughWhateverTheName() {
    Map<String, String> preferences = preferences(SETTING_NAME, "1", "{\"applications\":[]}");

    Map<String, String> copyPreferences = copyService.getCopyPreferences(preferences, false, USERNAME);

    assertEquals("{\"applications\":[]}", copyPreferences.get(DATA_INIT_PREFERENCE));
    assertFalse(copyPreferences.containsKey(NAME_PREFERENCE));
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void getCopyPreferencesOfADraftKeepsThemUnchanged() {
    Map<String, String> preferences = preferences(SETTING_NAME, "1", null);

    assertEquals(preferences, copyService.getCopyPreferences(preferences, true, USERNAME));
    verifyNoInteractions(applicationCenterService, translationService);
  }

  @Test
  void getCopyPreferencesOfNoPreferencesIsEmpty() {
    assertTrue(copyService.getCopyPreferences(null, false, USERNAME).isEmpty());
  }

  @Test
  void getCopyPreferencesWithNothingToCarryCarriesNoPayload() {
    appCenterApplication(2L);

    Map<String, String> copyPreferences = copyService.getCopyPreferences(preferences(SETTING_NAME, "2", null), false, USERNAME);

    assertFalse(copyPreferences.containsKey(DATA_INIT_PREFERENCE));
    assertEquals("2", copyPreferences.get(MyApplicationsSettingsService.APPLICATION_IDS));
  }

  @Test
  void getCopyPreferencesOfAWindowNeverRenderedCarriesNoPayload() {
    Map<String, String> preferences = preferences(null, "1", null);

    assertFalse(copyService.getCopyPreferences(preferences, false, USERNAME).containsKey(DATA_INIT_PREFERENCE));
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void getCopyPreferencesCarriesTheLegacyHeaderTitleOfAWindowWithNoOther() throws Exception {
    givenLabels(HEADER_TYPE, "4242", HEADER_FIELD, Map.of(Locale.ENGLISH, "Legacy"));
    Map<String, String> preferences = preferences(SETTING_NAME, "", null);
    preferences.put(APPLICATION_ID_PREFERENCE, "4242");

    Map<String, String> copyPreferences = copyService.getCopyPreferences(preferences, false, USERNAME);

    assertFalse(copyPreferences.containsKey(APPLICATION_ID_PREFERENCE));
    assertEquals(Map.of("en", "Legacy"),
                 JsonUtils.fromJsonString(copyPreferences.get(DATA_INIT_PREFERENCE), ApplicationListCopy.class).getHeaderTitles());
  }

  @Test
  void getCopyPreferencesOmitsAnIllustrationLargerThanAnIllustrationMayBe() throws Exception {
    Application owned = contextApplication(1L, SETTING_NAME);
    owned.setImageFileId(9L);
    when(applicationCenterService.getApplicationImageInputStream(1L))
                                                             .thenReturn(new ByteArrayInputStream(new byte[ApplicationCenterService.MAX_ILLUSTRATION_SIZE
                                                                                                           + 1]));

    ApplicationListCopy listCopy = listCopy(copyService.getCopyPreferences(preferences(SETTING_NAME, "1", null), false, USERNAME));

    assertNull(listCopy.getApplications().get(0).getIllustration());
  }

  /**
   * The file storage's checked exception reaches the copy unchecked, the
   * storage throwing it sneakily: the application is copied without its
   * illustration, and the layout operation asking for the copy does not
   * fail. Fails the mutant catching only the declared exceptions
   */
  @Test
  void getCopyPreferencesCarriesAnApplicationWithoutTheIllustrationItCannotRead() throws Exception {
    Application owned = contextApplication(1L, SETTING_NAME);
    owned.setImageFileId(9L);
    when(applicationCenterService.getApplicationImageInputStream(1L)).thenAnswer(invocation -> {
      throw new FileStorageException("Binary of file 9 not found");
    });

    ApplicationListCopy listCopy = listCopy(copyService.getCopyPreferences(preferences(SETTING_NAME, "1", null), false, USERNAME));

    assertEquals(1L, listCopy.getApplications().get(0).getId());
    assertNull(listCopy.getApplications().get(0).getIllustration());
  }

  @Test
  void consumeCopyCreatesTheCarriedApplicationsInTheCopyAndRewritesItsList() throws Exception {
    ApplicationCopy copiedApplication = copy(1L);
    copiedApplication.setTitles(Map.of("fr", "Wiki fr"));
    copiedApplication.setDescriptions(Map.of("en", "<b>Wiki</b>"));
    copiedApplication.setIllustration(Base64.getEncoder().encodeToString(PNG));
    appCenterApplication(2L);
    contextApplication(3L, OTHER_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));

    String applicationIds = copyService.consumeCopy(COPY_NAME,
                                                    payload(Map.of("en", "Shortcuts"), copiedApplication),
                                                    "1,2,3,4");

    assertEquals("41,2", applicationIds);
    ArgumentCaptor<ApplicationForm> form = ArgumentCaptor.forClass(ApplicationForm.class);
    ArgumentCaptor<byte[]> illustration = ArgumentCaptor.forClass(byte[].class);
    verify(applicationCenterService).createCopiedContextApplication(form.capture(), illustration.capture(), eq(COPY_NAME), eq(false));
    assertEquals("Wiki 1", form.getValue().getTitle());
    assertEquals(ApplicationType.LINK, form.getValue().getType());
    assertEquals("https://wiki.example.org/1", form.getValue().getUrl());
    assertTrue(form.getValue().isSameTab());
    assertArrayEquals(PNG, illustration.getValue());
    verify(translationService).saveTranslationLabels(APP_TYPE, "41", "title", Map.of(Locale.FRENCH, "Wiki fr"), false);
    verify(translationService).saveTranslationLabels(APP_TYPE, "41", "description", Map.of(Locale.ENGLISH, "<b>Wiki</b>"), true);
    verify(translationService).saveTranslationLabels(HEADER_TYPE, COPY_NAME, HEADER_FIELD, Map.of(Locale.ENGLISH, "Shortcuts"), false);
  }

  /**
   * Only what the copy's list names is created: fails the mutant creating
   * every carried application, which an edited payload could inflate with
   * applications listed nowhere
   */
  @Test
  void consumeCopyCreatesOnlyTheCarriedApplicationsTheListNames() throws Exception {
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));

    assertEquals("41", copyService.consumeCopy(COPY_NAME, payload(null, copy(1L), copy(9L)), "1"));
    verify(applicationCenterService, times(1)).createCopiedContextApplication(any(), any(), any(), anyBoolean());
  }

  @Test
  void consumeCopyDropsACarriedApplicationWhoseFieldsAreRefused() throws Exception {
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean()))
                                                                                         .thenThrow(new IllegalArgumentException("appCenter.contextual.invalidUrl"))
                                                                                         .thenReturn(created(42L));

    assertEquals("42", copyService.consumeCopy(COPY_NAME, payload(null, copy(1L), copy(2L)), "1,2"));
  }

  @Test
  void consumeCopyCreatesAnApplicationWithoutAnIllustrationThatIsNotBase64OrTooLong() throws Exception {
    ApplicationCopy notBase64 = copy(1L);
    notBase64.setIllustration("not base64 !");
    ApplicationCopy tooLong = copy(2L);
    tooLong.setIllustration("A".repeat(4 * ((ApplicationCenterService.MAX_ILLUSTRATION_SIZE + 2) / 3) + 4));
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L),
                                                                                                         created(42L));

    assertEquals("41,42", copyService.consumeCopy(COPY_NAME, payload(null, notBase64, tooLong), "1,2"));
    verify(applicationCenterService, times(2)).createCopiedContextApplication(any(), isNull(), eq(COPY_NAME), anyBoolean());
  }

  @Test
  void consumeCopyOfAnUnreadablePayloadKeepsTheApplicationCenterApplicationsOnly() throws Exception {
    appCenterApplication(2L);
    contextApplication(3L, OTHER_NAME);
    contextApplication(5L, COPY_NAME);

    assertEquals("2,5", copyService.consumeCopy(COPY_NAME, "not json", "1,2,3,5"));
    verify(applicationCenterService, never()).createCopiedContextApplication(any(), any(), any(), anyBoolean());
    verify(translationService, never()).saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  void consumeCopyKeepsTheHeaderTitleTheCopyHasAlready() throws Exception {
    givenLabels(HEADER_TYPE, COPY_NAME, HEADER_FIELD, Map.of(Locale.ENGLISH, "Mine"));

    copyService.consumeCopy(COPY_NAME, payload(Map.of("en", "Shortcuts")), "");

    verify(translationService, never()).saveTranslationLabels(eq(HEADER_TYPE), anyString(), anyString(), any(), anyBoolean());
  }

  @Test
  void consumeCopyDropsALabelUnderATagNamingNoLanguage() throws Exception {
    copyService.consumeCopy(COPY_NAME, payload(Map.of("", "Nobody's", "en", "Shortcuts")), "");

    verify(translationService).saveTranslationLabels(HEADER_TYPE, COPY_NAME, HEADER_FIELD, Map.of(Locale.ENGLISH, "Shortcuts"), false);
  }

  /**
   * Fails the mutant writing the generated name whatever the window holds
   */
  @Test
  void getOrStoreSettingNameKeepsTheNameTheWindowHolds() {
    committed.put(NAME_PREFERENCE, COPY_NAME);

    assertEquals(COPY_NAME, copyService.getOrStoreSettingName(STORAGE_ID, () -> OTHER_NAME));
    verify(windowStorage).getOrSetPreference(STORAGE_ID, NAME_PREFERENCE, OTHER_NAME);
    verify(windowStorage, never()).savePreferences(any(), any());
  }

  @Test
  void saveWindowPreferenceWritesOverTheCommittedPreferences() {
    copyService.saveWindowPreference(STORAGE_ID, APPLICATION_ID_PREFERENCE, "");

    verify(windowStorage).savePreferences(STORAGE_ID, Map.of(APPLICATION_ID_PREFERENCE, ""));
  }

  @Test
  void consumeWindowCopyCreatesTheCarriedApplicationsAndStoresTheList() throws Exception {
    givenACommittedCopy(COPY_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));

    Map<String, String> window = copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME);

    assertEquals("41", window.get(MyApplicationsSettingsService.APPLICATION_IDS));
    assertEquals("", window.get(DATA_INIT_PREFERENCE));
    assertEquals("41", committed.get(MyApplicationsSettingsService.APPLICATION_IDS));
    assertEquals("", committed.get(DATA_INIT_PREFERENCE));
    assertEquals(COPY_NAME, committed.get(NAME_PREFERENCE));
  }

  /**
   * Fails the mutant consuming under a name the window no longer holds
   */
  @Test
  void consumeWindowCopyOfAnotherNameCreatesNothing() throws Exception {
    givenACommittedCopy(OTHER_NAME);

    assertEquals(OTHER_NAME, copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME).get(NAME_PREFERENCE));
    verify(applicationCenterService, never()).createCopiedContextApplication(any(), any(), any(), anyBoolean());
    verify(window, never()).savePreferences(any());
  }

  /**
   * The render's view carried a payload another render consumed: fails the
   * mutant consuming it again, and the mutant leaving the stale cache entry
   * the view came from, which every later render would read
   */
  @Test
  void consumeWindowCopyOfAConsumedPayloadCreatesNothingAndEmptiesTheStaleCacheEntry() throws Exception {
    givenACommittedCopy(COPY_NAME);
    committed.put(DATA_INIT_PREFERENCE, "");

    assertEquals("1", copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME).get(MyApplicationsSettingsService.APPLICATION_IDS));
    verify(applicationCenterService, never()).createCopiedContextApplication(any(), any(), any(), anyBoolean());
    verify(windowStorage).savePreferences(STORAGE_ID, Map.of(DATA_INIT_PREFERENCE, ""));
  }

  /**
   * The applications are created before the window is read again under its
   * lock: fails the mutant writing the list of a window another node or a
   * settings write changed meanwhile, and the mutant keeping the applications
   * it then does not list
   */
  @Test
  void consumeWindowCopyOfAWindowChangedMeanwhileDeletesWhatItCreated() throws Exception {
    givenACommittedCopy(COPY_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenAnswer(invocation -> {
      committed.put(DATA_INIT_PREFERENCE, "");
      committed.put(MyApplicationsSettingsService.APPLICATION_IDS, "2");
      return created(41L);
    });

    Map<String, String> window = copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME);

    assertEquals("2", window.get(MyApplicationsSettingsService.APPLICATION_IDS));
    assertEquals("2", committed.get(MyApplicationsSettingsService.APPLICATION_IDS));
    verify(applicationCenterService).deleteApplication(41L);
  }

  /**
   * The applications exist when the window is read again: fails the mutant
   * keeping them when that read fails, which a later render would duplicate
   */
  @Test
  void consumeWindowCopyDeletesWhatItCreatedWhenTheWindowCannotBeReadAgain() throws Exception {
    givenACommittedCopy(COPY_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));
    when(window.getPreferences()).thenThrow(new IllegalStateException("database unreachable"));

    assertThrows(IllegalStateException.class, () -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
    verify(applicationCenterService).deleteApplication(41L);
  }

  /**
   * A render that stopped waiting leaves the consumption running: fails the
   * mutant releasing the claim with the render, after which another render
   * of this node would consume the copy again, and the mutant never
   * releasing it once the consumption ended
   */
  @Test
  void consumeWindowCopyThatARenderStoppedWaitingForKeepsItsClaimUntilItEnds() throws Exception {
    givenACommittedCopy(COPY_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));
    AtomicReference<Runnable> runningTaskEnd = new AtomicReference<>();
    when(windowStorage.callOnWindow(eq(STORAGE_ID), any(), any())).thenAnswer(invocation -> {
      runningTaskEnd.set(invocation.getArgument(2));
      throw new WindowPreferencesTimeoutException("not in time", null);
    });

    assertThrows(WindowPreferencesTimeoutException.class, () -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
    assertNull(copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
    verify(windowStorage, times(1)).callOnWindow(any(), any(), any());

    runningTaskEnd.get().run();
    assertThrows(WindowPreferencesTimeoutException.class, () -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
    verify(windowStorage, times(2)).callOnWindow(any(), any(), any());
  }

  /**
   * Fails the mutants letting a read that fails between two creations, or
   * the header title's copy, stop the consumption, which would leave the
   * applications created before it listed nowhere
   */
  @Test
  void consumeCopyGoesOnWhenAListedApplicationOrTheHeaderTitleCannotBeRead() throws Exception {
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));
    when(applicationCenterService.getApplication(7L)).thenThrow(new IllegalStateException("database unreachable"));
    doThrow(new IllegalStateException("database unreachable")).when(translationService)
                                                               .getTranslationField(HEADER_TYPE, COPY_NAME, HEADER_FIELD);

    assertEquals("41,7", copyService.consumeCopy(COPY_NAME, payload(Map.of("en", "Shortcuts"), copy(1L)), "1,7"));
  }

  /**
   * Fails the mutant letting one application's failure stop the
   * consumption, which would leave the ones created before it listed nowhere
   */
  @Test
  void consumeCopyGoesOnWhenACopiedApplicationFails() throws Exception {
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean()))
                                                                                         .thenReturn(created(41L))
                                                                                         .thenThrow(new IllegalStateException("database unreachable"))
                                                                                         .thenReturn(created(43L));
    ApplicationCopy labelled = copy(1L);
    labelled.setTitles(Map.of("fr", "Wiki fr"));
    doThrow(new IllegalStateException("database unreachable")).when(translationService)
                                                               .saveTranslationLabels(anyString(), anyString(), anyString(), any(), anyBoolean());

    assertEquals("41,43", copyService.consumeCopy(COPY_NAME, payload(null, labelled, copy(2L), copy(3L)), "1,2,3"));
  }

  /**
   * A render meeting another render's consumption of the same window, here
   * from within it: fails the mutant consuming again, whose list would name
   * a second set
   */
  @Test
  void consumeWindowCopyMeetingAnotherConsumptionOfTheWindowConsumesNothing() throws Exception {
    givenACommittedCopy(COPY_NAME);
    AtomicReference<Map<String, String>> concurrentRender = new AtomicReference<>(Map.of());
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenAnswer(invocation -> {
      concurrentRender.set(copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
      return created(41L);
    });

    copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME);

    assertNull(concurrentRender.get());
    verify(applicationCenterService, times(1)).createCopiedContextApplication(any(), any(), any(), anyBoolean());
  }

  @Test
  void consumeWindowCopyByTwoRendersAtOnceCreatesOneSet() throws Exception {
    givenACommittedCopy(COPY_NAME);
    CountDownLatch creating = new CountDownLatch(1);
    CountDownLatch otherRenderDone = new CountDownLatch(1);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenAnswer(invocation -> {
      creating.countDown();
      otherRenderDone.await(10, TimeUnit.SECONDS);
      return created(41L);
    });
    ExecutorService renders = Executors.newFixedThreadPool(2);
    try {
      Future<Map<String, String>> first = renders.submit(() -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
      assertTrue(creating.await(10, TimeUnit.SECONDS));
      Future<Map<String, String>> second = renders.submit(() -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));
      assertNull(second.get(10, TimeUnit.SECONDS));
      otherRenderDone.countDown();
      assertEquals("41", first.get(10, TimeUnit.SECONDS).get(MyApplicationsSettingsService.APPLICATION_IDS));
    } finally {
      renders.shutdownNow();
    }
    verify(applicationCenterService, times(1)).createCopiedContextApplication(any(), any(), any(), anyBoolean());
  }

  /**
   * Fails the mutant keeping the applications of a consumption whose list
   * could not be stored: the next render consumes again, and would leave a
   * set listed nowhere at each attempt
   */
  @Test
  void consumeWindowCopyDeletesWhatItCreatedWhenItsListCannotBeStored() throws Exception {
    givenACommittedCopy(COPY_NAME);
    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));
    doThrow(new IllegalStateException("database unreachable")).when(window).savePreferences(any());

    assertThrows(IllegalStateException.class, () -> copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME));

    verify(applicationCenterService).deleteApplication(41L);
    verify(applicationCenterService, never()).deleteApplication(1L);
    // The claim is released: a later render consumes again
    doAnswer(invocation -> {
      committed.putAll(invocation.getArgument(0));
      return null;
    }).when(window).savePreferences(any());
    copyService.consumeWindowCopy(STORAGE_ID, COPY_NAME);
    verify(applicationCenterService, times(2)).createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean());
  }

  /**
   * Two first renders of one copy, each with the view of the window it read
   * when it started: the second adopts the name the first committed, and
   * finds the payload consumed, so that the copy gets one set, owned by the
   * name it keeps
   */
  @Test
  void twoFirstRendersOfACopyCreateOneSetOwnedByTheNameItKeeps() throws Exception {
    committed.put(DATA_INIT_PREFERENCE, payload(null, copy(1L)));
    committed.put(MyApplicationsSettingsService.APPLICATION_IDS, "1");
    when(applicationCenterService.createCopiedContextApplication(any(), any(), any(), anyBoolean())).thenReturn(created(41L));

    String firstName = copyService.getOrStoreSettingName(STORAGE_ID, () -> COPY_NAME);
    String secondName = copyService.getOrStoreSettingName(STORAGE_ID, () -> OTHER_NAME);
    copyService.consumeWindowCopy(STORAGE_ID, firstName);
    copyService.consumeWindowCopy(STORAGE_ID, secondName);

    assertEquals(COPY_NAME, secondName);
    assertEquals(COPY_NAME, committed.get(NAME_PREFERENCE));
    assertEquals("41", committed.get(MyApplicationsSettingsService.APPLICATION_IDS));
    verify(applicationCenterService, times(1)).createCopiedContextApplication(any(), any(), any(), anyBoolean());
  }

  private void givenACommittedCopy(String name) {
    committed.put(NAME_PREFERENCE, name);
    committed.put(DATA_INIT_PREFERENCE, payload(null, copy(1L)));
    committed.put(MyApplicationsSettingsService.APPLICATION_IDS, "1");
  }

  /**
   * A copy keeps its source's categories and an audience an administrator
   * emptied: fails the mutants dropping either from the payload or from the
   * creation
   */
  @Test
  void copiesCarryTheSourceCategoriesAndAnEmptiedAudience() throws Exception {
    Application hidden = contextApplication(1L, SETTING_NAME);
    hidden.setPermissions(List.of());
    when(applicationCenterService.getApplicationCategoryIds(1L)).thenReturn(List.of(3L, 4L));

    ApplicationCopy copy = listCopy(copyService.getCopyPreferences(preferences(SETTING_NAME, "1", null), false, null)).getApplications()
                                                                                                                      .get(0);
    assertEquals(List.of(3L, 4L), copy.getCategoryIds());
    assertTrue(copy.isHidden());

    when(applicationCenterService.createCopiedContextApplication(any(), any(), eq(COPY_NAME), anyBoolean())).thenReturn(created(41L));
    copyService.consumeCopy(COPY_NAME, payload(null, copy), "1");
    ArgumentCaptor<ApplicationForm> form = ArgumentCaptor.forClass(ApplicationForm.class);
    verify(applicationCenterService).createCopiedContextApplication(form.capture(), any(), eq(COPY_NAME), eq(true));
    assertEquals(List.of(3L, 4L), form.getValue().getCategoryIds());
  }

  private Map<String, String> preferences(String name, String applicationIds, String payload) {
    Map<String, String> preferences = new LinkedHashMap<>();
    if (name != null) {
      preferences.put(NAME_PREFERENCE, name);
    }
    preferences.put(MyApplicationsSettingsService.LISTING_MODE, "SELECTED");
    preferences.put(MyApplicationsSettingsService.APPLICATION_IDS, applicationIds);
    if (payload != null) {
      preferences.put(DATA_INIT_PREFERENCE, payload);
    }
    return preferences;
  }

  private Application contextApplication(long id, String owner) {
    Application application = appCenterApplication(id);
    application.setContextSettingName(owner);
    application.setContextSpaceId(0L);
    return application;
  }

  private Application appCenterApplication(long id) {
    Application application = new Application();
    application.setId(id);
    application.setTitle("Wiki " + id);
    application.setType(ApplicationType.LINK);
    application.setUrl("https://wiki.example.org/" + id);
    application.setSameTab(true);
    application.setActive(true);
    lenient().when(applicationCenterService.getApplication(id)).thenReturn(application);
    return application;
  }

  private Application created(long id) {
    Application application = new Application();
    application.setId(id);
    application.setContextSettingName(COPY_NAME);
    return application;
  }

  private ApplicationCopy copy(long id) {
    ApplicationCopy copy = new ApplicationCopy();
    copy.setId(id);
    copy.setTitle("Wiki " + id);
    copy.setType(ApplicationType.LINK);
    copy.setUrl("https://wiki.example.org/" + id);
    copy.setSameTab(true);
    return copy;
  }

  private String payload(Map<String, String> headerTitles, ApplicationCopy... applications) {
    return JsonUtils.toJsonString(new ApplicationListCopy(headerTitles, List.of(applications)));
  }

  private void givenLabels(String objectType, String objectId, String fieldName, Map<Locale, String> labels) throws Exception {
    TranslationField translationField = new TranslationField();
    translationField.setLabels(new HashMap<>(labels));
    lenient().doReturn(translationField).when(translationService).getTranslationField(objectType, objectId, fieldName);
  }

  private ApplicationListCopy listCopy(Map<String, String> copyPreferences) {
    return JsonUtils.fromJsonString(copyPreferences.get(DATA_INIT_PREFERENCE), ApplicationListCopy.class);
  }

  private List<Long> copiedIds(Map<String, String> copyPreferences) {
    return listCopy(copyPreferences).getApplications().stream().map(ApplicationCopy::getId).toList();
  }

}
