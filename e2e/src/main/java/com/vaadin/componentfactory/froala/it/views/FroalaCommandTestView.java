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

import java.util.List;

import com.vaadin.componentfactory.froala.FroalaCommand;
import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;

/**
 * Fixture view for the application's own commands, see {@code FroalaCommandIT}. Two editors add the same command before
 * they are attached, a third one gets it only after, and a fourth has it without a toolbar button. The log names the
 * editor whose listener ran.
 */
@Route(FroalaCommandTestView.ROUTE)
@AnonymousAllowed
public class FroalaCommandTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-command";

    private static final FroalaOptions OPTIONS = FroalaOptions.defaults()
            .withToolbarButtons(FroalaToolbar.of("bold", "stamp")).withLinkEditButtons(List.of("linkOpen", "stamp"));

    // A title that breaks the HTML Froala builds from it, unless it is escaped. Not static, because the icon is a
    // component and belongs to one UI.
    private final FroalaCommand stamp = new FroalaCommand("stamp", "Stamp & \"seal\"", VaadinIcon.STAR.create())
            .withShortcut(Key.KEY_K, KeyModifier.SHIFT);

    private final Span log = new Span();

    public FroalaCommandTestView() {
        log.setId("log");

        FroalaEditor first = editor("first");
        Registration firstStamp = first.addCommand(stamp, event -> logTrigger("first"));
        FroalaEditor second = editor("second");
        second.addCommand(stamp, event -> logTrigger("second"));
        FroalaEditor late = editor("late");
        // Froala runs a shortcut through the toolbar's button when there is one, and differently when there is none
        FroalaEditor noButton = editor("no-button");
        noButton.setOptions(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.of("bold")));
        noButton.addCommand(stamp, event -> logTrigger("no-button"));

        Button removeFromFirst = new Button("Remove from first");
        removeFromFirst.setId("remove-from-first");
        removeFromFirst.addClickListener(event -> {
            firstStamp.remove();
            removeFromFirst.setEnabled(false);
        });

        Button addToLate = new Button("Add to late");
        addToLate.setId("add-to-late");
        addToLate.addClickListener(event -> {
            late.addCommand(stamp, commandEvent -> logTrigger("late"));
            addToLate.setEnabled(false);
        });

        add(log, removeFromFirst, addToLate, first, second, late, noButton);
    }

    private static FroalaEditor editor(String id) {
        FroalaEditor editor = new FroalaEditor(id, "<p>Text with <a href=\"https://example.com\">a link</a></p>");
        editor.setOptions(OPTIONS);
        editor.setId(id);
        return editor;
    }

    private void logTrigger(String editor) {
        log.setText(log.getText().isEmpty() ? editor : log.getText() + " " + editor);
    }
}
