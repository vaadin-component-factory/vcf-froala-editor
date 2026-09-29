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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.microsoft.playwright.Route;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.componentfactory.froala.it.views.FroalaAllPluginsTestView;
import com.vaadin.componentfactory.froala.it.views.FroalaLoadingTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test for loading Froala's plugin and language files on demand (CFG-16 in #26). The e2e app always runs a
 * production bundle, so this is the build where each file has to end up as a chunk of its own.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaLoadingIT extends SpringPlaywrightIT {

    @Override
    protected String getView() {
        return FroalaLoadingTestView.ROUTE;
    }

    private void waitForEditor(String id) {
        page.locator("#" + id + " .fr-element[contenteditable='true']").waitFor();
    }

    private void open(String route) {
        page.navigate(page.url().replace(FroalaLoadingTestView.ROUTE, route));
    }

    @SuppressWarnings("unchecked")
    private List<String> pluginsEnabled(String id) {
        return (List<String>) page.evaluate("id => document.getElementById(id).editor.opts.pluginsEnabled", id);
    }

    /** Whether Froala built the plugin into the editor. It keeps each plugin instance under the plugin's name. */
    private boolean hasPluginInstance(String id, String plugin) {
        return (boolean) page.evaluate("([id, plugin]) => plugin in document.getElementById(id).editor",
                List.of(id, plugin));
    }

    /** The file names of the scripts the page has downloaded so far. */
    @SuppressWarnings("unchecked")
    private List<String> downloadedScripts() {
        return (List<String>) page.evaluate("""
                () => performance.getEntriesByType('resource')
                    .map(entry => entry.name.split('/').pop().split('?')[0])
                    .filter(file => file.endsWith('.js'))""");
    }

    /**
     * Opens the view with every plugin. Requests that leave the test server are blocked, because some third-party
     * plugins fetch their library or talk to their service. The test is about our loading, not about their services.
     */
    private void openAllPluginsView() {
        String testServer = page.url().replaceAll("^(https?://[^/]+).*", "$1");
        page.route(url -> !url.startsWith(testServer), Route::abort);
        open(FroalaAllPluginsTestView.ROUTE);
    }

    @Test
    void language_isLoadedBeforeTheEditorIsBuilt() {
        waitForEditor("editor");

        // Froala reads its language only when the editor is built. So German tooltips prove the file was there in time.
        assertThat(page.locator("#editor .fr-command[data-cmd='bold']")).hasAttribute("title",
                Pattern.compile("^Fett\\b"));
    }

    @Test
    void restrictedPlugins_downloadOnlyTheirOwnFiles() {
        waitForEditor("editor");

        // A production chunk is named after its source file with a hash appended, such as align.min-BowZ32Ae.js
        List<String> scripts = downloadedScripts();
        List<String> pluginFiles = Arrays.stream(FroalaPlugin.values()).map(FroalaPlugin::getFileName)
                .filter(file -> scripts.stream().anyMatch(script -> script.startsWith(file + ".min-"))).toList();

        assertEquals(List.of("align"), pluginFiles, "plugin files downloaded");
        assertEquals(List.of("align"), pluginsEnabled("editor"));
        assertTrue(hasPluginInstance("editor", "align"));
    }

    @Test
    void optionsChangedWhileLoading_buildTheEditorWithTheNewOnes() {
        // Holds the plugin file back, so the editor is still loading when the other options arrive
        List<Route> heldBack = new ArrayList<>();
        page.route("**/align.min-*.js", heldBack::add);
        open(FroalaLoadingTestView.ROUTE);
        page.waitForCondition(() -> !heldBack.isEmpty());

        page.locator("#other-options").click();
        assertThat(page.locator("#other-options")).isDisabled();
        heldBack.forEach(Route::resume);
        waitForEditor("editor");

        assertEquals(List.of("lists"), pluginsEnabled("editor"));
    }

    @Test
    void focusBeforeTheFilesAreLoaded_reachesTheEditor() {
        waitForEditor("editor");

        page.locator("#add-focused").click();
        assertThat(page.locator("#add-focused")).isDisabled();
        waitForEditor("focused");

        assertThat(page.locator("#focused .fr-element")).isFocused();
    }

    @Test
    void everyFroalaPlugin_isLoadedAndBuilt() {
        openAllPluginsView();
        waitForEditor("all");

        List<String> missing = Arrays.stream(FroalaPlugin.values()).map(FroalaPlugin::getPluginName)
                .filter(plugin -> !hasPluginInstance("all", plugin)).toList();

        assertEquals(List.of(), missing, "plugins Froala did not build");
    }

    @Test
    void defaultPlugins_areTheBasicsWhateverElseThePageLoaded() {
        openAllPluginsView();
        // the first editor has registered the plugins basics() leaves out on the page by now
        waitForEditor("all");
        String table = FroalaPlugin.TABLE.getPluginName();
        assertTrue(pluginsEnabled("all").contains(table), "all() enables what basics() leaves out");

        page.locator("#add-defaults").click();
        assertThat(page.locator("#add-defaults")).isDisabled();
        waitForEditor("defaults");

        Set<String> basics = FroalaPlugin.basics().stream().map(FroalaPlugin::getPluginName)
                .collect(Collectors.toSet());
        assertEquals(basics, Set.copyOf(pluginsEnabled("defaults")));
        assertFalse(hasPluginInstance("defaults", table));
    }
}
