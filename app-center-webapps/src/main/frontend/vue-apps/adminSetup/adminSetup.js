import './components/initComponents.js';
import AdminSetupApp from './components/adminSetup.vue'

const lang = eXo.env.portal.language;
const url = `/app-center/vueLocales/locale_${lang}.json`;

export function init(preferences) {
	exoi18n.loadLanguageAsync(lang, url).then(i18n => {
		new Vue({
        	render: h => h(AdminSetupApp),
        	i18n,
			data: {
		    	preferences: preferences
			}
		}).$mount('#adminSetup');
	});
}