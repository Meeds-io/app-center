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
 * @returns {String} the href to use, or null for a non LINK application and for
 *          a value that wouldn't resolve to a browsable link
 */
export function computeApplicationUrl(application) {
  if (application?.type !== 'LINK' || !application?.url) {
    return null;
  }
  const escaped = application.url
    .replaceAll('\\\\', '\\')
    .replaceAll('\\', ESC);
  const expanded = escaped
    .replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/`)
    .replace('@user@', eXo.env.portal.userName);
  // toLinkUrl returns undefined when it doesn't recognize a link: applications
  // stored before URLs were normalized may hold anything, so fall back to the
  // raw value rather than breaking the rendering of the whole applications list.
  const url = Vue.prototype.$utils.toLinkUrl(expanded, {
    urls: true,
    email: true,
    phone: true,
  }) || expanded;
  return safeHref(url.replaceAll(ESC, '\\'));
}

/**
 * Normalizes an URL entered by an end user for a personal application. The
 * scheme is optional ('meeds.io' gives 'https://meeds.io'); when it is omitted
 * the host must be qualified, so that a plain word is rejected instead of
 * silently becoming 'https://word'. Portal relative links are kept as is, but
 * scheme relative ones ('//host') are rejected.
 *
 * This is NOT a security boundary: it only gives an immediate feedback in the
 * form. The rule that matters is normalizePersonalUrl() in
 * ApplicationCenterService, which every write goes through — keep both in sync.
 * Both parse with a different engine (WHATWG here, RFC 3986 on the server), so
 * exotic values may be judged differently; the server always has the last word.
 *
 * @param {String} url the URL as entered by the user
 * @returns {String} the normalized URL, or null when it isn't an http(s) link
 */
export function normalizePersonalUrl(url) {
  // browsers drop tabs and line breaks before parsing an URL, so they must not
  // be able to hide either a scheme ('java<TAB>script:') or a scheme relative
  // prefix ('/<TAB>/host')
  const trimmed = url?.replace(/[\t\n\r]/g, '')?.trim();
  if (!trimmed) {
    return null;
  }
  if (trimmed.startsWith('/') || trimmed.startsWith('./')) {
    // '//host' and '/\host' are resolved by the browser to an external origin
    return trimmed.startsWith('//') || trimmed.startsWith('/\\') ? null : trimmed;
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

/**
 * Last check before the value is used as an href. Allow listing the schemes the
 * browser will actually resolve is the only reliable way: denying 'javascript:'
 * by hand misses 'java<TAB>script:', which browsers execute all the same.
 * mailto and tel are allowed because toLinkUrl produces them (email and phone
 * options). Anything else returns null, so no href attribute is set at all.
 *
 * @param {String} url the computed href candidate
 * @returns {String} the same value when the browser would resolve it to a
 *          browsable link, null otherwise
 */
function safeHref(url) {
  try {
    const protocol = new URL(url, window.location.origin).protocol;
    return ['http:', 'https:', 'mailto:', 'tel:'].includes(protocol) ? url : null;
  } catch {
    return null;
  }
}
