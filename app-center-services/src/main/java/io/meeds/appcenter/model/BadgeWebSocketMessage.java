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
package io.meeds.appcenter.model;

import java.util.Map;

/**
 * The frame pushed on the badge WebSocket channel. Its shape matches what
 * {@code $socialWebSocket.initCometd} expects: {@code wsEventName} names the
 * DOM event the browser dispatches, {@code message} carries its detail.
 * <p>
 * Deliberately carries the badge <strong>name only, never the count</strong>,
 * so that reading a value always goes back through the authenticated REST
 * endpoint where the ACL is applied.
 *
 * @param wsEventName the event name the frontend listens to
 * @param message     the event payload
 */
public record BadgeWebSocketMessage(String wsEventName, Map<String, Object> message) {
}
