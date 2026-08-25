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
 * Enumeration of value change modes for the FroalaEditor.
 */
public enum ValueChangeMode implements ClientSideReference {

    /**
     * Syncs the value with the server on every "change" event, that is fired by the editor. Froala debounces typing
     * itself, so while the user types this happens once they pause -- after
     * {@link FroalaEditor#setValueChangeTimeout(int)} milliseconds. Everything that is not typing (toolbar commands,
     * paste, cut, undo/redo) is reported at once.
     */
    ON_CHANGE("change"),

    /**
     * Syncs the value with the server, when the editor loses the focus ("blur event").
     */
    ON_BLUR("blur"),

    /**
     * <p>
     * Syncs the value every {@link FroalaEditor#setIntervalPeriod(int)} milliseconds, regardless of any user events, as
     * long as there are changes to sync. The only mode that sends anything at all while the user types without pausing
     * -- Froala reports nothing during an uninterrupted burst.
     * </p>
     * <p>
     * This is the equivalent to Vaadin's native {@link com.vaadin.flow.data.value.ValueChangeMode#TIMEOUT}
     * </p>
     */
    INTERVAL("interval");

    private final String clientSideRepresentation;

    ValueChangeMode(String clientSideRepresentation) {
        this.clientSideRepresentation = clientSideRepresentation;
    }

    @Override
    public String getClientSideRepresentation() {
        return clientSideRepresentation;
    }

    /**
     * Interprets the given string as the client side representation of an enum and returns the matching instance.
     * 
     * @param clientSide client side representation
     * @return instance
     * @throws IllegalArgumentException on any unknown string
     */
    public static ValueChangeMode fromClientSide(String clientSide) {
        for (ValueChangeMode mode : values()) {
            if (mode.clientSideRepresentation.equals(clientSide)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown value change mode: " + clientSide);
    }
}
