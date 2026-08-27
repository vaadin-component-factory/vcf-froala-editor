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
 * Froala's options, typed. Immutable: every {@code with…} method answers a new instance and leaves this one alone, so
 * one configured object can be shared between editors.
 *
 * <pre>
 * FroalaOptions options = FroalaOptions.defaults().withPlaceholderText("Write something").withCharCounterMax(2000);
 *
 * FroalaEditor editor = new FroalaEditor(options);
 * </pre>
 *
 * <p>
 * <b>Options are read once, when the editor is built.</b> Froala has no API to change one on a running editor -- its
 * own answer is to destroy the instance and initialize it again -- so passing a different object to
 * {@link FroalaEditor#setOptions(FroalaOptions)} rebuilds the editor. The value survives that; caret, selection, scroll
 * position and undo history do not.
 *
 * <p>
 * Only options that have been set appear in the JSON, so everything left alone keeps Froala's own default. Froala's
 * defaults are documented at <a href="https://froala.com/wysiwyg-editor/docs/options/">froala.com</a>.
 *
 * <p>
 * Not every one of Froala's 322 options is typed here. What is missing can be passed as raw JSON through
 * {@link FroalaEditor#setOptions(String)}; there is deliberately no per-key setter next to these methods.
 *
 * <p>
 * Three options are <b>not</b> offered and will not be: {@code key} belongs to
 * {@link FroalaEditor#setLicenseKey(String)}, and {@code height} and {@code width} fight the Vaadin field that wraps
 * the editor -- size it with {@link FroalaEditor#setHeight(String)} and friends instead.
 *
 * <p>
 * <b>Two of Froala's options take callbacks and are therefore out of reach from Java</b>, whichever overload is used,
 * because every one of them ends as JSON and JSON has no functions: {@code events}, a map of 159 handlers, and
 * {@code aiAssistRequest}. {@link FroalaEditor#setOptions(String)} rejects {@code events} rather than let it arrive as
 * data Froala would then try to call. Value changes have a server side listener already
 * ({@link FroalaEditor#addValueChangeListener}); everything else Froala fires does not yet.
 */
public final class FroalaOptions implements Serializable {

    private static final FroalaOptions EMPTY = new FroalaOptions(Json.createObject());

    /**
     * The options as Froala will receive them. Never handed out and never written to after construction -- every
     * {@code with…} builds a fresh object -- so an instance really is immutable.
     */
    private final JsonObject values;

    private FroalaOptions(JsonObject values) {
        this.values = values;
    }

    /**
     * Returns an object that sets nothing, so the editor runs on Froala's own defaults. The starting point for every
     * configuration.
     *
     * @return options that set nothing
     */
    public static FroalaOptions defaults() {
        return EMPTY;
    }

    /**
     * Answers a copy with the given option set, or removed when the value is null. The only place that knows an option
     * name is the {@code with…} method next to it, which is what keeps the name and its type together.
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
     * Sets the language Froala's own texts -- toolbar tooltips, popups, error messages -- are shown in. Froala's
     * {@code language}, which takes a language file's name such as {@code de}, {@code pt_br} or {@code zh_cn}.
     *
     * <p>
     * <b>This has no effect yet.</b> Froala only applies the option once the matching file from {@code js/languages/}
     * has been loaded, and this add-on loads none, so the editor stays English whatever is set here. Loading language
     * files on demand is an open item in the roadmap; the option is here so configurations written today do not have to
     * change afterwards.
     *
     * @param language language file name, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withLanguage(String language) {
        return with("language", language);
    }

    /**
     * Sets the reading direction of the edited text. Froala's {@code direction}.
     *
     * @param direction text direction, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withDirection(FroalaTextDirection direction) {
        return with("direction", direction == null ? null : direction.getOptionValue());
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
     * Sets HTTP headers Froala adds to its own requests -- uploads, the image manager, the save plugin. Froala's
     * {@code requestHeaders}. A CSRF token is the usual reason to need this.
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
     * Typed as JSON on purpose: the shape belongs to a third-party library, not to Froala and not to this add-on, so
     * mirroring it in Java would only add a copy that goes stale.
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
     * Sets which buttons the toolbar shows, in the given order. Froala's {@code toolbarButtons}, which is its own
     * eight-group default when left alone. A button name is a registered command name; {@code "|"} draws a vertical
     * separator and {@code "-"} a horizontal one. A name Froala does not know, or one whose plugin is not enabled, is
     * dropped without a word.
     *
     * <p>
     * This is Froala's flat form, which always shows every button given. Its grouped form -- named groups with
     * {@code align} and a {@code buttonsVisible} count, and the separate {@code toolbarButtonsMD/SM/XS} for narrower
     * screens -- is not typed yet and needs {@link FroalaEditor#setOptions(String)}.
     *
     * @param buttons command names in the order they should appear, or none to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withToolbarButtons(String... buttons) {
        if (buttons == null || buttons.length == 0) {
            return with("toolbarButtons", (JsonValue) null);
        }

        JsonArray names = Json.createArray();

        for (String button : buttons) {
            names.set(names.length(), button);
        }

        return with("toolbarButtons", names);
    }

    /**
     * Turns the inline editing mode on. There is no toolbar until text is selected; selecting some pops one up next to
     * it. Froala's {@code toolbarInline}.
     *
     * @param toolbarInline whether the toolbar appears on selection instead of above the editor
     * @return a new instance
     */
    public FroalaOptions withToolbarInline(boolean toolbarInline) {
        return with("toolbarInline", toolbarInline);
    }

    /**
     * Turns the document editing mode on: the editing area is laid out as a page, with margins and a page width, the
     * way a word processor shows a document. Froala's {@code documentReady}, whose name says nothing about what it
     * does.
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
     * This is a hard input limit, not a validation: at the limit Froala swallows further keystrokes and truncates what
     * is pasted, and fires its {@code charCounter.exceeded} event. Binder validation is a separate thing and can be
     * used alongside -- note that the value is HTML, so its length is not what the user sees.
     *
     * @param charCounterMax maximum number of characters, or -1 for no limit
     * @return a new instance
     */
    public FroalaOptions withCharCounterMax(int charCounterMax) {
        return with("charCounterMax", charCounterMax);
    }

    /**
     * Shows or hides the word count below the editing area. Froala's {@code wordCounterCount}.
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
     * Restricts the editor to the given plugins. Froala's {@code pluginsEnabled}, which is all of them when left alone.
     * A toolbar button whose plugin is not enabled is dropped without a word.
     *
     * @param plugins the plugins the editor may use
     * @return a new instance
     */
    public FroalaOptions withPluginsEnabled(FroalaPlugin... plugins) {
        return withPluginsEnabled(plugins == null ? null : Arrays.asList(plugins));
    }

    /**
     * Restricts the editor to the given plugins. Froala's {@code pluginsEnabled}, which is all of them when left alone.
     * A toolbar button whose plugin is not enabled is dropped without a word.
     *
     * @param plugins the plugins the editor may use, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaOptions withPluginsEnabled(Collection<FroalaPlugin> plugins) {
        if (plugins == null) {
            return with("pluginsEnabled", (JsonValue) null);
        }

        JsonArray names = Json.createArray();
        plugins.forEach(plugin -> names.set(names.length(), plugin.getPluginName()));

        return with("pluginsEnabled", names);
    }

    // -----------------------------------------------------------------------------------------------------------
    // Content that leaves the browser
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the URL images are uploaded to. Froala's {@code imageUploadURL}.
     *
     * <p>
     * Without one Froala does not upload at all: it reads the file in the browser and inserts a {@code blob:} URL,
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
     * Sets how many seconds after a change Froala's {@code save} plugin posts the content to its {@code saveURL}.
     * Froala's {@code saveInterval}; {@code 0} turns the plugin off.
     *
     * <p>
     * A Flow application gets the content through the value change listener, so this is normally not what saves
     * anything -- and left at its default the plugin schedules a request per edit that then fails on the missing URL.
     *
     * @param saveInterval seconds between two saves, or 0 to switch the save plugin off
     * @return a new instance
     */
    public FroalaOptions withSaveInterval(int saveInterval) {
        return with("saveInterval", saveInterval);
    }

    /**
     * Sets the idle time in milliseconds after the last keystroke before Froala reports a change.
     *
     * @param typingTimer idle time in milliseconds, at least {@value FroalaEditor#MIN_VALUE_CHANGE_TIMEOUT}
     * @return a new instance
     * @deprecated Use {@link FroalaEditor#setValueChangeTimeout(int)}. It is the same Froala option, but the add-on's
     *             other value change settings are setters and Vaadin's own fields carry that name, so having it in two
     *             places for one thing is the odd one out. If the setter is never called this option still takes
     *             effect; if it is called, it wins.
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
     * count -- elemental compares by identity, so this walks the keys itself.
     */
    @Override
    public boolean equals(Object other) {
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
