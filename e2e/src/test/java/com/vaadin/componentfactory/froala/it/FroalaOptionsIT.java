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
package com.vaadin.componentfactory.froala.it;

import java.util.List;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaOptionsTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test for the options channel. The only layer that can answer whether an option reaches Froala at all:
 * Karibu proves the JSON arrives on the element, nothing more, because it runs no JavaScript.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaOptionsIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaOptionsTestView.ROUTE;
    }

    /** The command names Froala actually rendered into the toolbar, which is what an applied option looks like. */
    @SuppressWarnings("unchecked")
    private List<String> toolbarButtons() {
        page.locator("#editor .fr-toolbar .fr-command").first().waitFor();

        return (List<String>) page.locator("#editor .fr-toolbar .fr-command")
                .evaluateAll("buttons => buttons.map(button => button.getAttribute('data-cmd'))");
    }

    private Locator editableArea() {
        return page.locator("#editor .fr-element[contenteditable='true']");
    }

    @Test
    void options_reachFroalaOnTheFirstBuild() {
        // Set before the first attach, so they can only have arrived through the element property the connector reads
        // when it constructs Froala.
        assertEquals(List.of("bold", "italic"), toolbarButtons());
    }

    @Test
    void optionsInFroalasOwnOptionObject() {
        // The toolbar is what the user sees; this is the option itself, in the place Froala keeps it. An option we
        // pass but Froala silently ignores would still show up here, which is the point -- our end is what we own.
        editableArea().waitFor();

        Object placeholder = page.evaluate("() => document.querySelector('#editor').editor.opts.placeholderText");

        assertEquals(FroalaOptionsTestView.PLACEHOLDER, placeholder);
    }

    @Test
    void changedOptions_rebuildTheRunningEditor() {
        assertEquals(List.of("bold", "italic"), toolbarButtons());

        page.locator("#other-options").click();
        // the button disables itself server side, so this is the round trip having landed -- reading the client right
        // after the click would race it
        assertThat(page.locator("#other-options")).isDisabled();

        assertEquals(List.of("undo", "redo", "insertLink"), toolbarButtons());
    }

    @Test
    void clearedOptions_bringFroalasOwnDefaultsBack() {
        page.locator("#clear-options").click();
        assertThat(page.locator("#clear-options")).isDisabled();

        // Froala's default toolbar is its eight groups, so anything beyond the two configured buttons proves the
        // option was really taken away rather than merged over
        assertTrue(toolbarButtons().size() > 2, "expected Froala's own default toolbar, got " + toolbarButtons());
        assertTrue(toolbarButtons().contains("insertImage"));
    }

    @Test
    void rebuild_keepsWhatTheUserTyped() {
        // A rebuild throws the editor away, so the value has to be carried across. Caret, selection and undo history
        // are not -- that is the documented trade, and the same one a detach and re-attach makes.
        editableArea().click();
        editableArea().type("typed before the rebuild");
        assertThat(page.locator("vcf-froala-viewer")).containsText("typed before the rebuild");

        page.locator("#other-options").click();
        assertThat(page.locator("#other-options")).isDisabled();

        assertEquals(List.of("undo", "redo", "insertLink"), toolbarButtons());
        assertThat(editableArea()).containsText("typed before the rebuild");
        assertThat(editableArea()).containsText(FroalaOptionsTestView.INITIAL_VALUE.replaceAll("<[^>]+>", ""));
    }
}
