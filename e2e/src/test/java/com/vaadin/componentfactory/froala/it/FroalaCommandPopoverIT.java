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
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaCommandPopoverTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A popover handed to an own command opens at the command's toolbar button, and the editor never closes it. */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaCommandPopoverIT extends SpringPlaywrightIT {

    private static final String TARGETS_THE_BUTTON = "id => document.getElementById(id).target"
            + " === document.querySelector('#first .fr-toolbar button[data-cmd=\"info\"]')";

    @Override
    protected String getView() {
        return FroalaCommandPopoverTestView.ROUTE;
    }

    @Test
    void click_opensThePopoverAtTheButtonAndClosesItAgain() {
        toolbarButton().click();

        assertThat(content("popover")).isVisible();
        assertTrue((Boolean) page.evaluate(TARGETS_THE_BUTTON, "popover"));

        toolbarButton().click();
        assertThat(content("popover")).isHidden();
    }

    @Test
    void shortcut_opensThePopover() {
        page.locator("#first .fr-element").click();
        page.keyboard().press("Control+Shift+K");

        assertThat(content("popover")).isVisible();
    }

    @Test
    void shortcut_opensNothingWithoutAButton() {
        page.locator("#no-button .fr-element").click();
        page.keyboard().press("Control+Shift+K");

        // the shortcut handler opens the popover synchronously, so it would be open by now
        assertFalse((Boolean) page.evaluate("document.getElementById('no-button-popover').opened"));
        assertNull(page.evaluate("document.getElementById('no-button-popover').target"));
    }

    @Test
    void openPopover_staysOpenAcrossARebuildAndFollowsTheNewButton() {
        toolbarButton().click();
        assertThat(content("popover")).isVisible();

        page.locator("#rebuild").click();
        assertThat(page.locator("#rebuild")).isDisabled();
        // the new toolbar has the button first
        page.locator("#first .fr-toolbar button[data-cmd='info']:first-child").waitFor();

        assertThat(content("popover")).isVisible();
        assertTrue((Boolean) page.evaluate(TARGETS_THE_BUTTON, "popover"));
        // nothing closes it on an outside click or on Escape. The popover covers the first editor's text.
        page.locator("#no-button .fr-element").click();
        page.keyboard().press("Escape");
        assertThat(content("popover")).isVisible();

        toolbarButton().click();
        assertThat(content("popover")).isHidden();
    }

    @Test
    void reattachedEditor_opensThePopoverAtTheButton() {
        toolbarButton().waitFor();
        page.locator("#reattach").click();
        assertThat(page.locator("#reattach")).isDisabled();
        toolbarButton().waitFor();

        toolbarButton().click();

        assertThat(content("popover")).isVisible();
        assertTrue((Boolean) page.evaluate(TARGETS_THE_BUTTON, "popover"));
    }

    @Test
    void removedCommand_takesThePopoverAway() {
        assertThat(toolbarButton()).isVisible();

        page.locator("#remove").click();
        assertThat(page.locator("#remove")).isDisabled();

        assertThat(toolbarButton()).hasCount(0);
        assertThat(page.locator("#popover")).hasCount(0);
    }

    private Locator toolbarButton() {
        return page.locator("#first .fr-toolbar button[data-cmd='info']");
    }

    private Locator content(String popover) {
        return page.getByText("Content of " + popover, new Page.GetByTextOptions().setExact(true));
    }
}
