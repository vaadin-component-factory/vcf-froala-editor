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
package com.vaadin.componentfactory.froala;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Plain JUnit, no Vaadin. That an editor without options gets {@link FroalaPlugin#basics()} is covered in the browser
 * by {@code FroalaLoadingIT}.
 */
class FroalaPluginTest {

    @Test
    void basics_areExactlyTheAgreedList() {
        assertEquals(Set.of(FroalaPlugin.ALIGN, FroalaPlugin.COLORS, FroalaPlugin.FIND_AND_REPLACE,
                FroalaPlugin.FONT_FAMILY, FroalaPlugin.FONT_SIZE, FroalaPlugin.HELP, FroalaPlugin.LINE_HEIGHT,
                FroalaPlugin.LINK, FroalaPlugin.LINK_TO_ANCHOR, FroalaPlugin.LISTS, FroalaPlugin.PARAGRAPH_FORMAT,
                FroalaPlugin.QUOTE, FroalaPlugin.URL, FroalaPlugin.WORD_PASTE), FroalaPlugin.basics());
    }

    @Test
    void changingTheReturnedSet_leavesTheNextOneAlone() {
        // the documented way to add one plugin is to change the returned set, so no caller may change it for the next
        // one
        FroalaPlugin.basics().add(FroalaPlugin.TABLE);
        FroalaPlugin.all().clear();

        assertFalse(FroalaPlugin.basics().contains(FroalaPlugin.TABLE));
        assertFalse(FroalaPlugin.all().isEmpty());
    }

    @Test
    void all_holdsEveryConstant() {
        assertEquals(constants(), FroalaPlugin.all());
    }

    private static Set<String> constants() {
        return Arrays.stream(FroalaPlugin.class.getFields()).filter(field -> Modifier.isStatic(field.getModifiers()))
                .map(FroalaPluginTest::value).collect(Collectors.toSet());
    }

    private static String value(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}
