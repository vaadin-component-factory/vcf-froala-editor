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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;

/**
 * Displays HTML written in a {@link FroalaEditor}, outside the editor. It imports Froala's stylesheet and carries
 * Froala's {@code fr-view} class, so the content is rendered as it was during editing. Froala documents the same
 * approach for <a href=
 * "https://froala.com/wysiwyg-editor/docs/overview/install-froala-commonjs/#displaying-content-outside-of-the-froala-editor">displaying
 * content outside the editor</a>.
 *
 * <p>
 * The viewer also carries the class {@code vaadin-theme}, so the content rules of {@link FroalaTheme#VAADIN}, such as
 * the colours of tracked changes, apply as they do in the editor. An application whose editors use another Froala theme
 * removes it with {@code removeClassName("vaadin-theme")}. The base text, i.e. font, colour, size, weight and line
 * height, comes from the page, which under Lumo gives the same values as the editor by default. Overriding the
 * {@code --vcf-froala-*} or input field properties changes the editor only.
 */
@Tag("vcf-froala-viewer")
@NpmPackage(value = "froala-editor", version = "5.4.0")
@CssImport("froala-editor/css/froala_editor.pkgd.min.css")
@CssImport("./vcf-froala-editor/vcf-froala-editor.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin.css")
@CssImport("./vcf-froala-editor/vcf-froala-theme-vaadin-rules.css")
public class FroalaViewer extends Component implements HasSize, HasStyle {

    /**
     * The element property the click listener reads the patterns from, as regular expressions. A property is sent again
     * when the component is attached anew, so the patterns survive a detach.
     */
    private static final String ROUTER_IGNORE_PROPERTY = "vcfRouterIgnorePatterns";

    /**
     * Vaadin's router takes a click on a link inside the application as navigation to a route. It leaves a link with
     * {@code router-ignore} alone, so a clicked link whose path matches gets it here, before the router's listener on
     * the window sees the click. The path is taken relative to {@code document.baseURI}, the application's root, as the
     * router itself does. A link outside it is no concern of the router's.
     */
    private static final String ROUTER_IGNORE_LISTENER = """
            if (!this.vcfRouterIgnoreListener) {
                this.vcfRouterIgnoreListener = true;
                this.addEventListener('click', e => {
                    const patterns = this.%s;
                    const link = e.target.closest('a');
                    if (!patterns || !link || !link.href || !this.contains(link)) {
                        return;
                    }
                    const url = new URL(link.href);
                    const address = url.origin + url.pathname;
                    if (!address.startsWith(document.baseURI)) {
                        return;
                    }
                    const path = '/' + address.slice(document.baseURI.length);
                    if (patterns.some(pattern => new RegExp(pattern).test(path))) {
                        link.setAttribute('router-ignore', '');
                    }
                });
            }""".formatted(ROUTER_IGNORE_PROPERTY);

    private Registration routerIgnore;

    /**
     * Creates a new instance and adds Froala's {@code fr-view} class and the {@code vaadin-theme} class to it.
     * Replacing the class list with {@link HasStyle#setClassName(String)} removes both, and the content then loses
     * Froala's styling.
     */
    public FroalaViewer() {
        addClassNames("fr-view", "vaadin-theme");
    }

    /**
     * Sets the HTML to display.
     *
     * <p>
     * The given string is written to the element's {@code innerHTML} unchanged. It is not escaped or sanitized here,
     * and a value taken from {@link FroalaEditor} was not sanitized on the server either. Pass trusted HTML, or
     * sanitize it before calling this method.
     *
     * @param html the HTML to display
     */
    public void setContent(String html) {
        getElement().setProperty("innerHTML", html);
    }

    /**
     * Returns the HTML given to {@link #setContent(String)}.
     *
     * @return the HTML, or null if none was set
     */
    public String getContent() {
        return getElement().getProperty("innerHTML");
    }

    /**
     * Sets the paths whose links in the content open with a page load instead of Vaadin's router, see
     * {@link #applyRouterIgnore(Component, Collection)} for the patterns. The router takes a link inside the
     * application as a route, so a link to an uploaded file shows "Couldn't find route" without this. The paths replace
     * the ones set before. With no paths, every link goes to the router again.
     *
     * <pre>
     * viewer.setRouterIgnorePaths("/froala-upload");
     * </pre>
     *
     * @param paths the paths relative to the application's root, where a {@code *} matches any characters
     */
    public void setRouterIgnorePaths(String... paths) {
        setRouterIgnorePaths(Arrays.asList(paths));
    }

