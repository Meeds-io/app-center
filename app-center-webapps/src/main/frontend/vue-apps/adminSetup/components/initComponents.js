import ExoModal from './modal/ExoModal.vue';
import AdminSetupGeneralParams from './adminSetupGeneralParams.vue';
import AdminSetupList from './adminSetupList.vue';

const components = {
  'adminSetup-generalParams': AdminSetupGeneralParams,
  'adminSetup-list' : AdminSetupList,
  'exo-modal' : ExoModal
};

for(const key in components) {
  Vue.component(key, components[key]);
}