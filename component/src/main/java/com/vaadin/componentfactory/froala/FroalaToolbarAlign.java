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
 * Which side of the toolbar a button group sits on, Froala's {@code align} inside a toolbar group.
 */
public enum FroalaToolbarAlign {

    /** At the left end of the toolbar. Froala's default for a group that does not say. */
    LEFT("left"),

    /** At the right end of the toolbar, where Froala's own default puts undo, redo and full screen. */
    RIGHT("right");

    private final String optionValue;

    FroalaToolbarAlign(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * Returns the value Froala's {@code align} expects.
     *
     * @return option value
     */
    public String getOptionValue() {
        return optionValue;
    }
}
