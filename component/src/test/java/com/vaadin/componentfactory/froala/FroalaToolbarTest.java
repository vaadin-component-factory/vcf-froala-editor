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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain JUnit, no Vaadin. A toolbar's whole job is the JSON Froala reads it as, so the JSON is what is asserted. That
 * Froala then draws the buttons is Froala's business. See the project's testing rules.
 */
class FroalaToolbarTest {

    @Test
    void flatToolbar_isAnArrayInTheOrderGiven() {
        // The array form is what makes Froala show every button, because it switches its own overflow panel off.
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
    void freelyNamedGroup_isRejectedWhenItWouldHideButtonsNothingCanShow() {
        // Froala draws the button that opens the overflow panel from a command registered under the group's name, and
        // it only has one for its own four. Any other name leaves the hidden buttons in the DOM with nothing to reach
        // them, which is worse than saying so at the call site.
        assertThrows(IllegalArgumentException.class, () -> FroalaToolbar
                .ofGroups(FroalaToolbarGroup.named("myGroup", "bold", "italic", "underline", "strikeThrough")));

        // the same four buttons under a name Froala has a button for
        FroalaToolbar.ofGroups(
                FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold", "italic", "underline", "strikeThrough"));

        // and under any name at all, as soon as the group shows everything it holds
        FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold", "italic", "underline", "strikeThrough")
                .withButtonsVisible(4));

        // zero is the opposite of "no overflow", because it moves the whole group into the panel
        assertThrows(IllegalArgumentException.class,
                () -> FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold").withButtonsVisible(0)));

        // separators are drawn as lines, not buttons, and Froala does not count them towards buttonsVisible
        FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold", "|", "italic", "-", "underline"));

        // and a group with no buttons has nothing to hide, whatever the count says
        FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup").withButtonsVisible(0));
    }

    @Test
    void negativeButtonsVisible_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FroalaToolbarGroup.named("myGroup", "bold").withButtonsVisible(-1));
        assertThrows(IllegalArgumentException.class, () -> FroalaToolbar
                .ofGroups(FroalaToolbarGroup.named("myGroup", "bold", "italic")).withButtonsVisible("myGroup", -1));
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
        assertEquals("myGroup: {\"buttons\":[\"bold\"],\"align\":\"left\"}",
                base.withAlign(FroalaToolbarAlign.LEFT).toString());
        FroalaToolbarGroup derived = base.withAlign(FroalaToolbarAlign.RIGHT).withButtonsVisible(1);

        assertEquals("myGroup: {\"buttons\":[\"bold\"]}", base.toString());
        assertEquals("myGroup: {\"buttons\":[\"bold\"],\"align\":\"right\",\"buttonsVisible\":1}", derived.toString());
    }

    @Test
    void noButtonsVisible_isWrittenRatherThanTreatedAsUnset() {
        // Zero is the one value the API documents as meaningful and dangerous. It moves every button of the group into
        // the overflow panel. Dropping it as "nothing set" would silently give Froala's three instead.
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
        assertThrows(NullPointerException.class,
                () -> FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold")).withAllButtonsVisible(null));
    }

    @Test
    void blankGroupName_isRejected() {
        // Froala keys its groups by name, and a blank one only goes wrong in the browser
        assertThrows(IllegalArgumentException.class, () -> FroalaToolbarGroup.named(" ", "bold"));
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

    @Test
    void withAllButtonsVisible_changesOnlyThatGroup() {
        FroalaToolbar toolbar = FroalaToolbar.ofGroups(FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold"),
                FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_RICH, "insertLink", "insertImage"));

        assertEquals(
                "{\"moreText\":{\"buttons\":[\"bold\"]},"
                        + "\"moreRich\":{\"buttons\":[\"insertLink\",\"insertImage\"],\"buttonsVisible\":2}}",
                toolbar.withAllButtonsVisible(FroalaToolbarGroup.MORE_RICH).toString());
    }

    @Test
    void withAllButtonsVisible_showsEveryGroupInFull() {
        String json = FroalaToolbar.froalaDefault().withAllButtonsVisible().toString();

        // moreText has 13 buttons and no count of its own. Froala's trackChanges group shows none of its 5.
        assertTrue(json.contains("\"clearFormatting\"],\"buttonsVisible\":13}"), json);
        assertTrue(json.contains("\"removeLast\"],\"buttonsVisible\":5}"), json);
    }

    @Test
    void withAllButtonsVisible_leavesAFlatToolbarAsItIs() {
        FroalaToolbar flat = FroalaToolbar.of("bold", "italic");

        assertSame(flat, flat.withAllButtonsVisible());
    }

    @Test
    void basics_areFroalasGroupsWithTheBasicButtons() {
        assertEquals("{\"moreText\":{\"buttons\":[\"bold\",\"italic\",\"underline\",\"strikeThrough\",\"subscript\","
                + "\"superscript\",\"fontFamily\",\"fontSize\",\"textColor\",\"backgroundColor\",\"clearFormatting\"]},"
                + "\"moreParagraph\":{\"buttons\":[\"alignLeft\",\"alignCenter\",\"formatOLSimple\",\"alignRight\","
                + "\"alignJustify\",\"formatOL\",\"formatUL\",\"paragraphFormat\",\"lineHeight\",\"outdent\",\"indent\","
                + "\"quote\"]},\"moreRich\":{\"buttons\":[\"insertAnchor\",\"insertLink\",\"insertHR\"],\"buttonsVisible\":4},"
                + "\"moreMisc\":{\"buttons\":[\"undo\",\"redo\",\"selectAll\",\"help\",\"findReplaceButton\"],"
                + "\"align\":\"right\",\"buttonsVisible\":2}}", FroalaToolbar.basics().toString());
    }

    @Test
    void withButtonsVisible_setsTheCountOfThatGroup() {
        String json = FroalaToolbar.froalaDefault().withButtonsVisible(FroalaToolbarGroup.MORE_TEXT, 5).toString();

        assertTrue(json.contains("\"clearFormatting\"],\"buttonsVisible\":5}"), json);
    }

    @Test
    void groupVisibility_ofAnUnknownGroup_isRejected() {
        FroalaToolbar toolbar = FroalaToolbar.froalaDefault();

        assertThrows(IllegalArgumentException.class, () -> toolbar.withAllButtonsVisible("noSuchGroup"));
    }

    @Test
    void groupVisibility_belowTheSizeOfAGroupWithoutOverflowButton_isRejected() {
        // a group of its own name has no button to open its overflow panel, so it must keep showing everything
        FroalaToolbar toolbar = FroalaToolbar.ofGroups(FroalaToolbarGroup.named("myGroup", "bold", "italic"));

        assertThrows(IllegalArgumentException.class, () -> toolbar.withButtonsVisible("myGroup", 1));
    }

    @Test
    void groupVisibility_onAFlatToolbar_isRejected() {
        assertThrows(IllegalStateException.class,
                () -> FroalaToolbar.of("bold", "italic").withAllButtonsVisible(FroalaToolbarGroup.MORE_RICH));
    }
}
