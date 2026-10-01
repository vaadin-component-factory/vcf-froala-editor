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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FroalaCommandTest {

    @Test
    void vaadinIcon_isSentAsItsIconAttribute() {
        FroalaCommand command = new FroalaCommand("insertTemplate", "Insert template", VaadinIcon.FILE_TEXT.create());

        assertEquals(
                "{\"name\":\"insertTemplate\",\"title\":\"Insert template\",\"icon\":{\"icon\":\"vaadin:file-text\"},"
                        + "\"toggle\":false}",
                command.toJson().toJson());
    }

    @Test
    void iconProperties_areSentAsTheAttributesVaadinIconReadsThemFrom() {
        FroalaCommand svg = new FroalaCommand("a", "A", new SvgIcon("icons/sprite.svg", "star"));
        FroalaCommand font = new FroalaCommand("b", "B", new FontIcon("fa", "fa-star"));

        assertEquals("{\"src\":\"icons/sprite.svg\",\"symbol\":\"star\"}", svg.toJson().getObject("icon").toJson());
        assertEquals("{\"icon-class\":\"fa fa-star\"}", font.toJson().getObject("icon").toJson());
    }

    @Test
    void shortcut_isSentAsFroalasKeyCodeAndLetter() {
        FroalaCommand letter = new FroalaCommand("a", "A", VaadinIcon.STAR.create()).withShortcut(Key.KEY_T,
                KeyModifier.SHIFT);
        FroalaCommand digit = new FroalaCommand("b", "B", VaadinIcon.STAR.create()).withShortcut(Key.DIGIT_4,
                KeyModifier.ALT);

        assertEquals("{\"keyCode\":84,\"letter\":\"T\",\"shift\":true,\"alt\":false}",
                letter.toJson().getObject("shortcut").toJson());
        assertEquals("{\"keyCode\":52,\"letter\":\"4\",\"shift\":false,\"alt\":true}",
                digit.toJson().getObject("shortcut").toJson());
    }

    @Test
    void shortcut_takesOnlyWhatFroalaCanBind() {
        FroalaCommand command = new FroalaCommand("a", "A", VaadinIcon.STAR.create());

        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(Key.KEY_T, KeyModifier.ALT_GRAPH));
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(Key.F13));
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(Key.SLASH));
    }

    @Test
    void ctrlAndCmd_areIgnored_becauseFroalaAlwaysAddsThem() {
        FroalaCommand command = new FroalaCommand("a", "A", VaadinIcon.STAR.create());

        assertEquals(Set.of(KeyModifier.SHIFT),
                command.withShortcut(Key.KEY_T, KeyModifier.CONTROL, KeyModifier.SHIFT).getShortcutModifiers());
        assertEquals(Set.of(), command.withShortcut(113, "F2", KeyModifier.META).getShortcutModifiers());
    }

    @Test
    void functionKeyShortcut_isSentAsItsKeyCodeAndName() {
        FroalaCommand f1 = new FroalaCommand("a", "A", VaadinIcon.STAR.create()).withShortcut(Key.F1);
        FroalaCommand f12 = new FroalaCommand("b", "B", VaadinIcon.STAR.create()).withShortcut(Key.F12,
                KeyModifier.SHIFT);

        assertEquals("{\"keyCode\":112,\"letter\":\"F1\",\"shift\":false,\"alt\":false}",
                f1.toJson().getObject("shortcut").toJson());
        assertEquals("{\"keyCode\":123,\"letter\":\"F12\",\"shift\":true,\"alt\":false}",
                f12.toJson().getObject("shortcut").toJson());
    }

    @Test
    void keyCodeShortcut_isSentWithItsLabel() {
        FroalaCommand command = new FroalaCommand("a", "A", VaadinIcon.STAR.create()).withShortcut(113, "F2",
                KeyModifier.SHIFT);

        assertEquals("{\"keyCode\":113,\"letter\":\"F2\",\"shift\":true,\"alt\":false}",
                command.toJson().getObject("shortcut").toJson());
    }

    @Test
    void keyCodeShortcut_takesAnyKeyCodeFromOneWithALabel() {
        FroalaCommand command = new FroalaCommand("a", "A", VaadinIcon.STAR.create());

        assertEquals(1, command.withShortcut(1, "X").getShortcutKeyCode());
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(0, "X"));
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(-1, "X"));
        assertThrows(NullPointerException.class, () -> command.withShortcut(113, null));
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(113, " "));
        assertThrows(IllegalArgumentException.class, () -> command.withShortcut(113, "F2", KeyModifier.ALT_GRAPH));
        // 0 and null together are how the constructor reads "no shortcut", which this method must not produce
        assertThrows(NullPointerException.class, () -> command.withShortcut(Key.KEY_T).withShortcut(0, null));
    }

    @Test
    void toggle_isSentAndKeptByTheOtherCopies() {
        FroalaCommand plain = new FroalaCommand("a", "A", VaadinIcon.STAR.create());
        FroalaCommand toggle = plain.withToggle().withShortcut(Key.KEY_T);

        assertFalse(plain.toJson().getBoolean("toggle"));
        assertTrue(toggle.toJson().getBoolean("toggle"));
        assertEquals('T', toggle.getShortcutKeyCode());
        assertTrue(plain.withShortcut(Key.KEY_T).withToggle().isToggle());
    }

    @Test
    void name_mustBeUsableAsAFroalaCommandName() {
        assertThrows(IllegalArgumentException.class,
                () -> new FroalaCommand("insert template", "A", VaadinIcon.STAR.create()));
        assertThrows(IllegalArgumentException.class,
                () -> new FroalaCommand("x\" onclick=\"alert(1)", "A", VaadinIcon.STAR.create()));
        assertThrows(NullPointerException.class, () -> new FroalaCommand("a", "A", null));
    }
}
