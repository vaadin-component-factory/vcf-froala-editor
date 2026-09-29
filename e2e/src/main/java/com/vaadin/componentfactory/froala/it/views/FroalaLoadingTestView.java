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
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for loading Froala's files on demand. It starts with one editor with one of Froala's plugins and a
 * language, and a second editor with only a plugin of the page's own, which downloads nothing. What the browser
 * downloaded is only telling while no other editor on the page asks for more.
 *
 * <p>
 * Loading the files takes a round trip of its own, so the buttons act on an editor whose files are still loading when a
 * test holds them back. Each button disables itself, so a test can wait for the round trip to have landed.
 */
@Route(FroalaLoadingTestView.ROUTE)
@JsModule("./own-plugin.js")
@AnonymousAllowed
public class FroalaLoadingTestView extends Div {

    public static final String ROUTE = "it/froala-loading";

    public FroalaLoadingTestView() {
        FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withLanguage("de")
                .withPluginsEnabled(FroalaPlugin.ALIGN).withToolbarButtons(FroalaToolbar.of("bold", "align")));
        editor.setId("editor");
        editor.setHeight("300px");

        Button otherOptions = new Button("Other options");
        otherOptions.setId("other-options");
        otherOptions.addClickListener(event -> {
            // another plugin than the initial one, so a test can tell which of the two the editor was built with
            editor.setOptions(FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.LISTS)
                    .withToolbarButtons(FroalaToolbar.of("formatUL")));
            otherOptions.setEnabled(false);
        });

        // focus() in the same round trip as the attach, while the new editor has not even started to load its files
        Button addFocused = new Button("Add a focused editor");
        addFocused.setId("add-focused");
        addFocused.addClickListener(event -> {
            FroalaEditor focused = new FroalaEditor(FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.ALIGN));
            focused.setId("focused");
            add(focused);
            focused.focus();
            addFocused.setEnabled(false);
        });

        // a plugin the application registers itself, whose file is in the page's bundle, so nothing more is downloaded
        FroalaEditor own = new FroalaEditor(FroalaOptions.defaults().withPluginsEnabled("ownPlugin"));
        own.setId("own");

        add(otherOptions, addFocused, editor, own);
    }
}
