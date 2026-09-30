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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class FroalaViewBase extends VerticalLayout {
    private final Component toolbar;
    private final FroalaEditor editor;
    private final FroalaViewer viewer;

    protected FroalaViewBase() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        editor = new FroalaEditor("Test editor");
        editor.setId("editor");
        editor.setHelperText("Just a helper text");

        viewer = new DemoFroalaViewer();

        toolbar = createToolbar(editor, viewer);

        add(toolbar, editor, viewer);
        // setFlexGrow(1, editor, viewer);

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));
    }

    protected Component createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWrap(true);
        toolbar.setAlignItems(Alignment.BASELINE);

        return toolbar;
    }

    protected FroalaEditor getEditor() {
        return editor;
    }

    protected FroalaViewer getViewer() {
        return viewer;
    }

    protected Component getToolbar() {
        return toolbar;
    }
}
