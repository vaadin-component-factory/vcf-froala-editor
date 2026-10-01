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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.github.mvysny.kaributesting.v10.ElementUtilsKt;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browserless UI-unit test using Karibu Testing (license-free). It runs the component in the JVM, without a browser.
 *
 * <p>
 * Karibu executes no JavaScript, so everything here is server-side state, element properties and the JS calls the
 * server queues. Whether Froala then honours them is an e2e question, see {@code FroalaEditorIT}.
 *
 * <p>
 * Every test builds the component it asserts on. Nothing here navigates to a view.
 */
class FroalaEditorKaribuTest {

    private VerticalLayout layout;

    @BeforeEach
    void setup() {
        MockVaadin.setup();

        layout = new VerticalLayout();
        UI.getCurrent().add(layout);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void setValue_reachesTheClientProperty() {
        FroalaEditor editor = attachedEditor();

        editor.setValue("<p>typed on the server</p>");

        assertEquals("<p>typed on the server</p>", editor.getValue());
        assertEquals("<p>typed on the server</p>", editor.getElement().getProperty("value"));
    }

    @Test
    void nullValue_isRefusedLikeInATextField() {
        // A delta cannot be applied to null, so a null value would turn the next edit into garbage
        assertThrows(NullPointerException.class, () -> attachedEditor().setValue(null));
    }

    @Test
    void clientDelta_updatesTheValueAsAChangeFromTheClient() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>hello world</p>");
        List<String> seen = new ArrayList<>();
        editor.addValueChangeListener(event -> seen.add(event.getValue() + " " + event.isFromClient()));

        fireDelta(editor, "<p>hello world</p>", "<p>hello brave world</p>");

        assertEquals("<p>hello brave world</p>", editor.getValue());
        assertEquals(List.of("<p>hello brave world</p> true"), seen);
    }

    @Test
    void driftedDelta_asksTheClientToResendInsteadOfApplyingIt() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>the server's text</p>");
        drainPendingJavaScript();

        fireDelta(editor, "<p>something the server never had</p>", "<p>something the server never had, edited</p>");

