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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Test view for {@code FroalaSignalIT}. An editor and a viewer are bound to the same signal, and the viewer is the
 * assertion channel, because only the signal ever writes to it.
 */
@Route(FroalaSignalTestView.ROUTE)
@AnonymousAllowed
public class FroalaSignalTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-signal";

    public static final String SERVER_VALUE = "<p>set on the <strong>signal</strong></p>";
    public static final String SERVER_TEXT = "set on the signal";

    public FroalaSignalTestView() {
        ValueSignal<String> html = new ValueSignal<>("");

        FroalaEditor editor = new FroalaEditor();
        editor.setId("editor");
        editor.bindValue(html, html::set);

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.bindContent(html);

        Button setSignal = new Button("Set signal", event -> html.set(SERVER_VALUE));
        setSignal.setId("set-signal");

        add(editor, viewer, setSignal);
    }
}
