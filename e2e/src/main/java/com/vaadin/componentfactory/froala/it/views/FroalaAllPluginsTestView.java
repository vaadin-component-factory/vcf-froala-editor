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
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view with an editor that enables every {@link FroalaPlugin}. It registers on the page the plugins that
 * {@link FroalaPlugin#basics()} leaves out. The button adds an editor that enables none explicitly, once the first one
 * is built, to show whether its default depends on what another editor loaded. The button disables itself, so a test
 * can wait for the round trip. The module puts Froala's constructor on the page, so the tests can read its commands.
 */
@Route(FroalaAllPluginsTestView.ROUTE)
@JsModule("./froala-editor-class.js")
@AnonymousAllowed
public class FroalaAllPluginsTestView extends Div {

    public static final String ROUTE = "it/froala-all-plugins";

    public FroalaAllPluginsTestView() {
        FroalaEditor all = new FroalaEditor(FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.all()));
        all.setId("all");

        Button addDefaults = new Button("Add an editor with the default plugins");
        addDefaults.setId("add-defaults");
        addDefaults.addClickListener(event -> {
            FroalaEditor defaults = new FroalaEditor();
            defaults.setId("defaults");
            add(defaults);
            addDefaults.setEnabled(false);
        });

        add(addDefaults, all);
    }
}
