import "./components/initComponents.js";
import UserSetupApp from "./components/UserSetup.vue";

const lang = eXo.env.portal.language;
const url = `/app-center/vueLocales/locale_${lang}.json`;

export function init(preferences) {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    new Vue({
      data: {
        preferences: preferences
      },
      render: h => h(UserSetupApp),
      i18n
    }).$mount("#userSetup");
  });
}
