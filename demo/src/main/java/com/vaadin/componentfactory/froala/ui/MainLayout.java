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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell: a top bar plus a side navigation drawer. The {@code @Layout} annotation makes the router render
 * every view inside this layout.
 */
@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        Button darkMode = new Button(VaadinIcon.ADJUST.create(), _unused -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();

            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK);
            } else {
                themeList.add(Lumo.DARK);
            }
        });
        darkMode.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        darkMode.setAriaLabel("Dark mode");
        darkMode.setTooltipText("Dark mode");
        darkMode.getStyle().setMarginLeft("auto").setMarginRight("var(--lumo-space-m)");

        addToNavbar(new DrawerToggle(), new H1("Froala Editor for Vaadin Flow"), darkMode);

        SideNav nav = new SideNav();

        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(new SideNavItem(entry.title(), entry.path())));

        addToDrawer(nav);
    }
}
