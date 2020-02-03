import UserAuthorizedApplications from './UserAuthorizedApplications.vue';
import UserFavoriteApplications from './UserFavoriteApplications.vue';

const components = {
  'user-authorizedApplications': UserAuthorizedApplications,
  'user-favoriteApplications' : UserFavoriteApplications
};

for(const key in components) {
  Vue.component(key, components[key]);
}