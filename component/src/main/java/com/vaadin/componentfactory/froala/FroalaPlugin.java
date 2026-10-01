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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The names of Froala's plugins, for {@link FroalaOptions#withPluginsEnabled(String...)}. These are all plugin files of
 * froala-editor 5.4.0, the ones under {@code js/plugins/} and the five under {@code js/third_party/}. The browser
 * downloads a plugin's file only when an editor enables it. An editor without {@code pluginsEnabled} gets
 * {@link #basics()}. Passing a list disables every plugin not in it, and the toolbar buttons of a disabled plugin
 * disappear. See {@link FroalaOptions#withPluginsEnabled(java.util.Collection)}.
 *
 * <p>
 * Each constant holds the name the plugin registers itself under, which is what Froala's {@code pluginsEnabled}
 * expects. It differs from the file name, sometimes unpredictably. The file {@code find_and_replace.min.js} registers
 * {@code findReplace}.
 *
 * <p>
 * The constants are plain strings, so a plugin an application registers itself goes into the same list as a plain
 * string. The add-on only loads Froala's own plugin files. The application loads the file of its own plugin, for
 * example with {@code @JsModule}.
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
public final class FroalaPlugin {

    /**
     * Rewrites the selection, changes its tone or translates it, from a toolbar menu, and opens a chat panel for
     * free-form requests. Requires an endpoint in Froala's {@code aiAssistEndpoint} option, which has no default. Set
     * the option with {@link FroalaEditor#setOptions(String)}.
     */
    public static final String AI_ASSIST = "aiAssist";

    /**
     * Left, centre, right and justify alignment for the current block.
     */
    public static final String ALIGN = "align";

    /**
     * Counts characters below the editing area and can enforce a maximum.
     */
    public static final String CHAR_COUNTER = "charCounter";

    /**
     * Indents and formats the HTML shown in the code view. Has no toolbar button of its own.
     */
    public static final String CODE_BEAUTIFIER = "codeBeautifier";

    /**
     * Inserts a block of preformatted code.
     */
    public static final String CODE_SNIPPET = "codeSnippet";

    /**
     * Switches the editor between the rendered text and its HTML source.
     */
    public static final String CODE_VIEW = "codeView";

    /**
     * Several people editing one document, with cursors and comments. Requires a collaboration server to relay the
     * changes.
     */
    public static final String COLLABORATIVE = "collaborative";

    /**
     * Text and background colour for the selection.
     */
    public static final String COLORS = "colors";

    /**
     * Bundles the CryptoJS library, which the upload plugins use to sign uploads sent directly to Azure Blob Storage.
     * Has no toolbar button and no options of its own.
     */
    public static final String CRYPTO_JS = "cryptoJSPlugin";

    /**
     * Dragging images and other embedded content to another position in the text.
     */
    public static final String DRAGGABLE = "draggable";

    /**
     * Embeds a rich preview of a link, rendered by Embedly. Requires an Embedly account and loads Embedly's script from
     * {@code cdn.embedly.com}. Ships under {@code js/third_party/}.
     */
    public static final String EMBEDLY = "embedly";

    /**
     * An emoji picker. {@link FroalaOptions#withEmoticonsUseImage(boolean)} controls how the emoji are inserted.
     */
    public static final String EMOTICONS = "emoticons";

    /**
     * Writes non-ASCII characters as HTML entities when the value is read.
     */
    public static final String ENTITIES = "entities";

    /**
     * Downloads the content as a Word document. Generated in the browser, so it requires no service.
     */
    public static final String EXPORT_TO_WORD = "exportToWord";

    /**
     * Uploads a file and inserts a link to it. See {@link FroalaOptions#withFileUploadUrl(String)}.
     */
    public static final String FILE = "file";

    /**
     * Uploads and inserts several files at once, or inserts them by URL without uploading. Uploads go to the endpoint
     * in Froala's {@code filesManagerUploadURL} option, which has no default, or to S3 or Azure when configured.
     * Deleting in its dialog removes a file from the batch waiting to be uploaded, not from a server. Set the option
     * with {@link FroalaEditor#setOptions(String)}. Without an upload target the popup has no upload tab, because
     * Froala would insert a {@code blob:} URL that is dead after a reload.
     */
    public static final String FILES_MANAGER = "filesManager";

    /**
     * Inserts files through Filestack, a commercial file-handling service. Requires a Filestack account and API key,
     * set with {@link FroalaEditor#setOptions(String)}.
     */
    public static final String FILESTACK = "filestack";

    /**
     * Search and replace inside the edited text.
     */
    public static final String FIND_AND_REPLACE = "findReplace";

    /**
     * A picker that inserts Font Awesome icons. Requires the Font Awesome stylesheet on the page, which neither Froala
     * nor this add-on provides. Ships under {@code js/third_party/}.
     */
    public static final String FONT_AWESOME = "fontAwesome";

    /**
     * A font picker for the selection.
     */
    public static final String FONT_FAMILY = "fontFamily";

    /**
     * A font size picker for the selection.
     */
    public static final String FONT_SIZE = "fontSize";

    /**
     * Editing of form fields placed in the content, including their style and attributes.
     */
    public static final String FORMS = "forms";

    /**
     * A button that switches the editor to full screen.
     */
    public static final String FULLSCREEN = "fullscreen";

    /**
     * A dialog listing the keyboard shortcuts.
     */
    public static final String HELP = "help";

    /**
     * Inserting, uploading and editing images. See {@link FroalaOptions#withImageUploadUrl(String)}.
     */
    public static final String IMAGE = "image";

    /**
     * Opens an inserted image in the Filerobot image editor. Requires the Filerobot library on the page
     * ({@code window.FilerobotImageEditor}). Ships under {@code js/third_party/}.
     */
    public static final String IMAGE_FILEROBOT = "imageFilerobot";

    /**
     * A browser for images already uploaded. Requires a server endpoint that lists them, in Froala's
     * {@code imageManagerLoadURL} option, whose default points at Froala's own demo server. Set the option with
     * {@link FroalaEditor#setOptions(String)}.
     */
    public static final String IMAGE_MANAGER = "imageManager";

    /**
     * Opens an inserted image in the Toast UI image editor. Requires that library on the page ({@code window.tui}).
     * Ships under {@code js/third_party/}.
     */
    public static final String IMAGE_TUI = "imageTUI";

    /**
     * Reads a {@code .docx} file into the editor. Converts it in the browser using the third-party mammoth.js library,
     * which Froala does not bundle and expects on the page.
     */
    public static final String IMPORT_FROM_WORD = "importFromWord";

    /**
     * Applies a named CSS class to the selection, from a configurable list.
     */
    public static final String INLINE_CLASS = "inlineClass";

    /**
     * Applies a named inline style to the selection, from a configurable list.
     */
    public static final String INLINE_STYLE = "inlineStyle";

    /**
     * Adds a click target for starting a paragraph between two blocks that cannot otherwise be separated, such as two
     * tables.
     */
    public static final String LINE_BREAKER = "lineBreaker";

    /**
     * Line height for the current block.
     */
    public static final String LINE_HEIGHT = "lineHeight";

    /**
     * Inserting and editing links.
     */
    public static final String LINK = "link";

    /**
     * Linking to an anchor inside the same document.
     */
    public static final String LINK_TO_ANCHOR = "linkToAnchor";

    /**
     * Ordered and unordered lists, including indentation.
     */
    public static final String LISTS = "lists";

    /**
     * An additional markdown editing mode.
     */
    public static final String MARKDOWN = "markdown";

    /**
     * Inserts a page break for printing and for Word export.
     */
    public static final String PAGE_BREAK = "pageBreak";

    /**
     * Paragraph, heading and preformatted block types.
     */
    public static final String PARAGRAPH_FORMAT = "paragraphFormat";

    /**
     * Applies a named CSS class to the current block, from a configurable list.
     */
    public static final String PARAGRAPH_STYLE = "paragraphStyle";

    /**
     * A button that prints the content.
     */
    public static final String PRINT = "print";

    /**
     * A shortcut shown on an empty line for inserting an image, video, table or list.
     */
    public static final String QUICK_INSERT = "quickInsert";

    /**
     * Applies and removes block quotes.
     */
    public static final String QUOTE = "quote";

    /**
     * Posts the content to the endpoint in Froala's {@code saveURL} option on a timer. Requires that endpoint, which
     * has no method on {@link FroalaOptions} and is set with {@link FroalaEditor#setOptions(String)}. The timer is
     * {@link FroalaOptions#withSaveInterval(int)}.
     */
    public static final String SAVE = "save";

    /**
     * A picker for characters that are not on the keyboard.
     */
    public static final String SPECIAL_CHARACTERS = "specialCharacters";

    /**
     * Spell checking through WebSpellChecker's SCAYT service, which is a paid subscription. Loads its script from
     * {@code svc.webspellchecker.net}. Not the browser's own spell checker, which Froala's {@code spellcheck} option
     * switches and which works without this plugin. Ships under {@code js/third_party/}.
     */
    public static final String SPELL_CHECKER = "spellChecker";

    /**
     * Inserting and editing tables, including cell styling.
     */
    public static final String TABLE = "table";

    /**
     * Records insertions and deletions so they can be accepted or rejected.
     */
    public static final String TRACK_CHANGES = "track_changes";

    /**
     * Sets a start and end point on an inserted video. Works through the files manager, and sends the video to a
     * conversion server that froala-editor 5.4.0 hard-codes as {@code http://localhost:3000/convert}.
     */
    public static final String TRIM_VIDEO = "trimVideoPlugin";

    /**
     * Converts a URL or an email address into a link while the user types.
     */
    public static final String URL = "url";

    /**
     * Inserting and uploading videos, and embedding them from a URL. See
     * {@link FroalaOptions#withVideoUploadUrl(String)}.
     */
    public static final String VIDEO = "video";

    /**
     * Counts words below the editing area and can enforce a maximum.
     */
    public static final String WORD_COUNTER = "wordCounter";

    /**
     * Removes the markup Word puts on the clipboard when such content is pasted in.
     */
    public static final String WORD_PASTE = "wordPaste";

    /**
     * Returns the plugins an editor gets when its options name none. A basic rich-text editor: text and paragraph
     * formats, lists, quotes, links, find and replace, the keyboard shortcut dialog, links typed as URLs, and cleaning
     * of text pasted from Word. Nothing that inserts other content, such as images or tables, and no menu that only
     * offers Froala's sample styles. To add a plugin, change the returned set and pass it on:
     *
     * <pre>
     * Set&lt;String&gt; plugins = FroalaPlugin.basics();
     * plugins.add(FroalaPlugin.TABLE);
     * options.withPluginsEnabled(plugins);
     * </pre>
     *
     * @return a new set on every call, free to change
     */
    public static Set<String> basics() {
        return new LinkedHashSet<>(List.of(ALIGN, COLORS, FIND_AND_REPLACE, FONT_FAMILY, FONT_SIZE, HELP, LINE_HEIGHT,
                LINK, LINK_TO_ANCHOR, LISTS, PARAGRAPH_FORMAT, QUOTE, URL, WORD_PASTE));
    }

    /**
     * Returns every plugin listed here. An application's own plugins are not among them.
     *
     * @return a new set on every call, free to change
     */
    public static Set<String> all() {
        return new LinkedHashSet<>(List.of(AI_ASSIST, ALIGN, CHAR_COUNTER, CODE_BEAUTIFIER, CODE_SNIPPET, CODE_VIEW,
                COLLABORATIVE, COLORS, CRYPTO_JS, DRAGGABLE, EMBEDLY, EMOTICONS, ENTITIES, EXPORT_TO_WORD, FILE,
                FILES_MANAGER, FILESTACK, FIND_AND_REPLACE, FONT_AWESOME, FONT_FAMILY, FONT_SIZE, FORMS, FULLSCREEN,
                HELP, IMAGE, IMAGE_FILEROBOT, IMAGE_MANAGER, IMAGE_TUI, IMPORT_FROM_WORD, INLINE_CLASS, INLINE_STYLE,
                LINE_BREAKER, LINE_HEIGHT, LINK, LINK_TO_ANCHOR, LISTS, MARKDOWN, PAGE_BREAK, PARAGRAPH_FORMAT,
                PARAGRAPH_STYLE, PRINT, QUICK_INSERT, QUOTE, SAVE, SPECIAL_CHARACTERS, SPELL_CHECKER, TABLE,
                TRACK_CHANGES, TRIM_VIDEO, URL, VIDEO, WORD_COUNTER, WORD_PASTE));
    }

    private FroalaPlugin() {
    }
}
