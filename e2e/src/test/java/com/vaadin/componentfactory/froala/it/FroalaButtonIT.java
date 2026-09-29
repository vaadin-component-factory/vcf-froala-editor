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
package com.vaadin.componentfactory.froala.it;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.FroalaButton;
import com.vaadin.componentfactory.froala.FroalaQuickInsertButton;
import com.vaadin.componentfactory.froala.it.views.FroalaAllPluginsTestView;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end test for the button name constants. With every plugin enabled, each constant has to be a name Froala
 * registered. A Froala update that renames or drops a command fails here.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaButtonIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaAllPluginsTestView.ROUTE;
    }

    @BeforeEach
    void waitForTheEditorWithEveryPlugin() {
        // The plugin files register their commands before the editor is built, so a built editor means all are there
        page.locator("#all .fr-element[contenteditable='true']").waitFor();
        page.waitForFunction("() => window.FroalaEditorClass !== undefined");
    }

    @Test
    void everyButtonConstant_isARegisteredCommand() {
        Set<String> constants = constants(FroalaButton.class);
        constants.removeAll(Set.of(FroalaButton.VERTICAL_SEPARATOR, FroalaButton.HORIZONTAL_SEPARATOR));

        assertEquals(Set.of(), unregistered(constants, "COMMANDS"), "constants Froala has no command for");
    }

    @Test
    void everyQuickInsertConstant_isARegisteredQuickInsertButton() {
        assertEquals(Set.of(), unregistered(constants(FroalaQuickInsertButton.class), "QUICK_INSERT_BUTTONS"),
                "constants Froala has no quick insert button for");
    }

    private Set<String> unregistered(Set<String> names, String registry) {
        @SuppressWarnings("unchecked")
        List<String> registered = (List<String>) page
                .evaluate("registry => Object.keys(window.FroalaEditorClass[registry])", registry);

        return names.stream().filter(name -> !registered.contains(name)).collect(Collectors.toSet());
    }

    private static Set<String> constants(Class<?> type) {
        return Arrays.stream(type.getFields()).filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(FroalaButtonIT::value).collect(Collectors.toSet());
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
