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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the files in {@link FroalaFileUploadService} with a Spring REST controller, under {@link #PATH}. The demo's
 * editors upload through the add-on's upload handlers, which need no endpoint, but the link they put into the document
 * has to be served by the application. {@link FroalaFileServingVaadinRequestHandler} does the same without Spring.
 * <p>
 * An application with Spring Security protects the path with its rules, and {@code VaadinSecurityConfigurer} lets only
 * authenticated users through by default. What is <b>not</b> optional even here: the content type a browser sees on the
 * way back is decided by the application, see {@link FroalaFileContentTypes}.
 */
@RestController
@RequestMapping(FroalaFileServingRestController.PATH)
public class FroalaFileServingRestController {

    public static final String PATH = "/froala-upload-spring-rest";

    private final FroalaFileUploadService files;

    public FroalaFileServingRestController(FroalaFileUploadService files) {
        this.files = files;
    }

    /** The link a file stored under the id is served under. */
    public static String link(String id) {
        return PATH + "/" + id;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> serve(@PathVariable String id) {
        FroalaFileUploadService.StoredFile file = files.get(id);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        String inlineType = FroalaFileContentTypes.inlineType(file.bytes());

        return ResponseEntity.ok()
                .contentType(
                        inlineType != null ? MediaType.parseMediaType(inlineType) : MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", FroalaFileContentTypes.disposition(inlineType, file.name()))
                .header("X-Content-Type-Options", "nosniff").body(file.bytes());
    }
}
