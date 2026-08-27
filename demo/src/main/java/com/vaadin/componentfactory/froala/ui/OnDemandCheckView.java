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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Phase 2, check 3: can Froala's plugins and language files be loaded on demand after the Vaadin build, instead of
 * shipping the 1956 KB package with all 49 plugins in it? Decides whether that bundle stays.
 * <p>
 * The editor here is <b>not</b> the add-on. It is a spike element ({@code froala-ondemand-spike.js}) that imports only
 * Froala's core and pulls the selected plugins and the selected language with a dynamic import. Open the browser's
 * network tab: one request per selected plugin, none for the rest.
 */
@Route("check-on-demand")
@Menu(title = "Check 3: On-demand loading", order = 12)
public class OnDemandCheckView extends VerticalLayout {

    /** Enough for a normal editor, and small enough that the unloaded rest is obvious in the network tab. */
    private static final List<String> DEFAULT_PLUGINS = List.of("align", "colors", "font_family", "font_size", "link",
            "lists", "paragraph_format", "table");

    private static final List<String> ALL_PLUGINS = List.of("ai_assist", "align", "char_counter", "code_beautifier",
            "code_snippet", "code_view", "collaborative", "colors", "cryptojs", "draggable", "edit_in_popup",
            "emoticons", "entities", "export_to_word", "file", "files_manager", "filestack", "find_and_replace",
            "font_family", "font_size", "forms", "fullscreen", "help", "image", "image_manager", "import_from_word",
            "inline_class", "inline_style", "line_breaker", "line_height", "link", "link_to_anchor", "lists",
            "markdown", "page_break", "paragraph_format", "paragraph_style", "print", "quick_insert", "quote", "save",
            "special_characters", "table", "track_changes", "trim_video", "url", "video", "word_counter", "word_paste");

    private static final List<String> LANGUAGES = List.of("", "de", "fr", "es", "it", "nl", "pl", "pt_br", "ru", "sv",
            "fi", "ar", "he", "ja", "zh_cn");

    private final Div spikeSlot = new Div();
    private final TextArea report = new TextArea("Report from the browser");

    public OnDemandCheckView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        MultiSelectComboBox<String> plugins = new MultiSelectComboBox<>("Plugins to load");
        plugins.setId("plugin-select");
        plugins.setItems(ALL_PLUGINS);
        plugins.setValue(Set.copyOf(DEFAULT_PLUGINS));
        plugins.setWidth("40em");

        Select<String> language = new Select<>();
        language.setId("language-select");
        language.setLabel("Language file");
        language.setItems(LANGUAGES);
        language.setItemLabelGenerator(item -> item.isEmpty() ? "(none — English)" : item);
        language.setValue("de");

        Button apply = new Button("Load and build",
                event -> rebuild(plugins.getValue().stream().sorted().toList(), language.getValue()));
        apply.setId("apply-selection");

        Button all = new Button("Load all 49", event -> {
            plugins.setValue(Set.copyOf(ALL_PLUGINS));
            rebuild(ALL_PLUGINS, language.getValue());
        });
        all.setId("apply-all");

        report.setId("spike-report");
        report.setWidthFull();
        report.setMinHeight("14em");
        report.setReadOnly(true);

        spikeSlot.setId("spike-slot");
        spikeSlot.setWidthFull();

        add(new Paragraph("Not the add-on: a spike that imports Froala's core only and pulls each selected plugin and "
                + "the selected language with a dynamic import. Watch the network tab — one request per selection, "
                + "nothing for the rest."), new HorizontalLayout(plugins, language, apply, all), spikeSlot, report);

        rebuild(DEFAULT_PLUGINS, "de");
    }

    private void rebuild(List<String> plugins, String language) {
        spikeSlot.removeAll();
        report.clear();

        Spike spike = new Spike(plugins, language);
        spike.getElement()
                .addEventListener("spike-report",
                        event -> report.setValue(event.getEventData().getString("event.detail.json")))
                .addEventData("event.detail.json");

        spikeSlot.add(spike);
    }

    @Tag("froala-ondemand-spike")
    @JsModule("./froala-ondemand-spike.js")
    private static class Spike extends Component {

        Spike(List<String> plugins, String language) {
            getElement().setAttribute("plugins",
                    plugins.stream().map(name -> '"' + name + '"').collect(Collectors.joining(",", "[", "]")));
            if (language != null && !language.isEmpty()) {
                getElement().setAttribute("language", language);
            }
        }
    }
}
