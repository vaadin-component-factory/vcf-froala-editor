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
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * One named group of toolbar buttons in Froala's grouped toolbar, with the alignment and the overflow count that belong
 * to the group rather than to a button. Immutable. Every {@code with…} method returns a new instance.
 *
 * <pre>
 * FroalaToolbarGroup
 *         .named(FroalaToolbarGroup.MORE_TEXT, FroalaButton.BOLD, FroalaButton.ITALIC, FroalaButton.UNDERLINE)
 *         .withButtonsVisible(2);
 * </pre>
 *
 * <p>
 * Groups are passed to {@link FroalaToolbar#ofGroups(FroalaToolbarGroup...)}, and the resulting toolbar to
 * {@link FroalaOptions#withToolbarButtons(FroalaToolbar)}.
 *
 * <p>
 * The group's name is also the command name of its overflow button. A group overflows as soon as it holds more buttons
 * than its {@code buttonsVisible}, which is three unless {@link #withButtonsVisible(int)} sets another value, so a
 * four-button group overflows by default. Froala moves the remaining buttons into a collapsed panel, but draws the
 * button that opens it only for four names, {@link #MORE_TEXT}, {@link #MORE_PARAGRAPH}, {@link #MORE_RICH} and
 * {@link #MORE_MISC}. Under any other name the panel is rendered without a button to open it, and its buttons cannot be
 * reached.
 *
 * <p>
 * Any other name is therefore only valid for a group that shows all of its buttons.
 * {@link FroalaToolbar#ofGroups(FroalaToolbarGroup...)} throws otherwise.
 */
public final class FroalaToolbarGroup implements Serializable {

    /** The name Froala's default toolbar gives its text formatting group. */
    public static final String MORE_TEXT = "moreText";

    /** The name Froala's default toolbar gives its paragraph formatting group. */
    public static final String MORE_PARAGRAPH = "moreParagraph";

    /** The name Froala's default toolbar gives its inserted content group. */
    public static final String MORE_RICH = "moreRich";

    /** The name Froala's default toolbar gives its remaining buttons. */
    public static final String MORE_MISC = "moreMisc";

    /** Froala's default for {@code buttonsVisible}, applied to every group that does not set one. */
    private static final int DEFAULT_BUTTONS_VISIBLE = 3;

    /** The four names Froala registers an overflow command for. */
    private static final Set<String> NAMES_WITH_AN_OVERFLOW_BUTTON = Set.of(MORE_TEXT, MORE_PARAGRAPH, MORE_RICH,
            MORE_MISC);

    /** Separator entries. Froala draws them as lines and does not count them towards {@code buttonsVisible}. */
    private static final Set<String> SEPARATORS = Set.of(FroalaButton.VERTICAL_SEPARATOR,
            FroalaButton.HORIZONTAL_SEPARATOR);

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
     * Creates a group of the given buttons. A button name is a registered command name, such as those of
     * {@link FroalaButton}. {@link FroalaButton#VERTICAL_SEPARATOR} draws a vertical separator and
     * {@link FroalaButton#HORIZONTAL_SEPARATOR} a horizontal one. Neither is moved into the overflow panel. A name
     * Froala does not know is dropped silently, but still counts towards {@link #withButtonsVisible(int)}.
     *
     * @param name the group's name, which is also its overflow button's command name. See the class documentation.
     * @param buttons command names in the order they should appear, none of them null
     * @return a new instance
     * @throws NullPointerException if the name, the buttons or any button name is null
     * @throws IllegalArgumentException if the name is blank
     */
    public static FroalaToolbarGroup named(String name, String... buttons) {
        Objects.requireNonNull(name, "A toolbar group needs a name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A toolbar group needs a name that is not blank");
        }
        Objects.requireNonNull(buttons, "buttons must not be null");

        for (String button : buttons) {
            Objects.requireNonNull(button, "A toolbar button needs a command name");
        }

        return new FroalaToolbarGroup(name, List.of(buttons), null, null);
    }

    /**
     * Puts the group at the given end of the toolbar. Maps to Froala's {@code align} option, which defaults to
     * {@code left}.
     *
     * @param align which end of the toolbar the group sits at, or null to leave Froala's default
     * @return a new instance
     */
    public FroalaToolbarGroup withAlign(FroalaToolbarAlign align) {
        return new FroalaToolbarGroup(name, buttons, align, buttonsVisible);
    }

    /**
     * Sets how many of the group's buttons are shown before the rest move into the overflow panel. Maps to Froala's
     * {@code buttonsVisible} option, which defaults to {@code 3}.
     *
     * <p>
     * The overflow panel cannot be switched off. Froala enables it for every grouped toolbar and ignores
     * {@code showMoreButtons}. To show all of a group's buttons, set a count equal to or greater than the number of
     * buttons in the group. A count of {@code 0} moves every button into the panel.
     *
     * <p>
     * Separators are not counted, because {@code "|"} and {@code "-"} are drawn as lines and never move into the panel.
     * An unknown button name is counted, although Froala does not draw it.
     *
     * @param buttonsVisible how many buttons are shown before the overflow panel takes the rest, 0 or more
     * @return a new instance
     * @throws IllegalArgumentException if the count is negative
     */
    public FroalaToolbarGroup withButtonsVisible(int buttonsVisible) {
        if (buttonsVisible < 0) {
            throw new IllegalArgumentException("buttonsVisible must be 0 or more, but got " + buttonsVisible);
        }

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

    /**
     * Whether Froala would move buttons of this group into a panel that cannot be opened. Froala draws the opening
     * button from a command registered under the group's name, and registers one for four names only.
     *
     * @return true if the group would overflow under a name without a registered overflow command
     */
    boolean overflowsWithNothingToOpenIt() {
        if (NAMES_WITH_AN_OVERFLOW_BUTTON.contains(name)) {
            return false;
        }

        int drawn = countButtons();

        // A group with no buttons has nothing to hide, not even at a count of 0.
        return drawn > 0 && drawn > (buttonsVisible == null ? DEFAULT_BUTTONS_VISIBLE : buttonsVisible);
    }

    /** The number of buttons that count towards {@code buttonsVisible}, which is all of them but the separators. */
    int countButtons() {
        return (int) buttons.stream().filter(button -> !SEPARATORS.contains(button)).count();
    }

    /** Returns the group as Froala receives it: its buttons, plus align and buttonsVisible when they were set. */
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
