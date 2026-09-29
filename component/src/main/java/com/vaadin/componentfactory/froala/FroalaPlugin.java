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

/**
 * The Froala plugins that {@link FroalaOptions#withPluginsEnabled(FroalaPlugin...)} accepts. These are all plugin files
 * of froala-editor 5.4.0, the ones under {@code js/plugins/} and the five under {@code js/third_party/}. The browser
 * downloads a plugin's file only when an editor enables it. An editor without {@code pluginsEnabled} gets
 * {@link #basics()}. Passing a list disables every plugin not in it, and the toolbar buttons of a disabled plugin
 * disappear. See {@link FroalaOptions#withPluginsEnabled(java.util.Collection)}.
 *
 * <p>
 * A plugin's file name and its registered name differ, sometimes unpredictably ({@code find_and_replace} registers as
 * {@code findReplace}). Both are listed. See {@link #getFileName()} and {@link #getPluginName()}.
 *
 * <p>
 * Six of them need a server endpoint or an account that neither Froala nor this add-on provides, and do nothing without
 * it. These are {@link #AI_ASSIST}, {@link #COLLABORATIVE}, {@link #FILESTACK}, {@link #IMAGE_MANAGER}, {@link #SAVE},
 * and {@link #FILES_MANAGER} for uploading, though its by-URL tab works without one. {@link #IMPORT_FROM_WORD} needs
 * the third-party mammoth.js script in the page instead, and the five under {@code js/third_party/} each need a library
 * or a service of their own. The documentation of each names what it needs, and the options they read have no methods
 * on {@link FroalaOptions}. Pass them with {@link FroalaEditor#setOptions(String)}.
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
     */
    AI_ASSIST("ai_assist", "aiAssist"),

    /**
     * Left, centre, right and justify alignment for the current block.
     */
    ALIGN("align", "align"),

    /**
     * Counts characters below the editing area and can enforce a maximum.
     */
    CHAR_COUNTER("char_counter", "charCounter"),

    /**
     * Indents and formats the HTML shown in the code view. Has no toolbar button of its own.
     */
    CODE_BEAUTIFIER("code_beautifier", "codeBeautifier"),

    /**
     * Inserts a block of preformatted code.
     */
    CODE_SNIPPET("code_snippet", "codeSnippet"),

    /**
     * Switches the editor between the rendered text and its HTML source.
     */
    CODE_VIEW("code_view", "codeView"),

    /**
     * Several people editing one document, with cursors and comments. Requires a collaboration server to relay the
     * changes.
     */
    COLLABORATIVE("collaborative", "collaborative"),

    /**
     * Text and background colour for the selection.
     */
    COLORS("colors", "colors"),

    /**
     * Bundles the CryptoJS library, which the upload plugins use to sign uploads sent directly to Azure Blob Storage.
     * Has no toolbar button and no options of its own.
     */
    CRYPTOJS("cryptojs", "cryptoJSPlugin"),

    /**
     * Dragging images and other embedded content to another position in the text.
     */
    DRAGGABLE("draggable", "draggable"),

    /**
     * Embeds a rich preview of a link, rendered by Embedly. Requires an Embedly account and loads Embedly's script from
     * {@code cdn.embedly.com}. Ships under {@code js/third_party/}.
     */
    EMBEDLY("embedly", "embedly"),

    /**
     * An emoji picker. {@link FroalaOptions#withEmoticonsUseImage(boolean)} controls how the emoji are inserted.
     */
    EMOTICONS("emoticons", "emoticons"),

    /**
     * Writes non-ASCII characters as HTML entities when the value is read.
     */
    ENTITIES("entities", "entities"),

    /**
     * Downloads the content as a Word document. Generated in the browser, so it requires no service.
     */
    EXPORT_TO_WORD("export_to_word", "exportToWord"),

    /**
     * Uploads a file and inserts a link to it. See {@link FroalaOptions#withFileUploadUrl(String)}.
     */
    FILE("file", "file"),

    /**
     * Uploads and inserts several files at once, or inserts them by URL without uploading. Uploads go to the endpoint
     * in Froala's {@code filesManagerUploadURL} option, which has no default, or to S3 or Azure when configured.
     * Deleting in its dialog removes a file from the batch waiting to be uploaded, not from a server. Set the option
     * with {@link FroalaEditor#setOptions(String)}.
     */
    FILES_MANAGER("files_manager", "filesManager"),

    /**
     * Inserts files through Filestack, a commercial file-handling service. Requires a Filestack account and API key,
     * set with {@link FroalaEditor#setOptions(String)}.
     */
    FILESTACK("filestack", "filestack"),

    /**
     * Search and replace inside the edited text.
     */
    FIND_AND_REPLACE("find_and_replace", "findReplace"),

    /**
     * A picker that inserts Font Awesome icons. Requires the Font Awesome stylesheet on the page, which neither Froala
     * nor this add-on provides. Ships under {@code js/third_party/}.
     */
    FONT_AWESOME("font_awesome", "fontAwesome"),

    /**
     * A font picker for the selection.
     */
    FONT_FAMILY("font_family", "fontFamily"),

    /**
     * A font size picker for the selection.
     */
    FONT_SIZE("font_size", "fontSize"),

    /**
     * Editing of form fields placed in the content, including their style and attributes.
     */
    FORMS("forms", "forms"),

    /**
     * A button that switches the editor to full screen.
     */
    FULLSCREEN("fullscreen", "fullscreen"),

    /**
     * A dialog listing the keyboard shortcuts.
     */
    HELP("help", "help"),

    /**
     * Inserting, uploading and editing images. See {@link FroalaOptions#withImageUploadUrl(String)}.
     */
    IMAGE("image", "image"),

    /**
     * Opens an inserted image in the Filerobot image editor. Requires the Filerobot library on the page
     * ({@code window.FilerobotImageEditor}). Ships under {@code js/third_party/}.
     */
    IMAGE_FILEROBOT("imageFileRobot", "imageFilerobot"),

    /**
     * A browser for images already uploaded. Requires a server endpoint that lists them, in Froala's
     * {@code imageManagerLoadURL} option, whose default points at Froala's own demo server. Set the option with
     * {@link FroalaEditor#setOptions(String)}.
     */
    IMAGE_MANAGER("image_manager", "imageManager"),

    /**
     * Opens an inserted image in the Toast UI image editor. Requires that library on the page ({@code window.tui}).
     * Ships under {@code js/third_party/}.
     */
    IMAGE_TUI("image_tui", "imageTUI"),

    /**
     * Reads a {@code .docx} file into the editor. Converts it in the browser using the third-party mammoth.js library,
     * which Froala does not bundle and expects on the page.
     */
    IMPORT_FROM_WORD("import_from_word", "importFromWord"),

    /**
     * Applies a named CSS class to the selection, from a configurable list.
     */
    INLINE_CLASS("inline_class", "inlineClass"),

    /**
     * Applies a named inline style to the selection, from a configurable list.
     */
    INLINE_STYLE("inline_style", "inlineStyle"),

    /**
     * Adds a click target for starting a paragraph between two blocks that cannot otherwise be separated, such as two
     * tables.
     */
    LINE_BREAKER("line_breaker", "lineBreaker"),

    /**
     * Line height for the current block.
     */
    LINE_HEIGHT("line_height", "lineHeight"),

    /**
     * Inserting and editing links.
     */
    LINK("link", "link"),

    /**
     * Linking to an anchor inside the same document.
     */
    LINK_TO_ANCHOR("link_to_anchor", "linkToAnchor"),

    /**
     * Ordered and unordered lists, including indentation.
     */
    LISTS("lists", "lists"),

    /**
     * An additional markdown editing mode.
     */
    MARKDOWN("markdown", "markdown"),

    /**
     * Inserts a page break for printing and for Word export.
     */
    PAGE_BREAK("page_break", "pageBreak"),

    /**
     * Paragraph, heading and preformatted block types.
     */
    PARAGRAPH_FORMAT("paragraph_format", "paragraphFormat"),

    /**
     * Applies a named CSS class to the current block, from a configurable list.
     */
    PARAGRAPH_STYLE("paragraph_style", "paragraphStyle"),

    /**
     * A button that prints the content.
     */
    PRINT("print", "print"),

    /**
     * A shortcut shown on an empty line for inserting an image, video, table or list.
     */
    QUICK_INSERT("quick_insert", "quickInsert"),

    /**
     * Applies and removes block quotes.
     */
    QUOTE("quote", "quote"),

    /**
     * Posts the content to the endpoint in Froala's {@code saveURL} option on a timer. Requires that endpoint, which
     * has no method on {@link FroalaOptions} and is set with {@link FroalaEditor#setOptions(String)}. The timer is
     * {@link FroalaOptions#withSaveInterval(int)}.
     */
    SAVE("save", "save"),

    /**
     * A picker for characters that are not on the keyboard.
     */
    SPECIAL_CHARACTERS("special_characters", "specialCharacters"),

    /**
     * Spell checking through WebSpellChecker's SCAYT service, which is a paid subscription. Loads its script from
     * {@code svc.webspellchecker.net}. Not the browser's own spell checker, which Froala's {@code spellcheck} option
     * switches and which works without this plugin. Ships under {@code js/third_party/}.
     */
    SPELL_CHECKER("spell_checker", "spellChecker"),

    /**
     * Inserting and editing tables, including cell styling.
     */
    TABLE("table", "table"),

    /**
     * Records insertions and deletions so they can be accepted or rejected.
     */
    TRACK_CHANGES("track_changes", "track_changes"),

    /**
     * Sets a start and end point on an inserted video. Works through the files manager, and sends the video to a
     * conversion server that froala-editor 5.4.0 hard-codes as {@code http://localhost:3000/convert}.
     */
    TRIM_VIDEO("trim_video", "trimVideoPlugin"),

    /**
     * Converts a URL or an email address into a link while the user types.
     */
    URL("url", "url"),

    /**
     * Inserting and uploading videos, and embedding them from a URL. See
     * {@link FroalaOptions#withVideoUploadUrl(String)}.
     */
    VIDEO("video", "video"),

    /**
     * Counts words below the editing area and can enforce a maximum.
     */
    WORD_COUNTER("word_counter", "wordCounter"),

    /**
     * Removes the markup Word puts on the clipboard when such content is pasted in.
     */
    WORD_PASTE("word_paste", "wordPaste");

    private final String fileName;
    private final String pluginName;

    FroalaPlugin(String fileName, String pluginName) {
        this.fileName = fileName;
        this.pluginName = pluginName;
    }

    /**
     * Returns the plugins an editor gets when its options name none. A basic rich-text editor: text and paragraph
     * formats, lists, quotes, links, find and replace, the keyboard shortcut dialog, links typed as URLs, and cleaning
     * of text pasted from Word. Nothing that inserts other content, such as images or tables, and no menu that only
     * offers Froala's sample styles. To add a plugin, change the returned set and pass it on:
     *
     * <pre>
     * EnumSet&lt;FroalaPlugin&gt; plugins = FroalaPlugin.basics();
     * plugins.add(FroalaPlugin.TABLE);
     * options.withPluginsEnabled(plugins);
     * </pre>
     *
     * @return a new set on every call, free to change
     */
    public static EnumSet<FroalaPlugin> basics() {
        return EnumSet.of(ALIGN, COLORS, FIND_AND_REPLACE, FONT_FAMILY, FONT_SIZE, HELP, LINE_HEIGHT, LINK,
                LINK_TO_ANCHOR, LISTS, PARAGRAPH_FORMAT, QUOTE, URL, WORD_PASTE);
    }

    /**
     * Returns every plugin, like {@link EnumSet#allOf(Class)}.
     *
     * @return a new set on every call, free to change
     */
    public static EnumSet<FroalaPlugin> all() {
        return EnumSet.allOf(FroalaPlugin.class);
    }

    /**
     * Returns the plugin's file name without extension, as it appears under {@code js/plugins/} or
     * {@code js/third_party/} in the npm package.
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
