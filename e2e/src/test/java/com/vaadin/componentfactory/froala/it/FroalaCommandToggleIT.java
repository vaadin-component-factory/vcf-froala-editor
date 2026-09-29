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

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaCommandToggleTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A toggle command's button shows the state the server holds for that editor. */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaCommandToggleIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaCommandToggleTestView.ROUTE;
    }

    @Test
    void click_pressesTheButtonWithoutARebuildAndReleasesItAgain() {
        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("first=true");
        page.evaluate("window.button = document.querySelector(\"#first .fr-toolbar button[data-cmd='review']\")");

        assertPressed("first", true);
        // the same button, so the editor was not built again
        assertTrue((Boolean) page.evaluate(
                "window.button === document.querySelector(\"#first .fr-toolbar button[data-cmd='review']\")"));

        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("first=true first=false");
        assertPressed("first", false);
    }

    @Test
    void pressedButton_staysPressedOnARefreshWithAndWithoutFocus() {
        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("first=true");

        page.locator("#first .fr-element p").click();
        assertTrue(pressedAfterRefresh());
        // Without focus Froala clears a button on a refresh, unless the command asks to be refreshed anyway
        page.locator("#second .fr-element p").click();
        assertThat(page.locator("#second .fr-element")).isFocused();
        assertTrue(pressedAfterRefresh());
    }

    /**
     * Runs the refresh Froala runs on a selection change and reads the first editor's button once it is done. Froala
     * refreshes in a timeout, so an assertion right after the click would still see the state from before.
     */
    private boolean pressedAfterRefresh() {
        return (Boolean) page.evaluate("async () => {" + " const editor = document.getElementById('first');"
                + " editor.editor.button.bulkRefresh();" + " await new Promise((resolve) => setTimeout(resolve, 50));"
                + " return editor.querySelector(\".fr-toolbar button[data-cmd='review']\").classList.contains('fr-active');"
                + " }");
    }

    @Test
    void twoEditors_showTheirOwnState() {
        toolbarButton("second").click();
        assertThat(page.locator("#log")).hasText("second=true");

        assertPressed("second", true);
        assertPressed("first", false);
    }

    @Test
    void state_survivesARebuildAndAReattach() {
        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("first=true");

        page.locator("#rebuild").click();
        assertThat(page.locator("#rebuild")).isDisabled();
        // the new toolbar has the button first
        page.locator("#first .fr-toolbar button[data-cmd='review']:first-child").waitFor();
        assertPressed("first", true);

        page.locator("#reattach").click();
        assertThat(page.locator("#reattach")).isDisabled();
        assertPressed("first", true);
    }

    @Test
    void popupButton_showsTheState() {
        toolbarButton("first").click();
        assertThat(page.locator("#log")).hasText("first=true");

        page.locator("#first .fr-element a").click();
        Locator popupButton = page.locator(".fr-popup.fr-active button[data-cmd='review']");
        assertThat(popupButton).hasClass(Pattern.compile("\\bfr-active\\b"));
        assertThat(popupButton).hasAttribute("aria-pressed", "true");
    }

    private void assertPressed(String editor, boolean pressed) {
        Locator button = toolbarButton(editor);
        if (pressed) {
            assertThat(button).hasClass(Pattern.compile("\\bfr-active\\b"));
        } else {
            assertThat(button).not().hasClass(Pattern.compile("\\bfr-active\\b"));
        }
        assertThat(button).hasAttribute("aria-pressed", String.valueOf(pressed));
    }

    private Locator toolbarButton(String editor) {
        return page.locator("#" + editor + " .fr-toolbar button[data-cmd='review']");
    }
}
