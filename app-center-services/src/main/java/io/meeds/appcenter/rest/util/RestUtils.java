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
package io.meeds.appcenter.rest.util;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Builds the answers of the handlers that return per-user or role-restricted
 * content from a URL that does not identify the user. Such an answer must
 * never be stored by a shared cache: it would be served to another user, or
 * to a caller the role check refuses. The platform's security filter chain
 * installs no header writer, so the handler is the only place where the
 * directives can be set, on the success and on every refusal it renders.
 */
public class RestUtils {

  private static final CacheControl PRIVATE_NO_STORE = CacheControl.noStore().cachePrivate();

  private RestUtils() {
    // Utility class
  }

  public static <T> ResponseEntity<T> privateAnswer(T body) {
    return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE).body(body);
  }

  public static <T> ResponseEntity<T> privateRefusal(HttpStatus status) {
    return ResponseEntity.status(status).cacheControl(PRIVATE_NO_STORE).build();
  }

}
