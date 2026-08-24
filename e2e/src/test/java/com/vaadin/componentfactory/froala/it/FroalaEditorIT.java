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
package com.vaadin.componentfactory.froala.it;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end test driving the real Froala editor in a browser. Named *IT so failsafe runs it in the
 * integration-test/verify phase of the `production` profile, against the optimized frontend bundle.
 *
 * <p>
 * This is the only layer that proves the delta channel works, because Karibu runs no JavaScript. The demo view echoes
 * every value change into a {@code vcf-froala-viewer}, so the viewer's text is the evidence that the typed HTML made
 * the round trip through diff-match-patch and back into the server-side value.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaEditorIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaTestView.ROUTE;
    }

    /** Froala's own editable surface, inside our web component's light DOM. */
    private Locator editableArea() {
        return page.locator("#editor .fr-element[contenteditable='true']");
    }

    @Test
    void typedText_reachesTheServer() {
        editableArea().click();
        editableArea().type("Hello from Playwright");

        // ON_CHANGE is the default mode, so Froala's contentChanged event drives the sync; the viewer only ever gets
        // text that has been through the server
        assertThat(page.locator("vcf-froala-viewer")).containsText("Hello from Playwright");
    }

    @Test
    void initialValue_isInTheEditorOnLoad() {
        // The view sets this value server side before the first attach, so it can only have arrived through the
        // innerHTML seeding in _initEditor -- Froala has no init option for its content.
        assertThat(editableArea()).containsText(FroalaTestView.INITIAL_TEXT);
    }

    @Test
    void typing_neverPushesTheFullValueBackToTheClient() {
        // The point of the whole delta design, and the customer's actual requirement: an NST document can be large, so
        // a keystroke must not put it back on the wire. The server pushes a full value by writing the element's `value`
        // property, so counting those writes measures exactly that. A regression to setPresentationValue on every
        // change would pass every other test in this suite.
        page.locator("#editor .fr-element").waitFor();
        page.evaluate("""
                () => {
                    const el = document.querySelector('#editor');
                    const descriptor = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el), 'value');
                    window.__fullValueWrites = 0;
                    Object.defineProperty(el, 'value', {
                        configurable: true,
                        get: () => descriptor.get.call(el),
                        set: (newValue) => {
                            window.__fullValueWrites++;
                            descriptor.set.call(el, newValue);
                        }
                    });
                }
                """);

        editableArea().click();
        editableArea().type("a sentence long enough to produce several change events");

        // the round trip has to have happened, otherwise a count of zero would prove nothing
        assertThat(page.locator("#viewer")).containsText("several change events");
        assertEquals(0, page.evaluate("() => window.__fullValueWrites"));
    }

    @Test
    void licenseKey_arrivesInFroalasOwnOptions() {
        // Whether the key is valid is Froala's business -- this project runs unlicensed on purpose. What is ours is
        // that the key the server set is the key Froala was constructed with, which is only observable here: it is read
        // once, at init, and Karibu cannot see past the element property.
        page.locator("#editor .fr-element").waitFor();

        assertEquals(FroalaTestView.LICENSE_KEY,
                page.evaluate("() => document.querySelector('#editor').editor.opts.key"));
    }

    @Test
    void setValue_reachesTheClientEvenWhenItRepeatsTheLastServerValue() {
        editableArea().click();
        editableArea().type(" typed on top");
        page.locator("#viewer").click();
        assertThat(page.locator("#viewer")).containsText("typed on top");

        // The button sets exactly the value the server pushed at load. The client still believes it holds that value
        // -- client edits travel as deltas and never touch the property -- so a plain property update would be
        // dropped on the way out and the typed text would stay on screen.
        page.locator("#reset-value").click();

        assertThat(editableArea()).containsText(FroalaTestView.INITIAL_TEXT);
        assertThat(editableArea()).not().containsText("typed on top");
    }

    @Test
    void editorRendersWithToolbar() {
        assertThat(editableArea()).isVisible();
        assertThat(page.locator("vcf-froala-editor .fr-toolbar")).isVisible();
    }

    /**
     * Smoke check only: this exercises Vaadin's own FieldMixin slots, not the Froala integration — it would pass
     * unchanged if the whole editor were removed. Kept because it is nearly free, but it is not Froala coverage.
     */
    @Test
    void labelAndHelperText_areRendered() {
        assertThat(page.locator("vcf-froala-editor")).containsText(FroalaTestView.LABEL);
        assertThat(page.locator("vcf-froala-editor")).containsText(FroalaTestView.HELPER_TEXT);
    }

    @Test
    void focusButton_movesFocusIntoTheEditor() {
        page.locator("#focus-button").click();

        assertThat(editableArea()).isFocused();
    }

    @Test
    void fastTyping_thenBlur_losesNothing() {
        // The connector throttles value syncs to one per 50 ms. Typing quickly produces several Froala contentChanged
        // events inside one window, and the blur flush that follows can land inside it too. Dropping instead of
        // deferring used to lose whatever the last suppressed sync carried -- this is that regression.
        editableArea().click();
        editableArea().type("abcdefghijklmnopqrstuvwxyz");
        page.locator("#viewer").click();

        assertThat(page.locator("#viewer")).containsText("abcdefghijklmnopqrstuvwxyz");
    }

    @Test
    void syncInsideTheThrottleWindow_isDeferredNotDropped() {
        // Deliberately synthesized rather than typed: the 50 ms window cannot be hit reliably from the outside. The
        // call goes through the connector's real onValueChange, only the trigger is artificial. Without the deferral
        // the second value never reaches the server, because no further change follows to carry it.
        page.locator("#editor .fr-element").waitFor();
        page.evaluate("""
                () => {
                    const el = document.querySelector('#editor');
                    el.editor.html.set('<p>first</p>');
                    el.onValueChange();
                    el.editor.html.set('<p>second</p>');
                    el.onValueChange();
                }
                """);

        assertThat(page.locator("#viewer")).containsText("second");
    }

    @Test
    void onBlurMode_syncsOnlyWhenFocusLeaves() {
        selectValueChangeMode("ON_BLUR");

        editableArea().click();
        editableArea().type("only after blur");

        // still in the editor, so nothing may have reached the server yet
        assertThat(page.locator("#viewer")).not().containsText("only after blur");

        page.locator("#focus-button").click();
        page.locator("#viewer").click();

        assertThat(page.locator("#viewer")).containsText("only after blur");
    }

    @Test
    void detachAndReattach_keepsTheValueAndKeepsWorking() {
        editableArea().click();
        editableArea().type("survives detach");
        page.locator("#viewer").click();
        assertThat(page.locator("#viewer")).containsText("survives detach");

        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();

        // the server pushes the accumulated value on detach, so the rebuilt editor has to come back with the text
        assertThat(editableArea()).containsText("survives detach");

        // and it must still be a live editor, not a corpse
        editableArea().click();
        editableArea().type(" and still edits");
        page.locator("#viewer").click();

        assertThat(page.locator("#viewer")).containsText("survives detach and still edits");
    }

    @Test
    void driftedClient_recoversThroughResync() {
        editableArea().click();
        editableArea().type("base text");
        page.locator("#viewer").click();
        assertThat(page.locator("#viewer")).containsText("base text");

        // Corrupt the client's idea of what the server holds. Every delta built from here on is against a base the
        // server does not have, so applyDelta must reject it and ask for a full resync instead of silently diverging.
        page.evaluate("() => { document.querySelector('#editor')._lastSyncedValue = '<p>totally unrelated</p>'; }");

        editableArea().click();
        editableArea().type(" plus more");
        page.locator("#viewer").click();

        assertThat(page.locator("#viewer")).containsText("base text plus more");
    }

    @Test
    void readOnly_stopsEditing() {
        page.locator("#readonly-toggle input").check();

        assertThat(page.locator("#editor .fr-element")).hasAttribute("contenteditable", "false");
    }

    @Test
    void disabled_stopsEditing() {
        page.locator("#enabled-toggle input").uncheck();

        assertThat(page.locator("#editor .fr-element")).hasAttribute("contenteditable", "false");
    }

    private void selectValueChangeMode(String label) {
        page.locator("#value-change-mode").click();
        page.locator("vaadin-select-overlay vaadin-select-item").filter(new Locator.FilterOptions().setHasText(label))
                .first().click();
    }
}
