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
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Phase 2, check 1: are Froala's toolbar group names free, or do only {@code moreText}, {@code moreParagraph},
 * {@code moreRich} and {@code moreMisc} work? Decides whether the group name is a {@code String} or an enum in
 * {@code FroalaOptions}.
 * <p>
 * What to look at, per case: does the group render at all, and does it get a "more" (…) button once it holds more
 * buttons than {@code buttonsVisible}?
 */
@Route("check-toolbar")
@Menu(title = "Check 1: Toolbar groups", order = 10)
public class ToolbarCheckView extends VerticalLayout {

    private static final Map<String, String> CASES = new LinkedHashMap<>();

    static {
        CASES.put("1 — no toolbarButtons (Froala's own default)", "{}");

        CASES.put("2 — flat list of buttons", """
                {
                  "toolbarButtons": ["bold", "italic", "underline", "|", "insertLink", "insertImage"]
                }""");

        CASES.put("3 — flat list, nested arrays are groups", """
                {
                  "toolbarButtons": [["bold", "italic"], ["formatOL", "formatUL"], ["undo", "redo"]]
                }""");

        CASES.put("4 — groups, Froala's own names, overflow", """
                {
                  "toolbarButtons": {
                    "moreText": {
                      "buttons": ["bold", "italic", "underline", "strikeThrough", "textColor"],
                      "buttonsVisible": 2
                    },
                    "moreMisc": {
                      "buttons": ["undo", "redo", "html", "fullscreen"],
                      "align": "right",
                      "buttonsVisible": 2
                    }
                  }
                }""");

        CASES.put("5 — groups, free names, overflow", """
                {
                  "toolbarButtons": {
                    "myTextGroup": {
                      "buttons": ["bold", "italic", "underline", "strikeThrough", "textColor"],
                      "buttonsVisible": 2
                    },
                    "myMiscGroup": {
                      "buttons": ["undo", "redo", "html", "fullscreen"],
                      "align": "right",
                      "buttonsVisible": 2
                    }
                  }
                }""");

        CASES.put("6 — groups, free names, no overflow", """
                {
                  "toolbarButtons": {
                    "myTextGroup": {
                      "buttons": ["bold", "italic"],
                      "buttonsVisible": 2
                    },
                    "myMiscGroup": {
                      "buttons": ["undo", "redo"],
                      "align": "right",
                      "buttonsVisible": 2
                    }
                  }
                }""");

        CASES.put("7 — group named after a registered command", """
                {
                  "toolbarButtons": {
                    "insertLink": {
                      "buttons": ["bold", "italic", "underline", "strikeThrough", "textColor"],
                      "buttonsVisible": 2
                    }
                  }
                }""");

        CASES.put("8 — unknown button name in a group", """
                {
                  "toolbarButtons": {
                    "moreText": {
                      "buttons": ["bold", "thisCommandDoesNotExist", "italic"],
                      "buttonsVisible": 3
                    }
                  }
                }""");
    }

    private final Div editorSlot = new Div();

    public ToolbarCheckView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        TextArea json = new TextArea("Froala options (raw JSON)");
        json.setId("options-json");
        json.setWidthFull();
        json.setMinHeight("14em");

        Select<String> presets = new Select<>();
        presets.setId("preset-select");
        presets.setLabel("Case");
        presets.setItems(CASES.keySet());
        presets.addValueChangeListener(event -> json.setValue(CASES.get(event.getValue())));

        Button apply = new Button("Apply (rebuilds the editor)", event -> rebuild(json.getValue()));
        apply.setId("apply-options");

        editorSlot.setId("editor-slot");
        editorSlot.setWidthFull();

        add(new Paragraph("Options are init-only, so every case rebuilds the editor. Watch two things: does the "
                + "group render, and does an overflow group get a … button to open it?"),
                new HorizontalLayout(presets, apply), json, editorSlot);

        presets.setValue(CASES.keySet().iterator().next());
        rebuild(json.getValue());
    }

    private void rebuild(String rawJson) {
        editorSlot.removeAll();

        FroalaEditor editor = new FroalaEditor("Test editor");
        editor.setId("editor");
        editor.setWidthFull();
        editor.setHeight("400px");
        editor.setValue("<p>Hello <b>World</b></p>");
        editor.getElement().setPropertyJson("initialConfig", JacksonUtils.readTree(rawJson));

        editorSlot.add(editor);
    }
}
