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

import com.vaadin.componentfactory.froala.service.GreetingService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Demo view: injects a Spring bean (which lives in the demo, not the add-on) and wires it into the framework-agnostic
 * component from the add-on. Shows how a consuming app supplies behaviour to a reusable, Spring-free component — the
 * same pattern the Froala license key will follow.
 */
@Route("greeting")
@AnonymousAllowed
public class GreetingView extends VerticalLayout {

    final GreetingComponent greetingComponent;

    public GreetingView(GreetingService greetingService) {
        greetingComponent = new GreetingComponent(greetingService::greet);
        add(new H2("Greeting"), greetingComponent);
    }
}
