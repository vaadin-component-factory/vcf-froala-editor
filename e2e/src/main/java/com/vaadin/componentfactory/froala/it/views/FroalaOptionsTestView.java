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
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for {@code FroalaOptionsIT}, owned by the tests.
 *
 * <p>
 * The toolbar is the assertion channel. Options are only observable once Froala has read them, and a configured toolbar
 * is the cheapest thing to read back out of the DOM. Each button disables itself, so a test can wait for the round trip
 * to have landed before it looks at the client. A click returns when it is dispatched, not when the server has
 * answered.
 */
@Route(FroalaOptionsTestView.ROUTE)
@AnonymousAllowed
public class FroalaOptionsTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala-options";

    public static final String INITIAL_VALUE = "<p>seeded by the server</p>";
    public static final String PLACEHOLDER = "options placeholder under test";

    /** Not Froala's own default toolbar, so no default can pass for a configured one. */
    public static final FroalaOptions INITIAL_OPTIONS = FroalaOptions.defaults().withPlaceholderText(PLACEHOLDER)
            .withToolbarButtons(FroalaToolbar.of("bold", "italic"));

    /** Not 0, the add-on's default, so a test can tell an explicit value from the default. */
    public static final int OTHER_SAVE_INTERVAL = 5000;

    /** A second set, to show that options really are re-read when they change on a running editor. */
    public static final FroalaOptions OTHER_OPTIONS = FroalaOptions.defaults().withPlaceholderText(PLACEHOLDER)
            .withToolbarButtons(FroalaToolbar.of("undo", "redo", "insertLink")).withSaveInterval(OTHER_SAVE_INTERVAL);

    public FroalaOptionsTestView() {
        setSizeFull();

        FroalaEditor editor = new FroalaEditor("Editor under test", INITIAL_OPTIONS);
        editor.setId("editor");
        editor.setHeight("300px");

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.setMinHeight("100px");

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        Span selectionLog = new Span();
        selectionLog.setId("selection-log");
        editor.addSelectionChangeListener(
                event -> selectionLog.setText(selectionLog.getText() + " " + event.hasSelection()));

        Button otherOptions = new Button("Other options");
        otherOptions.addClickListener(event -> {
            editor.setOptions(OTHER_OPTIONS);
            otherOptions.setEnabled(false);
        });
        otherOptions.setId("other-options");

        // Detach, set options, attach again, all in one round trip. It leaves the client destroying an editor that is
        // still bootstrapping, and handlers of the discarded instance must not run against its successor.
        Button reattachWithOptions = new Button("Re-attach with other options");
        reattachWithOptions.addClickListener(event -> {
            int position = indexOf(editor);

            remove(editor);
            editor.setOptions(OTHER_OPTIONS);
            addComponentAtIndex(position, editor);

            reattachWithOptions.setEnabled(false);
        });
        reattachWithOptions.setId("reattach-with-options");

        Button clearOptions = new Button("Clear options");
        clearOptions.addClickListener(event -> {
            editor.setOptions((FroalaOptions) null);
            clearOptions.setEnabled(false);
        });
        clearOptions.setId("clear-options");

        add(otherOptions, reattachWithOptions, clearOptions, selectionLog, editor, viewer);

        // set last, so the value is on the server before the first attach reaches the client
        editor.setValue(INITIAL_VALUE);
    }
}