        assertEquals("<p>the server's text</p>", editor.getValue());
        assertTrue(hasPendingJavaScript("_resyncValue"));
    }

    @Test
    void malformedDelta_asksTheClientToResend() {
        FroalaEditor editor = attachedEditor();
        drainPendingJavaScript();

        fireClientEvent(editor, "_value-delta", "event.detail.delta", "@@ not a patch");

        assertTrue(hasPendingJavaScript("_resyncValue"));
    }

    @Test
    void resync_replacesTheValueAsAChangeFromTheClient() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>stale</p>");
        List<Boolean> fromClient = new ArrayList<>();
        editor.addValueChangeListener(event -> fromClient.add(event.isFromClient()));

        fireClientEvent(editor, "_value-resync", "event.detail.value", "<p>what the user sees</p>");

        assertEquals("<p>what the user sees</p>", editor.getValue());
        assertEquals(List.of(true), fromClient);
    }

    @Test
    void readOnly_revertsAClientDelta() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>locked</p>");
        editor.setReadOnly(true);

        fireDelta(editor, "<p>locked</p>", "<p>locked, edited anyway</p>");

        assertEquals("<p>locked</p>", editor.getValue());
    }

    @Test
    void licenseKey_isSetAsElementPropertyAndNullRemovesIt() {
        FroalaEditor editor = attachedEditor();

        editor.setLicenseKey("test-key");
        assertEquals("test-key", editor.getElement().getProperty("licenseKey"));

        editor.setLicenseKey(null);
        assertFalse(editor.getElement().hasProperty("licenseKey"));
    }

    @Test
    void uploadHandler_givesTheElementAnUploadUrlAndNullTakesItAway() {
        FroalaEditor editor = attachedEditor();

        // one at a time, so that two setters writing each other's attribute would show
        editor.setImageUploadHandler(event -> "/images/1");
        assertEquals(List.of("image-upload-url"), uploadAttributesOn(editor));
        editor.setImageUploadHandler(null);

        editor.setFileUploadHandler(event -> "/files/1");
        assertEquals(List.of("file-upload-url"), uploadAttributesOn(editor));
        editor.setFileUploadHandler(null);

        editor.setVideoUploadHandler(event -> "/videos/1");
        assertEquals(List.of("video-upload-url"), uploadAttributesOn(editor));
        editor.setVideoUploadHandler(null);

        assertEquals(List.of(), uploadAttributesOn(editor));
    }

    @Test
    void detach_reportsThatAReportedSelectionIsGone() {
        FroalaEditor editor = attachedEditor();
        List<Boolean> seen = new ArrayList<>();
        editor.addSelectionChangeListener(event -> seen.add(event.hasSelection()));
        fireClientEvent(editor, "selection-change", "event.detail.hasSelection", true);

        layout.remove(editor);

        assertEquals(List.of(true, false), seen);
    }

    @Test
    void replaceSelectionContent_rejectsNull() {
        assertThrows(NullPointerException.class, () -> attachedEditor().replaceSelectionContent(null));
    }

    @Test
    void themeVariant_reachesTheThemeAttributeTheStylesheetSelectsOn() {
        FroalaEditor editor = attachedEditor();

        // the literals vcf-froala-theme-vaadin.css selects on, not the enum's own names
        editor.addThemeVariants(FroalaEditorVariant.OUTLINED, FroalaEditorVariant.NO_HOVER_HIGHLIGHT);

        assertEquals(Set.of("outlined", "no-hover-highlight"), Set.copyOf(editor.getElement().getThemeList()));
    }

    @Test
    void valueChangeMode_roundTripsAndDefaults() {
        FroalaEditor editor = attachedEditor();

        // the strings the client compares against
        Map.of(FroalaValueChangeMode.ON_CHANGE, "change", FroalaValueChangeMode.ON_BLUR, "blur",
                FroalaValueChangeMode.INTERVAL, "interval").forEach((mode, clientValue) -> {
                    editor.setValueChangeMode(mode);
                    assertEquals(mode, editor.getValueChangeMode());
                    assertEquals(clientValue, editor.getElement().getProperty("valueChangeMode"));
                });

        editor.setValueChangeMode(null);
        assertEquals(FroalaEditor.DEFAULT_VALUE_CHANGE_MODE, editor.getValueChangeMode());
    }

    @Test
    void valueChangeTimeout_rejectsAnythingFroalaWouldIgnore() {
        FroalaEditor editor = attachedEditor();

        assertThrows(IllegalArgumentException.class, () -> editor.setValueChangeTimeout(-1));

        // zero must fail here at the call site, not later in the client's own setter
        assertThrows(IllegalArgumentException.class, () -> editor.setValueChangeTimeout(0));

        // Froala uses at least 250 ms for change reporting, whatever typingTimer says. A smaller value accepted here
        // would be quietly ignored, and the editor would keep syncing at 250 while the getter claimed otherwise.
        assertThrows(IllegalArgumentException.class,
                () -> editor.setValueChangeTimeout(FroalaEditor.MIN_VALUE_CHANGE_TIMEOUT - 1));

        editor.setValueChangeTimeout(FroalaEditor.MIN_VALUE_CHANGE_TIMEOUT);
        assertEquals(FroalaEditor.MIN_VALUE_CHANGE_TIMEOUT, editor.getValueChangeTimeout());
    }

    @Test
    void intervalPeriod_roundTripsAndRejectsAnythingButPositiveValues() {
        FroalaEditor editor = attachedEditor();

        assertEquals(FroalaEditor.DEFAULT_INTERVAL_PERIOD, editor.getIntervalPeriod());

        editor.setIntervalPeriod(5000);
        assertEquals(5000, editor.getIntervalPeriod());

        assertThrows(IllegalArgumentException.class, () -> editor.setIntervalPeriod(0));
        assertThrows(IllegalArgumentException.class, () -> editor.setIntervalPeriod(-1));
    }

    @Test
    void setValueRepeatingTheLastServerValue_queuesAnExplicitClientPush() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>A</p>");
        fireDelta(editor, "<p>A</p>", "<p>B</p>");
        drainPendingJavaScript();

        editor.setValue("<p>A</p>");

        // Same value the property already holds, so Flow sends no property update and the browser would ignore one
        // anyway. This is the case that needs the explicit push.
        assertTrue(hasPendingValuePush());
    }

    @Test
    void detach_doesNotQueueAValuePushForTheNextAttach() {
        FroalaEditor editor = attachedEditor();
        editor.setValue("<p>A</p>");
        drainPendingJavaScript();

        // The detach listener calls setPresentationValue with the value the property already holds, which is exactly
        // the shape the explicit push reacts to. A push queued here is not dropped. Flow defers it to the next attach,
        // where it would overwrite whatever the server set in between. isAttached() is no guard against it, because
        // it still answers true inside a detach listener.
        layout.remove(editor);
        editor.setValue("<p>B</p>");
        layout.add(editor);

        assertFalse(hasPendingValuePush());
    }

    @Test
    void binder_readsAndWritesTheEditorLikeAnyOtherField() {
        FroalaEditor editor = attachedEditor();

        Binder<Note> binder = new Binder<>();
        binder.forField(editor).asRequired("a note needs a body").bind(Note::getBody, Note::setBody);

        Note note = new Note();
        note.setBody("<p>from the bean</p>");
        binder.readBean(note);
        assertEquals("<p>from the bean</p>", editor.getValue());

        editor.setValue("<p>edited in the editor</p>");
        assertTrue(binder.writeBeanIfValid(note));
        assertEquals("<p>edited in the editor</p>", note.getBody());

        // asRequired works off the field's empty value, which is the empty string for this one. That is what makes
        // AbstractSinglePropertyField the right base.
        editor.setValue("");
        assertFalse(binder.writeBeanIfValid(note));
        assertEquals("<p>edited in the editor</p>", note.getBody());
    }

    @Test
    void languageFileCandidates_tryLanguageAndCountryThenTheLanguageAlone() {
        assertEquals(List.of("zh_cn", "zh"), FroalaEditor.languageFileCandidates(Locale.SIMPLIFIED_CHINESE));
        assertEquals(List.of("pt_br", "pt"), FroalaEditor.languageFileCandidates(Locale.forLanguageTag("pt-BR")));
        assertEquals(List.of("de_at", "de"), FroalaEditor.languageFileCandidates(Locale.forLanguageTag("de-AT")));
        assertEquals(List.of("de"), FroalaEditor.languageFileCandidates(Locale.GERMAN));
        assertEquals(List.of("he"), FroalaEditor.languageFileCandidates(Locale.forLanguageTag("he")));
        assertEquals(List.of(), FroalaEditor.languageFileCandidates(Locale.ROOT));
    }

    @Test
    void uiLocale_reachesTheElementOnAttach() {
        UI.getCurrent().setLocale(Locale.forLanguageTag("de-AT"));

        FroalaEditor editor = attachedEditor();

        assertEquals("[\"de_at\",\"de\"]", localeLanguagesOn(editor));
    }

    @Test
    void setOptions_sendsTheUiLocaleAsItIsNow() {
        FroalaEditor editor = attachedEditor();
        UI.getCurrent().setLocale(Locale.forLanguageTag("ar"));

        editor.setOptions(FroalaOptions.defaults());

        assertEquals("[\"ar\"]", localeLanguagesOn(editor));
    }

    @Test
    void options_reachTheElementAsJson() {
        FroalaEditor editor = new FroalaEditor();
        layout.add(editor);

        editor.setOptions(FroalaOptions.defaults().withPlaceholderText("Write something"));

        assertEquals("{\"placeholderText\":\"Write something\"}", optionsOn(editor));
        assertEquals("{\"placeholderText\":\"Write something\"}", editor.getOptionsJson());
    }

    @Test
    void options_canBeGivenToTheConstructor() {
        // A configured editor has to arrive configured. Options are read once, when Froala builds, so setting them
        // after the first attach would cost a rebuild for nothing.
        FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withToolbarInline(true));

        assertEquals("{\"toolbarInline\":true}", optionsOn(editor));
    }

    @Test
    void rawOptions_takeTheSamePath() {
        // The two overloads exist so that anything FroalaOptions does not type yet is still reachable. They have to
        // end in the same property, or "not typed yet" would mean "behaves differently".
        FroalaEditor typed = new FroalaEditor(FroalaOptions.defaults().withCharCounterMax(10));
        FroalaEditor raw = new FroalaEditor();
        raw.setOptions("{\"charCounterMax\": 10}");

        assertEquals(optionsOn(typed), optionsOn(raw));
    }

    @Test
    void settingOptionsAgain_replacesRatherThanMerges() {
        FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withLanguage("de"));

        editor.setOptions(FroalaOptions.defaults().withPlaceholderText("Write something"));

        assertEquals("{\"placeholderText\":\"Write something\"}", optionsOn(editor));
    }

    @Test
    void nullOptions_leaveFroalaOnItsOwnDefaults() {
        FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withLanguage("de"));

        editor.setOptions((FroalaOptions) null);

        assertNull(editor.getElement().getPropertyRaw("options"));
        assertNull(editor.getOptionsJson());
    }

    @Test
    void brokenJson_isRejectedWhereItWasWritten() {
        FroalaEditor editor = new FroalaEditor();

        assertThrows(IllegalArgumentException.class, () -> editor.setOptions("{not json"));
        assertThrows(IllegalArgumentException.class, () -> editor.setOptions("[1, 2, 3]"));
        assertThrows(IllegalArgumentException.class, () -> editor.setOptions(""));

        // elemental's own parser would take this and drop the rest
        assertThrows(IllegalArgumentException.class, () -> editor.setOptions("{\"tabSpaces\": 4} trailing"));
    }

    @Test
    void eventsOption_isRejected() {
        // Froala's `events` is a map of callbacks and JSON carries no functions, so it can only ever arrive here as
        // data Froala would then try to call. Failing in Java beats failing in the browser.
        FroalaEditor editor = new FroalaEditor();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> editor.setOptions("{\"events\": {\"initialized\": \"nope\"}}"));

        assertTrue(thrown.getMessage().contains("callbacks"));
    }

    @Test
    void addCommand_sendsTheCommandAndRemoveTakesItBack() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand first = new FroalaCommand("first", "First", VaadinIcon.STAR.create());
        FroalaCommand second = new FroalaCommand("second", "Second", VaadinIcon.STAR.create());

        Registration registration = editor.addCommand(first, event -> {
        });
        editor.addCommand(second, event -> {
        });
        assertEquals("[" + first.toJson().toJson() + "," + second.toJson().toJson() + "]", commandsOn(editor));

        registration.remove();
        assertEquals("[" + second.toJson().toJson() + "]", commandsOn(editor));
    }

    @Test
    void addCommand_beforeAttach_reachesTheElement() {
        FroalaEditor editor = new FroalaEditor();
        FroalaCommand command = new FroalaCommand("early", "Early", VaadinIcon.STAR.create());

        editor.addCommand(command, event -> {
        });
        layout.add(editor);

        assertEquals("[" + command.toJson().toJson() + "]", commandsOn(editor));
    }

    @Test
    void commandEvent_reachesTheListenerOfThatCommandOnly() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand first = new FroalaCommand("first", "First", VaadinIcon.STAR.create());
        List<String> seen = new ArrayList<>();
        editor.addCommand(first,
                event -> seen.add("first " + event.getCommand().getName() + " " + event.isFromClient()));
        editor.addCommand(new FroalaCommand("second", "Second", VaadinIcon.STAR.create()), event -> seen.add("second"));

        fireCommand(editor, "first");
        // a name this editor has no listener for, such as one removed while the click was on its way
        fireCommand(editor, "unknown");

        assertEquals(List.of("first first true"), seen);
    }

    @Test
    void removedCommand_noLongerReachesItsListener() {
        FroalaEditor editor = attachedEditor();
        List<String> seen = new ArrayList<>();
        Registration registration = editor.addCommand(new FroalaCommand("first", "First", VaadinIcon.STAR.create()),
                event -> seen.add("first"));

        registration.remove();
        fireCommand(editor, "first");

        assertEquals(List.of(), seen);
    }

    @Test
    void sameNameTwice_isRejected() {
        FroalaEditor editor = attachedEditor();
        editor.addCommand(new FroalaCommand("first", "First", VaadinIcon.STAR.create()), event -> {
        });

        assertThrows(IllegalArgumentException.class,
                () -> editor.addCommand(new FroalaCommand("first", "Other", VaadinIcon.STAR.create()), event -> {
                }));
    }

    @Test
    void staleRegistration_leavesALaterCommandOfTheSameNameAlone() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand command = new FroalaCommand("first", "First", VaadinIcon.STAR.create());
        Registration stale = editor.addCommand(command, event -> {
        });
        stale.remove();
        editor.addCommand(command, event -> {
        });

        stale.remove();

        assertEquals("[" + command.toJson().toJson() + "]", commandsOn(editor));
    }

    @Test
    void addCommandWithPopover_sendsTheCommandAndPutsThePopoverIntoTheUi() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand command = new FroalaCommand("first", "First", VaadinIcon.STAR.create());
        Popover popover = new Popover();

        editor.addCommand(command, popover);

        assertEquals("[" + command.toJson().toJson() + "]", commandsOn(editor));
        assertTrue(popover.isAttached());
    }

    @Test
    void removedPopoverCommand_takesThePopoverOutAgain() {
        FroalaEditor editor = attachedEditor();
        Popover popover = new Popover();
        Registration registration = editor.addCommand(new FroalaCommand("first", "First", VaadinIcon.STAR.create()),
                popover);

        registration.remove();

        assertEquals("[]", commandsOn(editor));
        assertNull(popover.getTarget());
        assertFalse(popover.isAttached());
    }

    @Test
    void staleRegistration_leavesTheLaterCommandsPopoverAlone() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand command = new FroalaCommand("first", "First", VaadinIcon.STAR.create());
        Registration stale = editor.addCommand(command, new Popover());
        stale.remove();
        Popover later = new Popover();
        editor.addCommand(command, later);

        stale.remove();

        assertTrue(later.isAttached());
    }

    @Test
    void setCommandActive_sendsTheStateAndIsCommandActiveReadsIt() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand first = new FroalaCommand("first", "First", VaadinIcon.STAR.create()).withToggle();
        FroalaCommand second = new FroalaCommand("second", "Second", VaadinIcon.STAR.create()).withToggle();
        editor.addCommand(first, event -> {
        });
        editor.addCommand(second, event -> {
        });

        editor.setCommandActive(first, true);
        editor.setCommandActive(second, true);
        editor.setCommandActive(first, false);

        assertEquals("[\"second\"]", activeCommandsOn(editor));
        assertFalse(editor.isCommandActive(first));
        assertTrue(editor.isCommandActive(second));
    }

    @Test
    void setCommandActive_takesOnlyAToggleOfThisEditor() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand plain = new FroalaCommand("plain", "Plain", VaadinIcon.STAR.create());
        editor.addCommand(plain, event -> {
        });

        assertThrows(IllegalArgumentException.class, () -> editor.setCommandActive(plain, true));
        assertThrows(IllegalArgumentException.class, () -> editor
                .isCommandActive(new FroalaCommand("other", "Other", VaadinIcon.STAR.create()).withToggle()));
    }

    @Test
    void removedCommand_dropsItsState() {
        FroalaEditor editor = attachedEditor();
        FroalaCommand toggle = new FroalaCommand("first", "First", VaadinIcon.STAR.create()).withToggle();
        Registration registration = editor.addCommand(toggle, event -> {
        });
        editor.setCommandActive(toggle, true);

        registration.remove();
        editor.addCommand(toggle, event -> {
        });

        assertEquals("[]", activeCommandsOn(editor));
        assertFalse(editor.isCommandActive(toggle));
    }

    @Test
    void addCommand_sendsTheUiLocaleAsItIsNow() {
        FroalaEditor editor = attachedEditor();
        UI.getCurrent().setLocale(Locale.forLanguageTag("fi"));

        editor.addCommand(new FroalaCommand("first", "First", VaadinIcon.STAR.create()), event -> {
        });

        assertEquals("[\"fi\"]", localeLanguagesOn(editor));
    }

    private FroalaEditor attachedEditor() {
        FroalaEditor editor = new FroalaEditor();
        layout.add(editor);

        return editor;
    }

    private void drainPendingJavaScript() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();
    }

    /** Fires the event the client sends when one of the editor's commands was triggered in the browser. */
    private void fireCommand(FroalaEditor editor, String name) {
        fireClientEvent(editor, "_command", "event.detail.name", name);
    }

    /** Fires the delta the client sends for an edit from one value to another, computed the way the client does. */
    private void fireDelta(FroalaEditor editor, String from, String to) {
        DiffMatchPatch diffMatchPatch = new DiffMatchPatch();

        fireClientEvent(editor, "_value-delta", "event.detail.delta",
                diffMatchPatch.patchToText(diffMatchPatch.patchMake(from, to)));
    }

    /** Fires a DOM event from the client with one entry of event data, the way Flow delivers it. */
    private void fireClientEvent(FroalaEditor editor, String type, String key, String value) {
        JsonObject data = Json.createObject();
        data.put(key, value);
        ElementUtilsKt._fireDomEvent(editor.getElement(), new DomEvent(editor.getElement(), type, data));
    }

    private void fireClientEvent(FroalaEditor editor, String type, String key, boolean value) {
        JsonObject data = Json.createObject();
        data.put(key, value);
        ElementUtilsKt._fireDomEvent(editor.getElement(), new DomEvent(editor.getElement(), type, data));
    }

    /** The commands as they sit on the element, which is what the client registers with Froala. */
    private String commandsOn(FroalaEditor editor) {
        return ((JsonArray) editor.getElement().getPropertyRaw("commands")).toJson();
    }

    /** The names of the pressed toggle commands as they sit on the element. */
    private String activeCommandsOn(FroalaEditor editor) {
        return ((JsonArray) editor.getElement().getPropertyRaw("activeCommands")).toJson();
    }

    /** The options as they sit on the element, which is what the client will read them from. */
    private String optionsOn(FroalaEditor editor) {
        return ((JsonObject) editor.getElement().getPropertyRaw("options")).toJson();
    }

    /** The locale's language file names as they sit on the element, best first. */
    private String localeLanguagesOn(FroalaEditor editor) {
        return ((JsonArray) editor.getElement().getPropertyRaw("localeLanguages")).toJson();
    }

    private List<String> uploadAttributesOn(FroalaEditor editor) {
        return editor.getElement().getAttributeNames().filter(name -> name.endsWith("-upload-url")).sorted().toList();
    }

    private boolean hasPendingValuePush() {
        return hasPendingJavaScript("this.value = $0");
    }

    /** The invocations only exist once the before-client-response tasks have run, so run them first. */
    private boolean hasPendingJavaScript(String fragment) {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();

        return UI.getCurrent().getInternals().containsPendingJavascript(fragment);
    }

    /** Minimal bean for the Binder test, a field this add-on would realistically be bound to. */
    private static class Note {

        private String body;

        String getBody() {
            return body;
        }

        void setBody(String body) {
            this.body = body;
        }
    }
}
