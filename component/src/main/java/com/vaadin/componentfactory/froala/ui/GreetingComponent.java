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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Placeholder sample component — to be replaced by the real {@code FroalaEditor} wrapper (see ROADMAP.md, phase 1). It
 * exists so the module has compiling, tested content and proves the add-on/demo/e2e wiring end to end.
 *
 * <p>
 * Deliberately Spring-free so the published add-on JAR forces nothing on consumers — behaviour is supplied as a
 * function, and the demo app wires in its Spring bean. Fields are package-private so the Karibu test can drive them
 * directly.
 */
public class GreetingComponent extends HorizontalLayout {

    final TextField nameField = new TextField("Name");
    final Button greetButton = new Button("Greet");
    final Span result = new Span();

    public GreetingComponent(SerializableFunction<String, String> greeter) {
        result.setId("greeting-result");
        greetButton.addClickListener(e -> result.setText(greeter.apply(nameField.getValue())));

        setAlignItems(FlexComponent.Alignment.BASELINE);
        add(nameField, greetButton, result);
    }
}
