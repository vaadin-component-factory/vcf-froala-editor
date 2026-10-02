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

import com.vaadin.componentfactory.froala.it.views.FroalaSignalTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Covers an editor bound to a signal with the {@code bindValue} it inherits from Vaadin's fields, in both directions,
 * and a viewer bound to the same signal with {@code bindContent}.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaSignalIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaSignalTestView.ROUTE;
    }

    private Locator editableArea() {
        return page.locator("#editor .fr-element[contenteditable='true']");
    }

    @Test
    void typedText_reachesTheSignal() {
        editableArea().click();
        editableArea().type("Typed into the editor");

        assertThat(page.locator("#viewer")).containsText("Typed into the editor");
    }

    @Test
    void signalSetOnTheServer_reachesTheEditor() {
        editableArea().waitFor();

        page.locator("#set-signal").click();

        assertThat(editableArea().locator("strong")).hasText("signal");
        assertThat(editableArea()).containsText(FroalaSignalTestView.SERVER_TEXT);
        assertThat(page.locator("#viewer")).containsText(FroalaSignalTestView.SERVER_TEXT);
    }
}
