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
package com.vaadin.componentfactory.froala;

import java.util.Set;
import java.util.regex.Pattern;

import elemental.json.JsonArray;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the router-ignore patterns and their registration. The browser reads the same regular expressions, which this
 * test runs through Java's regex engine, because both read an escaped non-alphanumeric character as literal.
 */
class FroalaViewerTest {

    @Test
    void viewer_carriesFroalasViewClassAndTheVaadinTheme() {
        assertEquals(Set.of("fr-view", "vaadin-theme"), new FroalaViewer().getClassNames());
    }

    @Test
    void content_reachesTheElementUnchanged() {
        FroalaViewer viewer = new FroalaViewer();

        viewer.setContent("<p>shown <b>as is</b></p>");

        assertEquals("<p>shown <b>as is</b></p>", viewer.getElement().getProperty("innerHTML"));
        assertEquals("<p>shown <b>as is</b></p>", viewer.getContent());
    }

    @Test
    void routerIgnorePaths_replaceTheEarlierOnesAndNoneClearsThem() {
        FroalaViewer viewer = new FroalaViewer();

        viewer.setRouterIgnorePaths("/a");
        viewer.setRouterIgnorePaths("/b");
        assertEquals("[\"" + FroalaViewer.toRegex("/b").replace("\\", "\\\\") + "\"]",
                ((JsonArray) viewer.getElement().getPropertyRaw("vcfRouterIgnorePatterns")).toJson());

        viewer.setRouterIgnorePaths();
        assertFalse(viewer.getElement().hasProperty("vcfRouterIgnorePatterns"));
    }

    @Test
    void pathWithoutWildcard_coversEverythingBelowItButNotItsNeighbours() {
        String regex = FroalaViewer.toRegex("/froala-upload");

        assertTrue(matches(regex, "/froala-upload/42"));
        assertTrue(matches(regex, "/froala-upload/a/b"));
        assertFalse(matches(regex, "/froala-uploads/42"));
        assertFalse(matches(regex, "/other/froala-upload/42"));
    }

    @Test
    void pathWithWildcard_isTakenAsItIs() {
        String regex = FroalaViewer.toRegex("/reports/*.pdf");

        assertTrue(matches(regex, "/reports/2026/q3.pdf"));
        assertFalse(matches(regex, "/reports/q3.pdfx"));
        assertFalse(matches(regex, "/reports/q3xpdf"));
    }

    @Test
    void charactersWithAMeaningInARegex_areTakenLiterally() {
        String regex = FroalaViewer.toRegex("/a.b+c");

        assertTrue(matches(regex, "/a.b+c/1"));
        assertFalse(matches(regex, "/axbbc/1"));
    }

    @Test
    void pathWithoutLeadingOrWithTrailingSlash_meansTheSame() {
        assertTrue(matches(FroalaViewer.toRegex("froala-upload"), "/froala-upload/42"));
        assertTrue(matches(FroalaViewer.toRegex("/froala-upload/"), "/froala-upload/42"));
    }

    @Test
    void blankPath_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> FroalaViewer.toRegex(" "));
    }

    @Test
    void invalidPath_leavesTheEarlierOnesInPlace() {
        FroalaViewer viewer = new FroalaViewer();
        viewer.setRouterIgnorePaths("/a");

        assertThrows(IllegalArgumentException.class, () -> viewer.setRouterIgnorePaths("/b", " "));

        assertTrue(viewer.getElement().hasProperty("vcfRouterIgnorePatterns"));
    }

    @Test
    void characterOutsideAscii_matchesTheBrowsersPercentEncodedPath() {
        // URL.pathname, which the client matches against, holds "ä" as %C3%A4
        String regex = FroalaViewer.toRegex("/dä");

        assertTrue(matches(regex, "/d%C3%A4/1"));
        assertFalse(matches(regex, "/dä/1"));
    }

    @Test
    void removingAnEarlierRegistration_keepsThePatternsOfALaterOne() {
        FroalaViewer viewer = new FroalaViewer();
        Registration earlier = FroalaViewer.applyRouterIgnore(viewer, "/a");
        Registration later = FroalaViewer.applyRouterIgnore(viewer, "/b");

        earlier.remove();
        assertTrue(viewer.getElement().hasProperty("vcfRouterIgnorePatterns"));

        later.remove();
        assertFalse(viewer.getElement().hasProperty("vcfRouterIgnorePatterns"));
    }

    private static boolean matches(String regex, String path) {
        return Pattern.compile(regex).matcher(path).find();
    }
}
