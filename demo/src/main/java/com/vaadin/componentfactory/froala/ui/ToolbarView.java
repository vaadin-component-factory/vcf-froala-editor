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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaToolbarAlign;
import com.vaadin.componentfactory.froala.FroalaToolbarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * Compose Froala's toolbar out of the buttons it already ships, in either of the two shapes it understands, then
 * rebuild the editor with it. Custom buttons are a later phase; nothing here registers one.
 * <p>
 * The two shapes are not the same toolbar with different syntax. A flat list always shows every button it names. Named
 * groups get an overflow panel, and with it the two settings that only exist there: which end of the toolbar the group
 * sits at, and how many of its buttons are shown before the rest collapse into the panel.
 */
@Route("toolbar")
@Menu(title = "Toolbar", order = 4)
public class ToolbarView extends VerticalLayout {

    private static final String INITIAL_VALUE = "<p><em>Moi,&nbsp;</em><strong><span style=\"color: rgb(44, 130, 201);\"><em>Vaadin</em> }&gt;&nbsp;<span class=\"fr-emoticon fr-deletable fr-emoticon-img\" style=\"background: url(https://cdnjs.cloudflare.com/ajax/libs/emojione/2.0.1/assets/svg/1f601.svg);\">&nbsp;</span></span></strong></p>";

    private static final String FLAT = "Flat list";
    private static final String GROUPED = "Named groups";

    /**
     * Froala's own default groups, minus the buttons that need a service behind them -- AI, collaboration, Filestack,
     * the spell checker and the Word conversion. A button whose plugin is not enabled is dropped without a word, so
     * offering them here would only produce toolbars with holes in them.
     */
    private static final Map<String, List<String>> PALETTE = new LinkedHashMap<>();

    static {
        PALETTE.put(FroalaToolbarGroup.MORE_TEXT,
                List.of("bold", "italic", "underline", "strikeThrough", "subscript", "superscript", "fontFamily",
                        "fontSize", "textColor", "backgroundColor", "inlineClass", "inlineStyle", "clearFormatting"));
        PALETTE.put(FroalaToolbarGroup.MORE_PARAGRAPH,
                List.of("alignLeft", "alignCenter", "alignRight", "alignJustify", "formatOL", "formatUL",
                        "paragraphFormat", "paragraphStyle", "lineHeight", "outdent", "indent", "quote"));
        PALETTE.put(FroalaToolbarGroup.MORE_RICH,
                List.of("insertLink", "insertImage", "insertVideo", "insertTable", "insertFile", "emoticons",
                        "fontAwesome", "specialCharacters", "insertHR", "insertAnchor", "pageBreak", "codeSnippet",
                        "markdown"));
        PALETTE.put(FroalaToolbarGroup.MORE_MISC,
                List.of("undo", "redo", "fullscreen", "print", "selectAll", "html", "help", "findReplaceButton"));
    }

    private final RadioButtonGroup<String> shape = new RadioButtonGroup<>();
    private final MultiSelectComboBox<String> flatButtons = new MultiSelectComboBox<>("Buttons");
    private final List<GroupForm> groupForms = new ArrayList<>();
    private final VerticalLayout groupedForm = new VerticalLayout();

    private final TextArea toolbarJson = new TextArea("What the editor is built with");
    private final FroalaEditor editor = new FroalaEditor("Editor");

    public ToolbarView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        shape.setLabel("Toolbar shape");
        shape.setItems(FLAT, GROUPED);
        shape.setValue(GROUPED);
        shape.addValueChangeListener(_unused -> showFormForShape());

        flatButtons.setItems(PALETTE.values().stream().flatMap(List::stream).toList());
        flatButtons.setValue("bold", "italic", "underline", "insertLink", "undo", "redo");
        flatButtons.setWidthFull();
        flatButtons.setHelperText("In the palette's order, not the order you pick them. The API also takes \"|\" and "
                + "\"-\" as separators; they are left out here because a multi-select cannot say where they go.");

