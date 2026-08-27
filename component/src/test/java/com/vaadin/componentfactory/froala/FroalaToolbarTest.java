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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain JUnit, no Vaadin: a toolbar's whole job is the JSON Froala reads it as, so the JSON is what is asserted. That
 * Froala then draws the buttons is Froala's business — see the project's testing rules.
 */
class FroalaToolbarTest {

    @Test
    void flatToolbar_isAnArrayInTheOrderGiven() {
        // The array form is what makes Froala show every button: it switches its own overflow panel off.
        assertEquals("{\"toolbarButtons\":[\"bold\",\"italic\",\"|\",\"undo\"]}", FroalaOptions.defaults()
                .withToolbarButtons(FroalaToolbar.of("bold", "italic", "|", "undo")).toString());
    }

    @Test
    void groupedToolbar_isAnObjectKeyedByGroupName() {
        FroalaToolbar toolbar = FroalaToolbar.ofGroups(
                FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold", "italic").withButtonsVisible(1),
                FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, "undo", "redo")
                        .withAlign(FroalaToolbarAlign.RIGHT));

        assertEquals(
                "{\"toolbarButtons\":{\"moreText\":{\"buttons\":[\"bold\",\"italic\"],\"buttonsVisible\":1},"
                        + "\"moreMisc\":{\"buttons\":[\"undo\",\"redo\"],\"align\":\"right\"}}}",
                FroalaOptions.defaults().withToolbarButtons(toolbar).toString());
    }

    @Test
    void group_writesOnlyWhatWasSet() {
        // align and buttonsVisible have Froala defaults of their own (left, 3). Emitting them unasked would override
        // a default the caller never touched.
        assertEquals("{\"toolbarButtons\":{\"myGroup\":{\"buttons\":[\"bold\"]}}}", FroalaOptions.defaults()
                .withToolbarButtons(FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold"))).toString());
    }

    @Test
    void twoGroupsOfOneName_areRejected() {
        // Froala keys its groups by name, so the second would replace the first and its buttons would vanish without
        // a word.
        assertThrows(IllegalArgumentException.class,
                () -> FroalaToolbar.ofGroups(FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold"),
                        FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "italic")));
    }

    @Test
    void everyBreakpoint_usesFroalasOwnOptionName() {
        FroalaToolbar toolbar = FroalaToolbar.of("bold");

        assertEquals("{\"toolbarButtonsMD\":[\"bold\"]}",
                FroalaOptions.defaults().withToolbarButtonsMd(toolbar).toString());
        assertEquals("{\"toolbarButtonsSM\":[\"bold\"]}",
                FroalaOptions.defaults().withToolbarButtonsSm(toolbar).toString());
        assertEquals("{\"toolbarButtonsXS\":[\"bold\"]}",
                FroalaOptions.defaults().withToolbarButtonsXs(toolbar).toString());
    }

    @Test
    void null_removesTheToolbarAgain() {
        assertEquals("{}", FroalaOptions.defaults().withToolbarButtons(FroalaToolbar.of("bold"))
                .withToolbarButtons(null).toString());
    }

    @Test
    void with_leavesTheOriginalGroupAlone() {
        FroalaToolbarGroup base = FroalaToolbarGroup.named("myGroup", "bold");
        FroalaToolbarGroup derived = base.withAlign(FroalaToolbarAlign.RIGHT).withButtonsVisible(1);

        assertEquals("myGroup: {\"buttons\":[\"bold\"]}", base.toString());
        assertEquals("myGroup: {\"buttons\":[\"bold\"],\"align\":\"right\",\"buttonsVisible\":1}", derived.toString());
    }

    @Test
    void noButtonsVisible_isWrittenRatherThanTreatedAsUnset() {
        // The one value the API documents as meaningful and dangerous: zero moves every button of the group into the
        // overflow panel. Dropping it as "nothing set" would silently give Froala's three instead.
        assertEquals("{\"toolbarButtons\":{\"moreText\":{\"buttons\":[\"bold\"],\"buttonsVisible\":0}}}",
                FroalaOptions.defaults()
                        .withToolbarButtons(FroalaToolbar.ofGroups(
                                FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold").withButtonsVisible(0)))
                        .toString());
    }

    @Test
    void nulls_areRejectedWhereTheyWouldOnlyShowUpLater() {
        // Not the same as a null option, which means "leave Froala's default". A null name or button would travel to
        // the browser as JSON null and be dropped there, far from the line that caused it.
        assertThrows(NullPointerException.class, () -> FroalaToolbarGroup.named(null, "bold"));
        assertThrows(NullPointerException.class, () -> FroalaToolbarGroup.named("myGroup", "bold", null));
        assertThrows(NullPointerException.class, () -> FroalaToolbar.of("bold", null));
        assertThrows(NullPointerException.class, () -> FroalaToolbar.ofGroups((FroalaToolbarGroup) null));
    }

    @Test
    void equality_isByContentNotByIdentity() {
        assertEquals(FroalaToolbar.of("bold"), FroalaToolbar.of("bold"));
        assertEquals(FroalaToolbar.of("bold").hashCode(), FroalaToolbar.of("bold").hashCode());
        assertNotEquals(FroalaToolbar.of("bold"), FroalaToolbar.of("italic"));

        assertEquals(FroalaToolbarGroup.named("myGroup", "bold").withButtonsVisible(1),
                FroalaToolbarGroup.named("myGroup", "bold").withButtonsVisible(1));
        assertEquals(FroalaToolbarGroup.named("myGroup", "bold").hashCode(),
                FroalaToolbarGroup.named("myGroup", "bold").hashCode());
        assertNotEquals(FroalaToolbarGroup.named("myGroup", "bold"),
                FroalaToolbarGroup.named("myGroup", "bold").withAlign(FroalaToolbarAlign.RIGHT));

        assertEquals(FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold")),
                FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold")));
        assertEquals(FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold")).hashCode(),
                FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold")).hashCode());

        // A one-group toolbar and a one-button toolbar are different shapes to Froala, so they are different here too.
        assertNotEquals(FroalaToolbar.of("bold"), FroalaToolbar.ofGroups(FroalaToolbarGroup.named("group1", "bold")));
    }
}
