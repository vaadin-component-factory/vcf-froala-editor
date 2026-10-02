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
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.aura.Aura;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Application shell: a top bar plus a side navigation drawer. The {@code @Layout} annotation makes the router render
 * every view inside this layout. The layout loads the theme, Lumo or Aura, and switches between them, so that the page
 * never has both. Aura and the dark mode are kept in the query parameters {@code theme=Aura} and {@code dark=true}, so
 * that a reload keeps them. Lumo in light mode, the default, has none.
 */
@Layout
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private static final String DIRECTORY_URL = "https://vaadin.com/directory/component/froala-editor";

    private final Select<String> theme = new Select<>();
    private Registration themeStyleSheet = UI.getCurrent().getPage().addStyleSheet(Lumo.STYLESHEET);

    /** The build info is missing when the demo runs from an IDE without a Maven build, so the version is left out. */
    public MainLayout(Optional<BuildProperties> buildProperties) {
        Button darkMode = new Button(VaadinIcon.CONTRAST.create(), _unused -> {
            Page page = UI.getCurrent().getPage();
            page.setColorScheme(
                    page.getColorScheme() == ColorScheme.Value.DARK ? ColorScheme.Value.LIGHT : ColorScheme.Value.DARK);
            writeQueryParameters(UI.getCurrent().getActiveViewLocation());
        });
        darkMode.addThemeVariants(ButtonVariant.TERTIARY);
        darkMode.setAriaLabel("Dark mode");
        darkMode.setTooltipText("Dark mode");
        darkMode.getStyle().setMarginRight("var(--vaadin-padding-m)");

        theme.setItems("Lumo", "Aura");
        theme.setValue("Lumo");
        theme.addValueChangeListener(event -> {
            themeStyleSheet.remove();
            themeStyleSheet = UI.getCurrent().getPage()
                    .addStyleSheet("Aura".equals(event.getValue()) ? Aura.STYLESHEET : Lumo.STYLESHEET);
            writeQueryParameters(UI.getCurrent().getActiveViewLocation());
        });
        theme.setAriaLabel("Theme");
        theme.getStyle().setMarginLeft("auto");

        addToNavbar(new DrawerToggle(), new H1("Froala Editor for Vaadin Flow"), theme, darkMode);

        SideNav nav = new SideNav();

        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(new SideNavItem(entry.title(), entry.path())));

        Div footer = new Div();
        buildProperties.map(build -> build.get("addon.version"))
                .ifPresent(version -> footer.add(new Div("Version " + version)));
        footer.add(new Anchor(DIRECTORY_URL, "Vaadin Directory", AnchorTarget.BLANK));
        footer.getStyle().setFontSize("var(--lumo-font-size-s, var(--aura-font-size-s))")
                .setColor("var(--vaadin-text-color-secondary)").setPadding("var(--vaadin-padding-m)");

        VerticalLayout drawer = new VerticalLayout(nav, footer);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setAlignItems(FlexComponent.Alignment.STRETCH);
        drawer.setFlexGrow(1, nav);

        addToDrawer(drawer);
    }

    /** Takes the theme and the dark mode from the address, and puts them back into it after a view without them. */
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        QueryParameters parameters = event.getLocation().getQueryParameters();
        parameters.getSingleParameter("theme").filter(value -> value.equals("Lumo") || value.equals("Aura"))
                .ifPresent(theme::setValue);
        parameters.getSingleParameter("dark").ifPresent(dark -> UI.getCurrent().getPage()
                .setColorScheme(Boolean.parseBoolean(dark) ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT));

        writeQueryParameters(event.getLocation());
    }

    private void writeQueryParameters(Location location) {
        Page page = UI.getCurrent().getPage();
        QueryParameters parameters = location.getQueryParameters().excluding("theme", "dark");
        if ("Aura".equals(theme.getValue())) {
            parameters = parameters.merging("theme", "Aura");
        }
        if (page.getColorScheme() == ColorScheme.Value.DARK) {
            parameters = parameters.merging("dark", "true");
        }
        page.getHistory().replaceState(null, new Location(location.getPath(), parameters));
    }
}
