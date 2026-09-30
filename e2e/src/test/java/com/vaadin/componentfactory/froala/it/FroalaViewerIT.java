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

import com.vaadin.componentfactory.froala.it.views.FroalaDirectionTestView;
import com.vaadin.componentfactory.froala.it.views.FroalaThemeTestView;
import com.vaadin.componentfactory.froala.it.views.FroalaViewerTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers a viewer on a view without an editor, which has to load the add-on's stylesheets on its own, and the links in
 * its content.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaViewerIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaViewerTestView.ROUTE;
    }

    @Test
    void viewerWithoutEditor_getsItsOwnRule() {
        assertThat(page.locator("#viewer")).hasCSS("overflow", "auto");
    }

    @Test
    void linkMatchingARouterIgnorePath_opensWithAPageLoad() {
        clickAfterMarking("#ignored", FroalaThemeTestView.ROUTE);

        assertNull(page.evaluate("window.beforeClick"));
    }

    @Test
    void linkMatchingNoRouterIgnorePath_staysWithTheRouter() {
        clickAfterMarking("#routed", FroalaDirectionTestView.ROUTE);

        assertEquals(true, page.evaluate("window.beforeClick"));
    }

    /**
     * The router changes the URL as well, so only a marker that a page load wipes out tells the two apart.
     */
    private void clickAfterMarking(String link, String route) {
        page.evaluate("window.beforeClick = true");

        page.locator(link).click();
        page.waitForURL("**/" + route);
    }
}
