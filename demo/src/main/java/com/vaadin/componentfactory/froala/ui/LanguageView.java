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
package com.vaadin.componentfactory.froala.ui;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("language")
@Menu(title = "Language", order = 6)
public class LanguageView extends VerticalLayout {

    public LanguageView() {
        FroalaEditor finnish = new FroalaEditor("Finnish", FroalaOptions.defaults().withLanguage("fi"));
        finnish.setValue("<p>Hei <b>maailma</b></p>");
        finnish.setWidthFull();

        add(new Paragraph("Froala's own texts, such as the toolbar tooltips, in Finnish. Without withLanguage an "
                + "editor takes the language of the UI's locale."), finnish);
    }
}
