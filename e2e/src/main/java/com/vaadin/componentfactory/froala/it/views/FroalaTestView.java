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
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for {@code FroalaEditorIT}, owned by the tests.
 *
 * <p>
 * Deliberately separate from the demo's {@code BasicView}: the demo exists to show the add-on off and its author must
 * stay free to change labels, values and layout without breaking tests. Everything an assertion depends on is a
 * constant here, and every control exists because a test needs to reach server-side API that a browser cannot call.
 *
 * <p>
 * The viewer is the assertion channel — only the server-side value change listener ever writes to it, so text appearing
 * there is proof that a client edit made the full round trip.
 */
@Route(FroalaTestView.ROUTE)
@AnonymousAllowed
public class FroalaTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala";

    public static final String LABEL = "Editor under test";
    public static final String HELPER_TEXT = "Helper text under test";
    public static final String INITIAL_VALUE = "<p>seeded by the server</p>";
    public static final String INITIAL_TEXT = "seeded by the server";

    /**
     * Not a real license key and never will be -- this project runs Froala unlicensed. It only has to be a string the
     * test can find again in Froala's own options, which proves our end of the wiring.
     */
    public static final String LICENSE_KEY = "it-dummy-license-key";

    /** A second one, to show that a key set on a running editor only reaches Froala on the next build. */
    public static final String OTHER_LICENSE_KEY = "it-second-dummy-license-key";

    /** Deliberately not what Froala would produce: an unclosed tag and a block it rewrites. */
    public static final String MESSY_VALUE = "<div>messy <b>markup</div>";
    public static final String MESSY_TEXT = "messy markup";

    /** What the insert buttons put into the editor. Bold, so a test can tell it was inserted as HTML, not as text. */
    public static final String SNIPPET_VALUE = "<strong>inserted by the server</strong>";
    public static final String SNIPPET_TEXT = "inserted by the server";

    /** Clearly above Froala's own 500 ms default, so a test can tell the two apart. */
    public static final int SLOW_TYPING_TIMEOUT = 1500;

    public FroalaTestView() {
        setSizeFull();

        FroalaEditor editor = new FroalaEditor(LABEL);
        editor.setId("editor");
        editor.setHelperText(HELPER_TEXT);
        editor.setLicenseKey(LICENSE_KEY);
        editor.setHeight("300px");

        FroalaViewer viewer = new FroalaViewer();
        viewer.setId("viewer");
        viewer.setMinHeight("100px");

        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));

        Button focus = new Button("Focus", event -> editor.focus());
        focus.setId("focus-button");

        // Froala's own focus and blur are re-dispatched from the host element; this is where they arrive server side
        Span focusLog = new Span();
        focusLog.setId("focus-log");
        editor.addFocusListener(event -> focusLog.setText(focusLog.getText() + " focus"));
        editor.addBlurListener(event -> focusLog.setText(focusLog.getText() + " blur"));

        Span selectionLog = new Span();
        selectionLog.setId("selection-log");
        editor.addSelectionChangeListener(
                event -> selectionLog.setText(selectionLog.getText() + " " + event.hasSelection()));

        Button otherLicenseKey = new Button("Other license key");
        otherLicenseKey.addClickListener(event -> {
            editor.setLicenseKey(OTHER_LICENSE_KEY);
            otherLicenseKey.setEnabled(false);
        });
        otherLicenseKey.setId("other-license-key");

        Select<ValueChangeMode> valueChangeMode = new Select<>("Value change mode",
                event -> editor.setValueChangeMode(event.getValue()));
        valueChangeMode.setItems(ValueChangeMode.values());
        // the enum name verbatim, so a test never depends on a label formatter
        valueChangeMode.setItemLabelGenerator(ValueChangeMode::name);
        valueChangeMode.setValue(editor.getValueChangeMode());
        valueChangeMode.setId("value-change-mode");

        Checkbox readOnly = new Checkbox("Read-only", event -> editor.setReadOnly(event.getValue()));
        readOnly.setId("readonly-toggle");

        Checkbox enabled = new Checkbox("Enabled", event -> editor.setEnabled(event.getValue()));
        enabled.setValue(editor.isEnabled());
        enabled.setId("enabled-toggle");

        // disables itself, so a test can tell that the round trip has landed before it reads the client -- a click
        // returns as soon as it is dispatched, not when the server has answered
        Button slowTyping = new Button("Slow typing timer");
        slowTyping.addClickListener(event -> {
            editor.setValueChangeTimeout(SLOW_TYPING_TIMEOUT);
            slowTyping.setEnabled(false);
        });
        slowTyping.setId("slow-typing");

        Button resetValue = new Button("Reset value", event -> editor.setValue(INITIAL_VALUE));
        resetValue.setId("reset-value");

        Button messyValue = new Button("Set messy value", event -> editor.setValue(MESSY_VALUE));
        messyValue.setId("messy-value");

        Button toggleAttached = new Button("Toggle attached", event -> {
            if (editor.getParent().isPresent()) {
                remove(editor);
            } else {
                addComponentAtIndex(1, editor);
            }
        });
        toggleAttached.setId("attach-toggle");

        Button insertSnippet = new Button("Insert snippet", event -> editor.replaceSelectionContent(SNIPPET_VALUE));
        insertSnippet.setId("insert-snippet");

        // Detach, attach and insert in one round trip: the insert then reaches a client editor Froala is still building
        Button reattachAndInsert = new Button("Re-attach and insert", event -> {
            remove(editor);
            addComponentAtIndex(1, editor);
            editor.replaceSelectionContent(SNIPPET_VALUE);
        });
        reattachAndInsert.setId("reattach-and-insert");

        add(focus, focusLog, selectionLog, valueChangeMode, readOnly, enabled, slowTyping, resetValue, messyValue,
                otherLicenseKey, toggleAttached, insertSnippet, reattachAndInsert, editor, viewer);

        // set last, so the value is on the server before the first attach reaches the client
        editor.setValue(INITIAL_VALUE);
    }
}
