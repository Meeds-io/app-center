<template>
  <div class="app-center-modal-mask">
    <div class="uiPopupWrapper">
      <div :class="modalClass" class="uiPopup app-center-modal-content">
        <div class="popupHeader">
          <span class="PopupTitle popupTitle">{{ title }}</span>
          <a
            v-if="displayClose"
            class="uiIconClose pull-right"
            @click.stop="closeModal"
          ></a>
        </div>
        <div class="app-center-modal-content PopupContent popupContent border-box-sizing">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    displayClose: {
      type: Boolean,
      default: true
    },
    title: {
      type: String,
      default: ''
    },
    modalClass: {
      type: String,
      default: ''
    }
  },
  methods: {
    closeModal() {
      // Emit the click event of close icon
      this.$emit('modal-closed');
    }
  }
};
</script>
