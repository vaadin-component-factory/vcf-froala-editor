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

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.util.LinkedList;
import java.util.List;

@NpmPackage(value = "froala-editor", version = "5.4.0")
@NpmPackage(value = "diff-match-patch", version = "1.0.5")

@Tag("vcf-froala-editor")
@JsModule("./vcf-froala-editor/vcf-froala-editor.js")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
public class FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String> implements HasValidationProperties,
        HasValidator<String>, InputNotifier, HasSize, HasStyle, Focusable<FroalaEditor>, HasLabel, HasHelper {

    public static final int DEFAULT_VALUE_CHANGE_MODE_TIMEOUT = 2000;
    public static final ValueChangeMode DEFAULT_VALUE_CHANGE_MODE = ValueChangeMode.ON_CHANGE;

    private static final DiffMatchPatch DIFF_MATCH_PATCH = new DiffMatchPatch();
    private boolean initialized;

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
    /// @param label        label
    /// @param initialValue initial value
    public FroalaEditor(String label, String initialValue) {
        this();
        setLabel(label);
        setValue(initialValue);
    }

    /// Creates a new instance with the given label, initial value and value change listener. The initial value is set as it is without
    /// any further processing.
    ///
    /// @param label               label
    /// @param initialValue        initial value
    /// @param valueChangeListener value change listener
    public FroalaEditor(String label, String initialValue, ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        setValue(initialValue);
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance with the given label and value change listener.
    ///
    /// @param label               label
    /// @param valueChangeListener value change listener
    public FroalaEditor(String label, ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance with the given value change listener.
    ///
    /// @param valueChangeListener value change listener
    public FroalaEditor(ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        addValueChangeListener(valueChangeListener);
    }

    /// Creates a new instance.
    public FroalaEditor() {
        super("value", "", true);

        setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        setValueChangeTimeout(DEFAULT_VALUE_CHANGE_MODE_TIMEOUT);

        Element element = getElement();
        element.addEventListener("_value-delta", event -> {
            String delta = event.getEventData().get("event.detail.delta").asString();
            String oldValue = getValue();
            String newValue = applyDelta(oldValue, delta);

            // we only update the model value here to prevent an auto sync of the full value with the client on each change (the server would send the full
            // value to the client each time). Also this allows us to fire a value change event with fromClient = true.
            // the presentation value is synced on detach, so that on the next attach, the client gets the latest value.
            setModelValue(newValue, true);
        }).addEventData("event.detail.delta");

        addAttachListener(event -> {
            // we do this in before client response to allow other attach listeners to do their configs as well
            runBeforeClientResponse(ui -> this.initialized = true);
        });

        addDetachListener(event -> {
            this.initialized = false;

            // we set the presentation value here to ensure that on the next attach, it will be set correctly
            // background is, that in our delta value change handler, only the model value is set, but not the presentation value,
            // since this would re-send the whole value to the client on each value change. Since we do not want to have this, but
            // just sync the value, when the editor is re-attached, we set the value here.
            setPresentationValue(getValue());
        });

    }

    /// Applies the given delta onto the "old" value. Returns the "new", resulting value
    ///
    /// @param oldValue old value
    /// @param delta    delta to apply
    /// @return new value
    public static String applyDelta(String oldValue, String delta) {
        // convert string to patch objectq
        List<DiffMatchPatch.Patch> patches = DIFF_MATCH_PATCH.patchFromText(delta);

        // apply patch object
        Object[] results = DIFF_MATCH_PATCH.patchApply(
                patches instanceof LinkedList<DiffMatchPatch.Patch> alreadyLinkedList
                        ? alreadyLinkedList
                        : new LinkedList<>(patches),
                oldValue);

        // extract the resulting string
        return (String) results[0];
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
        return ValueChangeMode.fromClientSide(getElement().getProperty("valueChangeMode", DEFAULT_VALUE_CHANGE_MODE.getClientSideRepresentation()));
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

    private void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui
                .beforeClientResponse(this, context -> command.accept(ui)));
    }
}
