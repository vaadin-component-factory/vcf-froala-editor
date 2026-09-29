// Test only. An application's own plugin, registered the way the README shows. Froala builds it with new and keeps the
// instance on the editor under the plugin's name.
import FroalaEditor from 'froala-editor';

FroalaEditor.PLUGINS.ownPlugin = function (editor) {
  return { _init() {} };
};
