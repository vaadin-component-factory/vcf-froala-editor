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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A toolbar layout. It sets which buttons the toolbar shows and in which order, and in the grouped form how they are
 * grouped. Immutable. Handed to {@link FroalaOptions#withToolbarButtons(FroalaToolbar)} or one of its three
 * narrower-screen siblings.
 *
 * <p>
 * Froala reads a toolbar in two shapes and treats them differently, so this class offers both:
 *
 * <ul>
 * <li>{@link #of(String...)} gives a flat list. Every button given is shown. Froala splits the list into groups at the
 * separators and disables its overflow panel.</li>
 * <li>{@link #ofGroups(FroalaToolbarGroup...)} gives named groups, each with its own alignment and its own count of
 * buttons shown before the rest move into an overflow panel. This is the shape Froala's default toolbar uses, and the
 * only one with an overflow panel.</li>
 * </ul>
 *
 * <pre>
 * FroalaToolbar toolbar = FroalaToolbar.ofGroups(
 *         FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, "bold", "italic", "underline"),
 *         FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, "undo", "redo").withAlign(FroalaToolbarAlign.RIGHT));
 *
 * FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withToolbarButtons(toolbar));
 * </pre>
 *
 * <p>
 * A button name is a registered command name, for example {@code "bold"}, {@code "paragraphFormat"},
 * {@code "insertImage"} or {@code "fullscreen"}. They are listed under {@code toolbarButtons} in
 * <a href="https://froala.com/wysiwyg-editor/docs/options/">Froala's option documentation</a>. A name Froala does not
 * know is dropped silently.
 */
public final class FroalaToolbar implements Serializable {

    /** Set in the flat form, null in the grouped one. Exactly one of the two fields is set. */
    private final List<String> buttons;

    /** Set in the grouped form, null in the flat one. Exactly one of the two fields is set. */
    private final List<FroalaToolbarGroup> groups;

    private FroalaToolbar(List<String> buttons, List<FroalaToolbarGroup> groups) {
        this.buttons = buttons;
        this.groups = groups;
    }

    /**
     * Creates a flat toolbar that shows every button given, in the order given. The overflow panel exists only in the
     * grouped form, so a flat toolbar wider than the window wraps instead of collapsing.
     *
     * <p>
     * {@code "|"} and {@code "-"} both start a new group and neither is drawn. Froala consumes them while splitting the
     * list into groups, so in this form the two are interchangeable. Inside a {@link FroalaToolbarGroup} they are drawn
     * as separator lines instead, one vertical and one horizontal.
     *
     * @param buttons command names in the order they should appear, none of them null
     * @return a new instance
     * @throws NullPointerException if any button name is null
     */
    public static FroalaToolbar of(String... buttons) {
        for (String button : buttons) {
            Objects.requireNonNull(button, "A toolbar button needs a command name");
        }

        return new FroalaToolbar(List.of(buttons), null);
    }

    /**
     * Creates a grouped toolbar from the given groups, in the order given.
     *
     * @param groups the button groups, none of them null and no two of them sharing a name
     * @return a new instance
     * @throws IllegalArgumentException under the same conditions as {@link #ofGroups(Collection)}
     * @throws NullPointerException if any group is null
     */
    public static FroalaToolbar ofGroups(FroalaToolbarGroup... groups) {
        return ofGroups(Arrays.asList(groups));
    }

    /**
     * Creates a grouped toolbar from the given groups, in the order given.
     *
     * @param groups the button groups, none of them null and no two of them sharing a name
     * @return a new instance
     * @throws IllegalArgumentException if two groups share a name, since Froala keys its groups by name and the second
     *             would replace the first. Also thrown if a group with a name of its own shows fewer buttons than it
     *             holds, since Froala draws no button to open its overflow panel. See {@link FroalaToolbarGroup}.
     * @throws NullPointerException if any group is null
     */
    public static FroalaToolbar ofGroups(Collection<FroalaToolbarGroup> groups) {
        Set<String> names = new LinkedHashSet<>();

        for (FroalaToolbarGroup group : groups) {
            Objects.requireNonNull(group, "A toolbar group must not be null");

            if (!names.add(group.getName())) {
                throw new IllegalArgumentException("Two toolbar groups are named '" + group.getName()
                        + "'. Froala keys its groups by name, so the second one would replace the first.");
            }

            if (group.overflowsWithNothingToOpenIt()) {
                throw new IllegalArgumentException("Toolbar group '" + group.getName()
                        + "' shows fewer buttons than it holds, and Froala draws no button to open the rest. It takes"
                        + " that button from a command registered under the group's name, and only its own four have"
                        + " one. Name the group FroalaToolbarGroup.MORE_TEXT, MORE_PARAGRAPH, MORE_RICH or MORE_MISC,"
                        + " or raise withButtonsVisible to the group's size so nothing has to be opened.");
            }
        }

        return new FroalaToolbar(null, List.copyOf(groups));
    }

    /** Returns the toolbar as Froala receives it: an array in the flat form, an object of named groups in the other. */
    JsonValue toJson() {
        if (groups == null) {
            JsonArray names = Json.createArray();
            buttons.forEach(button -> names.set(names.length(), button));

            return names;
        }

        JsonObject grouped = Json.createObject();
        groups.forEach(group -> grouped.put(group.getName(), group.toJson()));

        return grouped;
    }

    /**
     * Returns the toolbar as the JSON Froala receives. Meant for logging and debugging.
     *
     * @return the toolbar as JSON
     */
    @Override
    public String toString() {
        return toJson().toJson();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FroalaToolbar toolbar)) {
            return false;
        }

        return Objects.equals(buttons, toolbar.buttons) && Objects.equals(groups, toolbar.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buttons, groups);
    }
}
