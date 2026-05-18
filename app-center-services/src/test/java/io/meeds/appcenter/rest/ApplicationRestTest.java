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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.exoplatform.commons.file.services.FileService;
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

import io.meeds.appcenter.constant.ApplicationType;
import io.meeds.appcenter.model.Application;
import io.meeds.appcenter.model.ApplicationList;
import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.spring.web.security.PortalAuthenticationManager;
import io.meeds.spring.web.security.WebSecurityConfiguration;

import jakarta.servlet.Filter;
import lombok.SneakyThrows;

@SpringBootTest(classes = { ApplicationRest.class, PortalAuthenticationManager.class })
@ContextConfiguration(classes = { WebSecurityConfiguration.class })
@AutoConfigureWebMvc
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class ApplicationRestTest {

  private static final String APPLICATIONS_PATH     = "/applications";    // NOSONAR

  private static final String ALL_APPLICATIONS_PATH = "/applications/all";// NOSONAR

  private static final String SIMPLE_USER           = "simple";

  private static final String ADMIN_USER            = "admin";

  private static final String TEST_PASSWORD         = "testPassword";

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

  @MockitoBean
  private FileService fileService;

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
  void getAllApplicationsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(ALL_APPLICATIONS_PATH));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getAllApplications() throws Exception {
    when(applicationCenterService.getApplications(0, 10, "")).thenReturn(applicationList());
    ResultActions response = mockMvc.perform(get(ALL_APPLICATIONS_PATH).with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void getApplicationsAnonymously() throws Exception {
    ResultActions response = mockMvc.perform(get(APPLICATIONS_PATH));
    response.andExpect(status().isForbidden());
  }

  @Test
  void getApplications() throws Exception {
    ResultActions response = mockMvc.perform(get(APPLICATIONS_PATH).with(testSimpleUser()));
    response.andExpect(status().isOk());
  }

  @Test
  void createApplication() throws Exception {
    ResultActions response = mockMvc.perform(post(APPLICATIONS_PATH).with(testAdminUser())
                                                                    .content(asJsonString(application()))
                                                                    .contentType(MediaType.APPLICATION_JSON)
                                                                    .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
  }

  @Test
  void updateApplication() throws Exception {
    ResultActions response = mockMvc.perform(put(APPLICATIONS_PATH).with(testAdminUser())
                                                                   .content(asJsonString(application()))
                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                   .accept(MediaType.APPLICATION_JSON));
    response.andExpect(status().isOk());
  }

  @Test
  void deleteApplication() throws Exception {
    ResultActions response = mockMvc.perform(delete(APPLICATIONS_PATH + "/1").with(testAdminUser()));
    response.andExpect(status().isOk());
  }

  private RequestPostProcessor testAdminUser() {
    return user(ADMIN_USER).password(TEST_PASSWORD)
                           .authorities(new SimpleGrantedAuthority("administrators"));
  }

  private RequestPostProcessor testSimpleUser() {
    return user(SIMPLE_USER).password(TEST_PASSWORD)
                            .authorities(new SimpleGrantedAuthority("users"));
  }

  private ApplicationList applicationList() {
    ApplicationList result = new ApplicationList();
    result.setApplications(Collections.singletonList(application()));
    return result;
  }

  private Application application() {
    return new Application(1L,
                           "title",
                           "url",
                           true,
                           "",
                           "description",
                           "g",
                           ApplicationType.LINK,
                           false,
                           true,
                           false,
                           true,
                           false,
                           true,
                           null,
                           null,
                           5l,
                           "icon",
                           null,
                           null,
                           false);
  }

  @SneakyThrows
  public static String asJsonString(final Object obj) {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

}
