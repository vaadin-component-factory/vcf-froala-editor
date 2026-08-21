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

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Application shell: a top bar plus a side navigation drawer. The {@code @Layout} annotation makes the router render
 * every view inside this layout.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    public MainLayout() {
        addToNavbar(new DrawerToggle(), new H1("Froala Editor for Vaadin Flow"));

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Basic", BasicView.class));
        addToDrawer(nav);
    }
}
