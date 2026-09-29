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
package com.vaadin.componentfactory.froala.it;

import java.util.List;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaThemeTestView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end test for the vaadin theme. The THM-n requirements are specified in issue #27. */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaThemeIT extends SpringPlaywrightIT {

    /**
     * Any CSS colour as [r, g, b, a] from 0 to 255. The browser reports a color-mix result as {@code color(srgb ...)}
     * with channels from 0 to 1, so reading the numbers out of the string would get it wrong. Painting it does not.
     */
    private static final String RGBA = """
            color => {
              const context = document.createElement('canvas').getContext('2d');
              context.fillStyle = color;
              context.fillRect(0, 0, 1, 1);
              return [...context.getImageData(0, 0, 1, 1).data];
            }""";

    /** Relative luminance and contrast ratio as WCAG defines them. */
    private static final String CONTRAST = """
            (a, b) => {
              const luminance = color => {
                const [r, g, b] = (%s)(color).map(c => c / 255)
                    .map(c => c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
                return 0.2126 * r + 0.7152 * g + 0.0722 * b;
              };
              const [light, dark] = [luminance(a), luminance(b)].sort((x, y) => y - x);
              return (light + 0.05) / (dark + 0.05);
            }""".formatted(RGBA);

    @Override
    protected String getView() {
        return FroalaThemeTestView.ROUTE;
    }

    private void waitForEditors() {
        page.locator("#themed .fr-element").waitFor();
        page.locator("#outlined .fr-element").waitFor();
        page.locator("#no-hover .fr-element").waitFor();
        page.locator("#plain .fr-element").waitFor();
    }

    private void switchToLumoDark() {
        // Froala animates its button backgrounds, and a computed style read during the transition is the old colour
        page.addStyleTag(new Page.AddStyleTagOptions().setContent("* { transition: none !important; }"));
        page.evaluate("() => document.documentElement.setAttribute('theme', 'dark')");
    }

    private String computed(String selector, String property) {
        return (String) page.evaluate("([selector, property]) => getComputedStyle(document.querySelector(selector))"
                + ".getPropertyValue(property)", List.of(selector, property));
    }

    /** A Lumo colour as the browser resolves it, so it compares with a computed style. */
    private String lumo(String property) {
        return (String) page.evaluate("""
                property => {
                  const probe = document.createElement('div');
                  probe.style.color = 'var(' + property + ')';
                  document.body.append(probe);
                  const color = getComputedStyle(probe).color;
                  probe.remove();
                  return color;
                }""", property);
    }

    @Test
    void vaadinTheme_isOnByDefault() {
        // THM-1, THM-3, THM-5
        waitForEditors();

        assertEquals("vaadin", page.evaluate("() => document.querySelector('#themed').editor.opts.theme"));
        assertTrue(page.locator("#themed .fr-box.vaadin-theme").isVisible());
        assertTrue(page.locator("#themed .fr-toolbar.vaadin-theme").isVisible());
    }

    @Test
    void themeNone_leavesFroalaUnthemed() {
        // THM-5
        waitForEditors();

        assertEquals("", page.evaluate("() => document.querySelector('#plain').editor.opts.theme"));
        assertEquals(0, page.locator("#plain .vaadin-theme").count());
    }

    @Test
    void themeProperties_canBeOverridden() {
        // THM-7. The rules read our properties, so overriding one restyles the editor without touching Lumo.
        waitForEditors();

        setProperty("--vcf-froala-background-color", "rgb(1, 2, 3)");
        setProperty("--vcf-froala-border-color", "rgb(4, 5, 6)");
        setProperty("--vcf-froala-field-background", "rgb(16, 17, 18)");
        setProperty("--vcf-froala-field-border-radius", "3px");
        setProperty("--vcf-froala-content-font-size", "21px");
        setProperty("--vcf-froala-clickable-cursor", "crosshair");
        setProperty("--vcf-froala-value-color", "rgb(7, 8, 9)");

        assertEquals("rgb(1, 2, 3)", computed("#outlined .fr-wrapper", "background-color"));
        assertEquals("rgb(4, 5, 6)", computed("#outlined .fr-wrapper", "border-left-color"));
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains("rgb(16, 17, 18)"));
        assertEquals("3px", computed("#themed .fr-box", "border-top-left-radius"));
        assertEquals("21px", computed("#themed .fr-element", "font-size"));
        assertEquals("crosshair", computed("#themed .fr-toolbar button[data-cmd='bold']", "cursor"));
        assertEquals("rgb(7, 8, 9)", computed("#themed .fr-element", "color"));
    }

    @Test
    void default_isFilledLikeAVaadinField() {
        // The fill lies over the background colour, and the border stays but is transparent
        waitForEditors();

        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(lumo("--lumo-contrast-10pct")));
        assertEquals("rgba(0, 0, 0, 0)", computed("#themed .fr-toolbar", "border-top-color"));
        assertEquals("1px", computed("#themed .fr-toolbar", "border-top-width"));
    }

    @Test
    void outlinedVariant_hasFroalasBorder() {
        waitForEditors();

        assertEquals("outlined", page.locator("#outlined").getAttribute("theme"));
        assertEquals(lumo("--lumo-contrast-20pct"), computed("#outlined .fr-toolbar", "border-top-color"));
        assertEquals(lumo("--lumo-base-color"), computed("#outlined .fr-wrapper", "background-color"));
        assertTrue(computed("#outlined .fr-wrapper", "background-image").contains("rgba(0, 0, 0, 0)"));
    }

    @Test
    void outlinedVariant_isOnlyCss() {
        // The variant is only CSS on the theme attribute, so it switches on a running editor without a rebuild
        waitForEditors();
        page.evaluate("() => document.querySelector('#outlined').removeAttribute('theme')");

        assertEquals("rgba(0, 0, 0, 0)", computed("#outlined .fr-toolbar", "border-top-color"));
    }

    @Test
    void filledToolbar_letsTheButtonShadesShowOnTheFill() {
        // Froala's button greys are mixed with the background colour, and would disappear on the fill. Mixed with
        // transparent they lie over it.
        waitForEditors();
        page.addStyleTag(new Page.AddStyleTagOptions().setContent("* { transition: none !important; }"));

        page.locator("#themed .fr-toolbar button[data-cmd='bold']").hover();

        int alpha = ((Number) page.evaluate("color => (" + RGBA + ")(color)[3]",
                computed("#themed .fr-toolbar button[data-cmd='bold']", "background-color"))).intValue();
        assertTrue(alpha > 0 && alpha < 255, "alpha " + alpha);
    }

    @Test
    void focusRing_followsTheBoxCorners() {
        // Vaadin draws the focus ring on the element's input-field part, around Froala's box
        waitForEditors();
        setProperty("--vcf-froala-field-border-radius", "3px");

        assertEquals("3px", inputFieldPart("#themed", "border-top-left-radius"));
        // Froala's own box keeps its 10px without the theme, and so does the ring
        assertEquals("10px", inputFieldPart("#plain", "border-top-left-radius"));
    }

    @Test
    void focusRing_takesTheThemeProperties() {
        // Vaadin's own ring reads --vaadin-focus-ring-*, which the element sets from ours
        waitForEditors();
        setProperty("--vcf-froala-focus-ring-color", "rgb(10, 11, 12)");

        assertEquals("rgb(10, 11, 12)", computed("#themed", "--vaadin-focus-ring-color").trim());
    }

    @Test
    void hover_highlightsTheEditingArea() {
        waitForEditors();

        String before = computed("#themed .fr-wrapper", "background-image");

        page.locator("#themed .fr-element").hover();

        String after = computed("#themed .fr-wrapper", "background-image");
        assertNotEquals(before, after);
        // the highlight lies over the fill, it does not replace it
        assertTrue(after.contains(lumo("--lumo-contrast-10pct")), after);
    }

    private String inputFieldPart(String editor, String property) {
        return (String) page.evaluate(
                "([editor, property]) => getComputedStyle(document.querySelector(editor)"
                        + ".shadowRoot.querySelector('[part=input-field]')).getPropertyValue(property)",
                List.of(editor, property));
    }

    @Test
    void invalid_showsTheErrorColour() {
        waitForEditors();
        page.evaluate("() => ['#themed', '#outlined'].forEach(id => document.querySelector(id).invalid = true)");

        // A Vaadin text field shows the tint instead of its fill, and no border
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(lumo("--lumo-error-color-10pct")));
        assertEquals("rgba(0, 0, 0, 0)", computed("#themed .fr-wrapper", "border-left-color"));
        // The outlined variant keeps its border, in the error colour
        assertEquals(lumo("--lumo-error-color"), computed("#outlined .fr-wrapper", "border-left-color"));
        // but not the line between the editing area and the bottom bar
        assertNotEquals(lumo("--lumo-error-color"), computed("#outlined .fr-wrapper", "border-bottom-color"));
        assertTrue(computed("#outlined .fr-wrapper", "background-image").contains(lumo("--lumo-error-color-10pct")));
    }

    @Test
    void noHoverHighlightVariant_leavesTheEditingAreaAlone() {
        waitForEditors();
        // invalid, because an invalid editor matches both hover rules
        page.evaluate("() => document.querySelector('#no-hover').invalid = true");
        String before = computed("#no-hover .fr-wrapper", "background-image");

        page.locator("#no-hover .fr-element").hover();

        assertEquals(before, computed("#no-hover .fr-wrapper", "background-image"));
    }

    @Test
    void invalidHover_highlightsInTheErrorColour() {
        waitForEditors();
        page.evaluate("() => document.querySelector('#themed').invalid = true");
        String before = computed("#themed .fr-wrapper", "background-image");

        page.locator("#themed .fr-element").hover();

        String after = computed("#themed .fr-wrapper", "background-image");
        assertNotEquals(before, after);
        assertTrue(after.contains(lumo("--lumo-error-color-10pct")), after);
    }

    @Test
    void readonly_showsADashedBorder() {
        waitForEditors();
        page.evaluate("() => ['#themed', '#outlined'].forEach(id => document.querySelector(id).readonly = true)");

        assertEquals("dashed", computed("#themed .fr-box", "outline-style"));
        assertEquals("rgba(0, 0, 0, 0)", computed("#themed .fr-wrapper", "border-left-color"));
        // no fill either
        assertEquals("linear-gradient(rgba(0, 0, 0, 0), rgba(0, 0, 0, 0))",
                computed("#themed .fr-wrapper", "background-image"));
        // the outlined variant loses its border and background colour the same way
        assertEquals("dashed", computed("#outlined .fr-box", "outline-style"));
        assertEquals("rgba(0, 0, 0, 0)", computed("#outlined .fr-wrapper", "border-left-color"));
        assertEquals("rgba(0, 0, 0, 0)", computed("#outlined .fr-wrapper", "background-color"));
    }

    @Test
    void disabled_tintsTheBox() {
        waitForEditors();
        setProperty("--vcf-froala-disabled-value-color", "rgb(13, 14, 15)");
        page.evaluate("() => document.querySelector('#themed').disabled = true");

        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(lumo("--lumo-contrast-5pct")));
        // Vaadin greys the value with -webkit-text-fill-color, which wins over the text colour
        assertEquals("rgb(13, 14, 15)", computed("#themed .fr-element", "-webkit-text-fill-color"));
    }

    private void setProperty(String property, String value) {
        page.evaluate("([property, value]) => document.documentElement.style.setProperty(property, value)",
                List.of(property, value));
    }

    @Test
    void lumoDark_isFollowed() {
        // THM-8. Nothing in the theme knows about dark mode. Lumo's properties change, and the mix follows them.
        waitForEditors();
        switchToLumoDark();

        assertEquals(lumo("--lumo-base-color"), computed("#outlined .fr-wrapper", "background-color"));
        // the default's fill is see-through and lies over the same background colour
        assertEquals(lumo("--lumo-base-color"), computed("#themed .fr-wrapper", "background-color"));
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(lumo("--lumo-contrast-10pct")));
    }

    @Test
    void headings_areLegibleInLumoDark() {
        // THM-12. Lumo turns headings near-white in dark mode, so the background has to turn dark with them.
        waitForEditors();
        switchToLumoDark();

        String heading = computed("#themed .fr-element h1", "color");
        // the fill as it looks over the background colour
        String background = (String) page.evaluate("""
                ([fill, base]) => {
                  const [fr, fg, fb, fa] = (%s)(fill);
                  const [br, bg, bb] = (%s)(base);
                  const a = fa / 255;
                  return `rgb(${fr * a + br * (1 - a)}, ${fg * a + bg * (1 - a)}, ${fb * a + bb * (1 - a)})`;
                }""".formatted(RGBA, RGBA),
                List.of(lumo("--lumo-contrast-10pct"), computed("#themed .fr-wrapper", "background-color")));

        double contrast = ((Number) page.evaluate("([a, b]) => (" + CONTRAST + ")(a, b)", List.of(heading, background)))
                .doubleValue();
        assertTrue(contrast >= 4.5, "heading " + heading + " on " + background + " has contrast " + contrast);
    }

    @Test
    void noLightBackground_isLeftInLumoDark() {
        // THM-10. A rule the theme misses keeps Froala's hard-coded light colour, which is worse in dark mode than no
        // theme at all. Every visible element with the theme class, or inside one, has to paint a dark background.
        // That includes an open dropdown, and the second toolbar row. A see-through light shade, like the default's
        // fill, lies over a dark background and does not count.
        waitForEditors();
        switchToLumoDark();

        page.locator("#themed .fr-toolbar button[data-cmd='moreMisc']").click();
        page.locator("#themed .fr-toolbar button.fr-dropdown >> visible=true").first().click();
        page.locator("#themed .fr-dropdown.fr-active").waitFor();

        @SuppressWarnings("unchecked")
        List<String> light = (List<String>) page.evaluate("""
                () => [...document.querySelectorAll('.vaadin-theme, .vaadin-theme *')]
                  .filter(element => element.checkVisibility({ visibilityProperty: true, opacityProperty: true }))
                  .map(element => [element, getComputedStyle(element).backgroundColor])
                  .filter(([, color]) => {
                    const [r, g, b, a] = (%s)(color);
                    return a >= 128 && (r + g + b) / 3 > 128;
                  })
                  .map(([element, color]) => element.tagName + '.' + element.className + ' ' + color
                      + ' ' + (element.dataset.cmd ?? '') + ' ' + (element.getAttribute('style') ?? ''))"""
                .formatted(RGBA));

        assertTrue(light.isEmpty(), "light backgrounds in dark mode: " + light);
    }
}
