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

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.shared.HasThemeVariant;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Flow integration of the Froala WYSIWYG editor.
 *
 * <p>
 * Configure the editor with {@link #setOptions(FroalaOptions)}, its toolbar with {@link FroalaToolbar}, and its license
 * key with {@link #setLicenseKey(String)}. To render the value outside an editor, use {@link FroalaViewer}.
 *
 * <p>
 * The value is HTML. This component does not parse, escape or sanitize it on the server. Froala cleans the content it
 * is given, but only in the browser, so a value that reached the server by any other route was not cleaned at all.
 * Treat the value as untrusted input before storing it or rendering it.
 */
@Tag("vcf-froala-editor")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@NpmPackage(value = "diff-match-patch", version = "1.0.5")
@JsModule("./vcf-froala-editor/vcf-froala-editor.js")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin-rules.css")
public class FroalaEditor extends AbstractSinglePropertyField<FroalaEditor, String>
        implements HasValidationProperties, HasValidator<String>, InputNotifier, HasSize, HasStyle,
        Focusable<FroalaEditor>, HasLabel, HasHelper, HasThemeVariant<FroalaEditorVariant> {

    /** The value change mode of a new editor. */
    public static final FroalaValueChangeMode DEFAULT_VALUE_CHANGE_MODE = FroalaValueChangeMode.ON_CHANGE;

    /** The lowest accepted value change timeout in milliseconds. */
    public static final int MIN_VALUE_CHANGE_TIMEOUT = 250;

    /** The default value change timeout in milliseconds. Same as Froala's {@code typingTimer} default. */
    public static final int DEFAULT_VALUE_CHANGE_TIMEOUT = 500;

    /** The default interval period in milliseconds, used by {@link FroalaValueChangeMode#INTERVAL}. */
    public static final int DEFAULT_INTERVAL_PERIOD = 2000;

    private static final String VALUE_PROPERTY = "value";
    private static final String OPTIONS_PROPERTY = "options";
    private static final String DEFAULT_PLUGINS_PROPERTY = "defaultPluginsEnabled";
    private static final String LOCALE_LANGUAGES_PROPERTY = "localeLanguages";
    private static final String COMMANDS_PROPERTY = "commands";
    private static final String ACTIVE_COMMANDS_PROPERTY = "activeCommands";
    private static final String LICENSE_KEY_PROPERTY = "licenseKey";
    private static final String VALUE_CHANGE_MODE_PROPERTY = "valueChangeMode";
    private static final String VALUE_CHANGE_TIMEOUT_PROPERTY = "valueChangeTimeout";
    private static final String INTERVAL_PERIOD_PROPERTY = "intervalPeriod";

    private static final DiffMatchPatch DIFF_MATCH_PATCH = exactDiffMatchPatch();

    /**
     * Whether the browser has been told about this component's element. Deliberately not {@link #isAttached()}, which
     * answers {@code true} inside a detach listener as well. Flow fires those from {@code StateNode.setParent} before
     * it clears the node's parent.
     */
    private boolean liveOnClient;

    /** The JSON the editor was last configured with, kept for {@link #getOptionsJson()}. Null when nothing was set. */
    private String optionsJson;

    /** What the listeners were last told about the selection, see the detach listener in the constructor. */
    private boolean hasSelection;

    /** The commands added with {@link #addCommand}, by name, in the order they were added. */
    private final Map<String, AddedCommand> commands = new LinkedHashMap<>();

    /** The names of the toggle commands whose buttons show as pressed, see {@link #setCommandActive}. */
    private final Set<String> activeCommands = new LinkedHashSet<>();

    /**
     * Creates a new instance with the given label.
     *
     * @param label the label shown above the editor, or null for none
     */
    public FroalaEditor(String label) {
        this();
        setLabel(label);
    }

    /**
     * Creates a new instance with the given label and initial value.
     *
     * @param label the label shown above the editor, or null for none
     * @param initialValue the HTML the editor starts with, not null
     * @throws NullPointerException if the initial value is null
     */
    public FroalaEditor(String label, String initialValue) {
        this();
        setLabel(label);
        setValue(initialValue);
    }

    /**
     * Creates a new instance with the given label, initial value and value change listener.
     *
     * @param label the label shown above the editor, or null for none
     * @param initialValue the HTML the editor starts with, not null
     * @param valueChangeListener notified of every later change, but not of the initial value, which is set before the
     *            listener is added
     * @throws NullPointerException if the initial value is null
     */
    public FroalaEditor(String label, String initialValue,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        setValue(initialValue);
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance with the given label and value change listener.
     *
     * @param label the label shown above the editor, or null for none
     * @param valueChangeListener notified of every change of the value
     */
    public FroalaEditor(String label,
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        setLabel(label);
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance with the given value change listener.
     *
     * @param valueChangeListener notified of every change of the value
     */
    public FroalaEditor(
            ValueChangeListener<? super ComponentValueChangeEvent<FroalaEditor, String>> valueChangeListener) {
        this();
        addValueChangeListener(valueChangeListener);
    }

    /**
     * Creates a new instance configured with the given options.
     *
     * @param options Froala options, or null for the add-on's defaults
     */
    public FroalaEditor(FroalaOptions options) {
        this();
        setOptions(options);
    }

    /**
     * Creates a new instance with the given label, configured with the given options.
     *
     * @param label the label shown above the editor, or null for none
     * @param options Froala options, or null for the add-on's defaults
     */
    public FroalaEditor(String label, FroalaOptions options) {
        this();
        setLabel(label);
        setOptions(options);
    }

    /**
     * Creates a new instance.
     */
    public FroalaEditor() {
        // The three arg constructor registers Flow's listener for a "value-changed" DOM event, which the client never
        // dispatches. Changes arrive as deltas over `_value-delta`, and a notifying `value` property on the client
        // would open a second update path next to it. Null is refused like in a text field, because a delta cannot be
        // applied to it.
        super(VALUE_PROPERTY, "", false);
        setValueChangeMode(DEFAULT_VALUE_CHANGE_MODE);
        setIntervalPeriod(DEFAULT_INTERVAL_PERIOD);

        Element element = getElement();

        // The plugins for options that name none. A property of its own, so that the options on the element stay what
        // the application set, and the client needs no list of its own.
        JsonArray basics = Json.createArray();
        FroalaPlugin.basics().forEach(plugin -> basics.set(basics.length(), plugin));
        element.setPropertyJson(DEFAULT_PLUGINS_PROPERTY, basics);

        element.addEventListener("_value-delta", event -> {
            String delta = event.getEventData().get("event.detail.delta").asString();

            String newValue;
            try {
                newValue = applyDelta(getValue(), delta);
            } catch (DeltaMismatchException e) {
                // Both sides have drifted apart, so this delta and every following one is unusable. The client holds
                // the user's text and has to resend it. Pushing our stale value would throw that text away.
                element.callJsFunction("_resyncValue");
                return;
            }

            // Only the model value, never the presentation value. Writing the property would put the whole document
            // back on the wire for every keystroke. Updating the model also lets the value change event carry
            // fromClient = true.
            setModelValue(newValue, true);
        }).addEventData("event.detail.delta");

        element.addEventListener("_value-resync",
                event -> setModelValue(event.getEventData().get("event.detail.value").asString(), true))
                .addEventData("event.detail.value");

        element.addEventListener("_command", event -> {
            AddedCommand added = commands.get(event.getEventData().get("event.detail.name").asString());
            // null for a command removed while the click was on its way
            if (added != null) {
                added.listener().onComponentEvent(new FroalaCommandEvent(this, true, added.command()));
            }
        }).addEventData("event.detail.name");

        // A re-attach builds a new element in the browser, which knows none of the popovers yet
        addAttachListener(event -> commands.values().forEach(this::sendPopover));

        // A re-attach builds a new element in the browser, which starts out knowing nothing of a selection and so never
        // reports that the old one is gone. Listeners that were told "some" hear "none" from here instead.
        addSelectionChangeListener(event -> hasSelection = event.hasSelection());
        addDetachListener(event -> {
            if (hasSelection) {
                fireEvent(new FroalaSelectionChangeEvent(this, false, false));
            }
        });

        // Set in before-client-response, not directly on attach, so that other attach listeners still see a component
        // that the browser does not know yet.
        addAttachListener(event -> event.getUI().beforeClientResponse(this, context -> liveOnClient = true));

        // The editor is built on attach, and the locale is read for that build. See setOptions for the other builds.
        addAttachListener(event -> sendLocaleLanguages());

        addDetachListener(event -> {
            liveOnClient = false;

            // The property lags behind the editor by design, see above. Bringing it in step once here is enough,
            // because Flow replays a node's properties when it is attached again. That replay seeds the rebuilt
            // editor.
            setPresentationValue(getValue());
        });
    }

    @Override
    protected void setPresentationValue(String newPresentationValue) {
        // A client edit updates only the model, so re-setting the value the property still holds is dropped by Flow as
        // unchanged, and the browser would ignore it too. Push that case explicitly, but only while live. From the
        // detach listener Flow would defer the call and overwrite a later server value on re-attach.
        boolean clientNeedsExplicitPush = liveOnClient
                && Objects.equals(newPresentationValue, getElement().getProperty(VALUE_PROPERTY));

        super.setPresentationValue(newPresentationValue);

        if (clientNeedsExplicitPush) {
            getElement().executeJs("this.value = $0", newPresentationValue);
        }
    }

    /**
     * Applies a delta to the value it was computed against and returns the result.
     *
     * <p>
     * A delta is a diff-match-patch patch text, the format the editor's client uses to send changes. A mismatch is
     * answered by asking the client to resend its value.
     *
     * @param oldValue the value the delta was computed against
     * @param delta the patch text
     * @return the value with the delta applied
     * @throws DeltaMismatchException if the delta is no patch text, does not apply exactly to the given old value, or
     *             if diff-match-patch returns a result of an unexpected shape
     */
    static String applyDelta(String oldValue, String delta) {
        List<DiffMatchPatch.Patch> patches;
        try {
            patches = DIFF_MATCH_PATCH.patchFromText(delta);
        } catch (IllegalArgumentException e) {
            throw new DeltaMismatchException("Not a patch text: " + e.getMessage());
        }

        Object[] results = DIFF_MATCH_PATCH.patchApply(new LinkedList<>(patches), oldValue);

        // patchApply answers with an untyped pair. Checking its shape keeps a library change from surfacing as a
        // ClassCastException from inside a value update.
        if (results.length != 2 || !(results[0] instanceof String patched)
                || !(results[1] instanceof boolean[] applied)) {
            throw new DeltaMismatchException(
                    "diff-match-patch returned an unexpected result shape, expected a String and a boolean[]");
        }

        // One flag per patch. A false flag means that patch found no place to apply, so the string comes back only
        // partially patched or entirely unchanged. Without this check the edit is lost with nothing to notice it.
        for (int i = 0; i < applied.length; i++) {
            if (!applied[i]) {
                throw new DeltaMismatchException(
                        "Patch " + (i + 1) + " of " + applied.length + " did not apply to the current value");
            }
        }

        return patched;
    }

    /**
     * diff-match-patch applies a patch fuzzily by default, at a nearby place or to a slightly different text. A delta
     * that does not fit the server value exactly means both sides drifted apart, so it has to fail and trigger a resync
     * instead of landing somewhere else.
     */
    private static DiffMatchPatch exactDiffMatchPatch() {
        DiffMatchPatch diffMatchPatch = new DiffMatchPatch();
        diffMatchPatch.matchThreshold = 0;
        diffMatchPatch.patchDeleteThreshold = 0;

        return diffMatchPatch;
    }

    /**
     * Configures the underlying Froala editor.
     *
     * <p>
     * Froala reads its options once, when the editor is built, and has no API to change one on a running editor.
     * Calling this on an attached instance therefore destroys the editor and builds a new one. The value is kept, but
     * caret, selection, scroll position and undo history are lost. Several calls within one server round trip cause one
     * rebuild.
     *
     * <p>
     * Each call replaces the previous options. They are not merged.
     *
     * <p>
     * Two options are applied after the given ones and take precedence over them, whichever {@code setOptions} overload
     * was used: {@code key} while {@link #setLicenseKey(String)} holds a key, and {@code typingTimer} once
     * {@link #setValueChangeTimeout(int)} has been called. An option given here is used only while the matching setter
     * was not. An upload handler likewise takes precedence over the matching upload URL. See
     * {@link #setImageUploadHandler(FroalaUploadHandler)}.
     *
     * @param options Froala options, or null for the add-on's defaults
     */
    public void setOptions(FroalaOptions options) {
        applyOptions(options == null ? null : options.toJson());
    }

    private void applyOptions(JsonObject options) {
        // Froala's `events` option is a map of callbacks, and JSON has no functions. Whatever arrived here under that
        // name would reach Froala as data and blow up the first time it fires one. Rejected rather than dropped,
        // because the options were written to do something and silently doing nothing is worse.
        if (options != null && options.hasKey("events")) {
            throw new IllegalArgumentException("Froala's `events` option takes callbacks, which JSON cannot carry, so"
                    + " it cannot be set from the server. Froala events reach the server only through the listeners"
                    + " FroalaEditor offers, such as addValueChangeListener.");
        }

        // new options build the editor again, and that build reads the locale as it is now
        sendLocaleLanguages();

        if (options == null) {
            optionsJson = null;
            getElement().removeProperty(OPTIONS_PROPERTY);
            return;
        }

        optionsJson = options.toJson();
        getElement().setPropertyJson(OPTIONS_PROPERTY, options);
    }

    /**
     * Tells the client which language files fit the UI's locale, for a build without {@code language} in its options.
     * The client takes the first name it has a file for, so the list of Froala's language files lives in one place.
     */
    private void sendLocaleLanguages() {
        JsonArray names = Json.createArray();
        languageFileCandidates(getLocale()).forEach(name -> names.set(names.length(), name));
        getElement().setPropertyJson(LOCALE_LANGUAGES_PROPERTY, names);
    }

    /**
     * The language file names that fit a locale, best first: language and country such as {@code zh_cn}, then the
     * language alone such as {@code de}. Froala names its files that way, lower case and joined by an underscore.
     */
    static List<String> languageFileCandidates(Locale locale) {
        String language = locale.getLanguage();
        if (language.isEmpty()) {
            return List.of();
        }

        String country = locale.getCountry().toLowerCase(Locale.ROOT);

        return country.isEmpty() ? List.of(language) : List.of(language + "_" + country, language);
    }

    /**
     * Configures the underlying Froala editor from raw JSON, for options {@link FroalaOptions} has no method for.
     * Identical to {@link #setOptions(FroalaOptions)} in every other respect.
     *
     * @param options Froala options as a JSON object literal, or null for the add-on's defaults
     * @throws IllegalArgumentException if the given string is not parseable as a JSON object, or if it contains
     *             Froala's {@code events} option
     */
    public void setOptions(String options) {
        if (options == null) {
            applyOptions(null);
            return;
        }

        applyOptions(FroalaOptions.parseObject(options, "Froala options"));
    }

    /**
     * Returns the JSON the editor is configured with, whichever {@code setOptions} overload was used. Null if none was
     * called or the last call passed null, in which case the editor uses the add-on's defaults.
     *
     * @return the options as JSON, or null
     */
    public String getOptionsJson() {
        return optionsJson;
    }

    /**
     * Inserts an HTML snippet at the caret, replacing the selected content if there is any. Maps to Froala's
     * {@code html.insert}.
     *
     * <p>
     * Froala cleans the snippet with its own HTML cleaning before inserting it, so what ends up in the editor can
     * differ from the given markup. The cleaning always runs, because {@code html.insert}'s optional flags that skip it
     * or force a block split are not offered. The value on the server follows through the regular client update, which
     * means {@link #getValue()} does not include the snippet yet when this method returns. A value change listener does
     * receive it, as a change from the client.
     *
     * <p>
     * Nothing is inserted while the editor is read-only or disabled. Both lock the value against changes from the
     * client, and the snippet would reach the server as such a change.
     *
     * @param html the HTML snippet to insert, not null
     */
    public void replaceSelectionContent(String html) {
        Objects.requireNonNull(html, "html must not be null");

        getElement().callJsFunction("replaceSelectionContent", html);
    }

    /**
     * Selects the whole content of the editor. Maps to Froala's {@code commands.selectAll}. The selection change
     * listener reports the selection as it does one the user makes. A call that arrives before the editor is
     * initialized, e.g. in the same round trip as the attach, is applied once it is.
     */
    public void selectAll() {
        getElement().callJsFunction("selectAll");
    }

    /**
     * Lets users upload images in the editor. The handler stores each uploaded image and returns the link the editor
     * puts into the document. Covers the upload button, dropping an image onto the editor and pasting one. Needs
     * {@link FroalaPlugin#IMAGE}, which {@link FroalaPlugin#basics()} does not contain, and the upload button needs
     * {@link FroalaButton#INSERT_IMAGE} in the toolbar.
     *
     * <pre>
     * editor.setImageUploadHandler(event -&gt; {
     *     String id = storage.save(event.getInputStream()); // an id of its own, never the client's file name
     *     return "/images/" + id;
     * });
     * </pre>
     *
     * <p>
     * Without a handler, and without Froala's {@code imageUploadURL} in the options, image upload is switched off.
     * Froala would otherwise insert a {@code blob:} URL, which is valid only in the browser tab that created it, so the
     * stored HTML points at nothing after a reload. A handler takes precedence over
     * {@link FroalaOptions#withImageUploadUrl(String)}.
     *
     * <p>
     * The upload goes through Flow, so it needs no endpoint of its own and is refused while the editor is disabled or
     * read-only. Setting or removing a handler on an attached editor rebuilds it. See
     * {@link #setOptions(FroalaOptions)}.
     *
     * @param handler the handler, or null to remove it
     */
    public void setImageUploadHandler(FroalaUploadHandler handler) {
        setUploadHandler("image-upload-url", handler);
    }

    /**
     * Lets users upload files in the editor, which inserts a link to each one. The handler stores each uploaded file
     * and returns the link the editor puts into the document. Covers the upload button in the file popup and dropping a
     * file that is no image onto the editor. A dropped image goes to
     * {@link #setImageUploadHandler(FroalaUploadHandler)}. Needs {@link FroalaPlugin#FILE}, which
     * {@link FroalaPlugin#basics()} does not contain, and the upload button needs {@link FroalaButton#INSERT_FILE} in
     * the toolbar.
     *
     * <pre>
     * editor.setFileUploadHandler(event -&gt; {
     *     String id = storage.save(event.getInputStream()); // an id of its own, never the client's file name
     *     return "/files/" + id;
     * });
     * </pre>
     *
     * <p>
     * Without a handler, and without Froala's {@code fileUploadURL} in the options, file upload is switched off. Froala
     * would otherwise insert a {@code blob:} URL, which is valid only in the browser tab that created it, so the stored
     * HTML points at nothing after a reload. A handler takes precedence over
     * {@link FroalaOptions#withFileUploadUrl(String)}.
     *
     * <p>
     * The upload goes through Flow, so it needs no endpoint of its own and is refused while the editor is disabled or
     * read-only. Setting or removing a handler on an attached editor rebuilds it. See
     * {@link #setOptions(FroalaOptions)}.
     *
     * @param handler the handler, or null to remove it
     */
    public void setFileUploadHandler(FroalaUploadHandler handler) {
        setUploadHandler("file-upload-url", handler);
    }

    /**
     * Lets users upload videos in the editor. The handler stores each uploaded video and returns the link the editor
     * puts into the document. Covers the upload button in the video popup and dropping a video onto the editor. Needs
     * {@link FroalaPlugin#VIDEO}, which {@link FroalaPlugin#basics()} does not contain, and the upload button needs
     * {@link FroalaButton#INSERT_VIDEO} in the toolbar.
     *
     * <pre>
     * editor.setVideoUploadHandler(event -&gt; {
     *     String id = storage.save(event.getInputStream()); // an id of its own, never the client's file name
     *     return "/videos/" + id;
     * });
     * </pre>
     *
     * <p>
     * Without a handler, and without Froala's {@code videoUploadURL} in the options, video upload is switched off.
     * Froala would otherwise insert a {@code blob:} URL, which is valid only in the browser tab that created it, so the
     * stored HTML points at nothing after a reload. A handler takes precedence over
     * {@link FroalaOptions#withVideoUploadUrl(String)}.
     *
     * <p>
     * The upload goes through Flow, so it needs no endpoint of its own and is refused while the editor is disabled or
     * read-only. Setting or removing a handler on an attached editor rebuilds it. See
     * {@link #setOptions(FroalaOptions)}.
     *
     * @param handler the handler, or null to remove it
     */
    public void setVideoUploadHandler(FroalaUploadHandler handler) {
        setUploadHandler("video-upload-url", handler);
    }

    /** Flow turns the handler into a URL in the attribute, which the client passes to Froala as the upload URL. */
    private void setUploadHandler(String attribute, FroalaUploadHandler handler) {
        if (handler == null) {
            getElement().removeAttribute(attribute);
        } else {
            getElement().setAttribute(attribute, new LinkUpload(handler));
        }
    }

    /**
     * Adds a command of the application's own to this editor and runs the listener whenever the user triggers it in
     * this editor, by a button or by its shortcut. Where the command's button appears is decided by its name, in the
     * toolbar or in a popup's button list:
     *
     * <pre>
     * FroalaCommand insertTemplate = new FroalaCommand("insertTemplate", "Insert template",
     *         VaadinIcon.FILE_TEXT.create());
     * editor.setOptions(FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.of("bold", "insertTemplate")));
     * editor.addCommand(insertTemplate, event -&gt; editor.replaceSelectionContent("&lt;p&gt;Dear ...&lt;/p&gt;"));
     * </pre>
     *
     * <p>
     * Froala builds its toolbar and popups once, when the editor is built. Adding or removing a command on an attached
     * editor therefore rebuilds it, with the same losses as {@link #setOptions(FroalaOptions)}. Several calls within
     * one server round trip cause one rebuild.
     *
     * <p>
     * Froala keeps a command's title, icon, shortcut and whether it is a toggle for the whole page, not per editor. Two
     * editors that add a command of the same name with a different definition show the definition of the editor built
     * last in both. A command name that is also one of Froala's own, e.g. {@code bold}, replaces Froala's command in
     * every editor on the page, and a shortcut replaces one of Froala's with the same keys.
     *
     * <p>
     * A shortcut works only while the command is listed in the {@code shortcutsEnabled} option. Froala lists it there
     * by default, but options that set {@code shortcutsEnabled} themselves have to name the command.
     *
     * @param command the command, not null
     * @param listener runs when the user triggers the command in this editor, not null
     * @return a handle that removes the command from this editor again
     * @throws IllegalArgumentException if this editor already has a command of the same name
     */
    public Registration addCommand(FroalaCommand command, ComponentEventListener<FroalaCommandEvent> listener) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        return addCommand(new AddedCommand(command, listener, null));
    }

    /**
     * Adds a command of the application's own to this editor, whose toolbar button opens the given popover next to it.
     * The popover is a plain one, built by the application:
     *
     * <pre>
     * Popover popover = new Popover(new Paragraph("My own popup"));
     * editor.addCommand(new FroalaCommand("myPopup", "My popup", VaadinIcon.INFO_CIRCLE.create()), popover);
     * </pre>
     *
     * <p>
     * The popover opens and closes on a click on the button, as its own settings say. The command's shortcut opens it
     * too. The editor never closes it, so how it closes is up to the popover's configuration. Example: a popover with
     * {@code setCloseOnOutsideClick(false)} and {@code setCloseOnEsc(false)} stays open until its button is clicked
     * again, also while the editor is rebuilt.
     *
     * <p>
     * The editor takes the popover as its own. It becomes the popover's {@link Popover#setTarget(Component) target} on
     * the server and puts the popover into the UI, so the application neither adds it to a layout nor sets a target of
     * its own. In the browser the popover's target is the command's toolbar button. In an editor whose toolbar does not
     * list the command there is no button, so the popover never opens there, not even by the shortcut.
     *
     * <p>
     * Everything else is as in {@link #addCommand(FroalaCommand, ComponentEventListener)}.
     *
     * @param command the command, not null
     * @param popover opens at the command's toolbar button in this editor, not null
     * @return a handle that removes the command from this editor again, and takes the popover out of the UI
     * @throws IllegalArgumentException if this editor already has a command of the same name, or the popover already
     *             has a target, for example because it belongs to another command
     */
    public Registration addCommand(FroalaCommand command, Popover popover) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(popover, "popover must not be null");
        // Shared by two commands, removing one would take the target from the other
        if (popover.getTarget() != null) {
            throw new IllegalArgumentException(
                    "The popover already has a target. Each command needs a popover of its own");
        }

        AddedCommand added = new AddedCommand(command, event -> {
        }, popover);
        Registration registration = addCommand(added);
        popover.setTarget(this);
        sendPopover(added);

        return registration;
    }

    private Registration addCommand(AddedCommand added) {
        String name = added.command().getName();
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("This editor already has a command named '" + name + "'");
        }

        commands.put(name, added);
        sendCommands();

        return () -> {
            // a stale handle must not remove a later command of the same name
            if (commands.remove(name, added)) {
                sendCommands();
                if (activeCommands.remove(name)) {
                    sendActiveCommands();
                }
                if (added.popover() != null) {
                    added.popover().setTarget(null);
                    getElement().callJsFunction("_setCommandPopover", name, null);
                }
            }
        };
    }

    /**
     * Shows the button of a toggle command as pressed or released in this editor, e.g. in the command's listener:
     *
     * <pre>
     * FroalaCommand reviewMode = new FroalaCommand("reviewMode", "Review mode", VaadinIcon.EYE.create()).withToggle();
     * editor.addCommand(reviewMode, event -&gt; editor.setCommandActive(reviewMode, !editor.isCommandActive(reviewMode)));
     * </pre>
     *
     * <p>
     * The state belongs to this editor, so two editors with the same command show their own. Setting it does not build
     * the editor again, and a rebuild or a detach and re-attach keeps it. Removing the command drops it.
     *
     * @param command a toggle command of this editor, matched by its name, not null
     * @param active whether the button shows as pressed
     * @throws IllegalArgumentException if this editor has no command of that name, or it is not a
     *             {@link FroalaCommand#withToggle() toggle}
     */
    public void setCommandActive(FroalaCommand command, boolean active) {
        requireToggle(command);
        if (active ? activeCommands.add(command.getName()) : activeCommands.remove(command.getName())) {
            sendActiveCommands();
        }
    }

    /**
     * Returns whether the button of a toggle command shows as pressed in this editor.
     *
     * @param command a toggle command of this editor, matched by its name, not null
     * @return whether the button shows as pressed
     * @throws IllegalArgumentException if this editor has no command of that name, or it is not a
     *             {@link FroalaCommand#withToggle() toggle}
     */
    public boolean isCommandActive(FroalaCommand command) {
        requireToggle(command);

        return activeCommands.contains(command.getName());
    }

    private void requireToggle(FroalaCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AddedCommand added = commands.get(command.getName());
        if (added == null) {
            throw new IllegalArgumentException("This editor has no command named '" + command.getName() + "'");
        }

        if (!added.command().isToggle()) {
            throw new IllegalArgumentException("The command '" + command.getName() + "' is not a toggle");
        }
    }

    private void sendActiveCommands() {
        JsonArray json = Json.createArray();
        activeCommands.forEach(name -> json.set(json.length(), name));
        getElement().setPropertyJson(ACTIVE_COMMANDS_PROPERTY, json);
    }

    /**
     * Tells the client which popover belongs to the command. Froala replaces the command's button on every build, so
     * the client points the popover at the new one each time.
     */
    private void sendPopover(AddedCommand added) {
        if (added.popover() != null) {
            getElement().callJsFunction("_setCommandPopover", added.command().getName(), added.popover().getElement());
        }
    }

    /** Tells the client about the commands, which builds the editor again once it is running. */
    private void sendCommands() {
        // that build reads the locale as it is now, like one caused by setOptions
        sendLocaleLanguages();

        JsonArray json = Json.createArray();
        commands.values().forEach(added -> json.set(json.length(), added.command().toJson()));
        getElement().setPropertyJson(COMMANDS_PROPERTY, json);
    }

    /**
     * Adds a listener that learns when something gets selected in the editor and when the selection is gone again,
     * because it collapsed to a caret or moved outside the editor. Useful to enable an action that works on the
     * selection, such as one calling {@link #replaceSelectionContent(String)}, only while there is one.
     *
     * <p>
     * Clicking a button outside the editor leaves the selection in place, so an action in the view still finds it.
     *
     * @param listener the listener, not null
     * @return a handle to remove the listener
     */
    public Registration addSelectionChangeListener(ComponentEventListener<FroalaSelectionChangeEvent> listener) {
        return addListener(FroalaSelectionChangeEvent.class, listener);
    }

    /**
     * Sets the license key of this instance. Maps to Froala's {@code key} option.
     *
     * <p>
     * The add-on ships no key. Without one, Froala shows its unlicensed watermark.
     *
     * <p>
     * The key is read when the editor is built, on attach and on every rebuild, such as one caused by
     * {@link #setOptions(FroalaOptions)}. Calling this on an attached instance has no effect until then.
     *
     * @param licenseKey license key or null to unset
     */
    public void setLicenseKey(String licenseKey) {
        if (licenseKey == null) {
            getElement().removeProperty(LICENSE_KEY_PROPERTY);
        } else {
            getElement().setProperty(LICENSE_KEY_PROPERTY, licenseKey);
        }
    }

    /**
     * Returns the license key of this instance. May be null.
     *
     * @return license key or null
     */
    public String getLicenseKey() {
        return getElement().getProperty(LICENSE_KEY_PROPERTY);
    }

    /**
     * Sets the value change mode of this instance. By default the editor uses {@link FroalaValueChangeMode#ON_CHANGE}.
     * Takes effect at once on an attached editor, without a rebuild.
     *
     * @param valueChangeMode the new value change mode, not null
     * @throws NullPointerException if the value change mode is null
     */
    public void setValueChangeMode(FroalaValueChangeMode valueChangeMode) {
        Objects.requireNonNull(valueChangeMode, "valueChangeMode must not be null");
        getElement().setProperty(VALUE_CHANGE_MODE_PROPERTY, valueChangeMode.getClientValue());
    }

    /**
     * Returns the current value change mode. Never null.
     *
     * @return value change mode
     */
    public FroalaValueChangeMode getValueChangeMode() {
        return FroalaValueChangeMode.fromClientValue(
                getElement().getProperty(VALUE_CHANGE_MODE_PROPERTY, DEFAULT_VALUE_CHANGE_MODE.getClientValue()));
    }

    /**
     * Sets the idle time in milliseconds after the last keystroke before the editor reports the change. This is
     * Froala's {@code typingTimer} option, not a timer of this component. Froala restarts it on every keystroke, which
     * is why {@link FroalaValueChangeMode#ON_CHANGE} reports once the user pauses rather than per keystroke.
     *
     * <p>
     * The default is {@value #DEFAULT_VALUE_CHANGE_TIMEOUT}, the minimum {@value #MIN_VALUE_CHANGE_TIMEOUT}. Froala
     * reports changes after at least that long whatever the option says, so a smaller value would silently have no
     * effect. This setter throws instead. Takes effect at once on an attached editor, without a rebuild.
     *
     * <p>
     * Froala uses the option for more than reporting changes. It is also the delay before the inline toolbar is shown
     * again after a keystroke, so a long timeout delays that as well.
     *
     * <p>
     * Only {@link FroalaValueChangeMode#ON_CHANGE} uses this value. {@link FroalaValueChangeMode#ON_BLUR} and
     * {@link FroalaValueChangeMode#INTERVAL} are triggered by something else and are not delayed by it.
     *
     * @param timeoutInMilliseconds idle time before a change is reported, at least {@value #MIN_VALUE_CHANGE_TIMEOUT}
     * @throws IllegalArgumentException if the given timeout is below {@value #MIN_VALUE_CHANGE_TIMEOUT}
     */
    public void setValueChangeTimeout(int timeoutInMilliseconds) {
        if (timeoutInMilliseconds < MIN_VALUE_CHANGE_TIMEOUT) {
            throw new IllegalArgumentException(
                    "valueChangeTimeout must be at least " + MIN_VALUE_CHANGE_TIMEOUT + " ms, the minimum Froala uses");
        }

        getElement().setProperty(VALUE_CHANGE_TIMEOUT_PROPERTY, timeoutInMilliseconds);
    }

    /**
     * Returns the idle time in milliseconds that has to pass after the last keystroke before the editor reports the
     * change. The default is {@value #DEFAULT_VALUE_CHANGE_TIMEOUT}.
     *
     * <p>
     * This returns what {@link #setValueChangeTimeout(int)} was given, not what the editor runs on. A
     * {@code typingTimer} passed as raw JSON through {@link #setOptions(String)} takes effect as long as the setter was
     * never called, but is not reported here.
     *
     * @return idle time in milliseconds
     */
    public int getValueChangeTimeout() {
        return getElement().getProperty(VALUE_CHANGE_TIMEOUT_PROPERTY, DEFAULT_VALUE_CHANGE_TIMEOUT);
    }

    /**
     * Sets how many milliseconds pass between two sends of the value in {@link FroalaValueChangeMode#INTERVAL}. Also
     * the time before the first one. Has no effect in any other mode. Takes effect at once on an attached editor,
     * without a rebuild.
     *
     * <p>
     * The default is {@value #DEFAULT_INTERVAL_PERIOD}. The value must be greater than zero.
     *
     * @param periodInMilliseconds time between two sends of the value, greater than zero
     * @throws IllegalArgumentException if the given period is zero or negative
     */
    public void setIntervalPeriod(int periodInMilliseconds) {
        if (periodInMilliseconds <= 0) {
            throw new IllegalArgumentException("intervalPeriod must be greater than 0");
        }

        getElement().setProperty(INTERVAL_PERIOD_PROPERTY, periodInMilliseconds);
    }

    /**
     * Returns how many milliseconds pass between two sends of the value in {@link FroalaValueChangeMode#INTERVAL}. The
     * default is {@value #DEFAULT_INTERVAL_PERIOD}.
     *
     * @return time between two sends of the value
     */
    public int getIntervalPeriod() {
        return getElement().getProperty(INTERVAL_PERIOD_PROPERTY, DEFAULT_INTERVAL_PERIOD);
    }

    /**
     * Receives Froala's upload and answers with the {@code {"link": "…"}} JSON Froala expects. Flow already refuses an
     * upload to a disabled editor. A refusal answers 403 for a read-only editor and 413 for a file over the handler's
     * limit, without an exception, so that it is not logged as an error.
     */
    private final class LinkUpload implements UploadHandler {

        private final FroalaUploadHandler handler;

        private LinkUpload(FroalaUploadHandler handler) {
            this.handler = handler;
        }

        @Override
        public long getFileSizeMax() {
            return handler.getFileSizeMax();
        }

        @Override
        public void handleUploadRequest(UploadEvent event) throws IOException {
            // a form field such as Froala's imageUploadParams, not a file
            if (event.getFileName() == null) {
                return;
            }

            // Flow runs an upload outside the session lock. Read-only locks the value against the client like disabled
            // does, and the uploaded file would reach the value as a change from the client.
            boolean[] readOnly = new boolean[1];
            event.getUI().accessSynchronously(() -> readOnly[0] = isReadOnly());
            if (readOnly[0]) {
                event.getResponse().setStatus(HttpStatusCode.FORBIDDEN.getCode());
                return;
            }

            // Flow applies getFileSizeMax only when it parses a multipart request itself. Not where the servlet
            // container has parsed it already, as under Spring Boot, and not for a body that is not multipart at all,
            // whose size can be unknown. Froala always sends multipart with a known size.
            long max = handler.getFileSizeMax();
            if (max >= 0 && (event.getFileSize() < 0 || event.getFileSize() > max)) {
                event.getResponse().setStatus(HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.getCode());
                return;
            }

            String link = Objects.requireNonNull(handler.upload(event), "The upload handler returned no link");

            JsonObject body = Json.createObject();
            body.put("link", link);
            event.getResponse().setContentType("application/json;charset=UTF-8");
            event.getResponse().getWriter().write(body.toJson());
        }

        /**
         * Flow's default sets 200 on success, which would overwrite a refusal's status. The servlet's own default is
         * 200 already, so only a failure needs one.
         */
        @Override
        public void responseHandled(boolean success, VaadinResponse response) {
            if (!success) {
                response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
            }
        }
    }

    /** A command together with the listener it was added with, and its popover or null for none. */
    private record AddedCommand(FroalaCommand command, ComponentEventListener<FroalaCommandEvent> listener,
            Popover popover) implements Serializable {
    }
}
