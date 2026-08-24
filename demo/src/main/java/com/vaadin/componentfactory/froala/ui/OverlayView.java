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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("overlay")
@AnonymousAllowed
public class OverlayView extends BasicView {

    public OverlayView() {
        addComponentAsFirst(new Hr());

        HorizontalLayout buttons = new HorizontalLayout();
        addComponentAsFirst(buttons);

        Button showInDialog = new Button("In Dialog", _unused -> {
            Dialog dialog = new Dialog();
            dialog.setWidth(66, Unit.PERCENTAGE);
            dialog.setResizable(true);
            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.setAlignItems(Alignment.STRETCH);
            dialogLayout.setPadding(false);
            dialogLayout.add(new Span("Info Simply click outside to close the dialog"));
            dialogLayout.add(getOverlayContent());
            dialog.add(dialogLayout);

            dialog.addOpenedChangeListener(event -> {
                if (!event.isOpened()) {
                    add(getOverlayContent());
                }
            });

            dialog.setCloseOnEsc(false);
            dialog.open();
        });
        showInDialog.setId("show-in-dialog");
        buttons.add(showInDialog);

        Button showInPopover = new Button("In Popover");
        Popover popover = new Popover();
        popover.setTarget(showInPopover);

        VerticalLayout popoverLayout = new VerticalLayout();
        popoverLayout.setAlignItems(Alignment.STRETCH);
        popoverLayout.add(new Span("Info: Simply click outside to close the popover"));
        popover.add(popoverLayout);

        popover.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                add(getOverlayContent());
            } else {
                popoverLayout.add(getOverlayContent());
            }
        });
        popover.setCloseOnEsc(false);
        popover.setOpenOnClick(true);
        showInPopover.setId("show-in-dialog");
        buttons.add(showInPopover);
    }

    private Component[] getOverlayContent() {
        return new Component[] { getToolbar(), getEditor(), getViewer() };
    }

}
