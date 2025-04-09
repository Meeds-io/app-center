/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'activityComposer',
  icon: 'fa-pen-fancy',
  name: 'quickActions.activityComposer.name',
  description: 'quickActions.activityComposer.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'SHARED/ActivityStream'], exoi18n => init(exoi18n, resolve));
  }),
});

async function init(exoi18n, callback) {
  const appId = 'activity-stream-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initApp(appId, exoi18n, eXo.env.portal.maxFileSize);
  }
  document.dispatchEvent(new CustomEvent('activity-composer-drawer-open'));
  callback();
}

function initApp(appId, exoi18n, maxFileSize) {
  const lang = eXo.env.portal.language;
  const urls = [
    `/social/i18n/locale.portlet.Portlets?lang=${lang}`,
    `/social/i18n/locale.commons.Commons?lang=${lang}`,
    `/social/i18n/locale.social.Webui?lang=${lang}`,
  ];
  return new Promise(resolveInit => exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => Vue.createApp({
      data: {
        maxFileSize,
        activityTypes: {},
        activityActions: {},
        commentActions: {},
        extensionApp: 'activity',
        activityTypeExtension: 'type',
        activityActionExtension: 'action',
        commentActionExtension: 'comment-action',
      },
      computed: {
        isMobile() {
          return this.$vuetify?.breakpoint?.mobile;
        },
        drawerParams() {
          return {
            activityTypes: this.activityTypes,
            activityActions: this.activityActions,
            commentTypes: this.activityTypes,
            commentActions: this.commentActions,
          };
        },
      },
      created() {
        this.activityTypes = extensionRegistry.loadExtensions(this.extensionApp, this.activityTypeExtension);
        this.activityActions = extensionRegistry.loadExtensions(this.extensionApp, this.activityActionExtension);
        this.commentActions = extensionRegistry.loadExtensions(this.extensionApp, this.commentActionExtension);
      },
      mounted() {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        resolveInit();
      },
      template: `
        <extension-registry-components
          id="${appId}"
          :params="drawerParams"
          name="ActivityStream"
          type="activity-stream-drawers"
          parent-element="div"
          element="div"
          class="drawer-parent" />
      `,
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Activity Composer Quick Action'))
    .finally(() => Vue.prototype.$utils.includeExtensions('ActivityStreamExtension')));
}
