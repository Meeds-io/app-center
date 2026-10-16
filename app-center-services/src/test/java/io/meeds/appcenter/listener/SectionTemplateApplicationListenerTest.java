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
package io.meeds.appcenter.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.service.SectionTemplateService;

import jakarta.annotation.PostConstruct;

@SpringBootTest(classes = { SectionTemplateApplicationListener.class })
class SectionTemplateApplicationListenerTest {

  /** Not reset: the registration happens once, when the context starts */
  @MockitoBean(reset = MockReset.NONE)
  private ListenerService                    listenerService;

  @MockitoBean
  private ApplicationCenterService           applicationCenterService;

  @Autowired
  private SectionTemplateApplicationListener listener;

  /**
   * Also fails the mutant removing {@link PostConstruct} from init, which a
   * test calling init by hand cannot detect
   */
  @Test
  void registersOnStartupOnTheSectionTemplateDeletion() throws NoSuchMethodException {
    verify(listenerService).addListener(SectionTemplateService.TEMPLATE_DELETED_EVENT, listener);
    assertTrue(SectionTemplateApplicationListener.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    assertFalse(SectionTemplateApplicationListener.class.isAnnotationPresent(Asynchronous.class));
  }

  @Test
  void onEventDeletesTheApplicationsOfTheDeletedTemplate() {
    SectionTemplate sectionTemplate = new SectionTemplate();
    sectionTemplate.setId(7L);

    listener.onEvent(new Event<>(SectionTemplateService.TEMPLATE_DELETED_EVENT, "admin", sectionTemplate));

    verify(applicationCenterService).deleteSectionTemplateApplications(7L);
  }

  @Test
  void onEventIgnoresAnEventWithoutTemplate() {
    listener.onEvent(new Event<>(SectionTemplateService.TEMPLATE_DELETED_EVENT, "admin", "7"));

    verify(applicationCenterService, never()).deleteSectionTemplateApplications(anyLong());
  }

}
