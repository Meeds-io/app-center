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

// The configuration each template comes with. An empty color is none: a transparent tile, with the skin's border color when outlined
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

const TILE_ALIGNMENTS = {
  START: 'align-start',
  CENTER: 'align-center',
  END: 'align-end',
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

// Where 'see more' is displayed: HEADER on the header line, as the legacy layout does and a Column with its header,
// LIST at the end of the apps, in Row and Cards and in a Column without its header line; null when the display hides it
export function getSeeMorePlacement(display, showHeader) {
  if (!display) {
    return 'HEADER';
  } else if (!display.showSeeMore) {
    return null;
  }
  return display.template === 'COLUMN' && showHeader ? 'HEADER' : 'LIST';
}

export function isSameDisplay(display, otherDisplay) {
  return DISPLAY_KEYS.every(key => display?.[key] === otherDisplay?.[key]);
}

// The classes of an app's tile: the legacy one's without display settings
export function getTileClass(displaySettings, hover, focused) {
  if (!displaySettings) {
    return {
      'border-color-black': focused,
      'transparent': !hover,
      'light-grey-background-color': hover,
    };
  }
  return {
    // Stretched to the height of its line, a Row or Cards tile places its content as Vertical alignment says
    [`d-flex ${TILE_ALIGNMENTS[displaySettings.vAlign] || 'align-center'}`]: displaySettings.template !== 'COLUMN',
    'rounded-pill px-4 py-2': displaySettings.template === 'ROW',
    'pa-2': displaySettings.template === 'CARDS',
    'px-2 py-1': displaySettings.template === 'COLUMN',
    'border-color-black': focused,
  };
}

export function getTileStyle(displaySettings) {
  if (!displaySettings) {
    return null;
  } else if (displaySettings.outlined) {
    // No fill, and never the transparent class: it makes the border transparent as well
    return {border: `thin solid ${displaySettings.color || 'var(--allPagesBorderColor)'}`, backgroundColor: 'transparent'};
  } else {
    // No border, yet the room of the focus border so that focusing it moves nothing. Without a color, no fill
    // either, set inline: in the settings drawer, a clickable card is white otherwise
    return {border: 'thin solid transparent', backgroundColor: displaySettings.color || 'transparent'};
  }
}

// The style of a tile lifted over the page: what lies under it must not show through, so it is painted on the
// portlet's ground, the tile's own color over it when the tile is filled
export function getLiftedTileStyle(displaySettings) {
  const filledColor = !displaySettings.outlined && displaySettings.color;
  return {
    ...getTileStyle(displaySettings),
    backgroundColor: 'var(--appBackgroundColor, #FFFFFF)',
    backgroundImage: filledColor ? `linear-gradient(${filledColor}, ${filledColor})` : 'none',
  };
}
