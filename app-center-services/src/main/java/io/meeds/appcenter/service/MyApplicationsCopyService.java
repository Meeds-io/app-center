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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationCopy;
import io.meeds.appcenter.model.ApplicationForm;
import io.meeds.appcenter.model.ApplicationListCopy;
import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.model.exception.WindowPreferencesTimeoutException;
import io.meeds.appcenter.plugin.ApplicationTranslationPlugin;
import io.meeds.appcenter.storage.PortletWindow;
import io.meeds.appcenter.storage.PortletWindowStorage;
import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import io.meeds.social.util.JsonUtils;

/**
 * Copies of a Shortcuts portlet window (AppCenterMyApplicationsPortlet): a
 * section duplicated, a page saved as a template, a site or space created
 * from a template, a template exported. The copy step carries the
 * applications the window created and its header title by value, in the
 * copy's data.init preference, since it knows nothing of where the copy
 * lands; the first render of the copy that finds them creates its own
 * applications from them, in its own context. The window's preferences are
 * read and written as committed ({@link PortletWindowStorage}), and a
 * consumption is claimed on this node, so that two first renders of a copy
 * on one node name it once and consume it once.
 */
@Service
public class MyApplicationsCopyService {

  /** The window's CMS setting name, which a copy never carries. */
  public static final String  NAME_PREFERENCE           = "name";

  /** The legacy key of the header title, which a copy never carries. */
  public static final String  APPLICATION_ID_PREFERENCE = "applicationId";

  /** What a copy carries until its first render consumes it. */
  public static final String  DATA_INIT_PREFERENCE      = "data.init";

  private static final Log    LOG                       = ExoLogger.getLogger(MyApplicationsCopyService.class);

  private static final String TITLE_FIELD               = "title";

  private static final String DESCRIPTION_FIELD         = "description";

  /** Length of the base64 encoding of the largest illustration accepted. */
  private static final int    MAX_ILLUSTRATION_BASE64   = 4 * ((ApplicationCenterService.MAX_ILLUSTRATION_SIZE + 2) / 3);

  /**
   * The windows whose copy a consumption of this node is running for, from
   * the render that asks for it to the end of its task.
   */
  private final Set<String>   consumingWindows          = ConcurrentHashMap.newKeySet();

  @Autowired
  private ApplicationCenterService      applicationCenterService;

  @Autowired
  private MyApplicationsSettingsService myApplicationsSettingsService;

  @Autowired
  private TranslationService            translationService;

  @Autowired
  private PortletWindowStorage          windowStorage;

  /**
   * Computes the preferences of a copy of a Shortcuts portlet window: every
   * preference but its setting name, the legacy header key and data.init,
   * plus data.init carrying, by value, its header title and the applications
   * it created that it lists, active and, when a user copies the window, as
   * the window's settings drawer lists them to that user: those the user may
   * access, and for a user who may edit the window's settings, those the
   * window manages whatever their audience, unless an administrator emptied
   * it. A data.init the window did not consume yet is
   * carried as it is, whatever its setting name. A draft of the window, which
   * the editor publishes back over it, keeps its preferences unchanged.
   *
   * @param preferences the window's preferences, null when it has none
   * @param draft whether the preferences are those of a draft of the same
   *          window
   * @param username the user the copy is made for, null when no user does
   *          it, which carries every application
   * @return the copy's preferences, by name
   */
  public Map<String, String> getCopyPreferences(Map<String, String> preferences, boolean draft, String username) {
    Map<String, String> copyPreferences = new LinkedHashMap<>();
    if (preferences == null) {
      return copyPreferences;
    } else if (draft) {
      copyPreferences.putAll(preferences);
      return copyPreferences;
    }
    preferences.forEach((name, value) -> {
      if (!StringUtils.equalsAny(name, NAME_PREFERENCE, APPLICATION_ID_PREFERENCE, DATA_INIT_PREFERENCE)) {
        copyPreferences.put(name, value);
      }
    });
    String payload = preferences.get(DATA_INIT_PREFERENCE);
    String settingName = preferences.get(NAME_PREFERENCE);
    if (StringUtils.isNotBlank(payload)) {
      copyPreferences.put(DATA_INIT_PREFERENCE, getPassedPayload(payload, username));
    } else if (StringUtils.isNotBlank(settingName)) {
      ApplicationListCopy listCopy = getListCopy(settingName,
                                                 preferences.get(APPLICATION_ID_PREFERENCE),
                                                 preferences.get(MyApplicationsSettingsService.APPLICATION_IDS),
                                                 username);
      if (listCopy != null) {
        copyPreferences.put(DATA_INIT_PREFERENCE, JsonUtils.toJsonString(listCopy));
      }
    }
    return copyPreferences;
  }

