/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

export const COMETD_CHANNEL = '/eXo/Application/AppCenter/Badge';

export const BADGE_UPDATED_EVENT = 'appcenter.badge.updated';

/**
 * Shared reactive counters, keyed by badge name. The same badge is displayed by
 * up to four components at once (launcher card, expanded launcher, "My Apps"
 * tile, pinned topbar button) — they all read this single store instead of
 * holding their own copy.
 */
export const badges = Vue.observable({});

export const badgeListeners = Vue.observable({});

/**
 * Requests in flight, keyed by badge name. Four components mounting at the same
 * time must produce one HTTP call, not four: the server-side cache absorbs the
 * computation, not the round trips.
 */
const inFlight = {};

let initialized = false;

export function init() {
  if (initialized) {
    return;
  }
  initialized = true;
  Vue.prototype.$socialWebSocket.initCometd(COMETD_CHANNEL);
  document.addEventListener(BADGE_UPDATED_EVENT, handleBadgeUpdate);
  // Compensates for the sources that cannot push: coming back from a
  // third-party app refreshes what is displayed, with no server-side polling
  document.addEventListener('visibilitychange', refreshOnVisible);
}

export function addBadgeListener(badgeName, listener) {
  if (!badgeListeners[badgeName]) {
    badgeListeners[badgeName] = [listener];
  } else {
    badgeListeners[badgeName].push(listener);
  }
}

export function getBadge(badgeName) {
  return badges[badgeName] || 0;
}

/**
 * Loads a badge count once, and shares the pending promise with any other
 * component asking for the same badge meanwhile.
 *
 * @param {String} badgeName badge identifier to load
 * @returns {Promise<Number>} the loaded count
 */
export function loadBadge(badgeName) {
  if (!badgeName) {
    return Promise.resolve(0);
  }
  if (inFlight[badgeName]) {
    return inFlight[badgeName];
  }
  inFlight[badgeName] = fetch(`/app-center/rest/badges/${encodeURIComponent(badgeName)}`, {
    method: 'GET',
    credentials: 'include',
  })
    .then(resp => (resp?.ok ? resp.text() : null))
    .then(value => {
      const count = value === null ? 0 : (Number(value) || 0);
      if (count !== badges[badgeName] && badgeListeners[badgeName]?.length) {
        try {
          badgeListeners[badgeName].forEach(l => l(count));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.warn('Error triggering badge `', badgeName,'` update listeners: ', badgeListeners[badgeName], e);
        }
      }
      Vue.set(badges, badgeName, count);
      return count;
    })
    .catch(() => {
      // A badge that cannot be read simply isn't displayed; it must never
      // break the tile it sits on
      Vue.set(badges, badgeName, 0);
      return 0;
    })
    .finally(() => delete inFlight[badgeName]);
  return inFlight[badgeName];
}

/**
 * Lists the badge providers contributed by installed addons, with the urls they
 * declare, so the administration form can resolve an internal application's
 * binding by itself. Administrators only.
 *
 * @returns {Promise<Array>} the registered providers
 */
export function getBadgeProviders() {
  return fetch('/app-center/rest/badges/providers', {
    method: 'GET',
    credentials: 'include',
  })
    .then(resp => (resp?.ok ? resp.json() : []))
    .catch(() => []);
}

function handleBadgeUpdate(event) {
  const badgeName = event?.detail?.message?.badgeName;
  if (badgeName && badgeName in badges) {
    loadBadge(badgeName);
  }
}

function refreshOnVisible() {
  if (!document.hidden) {
    Object.keys(badges).forEach(loadBadge);
  }
}