        groupedForm.setPadding(false);
        groupedForm.setWidthFull();
        PALETTE.forEach((name, buttons) -> {
            GroupForm group = new GroupForm(name, buttons);

            groupForms.add(group);
            groupedForm.add(group);
        });

        Button reload = new Button("Reload editor", VaadinIcon.REFRESH.create(), _unused -> reload());
        reload.setId("reload-editor");

        toolbarJson.setReadOnly(true);
        toolbarJson.setSizeFull();
        toolbarJson.setMaxHeight("10em");

        editor.setMinHeight("300px");
        editor.setValue(INITIAL_VALUE);
        setFlexGrow(1, editor);

        showFormForShape();

        add(new Paragraph("A group's name is also the command name of its overflow button, which is why the four "
                + "groups below carry Froala's own names: those are the only ones it has a button for."),
                new Details("Toolbar Options", shape, flatButtons, groupedForm, reload),
                new Details("JSON", toolbarJson), editor);

        // After add(), so the editor has a position to be put back at. Without this the form says "named groups" while
        // the editor still runs on Froala's stock toolbar, which is a different toolbar than the one it describes.
        reload();
    }

    private void showFormForShape() {
        flatButtons.setVisible(FLAT.equals(shape.getValue()));
        groupedForm.setVisible(GROUPED.equals(shape.getValue()));
    }

    /** The same rebuild as the options view: detach, set, attach. See {@link OptionsView#reload()}. */
    private void reload() {
        FroalaToolbar toolbar = buildToolbar();
        int position = indexOf(editor);

        remove(editor);
        // every plugin that needs no service, so that nearly every button of the palette has the plugin it needs
        editor.setOptions(FroalaOptions.defaults().withPluginsEnabled(DemoPlugins.full()).withToolbarButtons(toolbar));
        addComponentAtIndex(position, editor);

        toolbarJson.setValue(toolbar.toString());
    }

    private FroalaToolbar buildToolbar() {
        if (FLAT.equals(shape.getValue())) {
            return FroalaToolbar.of(inPaletteOrder(flatButtons.getValue()).toArray(String[]::new));
        }

        return FroalaToolbar
                .ofGroups(groupForms.stream().filter(GroupForm::hasButtons).map(GroupForm::toGroup).toList());
    }

    /**
     * A multi-select answers a set, and a set has no order the user could have meant. The palette's own order is the
     * one thing that is stable between two picks, so that is what the toolbar is built in.
     */
    private static List<String> inPaletteOrder(Set<String> selected) {
        return PALETTE.values().stream().flatMap(List::stream).filter(selected::contains).toList();
    }

    /** One group's three controls: its buttons, which end of the toolbar it sits at, and its overflow count. */
    private static class GroupForm extends HorizontalLayout {

        private final String name;
        private final MultiSelectComboBox<String> buttons;
        private final Select<FroalaToolbarAlign> align = new Select<>();
        private final IntegerField buttonsVisible = new IntegerField("Buttons visible");

        GroupForm(String name, List<String> palette) {
            this.name = name;

            buttons = new MultiSelectComboBox<>(name);
            buttons.setItems(palette);
            buttons.setValue(palette.subList(0, Math.min(4, palette.size())));
            buttons.setWidth("30em");

            align.setLabel("Align");
            align.setItems(FroalaToolbarAlign.values());
            align.setEmptySelectionAllowed(true);
            align.setEmptySelectionCaption("Froala's default (left)");
            align.setWidth("14em");

            buttonsVisible.setHelperText("Empty for Froala's default (3)");
            buttonsVisible.setWidth("12em");

            setAlignItems(Alignment.BASELINE);
            add(buttons, align, buttonsVisible);
        }

        boolean hasButtons() {
            return !buttons.getValue().isEmpty();
        }

        FroalaToolbarGroup toGroup() {
            FroalaToolbarGroup group = FroalaToolbarGroup
                    .named(name, inPaletteOrder(buttons.getValue()).toArray(String[]::new)).withAlign(align.getValue());

            return buttonsVisible.getValue() == null ? group : group.withButtonsVisible(buttonsVisible.getValue());
        }
    }
}
