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
package com.vaadin.componentfactory.froala.ui;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import kotlin.jvm.functions.Function0;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.ValueChangeMode;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.SpringServlet;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browserless UI-unit test using Karibu Testing (license-free) — runs the view in the JVM, no browser.
 *
 * <p>
 * Karibu executes no JavaScript, so everything here is server-side state and element properties: the value round-trip,
 * the license key and the value change mode as they are handed to the client. Whether Froala then honours them is an
 * e2e question, see {@code FroalaEditorIT}.
 */
@SpringBootTest
class FroalaEditorKaribuTest {

    private static final Routes routes = new Routes().autoDiscoverViews("com.vaadin.componentfactory.froala");

    @Autowired
    ApplicationContext ctx;

    @BeforeEach
    void setup() {
        Function0<UI> uiFactory = UI::new;
        SpringServlet servlet = new MockSpringServlet(routes, ctx, uiFactory);

        MockVaadin.setup(uiFactory, servlet);
        UI.getCurrent().navigate(BasicView.class);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void setValue_reachesTheClientProperty() {
        FroalaEditor editor = _get(FroalaEditor.class);

        editor.setValue("<p>typed on the server</p>");

        assertEquals("<p>typed on the server</p>", editor.getValue());
        assertEquals("<p>typed on the server</p>", editor.getElement().getProperty("value"));
    }

    @Test
    void valueChangeListener_firesOnServerSideChange() {
        FroalaEditor editor = _get(FroalaEditor.class);
        String[] seen = new String[1];
        editor.addValueChangeListener(event -> seen[0] = event.getValue());

        editor.setValue("<p>observed</p>");

        assertEquals("<p>observed</p>", seen[0]);
    }

    @Test
    void licenseKey_isSetAsElementProperty() {
        FroalaEditor editor = _get(FroalaEditor.class);

        editor.setLicenseKey("test-key");

        assertEquals("test-key", editor.getLicenseKey());
        assertEquals("test-key", editor.getElement().getProperty("licenseKey"));
    }

    @Test
    void licenseKey_nullRemovesTheProperty() {
        FroalaEditor editor = _get(FroalaEditor.class);
        editor.setLicenseKey("test-key");

        editor.setLicenseKey(null);

        assertNull(editor.getLicenseKey());
    }

    @Test
    void valueChangeMode_roundTripsAndDefaults() {
        FroalaEditor editor = _get(FroalaEditor.class);

        editor.setValueChangeMode(ValueChangeMode.TIMEOUT);
        assertEquals(ValueChangeMode.TIMEOUT, editor.getValueChangeMode());
        assertEquals("timeout", editor.getElement().getProperty("valueChangeMode"));

        editor.setValueChangeMode(null);
        assertEquals(FroalaEditor.DEFAULT_VALUE_CHANGE_MODE, editor.getValueChangeMode());
    }

    @Test
    void valueChangeTimeout_rejectsAnythingButPositiveValues() {
        FroalaEditor editor = _get(FroalaEditor.class);

        assertThrows(IllegalArgumentException.class, () -> editor.setValueChangeTimeout(-1));

        // zero used to pass here and then throw in the client's own setter, so the failure surfaced in the browser
        // instead of at the call site
        assertThrows(IllegalArgumentException.class, () -> editor.setValueChangeTimeout(0));
    }

    @Test
    void setValueRepeatingTheLastServerValue_queuesAnExplicitClientPush() {
        VerticalLayout layout = new VerticalLayout();
        UI.getCurrent().add(layout);

        ProbeEditor editor = new ProbeEditor();
        layout.add(editor);
        editor.setValue("<p>A</p>");
        editor.simulateClientEdit("<p>B</p>");
        drainPendingJavaScript();

        editor.setValue("<p>A</p>");

        // Same value the property already holds, so Flow sends no property update and the browser would ignore one
        // anyway -- this is the case that needs the explicit push (finding 6).
        assertTrue(hasPendingValuePush());
    }

    @Test
    void detach_doesNotQueueAValuePushForTheNextAttach() {
        VerticalLayout layout = new VerticalLayout();
        UI.getCurrent().add(layout);

        ProbeEditor editor = new ProbeEditor();
        layout.add(editor);
        editor.setValue("<p>A</p>");
        drainPendingJavaScript();

        // The detach listener calls setPresentationValue with the value the property already holds, which is exactly
        // the shape the explicit push reacts to. A push queued here is not dropped: Flow defers it to the next attach,
        // where it would overwrite whatever the server set in between. isAttached() is no guard against it -- it
        // still answers true inside a detach listener.
        layout.remove(editor);
        editor.setValue("<p>B</p>");
        layout.add(editor);

        assertFalse(hasPendingValuePush());
    }

    private void drainPendingJavaScript() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations();
    }

    /** The invocations only exist once the before-client-response tasks have run, so run them first. */
    private boolean hasPendingValuePush() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();

        return UI.getCurrent().getInternals().containsPendingJavascript("this.value = $0");
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
