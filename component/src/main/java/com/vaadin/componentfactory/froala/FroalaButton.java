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
 * The names of Froala's toolbar and popup buttons, for {@link FroalaToolbar}, {@link FroalaToolbarGroup} and the button
 * lists of {@link FroalaOptions}. A button name is the name of the command it runs, so each constant holds the command
 * name froala-editor 5.4.0 registers.
 *
 * <pre>
 * FroalaToolbar.of(FroalaButton.BOLD, FroalaButton.ITALIC, FroalaButton.VERTICAL_SEPARATOR, "myCommand");
 * </pre>
 *
 * <p>
 * The constants are plain strings, so a command an application registers itself goes into the same list as a plain
 * string. A name Froala does not know is dropped silently. So is the button of a plugin that is not enabled.
 *
 * <p>
 * Listed are the 136 commands that appear in one of Froala's own default button lists, plus {@link #ALIGN},
 * {@link #SAVE}, {@link #AI_CHANGE_TONE} and {@link #AI_TRANSLATE_TO}, which Froala offers as buttons without placing
 * them anywhere. The other commands Froala registers run inside its popups and are not meant as buttons. Each constant
 * names the plugin that registers its command and the default lists it appears in.
 */
public final class FroalaButton {

    /**
     * Draws a vertical separator line inside a group or popup. In a flat {@link FroalaToolbar#of(String...)} it starts
     * a new group instead.
     */
    public static final String VERTICAL_SEPARATOR = "|";

    /**
     * Draws a horizontal separator inside a group or popup, which starts a new row. In a flat
     * {@link FroalaToolbar#of(String...)} it starts a new group instead.
     */
    public static final String HORIZONTAL_SEPARATOR = "-";

    // -----------------------------------------------------------------------------------------------------------
    // Commands of Froala's core
    // -----------------------------------------------------------------------------------------------------------

    /** Bold. Needs no plugin. In Froala's default toolbar. */
    public static final String BOLD = "bold";

    /** Italic. Needs no plugin. In Froala's default toolbar. */
    public static final String ITALIC = "italic";

    /** Underline. Needs no plugin. In Froala's default toolbar. */
    public static final String UNDERLINE = "underline";

    /** Strikethrough. Needs no plugin. In Froala's default toolbar. */
    public static final String STRIKE_THROUGH = "strikeThrough";

    /** Subscript. Needs no plugin. In Froala's default toolbar. */
    public static final String SUBSCRIPT = "subscript";

    /** Superscript. Needs no plugin. In Froala's default toolbar. */
    public static final String SUPERSCRIPT = "superscript";

    /** Decrease Indent. Needs no plugin. In Froala's default toolbar. */
    public static final String OUTDENT = "outdent";

    /** Increase Indent. Needs no plugin. In Froala's default toolbar. */
    public static final String INDENT = "indent";

    /** Undo. Needs no plugin. In Froala's default toolbar. */
    public static final String UNDO = "undo";

    /** Redo. Needs no plugin. In Froala's default toolbar. */
    public static final String REDO = "redo";

    /** Insert Horizontal Line. Needs no plugin. In Froala's default toolbar. */
    public static final String INSERT_HR = "insertHR";

    /** Clear Formatting. Needs no plugin. In Froala's default toolbar. */
    public static final String CLEAR_FORMATTING = "clearFormatting";

    /** Select All. Needs no plugin. In Froala's default toolbar. */
    public static final String SELECT_ALL = "selectAll";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.AI_ASSIST
    // -----------------------------------------------------------------------------------------------------------

    /** Improve Writing. Needs {@link FroalaPlugin#AI_ASSIST}. In {@code selectionActionButtons}. */
    public static final String AI_IMPROVE_WRITING = "aiImproveWriting";

    /**
     * Ask AI to generate or refine text, adjusting its tone and language. Needs {@link FroalaPlugin#AI_ASSIST}. In
     * Froala's default toolbar.
     */
    public static final String AI_ASSIST = "aiAssist";

    /** Open the AI Chat Assistant panel. Needs {@link FroalaPlugin#AI_ASSIST}. In Froala's default toolbar. */
    public static final String AI_CHAT_ASSISTANT = "aiChatAssistant";

    /** AI Shortcuts. Needs {@link FroalaPlugin#AI_ASSIST}. In Froala's default toolbar. */
    public static final String AI_SHORT_CUTS = "aiShortCuts";

    /** Change Tone. Needs {@link FroalaPlugin#AI_ASSIST}. */
    public static final String AI_CHANGE_TONE = "aiChangeTone";

    /** Translate To. Needs {@link FroalaPlugin#AI_ASSIST}. */
    public static final String AI_TRANSLATE_TO = "aiTranslateTo";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.ALIGN
    // -----------------------------------------------------------------------------------------------------------

    /** Align. Needs {@link FroalaPlugin#ALIGN}. */
    public static final String ALIGN = "align";

    /** Align Left. Needs {@link FroalaPlugin#ALIGN}. In Froala's default toolbar. */
    public static final String ALIGN_LEFT = "alignLeft";

    /** Align Right. Needs {@link FroalaPlugin#ALIGN}. In Froala's default toolbar. */
    public static final String ALIGN_RIGHT = "alignRight";

    /** Align Center. Needs {@link FroalaPlugin#ALIGN}. In Froala's default toolbar. */
    public static final String ALIGN_CENTER = "alignCenter";

    /** Align Justify. Needs {@link FroalaPlugin#ALIGN}. In Froala's default toolbar. */
    public static final String ALIGN_JUSTIFY = "alignJustify";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.CODE_SNIPPET
    // -----------------------------------------------------------------------------------------------------------

    /** Code Snippet. Needs {@link FroalaPlugin#CODE_SNIPPET}. In Froala's default toolbar. */
    public static final String CODE_SNIPPET = "codeSnippet";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.CODE_VIEW
    // -----------------------------------------------------------------------------------------------------------

    /** Code View. Needs {@link FroalaPlugin#CODE_VIEW}. In Froala's default toolbar. */
    public static final String HTML = "html";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.COLLABORATIVE
    // -----------------------------------------------------------------------------------------------------------

    /** Switch Mode. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar. */
    public static final String COLLAB_MODE = "collabMode";

    /** The {@code collabPresence} button. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar. */
    public static final String COLLAB_PRESENCE = "collabPresence";

    /**
     * Add Comment. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar and
     * {@code selectionActionButtons}.
     */
    public static final String COLLAB_ADD_COMMENT = "collabAddComment";

    /** Comments. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar. */
    public static final String COLLAB_PANEL = "collabPanel";

    /** Version Control. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar. */
    public static final String VERSION_CONTROL = "versionControl";

    /** The {@code autoSaveStatus} button. Needs {@link FroalaPlugin#COLLABORATIVE}. In Froala's default toolbar. */
    public static final String AUTO_SAVE_STATUS = "autoSaveStatus";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.COLORS
    // -----------------------------------------------------------------------------------------------------------

    /** Text Color. Needs {@link FroalaPlugin#COLORS}. In Froala's default toolbar. */
    public static final String TEXT_COLOR = "textColor";

    /** Background Color. Needs {@link FroalaPlugin#COLORS}. In Froala's default toolbar. */
    public static final String BACKGROUND_COLOR = "backgroundColor";

    /** Back. Needs {@link FroalaPlugin#COLORS}. In {@code colorsButtons}. */
    public static final String COLORS_BACK = "colorsBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.EMOTICONS
    // -----------------------------------------------------------------------------------------------------------

    /** Emoticons. Needs {@link FroalaPlugin#EMOTICONS}. In Froala's default toolbar. */
    public static final String EMOTICONS = "emoticons";

    /** Back. Needs {@link FroalaPlugin#EMOTICONS}. In {@code emoticonsButtons}. */
    public static final String EMOTICONS_BACK = "emoticonsBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.EXPORT_TO_WORD
    // -----------------------------------------------------------------------------------------------------------

    /** Export to Word. Needs {@link FroalaPlugin#EXPORT_TO_WORD}. In Froala's default toolbar. */
    public static final String EXPORT_TO_WORD = "export_to_word";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FILE
    // -----------------------------------------------------------------------------------------------------------

    /** Upload File. Needs {@link FroalaPlugin#FILE}. In Froala's default toolbar. */
    public static final String INSERT_FILE = "insertFile";

    /** Back. Needs {@link FroalaPlugin#FILE}. In {@code fileInsertButtons}. */
    public static final String FILE_BACK = "fileBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FILES_MANAGER
    // -----------------------------------------------------------------------------------------------------------

    /** Insert Files. Needs {@link FroalaPlugin#FILES_MANAGER}. In Froala's default toolbar. */
    public static final String INSERT_FILES = "insertFiles";

    /** Upload Files. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons}. */
    public static final String FILES_UPLOAD = "filesUpload";

    /** By URL. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons}. */
    public static final String FILES_BY_URL = "filesByURL";

    /** Embedded Code. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons}. */
    public static final String FILES_EMBED = "filesEmbed";

    /** Insert. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons2}. */
    public static final String INSERT_ALL = "insertAll";

    /** Delete. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons2}. */
    public static final String DELETE_ALL = "deleteAll";

    /** Cancel. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons2}. */
    public static final String CANCEL = "cancel";

    /** Minimize. Needs {@link FroalaPlugin#FILES_MANAGER}. In {@code filesInsertButtons2}. */
    public static final String MINIMIZE = "minimize";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FILESTACK
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Open Filestack File Picker. Needs {@link FroalaPlugin#FILESTACK}. In Froala's default toolbar and
     * {@code filesInsertButtons}.
     */
    public static final String OPEN_FILE_PICKER = "openFilePicker";

    /** Open Filestack Image Picker. Needs {@link FroalaPlugin#FILESTACK}. In {@code imageInsertButtons}. */
    public static final String OPEN_FILE_PICKER_IMAGE = "openFilePickerImage";

    /** Open Filestack Video Picker. Needs {@link FroalaPlugin#FILESTACK}. In {@code videoInsertButtons}. */
    public static final String OPEN_FILE_PICKER_VIDEO = "openFilePickerVideo";

    /** Image Transformations. Needs {@link FroalaPlugin#FILESTACK}. In {@code imageEditButtons}. */
    public static final String FILESTACK_ICON = "filestackIcon";

    /** Open Filestack Upload File. Needs {@link FroalaPlugin#FILESTACK}. In {@code fileInsertButtons}. */
    public static final String OPEN_FILE_PICKER_FILE = "openFilePickerFile";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FIND_AND_REPLACE
    // -----------------------------------------------------------------------------------------------------------

    /** Find and Replace. Needs {@link FroalaPlugin#FIND_AND_REPLACE}. In Froala's default toolbar. */
    public static final String FIND_REPLACE_BUTTON = "findReplaceButton";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FONT_FAMILY
    // -----------------------------------------------------------------------------------------------------------

    /** Font Family. Needs {@link FroalaPlugin#FONT_FAMILY}. In Froala's default toolbar. */
    public static final String FONT_FAMILY = "fontFamily";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FONT_SIZE
    // -----------------------------------------------------------------------------------------------------------

    /** Font Size. Needs {@link FroalaPlugin#FONT_SIZE}. In Froala's default toolbar. */
    public static final String FONT_SIZE = "fontSize";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FORMS
    // -----------------------------------------------------------------------------------------------------------

    /** Style. Needs {@link FroalaPlugin#FORMS}. In {@code formEditButtons}. */
    public static final String INPUT_STYLE = "inputStyle";

    /** Edit Button. Needs {@link FroalaPlugin#FORMS}. In {@code formEditButtons}. */
    public static final String INPUT_EDIT = "inputEdit";

    /** Back. Needs {@link FroalaPlugin#FORMS}. In {@code formUpdateButtons}. */
    public static final String INPUT_BACK = "inputBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FULLSCREEN
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Fullscreen. Needs {@link FroalaPlugin#FULLSCREEN}. In Froala's default toolbar and
     * {@code codeViewKeepActiveButtons}.
     */
    public static final String FULLSCREEN = "fullscreen";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.HELP
    // -----------------------------------------------------------------------------------------------------------

    /** Help. Needs {@link FroalaPlugin#HELP}. In Froala's default toolbar. */
    public static final String HELP = "help";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.IMAGE
    // -----------------------------------------------------------------------------------------------------------

    /** Insert Image. Needs {@link FroalaPlugin#IMAGE}. In Froala's default toolbar. */
    public static final String INSERT_IMAGE = "insertImage";

    /** Upload Image. Needs {@link FroalaPlugin#IMAGE}. In {@code imageInsertButtons}. */
    public static final String IMAGE_UPLOAD = "imageUpload";

    /** By URL. Needs {@link FroalaPlugin#IMAGE}. In {@code imageInsertButtons}. */
    public static final String IMAGE_BY_URL = "imageByURL";

    /** Display. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_DISPLAY = "imageDisplay";

    /** Align. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_ALIGN = "imageAlign";

    /** Replace. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_REPLACE = "imageReplace";

    /** Remove. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_REMOVE = "imageRemove";

    /**
     * Back. Needs {@link FroalaPlugin#IMAGE}. In {@code filesInsertButtons}, {@code imageAltButtons},
     * {@code imageSizeButtons} and {@code imageInsertButtons}.
     */
    public static final String IMAGE_BACK = "imageBack";

    /** Style. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_STYLE = "imageStyle";

    /** Alternative Text. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_ALT = "imageAlt";

    /** Change Size. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_SIZE = "imageSize";

    /** Image Caption. Needs {@link FroalaPlugin#IMAGE}. In {@code imageEditButtons}. */
    public static final String IMAGE_CAPTION = "imageCaption";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.IMAGE_MANAGER
    // -----------------------------------------------------------------------------------------------------------

    /** Browse. Needs {@link FroalaPlugin#IMAGE_MANAGER}. In {@code imageInsertButtons}. */
    public static final String IMAGE_MANAGER = "imageManager";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.IMPORT_FROM_WORD
    // -----------------------------------------------------------------------------------------------------------

    /** Import from Word. Needs {@link FroalaPlugin#IMPORT_FROM_WORD}. In Froala's default toolbar. */
    public static final String IMPORT_FROM_WORD = "import_from_word";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.INLINE_CLASS
    // -----------------------------------------------------------------------------------------------------------

    /** Inline Class. Needs {@link FroalaPlugin#INLINE_CLASS}. In Froala's default toolbar. */
    public static final String INLINE_CLASS = "inlineClass";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.INLINE_STYLE
    // -----------------------------------------------------------------------------------------------------------

    /** Inline Style. Needs {@link FroalaPlugin#INLINE_STYLE}. In Froala's default toolbar. */
    public static final String INLINE_STYLE = "inlineStyle";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.LINE_HEIGHT
    // -----------------------------------------------------------------------------------------------------------

    /** Line Height. Needs {@link FroalaPlugin#LINE_HEIGHT}. In Froala's default toolbar. */
    public static final String LINE_HEIGHT = "lineHeight";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.LINK
    // -----------------------------------------------------------------------------------------------------------

    /** Insert Link. Needs {@link FroalaPlugin#LINK}. In Froala's default toolbar. */
    public static final String INSERT_LINK = "insertLink";

    /** Open Link. Needs {@link FroalaPlugin#LINK}. In {@code imageEditButtons} and {@code linkEditButtons}. */
    public static final String LINK_OPEN = "linkOpen";

    /** Edit Link. Needs {@link FroalaPlugin#LINK}. In {@code imageEditButtons} and {@code linkEditButtons}. */
    public static final String LINK_EDIT = "linkEdit";

    /** Unlink. Needs {@link FroalaPlugin#LINK}. In {@code imageEditButtons} and {@code linkEditButtons}. */
    public static final String LINK_REMOVE = "linkRemove";

    /** Back. Needs {@link FroalaPlugin#LINK}. In {@code linkInsertButtons}. */
    public static final String LINK_BACK = "linkBack";

    /** Choose Link. Needs {@link FroalaPlugin#LINK}. In {@code linkInsertButtons}. */
    public static final String LINK_LIST = "linkList";

    /** Insert Link. Needs {@link FroalaPlugin#LINK}. In {@code imageEditButtons}. */
    public static final String IMAGE_LINK = "imageLink";

    /** Style. Needs {@link FroalaPlugin#LINK}. In {@code linkEditButtons}. */
    public static final String LINK_STYLE = "linkStyle";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.LINK_TO_ANCHOR
    // -----------------------------------------------------------------------------------------------------------

    /** Insert Anchor. Needs {@link FroalaPlugin#LINK_TO_ANCHOR}. In Froala's default toolbar. */
    public static final String INSERT_ANCHOR = "insertAnchor";

    /** Edit Anchor. Needs {@link FroalaPlugin#LINK_TO_ANCHOR}. In {@code anchorEditButtons}. */
    public static final String ANCHOR_EDIT = "anchorEdit";

    /** Delete. Needs {@link FroalaPlugin#LINK_TO_ANCHOR}. In {@code anchorEditButtons}. */
    public static final String ANCHOR_REMOVE = "anchorRemove";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.LISTS
    // -----------------------------------------------------------------------------------------------------------

    /** Ordered List. Needs {@link FroalaPlugin#LISTS}. In Froala's default toolbar. */
    public static final String FORMAT_OL_SIMPLE = "formatOLSimple";

    /** Unordered List. Needs {@link FroalaPlugin#LISTS}. In Froala's default toolbar. */
    public static final String FORMAT_UL = "formatUL";

    /** Ordered List. Needs {@link FroalaPlugin#LISTS}. In Froala's default toolbar. */
    public static final String FORMAT_OL = "formatOL";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.MARKDOWN
    // -----------------------------------------------------------------------------------------------------------

    /** Markdown. Needs {@link FroalaPlugin#MARKDOWN}. In Froala's default toolbar. */
    public static final String MARKDOWN = "markdown";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.PAGE_BREAK
    // -----------------------------------------------------------------------------------------------------------

    /** Page Break. Needs {@link FroalaPlugin#PAGE_BREAK}. In Froala's default toolbar. */
    public static final String PAGE_BREAK = "pageBreak";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.PARAGRAPH_FORMAT
    // -----------------------------------------------------------------------------------------------------------

    /** Paragraph Format. Needs {@link FroalaPlugin#PARAGRAPH_FORMAT}. In Froala's default toolbar. */
    public static final String PARAGRAPH_FORMAT = "paragraphFormat";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.PARAGRAPH_STYLE
    // -----------------------------------------------------------------------------------------------------------

    /** Paragraph Style. Needs {@link FroalaPlugin#PARAGRAPH_STYLE}. In Froala's default toolbar. */
    public static final String PARAGRAPH_STYLE = "paragraphStyle";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.PRINT
    // -----------------------------------------------------------------------------------------------------------

    /** Print. Needs {@link FroalaPlugin#PRINT}. In Froala's default toolbar. */
    public static final String PRINT = "print";

    /** Download PDF. Needs {@link FroalaPlugin#PRINT}. In Froala's default toolbar. */
    public static final String GET_PDF = "getPDF";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.QUOTE
    // -----------------------------------------------------------------------------------------------------------

    /** Quote. Needs {@link FroalaPlugin#QUOTE}. In Froala's default toolbar. */
    public static final String QUOTE = "quote";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.SAVE
    // -----------------------------------------------------------------------------------------------------------

    /** Save. Needs {@link FroalaPlugin#SAVE}. */
    public static final String SAVE = "save";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.SPECIAL_CHARACTERS
    // -----------------------------------------------------------------------------------------------------------

    /** Special Characters. Needs {@link FroalaPlugin#SPECIAL_CHARACTERS}. In Froala's default toolbar. */
    public static final String SPECIAL_CHARACTERS = "specialCharacters";

    /** Back. Needs {@link FroalaPlugin#SPECIAL_CHARACTERS}. In {@code specialCharButtons}. */
    public static final String SPECIAL_CHAR_BACK = "specialCharBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.TABLE
    // -----------------------------------------------------------------------------------------------------------

    /** Table Properties. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_PROPERTIES = "tableProperties";

    /** Cell Properties. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_CELL_PROPERTIES = "tableCellProperties";

    /** Insert Table. Needs {@link FroalaPlugin#TABLE}. In Froala's default toolbar. */
    public static final String INSERT_TABLE = "insertTable";

    /** Table Header. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_HEADER = "tableHeader";

    /** Table Footer. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_FOOTER = "tableFooter";

    /** Row. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_ROWS = "tableRows";

    /** Column. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_COLUMNS = "tableColumns";

    /** Cell. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_CELLS = "tableCells";

    /** Remove Table. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_REMOVE = "tableRemove";

    /** Back. Needs {@link FroalaPlugin#TABLE}. In {@code tableInsertButtons} and {@code tableColorsButtons}. */
    public static final String TABLE_BACK = "tableBack";

    /** Vertical Align. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_CELL_VERTICAL_ALIGN = "tableCellVerticalAlign";

    /** Horizontal Align. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_CELL_HORIZONTAL_ALIGN = "tableCellHorizontalAlign";

    /** Cell Style. Needs {@link FroalaPlugin#TABLE}. In {@code tableEditButtons}. */
    public static final String TABLE_CELL_STYLE = "tableCellStyle";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.TRACK_CHANGES
    // -----------------------------------------------------------------------------------------------------------

    /** Enable Track Changes. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String TRACK_CHANGES = "trackChanges";

    /** Show Changes. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String SHOW_CHANGES = "showChanges";

    /** Accept All Changes. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String APPLY_ALL = "applyAll";

    /** Reject All Changes. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String REMOVE_ALL = "removeAll";

    /** Accept Single Change. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String APPLY_LAST = "applyLast";

    /** Reject Single Change. Needs {@link FroalaPlugin#TRACK_CHANGES}. In Froala's default toolbar. */
    public static final String REMOVE_LAST = "removeLast";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.VIDEO
    // -----------------------------------------------------------------------------------------------------------

    /** Insert Video. Needs {@link FroalaPlugin#VIDEO}. In Froala's default toolbar. */
    public static final String INSERT_VIDEO = "insertVideo";

    /** By URL. Needs {@link FroalaPlugin#VIDEO}. In {@code videoInsertButtons}. */
    public static final String VIDEO_BY_URL = "videoByURL";

    /** Embedded Code. Needs {@link FroalaPlugin#VIDEO}. In {@code videoInsertButtons}. */
    public static final String VIDEO_EMBED = "videoEmbed";

    /** Upload Video. Needs {@link FroalaPlugin#VIDEO}. In {@code videoInsertButtons}. */
    public static final String VIDEO_UPLOAD = "videoUpload";

    /** Display. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String VIDEO_DISPLAY = "videoDisplay";

    /** Align. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String VIDEO_ALIGN = "videoAlign";

    /** Replace. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String VIDEO_REPLACE = "videoReplace";

    /** Remove. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String VIDEO_REMOVE = "videoRemove";

    /** Autoplay. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String AUTOPLAY = "autoplay";

    /** Change Size. Needs {@link FroalaPlugin#VIDEO}. In {@code videoEditButtons}. */
    public static final String VIDEO_SIZE = "videoSize";

    /** Back. Needs {@link FroalaPlugin#VIDEO}. In {@code videoInsertButtons} and {@code videoSizeButtons}. */
    public static final String VIDEO_BACK = "videoBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.EMBEDLY
    // -----------------------------------------------------------------------------------------------------------

    /** Embed URL. Needs {@link FroalaPlugin#EMBEDLY}. In Froala's default toolbar. */
    public static final String EMBEDLY = "embedly";

    /** Remove. Needs {@link FroalaPlugin#EMBEDLY}. In {@code embedlyEditButtons}. */
    public static final String EMBEDLY_REMOVE = "embedlyRemove";

    /** Back. Needs {@link FroalaPlugin#EMBEDLY}. In {@code embedlyInsertButtons}. */
    public static final String EMBEDLY_BACK = "embedlyBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.FONT_AWESOME
    // -----------------------------------------------------------------------------------------------------------

    /** Font Awesome. Needs {@link FroalaPlugin#FONT_AWESOME}. In Froala's default toolbar. */
    public static final String FONT_AWESOME = "fontAwesome";

    /** Back. Needs {@link FroalaPlugin#FONT_AWESOME}. In {@code faButtons}. */
    public static final String FONT_AWESOME_BACK = "fontAwesomeBack";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.IMAGE_FILEROBOT
    // -----------------------------------------------------------------------------------------------------------

    /** Advanced Edit (File-Robot). Needs {@link FroalaPlugin#IMAGE_FILEROBOT}. In {@code imageEditButtons}. */
    public static final String IMAGE_FILEROBOT = "imageFilerobot";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.IMAGE_TUI
    // -----------------------------------------------------------------------------------------------------------

    /** Advanced Edit. Needs {@link FroalaPlugin#IMAGE_TUI}. In {@code imageEditButtons}. */
    public static final String IMAGE_TUI = "imageTUI";

    // -----------------------------------------------------------------------------------------------------------
    // FroalaPlugin.SPELL_CHECKER
    // -----------------------------------------------------------------------------------------------------------

    /** Spell Checker. Needs {@link FroalaPlugin#SPELL_CHECKER}. In Froala's default toolbar. */
    public static final String SPELL_CHECKER = "spellChecker";

    private FroalaButton() {
    }
}
