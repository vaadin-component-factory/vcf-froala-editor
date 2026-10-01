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

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaEditorVariant;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaTheme;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("theme")
@Menu(title = "Theme", order = 5)
public class ThemeView extends VerticalLayout {

    private static final String HTML = """
            <h2>Heading</h2>
            <p>Some <strong>bold</strong>, <em>italic</em> and <a href="https://vaadin.com">linked</a> text.</p>
            <ul><li>First item</li><li>Second item</li></ul>
            <table><thead><tr><th>Name</th><th>Value</th></tr></thead>
            <tbody><tr><td>Lorem</td><td>42</td></tr><tr><td>Ipsum</td><td>7</td></tr></tbody></table>
            """;

    public ThemeView() {
        FroalaEditor froalaDefault = new FroalaEditor("Froala default",
                FroalaOptions.defaults().withPluginsEnabled(DemoPlugins.full()).withTheme(FroalaTheme.NONE));
        froalaDefault.setValue(HTML);
        froalaDefault.setWidthFull();

        // the plugins of DemoPlugins.full(), in both, so that the theme can be compared on most of what Froala draws
        FroalaEditor vaadinTheme = new FroalaEditor("Vaadin theme",
                FroalaOptions.defaults().withPluginsEnabled(DemoPlugins.full()));
        vaadinTheme.setValue(HTML);
        vaadinTheme.setWidthFull();

        MultiSelectComboBox<FroalaEditorVariant> variant = new MultiSelectComboBox<>("Theme Variant");
        variant.setItems(FroalaEditorVariant.values());
        variant.addValueChangeListener(event -> {
            vaadinTheme.removeThemeVariants(FroalaEditorVariant.values());
            vaadinTheme.addThemeVariants(event.getValue().toArray(FroalaEditorVariant[]::new));
        });

        add(froalaDefault, variant, vaadinTheme);
    }
}
