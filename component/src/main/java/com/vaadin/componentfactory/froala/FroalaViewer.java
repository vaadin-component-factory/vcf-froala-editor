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
import com.vaadin.flow.dom.Style;

/**
 * A convenience component to present froala generated html.
 * <p>
 * See for instance <a href=
 * "https://froala.com/wysiwyg-editor/docs/overview/install-froala-commonjs/#displaying-content-outside-of-the-froala-editor">this
 * sample</a>.
 * </p>
 *
 */
@Tag("vcf-froala-viewer")
@CssImport("froala-editor/css/froala_editor.pkgd.min.css")
public class FroalaViewer extends Component implements HasSize, HasStyle {
    public FroalaViewer() {
        addClassName("fr-view");
        getStyle().setDisplay(Style.Display.BLOCK);
    }

    public void setContent(String text) {
        getElement().setProperty("innerHTML", text);
    }
}
