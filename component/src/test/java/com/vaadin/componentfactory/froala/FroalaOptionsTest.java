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
        assertEquals("{\"spellcheck\":false}", FroalaOptions.defaults().withSpellcheck(false).toString());
        // THM-6
        assertEquals("{\"theme\":\"dark\"}", FroalaOptions.defaults().withTheme(FroalaTheme.DARK).toString());
        assertEquals("{\"theme\":\"brand\"}", FroalaOptions.defaults().withTheme("brand").toString());
    }

    @Test
    void htmlCleaningAndPasteOptions_useFroalasOwnNames() {
        List<String> list = List.of("a", "b");
        FroalaOptions options = FroalaOptions.defaults().withHtmlAllowedTags(list).withHtmlRemoveTags(list)
                .withHtmlAllowedAttrs(list).withHtmlAllowedStyleProps(list).withHtmlAllowedEmptyTags(list)
                .withHtmlDoNotWrapTags(list).withHtmlIgnoreCssProperties(list).withHtmlAllowComments(false)
                .withHtmlExecuteScripts(false).withHtmlSimpleAmpersand(true).withHtmlUntouched(true)
                .withUseClasses(false).withPastePlain(true).withPasteDeniedTags(list).withPasteDeniedAttrs(list)
                .withPasteAllowedStyleProps(list).withPasteAllowLocalImages(true).withWordPasteModal(false)
                .withWordPasteKeepFormatting(false).withWordDeniedTags(list).withWordDeniedAttrs(list)
                .withWordAllowedStyleProps(list);

        String ab = "[\"a\",\"b\"]";
        assertEquals("{\"htmlAllowedTags\":" + ab + ",\"htmlRemoveTags\":" + ab + ",\"htmlAllowedAttrs\":" + ab
                + ",\"htmlAllowedStyleProps\":" + ab + ",\"htmlAllowedEmptyTags\":" + ab + ",\"htmlDoNotWrapTags\":"
                + ab + ",\"htmlIgnoreCSSProperties\":" + ab
                + ",\"htmlAllowComments\":false,\"htmlExecuteScripts\":false,\"htmlSimpleAmpersand\":true,"
                + "\"htmlUntouched\":true,\"useClasses\":false,\"pastePlain\":true,\"pasteDeniedTags\":" + ab
                + ",\"pasteDeniedAttrs\":" + ab + ",\"pasteAllowedStyleProps\":" + ab
                + ",\"pasteAllowLocalImages\":true,\"wordPasteModal\":false,"
                + "\"wordPasteKeepFormatting\":false,\"wordDeniedTags\":" + ab + ",\"wordDeniedAttrs\":" + ab
                + ",\"wordAllowedStyleProps\":" + ab + "}", options.toString());
    }

    @Test
    void buttonListOptions_useFroalasOwnNames() {
        List<String> list = List.of(FroalaButton.BOLD);
        FroalaOptions options = FroalaOptions.defaults().withCodeViewKeepActiveButtons(list)
                .withSelectionActionButtons(list).withQuickInsertButtons(List.of(FroalaQuickInsertButton.TABLE))
                .withColorsButtons(list).withEmoticonsButtons(list).withSpecialCharButtons(list).withFaButtons(list)
                .withLinkEditButtons(list).withLinkInsertButtons(list).withAnchorEditButtons(list)
                .withImageEditButtons(list).withImageInsertButtons(list).withImageAltButtons(list)
                .withImageSizeButtons(list).withVideoEditButtons(list).withVideoInsertButtons(list)
                .withVideoSizeButtons(list).withFileInsertButtons(list).withFilesInsertButtons(list)
                .withFilesInsertButtons2(list).withTableEditButtons(list).withTableInsertButtons(list)
                .withTableColorsButtons(list).withEmbedlyEditButtons(list).withEmbedlyInsertButtons(list)
                .withFormEditButtons(list).withFormUpdateButtons(list);

        List<String> keys = List.of(options.toJson().keys());
        assertEquals(List.of("codeViewKeepActiveButtons", "selectionActionButtons", "quickInsertButtons",
                "colorsButtons", "emoticonsButtons", "specialCharButtons", "faButtons", "linkEditButtons",
                "linkInsertButtons", "anchorEditButtons", "imageEditButtons", "imageInsertButtons", "imageAltButtons",
                "imageSizeButtons", "videoEditButtons", "videoInsertButtons", "videoSizeButtons", "fileInsertButtons",
                "filesInsertButtons", "filesInsertButtons2", "tableEditButtons", "tableInsertButtons",
                "tableColorsButtons", "embedlyEditButtons", "embedlyInsertButtons", "formEditButtons",
                "formUpdateButtons"), keys);
        assertEquals("[\"table\"]", options.toJson().get("quickInsertButtons").toJson());
        keys.stream().filter(key -> !key.equals("quickInsertButtons"))
                .forEach(key -> assertEquals("[\"bold\"]", options.toJson().get(key).toJson(), key));
    }

    @Test
    void emptyList_isSentAsAnEmptyArray() {
        // Not the same as null. An empty htmlAllowedStyleProps removes every style attribute. Null keeps Froala's
        // default.
        assertEquals("{\"htmlAllowedStyleProps\":[]}",
                FroalaOptions.defaults().withHtmlAllowedStyleProps(List.of()).toString());
        assertEquals("{}", FroalaOptions.defaults().withHtmlAllowedStyleProps(List.of()).withHtmlAllowedStyleProps(null)
                .toString());
    }

    @Test
    void themeNone_isAnEmptyString() {
        // THM-5. The add-on's own default only applies to a missing key, so "no theme" has to be sent as a value that
        // Froala reads as none.
        assertEquals("{\"theme\":\"\"}", FroalaOptions.defaults().withTheme(FroalaTheme.NONE).toString());
    }

    @Test
    void themeNull_removesTheOption() {
        assertEquals("{}", FroalaOptions.defaults().withTheme(FroalaTheme.DARK).withTheme(null).toString());
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
        // The file is font_family.min.js, but pluginsEnabled only understands fontFamily. A constant holding the file
        // name would switch the plugin off instead of on, silently. An application's own name goes through as given.
        assertEquals("{\"pluginsEnabled\":[\"fontFamily\",\"findReplace\",\"track_changes\"]}", FroalaOptions.defaults()
                .withPluginsEnabled(FroalaPlugin.FONT_FAMILY, FroalaPlugin.FIND_AND_REPLACE, FroalaPlugin.TRACK_CHANGES)
                .toString());

        assertEquals("{\"pluginsEnabled\":[\"align\",\"myPlugin\"]}",
                FroalaOptions.defaults().withPluginsEnabled(List.of(FroalaPlugin.ALIGN, "myPlugin")).toString());
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
                .withPluginsEnabled((List<String>) null).toString());
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
