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

/**
 * The Froala plugins that {@link FroalaOptions#withPluginsEnabled} accepts, as shipped with froala-editor 5.4.0.
 *
 * <p>
 * A plugin has two names and they are not derived from each other: the file it lives in
 * ({@code js/plugins/font_family.min.js}) and the name it registers itself under, which is the one Froala's
 * {@code pluginsEnabled} option takes ({@code fontFamily}). Most are the same word in two spellings, some are not
 * ({@code find_and_replace} registers as {@code findReplace}), and {@code track_changes} keeps its underscore where
 * every other multi-word plugin is camel-cased. Hence the enum: neither name is guessable from the other.
 *
 * <p>
 * {@code edit_in_popup} ships as a plugin file but registers a module, not a plugin, so {@code pluginsEnabled} has no
 * say over it and it is not listed here.
 */
public enum FroalaPlugin {

    /** {@code js/plugins/ai_assist.min.js} */
    AI_ASSIST("ai_assist", "aiAssist"),

    /** {@code js/plugins/align.min.js} */
    ALIGN("align", "align"),

    /** {@code js/plugins/char_counter.min.js} */
    CHAR_COUNTER("char_counter", "charCounter"),

    /** {@code js/plugins/code_beautifier.min.js} */
    CODE_BEAUTIFIER("code_beautifier", "codeBeautifier"),

    /** {@code js/plugins/code_snippet.min.js} */
    CODE_SNIPPET("code_snippet", "codeSnippet"),

    /** {@code js/plugins/code_view.min.js} */
    CODE_VIEW("code_view", "codeView"),

    /** {@code js/plugins/collaborative.min.js} */
    COLLABORATIVE("collaborative", "collaborative"),

    /** {@code js/plugins/colors.min.js} */
    COLORS("colors", "colors"),

    /** {@code js/plugins/cryptojs.min.js} */
    CRYPTOJS("cryptojs", "cryptoJSPlugin"),

    /** {@code js/plugins/draggable.min.js} */
    DRAGGABLE("draggable", "draggable"),

    /** {@code js/plugins/emoticons.min.js} */
    EMOTICONS("emoticons", "emoticons"),

    /** {@code js/plugins/entities.min.js} */
    ENTITIES("entities", "entities"),

    /** {@code js/plugins/export_to_word.min.js} */
    EXPORT_TO_WORD("export_to_word", "exportToWord"),

    /** {@code js/plugins/file.min.js} */
    FILE("file", "file"),

    /** {@code js/plugins/files_manager.min.js} */
    FILES_MANAGER("files_manager", "filesManager"),

    /** {@code js/plugins/filestack.min.js} */
    FILESTACK("filestack", "filestack"),

    /** {@code js/plugins/find_and_replace.min.js} */
    FIND_AND_REPLACE("find_and_replace", "findReplace"),

    /** {@code js/plugins/font_family.min.js} */
    FONT_FAMILY("font_family", "fontFamily"),

    /** {@code js/plugins/font_size.min.js} */
    FONT_SIZE("font_size", "fontSize"),

    /** {@code js/plugins/forms.min.js} */
    FORMS("forms", "forms"),

    /** {@code js/plugins/fullscreen.min.js} */
    FULLSCREEN("fullscreen", "fullscreen"),

    /** {@code js/plugins/help.min.js} */
    HELP("help", "help"),

    /** {@code js/plugins/image.min.js} */
    IMAGE("image", "image"),

    /** {@code js/plugins/image_manager.min.js} */
    IMAGE_MANAGER("image_manager", "imageManager"),

    /** {@code js/plugins/import_from_word.min.js} */
    IMPORT_FROM_WORD("import_from_word", "importFromWord"),

    /** {@code js/plugins/inline_class.min.js} */
    INLINE_CLASS("inline_class", "inlineClass"),

    /** {@code js/plugins/inline_style.min.js} */
    INLINE_STYLE("inline_style", "inlineStyle"),

    /** {@code js/plugins/line_breaker.min.js} */
    LINE_BREAKER("line_breaker", "lineBreaker"),

    /** {@code js/plugins/line_height.min.js} */
    LINE_HEIGHT("line_height", "lineHeight"),

    /** {@code js/plugins/link.min.js} */
    LINK("link", "link"),

    /** {@code js/plugins/link_to_anchor.min.js} */
    LINK_TO_ANCHOR("link_to_anchor", "linkToAnchor"),

    /** {@code js/plugins/lists.min.js} */
    LISTS("lists", "lists"),

    /** {@code js/plugins/markdown.min.js} */
    MARKDOWN("markdown", "markdown"),

    /** {@code js/plugins/page_break.min.js} */
    PAGE_BREAK("page_break", "pageBreak"),

    /** {@code js/plugins/paragraph_format.min.js} */
    PARAGRAPH_FORMAT("paragraph_format", "paragraphFormat"),

    /** {@code js/plugins/paragraph_style.min.js} */
    PARAGRAPH_STYLE("paragraph_style", "paragraphStyle"),

    /** {@code js/plugins/print.min.js} */
    PRINT("print", "print"),

    /** {@code js/plugins/quick_insert.min.js} */
    QUICK_INSERT("quick_insert", "quickInsert"),

    /** {@code js/plugins/quote.min.js} */
    QUOTE("quote", "quote"),

    /** {@code js/plugins/save.min.js} */
    SAVE("save", "save"),

    /** {@code js/plugins/special_characters.min.js} */
    SPECIAL_CHARACTERS("special_characters", "specialCharacters"),

    /** {@code js/plugins/table.min.js} */
    TABLE("table", "table"),

    /** {@code js/plugins/track_changes.min.js} */
    TRACK_CHANGES("track_changes", "track_changes"),

    /** {@code js/plugins/trim_video.min.js} */
    TRIM_VIDEO("trim_video", "trimVideoPlugin"),

    /** {@code js/plugins/url.min.js} */
    URL("url", "url"),

    /** {@code js/plugins/video.min.js} */
    VIDEO("video", "video"),

    /** {@code js/plugins/word_counter.min.js} */
    WORD_COUNTER("word_counter", "wordCounter"),

    /** {@code js/plugins/word_paste.min.js} */
    WORD_PASTE("word_paste", "wordPaste");

    private final String fileName;
    private final String pluginName;

    FroalaPlugin(String fileName, String pluginName) {
        this.fileName = fileName;
        this.pluginName = pluginName;
    }

    /**
     * Returns the plugin's file name without extension, as it appears under {@code js/plugins/} in the npm package.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the name the plugin registers itself under, which is what Froala's {@code pluginsEnabled} option takes.
     *
     * @return plugin name
     */
    public String getPluginName() {
        return pluginName;
    }
}
