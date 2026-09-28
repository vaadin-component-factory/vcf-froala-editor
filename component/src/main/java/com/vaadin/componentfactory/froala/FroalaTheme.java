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
 * The look of the editor. Maps to Froala's {@code theme} option. The add-on uses {@link #VAADIN} unless the options set
 * another one.
 *
 * @see FroalaOptions#withTheme(FroalaTheme)
 * @see FroalaOptions#withCustomTheme(String)
 */
public enum FroalaTheme {

    /**
     * Follows the Vaadin theme, including its dark variant. The add-on's default. The colours come from the
     * {@code --vcf-froala-*} custom properties, which are set from Lumo and can be overridden.
     */
    VAADIN("vaadin"),

    /**
     * Froala's dark theme. Its stylesheet {@code froala-editor/css/themes/dark.min.css} is not loaded by the add-on.
     */
    DARK("dark"),

    /**
     * Froala's gray theme. Its stylesheet {@code froala-editor/css/themes/gray.min.css} is not loaded by the add-on.
     */
    GRAY("gray"),

    /**
     * Froala's royal theme. Its stylesheet {@code froala-editor/css/themes/royal.min.css} is not loaded by the add-on.
     */
    ROYAL("royal"),

    /** No theme. The editor looks as Froala's own stylesheet draws it. */
    NONE(null);

    private final String optionValue;

    FroalaTheme(String optionValue) {
        this.optionValue = optionValue;
    }

    /**
     * Returns the value Froala's {@code theme} option expects.
     *
     * @return the option value, for example {@code "dark"}, or null for {@link #NONE}
     */
    public String getOptionValue() {
        return optionValue;
    }
}
