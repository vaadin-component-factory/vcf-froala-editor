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
 * The reading direction of the edited text, Froala's {@code direction} option.
 */
public enum FroalaTextDirection {

    /** Left to right. Froala's default. */
    LTR("ltr"),

    /** Right to left, for Arabic, Hebrew, Persian and Urdu. */
    RTL("rtl"),

    /**
     * Let the browser decide per paragraph from the characters it contains, by writing {@code dir="auto"} into the
     * markup. Mixed-language documents are what this is for.
     */
    AUTO("auto");

    private final String optionValue;

    FroalaTextDirection(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * Returns the value Froala's {@code direction} option expects.
     *
     * @return option value
     */
    public String getOptionValue() {
        return optionValue;
    }
}
