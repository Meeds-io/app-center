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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.meeds.appcenter.model.exception.ApplicationNotFoundException;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;

@SpringBootTest(classes = { ApplicationPinRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class ApplicationPinRestTest {

  private static final String      PINS_PATH        = "/pins";             // NOSONAR

  private static final String      PIN_PATH         = PINS_PATH + "/1";

  private static final String      PRIVATE_NO_STORE = "no-store, private";

  private static final String      SIMPLE_USER      = "simple";

  private static final String      TEST_PASSWORD    = "testPassword";

  @MockitoBean
  private ApplicationCenterService applicationCenterService;

  @Autowired
  private SecurityFilterChain      filterChain;

  @Autowired
  private WebApplicationContext    context;

  private MockMvc                  mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
                             .addFilters(filterChain.getFilters().toArray(new Filter[0]))
                             .build();
  }

  @Test
  void getPinnedApplicationIdsAnonymously() throws Exception {
    mockMvc.perform(get(PINS_PATH)).andExpect(status().isForbidden());
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void getPinnedApplicationIds() throws Exception {
    when(applicationCenterService.getPinnedApplicationIds(SIMPLE_USER)).thenReturn(Arrays.asList(3l, 1l));
    // The URL is the same for every user: only the directives keep a shared
    // cache from serving one user's pins to another
    mockMvc.perform(get(PINS_PATH).with(testSimpleUser()))
           .andExpect(status().isOk())
           .andExpect(header().string("Cache-Control", PRIVATE_NO_STORE))
           .andExpect(content().json("[3,1]"));
  }

  @Test
  void pinApplicationAnonymously() throws Exception {
    mockMvc.perform(put(PIN_PATH)).andExpect(status().isForbidden());
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void pinApplication() throws Exception {
    mockMvc.perform(put(PIN_PATH).with(testSimpleUser())).andExpect(status().isOk());
    verify(applicationCenterService).pinApplication(1l, SIMPLE_USER);
  }

  @Test
  void pinApplicationNotFound() throws Exception {
    doThrow(ApplicationNotFoundException.class).when(applicationCenterService).pinApplication(anyLong(), anyString());
    mockMvc.perform(put(PIN_PATH).with(testSimpleUser())).andExpect(status().isNotFound());
  }

  @Test
  void pinApplicationNotAccessible() throws Exception {
    doThrow(IllegalAccessException.class).when(applicationCenterService).pinApplication(anyLong(), anyString());
    mockMvc.perform(put(PIN_PATH).with(testSimpleUser())).andExpect(status().isForbidden());
  }

  @Test
  void pinApplicationNotAllowed() throws Exception {
    doThrow(new IllegalArgumentException("appCenter.pin.tooManyPins")).when(applicationCenterService)
                                                                       .pinApplication(anyLong(), anyString());
    // the reason is the key of the label the client displays, sent as the
    // error body's message
    mockMvc.perform(put(PIN_PATH).with(testSimpleUser()))
           .andExpect(status().isBadRequest())
           .andExpect(status().reason("appCenter.pin.tooManyPins"));
  }

  @Test
  void pinApplicationWithInvalidId() throws Exception {
    mockMvc.perform(put(PINS_PATH + "/abc").with(testSimpleUser())).andExpect(status().isBadRequest());
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void unpinApplicationAnonymously() throws Exception {
    mockMvc.perform(delete(PIN_PATH)).andExpect(status().isForbidden());
    verifyNoInteractions(applicationCenterService);
  }

  @Test
  void unpinApplication() throws Exception {
    mockMvc.perform(delete(PIN_PATH).with(testSimpleUser())).andExpect(status().isOk());
    verify(applicationCenterService).unpinApplication(1l, SIMPLE_USER);
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

}
