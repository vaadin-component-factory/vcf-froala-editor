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
package com.vaadin.componentfactory.froala;

import java.io.IOException;
import java.io.Serializable;

import com.vaadin.flow.server.streams.UploadEvent;

/**
 * Takes a file the user uploaded in the editor and returns the link the editor puts into the document. See
 * {@link FroalaEditor#setImageUploadHandler(FroalaUploadHandler)}.
 */
@FunctionalInterface
public interface FroalaUploadHandler extends Serializable {

    /**
     * Stores an uploaded file and returns its link.
     *
     * <p>
     * The link ends up in the stored HTML, and the browser loads the file from it every time the content is shown. So
     * it has to stay valid across sessions. A URL of a Flow {@code DownloadHandler} does not, because it is bound to
     * the UI it was made for.
     *
     * <p>
     * Runs outside the UI's lock, like any Flow upload. Changes to components go through {@code UI.access}.
     *
     * @param event the upload, with the file's name, content type and content
     * @return the link to the stored file, not null
     * @throws IOException if reading or storing the file fails. The editor then shows Froala's upload error.
     */
    String upload(UploadEvent event) throws IOException;
}
