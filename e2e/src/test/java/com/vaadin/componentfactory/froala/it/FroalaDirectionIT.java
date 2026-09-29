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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaDirectionTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The host carries the direction the editor was built with, so the label, helper text and error message sit on the same
 * side as the text.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaDirectionIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaDirectionTestView.ROUTE;
    }

    @Test
    void directionOption_reachesTheHost() {
        assertThat(page.locator("#rtl")).hasAttribute("dir", "rtl");
    }

    @Test
    void rtlLanguageFile_reachesTheHost() {
        assertThat(page.locator("#arabic")).hasAttribute("dir", "rtl");
    }

    @Test
    void directionOption_winsOverTheApplicationsOwnDir() {
        assertThat(page.locator("#own-dir-overridden")).hasAttribute("dir", "rtl");
    }

    @Test
    void applicationsOwnDir_staysWhileTheOptionsSayNothing() {
        page.locator("#own-dir .fr-element").waitFor();
        assertThat(page.locator("#own-dir")).hasAttribute("dir", "rtl");

        page.locator("#own-dir .fr-element").evaluate("el => el.classList.add('before-rebuild')");
        switchToOtherOptions();
        // the old editable area is gone, so the editor was built again
        assertThat(page.locator("#own-dir .before-rebuild")).hasCount(0);
        page.locator("#own-dir .fr-element").waitFor();
        assertThat(page.locator("#own-dir")).hasAttribute("dir", "rtl");
    }

    @Test
    void autoAfterADirection_givesBackTheDirTheHostHadBefore() {
        assertThat(page.locator("#rtl")).hasAttribute("dir", "rtl");
        assertThat(page.locator("#own-dir-overridden")).hasAttribute("dir", "rtl");

        switchToOtherOptions();

        // the page has no dir, so the host without an own dir is left with none
        assertThat(page.locator("#rtl")).not().hasAttribute("dir", Pattern.compile(".*"));
        assertThat(page.locator("#own-dir-overridden")).hasAttribute("dir", "ltr");
    }

    @Test
    void switchingToLtrOptions_givesLtr() {
        assertThat(page.locator("#arabic")).hasAttribute("dir", "rtl");

        switchToOtherOptions();

        assertThat(page.locator("#arabic")).hasAttribute("dir", "ltr");
    }

    private void switchToOtherOptions() {
        page.locator("#other-options").click();
        assertThat(page.locator("#other-options")).isDisabled();
    }
}
