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

import com.github.mvysny.kaributesting.v10.ElementUtilsKt;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
    void valueChangeListener_firesOnServerSideChange() {
        FroalaEditor editor = attachedEditor();
        String[] seen = new String[1];
        editor.addValueChangeListener(event -> seen[0] = event.getValue());

        editor.setValue("<p>observed</p>");

        assertEquals("<p>observed</p>", seen[0]);
    }

    @Test
    void licenseKey_isSetAsElementProperty() {
        FroalaEditor editor = attachedEditor();

        editor.setLicenseKey("test-key");

        assertEquals("test-key", editor.getLicenseKey());
        assertEquals("test-key", editor.getElement().getProperty("licenseKey"));
    }

    @Test
    void licenseKey_nullRemovesTheProperty() {
        FroalaEditor editor = attachedEditor();
        editor.setLicenseKey("test-key");

        editor.setLicenseKey(null);

        assertNull(editor.getLicenseKey());
    }

    @Test
    void valueChangeMode_roundTripsAndDefaults() {
        FroalaEditor editor = attachedEditor();

        editor.setValueChangeMode(ValueChangeMode.INTERVAL);
        assertEquals(ValueChangeMode.INTERVAL, editor.getValueChangeMode());
        assertEquals("interval", editor.getElement().getProperty("valueChangeMode"));

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
        ProbeEditor editor = attachedProbeEditor();
        editor.setValue("<p>A</p>");
        editor.simulateClientEdit("<p>B</p>");
        drainPendingJavaScript();

        editor.setValue("<p>A</p>");

        // Same value the property already holds, so Flow sends no property update and the browser would ignore one
        // anyway. This is the case that needs the explicit push.
        assertTrue(hasPendingValuePush());
    }

    @Test
    void detach_doesNotQueueAValuePushForTheNextAttach() {
        ProbeEditor editor = attachedProbeEditor();
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
        // AbstractSinglePropertyField the right base (API-1).
        editor.setValue("");
        assertFalse(binder.writeBeanIfValid(note));
        assertEquals("<p>edited in the editor</p>", note.getBody());
    }

    private FroalaEditor attachedEditor() {
        FroalaEditor editor = new FroalaEditor();
        layout.add(editor);

        return editor;
    }

    private ProbeEditor attachedProbeEditor() {
        ProbeEditor editor = new ProbeEditor();
        layout.add(editor);

        return editor;
    }

    private void drainPendingJavaScript() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();
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
        // The three overloads exist so that anything FroalaOptions does not type yet is still reachable. They have to
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
        editor.addCommand(first, event -> seen.add("first " + event.getCommand().name() + " " + event.isFromClient()));
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
    void addCommand_sendsTheUiLocaleAsItIsNow() {
        FroalaEditor editor = attachedEditor();
        UI.getCurrent().setLocale(Locale.forLanguageTag("fi"));

        editor.addCommand(new FroalaCommand("first", "First", VaadinIcon.STAR.create()), event -> {
        });

        assertEquals("[\"fi\"]", localeLanguagesOn(editor));
    }

    /** Fires the event the client sends when one of the editor's commands was triggered in the browser. */
    private void fireCommand(FroalaEditor editor, String name) {
        JsonObject data = Json.createObject();
        data.put("event.detail.name", name);
        ElementUtilsKt._fireDomEvent(editor.getElement(), new DomEvent(editor.getElement(), "_command", data));
    }

    /** The commands as they sit on the element, which is what the client registers with Froala. */
    private String commandsOn(FroalaEditor editor) {
        return ((elemental.json.JsonArray) editor.getElement().getPropertyRaw("commands")).toJson();
    }

    /** The options as they sit on the element, which is what the client will read them from. */
    private String optionsOn(FroalaEditor editor) {
        return ((elemental.json.JsonObject) editor.getElement().getPropertyRaw("options")).toJson();
    }

    /** The locale's language file names as they sit on the element, best first. */
    private String localeLanguagesOn(FroalaEditor editor) {
        return ((elemental.json.JsonArray) editor.getElement().getPropertyRaw("localeLanguages")).toJson();
    }

    /** The invocations only exist once the before-client-response tasks have run, so run them first. */
    private boolean hasPendingValuePush() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();

        return UI.getCurrent().getInternals().containsPendingJavascript("this.value = $0");
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

    /**
     * Exposes the client-originated model update the delta listener performs, which no browserless test can trigger.
     */
    private static class ProbeEditor extends FroalaEditor {
        void simulateClientEdit(String value) {
            setModelValue(value, true);
        }
    }
}
