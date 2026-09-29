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

import com.vaadin.componentfactory.froala.it.views.FroalaLocaleTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * An editor without a language in its options is built in the UI's language. Froala reads its language only when the
 * editor is built, so a translated tooltip proves the language file was there in time.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaLocaleIT extends SpringPlaywrightIT {

    private static final String FIRST_LOCALE = "de";

    @Override
    protected String getView() {
        return FroalaLocaleTestView.ROUTE + "/" + FIRST_LOCALE;
    }

    @Test
    void germanLocale_buildsAGermanEditor() {
        assertBoldTitle("from-locale", "Fett");
    }

    @Test
    void arabicLocale_buildsAnArabicRightToLeftEditor() {
        open("ar");

        assertBoldTitle("from-locale", "غامق");
        assertThat(page.locator("#from-locale")).hasAttribute("dir", "rtl");
    }

    @Test
    void chineseLocale_buildsASimplifiedChineseEditor() {
        open("zh-CN");

        assertBoldTitle("from-locale", "粗体");
    }

    @Test
    void countryWithoutAFile_fallsBackToTheLanguage() {
        open("de-AT");

        assertBoldTitle("from-locale", "Fett");
    }

    @Test
    void englishOrUnknownLocale_leavesTheEditorInEnglish() {
        // English is also what an editor without the feature shows. This guards the fallback, that a locale without a
        // file neither breaks the build nor picks some other file.
        open("en");
        assertBoldTitle("from-locale", "Bold");

        open("xx");
        assertBoldTitle("from-locale", "Bold");
    }

    @Test
    void explicitLanguage_winsOverTheLocale() {
        assertBoldTitle("explicit", "Gras");
    }

    @Test
    void explicitEnglish_keepsTheDirectionOptionUnderAnotherLocale() {
        assertBoldTitle("english-rtl", "Bold");
        assertThat(page.locator("#english-rtl")).hasAttribute("dir", "rtl");
    }

    private void open(String languageTag) {
        page.navigate(page.url().replaceAll("/[^/]+$", "/" + languageTag));
    }

    private void assertBoldTitle(String id, String title) {
        // The title ends in the keyboard shortcut, such as "Fett (Ctrl+B)". Playwright runs the pattern as a JavaScript
        // regular expression, whose \b knows no Chinese or Arabic letters, hence the space.
        assertThat(page.locator("#" + id + " .fr-command[data-cmd='bold']")).hasAttribute("title",
                Pattern.compile("^" + title + " "));
    }
}
