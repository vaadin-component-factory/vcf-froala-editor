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
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

@Route("signals")
@Menu(title = "Signals", order = 9)
public class SignalsView extends VerticalLayout {

    public SignalsView() {
        setSizeFull();
        setAlignItems(Alignment.STRETCH);

        ValueSignal<String> html = new ValueSignal<>(DemoToolbar.INITIAL_VALUE);

        // writes the signal, not the editor, and both editor and viewer follow
        Button changeValue = new Button("Change value",
                _unused -> html.set(DemoToolbar.INITIAL_VALUE.equals(html.peek()) ? DemoToolbar.ALTERNATIVE_VALUE
                        : DemoToolbar.INITIAL_VALUE));

        FroalaEditor editor = new FroalaEditor("Bound to the signal");
        editor.setHeight("300px");
        editor.bindValue(html, html::set);

        // Demo only. The viewer shows the editor's HTML as it is. Sanitize it first when other users see it.
        FroalaViewer viewer = new DemoFroalaViewer();
        viewer.bindContent(html);

        add(new Paragraph("One ValueSignal<String>, bound to the editor with bindValue(html, html::set) and to the "
                + "viewer with bindContent(html). The viewer only reads the signal."), changeValue, editor, viewer);
        setAlignSelf(Alignment.START, changeValue);
    }
}
