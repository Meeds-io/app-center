/*
* This file is part of the Meeds project (https://meeds.io/).
*
* Copyright (C) 2026 Meeds Association contact@meeds.io
*
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
* Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

export const TEMPLATES = ['ROW', 'CARDS', 'COLUMN'];

// The server's MIN_ICON_SIZE and MAX_ICON_SIZE
export const MIN_ICON_SIZE = 12;
export const MAX_ICON_SIZE = 60;

// The settings a template reads but showHeader, which the legacy layout reads too, in the order they are compared
export const DISPLAY_KEYS = ['template', 'showSeeMore', 'listInRow', 'showIcon', 'iconSize', 'showName', 'color', 'outlined', 'hAlign', 'vAlign'];

// The configuration each template comes with. An empty color is the skin's light grey
const TEMPLATE_DEFAULTS = {
  ROW: {
    showHeader: false,
    showSeeMore: false,
    listInRow: true,
    showIcon: false,
    iconSize: 32,
    showName: true,
    color: '',
    outlined: true,
    hAlign: 'CENTER',
    vAlign: 'CENTER',
  },
  CARDS: {
    showHeader: false,
    showSeeMore: false,
    listInRow: true,
    showIcon: true,
    iconSize: 45,
    showName: true,
    color: '',
    outlined: false,
    hAlign: 'CENTER',
    vAlign: 'CENTER',
  },
  COLUMN: {
    showHeader: true,
    showSeeMore: true,
    // Not applicable: a column lists one app per line
    listInRow: true,
    showIcon: true,
    iconSize: 32,
    showName: true,
    color: '',
    outlined: false,
    hAlign: 'START',
    vAlign: 'CENTER',
  },
};

// The display settings to render, null for the legacy layout: no template, or one the server
// did not print. A setting the server printed null, unset or invalid, takes the template's default
export function getDisplaySettings(settings) {
  const template = TEMPLATES.includes(settings?.template) ? settings.template : null;
  if (!template) {
    return null;
  }
  return DISPLAY_KEYS.reduce((display, key) => {
    display[key] = settings[key] ?? TEMPLATE_DEFAULTS[template][key];
    return display;
  }, {template});
}

export function getTemplateDisplaySettings(template) {
  return getDisplaySettings({template});
}

export function getTemplateShowHeader(template) {
  return TEMPLATE_DEFAULTS[template].showHeader;
}

export function isSameDisplay(display, otherDisplay) {
  return DISPLAY_KEYS.every(key => display?.[key] === otherDisplay?.[key]);
}