  /**
   * Consumes what a copy of a Shortcuts portlet window carries, at its first
   * render: creates, owned by the copy, one application per application it
   * carries that its list names, with no right checked and every field
   * validated again, and gives the copy the header title it carries when it
   * has none. A carried application whose fields are refused is not created.
   *
   * @param settingName the copy's CMS setting name
   * @param payload the copy's data.init preference
   * @param applicationIds the copy's applicationIds preference, which still
   *          names the source window's applications
   * @return the copy's applicationIds: a copied application replaced by the
   *         one created, an Application Center application and one the copy
   *         owns kept, any other dropped
   */
  public String consumeCopy(String settingName, String payload, String applicationIds) {
    ApplicationListCopy listCopy = parseListCopy(settingName, payload);
    Map<Long, ApplicationCopy> copiedApplications = new HashMap<>();
    if (listCopy != null && listCopy.getApplications() != null) {
      listCopy.getApplications()
              .stream()
              .filter(Objects::nonNull)
              .limit(ApplicationCenterService.MAX_LISTED_APPLICATIONS)
              .forEach(application -> copiedApplications.putIfAbsent(application.getId(), application));
    }
    List<Long> ids = new ArrayList<>();
    for (Long id : MyApplicationsSettingsService.getApplicationIds(applicationIds)) {
      ApplicationCopy copiedApplication = copiedApplications.get(id);
      if (copiedApplication != null) {
        Long createdId = createCopiedApplication(settingName, copiedApplication);
        if (createdId != null) {
          ids.add(createdId);
        }
      } else if (isKeptByCopy(id, settingName)) {
        ids.add(id);
      }
    }
    if (listCopy != null) {
      copyHeaderTitle(settingName, listCopy.getHeaderTitles());
    }
    return StringUtils.join(ids, ",");
  }

  /**
   * Names a window rendered with no name, a new window or a copy: the name it
   * holds as committed, which another render stored meanwhile, or else a new
   * one, stored. Two first renders of a copy then initialize it under one
   * name.
   *
   * @param storageId the window's storage id
   * @param nameGenerator generates a new name for the window
   * @return the window's name
   */
  public String getOrStoreSettingName(String storageId, Supplier<String> nameGenerator) {
    // Generated before the window's lock is taken, on the render's thread
    return windowStorage.getOrSetPreference(storageId, NAME_PREFERENCE, nameGenerator.get());
  }

  /**
   * Writes a preference of a window over its committed preferences, never
   * over a render's earlier view of them.
   *
   * @param storageId the window's storage id
   * @param name the preference name
   * @param value the preference value
   */
  public void saveWindowPreference(String storageId, String name, String value) {
    Map<String, String> values = new HashMap<>();
    values.put(name, value);
    windowStorage.savePreferences(storageId, values);
  }

