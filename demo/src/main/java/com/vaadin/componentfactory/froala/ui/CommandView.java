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

import com.vaadin.componentfactory.froala.FroalaButton;
import com.vaadin.componentfactory.froala.FroalaCommand;
import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("command")
@Menu(title = "Own command", order = 7)
public class CommandView extends VerticalLayout {

    public CommandView() {
        FroalaEditor editor = new FroalaEditor("Letter", FroalaOptions.defaults()
                .withToolbarButtons(FroalaToolbar.of(FroalaButton.BOLD, FroalaButton.ITALIC, "insertGreeting")));
        editor.setValue("<p>Thank you for your order.</p>");
        editor.setWidthFull();

        FroalaCommand insertGreeting = new FroalaCommand("insertGreeting", "Insert greeting",
                VaadinIcon.COMMENT.create()).withShortcut(Key.KEY_G, KeyModifier.SHIFT);
        editor.addCommand(insertGreeting, event -> editor.replaceSelectionContent("<p>Dear customer,</p>"));

        add(new Paragraph("The speech bubble button is a command of the application's own. Its listener runs on the "
                + "server and inserts a greeting at the caret. Ctrl+Shift+G does the same."), editor);
    }
}
