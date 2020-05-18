import ExoAppCenterModal from './modal/ExoAppCenterModal.vue';
import AdminSetupGeneralParams from './AdminSetupGeneralParams.vue';
import AdminSetupList from './AdminSetupList.vue';

const components = {
  'adminSetup-generalParams': AdminSetupGeneralParams,
  'adminSetup-list': AdminSetupList,
  'exo-app-center-modal': ExoAppCenterModal
};

for (const key in components) {
  Vue.component(key, components[key]);
}
