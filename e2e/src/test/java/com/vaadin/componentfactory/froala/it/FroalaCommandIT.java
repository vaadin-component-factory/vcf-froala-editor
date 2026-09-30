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

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaCommandTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An application's own command reaches Froala with its title, icon and shortcut, and a trigger runs the listener of the
 * editor it happened in.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaCommandIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaCommandTestView.ROUTE;
    }

    @Test
    void toolbarButton_showsTheTitleAndTheVaadinIcon() {
        Locator button = toolbarButton("first");

        // Froala adds the shortcut to the title
        assertThat(button).hasAttribute("title", "Stamp & \"seal\" (Ctrl+Shift+K)");
        Locator icon = button.locator("vaadin-icon[icon='vaadin:star']");
        assertThat(icon).isVisible();
        // the iconset has drawn the icon into the element's shadow root
        assertTrue((Boolean) icon.evaluate("el => el.shadowRoot.querySelector('svg').innerHTML.includes('path')"));
    }

    @Test
    void toolbarButton_runsTheListenerOfTheEditorItWasClickedIn() {
        toolbarButton("second").click();
        assertThat(page.locator("#log")).hasText("second");

        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("second first");
    }

    @Test
    void popupButton_runsTheListener() {
        page.locator("#second .fr-element a").click();
        page.locator(".fr-popup.fr-active button[data-cmd='stamp']").click();

        assertThat(page.locator("#log")).hasText("second");
    }

    @Test
    void shortcut_runsTheListenerOfTheEditorItWasPressedIn() {
        page.locator("#second .fr-element").click();
        page.keyboard().press("Control+Shift+K");

        assertThat(page.locator("#log")).hasText("second");
    }

    @Test
    void keyCodeShortcut_runsTheListener_andShowsItsLabel() {
        assertThat(page.locator("#key-code .fr-toolbar button[data-cmd='memo']")).hasAttribute("title",
                "Memo (Ctrl+Shift+F2)");

        page.locator("#key-code .fr-element").click();
        page.keyboard().press("Control+Shift+F2");

        assertThat(page.locator("#log")).hasText("key-code");
    }

    @Test
    void shortcut_worksWithoutAToolbarButton() {
        assertThat(toolbarButton("no-button")).hasCount(0);

        page.locator("#no-button .fr-element").click();
        page.keyboard().press("Control+Shift+K");

        assertThat(page.locator("#log")).hasText("no-button");
    }

    @Test
    void removedCommand_leavesThatEditorOnly() {
        assertThat(toolbarButton("first")).isVisible();

        page.locator("#remove-from-first").click();
        assertThat(page.locator("#remove-from-first")).isDisabled();

        assertThat(page.locator("#first .fr-toolbar button[data-cmd='bold']")).isVisible();
        assertThat(toolbarButton("first")).hasCount(0);
        // the other editor still has it, although Froala keeps commands for the whole page
        toolbarButton("second").click();
        assertThat(page.locator("#log")).hasText("second");

        // and its shortcut no longer reaches the first editor's listener
        page.locator("#first .fr-element").click();
        page.keyboard().press("Control+Shift+K");
        toolbarButton("second").click();
        assertThat(page.locator("#log")).hasText("second second");
    }

    @Test
    void commandAddedAfterAttach_appearsAndWorks() {
        page.locator("#late .fr-toolbar button[data-cmd='bold']").waitFor();
        assertThat(toolbarButton("late")).hasCount(0);

        page.locator("#add-to-late").click();
        assertThat(page.locator("#add-to-late")).isDisabled();

        toolbarButton("late").click();
        assertThat(page.locator("#log")).hasText("late");
    }

    private Locator toolbarButton(String editor) {
        return page.locator("#" + editor + " .fr-toolbar button[data-cmd='stamp']");
    }
}
