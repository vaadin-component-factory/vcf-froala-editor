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
 * The names of the editor's looks, for Froala's {@code theme} option. The add-on uses {@link #VAADIN} unless the
 * options set another one.
 *
 * <p>
 * The constants are plain strings, so a theme of your own is a plain string too. Froala puts the class
 * {@code <theme>-theme} on the editor and on its popups, and the stylesheet for it is yours to load.
 *
 * @see FroalaOptions#withTheme(String)
 */
public final class FroalaTheme {

    /**
     * Follows the Vaadin theme, including its dark variant. The add-on's default. The colors come from the
     * {@code --vcf-froala-*} custom properties, which are set from Lumo and can be overridden.
     */
    public static final String VAADIN = "froala-vaadin";

    /**
     * Froala's dark theme. Its stylesheet {@code froala-editor/css/themes/dark.min.css} is not loaded by the add-on.
     */
    public static final String DARK = "dark";

    /**
     * Froala's gray theme. Its stylesheet {@code froala-editor/css/themes/gray.min.css} is not loaded by the add-on.
     */
    public static final String GRAY = "gray";

    /**
     * Froala's royal theme. Its stylesheet {@code froala-editor/css/themes/royal.min.css} is not loaded by the add-on.
     */
    public static final String ROYAL = "royal";

    /**
     * No theme. The editor looks as Froala draws it without one, which is Froala's own default. Sent as the empty
     * string, because null would bring back the add-on's default.
     */
    public static final String NONE = "";

    private FroalaTheme() {
    }
}
