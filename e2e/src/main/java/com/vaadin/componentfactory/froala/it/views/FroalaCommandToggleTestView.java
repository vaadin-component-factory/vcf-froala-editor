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
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for toggle commands, see {@code FroalaCommandToggleIT}. Both editors have the same toggle command, and a
 * click on its button switches the state of that editor. The log shows each switch, so a test can wait for the round
 * trip.
 */
@Route(FroalaCommandToggleTestView.ROUTE)
@AnonymousAllowed
public class FroalaCommandToggleTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-command-toggle";

    private final Span log = new Span();

    public FroalaCommandToggleTestView() {
        log.setId("log");

        FroalaEditor first = editor("first");
        FroalaEditor second = editor("second");

        // other options, so that the editor is built again
        Button rebuild = new Button("Rebuild");
        rebuild.setId("rebuild");
        rebuild.addClickListener(event -> {
            first.setOptions(options(FroalaToolbar.of("review", "bold")));
            rebuild.setEnabled(false);
        });

        // a new element in the browser, which starts out without the state
        Button reattach = new Button("Re-attach");
        reattach.setId("reattach");
        reattach.addClickListener(event -> {
            int index = indexOf(first);
            remove(first);
            addComponentAtIndex(index, first);
            reattach.setEnabled(false);
        });

        add(log, rebuild, reattach, first, second);
    }

    private FroalaEditor editor(String id) {
        FroalaEditor editor = new FroalaEditor(id, "<p>Text with <a href=\"https://example.com\">a link</a></p>");
        editor.setOptions(options(FroalaToolbar.of("bold", "review")));
        editor.setId(id);

        // a new command per editor, because the icon is a component and belongs to one editor
        FroalaCommand review = new FroalaCommand("review", "Review", VaadinIcon.EYE.create()).withToggle();
        editor.addCommand(review, event -> {
            editor.setCommandActive(review, !editor.isCommandActive(review));
            log.setText(log.getText() + " " + id + "=" + editor.isCommandActive(review));
        });
        return editor;
    }

    private static FroalaOptions options(FroalaToolbar toolbar) {
        return FroalaOptions.defaults().withToolbarButtons(toolbar).withLinkEditButtons(List.of("linkOpen", "review"));
    }
}
