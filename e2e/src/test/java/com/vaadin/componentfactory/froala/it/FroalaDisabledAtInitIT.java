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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaDisabledAtInitTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Covers the connector's init-time re-apply path: state the server set while Froala was still building.
 *
 * <p>
 * Separate from {@code FroalaEditorIT} because it needs a view whose editors are disabled and read-only before they are
 * ever attached. Toggling the state after load, as that suite does, runs through Lit's ordinary {@code updated()} and
 * says nothing about this path — Froala's modules cannot be touched before its own {@code initialized} event, so the
 * pending state has to be re-applied from that handler.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaDisabledAtInitIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaDisabledAtInitTestView.ROUTE;
    }

    @Test
    void disabledBeforeAttach_editorComesUpNotEditable() {
        assertThat(page.locator("#disabled-editor .fr-element")).isVisible();
        assertThat(page.locator("#disabled-editor .fr-element[contenteditable='true']")).hasCount(0);
    }

    @Test
    void readOnlyBeforeAttach_editorComesUpNotEditable() {
        assertThat(page.locator("#readonly-editor .fr-element")).isVisible();
        assertThat(page.locator("#readonly-editor .fr-element[contenteditable='true']")).hasCount(0);
    }
}
