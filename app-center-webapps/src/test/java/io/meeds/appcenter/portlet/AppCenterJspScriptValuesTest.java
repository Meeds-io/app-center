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
package io.meeds.appcenter.portlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AppCenterJspScriptValuesTest {

  private static final Path        JSP_FOLDER     = Path.of("src/main/webapp/WEB-INF/jsp/appCenter");

  // the values printed as they are: booleans, numbers, lists of numbers, and
  // the literals the scriptlets build through the script-string escapers
  private static final Set<String> TYPED_VALUES   = Set.of("isAdmin",
                                                           "pinnedApplicationIds",
                                                           "shortcutListString",
                                                           "appPlacementsJson",
                                                           "applicationIds",
                                                           "categoryIds",
                                                           "showHeader",
                                                           "showSeeMore",
                                                           "listInRow",
                                                           "showIcon",
                                                           "iconSize",
                                                           "showName",
                                                           "outlined",
                                                           "canEdit");

  private static final Pattern     SCRIPT         = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);

  private static final Pattern     EXPRESSION     = Pattern.compile("<%=(.*?)%>", Pattern.DOTALL);

  private static final Pattern     ESCAPED_STRING = Pattern.compile("((\\w+ == null \\? \"null\" : )?Utils\\.toScriptString|scriptString)\\([^()]*\\)");

  @ParameterizedTest
  @ValueSource(strings = { "appShortcuts.jsp", "myApplications.jsp", "appLauncher.jsp" })
  void everyTextValueOfAnInlineScriptGoesThroughTheScriptStringEscaper(String jspName) throws Exception {
    String jsp = read(jspName);
    Matcher script = SCRIPT.matcher(jsp);
    boolean printed = false;
    while (script.find()) {
      Matcher expression = EXPRESSION.matcher(script.group(1));
      while (expression.find()) {
        String value = expression.group(1).strip();
        printed = true;
        assertTrue(TYPED_VALUES.contains(value) || ESCAPED_STRING.matcher(value).matches(),
                   jspName + " prints without the script-string escaper: " + value);
      }
    }
    assertTrue(printed, jspName + " prints no value into a script");
  }

  @ParameterizedTest
  @ValueSource(strings = { "appShortcuts.jsp", "myApplications.jsp", "appLauncher.jsp" })
  void noInlineScriptValueIsEscapedWithoutTheLessThanSign(String jspName) throws Exception {
    Matcher script = SCRIPT.matcher(read(jspName));
    while (script.find()) {
      assertFalse(script.group(1).contains("escapeEcmaScript"), jspName);
      assertFalse(script.group(1).contains("String.format"), jspName);
      // the escaper writes the quotes itself
      assertFalse(script.group(1).contains("'<%=Utils."), jspName);
      assertFalse(script.group(1).contains("'<%=scriptString("), jspName);
    }
  }

  @Test
  void theLiteralsBuiltInScriptletsEscapeEveryLessThanSign() throws Exception {
    assertTrue(read("appShortcuts.jsp").contains("String shortcutListString = Utils.toScriptArray(shortcuts);"));
    // the Shortcuts init block keeps single-quoted literals, which its settings save reads back
    assertTrue(read("myApplications.jsp").contains("return value == null ? \"null\" : \"'\" + StringEscapeUtils.escapeEcmaScript(String.valueOf(value)).replace(\"<\", \"\\\\u003c\") + \"'\";"));
    assertTrue(read("appLauncher.jsp").contains("JsonUtils.toJsonString(placements).replace(\"<\", \"\\\\u003c\")"));
  }

  private static String read(String jspName) throws Exception {
    return Files.readString(JSP_FOLDER.resolve(jspName), StandardCharsets.UTF_8);
  }

}