  /**
   * Consumes what a copy of a window carries, when it is rendered under the
   * name it holds as committed and still carries it: see
   * {@link #consumeCopy(String, String, String)}. The applications are
   * created by the render; then one short task on the window
   * ({@link PortletWindowStorage#callOnWindow}) reads it again and, when it
   * still holds that name and that payload, writes its list and blanks its
   * data.init. Otherwise, or when that task fails, the render deletes them;
   * when the render stops waiting, the task's outcome is unknown and they are
   * kept. A render
   * meeting a consumption of the same window by this node consumes nothing:
   * the claim lasts until that task ended, whether its render still waits or
   * not. A render whose view carries a payload already consumed read the
   * window from a stale cache entry, which is emptied.
   *
   * @param storageId the window's storage id
   * @param settingName the name the window is rendered under
   * @return the window's preferences as committed once consumed, null when
   *         a consumption of the window by this node is running
   * @throws WindowPreferencesTimeoutException when the render stops waiting,
   *           the write still running
   */
  public Map<String, String> consumeWindowCopy(String storageId, String settingName) {
    if (!consumingWindows.add(storageId)) {
      return null;
    }
    boolean claimHandedOver = false;
    try {
      Map<String, String> preferences = windowStorage.getPreferences(storageId);
      String payload = preferences.get(DATA_INIT_PREFERENCE);
      if (StringUtils.isBlank(payload)) {
        // The render's view came from a stale cache entry: the write empties it
        windowStorage.savePreferences(storageId, Map.of(DATA_INIT_PREFERENCE, ""));
        return preferences;
      } else if (!StringUtils.equals(preferences.get(NAME_PREFERENCE), settingName)) {
        return preferences;
      }
      String sourceIds = preferences.get(MyApplicationsSettingsService.APPLICATION_IDS);
      String applicationIds = consumeCopy(settingName, payload, sourceIds);
      claimHandedOver = true;
      Map<String, String> stored;
      try {
        stored = windowStorage.callOnWindow(storageId,
                                            window -> storeConsumption(window, settingName, payload, applicationIds),
                                            () -> consumingWindows.remove(storageId));
      } catch (WindowPreferencesTimeoutException e) {
        throw e;
      } catch (RuntimeException e) {
        deleteCreatedApplications(sourceIds, applicationIds);
        throw e;
      }
      if (stored == null) {
        deleteCreatedApplications(sourceIds, applicationIds);
        return windowStorage.getPreferences(storageId);
      }
      return stored;
    } finally {
      if (!claimHandedOver) {
        consumingWindows.remove(storageId);
      }
    }
  }

  /**
   * Stores a consumption in the window, read again under its lock: its list
   * and a blank data.init, when it still holds the name and the payload the
   * applications were created for.
   *
   * @return the window's preferences once stored, null when it changed
   */
  private Map<String, String> storeConsumption(PortletWindow window,
                                               String settingName,
                                               String payload,
                                               String applicationIds) {
    Map<String, String> preferences = new HashMap<>(window.getPreferences());
    if (!StringUtils.equals(preferences.get(NAME_PREFERENCE), settingName)
        || !StringUtils.equals(preferences.get(DATA_INIT_PREFERENCE), payload)) {
      return null;
    }
    Map<String, String> consumed = new HashMap<>();
    consumed.put(MyApplicationsSettingsService.APPLICATION_IDS, applicationIds);
    consumed.put(DATA_INIT_PREFERENCE, "");
    window.savePreferences(consumed);
    preferences.putAll(consumed);
    return preferences;
  }

  /**
   * @return a payload not consumed yet, as a copy made for the user carries
   *         it: without the applications an administrator hid, unless the
   *         user is an administrator or no user makes the copy
   */
  private String getPassedPayload(String payload, String username) {
    ApplicationListCopy listCopy = parseListCopy(null, payload);
    if (listCopy == null
        || listCopy.getApplications() == null
        || listCopy.getApplications().stream().noneMatch(application -> application != null && application.isHidden())
        || username == null
        || applicationCenterService.isAdministrator(username)) {
      return payload;
    }
    listCopy.setApplications(listCopy.getApplications()
                                     .stream()
                                     .filter(application -> application != null && !application.isHidden())
                                     .toList());
    return JsonUtils.toJsonString(listCopy);
  }

