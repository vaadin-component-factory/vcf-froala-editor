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
package com.vaadin.componentfactory.froala;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.dom.Element;

/**
 * Flow integration of the Froala WYSIWYG editor.
 *
 * <p>
 * Please note, that html values are not parsed or sanitized by the Java code, but the client side only. Therefore
 * handle every input with care before saving or presenting it.
 */
@Tag("vcf-froala-editor")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@NpmPackage(value = "diff-match-patch", version = "1.0.5")
@JsModule("./vcf-froala-editor/vcf-froala-editor.js")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
public class FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String> implements HasValidationProperties,
        HasValidator<String>, InputNotifier, HasSize, HasStyle, Focusable<FroalaEditor>, HasLabel, HasHelper {

    public static final int DEFAULT_VALUE_CHANGE_TIMEOUT = 500;
    public static final int DEFAULT_VALUE_CHANGE_INTERVAL = 2000;
    public static final ValueChangeMode DEFAULT_VALUE_CHANGE_MODE = ValueChangeMode.ON_CHANGE;

    /** Froala floors its own {@code typingTimer} at this value, so anything below it would have no effect. */
    public static final int MIN_VALUE_CHANGE_TIMEOUT = 250;

    private static final String VALUE_PROPERTY = "value";

    private static final DiffMatchPatch DIFF_MATCH_PATCH = new DiffMatchPatch();

    /**
     * Whether the browser has been told about this component's element. Deliberately not {@link #isAttached()}, which
     * answers {@code true} inside a detach listener as well: Flow fires those from {@code StateNode.setParent} before
     * it clears the node's parent, so the node is still reachable from the tree at that point.
     */
    private boolean liveOnClient;

    /**
     * Creates a new instance with the given label.
     *
     * @param label label
     */
    public FroalaEditor(String label) {
        this();
        setLabel(label);
    }

    /**
     * Creates a new instance with the given label and initial value. The initial value is set as it is without any
     * further processing.
     *
     * @param label label
     * @param initialValue initial value
     */
    public FroalaEditor(String label, String initialValue) {
        this();
        setLabel(label);
        setValue(initialValue);
    }

    /**
     * Creates a new instance with the given label, initial value and value change listener. The initial value is set as
     * it is without any further processing.
     *
     * @param label label
     * @param initialValue initial value
     * @param valueChangeListener value change listener
     */
    public FroalaEditor(String label, String initialValue,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        setValue(initialValue);
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance with the given label and value change listener.
     *
     * @param label label
     * @param valueChangeListener value change listener
     */
    public FroalaEditor(String label,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance with the given value change listener.
     *
     * @param valueChangeListener value change listener
     */
    public FroalaEditor(
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance.
     */
    public FroalaEditor() {
        // The three arg constructor registers Flow's own listener for a "value-changed" DOM event. Nothing dispatches
        // that today and nothing should: the client reports changes as deltas over `_value-delta`. Making `value` a
        // notifying Lit property on the client would quietly open a second update path next to it.
        super(VALUE_PROPERTY, "", true);

        setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        setValueChangeTimeout(DEFAULT_VALUE_CHANGE_TIMEOUT);
        setValueChangeInterval(DEFAULT_VALUE_CHANGE_INTERVAL);

        Element element = getElement();
        element.addEventListener("_value-delta", event -> {
            String delta = event.getEventData().get("event.detail.delta").asString();

            String newValue;
            try {
                newValue = applyDelta(getValue(), delta);
            } catch (DeltaMismatchException e) {
                // Both sides have drifted apart, so this delta and every following one is unusable. The client holds
                // the user's text and has to resend it -- pushing our stale value would throw that text away.
                element.callJsFunction("resyncValue");
                return;
            }

            // Only the model value, never the presentation value: writing the property would put the whole document
            // back on the wire for every keystroke. Updating the model also lets the value change event carry
            // fromClient = true.
            setModelValue(newValue, true);
        }).addEventData("event.detail.delta");

        element.addEventListener("_value-resync",
                event -> setModelValue(event.getEventData().get("event.detail.value").asString(), true))
                .addEventData("event.detail.value");

        // Set in before-client-response, not directly on attach, so that other attach listeners still see a component
        // that the browser does not know yet -- the same reason hugerte-for-flow gives for its own flag. For the value
        // push below the timing makes no difference (that guard cannot be satisfied during an attach), but anything
        // added later that has to be configured before the client learns of the editor will need it this way.
        addAttachListener(event -> event.getUI().beforeClientResponse(this, context -> liveOnClient = true));

        addDetachListener(event -> {
            liveOnClient = false;

            // The property lags behind the editor by design, see above. Bringing it in step once here is enough,
            // because Flow replays a node's properties when it is attached again -- that is what seeds the rebuilt
            // editor.
            setPresentationValue(getValue());
        });
    }

    @Override
    protected void setPresentationValue(String newPresentationValue) {
        // A client edit updates the model only, so the property still holds whatever the server set last. Setting
        // exactly that value again is the case Flow drops as unchanged -- and the browser, whose own copy of the state
        // tree is stale for the same reason, would ignore the update even if it were sent. Only then is an explicit
        // push needed; every other value travels through the property as usual.
        //
        // Only while the browser has an element to push to. The detach listener calls this too, with the value the
        // property already holds -- exactly the shape below. Flow does not drop such a call, it defers it to the next
        // attach, where it would overwrite whatever the server set while the component was away.
        boolean clientNeedsExplicitPush = liveOnClient
                && Objects.equals(newPresentationValue, getElement().getProperty(VALUE_PROPERTY));

        super.setPresentationValue(newPresentationValue);

        if (clientNeedsExplicitPush) {
            getElement().executeJs("this.value = $0", newPresentationValue);
        }
    }

    /**
     * Applies the given delta onto the "old" value. Returns the "new", resulting value.
     *
     * @param oldValue old value
     * @param delta delta to apply
     * @return new value
     * @throws DeltaMismatchException if the delta cannot be applied to the given old value
     */
    public static String applyDelta(String oldValue, String delta) {
        List<DiffMatchPatch.Patch> patches = DIFF_MATCH_PATCH.patchFromText(delta);

        Object[] results = DIFF_MATCH_PATCH
                .patchApply(patches instanceof LinkedList<DiffMatchPatch.Patch> alreadyLinkedList ? alreadyLinkedList
                        : new LinkedList<>(patches), oldValue);

        // patchApply answers with an untyped pair. Checking its shape keeps a library change from surfacing as a
        // ClassCastException from inside a value update.
        if (results.length != 2 || !(results[0] instanceof String patched)
                || !(results[1] instanceof boolean[] applied)) {
            throw new DeltaMismatchException(
                    "diff-match-patch returned an unexpected result shape, expected a String and a boolean[]");
        }

        // One flag per patch. A false flag means that patch found no place to apply, so the string comes back only
        // partially patched or entirely unchanged -- without this check the edit is lost with nothing to notice it.
        for (int i = 0; i < applied.length; i++) {
            if (!applied[i]) {
                throw new DeltaMismatchException(
                        "Patch " + (i + 1) + " of " + applied.length + " did not apply to the current value");
            }
        }

        return patched;
    }

    /**
     * Sets the license key of this instance. Maps onto Froala's {@code key} option.
     *
     * <p>
     * Froala is commercial software and the key is customer specific, therefore this add-on ships none. Without a key
     * the editor still works, but shows Froala's unlicensed watermark.
     *
     * <p>
     * The key is only read when the client side editor initializes, so calling this on an already attached instance has
     * no effect until it is detached and attached again.
     *
     * @param licenseKey license key or null to unset
     */
    public void setLicenseKey(String licenseKey) {
        if (licenseKey == null) {
            getElement().removeProperty("licenseKey");
        } else {
            getElement().setProperty("licenseKey", licenseKey);
        }
    }

    /**
     * Returns the license key of this instance. May be null.
     *
     * @return license key or null
     */
    public String getLicenseKey() {
        return getElement().getProperty("licenseKey");
    }

    /**
     * Sets the value change mode of this instance. By default the editor uses {@link ValueChangeMode#ON_CHANGE}. Null
     * resets the mode to the default.
     *
     * @param valueChangeMode new value change mode
     */
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        if (valueChangeMode == null) {
            setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        } else {
            getElement().setProperty("valueChangeMode", valueChangeMode.getClientSideRepresentation());
        }
    }

    /**
     * Returns the current value change mode. Never null.
     *
     * @return value change mode
     */
    public ValueChangeMode getValueChangeMode() {
        return ValueChangeMode.fromClientSide(
                getElement().getProperty("valueChangeMode", DEFAULT_VALUE_CHANGE_MODE.getClientSideRepresentation()));
    }

    /**
     * Sets the idle time in milliseconds that has to pass after the last keystroke before the editor reports the
     * change. This is Froala's own {@code typingTimer} option, not a timer of this add-on: Froala restarts it on every
     * keystroke and only then reports, which is why {@link ValueChangeMode#ON_CHANGE} syncs once the user pauses rather
     * than per key.
     *
     * <p>
     * Default is 500, Froala's own default. The minimum is {@value #MIN_VALUE_CHANGE_TIMEOUT} -- Froala floors the
     * option there, so a smaller value would be silently ignored and is rejected here instead. The client rejects it
     * with the same message.
     *
     * <p>
     * Note that the option is not exclusive to the value sync: Froala uses the same timespan for its selection-change
     * flush, which drives the active state of the toolbar buttons, and for the reveal delay of the inline toolbar. A
     * long timeout slows those down as well.
     *
     * <p>
     * {@link ValueChangeMode#ON_BLUR} and {@link ValueChangeMode#INTERVAL} do not depend on it -- neither waits for a
     * reported change.
     *
     * @param timeoutInMilliseconds idle time before a change is reported, at least {@value #MIN_VALUE_CHANGE_TIMEOUT}
     * @throws IllegalArgumentException if the given timeout is below {@value #MIN_VALUE_CHANGE_TIMEOUT}
     */
    public void setValueChangeTimeout(int timeoutInMilliseconds) {
        if (timeoutInMilliseconds < MIN_VALUE_CHANGE_TIMEOUT) {
            throw new IllegalArgumentException("valueChangeTimeout must be at least " + MIN_VALUE_CHANGE_TIMEOUT
                    + " ms, the lower bound Froala " + "enforces");
        }

        getElement().setProperty("valueChangeTimeout", timeoutInMilliseconds);
    }

    /**
     * Returns the idle time in milliseconds that has to pass after the last keystroke before the editor reports the
     * change. Default is 500.
     *
     * @return idle time in milliseconds
     */
    public int getValueChangeTimeout() {
        return getElement().getProperty("valueChangeTimeout", DEFAULT_VALUE_CHANGE_TIMEOUT);
    }

    /**
     * Sets the time in milliseconds between two value syncs in {@link ValueChangeMode#INTERVAL}. Also the time before
     * the first one. Has no effect in any other mode.
     *
     * <p>
     * Default is 2000. Must be greater than zero -- the mode has no meaningful behaviour at zero, and the client
     * rejects it as well.
     *
     * @param intervalInMilliseconds time between two value syncs, greater than zero
     * @throws IllegalArgumentException if the given interval is zero or negative
     */
    public void setValueChangeInterval(int intervalInMilliseconds) {
        if (intervalInMilliseconds <= 0) {
            throw new IllegalArgumentException("valueChangeInterval must be greater than 0");
        }

        getElement().setProperty("valueChangeInterval", intervalInMilliseconds);
    }

    /**
     * Returns the time in milliseconds between two value syncs in {@link ValueChangeMode#INTERVAL}. Default is 2000.
     *
     * @return time between two value syncs
     */
    public int getValueChangeInterval() {
        return getElement().getProperty("valueChangeInterval", DEFAULT_VALUE_CHANGE_INTERVAL);
    }
}
