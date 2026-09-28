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
import com.vaadin.componentfactory.froala.FroalaEditorVariant;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaTheme;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Fixture view for {@code FroalaThemeIT}, owned by the tests. */
@Route(FroalaThemeTestView.ROUTE)
@AnonymousAllowed
public class FroalaThemeTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-theme";

    /** A heading, because Lumo colours headings globally and Froala's content sits in the light DOM (THM-12). */
    public static final String VALUE = "<h1>Heading</h1><p>Body text</p>";

    public FroalaThemeTestView() {
        FroalaEditor themed = new FroalaEditor("Default theme");
        themed.setId("themed");
        themed.setValue(VALUE);

        FroalaEditor outlined = new FroalaEditor("Outlined");
        outlined.setId("outlined");
        outlined.addThemeVariants(FroalaEditorVariant.OUTLINED);
        outlined.setValue(VALUE);

        FroalaEditor noHover = new FroalaEditor("No hover highlight");
        noHover.setId("no-hover");
        noHover.addThemeVariants(FroalaEditorVariant.NO_HOVER_HIGHLIGHT);
        noHover.setValue(VALUE);

        FroalaEditor plain = new FroalaEditor("No theme", FroalaOptions.defaults().withTheme(FroalaTheme.NONE));
        plain.setId("plain");
        plain.setValue(VALUE);

        add(themed, outlined, noHover, plain);
    }
}
