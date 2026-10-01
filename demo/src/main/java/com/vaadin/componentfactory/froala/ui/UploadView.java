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

import com.vaadin.componentfactory.froala.FroalaButton;
import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaUploadHandler;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.files.FroalaFileServingRestController;
import com.vaadin.componentfactory.froala.files.FroalaFileServingVaadinRequestHandler;
import com.vaadin.componentfactory.froala.files.FroalaFileUploadService;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Uploads through the add-on's upload handlers, with the two ways the demo serves the stored files. The first two tabs
 * look the same, and only the code behind them differs. The third shows an editor without an upload handler.
 */
@Route("upload")
@Menu(title = "Upload / Files", order = 8)
public class UploadView extends VerticalLayout {

    public UploadView(FroalaFileUploadService uploadedFiles) {
        // Both handlers store the file and return the link the editor puts into the document. Only the class that
        // serves the file under that link differs.
        FroalaUploadHandler vaadinHandler = event -> FroalaFileServingVaadinRequestHandler
                .link(uploadedFiles.store(event.getInputStream().readAllBytes(), event.getFileName()));

        FroalaUploadHandler springRestHandler = event -> FroalaFileServingRestController
                .link(uploadedFiles.store(event.getInputStream().readAllBytes(), event.getFileName()));

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Vaadin Request Handler", createSample(vaadinHandler, "FroalaFileServingVaadinRequestHandler serves "
                + "the files with a Vaadin RequestHandler. It runs inside Vaadin's request handling and gets the "
                + "user's VaadinSession, so it can check the user without Spring."));
        tabs.add("Spring Rest Controller", createSample(springRestHandler, "FroalaFileServingRestController serves "
                + "the files with a Spring REST controller. In an application with Spring Security, its path is "
                + "covered by the security rules like any other."));
        tabs.add("No upload", createSample(null, "This sample shows the behavior of the Froala editor"
                + " when no upload handler is defined. Images and videos are embedded with their original url instead of being uploaded "
                + " to the server. The \"file\" plugin has not been activated explicitly."));

        add(new Paragraph("Upload an image, a file or a video through the popup, or drop or paste an image. The "
                + "handler stores it and returns a link, which the viewer below shows."), tabs);
    }

    private static VerticalLayout createSample(FroalaUploadHandler handler, String description) {
        FroalaOptions options = FroalaOptions.defaults().withToolbarButtons(
                FroalaToolbar.of(FroalaButton.INSERT_IMAGE, FroalaButton.INSERT_FILE, FroalaButton.INSERT_VIDEO));

        if (handler != null) {
            options = options.withPluginsEnabled(FroalaPlugin.IMAGE, FroalaPlugin.FILE, FroalaPlugin.VIDEO);
        } else {
            // no file upload, so we disable the respective plugin and implicitly hide the toolbar button
            options = options.withPluginsEnabled(FroalaPlugin.IMAGE, FroalaPlugin.VIDEO);
        }

        FroalaEditor editor = new FroalaEditor(options);
        editor.setWidthFull();

        if (handler != null) {
            editor.setImageUploadHandler(handler);
            editor.setFileUploadHandler(handler);
            editor.setVideoUploadHandler(handler);
        }

        FroalaViewer viewer = new DemoFroalaViewer();
        // The router would take a click on an uploaded file's link as a route, which it is not.
        viewer.setRouterIgnorePaths(FroalaFileServingVaadinRequestHandler.PATH, FroalaFileServingRestController.PATH);
        // Demo only. The viewer shows the editor's HTML as it is. Sanitize it first when other users see it.
        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        VerticalLayout sample = new VerticalLayout(new Paragraph(description), editor, viewer);
        sample.setPadding(false);

        return sample;
    }
}
