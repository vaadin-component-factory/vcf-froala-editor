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

/**
 * Determines when the editor sends its value from the browser to the server. Set with
 * {@link FroalaEditor#setValueChangeMode(FroalaValueChangeMode)}.
 *
 * <p>
 * Not to be confused with Vaadin's {@link com.vaadin.flow.data.value.ValueChangeMode}. {@link #ON_CHANGE} behaves like
 * Vaadin's {@code LAZY} while the user types, not like Vaadin's {@code ON_CHANGE}. {@link #INTERVAL} matches Vaadin's
 * {@code TIMEOUT}. There is no equivalent of Vaadin's {@code EAGER}.
 *
 * <p>
 * In every mode a pending change is also sent when the editor loses focus.
 */
public enum FroalaValueChangeMode {

    /**
     * Sends the value on every change event the editor reports. Froala debounces typing itself, so while the user types
     * the value is sent once they pause, after {@link FroalaEditor#setValueChangeTimeout(int)} milliseconds. Changes
     * that are not typing, such as toolbar commands, paste, cut and undo, are reported immediately.
     */
    ON_CHANGE("change"),

    /**
     * Sends the value when the editor loses focus.
     */
    ON_BLUR("blur"),

    /**
     * Sends the value every {@link FroalaEditor#setIntervalPeriod(int)} milliseconds while there are changes to send,
     * independently of user events. This is the only mode that sends anything while the user types without pausing,
     * because Froala reports no change during an uninterrupted burst of typing.
     */
    INTERVAL("interval");

    private final String clientValue;

    FroalaValueChangeMode(String clientValue) {
        this.clientValue = clientValue;
    }

    /** The string the client element's {@code valueChangeMode} property expects. */
    String getClientValue() {
        return clientValue;
    }

    /** The mode for a {@code valueChangeMode} property value. */
    static FroalaValueChangeMode fromClientValue(String clientValue) {
        for (FroalaValueChangeMode mode : values()) {
            if (mode.clientValue.equals(clientValue)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown value change mode: " + clientValue);
    }
}
