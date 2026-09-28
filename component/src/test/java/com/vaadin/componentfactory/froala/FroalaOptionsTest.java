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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain JUnit, no Vaadin. {@link FroalaOptions} is a value object and its whole job is producing the JSON Froala is
 * initialized with, so the JSON is what is asserted. Whether Froala then honours an option is Froala's business, not
 * this add-on's. See the project's testing rules.
 */
class FroalaOptionsTest {

    @Test
    void defaults_setNothing() {
        // Anything we emit here is an option the user did not ask for, and it would silently win over Froala's own
        // default.
        assertEquals("{}", FroalaOptions.defaults().toString());
    }

    @Test
    void everyOption_usesFroalasOwnName() {
        // The one thing the compiler cannot check in the map-backed shape is the key literal next to each typed
        // method. A typo here is an option Froala ignores without a word.
        assertEquals("{\"placeholderText\":\"Write something\"}",
                FroalaOptions.defaults().withPlaceholderText("Write something").toString());
        assertEquals("{\"language\":\"de\"}", FroalaOptions.defaults().withLanguage("de").toString());
        assertEquals("{\"direction\":\"rtl\"}",
                FroalaOptions.defaults().withDirection(FroalaTextDirection.RTL).toString());
        assertEquals("{\"toolbarInline\":true}", FroalaOptions.defaults().withToolbarInline(true).toString());
        assertEquals("{\"documentReady\":true}", FroalaOptions.defaults().withDocumentReady(true).toString());
        assertEquals("{\"toolbarSticky\":false}", FroalaOptions.defaults().withToolbarSticky(false).toString());
        assertEquals("{\"toolbarBottom\":true}", FroalaOptions.defaults().withToolbarBottom(true).toString());
        assertEquals("{\"charCounterCount\":false}", FroalaOptions.defaults().withCharCounterCount(false).toString());
        assertEquals("{\"charCounterMax\":2000}", FroalaOptions.defaults().withCharCounterMax(2000).toString());
        assertEquals("{\"wordCounterCount\":true}", FroalaOptions.defaults().withWordCounterCount(true).toString());
        assertEquals("{\"wordCounterMax\":-1}", FroalaOptions.defaults().withWordCounterMax(-1).toString());
        assertEquals("{\"imageUploadURL\":\"/upload\"}",
                FroalaOptions.defaults().withImageUploadUrl("/upload").toString());
        assertEquals("{\"fileUploadURL\":\"/upload\"}",
                FroalaOptions.defaults().withFileUploadUrl("/upload").toString());
        assertEquals("{\"videoUploadURL\":\"/upload\"}",
                FroalaOptions.defaults().withVideoUploadUrl("/upload").toString());
        assertEquals("{\"emoticonsUseImage\":false}", FroalaOptions.defaults().withEmoticonsUseImage(false).toString());
        assertEquals("{\"saveInterval\":0}", FroalaOptions.defaults().withSaveInterval(0).toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    void typingTimer_isStillWritten() {
        // Deprecated in favour of the setter, which is a hint and not a removal. Anyone who prefers the option must
        // still be able to use it.
        assertEquals("{\"typingTimer\":800}", FroalaOptions.defaults().withTypingTimer(800).toString());
    }

    @Test
    void pluginsEnabled_usesTheRegistryNameNotTheFileName() {
        // This is the trap the enum exists for. The file is font_family.min.js, but pluginsEnabled only understands
        // fontFamily. Passing the file name switches the plugin off instead of on, silently.
        assertEquals("{\"pluginsEnabled\":[\"fontFamily\",\"findReplace\",\"track_changes\"]}", FroalaOptions.defaults()
                .withPluginsEnabled(FroalaPlugin.FONT_FAMILY, FroalaPlugin.FIND_AND_REPLACE, FroalaPlugin.TRACK_CHANGES)
                .toString());

        assertEquals("{\"pluginsEnabled\":[\"align\"]}",
                FroalaOptions.defaults().withPluginsEnabled(List.of(FroalaPlugin.ALIGN)).toString());
    }

    @Test
    void everyPlugin_carriesBothNames() {
        for (FroalaPlugin plugin : FroalaPlugin.values()) {
            assertFalse(plugin.getFileName().isBlank(), plugin + " has no file name");
            assertFalse(plugin.getPluginName().isBlank(), plugin + " has no plugin name");
        }
    }

    @Test
    void with_leavesTheOriginalAlone() {
        FroalaOptions base = FroalaOptions.defaults().withLanguage("de");
        FroalaOptions derived = base.withPlaceholderText("Write something");

        assertEquals("{\"language\":\"de\"}", base.toString());
        assertTrue(derived.toString().contains("\"language\":\"de\""));
        assertTrue(derived.toString().contains("\"placeholderText\":\"Write something\""));
    }

    @Test
    void null_removesTheOptionAgain() {
        // Not the same as setting it to JSON null. An absent option lets Froala's own default stand, but one that is
        // present and null overrides it with nothing.
        assertEquals("{}", FroalaOptions.defaults().withLanguage("de").withLanguage(null).toString());
        assertEquals("{}", FroalaOptions.defaults().withPluginsEnabled(FroalaPlugin.ALIGN)
                .withPluginsEnabled((List<FroalaPlugin>) null).toString());
    }

    @Test
    void setTwice_keepsTheLastValue() {
        assertEquals("{\"language\":\"fi\"}",
                FroalaOptions.defaults().withLanguage("de").withLanguage("fi").toString());
    }

    @Test
    void equality_isByContentNotByIdentity() {
        assertEquals(FroalaOptions.defaults().withLanguage("de"), FroalaOptions.defaults().withLanguage("de"));
        assertEquals(FroalaOptions.defaults().withLanguage("de").hashCode(),
                FroalaOptions.defaults().withLanguage("de").hashCode());
        assertNotEquals(FroalaOptions.defaults().withLanguage("de"), FroalaOptions.defaults().withLanguage("fi"));
    }
}
