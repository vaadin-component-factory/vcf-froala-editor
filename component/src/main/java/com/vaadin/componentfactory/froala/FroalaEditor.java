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

/// Flow integration of the Froala WYSIWYG editor.
///
/// Please note, that html values are not parsed or sanitized by the Java code, but the client side only. Therefore
/// handle every input with care before saving or presenting it.
@Tag("vcf-froala-editor")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@NpmPackage(value = "diff-match-patch", version = "1.0.5")
@JsModule("./vcf-froala-editor/vcf-froala-editor.js")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
public class FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String> implements HasValidationProperties,
        HasValidator<String>, InputNotifier, HasSize, HasStyle, Focusable<FroalaEditor>, HasLabel, HasHelper {

    public static final int DEFAULT_VALUE_CHANGE_MODE_TIMEOUT = 2000;
    public static final ValueChangeMode DEFAULT_VALUE_CHANGE_MODE = ValueChangeMode.ON_CHANGE;

    private static final DiffMatchPatch DIFF_MATCH_PATCH = new DiffMatchPatch();

    private static String defaultLicenseKey;

    /// Creates a new instance with the given label.
    ///
    /// @param label label
    public FroalaEditor(String label) {
        this();
        setLabel(label);
    }

    /// Creates a new instance with the given label and initial value. The initial value is set as it is without
    /// any further processing.
    ///
    /// @param label label
    /// @param initialValue initial value
    public FroalaEditor(String label, String initialValue) {
        this();
        setLabel(label);
        setValue(initialValue);
    }

    /// Creates a new instance with the given label, initial value and value change listener. The initial value is set
    /// as it is without
    /// any further processing.
    ///
    /// @param label label
    /// @param initialValue initial value
    /// @param valueChangeListener value change listener
    public FroalaEditor(String label, String initialValue,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        setValue(initialValue);
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance with the given label and value change listener.
    ///
    /// @param label label
    /// @param valueChangeListener value change listener
    public FroalaEditor(String label,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance with the given value change listener.
    ///
    /// @param valueChangeListener value change listener
    public FroalaEditor(
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance.
    public FroalaEditor() {
        // The three arg constructor also registers Flow's own listener for a "value-changed" DOM event. Nothing
        // dispatches that today and nothing should: the client reports changes as deltas over `_value-delta`. Making
        // `value` a notifying Lit property on the client would quietly activate a second, parallel update path.
        super("value", "", true);

        setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        setValueChangeTimeout(DEFAULT_VALUE_CHANGE_MODE_TIMEOUT);
        setLicenseKey(defaultLicenseKey);

        Element element = getElement();
        element.addEventListener("_value-delta", event -> {
            String delta = event.getEventData().get("event.detail.delta").asString();

            String newValue;
            try {
                newValue = applyDelta(getValue(), delta);
            } catch (DeltaMismatchException e) {
                // The delta was built against a value we do not have, so both sides have drifted apart and this delta
                // is unusable. The client holds the user's text, so it is the side that has to resend -- never push our
                // stale value onto it, that would throw away whatever was typed.
                element.callJsFunction("resyncValue");
                return;
            }

            // we only update the model value here to prevent an auto sync of the full value with the client on each
            // change (the server would send the full
            // value to the client each time). Also this allows us to fire a value change event with fromClient = true.
            // the presentation value is synced on detach, so that on the next attach, the client gets the latest value.
            setModelValue(newValue, true);
        }).addEventData("event.detail.delta");

        element.addEventListener("_value-resync", event -> {
            String value = event.getEventData().get("event.detail.value").asString();
            setModelValue(value, true);
        }).addEventData("event.detail.value");

        addDetachListener(event -> {
            // we set the presentation value here to ensure that on the next attach, it will be set correctly
            // background is, that in our delta value change handler, only the model value is set, but not the
            // presentation value,
            // since this would re-send the whole value to the client on each value change. Since we do not want to have
            // this, but
            // just sync the value, when the editor is re-attached, we set the value here.
            setPresentationValue(getValue());
        });

    }

    /// Applies the given delta onto the "old" value. Returns the "new", resulting value
    ///
    /// @param oldValue old value
    /// @param delta delta to apply
    /// @return new value
    /// @throws DeltaMismatchException if the delta does not fit the given old value
    public static String applyDelta(String oldValue, String delta) {
        // convert string to patch object
        List<DiffMatchPatch.Patch> patches = DIFF_MATCH_PATCH.patchFromText(delta);

        // apply patch object
        Object[] results = DIFF_MATCH_PATCH
                .patchApply(patches instanceof LinkedList<DiffMatchPatch.Patch> alreadyLinkedList ? alreadyLinkedList
                        : new LinkedList<>(patches), oldValue);

        // patchApply returns the resulting string plus one flag per patch. A false flag means that patch found no place
        // to apply and was skipped -- the string then comes back partially patched or, as verified against 1.2,
        // entirely unchanged, with the edit silently lost. Checking these is the whole difference to the reference
        // implementation this was ported from.
        boolean[] applied = (boolean[]) results[1];
        for (int i = 0; i < applied.length; i++) {
            if (!applied[i]) {
                throw new DeltaMismatchException(
                        "Patch " + (i + 1) + " of " + applied.length + " did not apply to the current value");
            }
        }

        // extract the resulting string
        return (String) results[0];
    }

    /// Sets the license key for every editor created afterwards. Instances that already exist are not modified.
    /// Applications will usually call this once at startup, so that [#FroalaEditor()] and friends need no key.
    ///
    /// Froala is commercial software and the key is customer specific, therefore this add-on ships none. Without a
    /// key the editor still works, but shows Froala's unlicensed watermark.
    ///
    /// This is global mutable state. A test that sets it has to reset it afterwards, or it leaks into whatever runs
    /// next in the same JVM.
    ///
    /// @param defaultLicenseKey license key or null to unset
    public static void setDefaultLicenseKey(String defaultLicenseKey) {
        FroalaEditor.defaultLicenseKey = defaultLicenseKey;
    }

    /// Returns the license key applied to newly created instances. May be null.
    ///
    /// @return default license key or null
    public static String getDefaultLicenseKey() {
        return defaultLicenseKey;
    }

    /// Sets the license key of this instance, overriding [#setDefaultLicenseKey(String)]. Maps onto Froala's `key`
    /// option.
    ///
    /// The key is only read when the client side editor initializes, so calling this on an already attached instance
    /// has no effect until it is detached and attached again.
    ///
    /// @param licenseKey license key or null to unset
    public void setLicenseKey(String licenseKey) {
        if (licenseKey == null) {
            getElement().removeProperty("licenseKey");
        } else {
            getElement().setProperty("licenseKey", licenseKey);
        }
    }

    /// Returns the license key of this instance. May be null.
    ///
    /// @return license key or null
    public String getLicenseKey() {
        return getElement().getProperty("licenseKey");
    }

    /// Sets the value change mode of this instance. By default the editor uses [ValueChangeMode#ON_CHANGE]. Null
    /// resets the mode to the default.
    ///
    /// @param valueChangeMode new value change mode
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        if (valueChangeMode == null) {
            setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        } else {
            getElement().setProperty("valueChangeMode", valueChangeMode.getClientSideRepresentation());
        }
    }

    /// Returns the current value change mode. Never null.
    ///
    /// @return value change mode
    public ValueChangeMode getValueChangeMode() {
        return ValueChangeMode.fromClientSide(
                getElement().getProperty("valueChangeMode", DEFAULT_VALUE_CHANGE_MODE.getClientSideRepresentation()));
    }

    /// Sets the timespan in milliseconds, that will be used by several value change modes.
    /// * TIMEOUT: the time, that is waited after the last change before the value is synced.
    /// * INTERVAL: the time between two value syncs. Also used as initial time before the first call.
    ///
    /// Default is 2000.
    ///
    /// Please note, that the client also throttles the amount of events, that might be fired to prevent the
    /// events from overhelming the server.
    ///
    /// @param timeoutInMilliseconds milliseconds to be used by the value change modes
    public void setValueChangeTimeout(int timeoutInMilliseconds) {
        if (timeoutInMilliseconds < 0) {
            throw new IllegalArgumentException("Timeout must be zero or greater!");
        }

        getElement().setProperty("valueChangeTimeout", timeoutInMilliseconds);
    }

    /// Sets the timespan in milliseconds, that will be used by several value change modes.
    /// * TIMEOUT: the time, that is waited after the last change before the value is synced.
    /// * INTERVAL: the time between two value syncs. Also used as initial time before the first call.
    ///
    /// Default is 2000.
    ///
    /// @return timespan in milliseconds
    public int getValueChangeTimeout() {
        return getElement().getProperty("valueChangeTimeout", DEFAULT_VALUE_CHANGE_MODE_TIMEOUT);
    }

    /// Thrown by [#applyDelta(String,String)] when a delta cannot be applied to the value it is handed, which means
    /// the client built it against a different base and the two sides have drifted apart.
    public static class DeltaMismatchException extends RuntimeException {

        /// Creates a new instance with the given message.
        ///
        /// @param message message
        public DeltaMismatchException(String message) {
            super(message);
        }
    }
}
