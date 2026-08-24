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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view whose editor is disabled and read-only <em>before</em> it is ever attached.
 *
 * <p>
 * This is the only way to reach the connector's init-time re-apply path: Froala builds asynchronously, so a state the
 * server set while it was still building can only be applied from Froala's own {@code initialized} event. A view that
 * toggles the state after load exercises the ordinary update path instead and proves nothing about this one.
 */
@Route(FroalaDisabledAtInitTestView.ROUTE)
@AnonymousAllowed
public class FroalaDisabledAtInitTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-disabled-at-init";

    public FroalaDisabledAtInitTestView() {
        FroalaEditor disabled = new FroalaEditor("Disabled before attach");
        disabled.setId("disabled-editor");
        disabled.setEnabled(false);

        FroalaEditor readOnly = new FroalaEditor("Read-only before attach");
        readOnly.setId("readonly-editor");
        readOnly.setReadOnly(true);

        add(disabled, readOnly);
    }
}
