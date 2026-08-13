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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * End-to-end test driving a real browser. Named *IT so failsafe runs it in the integration-test/verify phase of the
 * `production` profile, against the optimized frontend bundle.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GreetingViewIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return "greeting";
    }

    @Test
    void typeName_clickGreet_showsGreeting() {
        page.locator("vaadin-text-field input").fill("World");
        page.locator("vaadin-button").click();

        assertThat(page.locator("#greeting-result")).hasText("Hello, World!");
    }
}
