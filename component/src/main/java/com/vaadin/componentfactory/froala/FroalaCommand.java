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
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import elemental.json.Json;
import elemental.json.JsonObject;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.dom.Element;

/**
 * A command of the application's own, added to an editor with
 * {@link FroalaEditor#addCommand(FroalaCommand, com.vaadin.flow.component.ComponentEventListener)}. Froala triggers a
 * command from a toolbar button, from a popup's button list and from its keyboard shortcut. Where it appears is decided
 * by its name, like for any of Froala's own commands, e.g. {@code FroalaToolbar.of("bold", "insertTemplate")}.
 *
 * <pre>
 * new FroalaCommand("insertTemplate", "Insert template", VaadinIcon.FILE_TEXT.create()).withShortcut(Key.KEY_T,
 *         KeyModifier.SHIFT)
 * </pre>
 *
 * <p>
 * The icon is any Vaadin icon: a {@code VaadinIcon} or {@code LumoIcon}, an icon of an own iconset, an {@code SvgIcon}
 * with a URL or a {@code FontIcon}. The button draws a {@code <vaadin-icon>} with the icon's attributes and properties,
 * read when the command is added. An {@code SvgIcon} whose source is a {@code DownloadHandler} has no URL before it is
 * attached, so it draws nothing.
 *
 * @param name the command name, letters, digits and underscores, starting with a letter
 * @param title the button's tooltip and accessible name
 * @param icon the button's icon
 * @param shortcutKeyCode the key code that triggers the command together with Ctrl, or Cmd on a Mac, as Froala expects
 *            it: the keyboard event's {@code keyCode}. 0 for no shortcut.
 * @param shortcutLabel the key as the button's tooltip shows it after Ctrl, Shift and Alt, e.g. {@code T} or
 *            {@code F2}. Null for no shortcut.
 * @param shortcutModifiers {@link KeyModifier#SHIFT} and {@link KeyModifier#ALT} on top of Ctrl or Cmd, or empty.
 *            {@link KeyModifier#CONTROL} and {@link KeyModifier#META} are dropped, because Froala always adds them.
 * @param toggle whether the button shows a pressed state, see {@link FroalaEditor#setCommandActive}
 */
