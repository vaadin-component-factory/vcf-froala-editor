/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.componentfactory.froala.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Phase 2, check 2: where does an image, file or video go when no upload URL is configured? Decides whether the upload
 * plugins can be enabled by default.
 * <p>
 * Insert a file through one of the toolbar buttons and read the HTML box below. A {@code blob:} or {@code data:} URL
 * means the file never left the browser; an {@code https://i.froala.com/...} URL means it did. Open the browser's
 * network tab as well — the image manager ("Browse") is a separate case from the upload itself.
 * <p>
 * Case 3 is the counter-check: it points Froala at {@code FroalaUploadController} in this application, so the upload
 * that does not happen in case 1 can be watched happening.
 */
@Route("check-upload")
@Menu(title = "Check 2: Upload target", order = 11)
public class UploadCheckView extends VerticalLayout {

    private static final String INSERT_BUTTONS = """
            "toolbarButtons": ["insertImage", "insertFile", "insertVideo", "emoticons", "|", "html"]""";

    private static final Map<String, String> CASES = new LinkedHashMap<>();

    static {
        CASES.put("1 — nothing configured (the add-on's state today)", "{\n  " + INSERT_BUTTONS + "\n}");

        CASES.put("2 — image manager reachable from the insert popup", "{\n  " + INSERT_BUTTONS + """
                ,
                  "imageInsertButtons": ["imageBack", "|", "imageUpload", "imageByURL", "imageManager"]
                }""");

        CASES.put("3 — upload URL set (FroalaUploadController in this app)", "{\n  " + INSERT_BUTTONS + """
                ,
                  "imageUploadURL": "/froala-upload",
                  "fileUploadURL": "/froala-upload",
                  "videoUploadURL": "/froala-upload"
                }""");

        CASES.put("4 — emoji as plain characters instead of cdnjs images", "{\n  " + INSERT_BUTTONS + """
                ,
                  "emoticonsUseImage": false
                }""");
    }

    private final Div editorSlot = new Div();
    private final TextArea html = new TextArea("Editor HTML");

    public UploadCheckView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        TextArea json = new TextArea("Froala options (raw JSON)");
        json.setId("options-json");
        json.setWidthFull();
        json.setMinHeight("10em");

        Select<String> presets = new Select<>();
        presets.setId("preset-select");
        presets.setLabel("Case");
        presets.setItems(CASES.keySet());
        presets.addValueChangeListener(event -> json.setValue(CASES.get(event.getValue())));

        Button apply = new Button("Apply (rebuilds the editor)", event -> rebuild(json.getValue()));
        apply.setId("apply-options");

        html.setId("editor-html");
        html.setWidthFull();
        html.setMinHeight("8em");
        html.setReadOnly(true);

        editorSlot.setId("editor-slot");
        editorSlot.setWidthFull();

        add(new Paragraph("Insert an image, file, video or emoji and read the HTML below, with the browser's "
                + "network tab open. blob: means the file stayed in this browser tab and dies on reload; an "
                + "https://... URL means the content is fetched from, or was sent to, somebody else's server. "
                + "Case 3 posts to FroalaUploadController in this app, which stores the file and hands back the "
                + "/froala-upload/... link it is then served under — reload the page and the image survives."),
                new HorizontalLayout(presets, apply), json, editorSlot, html);

        presets.setValue(CASES.keySet().iterator().next());
        rebuild(json.getValue());
    }

    private void rebuild(String rawJson) {
        editorSlot.removeAll();
        html.clear();

        FroalaEditor editor = new FroalaEditor("Test editor");
        editor.setId("editor");
        editor.setWidthFull();
        editor.setHeight("400px");
        editor.setOptions(rawJson);
        editor.addValueChangeListener(event -> html.setValue(event.getValue() == null ? "" : event.getValue()));

        editorSlot.add(editor);
    }
}
