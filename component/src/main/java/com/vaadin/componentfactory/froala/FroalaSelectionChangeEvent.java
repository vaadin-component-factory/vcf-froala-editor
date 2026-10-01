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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

/**
 * Fired when the editor goes from having no selection to having one, or back. Only that switch is reported, not every
 * move of the caret or the selection's edges, so a selection being dragged out causes one event, not one per pixel.
 *
 * @see FroalaEditor#addSelectionChangeListener(com.vaadin.flow.component.ComponentEventListener)
 */
@DomEvent("selection-change")
public class FroalaSelectionChangeEvent extends ComponentEvent<FroalaEditor> {

    private final boolean hasSelection;

    /**
     * Creates a new event.
     *
     * @param source the editor the selection changed in
     * @param fromClient whether the event originated in the browser
     * @param hasSelection whether something is selected in the editor now
     */
    public FroalaSelectionChangeEvent(FroalaEditor source, boolean fromClient,
            @EventData("event.detail.hasSelection") boolean hasSelection) {
        super(source, fromClient);
        this.hasSelection = hasSelection;
    }

    /**
     * Returns whether something is selected in the editor, as opposed to a bare caret or the selection being outside
     * the editor altogether.
     *
     * @return true if the editor holds a non-empty selection
     */
    public boolean hasSelection() {
        return hasSelection;
    }
}
