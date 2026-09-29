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

import java.util.EnumSet;

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
        assertEquals(
                EnumSet.of(FroalaPlugin.ALIGN, FroalaPlugin.COLORS, FroalaPlugin.FIND_AND_REPLACE,
                        FroalaPlugin.FONT_FAMILY, FroalaPlugin.FONT_SIZE, FroalaPlugin.HELP, FroalaPlugin.LINE_HEIGHT,
                        FroalaPlugin.LINK, FroalaPlugin.LINK_TO_ANCHOR, FroalaPlugin.LISTS,
                        FroalaPlugin.PARAGRAPH_FORMAT, FroalaPlugin.QUOTE, FroalaPlugin.URL, FroalaPlugin.WORD_PASTE),
                FroalaPlugin.basics());
    }

    @Test
    void changingTheReturnedSet_leavesTheNextOneAlone() {
        // the documented way to add one plugin is to change the returned set, so no caller may change it for the next
        // one
        FroalaPlugin.basics().add(FroalaPlugin.TABLE);
        FroalaPlugin.all().clear();

        assertFalse(FroalaPlugin.basics().contains(FroalaPlugin.TABLE));
        assertEquals(EnumSet.allOf(FroalaPlugin.class), FroalaPlugin.all());
    }
}
