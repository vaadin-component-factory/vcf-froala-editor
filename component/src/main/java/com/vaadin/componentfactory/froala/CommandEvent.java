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

/**
 * Fired when the user triggers one of the editor's own commands, from a button or its keyboard shortcut.
 *
 * @see FroalaEditor#addCommand(FroalaCommand, com.vaadin.flow.component.ComponentEventListener)
 */
public class CommandEvent extends ComponentEvent<FroalaEditor> {

    private final FroalaCommand command;

    /**
     * Creates a new event.
     *
     * @param source the editor the command was triggered in
     * @param fromClient whether the event originated in the browser
     * @param command the command that was triggered
     */
    public CommandEvent(FroalaEditor source, boolean fromClient, FroalaCommand command) {
        super(source, fromClient);
        this.command = command;
    }

    /**
     * Returns the command that was triggered.
     *
     * @return the command
     */
    public FroalaCommand getCommand() {
        return command;
    }
}
