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
package io.meeds.appcenter.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.exoplatform.commons.exception.ObjectNotFoundException;

import io.meeds.appcenter.service.ApplicationBadgeService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

/**
 * A badge count is personal data, so what this pins is the access surface: who
 * may read it, and that the service's exceptions keep mapping to the statuses
 * the org's REST contract prescribes rather than leaking as 500s.
 */
@SpringBootTest(classes = { ApplicationBadgeRest.class, PortalAuthenticationManager.class })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class ApplicationBadgeRestTest {

  private static final String     BADGES_PATH    = "/badges";           // NOSONAR

  private static final String     PROVIDERS_PATH = "/badges/providers"; // NOSONAR

  private static final String     BADGE_NAME     = "emailUnread";

  private static final String     SIMPLE_USER    = "simple";

  private static final String     ADMIN_USER     = "admin";

  private static final String     TEST_PASSWORD  = "testPassword";

  @MockitoBean
  private ApplicationBadgeService badgeService;

  @Autowired
  private SecurityFilterChain     filterChain;

  @Autowired
  private WebApplicationContext   context;

  private MockMvc                 mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  void getBadgeAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(BADGES_PATH + "/" + BADGE_NAME));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getBadgeReturnsTheCount() throws Exception {
    when(badgeService.getBadge(eq(BADGE_NAME), eq(SIMPLE_USER))).thenReturn(4L);

    ResultActions response = mockMvc.perform(get(BADGES_PATH + "/" + BADGE_NAME).with(testSimpleUser()));

    response.andExpect(status().isOk()).andExpect(content().string("4"));
  }

  @Test
  void getUnknownBadgeIsNotFound() throws Exception {
    when(badgeService.getBadge(eq("unknown"), eq(SIMPLE_USER))).thenThrow(new ObjectNotFoundException("Badge doesn't exist"));

    ResultActions response = mockMvc.perform(get(BADGES_PATH + "/unknown").with(testSimpleUser()));

    response.andExpect(status().isNotFound());
  }

  @Test
  void getBadgeOfAnUnauthorizedApplicationIsForbidden() throws Exception {
    when(badgeService.getBadge(eq(BADGE_NAME), eq(SIMPLE_USER))).thenThrow(new IllegalAccessException("Not allowed"));

    ResultActions response = mockMvc.perform(get(BADGES_PATH + "/" + BADGE_NAME).with(testSimpleUser()));

    // Reading a count for an application the user may not access must not be
    // possible, whatever the badge name they guess
    response.andExpect(status().isForbidden());
  }

  @Test
  void getProvidersAsSimpleUserIsForbidden() throws Exception {
    ResultActions response = mockMvc.perform(get(PROVIDERS_PATH).with(testSimpleUser()));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getProvidersAsAdministrator() throws Exception {
    when(badgeService.getBadgeProviders()).thenReturn(Collections.emptyList());

    ResultActions response = mockMvc.perform(get(PROVIDERS_PATH).with(testAdminUser()));

    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testAdminUser() {
    return user(ADMIN_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("administrators"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD).authorities(new SimpleGrantedAuthority("users"));
  }

}
