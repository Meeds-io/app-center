<template>
  <div class="applicationsAdmin">
    <div class="uiTabNormal uiTabInPage">
      <ul class="nav nav-tabs">
        <li :class="{ active: activeTab === 1 }" @click="activeTab = 1">
          <a href="#globalParams" data-toggle="tab">{{
            $t("appCenter.adminSetupForm.generalSettings")
          }}</a>
        </li>
        <li :class="{ active: activeTab === 2 }" @click="activeTab = 2">
          <a href="#list" data-toggle="tab">{{
            $t("appCenter.adminSetupForm.applications")
          }}</a>
        </li>
      </ul>
      <div class="tab-content">
        <div
          v-if="activeTab === 1"
          id="globalParams"
          class="tab-pane fade in active">
          <adminSetup-generalParams />
        </div>
        <div
          v-if="activeTab === 2"
          id="list"
          class="tab-pane fade in active">
          <adminSetup-list />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 1
    };
  },
  created() {
    this.pageSize = this.$parent.$data.preferences.pageSize;
  },
  mounted() {
    const windowLocationHash = window.location.hash;
    if (windowLocationHash === "#list") {
      this.activeTab = 2;
    } else {
      this.activeTab = 1;
    }
  }
};
</script>
