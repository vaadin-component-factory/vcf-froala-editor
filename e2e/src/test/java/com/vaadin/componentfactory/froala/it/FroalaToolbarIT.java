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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.FroalaToolbar;
import com.vaadin.componentfactory.froala.it.views.FroalaToolbarTestView;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** End-to-end test for the toolbar presets {@link FroalaToolbar#froalaDefault()} and {@link FroalaToolbar#basics()}. */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaToolbarIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaToolbarTestView.ROUTE;
    }

    @Test
    void froalaDefault_isFroalasOwnDefaultToolbar() {
        // Our copy of Froala's list. A Froala update that changes its default toolbar fails here.
        page.waitForFunction("() => window.FroalaEditorClass !== undefined");
        String froalas = (String) page.evaluate("() => JSON.stringify(window.FroalaEditorClass.TOOLBAR_BUTTONS)");
        String ours = (String) page.evaluate("json => JSON.stringify(JSON.parse(json))",
                FroalaToolbar.froalaDefault().toString());

        assertEquals(froalas, ours);
    }

    @Test
    void basics_drawsEveryButtonWithTheBasicPlugins() {
        page.locator("#basics .fr-element[contenteditable='true']").waitFor();

        // A button whose plugin is not enabled, or whose name Froala does not know, is dropped silently. Froala adds
        // helper buttons of its own, such as formatOLOptions, so only what is missing counts. Buttons in an overflow
        // panel are in the page as well, only hidden.
        JsonObject groups = Json.parse(FroalaToolbar.basics().toString());
        Set<String> expected = Arrays.stream(groups.keys()).map(name -> groups.getObject(name).getArray("buttons"))
                .flatMap(buttons -> IntStream.range(0, buttons.length()).mapToObj(buttons::getString))
                .collect(Collectors.toSet());
        @SuppressWarnings("unchecked")
        List<String> drawn = (List<String>) page.evaluate(
                "() => [...document.querySelectorAll('#basics .fr-toolbar .fr-command[data-cmd]')].map(b => b.dataset.cmd)");

        Set<String> missing = expected.stream().filter(button -> !drawn.contains(button)).collect(Collectors.toSet());
        assertEquals(Set.of(), missing, "buttons of the basic toolbar that Froala did not draw");
    }
}
