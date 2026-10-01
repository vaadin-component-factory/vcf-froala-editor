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
 * The reading direction of the edited text. Maps to Froala's {@code direction} option, which defaults to {@link #AUTO}.
 */
public enum FroalaTextDirection {

    /** Left to right. */
    LTR("ltr"),

    /** Right to left, for Arabic, Hebrew, Persian and Urdu. */
    RTL("rtl"),

    /**
     * Writes {@code dir="auto"} into the markup and lets the browser determine the direction per paragraph from the
     * characters it contains. Intended for mixed-language documents. This is Froala's default.
     */
    AUTO("auto");

    private final String optionValue;

    FroalaTextDirection(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * Returns the value Froala's {@code direction} option expects.
     *
     * @return the option value, for example {@code "rtl"}
     */
    String getOptionValue() {
        return optionValue;
    }
}
