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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * One named group of toolbar buttons in Froala's grouped toolbar, with the alignment and the overflow count that belong
 * to the group rather than to a button. Immutable: every {@code with…} answers a new instance.
 *
 * <pre>
 * FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold", "italic", "underline").withButtonsVisible(2);
 * </pre>
 *
 * <p>
 * Groups are handed to {@link FroalaToolbar#ofGroups(FroalaToolbarGroup...)}, which is what an editor is configured
 * with.
 *
 * <p>
 * <b>The group's name is also the command name of its overflow button</b>, and that is the trap this class exists to
 * document. A group overflows as soon as it holds more buttons than its {@code buttonsVisible} -- <b>three, unless
 * {@link #withButtonsVisible(int)} says otherwise</b>, so a four-button group overflows without anyone asking for it.
 * Froala then moves the rest into a collapsed panel and looks up {@code FroalaEditor.COMMANDS[name]} to draw the button
 * that opens it. It ships four commands under such names -- {@link #MORE_TEXT}, {@link #MORE_PARAGRAPH},
 * {@link #MORE_RICH} and {@link #MORE_MISC} -- and finds nothing for any other name: the overflow buttons are then in
 * the DOM with nothing to open them. So unless a group is guaranteed to stay within its {@code buttonsVisible}, it has
 * to carry one of those four names.
 */
public final class FroalaToolbarGroup implements Serializable {

    /**
     * The name Froala's own default toolbar gives its text formatting group. One of the four names that has an overflow
     * command registered under it, so a group carrying it can overflow safely.
     */
    public static final String MORE_TEXT = "moreText";

    /**
     * The name Froala's own default toolbar gives its paragraph formatting group. One of the four names that has an
     * overflow command registered under it, so a group carrying it can overflow safely.
     */
    public static final String MORE_PARAGRAPH = "moreParagraph";

    /**
     * The name Froala's own default toolbar gives its inserted content group. One of the four names that has an
     * overflow command registered under it, so a group carrying it can overflow safely.
     */
    public static final String MORE_RICH = "moreRich";

    /**
     * The name Froala's own default toolbar gives its everything else group. One of the four names that has an overflow
     * command registered under it, so a group carrying it can overflow safely.
     */
    public static final String MORE_MISC = "moreMisc";

    private final String name;
    private final List<String> buttons;
    private final FroalaToolbarAlign align;
    private final Integer buttonsVisible;

    private FroalaToolbarGroup(String name, List<String> buttons, FroalaToolbarAlign align, Integer buttonsVisible) {
        this.name = name;
        this.buttons = buttons;
        this.align = align;
        this.buttonsVisible = buttonsVisible;
    }

    /**
     * Creates a group of the given buttons. A button name is a registered command name; {@code "|"} draws a vertical
     * separator and {@code "-"} a horizontal one, and neither ever ends up in the overflow panel. A name Froala does
     * not know, or one whose plugin is not enabled, is dropped without a word -- but an unknown name still counts
     * towards {@link #withButtonsVisible(int)}.
     *
     * @param name the group's name, which is also its overflow button's command name -- see the class documentation
     * @param buttons command names in the order they should appear, none of them null
     * @return a new instance
     * @throws NullPointerException if the name or any button name is null
     */
    public static FroalaToolbarGroup named(String name, String... buttons) {
        Objects.requireNonNull(name, "A toolbar group needs a name");

        for (String button : buttons) {
            Objects.requireNonNull(button, "A toolbar button needs a command name");
        }

        return new FroalaToolbarGroup(name, List.of(buttons), null, null);
    }

    /**
     * Puts the group at the given end of the toolbar. Froala's {@code align}, {@code left} when a group does not say.
     *
     * @param align which end of the toolbar the group sits at, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaToolbarGroup withAlign(FroalaToolbarAlign align) {
        return new FroalaToolbarGroup(name, buttons, align, buttonsVisible);
    }

    /**
     * Sets how many of the group's buttons are shown before the rest move into the overflow panel. Froala's
     * {@code buttonsVisible}, {@code 3} when a group does not say.
     *
     * <p>
     * The count is not a maximum with an escape hatch: {@code 0} or a negative number moves every button of the group
     * into the panel, it does not switch the panel off. Switching it off is what a count larger than the group is for
     * -- Froala forces overflow on for every grouped toolbar and ignores an attempt to set {@code showMoreButtons}.
     * Raising the count above the group's size is therefore also the only way to keep a freely named group out of the
     * trap described in the class documentation.
     *
     * @param buttonsVisible how many buttons are shown before the overflow panel takes the rest
     * @return a new instance
     */
    public FroalaToolbarGroup withButtonsVisible(int buttonsVisible) {
        return new FroalaToolbarGroup(name, buttons, align, buttonsVisible);
    }

    /**
     * Returns the group's name, which is also the command name of its overflow button.
     *
     * @return the group's name, never null
     */
    public String getName() {
        return name;
    }

    /** Returns the group as Froala receives it: its buttons plus whatever of align and buttonsVisible was set. */
    JsonObject toJson() {
        JsonArray names = Json.createArray();
        buttons.forEach(button -> names.set(names.length(), button));

        JsonObject group = Json.createObject();
        group.put("buttons", names);

        if (align != null) {
            group.put("align", align.getOptionValue());
        }
        if (buttonsVisible != null) {
            group.put("buttonsVisible", buttonsVisible);
        }

        return group;
    }

    /**
     * Returns the group as the JSON Froala receives. Meant for logging and debugging.
     *
     * @return the group as JSON
     */
    @Override
    public String toString() {
        return name + ": " + toJson().toJson();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FroalaToolbarGroup group)) {
            return false;
        }

        return name.equals(group.name) && buttons.equals(group.buttons) && align == group.align
                && Objects.equals(buttonsVisible, group.buttonsVisible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, buttons, align, buttonsVisible);
    }
}