public record FroalaCommand(String name, String title, AbstractIcon<?> icon, int shortcutKeyCode, String shortcutLabel,
        Set<KeyModifier> shortcutModifiers, boolean toggle) implements Serializable {

    /** What Froala puts into a {@code data-cmd} attribute and its button ids unescaped, so nothing else is allowed. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /** A letter or digit as a {@link Key} names it, e.g. {@code KeyT} or {@code Digit4}, or a bare {@code t}. */
    private static final Pattern SHORTCUT_KEY = Pattern.compile("(?:Key|Digit)?([A-Za-z0-9])");

    /** A function key from F1 to F12 as a {@link Key} names it. Their key codes are 112 to 123, on every layout. */
    private static final Pattern FUNCTION_KEY = Pattern.compile("F([1-9]|1[0-2])");

    /**
     * Creates a command.
     *
     * @throws IllegalArgumentException if the name is not a valid command name, or the shortcut is one Froala cannot
     *             bind
     */
    public FroalaCommand {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(icon, "icon must not be null");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Command name '" + name
                    + "' must start with a letter and hold only letters, digits and underscores");
        }

        // Froala always adds Ctrl, or Cmd on a Mac, so either one given here says nothing and is dropped
        shortcutModifiers = shortcutModifiers == null ? Set.of()
                : shortcutModifiers.stream()
                        .filter(modifier -> modifier != KeyModifier.CONTROL && modifier != KeyModifier.META)
                        .collect(Collectors.toUnmodifiableSet());
        if (shortcutKeyCode == 0 && shortcutLabel == null) {
            if (!shortcutModifiers.isEmpty()) {
                throw new IllegalArgumentException("Shortcut modifiers need a shortcut key");
            }
        } else {
            if (shortcutKeyCode < 1) {
                throw new IllegalArgumentException("A shortcut key code must be 1 or more, but got " + shortcutKeyCode);
            }
            // Froala's tooltip hint ends with the label, and would read "Ctrl+undefined" without one
            if (shortcutLabel == null || shortcutLabel.isBlank()) {
                throw new IllegalArgumentException("A shortcut needs a label for the button's tooltip");
            }
        }
        // Froala's shortcuts can add Shift and Alt to Ctrl or Cmd. Nothing else.
        if (!Set.of(KeyModifier.SHIFT, KeyModifier.ALT).containsAll(shortcutModifiers)) {
            throw new IllegalArgumentException(
                    "A shortcut is Ctrl or Cmd plus a key, with Shift or Alt optionally on top."
                            + " Other modifiers are not possible, but got " + shortcutModifiers);
        }
    }

    /**
     * Creates a command without a keyboard shortcut.
     *
     * @param name the command name, letters, digits and underscores, starting with a letter
     * @param title the button's tooltip and accessible name
     * @param icon the button's icon
     */
    public FroalaCommand(String name, String title, AbstractIcon<?> icon) {
        this(name, title, icon, 0, null, Set.of(), false);
    }

    /**
     * Returns a copy of this command triggered by Ctrl, or Cmd on a Mac, plus the given key. Froala adds Ctrl or Cmd
     * itself, and Ctrl and Cmd given as modifiers are ignored. Froala shows the shortcut in the button's tooltip.
     *
     * <p>
     * Froala finds a shortcut by the keyboard event's numeric {@code keyCode}, not by the {@link Key}. A letter, a
     * digit and a function key from F1 to F12 each have a fixed code, e.g. 84 for T and 121 for F10, so this method
     * derives it from the key.
     *
     * <p>
     * Other keys throw, e.g. {@link Key#SLASH}. Their code depends on the keyboard layout. The slash is a key of its
     * own on a US layout but Shift+7 on a German one, so the same {@link Key} leads to different key codes on different
     * layouts, and this method cannot map it to one code. Where such a shortcut is needed, define the key codes for the
     * layouts your users have with {@link #withShortcut(int, String, KeyModifier...)}.
     *
     * @param key a letter, digit or function key from F1 to F12, e.g. {@link Key#KEY_T}, {@link Key#DIGIT_4} or
     *            {@link Key#F10}
     * @param modifiers {@link KeyModifier#SHIFT} and {@link KeyModifier#ALT}, on top of Ctrl or Cmd. Ctrl and Cmd given
     *            here are ignored.
     * @return a new command with the shortcut
     * @throws IllegalArgumentException if the key is not a letter, digit or function key from F1 to F12, or a modifier
     *             is not Shift, Alt, Ctrl or Cmd
     * @see #withShortcut(int, String, KeyModifier...) for any other key
     */
    public FroalaCommand withShortcut(Key key, KeyModifier... modifiers) {
        Objects.requireNonNull(key, "key must not be null");
        Matcher functionKey = FUNCTION_KEY.matcher(key.getKeys().get(0));
        if (functionKey.matches()) {
            return withShortcut(111 + Integer.parseInt(functionKey.group(1)), functionKey.group(), modifiers);
        }
        String letter = shortcutLetter(key);
        // Froala matches the event's keyCode, which for a letter or digit is the code of the upper case character
        return withShortcut(letter.charAt(0), letter, modifiers);
    }

    /**
     * Returns a copy of this command triggered by Ctrl, or Cmd on a Mac, plus the key with the given key code, e.g.
     * {@code withShortcut(113, "F2")} for Ctrl+F2. Froala adds Ctrl or Cmd itself, and Ctrl and Cmd given as modifiers
     * are ignored. The key code is what Froala expects, the keyboard event's {@code keyCode}. Any key code from 1 up is
     * accepted. Which code a key has on which keyboard layout, and whether the browser or the operating system takes a
     * combination before the page sees it, is up to the caller.
     *
     * @param keyCode the key code, 1 or more
     * @param shortcutLabel the key as the button's tooltip shows it after Ctrl, Shift and Alt, e.g. {@code F2}
     * @param modifiers {@link KeyModifier#SHIFT} and {@link KeyModifier#ALT}, on top of Ctrl or Cmd. Ctrl and Cmd given
     *            here are ignored.
     * @return a new command with the shortcut
     * @throws IllegalArgumentException if the key code is below 1, the label is null or blank, or a modifier is not
     *             Shift, Alt, Ctrl or Cmd
     */
    public FroalaCommand withShortcut(int keyCode, String shortcutLabel, KeyModifier... modifiers) {
        return new FroalaCommand(name, title, icon, keyCode, shortcutLabel, Set.copyOf(Arrays.asList(modifiers)),
                toggle);
    }

    /**
     * Returns a copy of this command whose button shows a pressed state, like Froala's bold button. The application
     * holds the state and switches it with {@link FroalaEditor#setCommandActive(FroalaCommand, boolean)}, typically in
     * the command's listener.
     *
     * @return a new command that is a toggle
     */
    public FroalaCommand withToggle() {
        return new FroalaCommand(name, title, icon, shortcutKeyCode, shortcutLabel, shortcutModifiers, true);
    }

    /** The command as the client registers it with Froala. */
    JsonObject toJson() {
        JsonObject json = Json.createObject();
        json.put("name", name);
        json.put("title", title);
        json.put("icon", iconAttributes());
        json.put("toggle", toggle);

        if (shortcutLabel != null) {
            JsonObject shortcut = Json.createObject();
            shortcut.put("keyCode", shortcutKeyCode);
            shortcut.put("letter", shortcutLabel);
            shortcut.put("shift", shortcutModifiers.contains(KeyModifier.SHIFT));
            shortcut.put("alt", shortcutModifiers.contains(KeyModifier.ALT));
            json.put("shortcut", shortcut);
        }
        return json;
    }

    /**
     * The icon's attributes and properties, all as the attributes {@code <vaadin-icon>} reads them from. Froala draws
     * an icon from an HTML string, which has no properties. An icon sets some of each, e.g. {@code SvgIcon} its
     * {@code src} as an attribute and its {@code symbol} as a property.
     */
    private JsonObject iconAttributes() {
        Element element = icon.getElement();
        JsonObject attributes = Json.createObject();
        element.getAttributeNames().forEach(attribute -> {
            String value = element.getAttribute(attribute);
            if (value != null) {
                attributes.put(attribute, value);
            }
        });
        element.getPropertyNames().forEach(property -> {
            String value = element.getProperty(property);
            if (value != null) {
                attributes.put(property.replaceAll("([A-Z])", "-$1").toLowerCase(Locale.ROOT), value);
            }
        });
        return attributes;
    }

    private static String shortcutLetter(Key key) {
        Matcher matcher = SHORTCUT_KEY.matcher(key.getKeys().get(0));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "A shortcut key must be a letter, a digit or F1 to F12, but got " + key.getKeys().get(0));
        }
        return matcher.group(1).toUpperCase(Locale.ROOT);
    }
}
