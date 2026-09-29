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

import java.util.Set;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaTextDirection;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Configure a handful of the options people actually set, then rebuild the editor with them.
 * <p>
 * Froala reads its options once, when it is initialized, so every switch flipped here builds the editor again. What
 * that rebuild does <em>not</em> cost is the content: the value is held on the server and put back into the new editor.
 */
@Route("options")
@Menu(title = "Options", order = 3)
public class OptionsView extends VerticalLayout {

    private final TextField placeholderText = new TextField("Placeholder");
    private final Select<FroalaTextDirection> direction = new Select<>();
    private final Checkbox toolbarInline = new Checkbox("Inline toolbar (on selection)");
    private final Checkbox documentReady = new Checkbox("Document layout (page with margins)");
    private final Checkbox toolbarSticky = new Checkbox("Sticky toolbar", true);
    private final Checkbox toolbarBottom = new Checkbox("Toolbar below the text");
    private final Checkbox charCounterCount = new Checkbox("Character counter", true);
    private final IntegerField charCounterMax = new IntegerField("Max characters");
    private final Checkbox wordCounterCount = new Checkbox("Word counter");
    private final IntegerField wordCounterMax = new IntegerField("Max words");

    private final TextArea optionsJson = new TextArea("What the editor is built with");
    private final FroalaEditor editor = new FroalaEditor("Editor");

    public OptionsView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        direction.setLabel("Text direction");
        direction.setItems(FroalaTextDirection.values());
        direction.setEmptySelectionAllowed(true);
        direction.setEmptySelectionCaption("Froala's default");

        charCounterMax.setHelperText("Empty for no limit");
        wordCounterMax.setHelperText("Empty for no limit");

        FormLayout form = new FormLayout(placeholderText, direction, toolbarInline, documentReady, toolbarSticky,
                toolbarBottom, charCounterCount, charCounterMax, wordCounterCount, wordCounterMax);

        form.getChildren().map(HasValue.class::cast)
                .forEach(field -> field.addValueChangeListener(_unused -> reload()));

        optionsJson.setReadOnly(true);

        editor.setMinHeight("400px");
        setFlexGrow(1, editor);

        add(new Paragraph(
                "Pick options. Only what you set is passed on; everything else stays at " + "Froala's own default."),
                new Details("Sample Options", form), new Details("JSON", optionsJson), editor);

        // After add(), so the editor has a position to be put back at. Without this the form and the JSON box describe
        // options the editor was never built with -- two of the switches start out on.
        reload();
    }

    /**
     * Rebuilds the editor with the options the form describes. Detaching and re-attaching is a harder reset than
     * {@code setOptions} needs -- it already rebuilds the editor on its own -- and it is the sequence an application
     * would use to swap an editor out entirely, which is what this demo shows.
     */
    private void reload() {
        FroalaOptions options = buildOptions();
        int position = indexOf(editor);

        remove(editor);
        editor.setOptions(options);
        addComponentAtIndex(position, editor);

        showOptions(options);
    }

    private FroalaOptions buildOptions() {
        // the counters are not among the basics, and their options do nothing without their plugins
        Set<String> plugins = FroalaPlugin.basics();
        plugins.add(FroalaPlugin.CHAR_COUNTER);
        plugins.add(FroalaPlugin.WORD_COUNTER);

        FroalaOptions options = FroalaOptions.defaults().withPluginsEnabled(plugins)
                .withPlaceholderText(emptyToNull(placeholderText.getValue())).withDirection(direction.getValue())
                .withToolbarInline(toolbarInline.getValue()).withDocumentReady(documentReady.getValue())
                .withToolbarSticky(toolbarSticky.getValue()).withToolbarBottom(toolbarBottom.getValue())
                .withCharCounterCount(charCounterCount.getValue()).withWordCounterCount(wordCounterCount.getValue());

        if (charCounterMax.getValue() != null) {
            options = options.withCharCounterMax(charCounterMax.getValue());
        }
        if (wordCounterMax.getValue() != null) {
            options = options.withWordCounterMax(wordCounterMax.getValue());
        }

        return options;
    }

    private void showOptions(FroalaOptions options) {
        optionsJson.setValue(options.toString());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
