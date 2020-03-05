import "./components/initComponents.js";
import AdminSetupApp from "./components/AdminSetup.vue";

const lang = eXo.env.portal.language;
const url = `/app-center/vueLocales/locale_${lang}.json`;

export function init(preferences) {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    new Vue({
      data: {
        preferences: preferences
      },
      render: h => h(AdminSetupApp),
      i18n
    }).$mount("#adminSetup");
  });
}
