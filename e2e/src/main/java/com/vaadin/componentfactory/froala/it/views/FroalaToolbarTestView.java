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
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for the toolbar presets. The editor has the basic toolbar and no plugin options, so it gets the basic
 * plugins. The module puts Froala's own default toolbar on the page for comparison.
 */
@Route(FroalaToolbarTestView.ROUTE)
@AnonymousAllowed
@JsModule("./froala-default-toolbar.js")
public class FroalaToolbarTestView extends Div {

    public static final String ROUTE = "it/froala-toolbar";

    public FroalaToolbarTestView() {
        FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.basics()));
        editor.setId("basics");

        add(editor);
    }
}
