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
package com.vaadin.componentfactory.froala.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Set;

/**
 * Decides the content type an uploaded file is served under, for both ways the demo serves files.
 * <p>
 * The type is sniffed from the bytes and never taken from the upload, because the files are served from the
 * application's own origin. An uploaded HTML file served inline under a type the uploader chose would run as the
 * application's page, which is stored cross-site scripting. Only known image types are served inline. Everything else
 * is a download, and {@code X-Content-Type-Options: nosniff} stops the browser from overriding the decision.
 */
final class FroalaFileContentTypes {

    private static final Set<String> INLINE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp",
            "image/bmp");

    private FroalaFileContentTypes() {
    }

    /** The image type to serve the bytes inline under, or null when they are served as a download. */
    static String inlineType(byte[] bytes) {
        String sniffed;
        try {
            sniffed = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }

        return sniffed != null && INLINE_TYPES.contains(sniffed) ? sniffed : null;
    }

    /** The value of the {@code Content-Disposition} header, for a name that holds only safe characters. */
    static String disposition(String inlineType, String name) {
        return (inlineType != null ? "inline" : "attachment") + "; filename=\"" + name + "\"";
    }
}
