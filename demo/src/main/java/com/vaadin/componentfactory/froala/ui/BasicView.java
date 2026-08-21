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

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.apache.commons.text.WordUtils;
import org.jspecify.annotations.NonNull;

@Route("")
@AnonymousAllowed
@NpmPackage(value = "froala-editor", version = "5.4.0")
@CssImport("froala-editor/css/froala_editor.pkgd.min.css")
@JsModule("froala-editor/js/froala_editor.pkgd.min.js")
public class BasicView extends VerticalLayout {

    public BasicView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        FroalaEditor editor = new FroalaEditor("Test editor");
        editor.setHelperText("Hello World, it's-a-me, Malario");

        FroalaViewer viewer = new FroalaViewer();
        viewer.getStyle().setBorder("2px dashed gray").setBorderRadius("5px");

        HorizontalLayout toolbar = createToolbar(editor, viewer);


        add(toolbar, editor, viewer);
//        editor.setMinHeight("500px");
//        editor.setMaxHeight("750px");
        editor.setHeight("500px");
        viewer.setMinHeight("250px");
//        setFlexGrow(1, editor, viewer);

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));
    }

    private @NonNull HorizontalLayout createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.add(new Button("Focus", _unused -> editor.focus()));

        Select<ValueChangeMode> valueChangeMode = new Select<>("Value Change Mode", event -> editor.setValueChangeMode(event.getValue()));
        valueChangeMode.setItems(ValueChangeMode.values());
        valueChangeMode.setItemLabelGenerator(item -> WordUtils.capitalizeFully(item.name()));
        valueChangeMode.setValue(editor.getValueChangeMode());
        toolbar.add(valueChangeMode);

        return toolbar;
    }
}
