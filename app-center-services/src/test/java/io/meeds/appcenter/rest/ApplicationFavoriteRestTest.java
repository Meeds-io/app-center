/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.meeds.appcenter.model.ApplicationOrder;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationFavoriteRest.class, PortalAuthenticationManager.class, })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class ApplicationFavoriteRestTest {

  private static final String FAVORITES_PATH = "/favorites";  // NOSONAR

  private static final String SIMPLE_USER    = "simple";

  private static final String TEST_PASSWORD  = "testPassword";

  static final ObjectMapper   OBJECT_MAPPER;

  static {
    // Workaround when Jackson is defined in shared library with different
    // version and without artifact jackson-datatype-jsr310
    OBJECT_MAPPER = JsonMapper.builder()
                              .configure(JsonReadFeature.ALLOW_MISSING_VALUES, true)
                              .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                              .build();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

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
  void getApplicationFavoritesAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(FAVORITES_PATH));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getApplicationFavorites() throws Exception {
    ResultActions response = mockMvc.perform(get(FAVORITES_PATH).with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void addFavoriteApplicationAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(post(FAVORITES_PATH + "/1"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void addFavoriteApplication() throws Exception {
    ResultActions response = mockMvc.perform(post(FAVORITES_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void updateApplicationsOrderAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(put(FAVORITES_PATH).content(asJsonString(applicationOrders()))
                                                                .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isForbidden());
  }

  @Test
  void updateApplicationsOrder() throws Exception {
    ResultActions response = mockMvc.perform(put(FAVORITES_PATH).with(testSimpleUser())
                                                                .content(asJsonString(applicationOrders()))
                                                                .contentType(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
  }

  @Test
  void deleteFavoriteApplicationAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(delete(FAVORITES_PATH + "/1"));
    response.andExpect(status().isForbidden());
  }

  @Test
  void deleteFavoriteApplication() throws Exception {
    ResultActions response = mockMvc.perform(delete(FAVORITES_PATH + "/1").with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

  private List<ApplicationOrder> applicationOrders() {
    return Arrays.asList(new ApplicationOrder(1l, 2l), new ApplicationOrder(2l, 3l));
  }

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
