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

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view with room to the left of the editor. Froala's quick-insert button then goes outside the editor box, to
 * its left, and an editor flush with the page edge would never show whether that position gets clipped.
 *
 * <p>
 * The second editor starts with more content than its fixed height holds, to show whether the field keeps its height.
 */
@Route(FroalaQuickInsertTestView.ROUTE)
@AnonymousAllowed
public class FroalaQuickInsertTestView extends Div {

    public static final String ROUTE = "it/froala-quick-insert";

    public FroalaQuickInsertTestView() {
        getStyle().set("margin-left", "300px");

        // quick insert is not among the basics, and it offers what these plugins insert
        FroalaEditor empty = new FroalaEditor("Empty",
                FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.QUICK_INSERT, FroalaPlugin.IMAGE,
                        FroalaPlugin.VIDEO, FroalaPlugin.TABLE, FroalaPlugin.LISTS));
        empty.setId("empty-editor");
        empty.setHeight("300px");

        FroalaEditor overfull = new FroalaEditor("More content than fits");
        overfull.setId("overfull-editor");
        overfull.setHeight("300px");
        overfull.setValue("<p>Line</p>".repeat(50));

        add(empty, overfull);
    }
}
