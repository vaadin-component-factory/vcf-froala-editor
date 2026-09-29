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

import com.vaadin.componentfactory.froala.FroalaCommand;
import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;

/**
 * Fixture view for own commands with a popover, see {@code FroalaCommandPopoverIT}. The first editor has the command's
 * button in its toolbar, the second one has the command without a button. Both popovers close neither on an outside
 * click nor on Escape, so only a click on the button closes them.
 */
@Route(FroalaCommandPopoverTestView.ROUTE)
@AnonymousAllowed
public class FroalaCommandPopoverTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-command-popover";

    public FroalaCommandPopoverTestView() {
        FroalaEditor first = editor("first", FroalaToolbar.of("bold", "info"));
        Popover popover = popover("popover");
        Registration registration = first.addCommand(info(), popover);

        FroalaEditor noButton = editor("no-button", FroalaToolbar.of("bold"));
        noButton.addCommand(info(), popover("no-button-popover"));

        // other options, so that the editor is built again, and the button moves
        Button rebuild = new Button("Rebuild", event -> first
                .setOptions(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.of("info", "bold"))));
        rebuild.setId("rebuild");
        rebuild.addClickListener(event -> rebuild.setEnabled(false));

        Button remove = new Button("Remove");
        remove.setId("remove");
        remove.addClickListener(event -> {
            registration.remove();
            remove.setEnabled(false);
        });

        // a new element in the browser, which knows nothing of the popover
        Button reattach = new Button("Re-attach");
        reattach.setId("reattach");
        reattach.addClickListener(event -> {
            remove(first);
            addComponentAtIndex(3, first);
            reattach.setEnabled(false);
        });

        add(rebuild, remove, reattach, first, noButton);
    }

    // A new command per call, because the icon is a component and belongs to one editor
    private static FroalaCommand info() {
        return new FroalaCommand("info", "Info", VaadinIcon.INFO_CIRCLE.create()).withShortcut(Key.KEY_K,
                KeyModifier.SHIFT);
    }

    private static Popover popover(String id) {
        Popover popover = new Popover(new Paragraph("Content of " + id));
        popover.setId(id);
        popover.setCloseOnOutsideClick(false);
        popover.setCloseOnEsc(false);
        return popover;
    }

    private static FroalaEditor editor(String id, FroalaToolbar toolbar) {
        FroalaEditor editor = new FroalaEditor(id, "<p>Some text</p>");
        editor.setOptions(FroalaOptions.defaults().withToolbarButtons(toolbar));
        editor.setId(id);
        return editor;
    }
}
