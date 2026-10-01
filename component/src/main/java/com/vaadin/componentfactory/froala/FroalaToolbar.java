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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

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
 *         FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, FroalaButton.BOLD, FroalaButton.ITALIC),
 *         FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, FroalaButton.UNDO, FroalaButton.REDO)
 *                 .withAlign(FroalaToolbarAlign.RIGHT));
 *
 * FroalaEditor editor = new FroalaEditor(FroalaOptions.defaults().withToolbarButtons(toolbar));
 * </pre>
 *
 * <p>
 * A button name is a registered command name. {@link FroalaButton} has a constant for each of Froala's. A command the
 * application registers itself is passed as a plain string. A name Froala does not know is dropped silently.
 */
public final class FroalaToolbar implements Serializable {

    /** Froala's default toolbar with only the buttons of {@link FroalaPlugin#basics()} and of no plugin at all. */
    private static final FroalaToolbar BASICS = new FroalaToolbar(null,
            List.of(FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, FroalaButton.BOLD, FroalaButton.ITALIC,
                    FroalaButton.UNDERLINE, FroalaButton.STRIKE_THROUGH, FroalaButton.SUBSCRIPT,
                    FroalaButton.SUPERSCRIPT, FroalaButton.FONT_FAMILY, FroalaButton.FONT_SIZE, FroalaButton.TEXT_COLOR,
                    FroalaButton.BACKGROUND_COLOR, FroalaButton.CLEAR_FORMATTING),
                    FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_PARAGRAPH, FroalaButton.ALIGN_LEFT,
                            FroalaButton.ALIGN_CENTER, FroalaButton.FORMAT_OL_SIMPLE, FroalaButton.ALIGN_RIGHT,
                            FroalaButton.ALIGN_JUSTIFY, FroalaButton.FORMAT_OL, FroalaButton.FORMAT_UL,
                            FroalaButton.PARAGRAPH_FORMAT, FroalaButton.LINE_HEIGHT, FroalaButton.OUTDENT,
                            FroalaButton.INDENT, FroalaButton.QUOTE),
                    FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_RICH, FroalaButton.INSERT_ANCHOR,
                            FroalaButton.INSERT_LINK, FroalaButton.INSERT_HR).withButtonsVisible(4),
                    FroalaToolbarGroup
                            .named(FroalaToolbarGroup.MORE_MISC, FroalaButton.UNDO, FroalaButton.REDO,
                                    FroalaButton.SELECT_ALL, FroalaButton.HELP, FroalaButton.FIND_REPLACE_BUTTON)
                            .withAlign(FroalaToolbarAlign.RIGHT).withButtonsVisible(2)));

    /**
     * Froala's {@code TOOLBAR_BUTTONS} in 5.4.0. Built without {@link #ofGroups(Collection)}, whose check would reject
     * Froala's own {@code trackChanges} group. It shows none of its buttons outside the overflow panel, under a name
     * that is not one of the four the check knows.
     */
    private static final FroalaToolbar FROALA_DEFAULT = new FroalaToolbar(null, List.of(
            FroalaToolbarGroup.named("versionControl", FroalaButton.VERSION_CONTROL, FroalaButton.AUTO_SAVE_STATUS)
                    .withButtonsVisible(2),
            FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_TEXT, FroalaButton.BOLD, FroalaButton.ITALIC,
                    FroalaButton.UNDERLINE, FroalaButton.STRIKE_THROUGH, FroalaButton.SUBSCRIPT,
                    FroalaButton.SUPERSCRIPT, FroalaButton.FONT_FAMILY, FroalaButton.FONT_SIZE, FroalaButton.TEXT_COLOR,
                    FroalaButton.BACKGROUND_COLOR, FroalaButton.INLINE_CLASS, FroalaButton.INLINE_STYLE,
                    FroalaButton.CLEAR_FORMATTING),
            FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_PARAGRAPH, FroalaButton.ALIGN_LEFT,
                    FroalaButton.ALIGN_CENTER, FroalaButton.FORMAT_OL_SIMPLE, FroalaButton.ALIGN_RIGHT,
                    FroalaButton.ALIGN_JUSTIFY, FroalaButton.FORMAT_OL, FroalaButton.FORMAT_UL,
                    FroalaButton.PARAGRAPH_FORMAT, FroalaButton.PARAGRAPH_STYLE, FroalaButton.LINE_HEIGHT,
                    FroalaButton.OUTDENT, FroalaButton.INDENT, FroalaButton.QUOTE),
            FroalaToolbarGroup.named("exportImport", FroalaButton.IMPORT_FROM_WORD, FroalaButton.EXPORT_TO_WORD),
            FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_RICH, FroalaButton.COLLAB_PANEL,
                    FroalaButton.AI_CHAT_ASSISTANT, FroalaButton.AI_ASSIST, FroalaButton.AI_SHORT_CUTS,
                    FroalaButton.COLLAB_ADD_COMMENT, FroalaButton.TRACK_CHANGES, FroalaButton.MARKDOWN,
                    FroalaButton.INSERT_ANCHOR, FroalaButton.INSERT_LINK, FroalaButton.INSERT_FILES,
                    FroalaButton.INSERT_IMAGE, FroalaButton.INSERT_VIDEO, FroalaButton.PAGE_BREAK,
                    FroalaButton.INSERT_TABLE, FroalaButton.EMOTICONS, FroalaButton.FONT_AWESOME,
                    FroalaButton.SPECIAL_CHARACTERS, FroalaButton.EMBEDLY, FroalaButton.INSERT_FILE,
                    FroalaButton.INSERT_HR, FroalaButton.OPEN_FILE_PICKER, FroalaButton.CODE_SNIPPET)
                    .withButtonsVisible(4),
            FroalaToolbarGroup.named(FroalaToolbarGroup.MORE_MISC, FroalaButton.UNDO, FroalaButton.REDO,
                    FroalaButton.FULLSCREEN, FroalaButton.PRINT, FroalaButton.GET_PDF, FroalaButton.SPELL_CHECKER,
                    FroalaButton.SELECT_ALL, FroalaButton.HTML, FroalaButton.HELP, FroalaButton.FIND_REPLACE_BUTTON)
                    .withAlign(FroalaToolbarAlign.RIGHT).withButtonsVisible(2),
            FroalaToolbarGroup.named("collab", FroalaButton.COLLAB_MODE, FroalaButton.COLLAB_PRESENCE)
                    .withAlign(FroalaToolbarAlign.RIGHT).withButtonsVisible(2),
            FroalaToolbarGroup.named("trackChanges", FroalaButton.SHOW_CHANGES, FroalaButton.APPLY_ALL,
                    FroalaButton.REMOVE_ALL, FroalaButton.APPLY_LAST, FroalaButton.REMOVE_LAST).withButtonsVisible(0)));

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
        Objects.requireNonNull(buttons, "buttons must not be null");
        for (String button : buttons) {
            Objects.requireNonNull(button, "A toolbar button needs a command name");
        }

        return new FroalaToolbar(List.of(buttons), null);
    }

    /**
     * Returns Froala's default toolbar reduced to the buttons of {@link FroalaPlugin#basics()}, plus the commands that
     * need no plugin, such as bold, italic, undo and redo. The groups, and how many buttons each shows before its
     * overflow panel, are those of Froala's default:
     *
     * <ul>
     * <li>{@link FroalaToolbarGroup#MORE_TEXT}: bold, italic, underline, strikeThrough, subscript, superscript,
     * fontFamily, fontSize, textColor, backgroundColor, clearFormatting</li>
     * <li>{@link FroalaToolbarGroup#MORE_PARAGRAPH}: alignLeft, alignCenter, formatOLSimple, alignRight, alignJustify,
     * formatOL, formatUL, paragraphFormat, lineHeight, outdent, indent, quote</li>
     * <li>{@link FroalaToolbarGroup#MORE_RICH}: insertAnchor, insertLink, insertHR</li>
     * <li>{@link FroalaToolbarGroup#MORE_MISC}: undo, redo, selectAll, help, findReplaceButton</li>
     * </ul>
     *
     * @return the toolbar, the same on every call
     */
    public static FroalaToolbar basics() {
        return BASICS;
    }

    /**
     * Returns Froala's own default toolbar, the one an editor shows when {@code toolbarButtons} is not set. It is a
     * starting point for a toolbar that differs in a detail, such as
     * {@code froalaDefault().withAllButtonsVisible(FroalaToolbarGroup.MORE_RICH)}. Froala drops the buttons whose
     * plugin is not enabled. The groups and their buttons, as froala-editor 5.4.0 defines them:
     *
     * <ul>
     * <li>{@code versionControl}: versionControl, autoSaveStatus</li>
     * <li>{@link FroalaToolbarGroup#MORE_TEXT}: bold, italic, underline, strikeThrough, subscript, superscript,
     * fontFamily, fontSize, textColor, backgroundColor, inlineClass, inlineStyle, clearFormatting</li>
     * <li>{@link FroalaToolbarGroup#MORE_PARAGRAPH}: alignLeft, alignCenter, formatOLSimple, alignRight, alignJustify,
     * formatOL, formatUL, paragraphFormat, paragraphStyle, lineHeight, outdent, indent, quote</li>
     * <li>{@code exportImport}: import_from_word, export_to_word</li>
     * <li>{@link FroalaToolbarGroup#MORE_RICH}: collabPanel, aiChatAssistant, aiAssist, aiShortCuts, collabAddComment,
     * trackChanges, markdown, insertAnchor, insertLink, insertFiles, insertImage, insertVideo, pageBreak, insertTable,
     * emoticons, fontAwesome, specialCharacters, embedly, insertFile, insertHR, openFilePicker, codeSnippet</li>
     * <li>{@link FroalaToolbarGroup#MORE_MISC}: undo, redo, fullscreen, print, getPDF, spellChecker, selectAll, html,
     * help, findReplaceButton</li>
     * <li>{@code collab}: collabMode, collabPresence</li>
     * <li>{@code trackChanges}: showChanges, applyAll, removeAll, applyLast, removeLast</li>
     * </ul>
     *
     * <p>
     * Froala's own default also has narrower variants with fewer visible buttons per group. Setting a toolbar replaces
     * those as well, unless {@link FroalaOptions#withToolbarButtonsSm(FroalaToolbar)} and its siblings set their own.
     *
     * @return the toolbar, the same on every call
     */
    public static FroalaToolbar froalaDefault() {
        return FROALA_DEFAULT;
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
        Objects.requireNonNull(groups, "groups must not be null");
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
        Objects.requireNonNull(groups, "groups must not be null");
        Set<String> names = new LinkedHashSet<>();

        for (FroalaToolbarGroup group : groups) {
            Objects.requireNonNull(group, "A toolbar group must not be null");

            if (!names.add(group.getName())) {
                throw new IllegalArgumentException("Two toolbar groups are named '" + group.getName()
                        + "'. Froala keys its groups by name, so the second one would replace the first.");
            }

            requireOpenableOverflow(group);
        }

        return new FroalaToolbar(null, List.copyOf(groups));
    }

    /**
     * Returns a copy in which every group shows all its buttons, so that none needs an overflow panel. A flat toolbar
     * shows every button anyway and is returned as it is.
     *
     * @return a new instance, or this one if it is flat
     */
    public FroalaToolbar withAllButtonsVisible() {
        if (groups == null) {
            return this;
        }

        return new FroalaToolbar(null,
                groups.stream().map(group -> group.withButtonsVisible(group.countButtons())).toList());
    }

    /**
     * Returns a copy in which the named group shows all its buttons, so that it needs no overflow panel. Froala then
     * draws no button to open one.
     *
     * @param group the group's name, such as {@link FroalaToolbarGroup#MORE_RICH}
     * @return a new instance
     * @throws IllegalArgumentException if no group has that name
     * @throws IllegalStateException if this is a flat toolbar, which has no groups
     * @throws NullPointerException if the group name is null
     */
    public FroalaToolbar withAllButtonsVisible(String group) {
        return withGroup(group, found -> found.withButtonsVisible(found.countButtons()));
    }

    /**
     * Returns a copy in which the named group shows the given number of buttons before the rest move into its overflow
     * panel. See {@link FroalaToolbarGroup#withButtonsVisible(int)}.
     *
     * @param group the group's name, such as {@link FroalaToolbarGroup#MORE_TEXT}
     * @param buttonsVisible how many buttons are shown before the overflow panel takes the rest
     * @return a new instance
     * @throws IllegalArgumentException if no group has that name, if the count is negative, or under the same
     *             conditions as {@link #ofGroups(Collection)}. That includes Froala's own groups
     *             {@code versionControl}, {@code collab} and {@code trackChanges} of {@link #froalaDefault()}, whose
     *             names the check does not know, at a count below their size.
     * @throws IllegalStateException if this is a flat toolbar, which has no groups
     * @throws NullPointerException if the group name is null
     */
    public FroalaToolbar withButtonsVisible(String group, int buttonsVisible) {
        return withGroup(group, found -> found.withButtonsVisible(buttonsVisible));
    }

    private FroalaToolbar withGroup(String name, UnaryOperator<FroalaToolbarGroup> change) {
        Objects.requireNonNull(name, "group must not be null");
        if (groups == null) {
            throw new IllegalStateException("A flat toolbar has no groups, and it shows every button anyway.");
        }

        List<FroalaToolbarGroup> changed = new ArrayList<>(groups);
        for (int i = 0; i < changed.size(); i++) {
            if (changed.get(i).getName().equals(name)) {
                FroalaToolbarGroup group = change.apply(changed.get(i));
                requireOpenableOverflow(group);
                changed.set(i, group);

                return new FroalaToolbar(null, List.copyOf(changed));
            }
        }

        throw new IllegalArgumentException("The toolbar has no group named '" + name + "'.");
    }

    private static void requireOpenableOverflow(FroalaToolbarGroup group) {
        if (group.overflowsWithNothingToOpenIt()) {
            throw new IllegalArgumentException("Toolbar group '" + group.getName()
                    + "' shows fewer buttons than it holds, and Froala draws no button to open the rest. It takes"
                    + " that button from a command registered under the group's name, and only its own four have"
                    + " one. Name the group FroalaToolbarGroup.MORE_TEXT, MORE_PARAGRAPH, MORE_RICH or MORE_MISC,"
                    + " or raise withButtonsVisible to the group's size so nothing has to be opened.");
        }
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
