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
package io.meeds.appcenter.constant;

/**
 * Where a Shortcuts portlet window is placed, which decides the context of
 * its setting and the marker its setting name carries.
 */
public enum WindowKind {

  /** A window of a page. */
  PAGE,

  /**
   * A window of a site layout, rendered with every page its site shows, and,
   * in the meta portal's layout, under every displayed site.
   */
  SITE_LAYOUT,

  /**
   * A window of a shared layout, rendered on every page of the sites it
   * serves: a site's own sharedlayout-&lt;site&gt;.xml, else the default
   * sharedlayout.xml. A configured shared
   * layout gives its windows no storage id: the name of such a window must be
   * configured, and is kept, and its setting records the shared-layout
   * reference in place of a page.
   */
  SHARED_LAYOUT,

  /**
   * A window of a section-template draft page, which the section-template
   * editor saves again, with new windows, each time it opens the template:
   * edited by administrators only, in the platform context.
   */
  SECTION_TEMPLATE_DRAFT;

}
