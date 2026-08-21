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
package com.vaadin.componentfactory.froala.ui;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.junit.jupiter.api.Test;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaEditor.DeltaMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for the server half of the delta channel. Plain JUnit — no Vaadin, no browser, since
 * {@link FroalaEditor#applyDelta(String, String)} is a pure function.
 *
 * <p>
 * The mismatch case is the one that matters: diff-match-patch reports a failed patch through a flag array rather than
 * an exception and hands back the unpatched string, so without the check the value silently loses the edit.
 */
class FroalaEditorDeltaTest {

    private static final DiffMatchPatch DIFF_MATCH_PATCH = new DiffMatchPatch();

    @Test
    void applyDelta_matchingBase_appliesTheEdit() {
        String delta = deltaBetween("<p>hello world</p>", "<p>hello brave world</p>");

        assertEquals("<p>hello brave world</p>", FroalaEditor.applyDelta("<p>hello world</p>", delta));
    }

    @Test
    void applyDelta_emptyBase_appliesTheEdit() {
        String delta = deltaBetween("", "<p>first words</p>");

        assertEquals("<p>first words</p>", FroalaEditor.applyDelta("", delta));
    }

    @Test
    void applyDelta_driftedBase_throwsInsteadOfLosingTheEdit() {
        String delta = deltaBetween("<p>hello world</p>", "<p>hello brave world</p>");
        String unrelatedBase = "<p>something entirely different</p>";

        DeltaMismatchException exception = assertThrows(DeltaMismatchException.class,
                () -> FroalaEditor.applyDelta(unrelatedBase, delta));

        // the message names which patch failed, so a server log tells us how far the drift goes
        assertEquals("Patch 1 of 1 did not apply to the current value", exception.getMessage());
    }

    private String deltaBetween(String oldValue, String newValue) {
        return DIFF_MATCH_PATCH.patchToText(DIFF_MATCH_PATCH.patchMake(oldValue, newValue));
    }
}
