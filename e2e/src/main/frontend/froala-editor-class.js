// Test only. Puts Froala's constructor on the page, which no page global exposes and no editor instance leads back to.
// The tests read Froala's own tables off it: its default toolbar, its commands and its quick insert buttons.
import FroalaEditor from 'froala-editor';

window.FroalaEditorClass = FroalaEditor;
