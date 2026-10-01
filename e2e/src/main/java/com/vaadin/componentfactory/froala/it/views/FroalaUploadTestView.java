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

import java.io.IOException;
import java.util.Base64;

import com.vaadin.componentfactory.froala.FroalaButton;
import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaUploadHandler;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.UploadEvent;

/**
 * Fixture view for {@code FroalaUploadIT}, owned by the tests.
 *
 * <p>
 * The handler answers with the uploaded bytes as a {@code data:} URL. Froala loads the link it gets back and drops the
 * image when that fails, so the link has to be loadable, and a {@code data:} URL is without an endpoint. It also shows
 * in the value that the bytes arrived unchanged. The parameter {@code source=handler} tells the link apart from a
 * {@code data:} URL Froala makes itself, which it does for a pasted image before it uploads it.
 */
@Route(FroalaUploadTestView.ROUTE)
@AnonymousAllowed
public class FroalaUploadTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-upload";

    /**
     * An endpoint of the application's own. Never called, because the tests only read it back out of Froala's options.
     */
    public static final String OWN_IMAGE_UPLOAD_URL = "/own/upload/image";

    /** The largest file in bytes the limited editor's handler takes. */
    public static final long FILE_SIZE_MAX = 10;

    private static final FroalaOptions OPTIONS = FroalaOptions.defaults()
            .withPluginsEnabled(FroalaPlugin.IMAGE, FroalaPlugin.FILE, FroalaPlugin.VIDEO).withToolbarButtons(
                    FroalaToolbar.of(FroalaButton.INSERT_IMAGE, FroalaButton.INSERT_FILE, FroalaButton.INSERT_VIDEO));

    public FroalaUploadTestView() {
        FroalaUploadHandler handler = event -> "data:" + event.getContentType() + ";source=handler;base64,"
                + Base64.getEncoder().encodeToString(event.getInputStream().readAllBytes());

        FroalaEditor editor = new FroalaEditor("With handlers", OPTIONS.withImageUploadUrl(OWN_IMAGE_UPLOAD_URL));
        editor.setId("editor");
        editor.setImageUploadHandler(handler);
        editor.setFileUploadHandler(handler);
        editor.setVideoUploadHandler(handler);

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        Button readOnly = new Button("Read-only", event -> {
            editor.setReadOnly(true);
            event.getSource().setEnabled(false);
        });
        readOnly.setId("read-only");

        Button removeHandlers = new Button("Remove handlers", event -> {
            editor.setImageUploadHandler(null);
            editor.setFileUploadHandler(null);
            editor.setVideoUploadHandler(null);
            event.getSource().setEnabled(false);
        });
        removeHandlers.setId("remove-handlers");

        FroalaEditor withoutHandlers = new FroalaEditor("Without handlers", OPTIONS);
        withoutHandlers.setId("without-handlers");

        FroalaEditor withOwnUrl = new FroalaEditor("With an own URL", FroalaOptions.defaults()
                .withPluginsEnabled(FroalaPlugin.IMAGE).withImageUploadUrl(OWN_IMAGE_UPLOAD_URL));
        withOwnUrl.setId("with-own-url");

        FroalaEditor limited = new FroalaEditor("With a size limit", OPTIONS);
        limited.setId("limited");
        limited.setImageUploadHandler(new FroalaUploadHandler() {
            @Override
            public String upload(UploadEvent event) throws IOException {
                return handler.upload(event);
            }

            @Override
            public long getFileSizeMax() {
                return FILE_SIZE_MAX;
            }
        });

        add(readOnly, removeHandlers, editor, viewer, withoutHandlers, withOwnUrl, limited);
    }
}
