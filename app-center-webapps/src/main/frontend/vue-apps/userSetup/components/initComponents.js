import UserAuthorizedApplications from './userAuthorizedApplications.vue';
import UserFavoriteApplications from './userFavoriteApplications.vue';

const components = {
  'user-authorizedApplications': UserAuthorizedApplications,
  'user-favoriteApplications' : UserFavoriteApplications
};

for(const key in components) {
  Vue.component(key, components[key]);
}