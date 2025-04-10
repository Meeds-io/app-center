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
    window.require(['SHARED/eXoVueI18n', 'SHARED/ActivityStream'], exoi18n => initActivityDrawer(exoi18n, resolve));
  }),
});

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'spaceForm',
  icon: 'fa-layer-group',
  name: 'quickActions.spaceForm.name',
  description: 'quickActions.spaceForm.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/spaceForm'], drawer => {
      drawer.open(null, eXo.env.portal.isExternalFeatureEnabled);
      resolve();
    });
  }),
});

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'editLanguage',
  icon: 'fa-language',
  name: 'quickActions.editLanguage.name',
  description: 'quickActions.editLanguage.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'PORTLET/social/UserSettingLanguage'], exoi18n => initLanguageDrawer(exoi18n, resolve));
  }),
});

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'mutedSpaces',
  icon: 'fa-bell-slash',
  name: 'quickActions.mutedSpaces.name',
  description: 'quickActions.mutedSpaces.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'PORTLET/social/UserSettingNotifications'], exoi18n => initMutedSpacesDrawer(exoi18n, resolve));
  }),
});

extensionRegistry.registerExtension('QuickAction', 'Extension', {
  id: 'editAboutMe',
  icon: 'fa-user',
  name: 'quickActions.editAboutMe.name',
  description: 'quickActions.editAboutMe.description',
  click: () => new Promise(resolve => {
    window.require(['SHARED/eXoVueI18n', 'PORTLET/social/ProfileAboutMe'], exoi18n => initAboutMeDrawer(exoi18n, resolve));
  }),
});

async function initActivityDrawer(exoi18n, callback) {
  const appId = 'activity-stream-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initActivityDrawerApp(appId, exoi18n, eXo.env.portal.maxFileSize);
  }
  document.dispatchEvent(new CustomEvent('activity-composer-drawer-open'));
  callback();
}

async function initLanguageDrawer(exoi18n, callback) {
  const appId = 'edit-language-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initLanguageDrawerApp(appId, exoi18n);
  }
  document.dispatchEvent(new CustomEvent('quick-action-edit-language-drawer'));
  callback();
}

async function initMutedSpacesDrawer(exoi18n, callback) {
  const appId = 'spaces-list-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initMutedSpacesDrawerApp(appId, exoi18n);
  }
  document.dispatchEvent(new CustomEvent('quick-action-muted-spaces-drawer', {detail: callback}));
}

async function initAboutMeDrawer(exoi18n, callback) {
  const appId = 'about-me-quick-actions';
  if (!document.querySelector(`#${appId}`)) {
    const parent = document.createElement('div');
    parent.id = appId;
    document.querySelector('#vuetify-apps').appendChild(parent);
    await initAboutMeDrawerApp(appId, exoi18n, eXo.env.portal.maxFileSize);
  }
  document.dispatchEvent(new CustomEvent('quick-action-about-me-drawer', {detail: callback}));
}

function initAboutMeDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `/social/i18n/locale.portlet.social.ProfileAboutMe?lang=${lang}`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `
        <profile-about-me-drawer
          id="${appId}"
          ref="drawer"
          :value="aboutMe" />
      `,
      data: () => ({
        aboutMe: null,
      }),
      created() {
        document.addEventListener('quick-action-about-me-drawer', this.openDrawer);
      },
      mounted() {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-about-me-drawer', this.openDrawer);
      },
      methods: {
        async openDrawer(event) {
          try {
            await this.refresh();
            this.$refs.drawer.open();
          } finally {
            const callback = event?.detail;
            if (callback) {
              callback();
            }
          }
        },
        refresh() {
          return this.$userService.getUser(eXo.env.portal.profileOwner)
            .then(user => this.aboutMe = user?.aboutMe || '');
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'About Me Quick Action')));
}

function initMutedSpacesDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const urls = [
    `/social/i18n/locale.portlet.UserNotificationPortlet?lang=${lang}`,
    `/social/i18n/locale.portlet.social.UserSettings?lang=${lang}`
  ];
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, urls)
    .then(i18n => Vue.createApp({
      template: `
        <user-setting-notification-mute-spaces-drawer
          id="${appId}"
          ref="drawer"
          :settings="settings" />
      `,
      data: () => ({
        settings: null,
      }),
      created() {
        document.addEventListener('quick-action-muted-spaces-drawer', this.openDrawer);
      },
      mounted() {
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-muted-spaces-drawer', this.openDrawer);
      },
      methods: {
        async openDrawer(event) {
          try {
            await this.refresh();
            this.$refs.drawer.open();
          } finally {
            const callback = event?.detail;
            if (callback) {
              callback();
            }
          }
        },
        refresh() {
          return fetch(`${eXo.env.portal.context}/${eXo.env.portal.rest}/notifications/settings/${eXo.env.portal.userName}`, {
            method: 'GET',
            credentials: 'include',
          })
            .then(resp => resp && resp.ok && resp.json())
            .then(settings => this.settings = settings);
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Muted Spaces Quick Action')));
}

function initActivityDrawerApp(appId, exoi18n, maxFileSize) {
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

function initLanguageDrawerApp(appId, exoi18n) {
  const lang = eXo.env.portal.language;
  const url = `/social/i18n/locale.portlet.social.UserSettings?lang=${lang}`;
  return new Promise(resolve => exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => Vue.createApp({
      template: `
        <user-language-drawer
          id="${appId}"
          :value="lang"
          :languages="supportedLanguages"
          ref="drawer" />
      `,
      data: () => ({
        lang,
        translationConfiguration: null,
        collator: new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'}),
      }),
      computed: {
        isMobile() {
          return this.$vuetify?.breakpoint?.mobile;
        },
        supportedLanguages() {
          if (this.translationConfiguration?.supportedLanguages) {
            const supportedLanguages = Object.keys(this.translationConfiguration.supportedLanguages)
              .map(l => ({
                value: l,
                text: this.translationConfiguration.supportedLanguages[l],
              }));
            supportedLanguages.sort((a, b) => this.collator.compare(a.text.toLowerCase(), b.text.toLowerCase()));
            return supportedLanguages;
          } else {
            return [];
          }
        },
      },
      created() {
        document.addEventListener('quick-action-edit-language-drawer', this.openDrawer);
      },
      async mounted() {
        this.translationConfiguration = await this.$translationService.getTranslationConfiguration();
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
        resolve();
      },
      beforeDestroy() {
        document.removeEventListener('quick-action-edit-language-drawer', this.openDrawer);
      },
      methods: {
        openDrawer() {
          this.$refs.drawer.open(this.supportedLanguages);
        },
      },
      vuetify: Vue.prototype.vuetifyOptions,
      i18n,
    }, `#${appId}`, 'Edit Language Quick Action')));
}
