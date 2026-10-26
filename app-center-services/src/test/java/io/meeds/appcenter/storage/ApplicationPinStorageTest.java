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
package io.meeds.appcenter.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;

@ExtendWith(MockitoExtension.class)
class ApplicationPinStorageTest {

  private static final String   USERNAME = "testuser";

  @Mock
  private SettingService        settingService;

  @InjectMocks
  private ApplicationPinStorage pinStorage;

  @Test
  void getPinnedApplicationIdsWhenUnset() {
    assertEquals(Collections.emptyList(), pinStorage.getPinnedApplicationIds(USERNAME));
  }

  @Test
  void getPinnedApplicationIdsWhenSet() {
    storePins("[3,1,2]");
    assertEquals(Arrays.asList(3l, 1l, 2l), pinStorage.getPinnedApplicationIds(USERNAME));

    ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> scope = ArgumentCaptor.forClass(Scope.class);
    verify(settingService).get(context.capture(), scope.capture(), eq(ApplicationPinStorage.PINS_KEY));
    assertEquals(Context.USER.getName(), context.getValue().getName());
    assertEquals(USERNAME, context.getValue().getId());
    assertEquals(Scope.APPLICATION.getName(), scope.getValue().getName());
    assertEquals("PinnedApplications", scope.getValue().getId());
  }

  @Test
  void getPinnedApplicationIdsDropsDuplicatesAndInvalidIds() {
    storePins("[3,0,-1,3,null,1]");
    assertEquals(Arrays.asList(3l, 1l), pinStorage.getPinnedApplicationIds(USERNAME));
  }

  @Test
  void getPinnedApplicationIdsWhenNotAnArrayOfIds() {
    storePins("junk");
    assertEquals(Collections.emptyList(), pinStorage.getPinnedApplicationIds(USERNAME));
    storePins("{\"pins\":[1]}");
    assertEquals(Collections.emptyList(), pinStorage.getPinnedApplicationIds(USERNAME));
    storePins("[\"1');alert(1);//\"]");
    assertEquals(Collections.emptyList(), pinStorage.getPinnedApplicationIds(USERNAME));
  }

  /**
   * Whatever is stored, the read returns application ids only: the topbar JSPs
   * print the result into their inline scripts.
   */
  @Test
  void getPinnedApplicationIdsReturnsIdsOnly() {
    storePins("[1]);alert(1);//");
    assertEquals(Collections.singletonList(1l), pinStorage.getPinnedApplicationIds(USERNAME));
  }

  @Test
  void getPinnedApplicationIdsIsBounded() {
    storePins(LongStream.rangeClosed(1, ApplicationPinStorage.MAX_PINNED_APPLICATIONS + 1l)
                        .boxed()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",", "[", "]")));
    assertEquals(LongStream.rangeClosed(1, ApplicationPinStorage.MAX_PINNED_APPLICATIONS).boxed().toList(),
                 pinStorage.getPinnedApplicationIds(USERNAME));
  }

  @Test
  void setPinnedApplicationIds() {
    pinStorage.setPinnedApplicationIds(USERNAME, Arrays.asList(3l, 1l));

    ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
    ArgumentCaptor<Scope> scope = ArgumentCaptor.forClass(Scope.class);
    ArgumentCaptor<SettingValue> value = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(context.capture(), scope.capture(), eq(ApplicationPinStorage.PINS_KEY), value.capture());
    assertEquals(USERNAME, context.getValue().getId());
    assertEquals("PinnedApplications", scope.getValue().getId());
    // the stored format: a JSON array of numbers
    assertEquals("[3,1]", value.getValue().getValue());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void storePins(String value) {
    when(settingService.get(any(Context.class), any(Scope.class), eq(ApplicationPinStorage.PINS_KEY)))
                                                                                                     .thenReturn((SettingValue) SettingValue.create(value));
  }

}
