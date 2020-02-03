import ExoModal from './modal/ExoModal.vue';
import AdminSetupGeneralParams from './AdminSetupGeneralParams.vue';
import AdminSetupList from './AdminSetupList.vue';

const components = {
  'adminSetup-generalParams': AdminSetupGeneralParams,
  'adminSetup-list' : AdminSetupList,
  'exo-modal' : ExoModal
};

for(const key in components) {
  Vue.component(key, components[key]);
}