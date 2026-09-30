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
package com.vaadin.componentfactory.froala.ui;

import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.dom.Style;

/**
 * Just a prestyled subclass of the {@link FroalaViewer}
 */
public class DemoFroalaViewer extends FroalaViewer {
    public DemoFroalaViewer() {
        setId("viewer");
        getStyle().setBorder("2px dashed gray").setBorderRadius("5px").setAlignSelf(Style.AlignSelf.STRETCH);

        setMinHeight("250px");

        // The router would take a click on an uploaded file's link as a route, which it is not.
        setRouterIgnorePaths("/froala-upload");
    }
}
