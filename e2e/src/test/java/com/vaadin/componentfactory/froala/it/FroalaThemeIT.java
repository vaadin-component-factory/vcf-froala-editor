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
import java.util.Map;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaThemeTestView;

import static java.util.Map.entry;
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

    /** Each private copy and the property it takes its value from under Lumo (THM-13). */
    private static final Map<String, String> LUMO_SOURCES = Map.ofEntries(
            entry("--_vcf-froala-background-color", "--vaadin-background-color"),
            entry("--_vcf-froala-neutral-color", "--lumo-contrast"),
            entry("--_vcf-froala-accent-color", "--lumo-primary-color"),
            entry("--_vcf-froala-accent-contrast-color", "--lumo-primary-contrast-color"),
            entry("--_vcf-froala-error-color", "--lumo-error-color"),
            entry("--_vcf-froala-success-color", "--lumo-success-color"),
            entry("--_vcf-froala-warning-color", "--lumo-warning-color"),
            entry("--_vcf-froala-border-color", "--lumo-contrast-20pct"),
            entry("--_vcf-froala-text-color", "--vaadin-text-color"),
            entry("--_vcf-froala-text-color-secondary", "--vaadin-text-color-secondary"),
            entry("--_vcf-froala-text-color-tertiary", "--lumo-tertiary-text-color"),
            entry("--_vcf-froala-text-color-disabled", "--vaadin-text-color-disabled"),
            entry("--_vcf-froala-value-color", "--vaadin-text-color"),
            entry("--_vcf-froala-disabled-value-color", "--vaadin-text-color-disabled"),
            entry("--_vcf-froala-placeholder-color", "--vaadin-text-color-secondary"),
            entry("--_vcf-froala-font-family", "--lumo-font-family"),
            entry("--_vcf-froala-content-font-size", "--lumo-font-size-m"),
            entry("--_vcf-froala-content-line-height", "--lumo-line-height-m"),
            entry("--_vcf-froala-font-size-xxs", "--lumo-font-size-xxs"),
            entry("--_vcf-froala-font-size-xs", "--lumo-font-size-xs"),
            entry("--_vcf-froala-font-size-s", "--lumo-font-size-s"),
            entry("--_vcf-froala-font-size-m", "--lumo-font-size-m"),
            entry("--_vcf-froala-field-border-radius", "--lumo-border-radius-m"),
            entry("--_vcf-froala-radius-s", "--lumo-border-radius-s"),
            entry("--_vcf-froala-radius-m", "--lumo-border-radius-m"),
            entry("--_vcf-froala-radius-l", "--lumo-border-radius-l"),
            entry("--_vcf-froala-shadow-xs", "--lumo-box-shadow-xs"),
            entry("--_vcf-froala-shadow-s", "--lumo-box-shadow-s"),
            entry("--_vcf-froala-shadow-m", "--lumo-box-shadow-m"),
            entry("--_vcf-froala-shadow-l", "--lumo-box-shadow-l"),
            entry("--_vcf-froala-focus-ring-color", "--lumo-primary-color-50pct"),
            entry("--_vcf-froala-clickable-cursor", "--lumo-clickable-cursor"),
            entry("--_vcf-froala-field-background", "--vaadin-background-container-strong"),
            entry("--_vcf-froala-hover-highlight", "--lumo-contrast-50pct"),
            entry("--_vcf-froala-invalid-border-color", "--lumo-error-color"),
            entry("--_vcf-froala-invalid-background", "--lumo-error-color-10pct"),
            entry("--_vcf-froala-invalid-hover-highlight", "--lumo-error-color-50pct"),
            entry("--_vcf-froala-disabled-background", "--vaadin-background-container"));

    /** Each private copy and the property it takes its value from under Aura (THM-13). */
    private static final Map<String, String> AURA_SOURCES = Map.ofEntries(
            entry("--_vcf-froala-background-color", "--vaadin-background-color"),
            entry("--_vcf-froala-neutral-color", "--vaadin-text-color"),
            entry("--_vcf-froala-accent-color", "--aura-accent-color"),
            entry("--_vcf-froala-accent-contrast-color", "--aura-accent-contrast-color"),
            entry("--_vcf-froala-error-color", "--aura-red"), entry("--_vcf-froala-success-color", "--aura-green"),
            entry("--_vcf-froala-warning-color", "--aura-orange"),
            entry("--_vcf-froala-border-color", "--vaadin-border-color"),
            entry("--_vcf-froala-text-color", "--vaadin-text-color"),
            entry("--_vcf-froala-text-color-secondary", "--vaadin-text-color-secondary"),
            entry("--_vcf-froala-text-color-tertiary", "--vaadin-text-color-secondary"),
            entry("--_vcf-froala-text-color-disabled", "--vaadin-text-color-disabled"),
            entry("--_vcf-froala-value-color", "--vaadin-text-color"),
            entry("--_vcf-froala-disabled-value-color", "--vaadin-text-color-disabled"),
            entry("--_vcf-froala-placeholder-color", "--vaadin-text-color-secondary"),
            entry("--_vcf-froala-font-family", "--aura-font-family"),
            entry("--_vcf-froala-content-font-size", "--aura-font-size-m"),
            entry("--_vcf-froala-content-line-height", "--aura-line-height-m"),
            entry("--_vcf-froala-font-size-xxs", "--aura-font-size-xs"),
            entry("--_vcf-froala-font-size-xs", "--aura-font-size-xs"),
            entry("--_vcf-froala-font-size-s", "--aura-font-size-s"),
            entry("--_vcf-froala-font-size-m", "--aura-font-size-m"),
            entry("--_vcf-froala-field-border-radius", "--vaadin-radius-m"),
            entry("--_vcf-froala-radius-s", "--vaadin-radius-s"), entry("--_vcf-froala-radius-m", "--vaadin-radius-m"),
            entry("--_vcf-froala-radius-l", "--vaadin-radius-l"), entry("--_vcf-froala-shadow-xs", "--aura-shadow-xs"),
            entry("--_vcf-froala-shadow-s", "--aura-shadow-s"), entry("--_vcf-froala-shadow-m", "--aura-shadow-m"),
            entry("--_vcf-froala-shadow-l", "--aura-shadow-m"),
            entry("--_vcf-froala-focus-ring-color", "--vaadin-focus-ring-color"),
            entry("--_vcf-froala-focus-ring-width", "--vaadin-focus-ring-width"),
            entry("--_vcf-froala-clickable-cursor", "--vaadin-clickable-cursor"),
            entry("--_vcf-froala-field-background", "--vaadin-background-container-strong"),
            entry("--_vcf-froala-hover-highlight", "--vaadin-text-color"),
            entry("--_vcf-froala-invalid-border-color", "--aura-red"),
            entry("--_vcf-froala-disabled-background", "--vaadin-background-container"));

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
        switchToLumoDark("document.documentElement");
    }

    private void switchToLumoDark(String element) {
        disableTransitions();
        page.evaluate("() => " + element + ".setAttribute('theme', 'dark')");
    }

    /** Froala animates its button backgrounds, and a computed style read during the transition is the old colour. */
    private void disableTransitions() {
        page.addStyleTag(new Page.AddStyleTagOptions().setContent("* { transition: none !important; }"));
    }

    private String computed(String selector, String property) {
        return (String) page.evaluate("([selector, property]) => getComputedStyle(document.querySelector(selector))"
                + ".getPropertyValue(property)", List.of(selector, property));
    }

    /** A theme colour as the browser resolves it, so it compares with a computed style. */
    private String color(String property) {
        return resolved("color", property);
    }

    /** A theme property as the browser resolves it in the given CSS property, so it compares with a computed style. */
    private String resolved(String cssProperty, String property) {
        return (String) page.evaluate("""
                ([cssProperty, property]) => {
                  const probe = document.createElement('div');
                  probe.style.setProperty(cssProperty, 'var(' + property + ')');
                  document.body.append(probe);
                  const value = getComputedStyle(probe).getPropertyValue(cssProperty);
                  probe.remove();
                  return value;
                }""", List.of(cssProperty, property));
    }

    private void switchToAura() {
        page.navigate(page.url().replace(FroalaThemeTestView.ROUTE, FroalaThemeTestView.AURA_ROUTE));
        waitForEditors();
    }

    @Test
    void vaadinTheme_isOnByDefault() {
        // THM-1, THM-3, THM-5
        waitForEditors();

        assertEquals("froala-vaadin", page.evaluate("() => document.querySelector('#themed').editor.opts.theme"));
        assertTrue(page.locator("#themed .fr-box.froala-vaadin-theme").isVisible());
        assertTrue(page.locator("#themed .fr-toolbar.froala-vaadin-theme").isVisible());
    }

    @Test
    void themeNone_leavesFroalaUnthemed() {
        // THM-5
        waitForEditors();

        assertEquals("", page.evaluate("() => document.querySelector('#plain').editor.opts.theme"));
        assertEquals(0, page.locator("#plain .froala-vaadin-theme").count());
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

        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(color("--lumo-contrast-10pct")));
        assertEquals("rgba(0, 0, 0, 0)", computed("#themed .fr-toolbar", "border-top-color"));
        assertEquals("1px", computed("#themed .fr-toolbar", "border-top-width"));
    }

    @Test
    void outlinedVariant_hasFroalasBorder() {
        waitForEditors();

        assertEquals("outlined", page.locator("#outlined").getAttribute("theme"));
        assertEquals(color("--lumo-contrast-20pct"), computed("#outlined .fr-toolbar", "border-top-color"));
        assertEquals(color("--lumo-base-color"), computed("#outlined .fr-wrapper", "background-color"));
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
        disableTransitions();

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
        // The field styles draw the ring on the input-field part, in the colour the element takes from ours
        waitForEditors();
        setProperty("--vcf-froala-focus-ring-color", "rgb(10, 11, 12)");
        setProperty("--vcf-froala-focus-ring-width", "3px");

        assertEquals("rgb(10, 11, 12) 0px 0px 0px 3px", focusRing("#themed"));
    }

    /** The ring the field styles draw while the element shows its keyboard focus. */
    private String focusRing(String editor) {
        page.evaluate("editor => document.querySelector(editor).setAttribute('focus-ring', '')", editor);
        return inputFieldPart(editor, "box-shadow");
    }

    @Test
    void hover_highlightsTheEditingArea() {
        waitForEditors();

        String before = computed("#themed .fr-wrapper", "background-image");

        page.locator("#themed .fr-element").hover();

        String after = computed("#themed .fr-wrapper", "background-image");
        assertNotEquals(before, after);
        // the highlight lies over the fill, it does not replace it
        assertTrue(after.contains(color("--lumo-contrast-10pct")), after);
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
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(color("--lumo-error-color-10pct")));
        assertEquals("rgba(0, 0, 0, 0)", computed("#themed .fr-wrapper", "border-left-color"));
        // The outlined variant keeps its border, in the error colour
        assertEquals(color("--lumo-error-color"), computed("#outlined .fr-wrapper", "border-left-color"));
        // but not the line between the editing area and the bottom bar
        assertNotEquals(color("--lumo-error-color"), computed("#outlined .fr-wrapper", "border-bottom-color"));
        assertTrue(computed("#outlined .fr-wrapper", "background-image").contains(color("--lumo-error-color-10pct")));
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
        assertTrue(after.contains(color("--lumo-error-color-10pct")), after);
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

        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(color("--lumo-contrast-5pct")));
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

        assertEquals(color("--lumo-base-color"), computed("#outlined .fr-wrapper", "background-color"));
        // the default's fill is see-through and lies over the same background colour
        assertEquals(color("--lumo-base-color"), computed("#themed .fr-wrapper", "background-color"));
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(color("--lumo-contrast-10pct")));
    }

    @Test
    void lumoDark_onTheUi_isFollowed() {
        // Vaadin switches the variant at runtime through the UI's theme list, which sets it on the body, not on html.
        // A property resolved on html would keep the light colours there.
        waitForEditors();
        switchToLumoDark("document.body");

        assertEquals(color("--lumo-base-color"), computed("#outlined .fr-wrapper", "background-color"));
        assertTrue(computed("#themed .fr-wrapper", "background-image").contains(color("--lumo-contrast-10pct")));
    }

    @Test
    void themeProperties_onHtml_winInLumoDarkOnTheUi() {
        // An override on html still wins below an element that carries the dark variant
        waitForEditors();
        switchToLumoDark("document.body");
        setProperty("--vcf-froala-background-color", "rgb(1, 2, 3)");

        assertEquals("rgb(1, 2, 3)", computed("#outlined .fr-wrapper", "background-color"));
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
                List.of(color("--lumo-contrast-10pct"), computed("#themed .fr-wrapper", "background-color")));

        double contrast = ((Number) page.evaluate("([a, b]) => (" + CONTRAST + ")(a, b)", List.of(heading, background)))
                .doubleValue();
        assertTrue(contrast >= 4.5, "heading " + heading + " on " + background + " has contrast " + contrast);
    }

    @Test
    void noLightBackground_isLeftInLumoDark() {
        // THM-10. A rule the theme misses keeps Froala's hard-coded light colour, which is worse in dark mode than no
        // theme at all.
        waitForEditors();
        switchToLumoDark();

        assertNoLightBackground();
    }

    /**
     * Every visible element with the theme class, or inside one, has to paint a dark background. That includes an open
     * dropdown, and the second toolbar row. A see-through light shade, like the default's fill, lies over a dark
     * background and does not count.
     */
    private void assertNoLightBackground() {
        page.locator("#themed .fr-toolbar button[data-cmd='moreMisc']").click();
        page.locator("#themed .fr-toolbar button.fr-dropdown >> visible=true").first().click();
        page.locator("#themed .fr-dropdown.fr-active").waitFor();

        @SuppressWarnings("unchecked")
        List<String> light = (List<String>) page.evaluate("""
                () => [...document.querySelectorAll('.froala-vaadin-theme, .froala-vaadin-theme *')]
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

    @Test
    void lumo_feedsEveryThemeProperty() {
        waitForEditors();

        assertSources(LUMO_SOURCES);
    }

    @Test
    void aura_feedsEveryThemeProperty() {
        switchToAura();

        assertSources(AURA_SOURCES);
    }

    /** Read on the editor, where the copies are resolved. */
    private void assertSources(Map<String, String> sources) {
        sources.forEach((copy, property) -> {
            String source = computed("#themed", property).trim();
            assertNotEquals("", source, property + " is not set");
            assertEquals(source, computed("#themed", copy).trim(), copy);
        });
    }

    @Test
    void aura_reachesTheRules() {
        switchToAura();

        assertEquals(color("--vaadin-background-color"), computed("#outlined .fr-wrapper", "background-color"));
        assertEquals(color("--vaadin-border-color"), computed("#outlined .fr-toolbar", "border-top-color"));
        assertTrue(computed("#themed .fr-wrapper", "background-image")
                .contains(color("--vaadin-background-container-strong")));
        assertEquals(resolved("border-top-left-radius", "--vaadin-radius-m"),
                computed("#themed .fr-box", "border-top-left-radius"));
        assertEquals(resolved("font-family", "--aura-font-family"), computed("#themed .fr-element", "font-family"));
        // Aura lightens the input-field part, which the toolbar inside it must not pick up
        assertEquals(computed("#themed .fr-wrapper", "background-color"),
                computed("#themed .fr-toolbar", "background-color"));
    }

    @Test
    void aura_focusRing_takesAurasColour() {
        switchToAura();

        assertEquals(color("--vaadin-focus-ring-color") + " 0px 0px 0px "
                + resolved("margin-top", "--vaadin-focus-ring-width"), focusRing("#themed"));
    }

    @Test
    void aura_darkScheme_isFollowed() {
        // Aura switches with the color-scheme property, and its colours follow through light-dark()
        switchToAura();
        disableTransitions();
        page.evaluate("() => document.documentElement.style.colorScheme = 'dark'");

        assertEquals(color("--vaadin-background-color"), computed("#outlined .fr-wrapper", "background-color"));
        assertNoLightBackground();
    }
}
