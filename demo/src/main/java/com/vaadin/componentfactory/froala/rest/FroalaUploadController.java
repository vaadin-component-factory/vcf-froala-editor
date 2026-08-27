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
package com.vaadin.componentfactory.froala.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The endpoint the {@code /check-upload} view points Froala's {@code imageUploadURL}, {@code fileUploadURL} and
 * {@code videoUploadURL} at, so an upload can be watched going somewhere real instead of into a 404.
 * <p>
 * Froala posts a multipart request with the file under the parameter name {@code file} — the default of
 * {@code imageUploadParam}, {@code fileUploadParam} and {@code videoUploadParam} alike — and expects a JSON object with
 * a {@code link} member back. It then puts that link into the document, so the same application has to serve the file
 * again under it. That round trip is the whole reason upload needs an endpoint and not just a URL.
 * <p>
 * Demo only. Files live in a map for as long as the process does — no disk, no size cap beyond Spring's own, no
 * cleanup, no access control. The add-on's own upload support is phase 3.
 * <p>
 * What is <b>not</b> optional even here: the content type and the file name a browser sees on the way back are decided
 * by this class, not by whoever uploaded. The endpoint answers on the application's own origin, so serving an uploaded
 * file inline under a type the uploader chose is stored cross-site scripting.
 */
@RestController
@RequestMapping("/froala-upload")
public class FroalaUploadController {

    /**
     * What may be handed back with its own content type. Everything else is served as a download, because this endpoint
     * answers on the application's own origin: an uploaded HTML file served inline would run as our page.
     */
    private static final Set<String> INLINE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp",
            "image/bmp");

    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile uploaded) throws IOException {
        String id = UUID.randomUUID().toString();

        files.put(id, new StoredFile(uploaded.getBytes()));

        return Map.of("link", "/froala-upload/" + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serve(@PathVariable String id) {
        StoredFile file = files.get(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        // Sniffed from the bytes, never taken from the upload: the client decides neither the type it is served under
        // nor the name. `nosniff` stops the browser from overriding the decision made here.
        String sniffed = sniff(file.bytes());
        boolean inline = sniffed != null && INLINE_TYPES.contains(sniffed);

        return ResponseEntity.ok()
                .contentType(inline ? MediaType.parseMediaType(sniffed) : MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + id + "\"")
                .header("X-Content-Type-Options", "nosniff").body(file.bytes());
    }

    /** Null when the bytes do not identify themselves, which lands the file in the download branch. */
    private static String sniff(byte[] bytes) {
        try {
            return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    private record StoredFile(byte[] bytes) {
    }
}