  private ApplicationListCopy getListCopy(String settingName,
                                          String legacyHeaderKey,
                                          String applicationIds,
                                          String username) {
    Map<String, String> headerTitles = getLabels(MyApplicationsSettingsService.SETTING_TYPE,
                                                 settingName,
                                                 MyApplicationsSettingsService.HEADER_TITLE_FIELD);
    if (headerTitles.isEmpty() && StringUtils.isNotBlank(legacyHeaderKey)) {
      headerTitles = getLabels(MyApplicationsSettingsService.SETTING_TYPE,
                               legacyHeaderKey,
                               MyApplicationsSettingsService.HEADER_TITLE_FIELD);
    }
    List<Application> listed = MyApplicationsSettingsService.getApplicationIds(applicationIds)
                                                            .stream()
                                                            .map(applicationCenterService::getApplication)
                                                            .filter(application -> isCopied(application, settingName))
                                                            .toList();
    List<Application> applications = listed.stream()
                                               .filter(isCarriedTo(listed, settingName, username))
                                               .toList();
    Map<Long, List<Long>> categoryIds = getCategoryIds(applications);
    List<ApplicationCopy> copies = applications.stream()
                                               .map(application -> toCopy(application, categoryIds.get(application.getId())))
                                               .toList();
    return headerTitles.isEmpty() && copies.isEmpty() ? null : new ApplicationListCopy(headerTitles, copies);
  }

  private boolean isCopied(Application application, String settingName) {
    return application != null
           && application.isContextual()
           && StringUtils.equals(application.getContextSettingName(), settingName)
           && application.isActive();
  }

  /**
   * @param listed the applications the window created that it lists
   * @return which of them a copy made for the user carries, as the window's
   *         settings drawer lists them to that user: those the user may
   *         access, and for a user who may edit the window's settings, those
   *         the window manages, whose audience an administrator did not
   *         empty; every one when no user makes the copy. The settings right
   *         is read only when an application is outside the user's audience.
   */
  private Predicate<Application> isCarriedTo(List<Application> listed, String settingName, String username) {
    if (username == null) {
      return application -> true;
    }
    boolean editor = listed.stream().anyMatch(application -> !applicationCenterService.canAccess(application, username))
                     && myApplicationsSettingsService.canEditSettings(settingName, username);
    return application -> applicationCenterService.isShown(application, settingName, username, editor);
  }

  /**
   * @param application the cache's shared instance, read only
   */
  private ApplicationCopy toCopy(Application application, List<Long> categoryIds) {
    String id = String.valueOf(application.getId());
    ApplicationCopy copy = new ApplicationCopy();
    copy.setId(application.getId());
    copy.setTitle(application.getTitle());
    copy.setDescription(application.getDescription());
    copy.setTitles(getLabels(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE, id, TITLE_FIELD));
    copy.setDescriptions(getLabels(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE, id, DESCRIPTION_FIELD));
    copy.setType(application.getType());
    copy.setUrl(application.getUrl());
    copy.setHelpPageURL(application.getHelpPageURL());
    copy.setIcon(application.getIcon());
    copy.setBadgeName(application.getBadgeName());
    copy.setSameTab(application.isSameTab());
    copy.setMobile(application.isMobile());
    copy.setAllowDetach(application.isAllowDetach());
    copy.setIllustration(getIllustration(application));
    copy.setCategoryIds(categoryIds);
    copy.setHidden(application.getPermissions() == null || application.getPermissions().isEmpty());
    return copy;
  }

  /**
   * @return the ids of the applications' categories, read in one query, by
   *         application id, none when they cannot be read
   */
  private Map<Long, List<Long>> getCategoryIds(List<Application> applications) {
    if (applications.isEmpty()) {
      return Map.of();
    }
    try {
      return applicationCenterService.getApplicationCategoryIds(applications.stream().map(Application::getId).toList());
    } catch (RuntimeException e) {
      LOG.debug("Categories of copied applications not copied", e);
      return Map.of();
    }
  }

