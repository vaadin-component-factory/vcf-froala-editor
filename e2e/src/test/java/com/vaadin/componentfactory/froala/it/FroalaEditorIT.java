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

import com.vaadin.componentfactory.froala.FroalaEditor;
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
    void typingThenDetachingImmediately_keepsTheLastChange_inIntervalMode() {
        selectValueChangeMode("INTERVAL");

        assertLastChangeSurvivesAnImmediateDetach("typed in interval mode");
    }

    @Test
    void typingThenDetachingImmediately_keepsTheLastChange() {
        editableArea().click();
        editableArea().type("typed just before detaching");

        // No pause and no click elsewhere first: straight to the toggle. Focus leaving the editor makes Froala fire
        // blur, and that is the last moment anything can be sent -- the detach clears every pending timer on the
        // client, so a change still sitting in one is gone.
        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();

        assertThat(editableArea()).containsText("typed just before detaching");
    }

    @Test
    void valueChangeTimeout_setsFroalasTypingTimer_andGovernsWhenAChangeIsReported() {
        // valueChangeTimeout is not a timer of ours: it is Froala's typingTimer, the debounce that decides when
        // contentChanged fires at all. Both halves are ours to prove -- that the value reaches Froala's options, and
        // that setting it actually moves the sync.
        //
        // Playwright's fake clock, and it has to be runFor, not fastForward: fastForward fires each due timer at most
        // once and never the ones scheduled while it jumps, which is exactly this chain -- Froala's debounce is what
        // triggers our sync.
        withFakeClock();

        // the constructor's default, applied through the init options
        assertEquals(FroalaEditor.DEFAULT_VALUE_CHANGE_TIMEOUT, typingTimer());

        // and a later change reaches a running editor, because Froala reads the option on every keystroke. The
        // button disables itself, which is how the test knows the round trip landed -- clicking only dispatches.
        page.locator("#slow-typing").click();
        assertThat(page.locator("#slow-typing")).isDisabled();
        assertEquals(FroalaTestView.SLOW_TYPING_TIMEOUT, typingTimer());

        // A rebuilt editor has to get it through the init options instead. Worth its own step because the default is
        // Froala's own default as well, so the assertion above proves nothing about how the value got there.
        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();
        page.locator("#editor .fr-element").waitFor();
        assertEquals(FroalaTestView.SLOW_TYPING_TIMEOUT, typingTimer());

        editableArea().click();
        recordDeltaDispatches();
        editableArea().type("slowly");

        // past Froala's own 500 ms default: with the option ignored, this is where the change would have been reported
        page.clock().runFor(800);
        assertEquals(0, dispatchedDeltas(), deltaLog());

        page.clock().runFor(1000);
        assertEquals(1, dispatchedDeltas(), deltaLog());
        assertThat(page.locator("#viewer")).containsText("slowly");
    }

    @Test
    void intervalMode_syncsOnEveryTick_whileTheUserKeepsTyping() {
        withFakeClock();
        selectValueChangeMode("INTERVAL");

        editableArea().click();
        recordDeltaDispatches();
        editableArea().type("first tick");

        // short, because the interval was armed when the mode was selected -- a little before this test's clock zero
        page.clock().runFor(600);
        assertEquals(0, dispatchedDeltas(), deltaLog());

        page.clock().runFor(1600);
        assertThat(page.locator("#viewer")).containsText("first tick");

        // The second tick is what separates INTERVAL from TIMEOUT: the editor never lost focus and the user never
        // paused, yet the next span of text has to arrive on its own as well -- and not before its tick.
        editableArea().type(" and second");
        page.clock().runFor(1000);
        assertEquals(1, dispatchedDeltas(), deltaLog());

        page.clock().runFor(1200);
        assertThat(page.locator("#viewer")).containsText("first tick and second");
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
    void froalasFocusAndBlur_reachFlowSideListeners() {
        // Froala's events fire on its own editing area, which Flow knows nothing about. The connector re-dispatches
        // them from the host element, and this is the only place that can prove they arrive: the log is written by
        // server-side listeners.
        editableArea().click();
        assertThat(page.locator("#focus-log")).containsText("focus");

        page.locator("#viewer").click();
        assertThat(page.locator("#focus-log")).containsText("blur");
    }

    @Test
    void licenseKey_isReadOnceWhenTheEditorIsBuilt() {
        page.locator("#editor .fr-element").waitFor();
        assertEquals(FroalaTestView.LICENSE_KEY, licenseKeyInFroala());

        // Pins the documented limitation of API-11 rather than a behaviour we would want: Froala reads opts.key at
        // init and never again, so a key set on a running editor sits on the element until the next build.
        page.locator("#other-license-key").click();
        assertThat(page.locator("#other-license-key")).isDisabled();
        assertEquals(FroalaTestView.LICENSE_KEY, licenseKeyInFroala());

        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();
        page.locator("#editor .fr-element").waitFor();

        assertEquals(FroalaTestView.OTHER_LICENSE_KEY, licenseKeyInFroala());
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
                    el.onValueChangeThrottled();
                    el.editor.html.set('<p>second</p>');
                    el.onValueChangeThrottled();
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
    void resyncWhileASyncIsPending_carriesWhatTheEditorHoldsNow() {
        editableArea().click();
        editableArea().type("base");
        page.locator("#viewer").click();
        assertThat(page.locator("#viewer")).containsText("base");

        // Synthesized down to the resync call, because the state only exists for 50 ms and a server round trip is
        // slower than that: drift the client so its next delta would be rejected, edit twice so the second sync is
        // still sitting in the throttle, and then make the call the server would make. The resync has to carry that
        // second edit -- it is the only thing left that can deliver it, because it also clears the throttle.
        //
        // Asserted on the events, not on the viewer: the viewer would be rescued by the pending throttle firing a
        // moment later and would prove nothing. The server's own half of this is driftedClient_recoversThroughResync.
        page.evaluate("""
                () => {
                    const el = document.querySelector('#editor');
                    window.__resynced = null;
                    el.addEventListener('_value-resync',
                        (e) => window.__resynced ??= e.detail.value);

                    el._lastSyncedValue = '<p>drifted</p>';
                    el.editor.html.set('<p>base plus one</p>');
                    el.onValueChangeThrottled();

                    el.editor.html.set('<p>base plus one plus two</p>');
                    el.onValueChangeThrottled();

                    window.__deltasAfterResync = 0;
                    el.resyncValue();
                    el.addEventListener('_value-delta', () => window.__deltasAfterResync++);

                    // a server-side setValue lands here, and html.set fires no contentChanged of its own, so a
                    // throttle the resync failed to clear is the only thing that could still send anything
                    el.editor.html.set('<p>set by the server</p>');
                }
                """);

        assertEquals("<p>base plus one plus two</p>", page.evaluate("() => window.__resynced"));

        // well past the 50 ms window
        page.waitForTimeout(300);
        assertEquals(0, ((Number) page.evaluate("() => window.__deltasAfterResync")).intValue());
    }

    @Test
    void setValue_producesNoDeltaNobodyTyped() {
        editableArea().click();
        editableArea().type("typed by the user");

        // blur first, so the click on the button below cannot flush anything (VCM-9) and the count stays clean
        page.locator("#viewer").click();
        assertThat(page.locator("#viewer")).containsText("typed by the user");

        // Then let Froala's pending undo step run out. Typing leaves one scheduled for max(250, typingTimer) ms
        // (VCM-15), and a step that runs after the push below sees markup it has not recorded and fires
        // contentChanged for a change the server made -- which is a real delta, just not the one under test here
        // (VCM-16). Without this wait the test passes or fails on how long the blur round trip took.
        page.waitForTimeout(FroalaEditor.DEFAULT_VALUE_CHANGE_TIMEOUT + 100);

        // Deliberately messy markup, because that is where the risk is: Froala rewrites what it is given, so a value
        // reported back after a server push would differ from what the server sent and arrive as a delta describing a
        // change nobody made. Setting already-normalized HTML would produce an empty delta and prove nothing.
        recordDeltaDispatches();
        page.locator("#messy-value").click();
        assertThat(editableArea()).containsText(FroalaTestView.MESSY_TEXT);

        // It stays quiet because html.set fires no contentChanged of its own (VCM-16), and no undo step is left to
        // find the pushed markup; this asserts nothing else sends one either.
        page.waitForTimeout(300);
        assertEquals(0, dispatchedDeltas(), deltaLog());
    }

    @Test
    void detachingInIntervalMode_leavesNoTimerBehind() {
        withFakeClock();
        selectValueChangeMode("INTERVAL");

        editableArea().click();
        editableArea().type("typed before the detach");

        // Keep a reference and a counter: after the detach the element is out of the DOM, but a leaked interval would
        // still be firing against this object.
        page.evaluate("""
                () => {
                    const el = document.querySelector('#editor');
                    window.__detached = el;
                    window.__ticks = 0;
                    el.addEventListener('_value-delta', () => window.__ticks++);
                }
                """);

        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);

        // The count starts here, because the click blurs the editor and that flushes one delta on the way out
        // (VCM-9). A tick on a detached element would find no editor and compute an empty delta, which dispatches
        // nothing -- so the leak would be invisible. This gives it something to report: anything from here is a leak.
        page.evaluate("""
                () => {
                    window.__ticks = 0;
                    window.__detached.editor = {html: {get: () => '<p>reported by a leaked interval</p>'}};
                }
                """);

        page.clock().runFor(10_000);
        assertEquals(0, ((Number) page.evaluate("() => window.__ticks")).intValue());
    }

    @Test
    void initEditorOnAHostThatIsAlreadyDetached_buildsNothing() {
        page.locator("#editor .fr-element").waitFor();

        // The state the isConnected guard exists for: a host that has already had its one and only
        // disconnectedCallback. Anything built there would never be destroyed and would stay in Froala's global
        // registry, which the live editors reach into from their window handlers (LC-6).
        //
        // Attached and removed inside one task first, so this is that host and not merely a loose element. Lit does
        // complete an update on it -- hasUpdated turns true -- but no editor comes out of that path even with the
        // guard removed, so what is asserted here is the guard itself, reached the way the connector reaches it.
        page.evaluate("""
                async () => {
                    const orphan = document.createElement('vcf-froala-editor');
                    document.body.appendChild(orphan);
                    orphan.remove();

                    await orphan._initEditor();

                    window.__orphan = {
                        editor: orphan.editor !== undefined,
                        editable: orphan.querySelector('.fr-element') !== null
                    };
                }
                """);

        assertEquals(Boolean.FALSE, page.evaluate("() => window.__orphan.editor"));
        assertEquals(Boolean.FALSE, page.evaluate("() => window.__orphan.editable"));
    }

    @Test
    void buildingTheEditorASecondTime_changesNothing() {
        page.locator("#editor .fr-element").waitFor();

        // firstUpdated and connectedCallback both call _initEditor, so it has to be idempotent. A second instance on
        // the same host would leave the first one live and unreachable, registered and never destroyed.
        page.evaluate("""
                async () => {
                    const el = document.querySelector('#editor');
                    const before = el.editor;

                    await el._initEditor();

                    window.__reinit = {
                        same: el.editor === before,
                        editables: el.querySelectorAll('.fr-element').length
                    };
                }
                """);

        assertEquals(Boolean.TRUE, page.evaluate("() => window.__reinit.same"));
        assertEquals(1, ((Number) page.evaluate("() => window.__reinit.editables")).intValue());
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

    @Test
    void replaceSelectionContent_insertsAtTheCaret_andReachesTheServer() {
        editableArea().click();
        page.keyboard().press("End");

        // Clicking the button moves focus out of the editor. Froala keeps the caret it had and inserts there.
        page.locator("#insert-snippet").click();

        assertThat(editableArea().locator("strong")).hasText(FroalaTestView.SNIPPET_TEXT);
        assertThat(editableArea()).containsText(FroalaTestView.INITIAL_TEXT + FroalaTestView.SNIPPET_TEXT);
        assertThat(page.locator("#viewer")).containsText(FroalaTestView.INITIAL_TEXT + FroalaTestView.SNIPPET_TEXT);
    }

    @Test
    void replaceSelectionContent_replacesTheSelection() {
        editableArea().click();
        page.keyboard().press("ControlOrMeta+A");

        page.locator("#insert-snippet").click();

        assertThat(editableArea().locator("strong")).hasText(FroalaTestView.SNIPPET_TEXT);
        assertThat(editableArea()).not().containsText(FroalaTestView.INITIAL_TEXT);
        assertThat(page.locator("#viewer")).not().containsText(FroalaTestView.INITIAL_TEXT);
    }

    @Test
    void replaceSelectionContent_whileTheEditorIsStillBuilding_isNotLost() {
        page.locator("#editor .fr-element").waitFor();

        page.locator("#reattach-and-insert").click();

        assertThat(editableArea().locator("strong")).hasText(FroalaTestView.SNIPPET_TEXT);
        assertThat(page.locator("#viewer")).containsText(FroalaTestView.SNIPPET_TEXT);
    }

    @Test
    void selectionChange_isReportedOncePerSwitch() {
        editableArea().click();
        page.keyboard().press("End");
        page.evaluate("""
                () => {
                    window.__selectionChanges = 0;
                    document.addEventListener('selectionchange', () => window.__selectionChanges++);
                }
                """);

        // Three selection changes, but only the first turns "no selection" into "some". Each press waits for its own
        // selectionchange: Chrome merges the ones that come in quick succession, and three presses sent at once would
        // look like a single change -- the test would then pass without the connector filtering anything.
        for (int i = 1; i <= 3; i++) {
            page.keyboard().press("Shift+ArrowLeft");
            page.waitForFunction("n => window.__selectionChanges >= n", i);
        }
        page.keyboard().press("ArrowRight");

        assertThat(page.locator("#selection-log")).hasText("true false");
    }

    @Test
    void selectionChange_reportsASelectionOutsideTheEditorAsNone() {
        editableArea().click();
        page.keyboard().press("ControlOrMeta+A");
        assertThat(page.locator("#selection-log")).hasText("true");

        // not collapsed, but outside the editor
        page.locator("#viewer").click(new Locator.ClickOptions().setClickCount(3));

        assertThat(page.locator("#selection-log")).hasText("true false");
    }

    @Test
    void selectionChange_reportsNoneAfterTheEditorIsRebuilt() {
        editableArea().click();
        page.keyboard().press("ControlOrMeta+A");
        assertThat(page.locator("#selection-log")).hasText("true");

        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();

        assertThat(page.locator("#selection-log")).hasText("true false");
    }

    private void assertLastChangeSurvivesAnImmediateDetach(String text) {
        editableArea().click();
        editableArea().type(text);

        page.locator("#attach-toggle").click();
        assertThat(page.locator("#editor")).hasCount(0);
        page.locator("#attach-toggle").click();

        assertThat(editableArea()).containsText(text);
    }

    /**
     * Installs Playwright's fake clock and reloads, because the clock only applies to a document opened after it. Every
     * {@code setTimeout} and {@code setInterval} on the page then advances only when a test says so.
     */
    /**
     * Counts the deltas the connector puts on the wire. Under a fake clock this is the only honest way to assert that
     * nothing was sent *yet*: a delta reaches the viewer through a server round trip in real time, so looking at the
     * viewer right after a time jump proves nothing -- it is merely early. The dispatch itself happens synchronously
     * inside the timer callback, so the count is exact the moment {@code fastForward} returns.
     */
    private void recordDeltaDispatches() {
        page.evaluate(
                """
                        () => {
                            window.__deltas = 0;
                            window.__log = [];
                            window.__t0 = Date.now();
                            const el = document.querySelector('#editor');
                            el.addEventListener('_value-delta', (e) => { window.__deltas++;
                                window.__log.push(Date.now() - window.__t0 + ':' + el.valueChangeMode + ':' + e.detail.delta.length); });
                        }
                        """);
    }

    private String licenseKeyInFroala() {
        return String.valueOf(page.evaluate("() => document.querySelector('#editor').editor.opts.key"));
    }

    private int typingTimer() {
        return ((Number) page.evaluate("() => document.querySelector('#editor').editor.opts.typingTimer")).intValue();
    }

    private int dispatchedDeltas() {
        return ((Number) page.evaluate("() => window.__deltas")).intValue();
    }

    private String deltaLog() {
        return String.valueOf(page.evaluate(
                "() => JSON.stringify(window.__log) + ' mode=' + document.querySelector('#editor').valueChangeMode"));
    }

    private void withFakeClock() {
        page.clock().install();
        page.reload();
        page.locator("#editor .fr-element").waitFor();
    }

    private void selectValueChangeMode(String label) {
        page.locator("#value-change-mode").click();
        page.locator("vaadin-select-overlay vaadin-select-item").filter(new Locator.FilterOptions().setHasText(label))
                .first().click();
    }
}
