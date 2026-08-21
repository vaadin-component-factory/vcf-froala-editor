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

import org.apache.commons.text.WordUtils;
import org.jspecify.annotations.NonNull;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@AnonymousAllowed
public class BasicView extends VerticalLayout {

    public BasicView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        FroalaEditor editor = new FroalaEditor("Test editor");
        editor.setId("editor");
        editor.setHelperText("Hello World, it's-a-me, Malario");

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.getStyle().setBorder("2px dashed gray").setBorderRadius("5px");

        HorizontalLayout toolbar = createToolbar(editor, viewer);

        add(toolbar, editor, viewer);
        // editor.setMinHeight("500px");
        // editor.setMaxHeight("750px");
        editor.setHeight("500px");
        viewer.setMinHeight("250px");
        // setFlexGrow(1, editor, viewer);

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));
    }

    private @NonNull HorizontalLayout createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setAlignItems(Alignment.BASELINE);
        Button focus = new Button("Focus", _unused -> editor.focus());
        focus.setId("focus-button");
        toolbar.add(focus);

        Select<ValueChangeMode> valueChangeMode = new Select<>("Value Change Mode",
                event -> editor.setValueChangeMode(event.getValue()));
        valueChangeMode.setItems(ValueChangeMode.values());
        // the underscore has to go first, capitalizeFully only splits on whitespace -- ON_BLUR would read "On_blur"
        valueChangeMode.setItemLabelGenerator(item -> WordUtils.capitalizeFully(item.name().replace('_', ' ')));
        valueChangeMode.setValue(editor.getValueChangeMode());
        valueChangeMode.setId("value-change-mode");
        toolbar.add(valueChangeMode);

        // The controls below exist for the e2e tests as much as for the demo: readonly, disabled and detach are
        // behaviours the browser has to prove, and a test cannot reach the server-side API on its own.
        Checkbox readOnly = new Checkbox("Read-only", event -> editor.setReadOnly(event.getValue()));
        readOnly.setId("readonly-toggle");
        toolbar.add(readOnly);

        Checkbox enabled = new Checkbox("Enabled", event -> editor.setEnabled(event.getValue()));
        enabled.setValue(editor.isEnabled());
        enabled.setId("enabled-toggle");
        toolbar.add(enabled);

        Button toggleAttached = new Button("Toggle attached", _unused -> toggleAttached(editor));
        toggleAttached.setId("attach-toggle");
        toolbar.add(toggleAttached);

        return toolbar;
    }

    /// Detaches the editor from this view, or re-attaches it in its original position. Exercises the round trip the
    /// delta design depends on: on detach the server pushes the accumulated value into the element, on attach the
    /// client rebuilds Froala from it.
    private void toggleAttached(FroalaEditor editor) {
        if (editor.getParent().isPresent()) {
            remove(editor);
        } else {
            addComponentAtIndex(1, editor);
        }
    }
}
