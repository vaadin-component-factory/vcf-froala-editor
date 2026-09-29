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
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/** Every plugin that needs no service, with Froala's own toolbar. */
@Route("full")
@Menu(title = "Full", order = 0.5)
public class FullView extends FroalaViewBase {

    public FullView() {
        getEditor().setHeight("500px");
        getEditor().setOptions(FroalaOptions.defaults().withPluginsEnabled(DemoPlugins.full())
                .withToolbarButtons(FroalaToolbar.froalaDefault()));

        addComponentAtIndex(indexOf(getToolbar()) + 1,
                new Paragraph("Every Froala plugin except those that need a server, a second library or a paid "
                        + "service, such as the AI assistant, collaboration or the image manager. The toolbar is "
                        + "Froala's own. No upload URL is set, so a file or an "
                        + "image picked from disk does not reach a server."));

        getViewer().setMinHeight("250px");
    }

    @Override
    protected Component createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        return new DemoToolbar(editor, DemoPlugins.full(), FroalaToolbar.froalaDefault());
    }
}
