import MyApplicationsApp from './components/MyApplications.vue'

const lang = eXo.env.portal.language;
const url = `/app-center/vueLocales/locale_${lang}.json`;

export function init() {
	exoi18n.loadLanguageAsync(lang, url).then(i18n => {
		new Vue({
        	render: h => h(MyApplicationsApp),
        	i18n
		}).$mount('#myApplications');
	});
}