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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.text.WordUtils;
import org.vaadin.addons.componentfactory.toolbarlayout.ToolbarLayout;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaEditorVariant;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaTextDirection;
import com.vaadin.componentfactory.froala.FroalaTheme;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("")
@Menu(title = "Basics", order = 0)
// Froala's own themes, which the add-on does not load
@CssImport("froala-editor/css/themes/dark.min.css")
@CssImport("froala-editor/css/themes/gray.min.css")
@CssImport("froala-editor/css/themes/royal.min.css")
public class BasicView extends FroalaViewBase {

    public static final String INITIAL_VALUE = "<p>Hello <b>World</b></p>";
    public static final String ALTERNATIVE_VALUE = "<p><em>Moi,&nbsp;</em><strong><span style=\"color: rgb(44, 130, 201);\"><em>Vaadin</em> }&gt;&nbsp;<span class=\"fr-emoticon fr-deletable fr-emoticon-img\" style=\"background: url(https://cdnjs.cloudflare.com/ajax/libs/emojione/2.0.1/assets/svg/1f601.svg);\">&nbsp;</span></span></strong></p>";

    /**
     * Deliberately not well-formed -- it closes an {@code <em>} it never opened and leaves {@code <strong>} and
     * {@code <span>} open -- to show that Froala's HTML cleaning repairs a snippet on the way in.
     */
    public static final String REPLACE_SELECTION_SNIPPET = "Hello, </em><strong><span style=\"color: rgb(44, 130, 201);\"><em>Vaadin</em> }&gt;&nbsp;";

    private HasComponents froalaReattachParent = this;

    public BasicView() {

        FroalaEditor editor = getEditor();

        editor.setHeight("500px");
        // starts without plugin options, so the editor gets FroalaPlugin.basics()
        editor.setOptions(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.basics()));

        addComponentAtIndex(indexOf(getToolbar()) + 1, new Paragraph(
                "An editor without plugin options gets FroalaPlugin.basics(): text and paragraph formats, "
                        + "lists, quotes, links and find and replace. Nothing that inserts images, tables or other "
                        + "content. The toolbar is FroalaToolbar.basics() with all buttons visible."));

