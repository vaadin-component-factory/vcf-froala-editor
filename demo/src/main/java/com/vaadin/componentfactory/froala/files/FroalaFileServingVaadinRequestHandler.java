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

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

/**
 * Serves the files in {@link FroalaFileUploadService} with a Vaadin {@code RequestHandler}, under {@link #PATH}. This
 * works without Spring, and {@link FroalaFileServingRestController} does the same with it.
 * <p>
 * The handler is registered by a {@link VaadinServiceInitListener}. Here it is a Spring bean, which Vaadin picks up by
 * itself. Without Spring, the listener is named in the file
 * {@code META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener} instead. Flow runs such a handler before
 * its own, so the router never sees the path. The handler gets the user's {@link VaadinSession}, which is what an
 * application without Spring Security checks its user against.
 * <p>
 * What is <b>not</b> optional even here: the content type a browser sees on the way back is decided by the application,
 * see {@link FroalaFileContentTypes}.
 */
@Component
public class FroalaFileServingVaadinRequestHandler implements VaadinServiceInitListener {

    public static final String PATH = "/froala-upload-vaadin";

    private final FroalaFileUploadService files;

    public FroalaFileServingVaadinRequestHandler(FroalaFileUploadService files) {
        this.files = files;
    }

    /** The link a file stored under the id is served under. */
    public static String link(String id) {
        return PATH + "/" + id;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(this::serve);
    }

    /** Answers a request for a file, and returns false for every other request so Flow handles it. */
    private boolean serve(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        String path = request.getPathInfo();

        if (path == null || !path.startsWith(PATH + "/")) {
            return false;
        }

        if (!mayRead(session, request)) {
            response.sendError(403, "Forbidden");

            return true;
        }

        String id = path.substring(PATH.length() + 1);
        byte[] bytes = files.get(id);

        if (bytes == null) {
            response.sendError(404, "Not found");

            return true;
        }

        String inlineType = FroalaFileContentTypes.inlineType(bytes);
        response.setContentType(inlineType != null ? inlineType : "application/octet-stream");
        response.setHeader("Content-Disposition", FroalaFileContentTypes.disposition(inlineType, id));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getOutputStream().write(bytes);

        return true;
    }

    /**
     * Where an application checks whether the user may see the file. The user is known from
     * {@code request.getUserPrincipal()}, or from what the application keeps in the session. The handler runs without
     * the session's lock, so it reads the session under it, for example
     * {@code session.accessSynchronously(() -> session.getAttribute(User.class))}. The demo has no login and lets
     * everyone in.
     */
    private static boolean mayRead(VaadinSession session, VaadinRequest request) {
        return true;
    }
}
