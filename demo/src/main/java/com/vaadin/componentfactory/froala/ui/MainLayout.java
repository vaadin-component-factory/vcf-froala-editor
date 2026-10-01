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

import java.util.Optional;

import org.springframework.boot.info.BuildProperties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

    private static final String DIRECTORY_URL = "https://vaadin.com/directory/component/froala-editor";

    /** The build info is missing when the demo runs from an IDE without a Maven build, so the version is left out. */
    public MainLayout(Optional<BuildProperties> buildProperties) {
        Button darkMode = new Button(VaadinIcon.CONTRAST.create(), _unused -> {
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

        Div footer = new Div();
        buildProperties.map(build -> build.get("addon.version"))
                .ifPresent(version -> footer.add(new Div("Version " + version)));
        footer.add(new Anchor(DIRECTORY_URL, "Vaadin Directory", AnchorTarget.BLANK));
        footer.getStyle().setFontSize("var(--lumo-font-size-s)").setColor("var(--lumo-secondary-text-color)")
                .setPadding("var(--lumo-space-m)");

        VerticalLayout drawer = new VerticalLayout(nav, footer);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setAlignItems(FlexComponent.Alignment.STRETCH);
        drawer.setFlexGrow(1, nav);

        addToDrawer(drawer);
    }
}
