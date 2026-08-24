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
package com.vaadin.componentfactory.froala.it.views;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for {@code FroalaEditorIT}, owned by the tests.
 *
 * <p>
 * Deliberately separate from the demo's {@code BasicView}: the demo exists to show the add-on off and its author must
 * stay free to change labels, values and layout without breaking tests. Everything an assertion depends on is a
 * constant here, and every control exists because a test needs to reach server-side API that a browser cannot call.
 *
 * <p>
 * The viewer is the assertion channel — only the server-side value change listener ever writes to it, so text appearing
 * there is proof that a client edit made the full round trip.
 */
@Route(FroalaTestView.ROUTE)
@AnonymousAllowed
public class FroalaTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala";

    public static final String LABEL = "Editor under test";
    public static final String HELPER_TEXT = "Helper text under test";
    public static final String INITIAL_VALUE = "<p>seeded by the server</p>";
    public static final String INITIAL_TEXT = "seeded by the server";

    public FroalaTestView() {
        setSizeFull();

        FroalaEditor editor = new FroalaEditor(LABEL);
        editor.setId("editor");
        editor.setHelperText(HELPER_TEXT);
        editor.setHeight("300px");

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.setMinHeight("100px");

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        Button focus = new Button("Focus", event -> editor.focus());
        focus.setId("focus-button");

        Select<ValueChangeMode> valueChangeMode = new Select<>("Value change mode",
                event -> editor.setValueChangeMode(event.getValue()));
        valueChangeMode.setItems(ValueChangeMode.values());
        // the enum name verbatim, so a test never depends on a label formatter
        valueChangeMode.setItemLabelGenerator(ValueChangeMode::name);
        valueChangeMode.setValue(editor.getValueChangeMode());
        valueChangeMode.setId("value-change-mode");

        Checkbox readOnly = new Checkbox("Read-only", event -> editor.setReadOnly(event.getValue()));
        readOnly.setId("readonly-toggle");

        Checkbox enabled = new Checkbox("Enabled", event -> editor.setEnabled(event.getValue()));
        enabled.setValue(editor.isEnabled());
        enabled.setId("enabled-toggle");

        Button toggleAttached = new Button("Toggle attached", event -> {
            if (editor.getParent().isPresent()) {
                remove(editor);
            } else {
                addComponentAtIndex(1, editor);
            }
        });
        toggleAttached.setId("attach-toggle");

        add(focus, valueChangeMode, readOnly, enabled, toggleAttached, editor, viewer);

        // set last, so the value is on the server before the first attach reaches the client
        editor.setValue(INITIAL_VALUE);
    }
}
