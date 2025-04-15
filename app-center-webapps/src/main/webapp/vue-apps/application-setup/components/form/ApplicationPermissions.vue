<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->
<template>
  <identity-suggester
    v-model="groups"
    :labels="suggesterLabels"
    :search-options="{filterType: 'all'}"
    :include-users="false"
    name="applicationPermissions"
    sugester-class="mt-0"
    all-groups-for-admin
    include-spaces
    include-groups
    multiple />
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
  },
  data: () => ({
    groups: null,
  }),
  computed: {
    permissions() {
      if (this.groups?.length) {
        return this.groups?.filter?.(g => g)?.map?.(g => `*:${g.groupId}`) || [];
      } else {
        return [];
      }
    },
    suggesterLabels() {
      return {
        searchPlaceholder: this.$t('appCenter.adminSetupForm.permissionsPlaceHolder'),
        placeholder: this.$t('appCenter.adminSetupForm.permissionsPlaceHolder'),
        noDataLabel: this.$t('appCenter.adminSetupForm.permissionsNoResult'),
      };
    },
  },
  watch: {
    permissions() {
      if (JSON.stringify(this.permissions) !== JSON.stringify(this.value)) {
        this.$emit('input', this.permissions);
      }
    },
  },
  created() {
    this.groups = [];
    if (this.value?.length) {
      this.value.forEach(this.retrieveObject);
    }
  },
  methods: {
    async retrieveObject(groupId) {
      try {
        groupId = groupId.includes(':') ? groupId.split(':')[1] : groupId;
        if (groupId.indexOf('/spaces/') === 0) {
          const space = await this.$spaceService.getSpaceByGroupId(groupId);
          if (space) {
            this.groups.push({
              id: `space:${space.prettyName}`,
              remoteId: space.prettyName,
              spaceId: space.id,
              groupId: space.groupId,
              providerId: 'space',
              displayName: space.displayName,
              profile: {
                fullName: space.displayName,
                originalName: space.shortName,
                avatarUrl: space.avatarUrl ? space.avatarUrl : `/portal/rest/v1/social/spaces/${space.prettyName}/avatar`,
              },
            });
          }
        } else {
          const group = await this.$identityService.getIdentityByProviderIdAndRemoteId('group', groupId);
          if (group) {
            this.groups.push({
              id: `group:${group.remoteId}`,
              remoteId: group.remoteId,
              spaceId: groupId,
              groupId: groupId,
              providerId: 'group',
              displayName: group.profile?.fullname,
              profile: {
                fullName: group.profile?.fullname,
                originalName: group.profile?.fullname,
              },
            });
          }
        }
      } catch (e) {
        console.error('Error retrieving group details with id', groupId, e);
      }
    },
  },
};
</script>