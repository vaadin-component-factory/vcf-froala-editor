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
import com.vaadin.componentfactory.froala.FroalaTextDirection;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for mirroring the editor's text direction onto the host, see {@code FroalaDirectionIT}.
 */
@Route(FroalaDirectionTestView.ROUTE)
@AnonymousAllowed
public class FroalaDirectionTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-direction";

    public FroalaDirectionTestView() {
        FroalaEditor rtl = new FroalaEditor("Direction option",
                FroalaOptions.defaults().withDirection(FroalaTextDirection.RTL));
        rtl.setId("rtl");

        // the direction comes from the language file alone
        FroalaEditor arabic = new FroalaEditor("Arabic language file", FroalaOptions.defaults().withLanguage("ar"));
        arabic.setId("arabic");

        // a dir the application set itself, with options that say nothing about the direction
        FroalaEditor ownDir = new FroalaEditor("Application's own dir");
        ownDir.setId("own-dir");
        ownDir.getElement().setAttribute("dir", "rtl");

        // a dir the application set itself, and options that name another direction
        FroalaEditor ownDirOverridden = new FroalaEditor("Application's own dir, overridden",
                FroalaOptions.defaults().withDirection(FroalaTextDirection.RTL));
        ownDirOverridden.setId("own-dir-overridden");
        ownDirOverridden.getElement().setAttribute("dir", "ltr");

        // every editor gets new options in one round trip, so a test sees one consistent state
        Button otherOptions = new Button("Other options");
        otherOptions.setId("other-options");
        otherOptions.addClickListener(event -> {
            rtl.setOptions(FroalaOptions.defaults());
            arabic.setOptions(FroalaOptions.defaults().withDirection(FroalaTextDirection.LTR));
            // not the defaults, so the editor is certainly built again
            ownDir.setOptions(FroalaOptions.defaults().withSpellcheck(false));
            ownDirOverridden.setOptions(FroalaOptions.defaults());
            otherOptions.setEnabled(false);
        });

        add(otherOptions, rtl, arabic, ownDir, ownDirOverridden);
    }
}
