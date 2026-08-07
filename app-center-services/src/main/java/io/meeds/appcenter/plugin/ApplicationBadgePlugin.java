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
package io.meeds.appcenter.plugin;

/**
 * Contributes an unread/pending counter — a "badge" — for one Application
 * Center catalog entry. An addon implements this to make its own count appear
 * on its application tile, in the app launcher, in the "My Applications"
 * portlet and on the pinned topbar button, without any frontend code.
 * <p>
 * Implementations are discovered as Spring beans, or registered explicitly
 * through {@code ApplicationBadgePluginRegistry#addPlugin} for addons still
 * wired through Kernel XML.
 * <p>
 * A plugin carries <strong>no business logic</strong>: it transfers the call to
 * its own domain's Service layer, and never reaches a DAO or a Storage bean.
 */
public interface ApplicationBadgePlugin {

  /**
   * @return the stable identifier of this badge, e.g.
   *         {@code agendaPendingInvitations}. It is what an administrator binds
   *         to a catalog entry, and what travels on the WebSocket frame — so it
   *         must never change once released.
   */
  String getName();

  /**
   * Counts the items this badge reports for one user. Called on the read path,
   * so it must be cheap — or the plugin must declare itself
   * {@link #isSelfCached()} and do its own caching.
   *
   * @param  username the user the count is computed for
   * @return          the number of items to display, {@code 0} to hide the
   *                  badge
   */
  long countBadge(String username);

  /**
   * Lets a plugin opt a user out entirely, so that no count is ever computed
   * for them. Typically used when the user has not connected the underlying
   * account.
   *
   * @param  username the user to test
   * @return          {@code true} when this badge applies to that user
   */
  default boolean isEnabled(String username) {
    return true;
  }

  /**
   * Whether this plugin owns its own caching, in which case App Center calls
   * {@link #countBadge(String)} directly instead of going through its own
   * cache.
   * <p>
   * Meant for a source whose lifecycle sits <strong>outside</strong> the
   * container — a third-party system App Center can neither invalidate nor
   * observe — and whose count must stay consistent with what the same addon
   * displays elsewhere. Such a plugin owns <strong>both its caching and its
   * single-flight</strong>: self-cached never means uncached.
   *
   * @return {@code true} to bypass the App Center badge cache
   */
  default boolean isSelfCached() {
    return false;
  }

  /**
   * @return the {@code url} of the catalog entry of type {@code DRAWER} this
   *         badge belongs to, so that the binding resolves with no
   *         administrator action, or {@code null}
   */
  default String getDrawerName() {
    return null;
  }

  /**
   * @return the {@code url} of the catalog entry of type {@code PORTLET} this
   *         badge belongs to, so that the binding resolves with no
   *         administrator action, or {@code null}
   */
  default String getPortletName() {
    return null;
  }

}
