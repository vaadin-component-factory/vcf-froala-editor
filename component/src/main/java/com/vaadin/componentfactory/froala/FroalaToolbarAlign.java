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
 * Which side of the toolbar a button group sits on. Maps to Froala's {@code align} option inside a toolbar group, which
 * defaults to {@link #LEFT}.
 */
public enum FroalaToolbarAlign {

    /** At the left end of the toolbar. Froala's default for a group that does not set the option. */
    LEFT("left"),

    /** At the right end of the toolbar. Froala's default toolbar places undo, redo and full screen there. */
    RIGHT("right");

    private final String optionValue;

    FroalaToolbarAlign(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * Returns the value Froala's {@code align} option expects.
     *
     * @return the option value, e.g. {@code "right"}
     */
    String getOptionValue() {
        return optionValue;
    }
}
