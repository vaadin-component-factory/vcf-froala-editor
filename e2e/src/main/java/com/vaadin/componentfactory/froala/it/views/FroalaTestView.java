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
import com.vaadin.componentfactory.froala.FroalaValueChangeMode;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
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
 * Everything an assertion depends on is a constant here, and every control exists because a test needs to reach
 * server-side API that a browser cannot call.
 *
 * <p>
 * The viewer is the assertion channel. Only the server-side value change listener ever writes to it, so text appearing
 * there is proof that a client edit made the full round trip.
 */
@Route(FroalaTestView.ROUTE)
@AnonymousAllowed
public class FroalaTestView extends VerticalLayout {

    public static final String ROUTE = "it/froala";

    public static final String LABEL = "Editor under test";
    public static final String HELPER_TEXT = "Helper text under test";
    public static final String OTHER_LABEL = "Relabelled editor";
    public static final String ERROR_MESSAGE = "Error message under test";
    public static final String INITIAL_VALUE = "<p>seeded by the server</p>";
    public static final String INITIAL_TEXT = "seeded by the server";

    /**
     * Not a real license key and never will be, because this project runs Froala unlicensed. It only has to be a string
     * the test can find again in Froala's own options, which proves our end of the wiring.
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

    /** An emoji, whose UTF-16 surrogate pair an edit can split. A neighbouring emoji shares its first half. */
    public static final String EMOJI_VALUE = "<p>\uD83D\uDE00</p>";

    /**
     * Runs script through an event handler if it reaches the page as markup without Froala's cleaning. Not in the
     * viewer, which shows every value unsanitized by design.
     */
    public static final String HOSTILE_VALUE = "<p>hostile<img src=\"x\" onerror=\"if (!this.closest('#viewer')) window.__xss = true\"></p>";

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

        // Froala's own focus and blur are re-dispatched from the host element. This is where they arrive server side.
        Span focusLog = new Span();
        focusLog.setId("focus-log");
        editor.addFocusListener(event -> focusLog.setText(focusLog.getText() + " focus"));
        editor.addBlurListener(event -> focusLog.setText(focusLog.getText() + " blur"));

        // Alt+B blurs from the server without the keyboard focus leaving the editor first, as a button click would
        Shortcuts.addShortcutListener(this, editor::blur, Key.KEY_B, KeyModifier.ALT).listenOn(editor);

        Span inputLog = new Span();
        inputLog.setId("input-log");
        editor.addInputListener(event -> inputLog.setText("input"));

        Button untabbable = new Button("Untabbable");
        untabbable.addClickListener(event -> {
            editor.setTabIndex(-1);
            untabbable.setEnabled(false);
        });
        untabbable.setId("untabbable");

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

        Select<FroalaValueChangeMode> valueChangeMode = new Select<>("Value change mode",
                event -> editor.setValueChangeMode(event.getValue()));
        valueChangeMode.setItems(FroalaValueChangeMode.values());
        // the enum name verbatim, so a test never depends on a label formatter
        valueChangeMode.setItemLabelGenerator(FroalaValueChangeMode::name);
        valueChangeMode.setValue(editor.getValueChangeMode());
        valueChangeMode.setId("value-change-mode");

        Checkbox readOnly = new Checkbox("Read-only", event -> editor.setReadOnly(event.getValue()));
        readOnly.setId("readonly-toggle");

        Checkbox enabled = new Checkbox("Enabled", event -> editor.setEnabled(event.getValue()));
        enabled.setValue(editor.isEnabled());
        enabled.setId("enabled-toggle");

        // The button disables itself, so a test can tell that the round trip has landed before it reads the client.
        // A click returns as soon as it is dispatched, not when the server has answered.
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

        // Disables itself, so a test can tell the call has reached the client before it asserts that nothing happened
        Button insertSnippetOnce = new Button("Insert snippet once");
        insertSnippetOnce.addClickListener(event -> {
            editor.replaceSelectionContent(SNIPPET_VALUE);
            insertSnippetOnce.setEnabled(false);
        });
        insertSnippetOnce.setId("insert-snippet-once");

        Button emojiValue = new Button("Set emoji value", event -> editor.setValue(EMOJI_VALUE));
        emojiValue.setId("emoji-value");

        Button hostileValue = new Button("Set hostile value", event -> editor.setValue(HOSTILE_VALUE));
        hostileValue.setId("hostile-value");

        // Detach, attach and insert in one round trip, so the insert reaches a client editor Froala is still building
        Button reattachAndInsert = new Button("Re-attach and insert", event -> {
            remove(editor);
            addComponentAtIndex(1, editor);
            editor.replaceSelectionContent(SNIPPET_VALUE);
        });
        reattachAndInsert.setId("reattach-and-insert");

        Button selectAll = new Button("Select all", event -> editor.selectAll());
        selectAll.setId("select-all");

        // Detach, attach and select all in one round trip, so the call reaches a client editor Froala is still building
        Button reattachAndSelectAll = new Button("Re-attach and select all", event -> {
            remove(editor);
            addComponentAtIndex(1, editor);
            editor.selectAll();
        });
        reattachAndSelectAll.setId("reattach-and-select-all");

        // Label, helper text and error message all change in one round trip, so a test sees one consistent state
        Button otherFieldTexts = new Button("Other field texts");
        otherFieldTexts.addClickListener(event -> {
            editor.setLabel(OTHER_LABEL);
            editor.setHelperText(null);
            editor.setErrorMessage(ERROR_MESSAGE);
            editor.setInvalid(true);
            otherFieldTexts.setEnabled(false);
        });
        otherFieldTexts.setId("other-field-texts");

        // setOptions on an attached editor throws the client editor away and builds a new one
        Button rebuild = new Button("Rebuild");
        rebuild.addClickListener(event -> {
            editor.setOptions(FroalaOptions.defaults());
            rebuild.setEnabled(false);
        });
        rebuild.setId("rebuild");

        add(focus, focusLog, inputLog, untabbable, selectionLog, valueChangeMode, readOnly, enabled, slowTyping,
                resetValue, messyValue, otherLicenseKey, toggleAttached, insertSnippet, insertSnippetOnce, emojiValue,
                hostileValue, reattachAndInsert, selectAll, reattachAndSelectAll, otherFieldTexts, rebuild, editor,
                viewer);

        // set last, so the value is on the server before the first attach reaches the client
        editor.setValue(INITIAL_VALUE);
    }
}
