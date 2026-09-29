// Test only. Hands Froala's own default toolbar to FroalaToolbarIT, which compares FroalaToolbar.froalaDefault()
// against it. Froala keeps it on its constructor, which no page global exposes.
import FroalaEditor from 'froala-editor';

window.froalaDefaultToolbar = FroalaEditor.TOOLBAR_BUTTONS;
