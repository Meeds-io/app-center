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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.meeds.appcenter.constant.ApplicationType;

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
   * A badge may legitimately belong to several catalog entries — an application
   * and its timeline or dashboard variant report the same counter — so several
   * urls may be declared.
   *
   * @return the {@code url} of every catalog entry of type {@code DRAWER} this
   *         badge belongs to, so that the binding resolves with no
   *         administrator action, empty when none
   */
  default List<String> getDrawerNames() {
    return Collections.emptyList();
  }

  /**
   * The portlets whose catalog entries carry this badge, preferably as
   * {@code applicationName/portletName} content ids — unambiguous when two
   * addons ship a portlet of the same name. A bare {@code portlet-name} is also
   * accepted.
   * <p>
   * Note this is <strong>not</strong> what a {@code PORTLET} entry stores: it
   * stores the portlet <em>instance</em> id chosen in the administration
   * suggester, and instance ids are generated per deployment so a plugin cannot
   * know them. App Center maps one to the other.
   *
   * @return every portlet this badge belongs to, empty when none
   * @see    #getDrawerNames()
   */
  default List<String> getPortletNames() {
    return Collections.emptyList();
  }

  /**
   * @param  type the catalog entry type to resolve against
   * @return      the urls this badge binds to for that type, never null
   */
  default List<String> getDeclaredUrls(ApplicationType type) {
    if (type == ApplicationType.DRAWER) {
      return getDrawerNames() == null ? Collections.emptyList() : getDrawerNames();
    } else if (type == ApplicationType.PORTLET) {
      return getPortletNames() == null ? Collections.emptyList() : getPortletNames();
    }
    return Collections.emptyList();
  }

  /**
   * @return every url this badge binds to, whatever the entry type, never null
   */
  default List<String> getDeclaredUrls() {
    return Stream.concat(getDeclaredUrls(ApplicationType.DRAWER).stream(),
                         getDeclaredUrls(ApplicationType.PORTLET).stream())
                 .filter(StringUtils::isNotBlank)
                 .distinct()
                 .toList();
  }

}