    /**
     * Sets the paths whose links in the content open with a page load instead of Vaadin's router, see
     * {@link #setRouterIgnorePaths(String...)}.
     *
     * @param paths the paths relative to the application's root, where a {@code *} matches any characters
     */
    public void setRouterIgnorePaths(Collection<String> paths) {
        if (routerIgnore != null) {
            routerIgnore.remove();
            routerIgnore = null;
        }

        if (!paths.isEmpty()) {
            routerIgnore = applyRouterIgnore(this, paths);
        }
    }

    /**
     * Lets the links inside the given component open with a page load instead of Vaadin's router when their path
     * matches one of the patterns, see {@link #applyRouterIgnore(Component, Collection)}. For HTML displayed without a
     * {@link FroalaViewer}, like in an {@code Html} component or a {@code Div}.
     *
     * @param component the component whose links are concerned
     * @param paths the paths relative to the application's root, where a {@code *} matches any characters
     * @return a registration that leaves the links to the router again
     */
    public static Registration applyRouterIgnore(Component component, String... paths) {
        return applyRouterIgnore(component, Arrays.asList(paths));
    }

    /**
     * Lets the links inside the given component open with a page load instead of Vaadin's router when their path
     * matches one of the patterns. The router takes a click on a link inside the application as navigation to a route,
     * which a link to an uploaded file or another resource of the application is not. A link that matches gets the
     * attribute {@code router-ignore} when it is clicked, and every other link stays with the router.
     *
     * <p>
     * A path is matched relative to the application's root, so {@code /froala-upload} matches
     * {@code https://example.com/app/froala-upload/42} when the application runs under {@code /app}. A {@code *}
     * matches any characters, slashes included. A pattern without one covers everything below the path, so
     * {@code /froala-upload} stands for {@code /froala-upload/*}:
     *
     * <pre>
     * FroalaViewer.applyRouterIgnore(div, "/froala-upload", "/reports/*.pdf");
     * </pre>
     *
     * <p>
     * Calling it again for the same component replaces the patterns.
     *
     * @param component the component whose links are concerned
     * @param paths the paths relative to the application's root, where a {@code *} matches any characters
     * @return a registration that leaves the links to the router again
     */
    public static Registration applyRouterIgnore(Component component, Collection<String> paths) {
        var element = component.getElement();
        element.setPropertyList(ROUTER_IGNORE_PROPERTY, paths.stream().map(FroalaViewer::toRegex).toList());
        Serializable patterns = element.getPropertyRaw(ROUTER_IGNORE_PROPERTY);

        // Runs on every attach, because a component attached anew can get a new element in the browser.
        Registration attach = component.addAttachListener(event -> element.executeJs(ROUTER_IGNORE_LISTENER));
        if (component.isAttached()) {
            element.executeJs(ROUTER_IGNORE_LISTENER);
        }

        return () -> {
            attach.remove();
            // Only its own patterns, since a later call for the same component replaced them with its own.
            if (element.getPropertyRaw(ROUTER_IGNORE_PROPERTY) == patterns) {
                element.removeProperty(ROUTER_IGNORE_PROPERTY);
            }
        };
    }

    /**
     * Turns a path pattern into a regular expression that JavaScript and Java read alike. A backslash before a
     * character other than a letter or a digit makes it literal in both.
     */
    static String toRegex(String path) {
        String pattern = path.trim();

        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("A router-ignore path must not be blank");
        }

        if (!pattern.startsWith("/")) {
            pattern = "/" + pattern;
        }

        if (!pattern.contains("*")) {
            pattern = (pattern.endsWith("/") ? pattern : pattern + "/") + "*";
        }

        var regex = new StringBuilder("^");
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                regex.append(".*");
            } else if (Character.isLetterOrDigit(c)) {
                regex.append(c);
            } else {
                regex.append('\\').append(c);
            }
        }

        return regex.append('$').toString();
    }
}
