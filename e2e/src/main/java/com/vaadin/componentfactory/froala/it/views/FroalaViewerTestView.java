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

import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view that holds a viewer and no editor, so nothing but the viewer itself loads the add-on's stylesheets.
 */
@Route(FroalaViewerTestView.ROUTE)
@AnonymousAllowed
public class FroalaViewerTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-viewer";

    public FroalaViewerTestView() {
        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        // Two views of the e2e app, one of them matched by a router-ignore pattern.
        viewer.setContent("<p>Shown without an editor</p>" //
                + "<p><a id=\"ignored\" href=\"" + FroalaThemeTestView.ROUTE + "\">Page load</a></p>" //
                + "<p><a id=\"routed\" href=\"" + FroalaDirectionTestView.ROUTE + "\">Router</a></p>");
        viewer.setRouterIgnorePaths("/" + FroalaThemeTestView.ROUTE + "*");
        add(viewer);
    }
}
