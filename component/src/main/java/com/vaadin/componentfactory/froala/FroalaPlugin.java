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
 * The Froala plugins that {@link FroalaOptions#withPluginsEnabled(FroalaPlugin...)} accepts: those shipped under
 * {@code js/plugins/} in froala-editor 5.4.0. Five more ship under {@code js/third_party/} and register themselves the
 * same way ({@code embedly}, {@code fontAwesome}, {@code imageFilerobot}, {@code imageTUI}, {@code spellChecker}). They
 * are not listed here; each needs a third-party library or service, and a list containing one has to be passed as raw
 * JSON through {@link FroalaEditor#setOptions(String)}. Passing a list disables every plugin not in it. A toolbar
 * button whose command declares a disabled plugin is dropped silently: it is neither drawn nor counted towards a
 * group's {@link FroalaToolbarGroup#withButtonsVisible(int)}. Commands that declare no plugin, among them {@code bold}
 * and {@code italic}, are drawn whatever this option holds.
 *
 * <p>
 * A plugin has two names and they are not derived from each other: the file it lives in
 * ({@code js/plugins/font_family.min.js}) and the name it registers itself under, which is the one Froala's
 * {@code pluginsEnabled} option takes ({@code fontFamily}). Most are the same word in two spellings, some are not
 * ({@code find_and_replace} registers as {@code findReplace}), two carry a {@code Plugin} suffix ({@code cryptojs}
 * registers as {@code cryptoJSPlugin}), and {@code track_changes} keeps its underscore where every other multi-word
 * plugin is camel-cased. Neither name can be derived from the other, which is why both are listed here.
 *
 * <p>
 * Six of them need a server endpoint or an account that neither Froala nor this add-on provides, and do nothing without
 * it: {@link #AI_ASSIST}, {@link #COLLABORATIVE}, {@link #FILESTACK}, {@link #IMAGE_MANAGER}, {@link #SAVE}, and
 * {@link #FILES_MANAGER} for uploading, though its by-URL tab works without one. {@link #IMPORT_FROM_WORD} needs the
 * third-party mammoth.js script in the page instead. The documentation of each names what it needs, and the options
 * they read have no methods on {@link FroalaOptions}; pass them with {@link FroalaEditor#setOptions(String)}.
 *
 * <p>
 * {@code edit_in_popup} ships as a plugin file but registers a module, not a plugin, so {@code pluginsEnabled} has no
 * say over it and it is not listed here.
 */
public enum FroalaPlugin {

    /**
     * Rewrites the selection, changes its tone or translates it, from a toolbar menu, and opens a chat panel for
     * free-form requests. Requires an endpoint in Froala's {@code aiAssistEndpoint} option, which has no default. Set
     * the option with {@link FroalaEditor#setOptions(String)}.
     *
     * <p>
     * {@code js/plugins/ai_assist.min.js}
     */
    AI_ASSIST("ai_assist", "aiAssist"),

    /**
     * Left, centre, right and justify alignment for the current block.
     *
     * <p>
     * {@code js/plugins/align.min.js}
     */
    ALIGN("align", "align"),

    /**
     * Counts characters below the editing area and can enforce a maximum.
     *
     * <p>
     * {@code js/plugins/char_counter.min.js}
     */
    CHAR_COUNTER("char_counter", "charCounter"),

    /**
     * Indents and formats the HTML shown in the code view. Has no toolbar button of its own.
     *
     * <p>
     * {@code js/plugins/code_beautifier.min.js}
     */
    CODE_BEAUTIFIER("code_beautifier", "codeBeautifier"),

    /**
     * Inserts a block of preformatted code.
     *
     * <p>
     * {@code js/plugins/code_snippet.min.js}
     */
    CODE_SNIPPET("code_snippet", "codeSnippet"),

    /**
     * Switches the editor between the rendered text and its HTML source.
     *
     * <p>
     * {@code js/plugins/code_view.min.js}
     */
    CODE_VIEW("code_view", "codeView"),

    /**
     * Several people editing one document, with cursors and comments. Requires a collaboration server to relay the
     * changes.
     *
     * <p>
     * {@code js/plugins/collaborative.min.js}
     */
    COLLABORATIVE("collaborative", "collaborative"),

    /**
     * Text and background colour for the selection.
     *
     * <p>
     * {@code js/plugins/colors.min.js}
     */
    COLORS("colors", "colors"),

    /**
     * Bundles the CryptoJS library, which the upload plugins use to sign uploads sent directly to Azure Blob Storage.
     * Has no toolbar button and no options of its own.
     *
     * <p>
     * {@code js/plugins/cryptojs.min.js}
     */
    CRYPTOJS("cryptojs", "cryptoJSPlugin"),

    /**
     * Dragging images and other embedded content to another position in the text.
     *
     * <p>
     * {@code js/plugins/draggable.min.js}
     */
    DRAGGABLE("draggable", "draggable"),

    /**
     * An emoji picker. {@link FroalaOptions#withEmoticonsUseImage(boolean)} controls how the emoji are inserted.
     *
     * <p>
     * {@code js/plugins/emoticons.min.js}
     */
    EMOTICONS("emoticons", "emoticons"),

    /**
     * Writes non-ASCII characters as HTML entities when the value is read.
     *
     * <p>
     * {@code js/plugins/entities.min.js}
     */
    ENTITIES("entities", "entities"),

    /**
     * Downloads the content as a Word document. Generated in the browser; requires no service.
     *
     * <p>
     * {@code js/plugins/export_to_word.min.js}
     */
    EXPORT_TO_WORD("export_to_word", "exportToWord"),

    /**
     * Uploads a file and inserts a link to it. See {@link FroalaOptions#withFileUploadUrl(String)}.
     *
     * <p>
     * {@code js/plugins/file.min.js}
     */
    FILE("file", "file"),

    /**
     * Uploads and inserts several files at once, or inserts them by URL without uploading. Uploads go to the endpoint
     * in Froala's {@code filesManagerUploadURL} option, which has no default, or to S3 or Azure when configured.
     * Deleting in its dialog removes a file from the batch waiting to be uploaded, not from a server. Set the option
     * with {@link FroalaEditor#setOptions(String)}.
     *
     * <p>
     * {@code js/plugins/files_manager.min.js}
     */
    FILES_MANAGER("files_manager", "filesManager"),

    /**
     * Inserts files through Filestack, a commercial file-handling service. Requires a Filestack account and API key,
     * set with {@link FroalaEditor#setOptions(String)}.
     *
     * <p>
     * {@code js/plugins/filestack.min.js}
     */
    FILESTACK("filestack", "filestack"),

    /**
     * Search and replace inside the edited text.
     *
     * <p>
     * {@code js/plugins/find_and_replace.min.js}
     */
    FIND_AND_REPLACE("find_and_replace", "findReplace"),

    /**
     * A font picker for the selection.
     *
     * <p>
     * {@code js/plugins/font_family.min.js}
     */
    FONT_FAMILY("font_family", "fontFamily"),

    /**
     * A font size picker for the selection.
     *
     * <p>
     * {@code js/plugins/font_size.min.js}
     */
    FONT_SIZE("font_size", "fontSize"),

    /**
     * Editing of form fields placed in the content, including their style and attributes.
     *
     * <p>
     * {@code js/plugins/forms.min.js}
     */
    FORMS("forms", "forms"),

    /**
     * A button that switches the editor to full screen.
     *
     * <p>
     * {@code js/plugins/fullscreen.min.js}
     */
    FULLSCREEN("fullscreen", "fullscreen"),

    /**
     * A dialog listing the keyboard shortcuts.
     *
     * <p>
     * {@code js/plugins/help.min.js}
     */
    HELP("help", "help"),

    /**
     * Inserting, uploading and editing images. See {@link FroalaOptions#withImageUploadUrl(String)}.
     *
     * <p>
     * {@code js/plugins/image.min.js}
     */
    IMAGE("image", "image"),

    /**
     * A browser for images already uploaded. Requires a server endpoint that lists them, in Froala's
     * {@code imageManagerLoadURL} option, whose default points at Froala's own demo server. Set the option with
     * {@link FroalaEditor#setOptions(String)}.
     *
     * <p>
     * {@code js/plugins/image_manager.min.js}
     */
    IMAGE_MANAGER("image_manager", "imageManager"),

    /**
     * Reads a {@code .docx} file into the editor. Converts it in the browser using the third-party mammoth.js library,
     * which Froala does not bundle and expects on the page.
     *
     * <p>
     * {@code js/plugins/import_from_word.min.js}
     */
    IMPORT_FROM_WORD("import_from_word", "importFromWord"),

    /**
     * Applies a named CSS class to the selection, from a configurable list.
     *
     * <p>
     * {@code js/plugins/inline_class.min.js}
     */
    INLINE_CLASS("inline_class", "inlineClass"),

    /**
     * Applies a named inline style to the selection, from a configurable list.
     *
     * <p>
     * {@code js/plugins/inline_style.min.js}
     */
    INLINE_STYLE("inline_style", "inlineStyle"),

    /**
     * Adds a click target for starting a paragraph between two blocks that cannot otherwise be separated, such as two
     * tables.
     *
     * <p>
     * {@code js/plugins/line_breaker.min.js}
     */
    LINE_BREAKER("line_breaker", "lineBreaker"),

    /**
     * Line height for the current block.
     *
     * <p>
     * {@code js/plugins/line_height.min.js}
     */
    LINE_HEIGHT("line_height", "lineHeight"),

    /**
     * Inserting and editing links.
     *
     * <p>
     * {@code js/plugins/link.min.js}
     */
    LINK("link", "link"),

    /**
     * Linking to an anchor inside the same document.
     *
     * <p>
     * {@code js/plugins/link_to_anchor.min.js}
     */
    LINK_TO_ANCHOR("link_to_anchor", "linkToAnchor"),

    /**
     * Ordered and unordered lists, including indentation.
     *
     * <p>
     * {@code js/plugins/lists.min.js}
     */
    LISTS("lists", "lists"),

    /**
     * An additional markdown editing mode.
     *
     * <p>
     * {@code js/plugins/markdown.min.js}
     */
    MARKDOWN("markdown", "markdown"),

    /**
     * Inserts a page break for printing and for Word export.
     *
     * <p>
     * {@code js/plugins/page_break.min.js}
     */
    PAGE_BREAK("page_break", "pageBreak"),

    /**
     * Paragraph, heading and preformatted block types.
     *
     * <p>
     * {@code js/plugins/paragraph_format.min.js}
     */
    PARAGRAPH_FORMAT("paragraph_format", "paragraphFormat"),

    /**
     * Applies a named CSS class to the current block, from a configurable list.
     *
     * <p>
     * {@code js/plugins/paragraph_style.min.js}
     */
    PARAGRAPH_STYLE("paragraph_style", "paragraphStyle"),

    /**
     * A button that prints the content.
     *
     * <p>
     * {@code js/plugins/print.min.js}
     */
    PRINT("print", "print"),

    /**
     * A shortcut shown on an empty line for inserting an image, video, table or list.
     *
     * <p>
     * {@code js/plugins/quick_insert.min.js}
     */
    QUICK_INSERT("quick_insert", "quickInsert"),

    /**
     * Applies and removes block quotes.
     *
     * <p>
     * {@code js/plugins/quote.min.js}
     */
    QUOTE("quote", "quote"),

    /**
     * Posts the content to the endpoint in Froala's {@code saveURL} option on a timer. Requires that endpoint, which
     * has no method on {@link FroalaOptions} and is set with {@link FroalaEditor#setOptions(String)}. The timer is
     * {@link FroalaOptions#withSaveInterval(int)}.
     *
     * <p>
     * {@code js/plugins/save.min.js}
     */
    SAVE("save", "save"),

    /**
     * A picker for characters that are not on the keyboard.
     *
     * <p>
     * {@code js/plugins/special_characters.min.js}
     */
    SPECIAL_CHARACTERS("special_characters", "specialCharacters"),

    /**
     * Inserting and editing tables, including cell styling.
     *
     * <p>
     * {@code js/plugins/table.min.js}
     */
    TABLE("table", "table"),

    /**
     * Records insertions and deletions so they can be accepted or rejected. The only plugin whose registered name keeps
     * its underscore.
     *
     * <p>
     * {@code js/plugins/track_changes.min.js}
     */
    TRACK_CHANGES("track_changes", "track_changes"),

    /**
     * Sets a start and end point on an inserted video. Works through the files manager.
     *
     * <p>
     * {@code js/plugins/trim_video.min.js}
     */
    TRIM_VIDEO("trim_video", "trimVideoPlugin"),

    /**
     * Converts a URL or an email address into a link while the user types.
     *
     * <p>
     * {@code js/plugins/url.min.js}
     */
    URL("url", "url"),

    /**
     * Inserting and uploading videos, and embedding them from a URL. See
     * {@link FroalaOptions#withVideoUploadUrl(String)}.
     *
     * <p>
     * {@code js/plugins/video.min.js}
     */
    VIDEO("video", "video"),

    /**
     * Counts words below the editing area and can enforce a maximum.
     *
     * <p>
     * {@code js/plugins/word_counter.min.js}
     */
    WORD_COUNTER("word_counter", "wordCounter"),

    /**
     * Removes the markup Word puts on the clipboard when such content is pasted in.
     *
     * <p>
     * {@code js/plugins/word_paste.min.js}
     */
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
     * @return the file name, for example {@code font_family}
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the name the plugin registers itself under. This is the name Froala's {@code pluginsEnabled} option
     * expects.
     *
     * @return the registered name, for example {@code fontFamily}
     */
    public String getPluginName() {
        return pluginName;
    }
}
