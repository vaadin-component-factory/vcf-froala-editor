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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Froala's options, typed. Immutable, because every {@code with…} method returns a new instance and leaves this one
 * unchanged. One configured object can therefore be shared between editors.
 *
 * <pre>
 * FroalaOptions options = FroalaOptions.defaults().withPlaceholderText("Write something").withCharCounterMax(2000);
 *
 * FroalaEditor editor = new FroalaEditor(options);
 * </pre>
 *
 * <p>
 * Froala reads its options once, when the editor is built, and has no API to change one on a running editor. Passing a
 * different object to {@link FroalaEditor#setOptions(FroalaOptions)} therefore rebuilds the editor. The value is kept,
 * but caret, selection, scroll position and undo history are lost.
 *
 * <p>
 * Only options that were set appear in the JSON sent to the browser. Every other option keeps Froala's default, as
 * documented at <a href="https://froala.com/wysiwyg-editor/docs/options/">froala.com</a>.
 *
 * <p>
 * Not every Froala option is typed here. Untyped options can be passed as raw JSON through
 * {@link FroalaEditor#setOptions(String)}. There is no per-key setter.
 *
 * <p>
 * Three options are not offered. {@code key} is set with {@link FroalaEditor#setLicenseKey(String)}, and {@code height}
 * and {@code width} conflict with the size of the Vaadin component, which is set with
 * {@link FroalaEditor#setHeight(String)} and the other {@code HasSize} methods.
 *
 * <p>
 * Two options cannot be set from Java at all, {@code events} and {@code aiAssistRequest}. Both take JavaScript
 * functions, and options are transferred as JSON, which cannot hold a function. {@link FroalaEditor#setOptions(String)}
 * throws if the options contain {@code events}, since Froala would try to call what arrived. It does not check for
 * {@code aiAssistRequest}, so passing that as raw JSON is accepted and has no effect. Froala events reach the server
 * only through the listeners {@link FroalaEditor} offers, such as {@link FroalaEditor#addValueChangeListener}.
 */
public final class FroalaOptions implements Serializable {

    private static final FroalaOptions EMPTY = new FroalaOptions(Json.createObject());

    /** The options as Froala will receive them. Never handed out or mutated, which keeps the class immutable. */
    private final JsonObject values;

    private FroalaOptions(JsonObject values) {
        this.values = values;
    }

    /**
     * Returns an object that sets nothing, so the editor runs on Froala's own defaults. Every configuration starts
     * here.
     *
     * @return options that set nothing
     */
    public static FroalaOptions defaults() {
        return EMPTY;
    }

    /**
     * Returns a copy with the given option set, or removed if the value is null. Each option name appears in exactly
     * one {@code with…} method, so the name and its type stay together.
     */
    private FroalaOptions with(String option, JsonValue value) {
        JsonObject copy = copyOf(values);

        if (value == null) {
            copy.remove(option);
        } else {
            copy.put(option, value);
        }

        return new FroalaOptions(copy);
    }

    private FroalaOptions with(String option, String value) {
        return with(option, value == null ? null : Json.create(value));
    }

    private FroalaOptions with(String option, boolean value) {
        return with(option, Json.create(value));
    }

    private FroalaOptions with(String option, int value) {
        return with(option, Json.create(value));
    }

    private FroalaOptions with(String option, Collection<String> values) {
        if (values == null) {
            return with(option, (JsonValue) null);
        }

        JsonArray array = Json.createArray();
        values.forEach(value -> array.set(array.length(), value));

        return with(option, array);
    }

    /** Elemental has no copy operation, so a copy is a fresh object with every entry put into it again. */
    private static JsonObject copyOf(JsonObject source) {
        JsonObject copy = Json.createObject();

        for (String key : source.keys()) {
            copy.put(key, source.<JsonValue> get(key));
        }

        return copy;
    }

    // -----------------------------------------------------------------------------------------------------------
    // Editing surface
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the text shown while the editor is empty. Froala's {@code placeholderText}.
     *
     * @param placeholderText placeholder text, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withPlaceholderText(String placeholderText) {
        return with("placeholderText", placeholderText);
    }

    /**
     * Sets the language Froala's own texts are shown in, such as toolbar tooltips, popups and error messages. Froala's
     * {@code language}, which takes a language file's name such as {@code de}, {@code pt_br} or {@code zh_cn}. The
     * editor downloads that file from {@code js/languages/} before it is built. A name with no such file leaves the
     * editor in English, which is built in and has no file of its own.
     *
     * <p>
     * Without this option the editor takes the language of the UI's locale, when it is built. It tries language and
     * country first and the language alone second, so {@code zh_CN} gives {@code zh_cn} and {@code de_AT} gives
     * {@code de}. A locale without a file, such as English, leaves the editor in English.
     *
     * @param language language file name, or null for the UI's locale
     * @return a new instance
     */
    public FroalaOptions withLanguage(String language) {
        return with("language", language);
    }

    /**
     * Sets the reading direction of the edited text. Froala's {@code direction}.
     *
     * <p>
     * This takes effect only without a language file, and the UI's locale picks one as well, see
     * {@link #withLanguage(String)}. Every Froala language file names its own direction, and Froala lets it win over
     * this option. So {@code withLanguage("ar").withDirection(LTR)} builds a right-to-left editor, and
     * {@code withLanguage("de").withDirection(RTL)} a left-to-right one. For right-to-left text with English tooltips
     * under any locale, use {@code withLanguage("en").withDirection(RTL)}. English has no file.
     *
     * <p>
     * The component's own {@code dir} follows the direction the editor is built with, so the label, helper text and
     * error message sit on the same side as the text. {@link FroalaTextDirection#AUTO} leaves the component's
     * {@code dir} as it was before.
     *
     * @param direction text direction, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withDirection(FroalaTextDirection direction) {
        return with("direction", direction == null ? null : direction.getOptionValue());
    }

    /**
     * Sets the look of the editor. Froala's {@code theme}. Without it the add-on uses {@link FroalaTheme#VAADIN}. A
     * theme of your own is its name. Froala puts the class {@code <theme>-theme} on the editor and on its popups, and
     * the stylesheet for it is yours to load.
     *
     * <pre>
     * options.withTheme(FroalaTheme.NONE);
     * options.withTheme("brand"); // the class brand-theme
     * </pre>
     *
     * @param theme a {@link FroalaTheme} or the name of your own, {@link FroalaTheme#NONE} for no theme, or null for
     *            the add-on's default
     * @return a new instance
     */
    public FroalaOptions withTheme(String theme) {
        return with("theme", theme);
    }

    /**
     * Switches the browser's spell checker on or off for the editing area. Froala's {@code spellcheck}, on by default.
     * On mobile devices it also switches autocorrect and autocapitalization.
     *
     * @param spellcheck whether the browser checks the spelling
     * @return a new instance
     */
    public FroalaOptions withSpellcheck(boolean spellcheck) {
        return with("spellcheck", spellcheck);
    }

    /**
     * Sets extra form fields sent along with every image upload. Froala's {@code imageUploadParams}.
     *
     * @param imageUploadParams parameter name to value, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageUploadParams(Map<String, String> imageUploadParams) {
        return with("imageUploadParams", toJsonObject(imageUploadParams));
    }

    /**
     * Sets HTTP headers Froala adds to its own requests, such as uploads, the image manager and the save plugin.
     * Froala's {@code requestHeaders}. A CSRF token is the usual reason to need this.
     *
     * @param requestHeaders header name to value, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withRequestHeaders(Map<String, String> requestHeaders) {
        return with("requestHeaders", toJsonObject(requestHeaders));
    }

    /**
     * Sets the configuration Froala passes straight through to the Toast UI image editor. Froala's
     * {@code imageTUIOptions}.
     *
     * <p>
     * The shape is defined by Toast UI, not by Froala, so it is passed as JSON.
     *
     * @param imageTuiOptions options for the Toast UI image editor, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageTuiOptions(JsonObject imageTuiOptions) {
        return with("imageTUIOptions", imageTuiOptions);
    }

    private static JsonObject toJsonObject(Map<String, String> entries) {
        if (entries == null) {
            return null;
        }

        JsonObject object = Json.createObject();
        entries.forEach(object::put);

        return object;
    }

    // -----------------------------------------------------------------------------------------------------------
    // Toolbar and editing mode
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets which buttons the toolbar shows and how they are grouped. Froala's {@code toolbarButtons}, which is its own
     * eight-group default when left alone.
     *
     * <p>
     * This is also the toolbar Froala falls back to on a narrower screen. {@link #withToolbarButtonsMd(FroalaToolbar)},
     * {@link #withToolbarButtonsSm(FroalaToolbar)} and {@link #withToolbarButtonsXs(FroalaToolbar)} are each consulted
     * first, but only if they were set. Which of the four is used at a given moment follows the editor's own width when
     * Froala's {@code toolbarResponsiveToEditor} is on, and the window's otherwise.
     *
     * @param toolbar the toolbar layout, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withToolbarButtons(FroalaToolbar toolbar) {
        return withToolbar("toolbarButtons", toolbar);
    }

    /**
     * Sets the toolbar used from 992 to 1199 pixels wide. Froala's {@code toolbarButtonsMD}. Without it that width uses
     * {@link #withToolbarButtons(FroalaToolbar)}, which also documents what the width is measured on.
     *
     * @param toolbar the toolbar layout, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withToolbarButtonsMd(FroalaToolbar toolbar) {
        return withToolbar("toolbarButtonsMD", toolbar);
    }

    /**
     * Sets the toolbar used from 768 to 991 pixels wide. Froala's {@code toolbarButtonsSM}. Without it that width uses
     * {@link #withToolbarButtons(FroalaToolbar)}, which also documents what the width is measured on.
     *
     * @param toolbar the toolbar layout, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withToolbarButtonsSm(FroalaToolbar toolbar) {
        return withToolbar("toolbarButtonsSM", toolbar);
    }

    /**
     * Sets the toolbar used below 768 pixels wide. Froala's {@code toolbarButtonsXS}. Without it that width uses
     * {@link #withToolbarButtons(FroalaToolbar)}, which also documents what the width is measured on.
     *
     * @param toolbar the toolbar layout, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withToolbarButtonsXs(FroalaToolbar toolbar) {
        return withToolbar("toolbarButtonsXS", toolbar);
    }

    private FroalaOptions withToolbar(String option, FroalaToolbar toolbar) {
        return with(option, toolbar == null ? null : toolbar.toJson());
    }

    /**
     * Turns the inline editing mode on. There is no toolbar until text is selected, and selecting some pops one up next
     * to it. Froala's {@code toolbarInline}.
     *
     * @param toolbarInline whether the toolbar appears on selection instead of above the editor
     * @return a new instance
     */
    public FroalaOptions withToolbarInline(boolean toolbarInline) {
        return with("toolbarInline", toolbarInline);
    }

    /**
     * Turns the document editing mode on. The editing area is laid out as a page, with margins and a page width, the
     * way a word processor shows a document. Froala's {@code documentReady}.
     *
     * @param documentReady whether the editing area is laid out as a page
     * @return a new instance
     */
    public FroalaOptions withDocumentReady(boolean documentReady) {
        return with("documentReady", documentReady);
    }

    /**
     * Keeps the toolbar in view while the user scrolls through a long document. Froala's {@code toolbarSticky}, on by
     * default.
     *
     * @param toolbarSticky whether the toolbar stays in view while scrolling
     * @return a new instance
     */
    public FroalaOptions withToolbarSticky(boolean toolbarSticky) {
        return with("toolbarSticky", toolbarSticky);
    }

    /**
     * Moves the toolbar below the editing area. Froala's {@code toolbarBottom}.
     *
     * @param toolbarBottom whether the toolbar sits below the editing area
     * @return a new instance
     */
    public FroalaOptions withToolbarBottom(boolean toolbarBottom) {
        return with("toolbarBottom", toolbarBottom);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Button lists of popups and plugins
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the toolbar buttons that stay usable while the code view shows the HTML source. Froala's
     * {@code codeViewKeepActiveButtons}. Needs {@link FroalaPlugin#CODE_VIEW}. Default {@code fullscreen}.
     *
     * @param codeViewKeepActiveButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withCodeViewKeepActiveButtons(Collection<String> codeViewKeepActiveButtons) {
        return with("codeViewKeepActiveButtons", codeViewKeepActiveButtons);
    }

    /**
     * Sets the buttons of the small popup Froala shows on a text selection. Froala leaves out {@code aiImproveWriting}
     * until {@code aiSupplementalTermsAccepted} is set, and {@code collabAddComment} without a {@code collabConfig}.
     * Froala's {@code selectionActionButtons}. Default {@code aiImproveWriting}, {@code collabAddComment}.
     *
     * @param selectionActionButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withSelectionActionButtons(Collection<String> selectionActionButtons) {
        return with("selectionActionButtons", selectionActionButtons);
    }

    /**
     * Sets the buttons offered next to an empty line. These take the names of {@link FroalaQuickInsertButton}, not
     * command names. Froala's {@code quickInsertButtons}. Needs {@link FroalaPlugin#QUICK_INSERT}. Default
     * {@code image}, {@code video}, {@code embedly}, {@code table}, {@code ul}, {@code ol}, {@code hr}.
     *
     * @param quickInsertButtons names from {@link FroalaQuickInsertButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withQuickInsertButtons(Collection<String> quickInsertButtons) {
        return with("quickInsertButtons", quickInsertButtons);
    }

    /**
     * Sets the buttons above the swatches of the text and background color popup. Froala's {@code colorsButtons}. Needs
     * {@link FroalaPlugin#COLORS}. Default {@code colorsBack}, {@code |}, {@code -}.
     *
     * @param colorsButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withColorsButtons(Collection<String> colorsButtons) {
        return with("colorsButtons", colorsButtons);
    }

    /**
     * Sets the buttons above the emoticons popup. Froala's {@code emoticonsButtons}. Needs
     * {@link FroalaPlugin#EMOTICONS}. Default {@code emoticonsBack}, {@code |}.
     *
     * @param emoticonsButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withEmoticonsButtons(Collection<String> emoticonsButtons) {
        return with("emoticonsButtons", emoticonsButtons);
    }

    /**
     * Sets the buttons above the special characters popup. Froala's {@code specialCharButtons}. Needs
     * {@link FroalaPlugin#SPECIAL_CHARACTERS}. Default {@code specialCharBack}, {@code |}.
     *
     * @param specialCharButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withSpecialCharButtons(Collection<String> specialCharButtons) {
        return with("specialCharButtons", specialCharButtons);
    }

    /**
     * Sets the buttons above the Font Awesome icon popup. Froala's {@code faButtons}. Needs
     * {@link FroalaPlugin#FONT_AWESOME}. Default {@code fontAwesomeBack}, {@code |}.
     *
     * @param faButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFaButtons(Collection<String> faButtons) {
        return with("faButtons", faButtons);
    }

    /**
     * Sets the buttons of the popup that opens on a link. Froala's {@code linkEditButtons}. Needs
     * {@link FroalaPlugin#LINK}. Default {@code linkOpen}, {@code linkStyle}, {@code linkEdit}, {@code linkRemove}.
     *
     * @param linkEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withLinkEditButtons(Collection<String> linkEditButtons) {
        return with("linkEditButtons", linkEditButtons);
    }

    /**
     * Sets the buttons above the insert link popup. Froala's {@code linkInsertButtons}. Needs
     * {@link FroalaPlugin#LINK}. Default {@code linkBack}, {@code |}, {@code linkList}.
     *
     * @param linkInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withLinkInsertButtons(Collection<String> linkInsertButtons) {
        return with("linkInsertButtons", linkInsertButtons);
    }

    /**
     * Sets the buttons of the popup that opens on an anchor. Froala's {@code anchorEditButtons}. Needs
     * {@link FroalaPlugin#LINK_TO_ANCHOR}. Default {@code anchorEdit}, {@code anchorRemove}.
     *
     * @param anchorEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withAnchorEditButtons(Collection<String> anchorEditButtons) {
        return with("anchorEditButtons", anchorEditButtons);
    }

    /**
     * Sets the buttons of the popup that opens on an image. Froala's {@code imageEditButtons}. Needs
     * {@link FroalaPlugin#IMAGE}. Default {@code imageReplace}, {@code imageAlign}, {@code imageCaption},
     * {@code imageRemove}, {@code imageLink}, {@code linkOpen}, {@code linkEdit}, {@code linkRemove}, {@code -},
     * {@code imageDisplay}, {@code imageStyle}, {@code imageAlt}, {@code imageSize}, {@code filestackIcon},
     * {@code imageFilerobot}, {@code imageTUI}. Of these, {@code filestackIcon}, {@code imageFilerobot} and
     * {@code imageTUI} are added by their own plugins and appear only while those are enabled.
     *
     * @param imageEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageEditButtons(Collection<String> imageEditButtons) {
        return with("imageEditButtons", imageEditButtons);
    }

    /**
     * Sets the tabs of the insert image popup. Froala's {@code imageInsertButtons}. Needs {@link FroalaPlugin#IMAGE}.
     * Default {@code imageBack}, {@code |}, {@code imageUpload}, {@code imageByURL}, {@code imageManager},
     * {@code openFilePickerImage}. Of these, {@code imageManager} and {@code openFilePickerImage} are added by their
     * own plugins and appear only while those are enabled.
     *
     * @param imageInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageInsertButtons(Collection<String> imageInsertButtons) {
        return with("imageInsertButtons", imageInsertButtons);
    }

    /**
     * Sets the buttons above the alternative text popup of an image. Froala's {@code imageAltButtons}. Needs
     * {@link FroalaPlugin#IMAGE}. Default {@code imageBack}, {@code |}.
     *
     * @param imageAltButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageAltButtons(Collection<String> imageAltButtons) {
        return with("imageAltButtons", imageAltButtons);
    }

    /**
     * Sets the buttons above the size popup of an image. Froala's {@code imageSizeButtons}. Needs
     * {@link FroalaPlugin#IMAGE}. Default {@code imageBack}, {@code |}.
     *
     * @param imageSizeButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageSizeButtons(Collection<String> imageSizeButtons) {
        return with("imageSizeButtons", imageSizeButtons);
    }

    /**
     * Sets the buttons of the popup that opens on a video. Froala's {@code videoEditButtons}. Needs
     * {@link FroalaPlugin#VIDEO}. Default {@code videoReplace}, {@code videoRemove}, {@code videoDisplay},
     * {@code videoAlign}, {@code videoSize}, {@code autoplay}.
     *
     * @param videoEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withVideoEditButtons(Collection<String> videoEditButtons) {
        return with("videoEditButtons", videoEditButtons);
    }

    /**
     * Sets the tabs of the insert video popup. Froala's {@code videoInsertButtons}. Needs {@link FroalaPlugin#VIDEO}.
     * Default {@code videoBack}, {@code |}, {@code videoByURL}, {@code videoEmbed}, {@code videoUpload},
     * {@code openFilePickerVideo}. Of these, {@code openFilePickerVideo} is added by {@link FroalaPlugin#FILESTACK} and
     * appears only while it is enabled.
     *
     * @param videoInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withVideoInsertButtons(Collection<String> videoInsertButtons) {
        return with("videoInsertButtons", videoInsertButtons);
    }

    /**
     * Sets the buttons above the size popup of a video. Froala's {@code videoSizeButtons}. Needs
     * {@link FroalaPlugin#VIDEO}. Default {@code videoBack}, {@code |}.
     *
     * @param videoSizeButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withVideoSizeButtons(Collection<String> videoSizeButtons) {
        return with("videoSizeButtons", videoSizeButtons);
    }

    /**
     * Sets the buttons above the upload file popup. Froala's {@code fileInsertButtons}. Needs
     * {@link FroalaPlugin#FILE}. Default {@code fileBack}, {@code |}, {@code openFilePickerFile}. Of these,
     * {@code openFilePickerFile} is added by {@link FroalaPlugin#FILESTACK} and appears only while it is enabled.
     *
     * @param fileInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFileInsertButtons(Collection<String> fileInsertButtons) {
        return with("fileInsertButtons", fileInsertButtons);
    }

    /**
     * Sets the tabs of the files manager popup. Froala's {@code filesInsertButtons}. Needs
     * {@link FroalaPlugin#FILES_MANAGER}. Default {@code imageBack}, {@code |}, {@code filesUpload},
     * {@code filesByURL}, {@code filesEmbed}, {@code openFilePicker}. Of these, {@code openFilePicker} is added by
     * {@link FroalaPlugin#FILESTACK} and appears only while it is enabled.
     *
     * @param filesInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFilesInsertButtons(Collection<String> filesInsertButtons) {
        return with("filesInsertButtons", filesInsertButtons);
    }

    /**
     * Sets the buttons at the right end of the files manager popup. Froala's {@code filesInsertButtons2}. Needs
     * {@link FroalaPlugin#FILES_MANAGER}. Default {@code deleteAll}, {@code insertAll}, {@code cancel},
     * {@code minimize}.
     *
     * @param filesInsertButtons2 names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFilesInsertButtons2(Collection<String> filesInsertButtons2) {
        return with("filesInsertButtons2", filesInsertButtons2);
    }

    /**
     * Sets the buttons of the popup that opens on selected table cells. Froala's {@code tableEditButtons}. Needs
     * {@link FroalaPlugin#TABLE}. Default {@code tableHeader}, {@code tableFooter}, {@code tableRemove},
     * {@code tableRows}, {@code tableColumns}, {@code tableProperties}, {@code -}, {@code tableCells},
     * {@code tableCellProperties}, {@code tableCellVerticalAlign}, {@code tableCellHorizontalAlign},
     * {@code tableCellStyle}.
     *
     * @param tableEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withTableEditButtons(Collection<String> tableEditButtons) {
        return with("tableEditButtons", tableEditButtons);
    }

    /**
     * Sets the buttons above the insert table popup. Froala's {@code tableInsertButtons}. Needs
     * {@link FroalaPlugin#TABLE}. Default {@code tableBack}, {@code |}.
     *
     * @param tableInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withTableInsertButtons(Collection<String> tableInsertButtons) {
        return with("tableInsertButtons", tableInsertButtons);
    }

    /**
     * Sets the buttons above the cell background color popup. Froala's {@code tableColorsButtons}. Needs
     * {@link FroalaPlugin#TABLE}. Default {@code tableBack}, {@code |}.
     *
     * @param tableColorsButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withTableColorsButtons(Collection<String> tableColorsButtons) {
        return with("tableColorsButtons", tableColorsButtons);
    }

    /**
     * Sets the buttons of the popup that opens on embedded content. Froala's {@code embedlyEditButtons}. Needs
     * {@link FroalaPlugin#EMBEDLY}. Default {@code embedlyRemove}.
     *
     * @param embedlyEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withEmbedlyEditButtons(Collection<String> embedlyEditButtons) {
        return with("embedlyEditButtons", embedlyEditButtons);
    }

    /**
     * Sets the buttons above the embed URL popup. Froala's {@code embedlyInsertButtons}. Needs
     * {@link FroalaPlugin#EMBEDLY}. Default {@code embedlyBack}, {@code |}.
     *
     * @param embedlyInsertButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withEmbedlyInsertButtons(Collection<String> embedlyInsertButtons) {
        return with("embedlyInsertButtons", embedlyInsertButtons);
    }

    /**
     * Sets the buttons of the popup that opens on a button of a form in the content. Froala's {@code formEditButtons}.
     * Needs {@link FroalaPlugin#FORMS}. Default {@code inputStyle}, {@code inputEdit}.
     *
     * @param formEditButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFormEditButtons(Collection<String> formEditButtons) {
        return with("formEditButtons", formEditButtons);
    }

    /**
     * Sets the buttons above the popup that edits the text of a form button. Froala's {@code formUpdateButtons}. Needs
     * {@link FroalaPlugin#FORMS}. Default {@code inputBack}, {@code |}.
     *
     * @param formUpdateButtons names from {@link FroalaButton}, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFormUpdateButtons(Collection<String> formUpdateButtons) {
        return with("formUpdateButtons", formUpdateButtons);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Counters
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Shows or hides the character count below the editing area. Froala's {@code charCounterCount}, on by default.
     *
     * @param charCounterCount whether the character count is shown
     * @return a new instance
     */
    public FroalaOptions withCharCounterCount(boolean charCounterCount) {
        return with("charCounterCount", charCounterCount);
    }

    /**
     * Limits how many characters the editor accepts. Froala's {@code charCounterMax}, {@code -1} for no limit.
     *
     * <p>
     * This is a hard input limit, not a validation. At the limit Froala swallows further keystrokes, truncates what is
     * pasted and fires its {@code charCounter.exceeded} event. Binder validation is a separate thing and can be used
     * alongside. Note that the value is HTML, so its length is not what the user sees.
     *
     * @param charCounterMax maximum number of characters, or -1 for no limit
     * @return a new instance
     */
    public FroalaOptions withCharCounterMax(int charCounterMax) {
        return with("charCounterMax", charCounterMax);
    }

    /**
     * Shows or hides the word count below the editing area. Froala's {@code wordCounterCount}, on by default.
     *
     * @param wordCounterCount whether the word count is shown
     * @return a new instance
     */
    public FroalaOptions withWordCounterCount(boolean wordCounterCount) {
        return with("wordCounterCount", wordCounterCount);
    }

    /**
     * Limits how many words the editor accepts. Froala's {@code wordCounterMax}, {@code -1} for no limit. A hard input
     * limit, like {@link #withCharCounterMax(int)}.
     *
     * @param wordCounterMax maximum number of words, or -1 for no limit
     * @return a new instance
     */
    public FroalaOptions withWordCounterMax(int wordCounterMax) {
        return with("wordCounterMax", wordCounterMax);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Plugins
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Restricts the editor to the given plugins. Same as {@link #withPluginsEnabled(Collection)}.
     *
     * @param plugins the plugins the editor may use, or null for {@link FroalaPlugin#basics()}
     * @return a new instance
     */
    public FroalaOptions withPluginsEnabled(String... plugins) {
        return withPluginsEnabled(plugins == null ? null : Arrays.asList(plugins));
    }

    /**
     * Restricts the editor to the given plugins. Froala's {@code pluginsEnabled}. Left alone, the editor gets
     * {@link FroalaPlugin#basics()}. {@link FroalaPlugin#all()} enables every plugin. The browser downloads only the
     * files of the plugins an editor enables. A toolbar button is dropped silently when its command declares a plugin
     * that is not enabled. It is then neither drawn nor counted towards a group's
     * {@link FroalaToolbarGroup#withButtonsVisible(int)}. Commands that declare no plugin, among them {@code bold} and
     * {@code italic}, are drawn whatever this option holds.
     *
     * <p>
     * A plugin is the name it registers itself under. {@link FroalaPlugin} lists Froala's. A plugin of your own goes
     * into the same list, and its file is yours to load:
     *
     * <pre>
     * Set&lt;String&gt; plugins = FroalaPlugin.basics();
     * plugins.add("myPlugin");
     * options.withPluginsEnabled(plugins);
     * </pre>
     *
     * @param plugins the plugins the editor may use, or null for {@link FroalaPlugin#basics()}
     * @return a new instance
     */
    public FroalaOptions withPluginsEnabled(Collection<String> plugins) {
        return with("pluginsEnabled", plugins);
    }

    // -----------------------------------------------------------------------------------------------------------
    // HTML cleaning
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the tags the editor keeps. Froala's {@code htmlAllowedTags}, which by default allows about a hundred tags,
     * {@code script} and {@code iframe} among them. A tag not in the list is unwrapped, so its content stays and only
     * the tag goes. Froala cleans whatever HTML it is given this way, when the value is set and when text is pasted.
     *
     * <p>
     * Each entry is a regular expression matched against the whole tag name, ignoring case. {@code "h[1-6]"} allows all
     * six headings and {@code ".*"} every tag.
     *
     * <p>
     * If the page has DOMPurify loaded as {@code window.DOMPurify}, Froala also hands this list and
     * {@link #withHtmlAllowedAttrs(Collection)} to it. DOMPurify reads each entry as a plain name, so a pattern such as
     * {@code "h[1-6]"} then removes the tags it was meant to allow. The add-on does not load DOMPurify.
     *
     * <p>
     * This cleaning runs in the browser and protects nothing on the server. See {@link FroalaEditor} on treating the
     * value as untrusted input.
     *
     * @param htmlAllowedTags tag name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlAllowedTags(Collection<String> htmlAllowedTags) {
        return with("htmlAllowedTags", htmlAllowedTags);
    }

    /**
     * Sets the tags the editor removes together with their content. Froala's {@code htmlRemoveTags}, which is
     * {@code script} and {@code style} by default. The entries are patterns like those of
     * {@link #withHtmlAllowedTags(Collection)}.
     *
     * @param htmlRemoveTags tag name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlRemoveTags(Collection<String> htmlRemoveTags) {
        return with("htmlRemoveTags", htmlRemoveTags);
    }

    /**
     * Sets the attributes the editor keeps. Froala's {@code htmlAllowedAttrs}, which by default allows about 120
     * attributes, among them {@code style}, {@code class} and every {@code data-} attribute. Any other attribute is
     * removed. The entries are patterns like those of {@link #withHtmlAllowedTags(Collection)}, so {@code "data-.*"}
     * allows every data attribute. Froala keeps its own {@code fr-} and {@code data-fr-} attributes whatever this
     * holds.
     *
     * @param htmlAllowedAttrs attribute name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlAllowedAttrs(Collection<String> htmlAllowedAttrs) {
        return with("htmlAllowedAttrs", htmlAllowedAttrs);
    }

    /**
     * Sets the CSS properties the editor keeps in a {@code style} attribute. Froala's {@code htmlAllowedStyleProps},
     * which is {@code ".*"} by default and keeps every property. The others are removed from the attribute. The entries
     * are patterns like those of {@link #withHtmlAllowedTags(Collection)}, so {@code "border-.*"} keeps every border
     * property.
     *
     * <p>
     * A style attribute left with no allowed property is removed. An empty list therefore removes every style
     * attribute.
     *
     * @param htmlAllowedStyleProps CSS property name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlAllowedStyleProps(Collection<String> htmlAllowedStyleProps) {
        return with("htmlAllowedStyleProps", htmlAllowedStyleProps);
    }

    /**
     * Sets the elements the editor keeps although they are empty. Froala's {@code htmlAllowedEmptyTags}, by default
     * {@code textarea}, {@code a}, {@code iframe}, {@code object}, {@code video}, {@code style}, {@code script},
     * {@code .fa}, {@code .fr-emoticon}, {@code .fr-inner}, {@code path}, {@code line} and {@code hr}. Froala removes
     * any other empty element.
     *
     * <p>
     * Each entry is a tag name, not a pattern. Froala's own list also holds CSS selectors such as {@code .fa}, but only
     * some of its checks honour a selector, so a tag name is the reliable choice.
     *
     * @param htmlAllowedEmptyTags tag names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlAllowedEmptyTags(Collection<String> htmlAllowedEmptyTags) {
        return with("htmlAllowedEmptyTags", htmlAllowedEmptyTags);
    }

    /**
     * Sets the tags the editor does not wrap in a paragraph when they stand at the top level. Froala's
     * {@code htmlDoNotWrapTags}, which is {@code script} and {@code style} by default. Each entry is a tag name, not a
     * pattern.
     *
     * @param htmlDoNotWrapTags tag names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlDoNotWrapTags(Collection<String> htmlDoNotWrapTags) {
        return with("htmlDoNotWrapTags", htmlDoNotWrapTags);
    }

    /**
     * Sets the CSS properties that are not copied from the page's stylesheets into the content. Froala's
     * {@code htmlIgnoreCSSProperties}, empty by default. It only has an effect while {@link #withUseClasses(boolean)}
     * is off, since only then does Froala copy the styles of the classes into {@code style} attributes. The entries are
     * patterns like those of {@link #withHtmlAllowedTags(Collection)}.
     *
     * @param htmlIgnoreCssProperties CSS property name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withHtmlIgnoreCssProperties(Collection<String> htmlIgnoreCssProperties) {
        return with("htmlIgnoreCSSProperties", htmlIgnoreCssProperties);
    }

    /**
     * Decides whether the value keeps the classes of the content or their styles. Froala's {@code useClasses}, on by
     * default. Off, Froala copies the CSS rules of the page's stylesheets that apply to an element into its
     * {@code style} attribute, so the value looks the same where those stylesheets are missing.
     *
     * @param useClasses whether the value keeps classes instead of copying their styles
     * @return a new instance
     */
    public FroalaOptions withUseClasses(boolean useClasses) {
        return with("useClasses", useClasses);
    }

    /**
     * Keeps or removes HTML comments. Froala's {@code htmlAllowComments}, on by default.
     *
     * @param htmlAllowComments whether comments stay in the content
     * @return a new instance
     */
    public FroalaOptions withHtmlAllowComments(boolean htmlAllowComments) {
        return with("htmlAllowComments", htmlAllowComments);
    }

    /**
     * Decides whether scripts in the content run when it is set. Froala's {@code htmlExecuteScripts}, on by default. A
     * script only gets that far if {@link #withHtmlRemoveTags(Collection)} no longer removes {@code script}, which it
     * does by default.
     *
     * @param htmlExecuteScripts whether scripts in the content run
     * @return a new instance
     */
    public FroalaOptions withHtmlExecuteScripts(boolean htmlExecuteScripts) {
        return with("htmlExecuteScripts", htmlExecuteScripts);
    }

    /**
     * Writes a plain {@code &} into the value instead of {@code &amp;}. Froala's {@code htmlSimpleAmpersand}, off by
     * default. An ampersand in the text then reaches the server as a bare {@code &}.
     *
     * @param htmlSimpleAmpersand whether the value holds {@code &} instead of {@code &amp;}
     * @return a new instance
     */
    public FroalaOptions withHtmlSimpleAmpersand(boolean htmlSimpleAmpersand) {
        return with("htmlSimpleAmpersand", htmlSimpleAmpersand);
    }

    /**
     * Stops Froala from restructuring the content. Froala's {@code htmlUntouched}, off by default. With it on, Froala
     * no longer removes empty tags, fixes lists and tables, turns old tags into their HTML5 form or normalizes spaces.
     * The allowed and removed tags and attributes still apply.
     *
     * @param htmlUntouched whether Froala leaves the structure of the content as it is
     * @return a new instance
     */
    public FroalaOptions withHtmlUntouched(boolean htmlUntouched) {
        return with("htmlUntouched", htmlUntouched);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Paste
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Pastes text without its formatting. Froala's {@code pastePlain}, off by default. Lists and tables stay. Headings,
     * quotes and preformatted blocks become plain paragraphs, and fonts, colors and other formatting are removed.
     *
     * @param pastePlain whether pasted text loses its formatting
     * @return a new instance
     */
    public FroalaOptions withPastePlain(boolean pastePlain) {
        return with("pastePlain", pastePlain);
    }

    /**
     * Sets tags that pasted text loses, on top of those {@link #withHtmlAllowedTags(Collection)} leaves out. Froala's
     * {@code pasteDeniedTags}, which is {@code colgroup}, {@code col} and {@code meta} by default. A denied tag is
     * unwrapped, so its content stays. Each entry is a tag name exactly as it appears in the allowed list.
     *
     * @param pasteDeniedTags tag names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withPasteDeniedTags(Collection<String> pasteDeniedTags) {
        return with("pasteDeniedTags", pasteDeniedTags);
    }

    /**
     * Sets attributes that pasted text loses, on top of those {@link #withHtmlAllowedAttrs(Collection)} leaves out.
     * Froala's {@code pasteDeniedAttrs}, which is {@code class} and {@code id} by default. Each entry is an attribute
     * name exactly as it appears in the allowed list.
     *
     * @param pasteDeniedAttrs attribute names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withPasteDeniedAttrs(Collection<String> pasteDeniedAttrs) {
        return with("pasteDeniedAttrs", pasteDeniedAttrs);
    }

    /**
     * Sets the CSS properties pasted text keeps. Froala's {@code pasteAllowedStyleProps}, which is {@code ".*"} by
     * default. It takes the place of {@link #withHtmlAllowedStyleProps(Collection)} while text is pasted, and works the
     * same way.
     *
     * @param pasteAllowedStyleProps CSS property name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withPasteAllowedStyleProps(Collection<String> pasteAllowedStyleProps) {
        return with("pasteAllowedStyleProps", pasteAllowedStyleProps);
    }

    /**
     * Keeps pasted images whose source is a local {@code file://} path. Froala's {@code pasteAllowLocalImages}, off by
     * default, which removes them. Such an image shows only on the computer it was pasted on.
     *
     * @param pasteAllowLocalImages whether pasted images with a local path are kept
     * @return a new instance
     */
    public FroalaOptions withPasteAllowLocalImages(boolean pasteAllowLocalImages) {
        return with("pasteAllowLocalImages", pasteAllowLocalImages);
    }

    /**
     * Asks the user whether text pasted from Word keeps its formatting. Froala's {@code wordPasteModal}, on by default.
     * The dialog offers "Keep" and "Clean". Without it, {@link #withWordPasteKeepFormatting(boolean)} decides. Needs
     * {@link FroalaPlugin#WORD_PASTE}, like the other {@code withWord…} options.
     *
     * @param wordPasteModal whether a dialog asks what to do with text pasted from Word
     * @return a new instance
     */
    public FroalaOptions withWordPasteModal(boolean wordPasteModal) {
        return with("wordPasteModal", wordPasteModal);
    }

    /**
     * Decides what happens to text pasted from Word when {@link #withWordPasteModal(boolean)} is off. Froala's
     * {@code wordPasteKeepFormatting}, on by default. On keeps the CSS properties of
     * {@link #withWordAllowedStyleProps(Collection)}. Off keeps only {@code list-style-type} and {@code margin-left},
     * which is what "Clean" in the dialog does.
     *
     * @param wordPasteKeepFormatting whether text pasted from Word keeps its formatting
     * @return a new instance
     */
    public FroalaOptions withWordPasteKeepFormatting(boolean wordPasteKeepFormatting) {
        return with("wordPasteKeepFormatting", wordPasteKeepFormatting);
    }

    /**
     * Sets the tags text pasted from Word loses. Froala's {@code wordDeniedTags}, empty by default. Works like
     * {@link #withPasteDeniedTags(Collection)}.
     *
     * @param wordDeniedTags tag names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withWordDeniedTags(Collection<String> wordDeniedTags) {
        return with("wordDeniedTags", wordDeniedTags);
    }

    /**
     * Sets the attributes text pasted from Word loses. Froala's {@code wordDeniedAttrs}, empty by default. Works like
     * {@link #withPasteDeniedAttrs(Collection)}.
     *
     * @param wordDeniedAttrs attribute names, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withWordDeniedAttrs(Collection<String> wordDeniedAttrs) {
        return with("wordDeniedAttrs", wordDeniedAttrs);
    }

    /**
     * Sets the CSS properties text pasted from Word keeps when its formatting is kept. Froala's
     * {@code wordAllowedStyleProps}, which by default keeps fonts, colors, sizes, spacing, borders and text decoration.
     * Works like {@link #withPasteAllowedStyleProps(Collection)}.
     *
     * @param wordAllowedStyleProps CSS property name patterns, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withWordAllowedStyleProps(Collection<String> wordAllowedStyleProps) {
        return with("wordAllowedStyleProps", wordAllowedStyleProps);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Content that leaves the browser
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the URL images are uploaded to. Froala's {@code imageUploadURL}.
     *
     * <p>
     * Without one Froala does not upload at all. It reads the file in the browser and inserts a {@code blob:} URL,
     * which is valid only in the tab that created it. The document the server stores then points at nothing after a
     * reload, so leaving this unset means leaving image upload switched off, not making it local.
     *
     * <p>
     * The endpoint receives a multipart POST with the file under the parameter name {@code file} and has to answer
     * {@code {"link": "…"}} with a URL it serves the file under afterwards.
     *
     * @param imageUploadUrl upload URL, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withImageUploadUrl(String imageUploadUrl) {
        return with("imageUploadURL", imageUploadUrl);
    }

    /**
     * Sets the URL files are uploaded to. Froala's {@code fileUploadURL}. Behaves like
     * {@link #withImageUploadUrl(String)} in every respect, including what happens without one.
     *
     * @param fileUploadUrl upload URL, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withFileUploadUrl(String fileUploadUrl) {
        return with("fileUploadURL", fileUploadUrl);
    }

    /**
     * Sets the URL videos are uploaded to. Froala's {@code videoUploadURL}. Behaves like
     * {@link #withImageUploadUrl(String)} in every respect, including what happens without one.
     *
     * @param videoUploadUrl upload URL, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withVideoUploadUrl(String videoUploadUrl) {
        return with("videoUploadURL", videoUploadUrl);
    }

    /**
     * Decides whether an inserted emoji is an image or a character. Froala's {@code emoticonsUseImage}, which is
     * {@code true} by default and then fetches its icons from {@code cdnjs.cloudflare.com} and leaves a cdnjs URL in
     * the stored HTML. Set it to false and Froala inserts the plain unicode character, which needs no third party and
     * survives anywhere.
     *
     * @param emoticonsUseImage whether emoji are inserted as images from a CDN
     * @return a new instance
     */
    public FroalaOptions withEmoticonsUseImage(boolean emoticonsUseImage) {
        return with("emoticonsUseImage", emoticonsUseImage);
    }

    /**
     * Sets how many milliseconds after a change Froala's {@code save} plugin posts the content to its {@code saveURL}.
     * Froala's {@code saveInterval}. {@code 0} turns the plugin off.
     *
     * <p>
     * {@link FroalaEditor} uses {@code 0} unless this is set, where Froala's own default is 10000. A Flow application
     * gets the content through the value change listener, so the save plugin is normally not what saves anything, and
     * without a {@code saveURL} it only reports a failed save after every edit.
     *
     * @param saveInterval milliseconds between two saves, or 0 to switch the save plugin off
     * @return a new instance
     */
    public FroalaOptions withSaveInterval(int saveInterval) {
        return with("saveInterval", saveInterval);
    }

    /**
     * Sets the idle time in milliseconds after the last keystroke before Froala reports a change. Froala's
     * {@code typingTimer}.
     *
     * @param typingTimer idle time in milliseconds. Froala reports a change after at least
     *            {@value FroalaEditor#MIN_VALUE_CHANGE_TIMEOUT} ms, whatever smaller value is given here.
     * @return a new instance
     * @deprecated Use {@link FroalaEditor#setValueChangeTimeout(int)}, which sets the same Froala option. This option
     *             takes effect as long as the setter is never called. Once it is, the setter wins.
     */
    @Deprecated
    public FroalaOptions withTypingTimer(int typingTimer) {
        return with("typingTimer", typingTimer);
    }

    // -----------------------------------------------------------------------------------------------------------

    /**
     * Returns these options as the JSON object Froala is initialized with. Only options that were set appear in it.
     *
     * @return a new JSON object, never null
     */
    public JsonObject toJson() {
        return copyOf(values);
    }

    /**
     * Returns the JSON these options are passed to Froala as. Meant for logging and debugging.
     *
     * @return the options as JSON
     */
    @Override
    public String toString() {
        return values.toJson();
    }

    /**
     * Two option sets are equal when they set the same options to the same values. The order they were set in does not
     * count.
     */
    @Override
    public boolean equals(Object other) {
        // elemental compares by identity, so this walks the keys itself
        if (this == other) {
            return true;
        }
        if (!(other instanceof FroalaOptions options) || values.keys().length != options.values.keys().length) {
            return false;
        }

        for (String key : values.keys()) {
            JsonValue theirs = options.values.get(key);

            if (theirs == null || !values.<JsonValue> get(key).toJson().equals(theirs.toJson())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 0;

        // Sum, so that the result does not depend on the order the options were set in, matching equals.
        for (String key : values.keys()) {
            hash += key.hashCode() ^ values.<JsonValue> get(key).toJson().hashCode();
        }

        return hash;
    }
}
