import '../../skin/less/app-center.less';
import AppLauncherApp from "./components/AppLauncher.vue";

Vue.use(Vuetify);
const vuetify = new Vuetify({
  dark: true,
  iconfont: ""
});

// getting language of user
const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
const url = `/app-center/vueLocales/locale_${lang}.json`;

export function init() {
  //getting locale ressources
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    new Vue({
      render: h => h(AppLauncherApp),
      i18n,
      vuetify
    }).$mount("#appLauncher");
  });
}
