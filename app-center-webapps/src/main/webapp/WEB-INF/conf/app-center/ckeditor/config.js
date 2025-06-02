const origEditorConfigFn = CKEDITOR.editorConfig;
CKEDITOR.editorConfig = function (config) {
  origEditorConfigFn(config);
  CKEDITOR.on('instanceReady', function(e) {
    e.editor.on('key', function(event) {
      if (event?.data?.domEvent?.$?.ctrlKey && event?.data?.domEvent?.$?.shiftKey) {
        if (!e.editor.editable().isInline() && window.parent) {
          window.parent.dispatchEvent(new CustomEvent('key-down', {detail: {
            key: event.data.domEvent.$.key,
          }}));
        }
      }
    });
  }); 
};
