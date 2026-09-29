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
import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("")
@Menu(title = "Basics", order = 0)
public class BasicView extends FroalaViewBase {

    public BasicView() {
        FroalaEditor editor = getEditor();

        editor.setHeight("500px");
        // starts without plugin options, so the editor gets FroalaPlugin.basics()
        editor.setOptions(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.basics()));

        addComponentAtIndex(indexOf(getToolbar()) + 1, new Paragraph(
                "An editor without plugin options gets FroalaPlugin.basics(): text and paragraph formats, "
                        + "lists, quotes, links and find and replace. Nothing that inserts images, tables or other "
                        + "content. The toolbar is FroalaToolbar.basics()."));

        getViewer().setMinHeight("250px");
    }

    @Override
    protected Component createToolbar(FroalaEditor editor, FroalaViewer viewer) {
        return new DemoToolbar(editor, FroalaPlugin.basics(), FroalaToolbar.basics());
    }
}