  /**
   * @return the illustration's content, base64-encoded, null when the
   *         application has none, when it is larger than an illustration may
   *         be, which the copy could not store, or when it cannot be read: the
   *         file storage's own exception is thrown unchecked, and a copy is
   *         made without the illustration rather than failing the layout
   *         operation that asks for it
   */
  private String getIllustration(Application application) {
    if (application.getImageFileId() == null) {
      return null;
    }
    try (InputStream inputStream = applicationCenterService.getApplicationImageInputStream(application.getId())) {
      byte[] content = inputStream == null ? null : inputStream.readNBytes(ApplicationCenterService.MAX_ILLUSTRATION_SIZE + 1);
      return content == null || content.length > ApplicationCenterService.MAX_ILLUSTRATION_SIZE ? null
                                                                                                : Base64.getEncoder()
                                                                                                        .encodeToString(content);
    } catch (Exception e) {
      LOG.debug("Illustration of application {} not copied", application.getId(), e);
      return null;
    }
  }

  private Long createCopiedApplication(String settingName, ApplicationCopy copiedApplication) {
    ApplicationForm form = new ApplicationForm();
    form.setTitle(copiedApplication.getTitle());
    form.setDescription(copiedApplication.getDescription());
    form.setType(copiedApplication.getType());
    form.setUrl(copiedApplication.getUrl());
    form.setHelpPageURL(copiedApplication.getHelpPageURL());
    form.setIcon(copiedApplication.getIcon());
    form.setBadgeName(copiedApplication.getBadgeName());
    form.setSameTab(copiedApplication.isSameTab());
    form.setMobile(copiedApplication.isMobile());
    form.setAllowDetach(copiedApplication.isAllowDetach());
    List<Long> categoryIds = copiedApplication.getCategoryIds();
    form.setCategoryIds(categoryIds == null ? null
                                            : categoryIds.stream()
                                                         .limit(MyApplicationsSettingsService.MAX_LISTED_CATEGORIES)
                                                         .toList());
    Application createdApplication;
    try {
      createdApplication = applicationCenterService.createCopiedContextApplication(form,
                                                                                   decodeIllustration(copiedApplication.getIllustration()),
                                                                                   settingName,
                                                                                   copiedApplication.isHidden());
    } catch (ObjectNotFoundException | IllegalArgumentException e) {
      LOG.debug("Application {} not copied for {}", copiedApplication.getId(), settingName, e);
      return null;
    } catch (RuntimeException e) {
      LOG.warn("Application {} not copied for {}", copiedApplication.getId(), settingName, e);
      return null;
    }
    String id = String.valueOf(createdApplication.getId());
    saveLabels(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE, id, TITLE_FIELD, copiedApplication.getTitles(), false);
    saveLabels(ApplicationTranslationPlugin.APPLICATION_OBJECT_TYPE,
               id,
               DESCRIPTION_FIELD,
               copiedApplication.getDescriptions(),
               true);
    return createdApplication.getId();
  }

  /**
   * Deletes the applications a consumption created: those its list names
   * that the source list did not.
   */
  private void deleteCreatedApplications(String sourceIds, String applicationIds) {
    List<Long> source = MyApplicationsSettingsService.getApplicationIds(sourceIds);
    for (Long id : MyApplicationsSettingsService.getApplicationIds(applicationIds)) {
      if (!source.contains(id)) {
        try {
          applicationCenterService.deleteApplication(id);
        } catch (ApplicationNotFoundException e) {
          LOG.debug("Copied application {} already deleted", id, e);
        } catch (RuntimeException e) {
          LOG.warn("Unable to delete copied application {} of a consumption not stored", id, e);
        }
      }
    }
  }

  /**
   * @return whether the copy keeps an application its list names that it
   *         carries no copy of: an Application Center application, or one
   *         the copy owns; one that cannot be read is kept, as listed before
   */
  private boolean isKeptByCopy(Long id, String settingName) {
    Application application;
    try {
      application = applicationCenterService.getApplication(id);
    } catch (RuntimeException e) {
      LOG.warn("Application {} kept in the list of {}: it cannot be read", id, settingName, e);
      return true;
    }
    return application != null
           && (!application.isContextual() || StringUtils.equals(application.getContextSettingName(), settingName));
  }

