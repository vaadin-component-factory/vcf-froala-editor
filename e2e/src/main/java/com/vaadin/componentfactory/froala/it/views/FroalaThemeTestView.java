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

import java.util.Set;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaEditorVariant;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaTheme;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Fixture view for {@code FroalaThemeIT}, owned by the tests. Under {@link #AURA_ROUTE} it runs with Aura. */
@Route(FroalaThemeTestView.ROUTE)
@RouteAlias(value = FroalaThemeTestView.AURA_ROUTE, layout = AuraLayout.class)
@AnonymousAllowed
public class FroalaThemeTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-theme";

    public static final String AURA_ROUTE = "it/froala-theme-aura";

    /** A heading, because Lumo colours headings globally and Froala's content sits in the light DOM (THM-12). */
    public static final String VALUE = "<h1>Heading</h1><p>Body text</p>";

    /**
     * The plugins of Froala's packaged bundle, whose stylesheet the theme is generated from, so that the tests see
     * everything the theme covers. That is every plugin except track changes, video trimming and the third-party ones.
     */
    private static final FroalaOptions WITH_BUNDLED_PLUGINS = FroalaOptions.defaults().withPluginsEnabled(bundled());

    private static Set<String> bundled() {
        Set<String> plugins = FroalaPlugin.all();
        plugins.removeAll(Set.of(FroalaPlugin.TRACK_CHANGES, FroalaPlugin.TRIM_VIDEO, FroalaPlugin.EMBEDLY,
                FroalaPlugin.FONT_AWESOME, FroalaPlugin.IMAGE_FILEROBOT, FroalaPlugin.IMAGE_TUI,
                FroalaPlugin.SPELL_CHECKER));

        return plugins;
    }

    public FroalaThemeTestView() {
        FroalaEditor themed = new FroalaEditor("Default theme", WITH_BUNDLED_PLUGINS);
        themed.setId("themed");
        themed.setValue(VALUE);

        FroalaEditor outlined = new FroalaEditor("Outlined", WITH_BUNDLED_PLUGINS);
        outlined.setId("outlined");
        outlined.addThemeVariants(FroalaEditorVariant.OUTLINED);
        outlined.setValue(VALUE);

        FroalaEditor noHover = new FroalaEditor("No hover highlight", WITH_BUNDLED_PLUGINS);
        noHover.setId("no-hover");
        noHover.addThemeVariants(FroalaEditorVariant.NO_HOVER_HIGHLIGHT);
        noHover.setValue(VALUE);

        FroalaEditor plain = new FroalaEditor("No theme", WITH_BUNDLED_PLUGINS.withTheme(FroalaTheme.NONE));
        plain.setId("plain");
        plain.setValue(VALUE);

        add(themed, outlined, noHover, plain);
    }
}
