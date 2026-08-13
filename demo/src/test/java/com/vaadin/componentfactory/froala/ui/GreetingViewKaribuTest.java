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

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import kotlin.jvm.functions.Function0;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.SpringServlet;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;

/**
 * Browserless UI-unit test using Karibu Testing (license-free) — runs the view in the JVM, no browser. Spring-aware via
 * MockSpringServlet so the view gets the real GreetingService bean. Lives in the view's package to reach the
 * package-private component fields.
 *
 * <p>
 * Note for the Froala work: Karibu executes no JavaScript, so it can assert server-side state and element attributes
 * but never the editor itself. Anything that depends on the Froala JS running belongs in the e2e module.
 */
@SpringBootTest
class GreetingViewKaribuTest {

    private static final Routes routes = new Routes().autoDiscoverViews("com.vaadin.componentfactory.froala");

    @Autowired
    ApplicationContext ctx;

    @BeforeEach
    void setup() {
        Function0<UI> uiFactory = UI::new;
        SpringServlet servlet = new MockSpringServlet(routes, ctx, uiFactory);

        MockVaadin.setup(uiFactory, servlet);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void enterName_clickGreet_showsGreeting() {
        UI.getCurrent().navigate(GreetingView.class);
        GreetingComponent greeting = _get(GreetingView.class).greetingComponent;

        _setValue(greeting.nameField, "World");
        _click(greeting.greetButton);

        Assertions.assertEquals("Hello, World!", greeting.result.getText());
    }
}
