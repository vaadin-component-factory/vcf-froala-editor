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

import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;
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
 * Configure the editor with {@link #setOptions(FroalaOptions)}, its toolbar with {@link FroalaToolbar}, and its license
 * key with {@link #setLicenseKey(String)}. Without a key Froala shows a watermark. To render the value outside an
 * editor, use {@link FroalaViewer}.
 *
 * <p>
 * The value is HTML. This component does not parse, escape or sanitize it on the server. Froala cleans the content it
 * is given, but only in the browser, so a value that reached the server by any other route was not cleaned at all.
 * Treat the value as untrusted input before storing it or rendering it.
 */
@Tag("vcf-froala-editor")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@NpmPackage(value = "diff-match-patch", version = "1.0.5")
@JsModule("./vcf-froala-editor/vcf-froala-editor.js")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
public class FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String> implements HasValidationProperties,
        HasValidator<String>, InputNotifier, HasSize, HasStyle, Focusable<FroalaEditor>, HasLabel, HasHelper {

    /** The value change mode of a new editor. */
    public static final ValueChangeMode DEFAULT_VALUE_CHANGE_MODE = ValueChangeMode.ON_CHANGE;

    /** The lowest accepted value change timeout. Froala floors its {@code typingTimer} at 250 ms. */
    public static final int MIN_VALUE_CHANGE_TIMEOUT = 250;

    /** The default value change timeout in milliseconds. Same as Froala's {@code typingTimer} default. */
    public static final int DEFAULT_VALUE_CHANGE_TIMEOUT = 500;

    /** The default interval period in milliseconds, used by {@link ValueChangeMode#INTERVAL}. */
    public static final int DEFAULT_INTERVAL_PERIOD = 2000;

    private static final String VALUE_PROPERTY = "value";
    private static final String OPTIONS_PROPERTY = "options";

    private static final DiffMatchPatch DIFF_MATCH_PATCH = new DiffMatchPatch();

    /**
     * Whether the browser has been told about this component's element. Deliberately not {@link #isAttached()}, which
     * answers {@code true} inside a detach listener as well: Flow fires those from {@code StateNode.setParent} before
     * it clears the node's parent, so the node is still reachable from the tree at that point.
     */
    private boolean liveOnClient;

    /** The JSON the editor was last configured with, kept for {@link #getOptionsJson()}. Null when nothing was set. */
    private String optionsJson;

    /**
     * Creates a new instance with the given label.
     *
     * @param label the label shown above the editor, or null for none
     */
    public FroalaEditor(String label) {
        this();
        setLabel(label);
    }

    /**
     * Creates a new instance with the given label and initial value.
     *
     * @param label the label shown above the editor, or null for none
     * @param initialValue the HTML the editor starts with
     */
    public FroalaEditor(String label, String initialValue) {
        this();
        setLabel(label);
        setValue(initialValue);
    }

    /**
     * Creates a new instance with the given label, initial value and value change listener.
     *
     * @param label the label shown above the editor, or null for none
     * @param initialValue the HTML the editor starts with
     * @param valueChangeListener notified of every later change; not notified of the initial value, which is set before
     *            the listener is added
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
     * @param label the label shown above the editor, or null for none
     * @param valueChangeListener notified of every change of the value
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
     * @param valueChangeListener notified of every change of the value
     */
    public FroalaEditor(
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance configured with the given options.
     *
     * @param options Froala options, or null for Froala's defaults
     */
    public FroalaEditor(FroalaOptions options) {
        this();
        setOptions(options);
    }

    /**
     * Creates a new instance with the given label, configured with the given options.
     *
     * @param label the label shown above the editor, or null for none
     * @param options Froala options, or null for Froala's defaults
     */
    public FroalaEditor(String label, FroalaOptions options) {
        this();
        setLabel(label);
        setOptions(options);
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
        setIntervalPeriod(DEFAULT_INTERVAL_PERIOD);

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
     * Applies a delta to the value it was computed against and returns the result.
     *
     * <p>
     * A delta is a diff-match-patch patch text. The client sends every change the user makes in this form, so that a
     * keystroke does not transfer the whole document. The editor applies incoming deltas itself; this method is public
     * so the same conversion can be used outside it.
     *
     * @param oldValue the value the delta was computed against
     * @param delta a diff-match-patch patch text
     * @return the value with the delta applied
     * @throws DeltaMismatchException if the delta does not apply to the given old value
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
     * Configures the underlying Froala editor.
     *
     * <p>
     * Froala reads its options once, when the editor is built, and has no API to change one on a running editor.
     * Calling this on an attached instance therefore destroys the editor and builds a new one. The value is kept;
     * caret, selection, scroll position and undo history are lost. Several calls within one server round trip cause one
     * rebuild.
     *
     * <p>
     * Each call replaces the previous options. They are not merged.
     *
     * <p>
     * Two options are applied after the given ones and take precedence over them, whichever {@code setOptions} overload
     * was used: {@code key} while {@link #setLicenseKey(String)} holds a key, and {@code typingTimer} once
     * {@link #setValueChangeTimeout(int)} has been called. An option given here is used only while the matching setter
     * was not.
     *
     * @param options Froala options, or null for Froala's defaults
     */
    public void setOptions(FroalaOptions options) {
        setOptions(options == null ? null : options.toJson());
    }

    /**
     * Configures the underlying Froala editor from a JSON object, for options {@link FroalaOptions} has no method for.
     * Identical to {@link #setOptions(FroalaOptions)} in every other respect.
     *
     * @param options Froala options as JSON, or null for Froala's defaults
     * @throws IllegalArgumentException if the options contain Froala's {@code events} option
     */
    public void setOptions(JsonObject options) {
        if (options == null) {
            optionsJson = null;
            getElement().removeProperty(OPTIONS_PROPERTY);
            return;
        }

        // Froala's `events` option is a map of callbacks, and JSON has no functions -- whatever arrived here under that
        // name would reach Froala as data and blow up the first time it fires one. Rejected rather than dropped: the
        // options were written to do something, and silently doing nothing is the worse answer.
        if (options.hasKey("events")) {
            throw new IllegalArgumentException("Froala's `events` option takes callbacks, which JSON cannot carry, so"
                    + " it cannot be set from the server. Value changes are reported through"
                    + " addValueChangeListener; anything else Froala fires has no server side listener.");
        }

        optionsJson = options.toJson();
        getElement().setPropertyJson(OPTIONS_PROPERTY, options);
    }

    /**
     * Configures the underlying Froala editor from raw JSON, for options {@link FroalaOptions} has no method for.
     * Identical to {@link #setOptions(FroalaOptions)} in every other respect.
     *
     * @param options Froala options as a JSON object literal, or null for Froala's defaults
     * @throws IllegalArgumentException if the given string is not parseable as a JSON object
     */
    public void setOptions(String options) {
        if (options == null) {
            setOptions((JsonObject) null);
            return;
        }

        JsonObject parsed;
        try {
            parsed = Json.parse(options);
        } catch (JsonException | ClassCastException e) {
            // ClassCastException is elemental's answer to valid JSON that is not an object -- Json.parse is typed as
            // returning one and only fails on the way out.
            throw new IllegalArgumentException("Froala options must be a JSON object: " + e.getMessage(), e);
        }

        setOptions(parsed);
    }

    /**
     * Returns the JSON the editor is configured with, whichever {@code setOptions} overload was used. Null if none was
     * called, in which case Froala uses its defaults.
     *
     * @return the options as JSON, or null
     */
    public String getOptionsJson() {
        return optionsJson;
    }

    /**
     * Inserts an HTML snippet at the caret, replacing the selected content if there is any. Maps onto Froala's
     * {@code html.insert}.
     *
     * <p>
     * Froala cleans the snippet with its own HTML cleaning before inserting it, so what ends up in the editor can
     * differ from the given markup. The cleaning always runs: {@code html.insert}'s optional flags, which skip it or
     * force a block split, are not offered. The value on the server follows through the regular client update, which
     * means {@link #getValue()} does not include the snippet yet when this method returns. A value change listener does
     * receive it, as a change from the client.
     *
     * @param html the HTML snippet to insert, not null
     */
    public void replaceSelectionContent(String html) {
        Objects.requireNonNull(html, "html must not be null");

        getElement().callJsFunction("replaceSelectionContent", html);
    }

    /**
     * Sets the license key of this instance. Maps onto Froala's {@code key} option.
     *
     * <p>
     * Froala is commercial software and the key is customer specific, so this add-on ships none. Without a key the
     * editor works, but shows Froala's unlicensed watermark.
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
     * @param valueChangeMode the new value change mode, or null for the default
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
     * Sets the idle time in milliseconds after the last keystroke before the editor reports the change. This is
     * Froala's {@code typingTimer} option, not a timer of this component. Froala restarts it on every keystroke, which
     * is why {@link ValueChangeMode#ON_CHANGE} reports once the user pauses rather than per keystroke.
     *
     * <p>
     * The default is 500, the minimum {@value #MIN_VALUE_CHANGE_TIMEOUT}. Froala floors the option at that minimum and
     * ignores smaller values silently, so this setter throws instead.
     *
     * <p>
     * Froala uses the option for more than the value sync. It is also the delay before the inline toolbar is shown
     * again after a keystroke, so a long timeout delays that as well.
     *
     * <p>
     * Only {@link ValueChangeMode#ON_CHANGE} uses this value. {@link ValueChangeMode#ON_BLUR} and
     * {@link ValueChangeMode#INTERVAL} are triggered by something else and are not delayed by it.
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
     * <p>
     * This returns what {@link #setValueChangeTimeout(int)} was given, not what the editor runs on. A
     * {@code typingTimer} passed through {@link #setOptions(FroalaOptions)} takes effect as long as the setter was
     * never called, but is not reported here.
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
     * The default is 2000. The value must be greater than zero; the client rejects zero and negative values as well.
     *
     * @param periodInMilliseconds time between two value syncs, greater than zero
     * @throws IllegalArgumentException if the given period is zero or negative
     */
    public void setIntervalPeriod(int periodInMilliseconds) {
        if (periodInMilliseconds <= 0) {
            throw new IllegalArgumentException("intervalPeriod must be greater than 0");
        }

        getElement().setProperty("intervalPeriod", periodInMilliseconds);
    }

    /**
     * Returns the time in milliseconds between two value syncs in {@link ValueChangeMode#INTERVAL}. Default is 2000.
     *
     * @return time between two value syncs
     */
    public int getIntervalPeriod() {
        return getElement().getProperty("intervalPeriod", DEFAULT_INTERVAL_PERIOD);
    }
}
