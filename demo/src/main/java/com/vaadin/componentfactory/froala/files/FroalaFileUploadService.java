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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * The files users uploaded in the demo's editors. {@link FroalaFileServingVaadinRequestHandler} and
 * {@link FroalaFileServingRestController} each serve them under a path of their own.
 * <p>
 * Demo only. Files live in a map for as long as the process does. No disk, no size cap, no cleanup, no access control.
 */
@Service
public class FroalaFileUploadService {

    /** Every character outside this set is replaced in a file name, see {@link #store(byte[], String)}. */
    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");

    private final Map<String, StoredFile> files = new ConcurrentHashMap<>();

    /**
     * Stores the bytes and returns their id, which stays valid as long as the process runs. The name comes from the
     * uploading browser and goes into a response header when the file is served. So every character other than a
     * letter, a digit, a dot, an underscore or a hyphen becomes an underscore, and {@code a"b.pdf} is kept as
     * {@code a_b.pdf}. Without a name the file is downloaded under its id.
     */
    public String store(byte[] bytes, String name) {
        String id = UUID.randomUUID().toString();
        String safeName = name == null || name.isBlank() ? id : UNSAFE_CHARACTERS.matcher(name).replaceAll("_");
        files.put(id, new StoredFile(safeName, bytes));

        return id;
    }

    /** The file stored under the id, or null for an unknown one. */
    public StoredFile get(String id) {
        return files.get(id);
    }

    /**
     * A stored file with the name it is downloaded under.
     *
     * @param name the name of the uploaded file with only safe characters
     * @param bytes the content
     */
    public record StoredFile(String name, byte[] bytes) {
    }
}
