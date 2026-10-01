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
import com.vaadin.componentfactory.froala.FroalaViewer;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route("command")
@Menu(title = "Own commands", order = 7)
public class CommandView extends VerticalLayout {

    public CommandView() {
        FroalaCommand selectAllText = new FroalaCommand("selectAllText", "Select all text",
                VaadinIcon.ALIGN_JUSTIFY.create());
        FroalaCommand showDialog = new FroalaCommand("showDialog", "Show dialog", VaadinIcon.MODAL.create())
                .withShortcut(Key.KEY_L, KeyModifier.SHIFT);
        // The button is a toggle that shows as pressed while the popover is open.
        FroalaCommand showPopover = new FroalaCommand("showPopover", "Show popover", VaadinIcon.INFO_CIRCLE.create())
                .withToggle();
        FroalaCommand showViewer = new FroalaCommand("showViewer", "Show viewer", VaadinIcon.EYE.create()).withToggle()
                .withShortcut(Key.F10);

        // The toolbar places each command's button by its name.
        FroalaEditor editor = new FroalaEditor("Letter",
                FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.of(FroalaButton.BOLD, FroalaButton.ITALIC,
                        FroalaButton.UNDERLINE, FroalaButton.STRIKE_THROUGH, FroalaButton.VERTICAL_SEPARATOR,
                        selectAllText.getName(), showDialog.getName(), showPopover.getName(), showViewer.getName())));
        editor.setValue("<p>Thank you for your order. It will be shipped <b>tomorrow</b>.</p>");
        editor.setWidthFull();

        editor.addCommand(selectAllText, event -> editor.selectAll());

        Dialog dialog = new Dialog(new Paragraph("Opened by an own command of the editor."));
        dialog.setHeaderTitle("Own command");
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        editor.addCommand(showDialog, event -> dialog.open());

        // The editor puts the popover into the UI and opens it at the button.
        Popover popover = new Popover(new Paragraph("A popover next to the button that opened it."));
        editor.addCommand(showPopover, popover);
        popover.addOpenedChangeListener(event -> editor.setCommandActive(showPopover, event.isOpened()));

        // A toggle whose listener switches the state and shows or hides the viewer with it.
        FroalaViewer viewer = new FroalaViewer();
        viewer.setContent(editor.getValue());
        viewer.setVisible(false);
        // Demo only. The viewer shows the editor's HTML as it is. Sanitize it first when other users see it.
        editor.addValueChangeListener(event -> viewer.setContent(event.getValue()));
        editor.addCommand(showViewer, event -> {
            boolean active = !editor.isCommandActive(showViewer);
            editor.setCommandActive(showViewer, active);
            viewer.setVisible(active);
        });

        add(new Paragraph("The last four buttons are commands of the application's own, each with a Vaadin icon. "
                + "Ctrl+Shift+L opens the dialog as well."), editor, viewer);
    }
}
