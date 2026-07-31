/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

// Backslashes are not preserved by $utils.toLinkUrl, so they are swapped with a
// placeholder around the call and restored afterwards.
const ESC = '__BACKSLASH__';

/**
 * Computes the href of a LINK application: expands the portal placeholders
 * ('./' and '@user@') then turns the result into an anchor href.
 *
 * @param {Object} application the application to compute the URL of
 * @returns {String} the href to use, or null for a non LINK application
 */
export function computeApplicationUrl(application) {
  if (application?.type !== 'LINK' || !application?.url) {
    return null;
  }
  const escaped = application.url
    .replace(/\\\\/g, '\\')
    .replace(/\\/g, ESC);
  const expanded = escaped
    .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`)
    .replace('@user@', eXo.env.portal.userName);
  // toLinkUrl returns undefined when it doesn't recognize a link: personal apps
  // stored before URLs were normalized may hold anything, so fall back to the
  // raw value rather than breaking the rendering of the whole applications list.
  const url = Vue.prototype.$utils.toLinkUrl(expanded, {
    urls: true,
    email: true,
    phone: true,
  }) || sanitize(expanded);
  return url.replace(new RegExp(ESC, 'g'), '\\');
}

/**
 * Normalizes an URL entered by an end user for a personal application. The
 * scheme is optional ('meeds.io' gives 'https://meeds.io'); when it is omitted
 * the host must be qualified, so that a plain word is rejected instead of
 * silently becoming 'https://word'. Portal relative links are kept as is.
 * Mirrors the server side normalization done in ApplicationCenterService.
 *
 * @param {String} url the URL as entered by the user
 * @returns {String} the normalized URL, or null when it isn't an http(s) link
 */
export function normalizePersonalUrl(url) {
  const trimmed = url?.trim();
  if (!trimmed) {
    return null;
  }
  if (trimmed.startsWith('/') || trimmed.startsWith('./')) {
    return trimmed;
  }
  const schemeOmitted = !/^[a-z][a-z0-9+.-]*:/i.test(trimmed);
  const withScheme = schemeOmitted ? `https://${trimmed}` : trimmed;
  try {
    const parsed = new URL(withScheme);
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return null;
    }
    if (schemeOmitted && !parsed.hostname.includes('.')) {
      return null;
    }
    return withScheme;
  } catch {
    return null;
  }
}

function sanitize(url) {
  return url.replace(/^\s*javascript:/i, '');
}
