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
import com.vaadin.componentfactory.froala.rest.UploadedFiles;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("upload")
@Menu(title = "Uploads", order = 8)
public class UploadView extends VerticalLayout {

    public UploadView(UploadedFiles uploadedFiles) {
        FroalaOptions options = FroalaOptions.defaults()
                .withPluginsEnabled(FroalaPlugin.IMAGE, FroalaPlugin.FILE, FroalaPlugin.VIDEO)
                .withToolbarButtons(FroalaToolbar.of(FroalaButton.INSERT_IMAGE, FroalaButton.INSERT_FILE,
                        FroalaButton.INSERT_VIDEO));

        // Stores the file and returns the link the editor puts into the document. The application serves the file
        // under that link, here FroalaUploadController.
        FroalaUploadHandler handler = event -> uploadedFiles.store(event.getInputStream().readAllBytes());

        FroalaEditor editor = new FroalaEditor("With upload handlers", options);
        editor.setWidthFull();
        editor.setImageUploadHandler(handler);
        editor.setFileUploadHandler(handler);
        editor.setVideoUploadHandler(handler);

        FroalaViewer viewer = new FroalaViewer();
        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        FroalaEditor withoutHandlers = new FroalaEditor("Without upload handlers", options);
        withoutHandlers.setWidthFull();

        add(new Paragraph("Upload an image, a file or a video through the popup, or drop or paste an image. The "
                + "handler stores it and returns a /froala-upload/... link, which the viewer below shows."), editor,
                viewer,
                new Paragraph("Without a handler the upload is off. The popups have no upload button, and a dropped "
                        + "or pasted image is not inserted. Inserting an image or a video by URL still works."),
                withoutHandlers);
    }
}
