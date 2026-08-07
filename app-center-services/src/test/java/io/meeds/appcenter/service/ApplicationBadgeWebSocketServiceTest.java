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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.ws.frameworks.cometd.ContinuationService;

@SpringBootTest(classes = { ApplicationBadgeWebSocketService.class })
@ExtendWith(MockitoExtension.class)
public class ApplicationBadgeWebSocketServiceTest {

  private static final String              BADGE_NAME = "emailUnread";

  private static final String              USERNAME   = "testuser";

  private static final String              EVENT_NAME = ApplicationBadgeService.BADGE_UPDATED_EVENT;

  @MockitoBean
  private ContinuationService               continuationService;

  @Autowired
  private ApplicationBadgeWebSocketService badgeWebSocketService;

  @Test
  void sendsOnlyToAConnectedUser() {
    when(continuationService.isPresent(USERNAME)).thenReturn(true);

    badgeWebSocketService.sendMessage(EVENT_NAME, BADGE_NAME, USERNAME);

    // The frame names the badge and never carries its value
    verify(continuationService).sendMessage(eq(USERNAME),
                                           eq(ApplicationBadgeWebSocketService.COMETD_CHANNEL),
                                           contains(BADGE_NAME));
  }

  @Test
  void skipsADisconnectedUser() {
    when(continuationService.isPresent(USERNAME)).thenReturn(false);

    badgeWebSocketService.sendMessage(EVENT_NAME, BADGE_NAME, USERNAME);

    verify(continuationService, never()).sendMessage(anyString(), anyString(), any());
  }

  /**
   * CometD's {@code Seti} is only assigned when the cometd servlet starts, so a
   * badge event raised during another addon's boot — a mailbox synchronisation,
   * typically — reaches this service before the transport exists. The
   * notification is a best-effort invalidation hint, so it must be dropped
   * quietly rather than fail the operation that triggered it.
   */
  @Test
  void swallowsATransportNotReadyYet() {
    when(continuationService.isPresent(USERNAME)).thenThrow(new NullPointerException("seti is null"));

    assertDoesNotThrow(() -> badgeWebSocketService.sendMessage(EVENT_NAME, BADGE_NAME, USERNAME));
    verify(continuationService, never()).sendMessage(anyString(), anyString(), any());
  }

  @Test
  void swallowsAFailingSend() {
    when(continuationService.isPresent(USERNAME)).thenReturn(true);
    doThrow(new IllegalStateException("boom")).when(continuationService).sendMessage(anyString(), anyString(), any());

    assertDoesNotThrow(() -> badgeWebSocketService.sendMessage(EVENT_NAME, BADGE_NAME, USERNAME));
  }

  @Test
  void ignoresBlankArguments() {
    badgeWebSocketService.sendMessage(EVENT_NAME, null, USERNAME);
    badgeWebSocketService.sendMessage(EVENT_NAME, BADGE_NAME, null);

    verify(continuationService, never()).isPresent(anyString());
    verify(continuationService, never()).sendMessage(anyString(), anyString(), any());
  }

}
