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
import com.microsoft.playwright.options.BoundingBox;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaQuickInsertTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the overflow of the element's own shadow template (#6): it has to keep Froala within the field's size without
 * clipping what Froala deliberately places outside its box.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaQuickInsertIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaQuickInsertTestView.ROUTE;
    }

    @Test
    void quickInsert_isNotClippedWhenPlacedLeftOfTheEditor() {
        page.locator("#empty-editor .fr-element").click();
        Locator button = page.locator("#empty-editor .fr-quick-insert.fr-visible > a");
        assertThat(button).isVisible();

        // isVisible() ignores clipping by an ancestor's overflow, so ask the browser what is really at that spot
        BoundingBox box = button.boundingBox();
        boolean hit = (boolean) page.evaluate(
                "([x, y]) => document.elementFromPoint(x, y)?.closest('.fr-quick-insert') != null",
                List.of(box.x + box.width / 2, box.y + box.height / 2));

        assertTrue(hit, "the quick-insert button is covered or clipped");
    }

    @Test
    void overfullEditor_keepsItsHeight() {
        BoundingBox field = page.locator("#overfull-editor").boundingBox();
        BoundingBox froala = page.locator("#overfull-editor .fr-box").boundingBox();

        assertTrue(froala.y + froala.height <= field.y + field.height,
                "Froala ends at " + (froala.y + froala.height) + ", below the field's " + (field.y + field.height));
    }
}
