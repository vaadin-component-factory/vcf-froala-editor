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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Displays HTML written in a {@link FroalaEditor}, outside the editor. It imports Froala's stylesheet and carries
 * Froala's {@code fr-view} class, so the content is rendered as it was during editing. Froala documents the same
 * approach for <a href=
 * "https://froala.com/wysiwyg-editor/docs/overview/install-froala-commonjs/#displaying-content-outside-of-the-froala-editor">displaying
 * content outside the editor</a>.
 *
 * <p>
 * The viewer also carries the class {@code vaadin-theme}, so the content rules of {@link FroalaTheme#VAADIN}, such as
 * the colours of tracked changes, apply as they do in the editor. An application whose editors use another Froala theme
 * removes it with {@code removeClassName("vaadin-theme")}. The base text, i.e. font, colour, size, weight and line
 * height, comes from the page, which under Lumo gives the same values as the editor by default. Overriding the
 * {@code --vcf-froala-*} or input field properties changes the editor only.
 */
@Tag("vcf-froala-viewer")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@CssImport("froala-editor/css/froala_editor.pkgd.min.css")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin-rules.css")
public class FroalaViewer extends Component implements HasSize, HasStyle {

    /**
     * Creates a new instance and adds Froala's {@code fr-view} class and the {@code vaadin-theme} class to it.
     * Replacing the class list with {@link HasStyle#setClassName(String)} removes both, and the content then loses
     * Froala's styling.
     */
    public FroalaViewer() {
        addClassNames("fr-view", "vaadin-theme");
    }

    /**
     * Sets the HTML to display.
     *
     * <p>
     * The given string is written to the element's {@code innerHTML} unchanged. It is not escaped or sanitized here,
     * and a value taken from {@link FroalaEditor} was not sanitized on the server either. Pass trusted HTML, or
     * sanitize it before calling this method.
     *
     * @param text the HTML to display
     */
    public void setContent(String text) {
        getElement().setProperty("innerHTML", text);
    }
}
