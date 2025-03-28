/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2025 Meeds Association contact@meeds.io
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

package io.meeds.appcenter.plugin;

import io.meeds.social.translation.model.TranslationField;
import io.meeds.social.translation.service.TranslationService;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class MyApplicationsHeaderTranslationTest {

  @Autowired
  private TranslationService translationService;

  @Test
  public void testTranslateHeaderTitle() throws ObjectNotFoundException {

    Map<Locale, String> labels = new HashMap<>();
    labels.put(Locale.US, "header en");
    labels.put(Locale.FRANCE, "header fr");
    translationService.saveTranslationLabels("myApplicationsPortlet", 1L, "headerTitle", labels);

    TranslationField translationField = translationService.getTranslationField("myApplicationsPortlet", 1L, "headerTitle");
    assertNotNull(translationField);
    assertNotNull(translationField.getLabels());
    assertEquals(2, translationField.getLabels().size());
  }
}