  /**
   * @return the illustration's content, null when none is carried or when it
   *         is not valid base64 or longer than an illustration may be, whose
   *         decoding is not attempted
   */
  private byte[] decodeIllustration(String illustration) {
    if (StringUtils.isBlank(illustration) || illustration.length() > MAX_ILLUSTRATION_BASE64) {
      return null; // NOSONAR
    }
    try {
      return Base64.getDecoder().decode(illustration);
    } catch (IllegalArgumentException e) {
      LOG.debug("Copied illustration dropped: not base64", e);
      return null; // NOSONAR
    }
  }

  private void copyHeaderTitle(String settingName, Map<String, String> headerTitles) {
    try {
      copyHeaderTitleLabels(settingName, headerTitles);
    } catch (RuntimeException e) {
      LOG.warn("Header title not copied for {}", settingName, e);
    }
  }

  private void copyHeaderTitleLabels(String settingName, Map<String, String> headerTitles) {
    if (headerTitles == null
        || headerTitles.isEmpty()
        || !getLabels(MyApplicationsSettingsService.SETTING_TYPE,
                      settingName,
                      MyApplicationsSettingsService.HEADER_TITLE_FIELD).isEmpty()) {
      return;
    }
    saveLabels(MyApplicationsSettingsService.SETTING_TYPE,
               settingName,
               MyApplicationsSettingsService.HEADER_TITLE_FIELD,
               headerTitles,
               false);
  }

  private ApplicationListCopy parseListCopy(String settingName, String payload) {
    try {
      return JsonUtils.fromJsonString(payload, ApplicationListCopy.class);
    } catch (Exception e) { // NOSONAR the parser throws its checked exceptions unchecked
      LOG.debug("Unreadable copy carried by {} dropped", settingName, e);
      return null;
    }
  }

  /**
   * @return the labels of a field, by language tag, empty when it has none,
   *         read with no right checked
   */
  private Map<String, String> getLabels(String objectType, String objectId, String fieldName) {
    Map<String, String> labels = new LinkedHashMap<>();
    try {
      TranslationField translationField = translationService.getTranslationField(objectType, objectId, fieldName);
      if (translationField != null && translationField.getLabels() != null) {
        translationField.getLabels().forEach((locale, label) -> {
          if (locale != null && StringUtils.isNotBlank(label)) {
            labels.put(locale.toLanguageTag(), label);
          }
        });
      }
    } catch (ObjectNotFoundException e) {
      LOG.debug("No {} labels for {} {}", fieldName, objectType, objectId, e);
    }
    return labels;
  }

  /**
   * Saves carried labels with no right checked, a rich text one sanitized: a
   * label under a language tag naming no language is dropped.
   */
  private void saveLabels(String objectType, String objectId, String fieldName, Map<String, String> labels, boolean richText) {
    Map<Locale, String> localizedLabels = new HashMap<>();
    if (labels != null) {
      labels.forEach((languageTag, label) -> {
        Locale locale = languageTag == null ? null : Locale.forLanguageTag(languageTag);
        if (locale != null && StringUtils.isNotBlank(locale.getLanguage()) && StringUtils.isNotBlank(label)) {
          localizedLabels.put(locale, label);
        }
      });
    }
    if (localizedLabels.isEmpty()) {
      return;
    }
    try {
      translationService.saveTranslationLabels(objectType, objectId, fieldName, localizedLabels, richText);
    } catch (ObjectNotFoundException e) {
      LOG.debug("{} labels of {} {} not copied", fieldName, objectType, objectId, e);
    } catch (RuntimeException e) {
      LOG.warn("{} labels of {} {} not copied", fieldName, objectType, objectId, e);
    }
  }

}