        getViewer().setMinHeight("250px");
    }

    @Override
    protected Component createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        ToolbarLayout toolbar = new ToolbarLayout();
        addComponentMenu(toolbar.addItem("Component").getSubMenu(), editor);
        addValueMenu(toolbar.addItem("Value").getSubMenu(), editor);
        addFroalaOptionsMenu(toolbar.addItem("Froala options").getSubMenu(), editor);

        return toolbar;
    }

    private void addComponentMenu(SubMenu menu, FroalaEditor editor) {
        checkable(menu, "Attached", true).addClickListener(_unused -> toggleAttached(editor));
        checkable(menu, "Enabled", editor.isEnabled())
                .addClickListener(event -> editor.setEnabled(event.getSource().isChecked()));
        checkable(menu, "Read-only", editor.isReadOnly())
                .addClickListener(event -> editor.setReadOnly(event.getSource().isChecked()));
        checkable(menu, "Visible", editor.isVisible())
                .addClickListener(event -> editor.setVisible(event.getSource().isChecked()));
        SubMenu variantMenu = menu.addItem("Theme Variant").getSubMenu();
        for (FroalaEditorVariant variant : FroalaEditorVariant.values()) {
            checkable(variantMenu, variant.name(), false).addClickListener(event -> {
                if (event.getSource().isChecked()) {
                    editor.addThemeVariants(variant);
                } else {
                    editor.removeThemeVariants(variant);
                }
            });
        }
        menu.add(new Hr());
        menu.addItem("Focus", _unused -> editor.focus());
    }

    private void addValueMenu(SubMenu menu, FroalaEditor editor) {
        menu.addItem("Change value", _unused -> {
            if (INITIAL_VALUE.equals(editor.getValue())) {
                editor.setValue(ALTERNATIVE_VALUE);
            } else {
                editor.setValue(INITIAL_VALUE);
            }
        });

        MenuItem replaceSelection = menu.addItem("Replace selection",
                _unused -> editor.replaceSelectionContent(REPLACE_SELECTION_SNIPPET));
        replaceSelection.setEnabled(false);
        editor.addSelectionChangeListener(event -> replaceSelection.setEnabled(event.hasSelection()));

        menu.add(new Hr());
        // the underscore has to go first, capitalizeFully only splits on whitespace -- ON_BLUR would read "On_blur"
        choice(menu.addItem("Value change mode").getSubMenu(), List.of(ValueChangeMode.values()),
                editor.getValueChangeMode(), mode -> WordUtils.capitalizeFully(mode.name().replace('_', ' ')),
                editor::setValueChangeMode);
    }

    /**
     * Every item starts at its default, which is Froala's own or, for the theme, the add-on's. So the editor is built
     * with its plugins only until one is clicked. A click then sets all of them at once. {@code setOptions} rebuilds
     * the editor, because Froala cannot change options on a running instance.
     */
    private void addFroalaOptionsMenu(SubMenu menu, FroalaEditor editor) {
        SubMenu directionMenu = menu.addItem("Text direction").getSubMenu();
        SubMenu themeMenu = menu.addItem("Theme").getSubMenu();

        SubMenu toolbarMenu = menu.addItem("Toolbar").getSubMenu();
        MenuItem sticky = checkable(toolbarMenu, "Sticky", true);
        MenuItem inline = checkable(toolbarMenu, "Inline (shown on selection)", false);
        MenuItem bottom = checkable(toolbarMenu, "Below the text", false);

        MenuItem documentReady = checkable(menu, "Document layout", false);
        MenuItem charCounter = checkable(menu, "Character counter", false);
        MenuItem wordCounter = checkable(menu, "Word counter", false);

        AtomicReference<FroalaTextDirection> direction = new AtomicReference<>();
        AtomicReference<String> theme = new AtomicReference<>();
        Runnable apply = () -> {
            // the counters are plugins of their own, not among the basics
            Set<String> plugins = FroalaPlugin.basics();
            if (charCounter.isChecked()) {
                plugins.add(FroalaPlugin.CHAR_COUNTER);
            }
            if (wordCounter.isChecked()) {
                plugins.add(FroalaPlugin.WORD_COUNTER);
            }

            editor.setOptions(FroalaOptions.defaults().withPluginsEnabled(plugins)
                    .withToolbarButtons(FroalaToolbar.basics().withAllButtonsVisible()).withDirection(direction.get())
                    .withTheme(theme.get()).withToolbarSticky(sticky.isChecked()).withToolbarInline(inline.isChecked())
                    .withToolbarBottom(bottom.isChecked()).withDocumentReady(documentReady.isChecked()));
        };

        Stream.of(sticky, inline, bottom, documentReady, charCounter, wordCounter)
                .forEach(item -> item.addClickListener(_unused -> apply.run()));

        choice(directionMenu,
                Arrays.asList(null, FroalaTextDirection.LTR, FroalaTextDirection.RTL, FroalaTextDirection.AUTO), null,
                value -> value == null ? "Froala's default" : value.name(), value -> {
                    direction.set(value);
                    apply.run();
                });

        List<String> themes = Arrays.asList(null, FroalaTheme.VAADIN, FroalaTheme.DARK, FroalaTheme.GRAY,
                FroalaTheme.ROYAL, FroalaTheme.NONE);
        choice(themeMenu, themes, null, BasicView::themeLabel, value -> {
            theme.set(value);
            apply.run();
        });
    }

    private static String themeLabel(String theme) {
        if (theme == null) {
            return "The add-on's default";
        }
        if (theme.isEmpty()) {
            return "None";
        }
        return theme;
    }

    private static MenuItem checkable(SubMenu menu, String text, boolean checked) {
        MenuItem item = menu.addItem(text);
        item.setCheckable(true);
        item.setChecked(checked);

        return item;
    }

    /** Adds one checkable item per value, of which exactly one is checked: a radio group, as far as a menu has one. */
    private static <T> void choice(SubMenu menu, List<T> values, T selected, Function<T, String> label,
            Consumer<T> onChoose) {
        List<MenuItem> items = new ArrayList<>();
        for (T value : values) {
            MenuItem item = checkable(menu, label.apply(value), Objects.equals(value, selected));
            item.addClickListener(_unused -> {
                items.forEach(other -> other.setChecked(other == item));
                onChoose.accept(value);
            });
            items.add(item);
        }
    }

    /**
     * Detaches the editor from this view, or re-attaches it in its original position. Exercises the round trip the
     * delta design depends on: the value the client accumulated has to survive the rebuild.
     */
    private void toggleAttached(FroalaEditor editor) {
        if (editor.getParent().isPresent()) {
            froalaReattachParent = (HasComponents) editor.getParent().get();
            editor.removeFromParent();
        } else {
            froalaReattachParent.addComponentAtIndex(froalaReattachParent.indexOf(getToolbar()) + 1, editor);
        }
    }
}
