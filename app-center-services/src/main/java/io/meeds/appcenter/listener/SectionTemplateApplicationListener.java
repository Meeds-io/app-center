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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.appcenter.service.ApplicationCenterService;
import io.meeds.layout.model.SectionTemplate;
import io.meeds.layout.service.SectionTemplateService;

import jakarta.annotation.PostConstruct;

/**
 * Deletes the applications the Shortcuts portlet windows of a section
 * template's drafts created once the template is deleted: the
 * section-template editor's working state, listed nowhere else.
 */
@Component
public class SectionTemplateApplicationListener extends Listener<Object, Object> {

  @Autowired
  private ApplicationCenterService applicationCenterService;

  @Autowired
  private ListenerService          listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(SectionTemplateService.TEMPLATE_DELETED_EVENT, this);
  }

  @Override
  public void onEvent(Event<Object, Object> event) {
    if (event.getData() instanceof SectionTemplate sectionTemplate) {
      applicationCenterService.deleteSectionTemplateApplications(sectionTemplate.getId());
    }
  }

}
