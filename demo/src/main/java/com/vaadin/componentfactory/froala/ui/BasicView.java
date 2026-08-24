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
import com.vaadin.flow.component.HasComponents;
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

    public static final String INITIAL_VALUE = "<p>Hello <b>World</b></p>";
    public static final String ALTERNATIVE_VALUE = "<p><em>Moi,&nbsp;</em><strong><span style=\"color: rgb(44, 130, 201);\"><em>Vaadin</em> }&gt;&nbsp;<span class=\"fr-emoticon fr-deletable fr-emoticon-img\" style=\"background: url(https://cdnjs.cloudflare.com/ajax/libs/emojione/2.0.1/assets/svg/1f601.svg);\">&nbsp;</span></span></strong></p>";
    private final HorizontalLayout toolbar;
    private final FroalaEditor editor;
    private final FroalaViewer viewer;

    private HasComponents froalaReattachParent = this;

    public BasicView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        editor = new FroalaEditor("Test editor");
        editor.setId("editor");
        editor.setHelperText("Just a helper text");

        viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.getStyle().setBorder("2px dashed gray").setBorderRadius("5px");

        toolbar = createToolbar(editor, viewer);

        add(toolbar, editor, viewer);
        // editor.setMinHeight("500px");
        // editor.setMaxHeight("750px");
        editor.setHeight("500px");
        viewer.setMinHeight("250px");
        // setFlexGrow(1, editor, viewer);

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));
        editor.setValue(INITIAL_VALUE);
    }

    private @NonNull HorizontalLayout createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWrap(true);
        toolbar.setAlignItems(Alignment.BASELINE);

        Button changeValue = new Button("Change value", _unused -> {
            if (INITIAL_VALUE.equals(editor.getValue())) {
                editor.setValue(ALTERNATIVE_VALUE);
            } else {
                editor.setValue(INITIAL_VALUE);
            }
        });
        toolbar.add(changeValue);

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

        Button focus = new Button("Focus", _unused -> editor.focus());
        focus.setId("focus-button");
        toolbar.add(focus);

        Button toggleAttached = new Button("Toggle attached", _unused -> toggleAttached(editor));
        toggleAttached.setId("attach-toggle");
        toolbar.add(toggleAttached);

        return toolbar;
    }

    /**
     * Detaches the editor from this view, or re-attaches it in its original position. Exercises the round trip the
     * delta design depends on: the value the client accumulated has to survive the rebuild.
     */
    private void toggleAttached(FroalaEditor editor) {
        if (editor.getParent().isPresent()) {
            froalaReattachParent = (HasComponents) editor.getParent().get();
            editor.removeFromParent();
        } else {
            froalaReattachParent.addComponentAtIndex(froalaReattachParent.indexOf(toolbar) + 1, editor);
        }
    }

    public FroalaEditor getEditor() {
        return editor;
    }

    public FroalaViewer getViewer() {
        return viewer;
    }

    public HorizontalLayout getToolbar() {
        return toolbar;
    }
}
