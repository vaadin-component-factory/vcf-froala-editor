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

import org.springframework.stereotype.Service;

/**
 * The files users uploaded in the demo's editors. {@link FroalaFileServingRestController} serves them under the link
 * {@link #store(byte[])} returns.
 * <p>
 * Demo only. Files live in a map for as long as the process does. No disk, no size cap, no cleanup, no access control.
 */
@Service
public class FroalaFileUploadService {

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    /** Stores the bytes and returns the link they are served under, which stays valid as long as the process runs. */
    public String store(byte[] bytes) {
        String id = UUID.randomUUID().toString();
        files.put(id, bytes);
        return "/froala-upload/" + id;
    }

    /** The bytes stored under the id, or null for an unknown one. */
    public byte[] get(String id) {
        return files.get(id);
    }
}
