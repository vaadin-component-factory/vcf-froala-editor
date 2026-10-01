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

import java.util.Set;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaUploadHandler;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.files.FroalaFileServingVaadinRequestHandler;
import com.vaadin.componentfactory.froala.files.FroalaFileUploadService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/** Every plugin that needs no service, with Froala's own toolbar. */
@Route("full")
@Menu(title = "Full", order = 0.5)
public class FullView extends FroalaViewBase {

    public FullView(FroalaFileUploadService uploadedFiles) {
        getEditor().setHeight("500px");
        getEditor().setOptions(FroalaOptions.defaults().withPluginsEnabled(plugins())
                .withToolbarButtons(FroalaToolbar.froalaDefault()));

        // the same handler as in UploadView's first tab
        FroalaUploadHandler handler = event -> FroalaFileServingVaadinRequestHandler
                .link(uploadedFiles.store(event.getInputStream().readAllBytes(), event.getFileName()));
        getEditor().setImageUploadHandler(handler);
        getEditor().setFileUploadHandler(handler);
        getEditor().setVideoUploadHandler(handler);
        // The router would take a click on an uploaded file's link as a route, which it is not.
        getViewer().setRouterIgnorePaths(FroalaFileServingVaadinRequestHandler.PATH);

        addComponentAtIndex(indexOf(getToolbar()) + 1,
                new Paragraph("Every Froala plugin except those that need a server, a second library or a paid "
                        + "service, such as the AI assistant, collaboration or the image manager. The toolbar is "
                        + "Froala's own. Uploads go to the demo's upload handler, as in the Upload / Files view."));

    }

    @Override
    protected Component createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        return new DemoToolbar(editor, plugins(), FroalaToolbar.froalaDefault());
    }

    /** The demo's plugins plus the file plugin, which this view can offer because it has a file upload handler. */
    private static Set<String> plugins() {
        Set<String> plugins = DemoPlugins.full();
        plugins.add(FroalaPlugin.FILE);

        return plugins;
    }
}
