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
package com.vaadin.componentfactory.froala.it.views;

import java.util.Locale;

import com.vaadin.componentfactory.froala.FroalaEditor;
import com.vaadin.componentfactory.froala.FroalaOptions;
import com.vaadin.componentfactory.froala.FroalaTextDirection;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Fixture view for taking the editor's language from the UI's locale, see {@code FroalaLocaleIT}. The locale is the
 * last path segment as a language tag, such as {@code it/froala-locale/zh-CN}.
 */
@Route(FroalaLocaleTestView.ROUTE)
@AnonymousAllowed
public class FroalaLocaleTestView extends VerticalLayout implements HasUrlParameter<String> {

    public static final String ROUTE = "it/froala-locale";

    public FroalaLocaleTestView() {
        FroalaEditor fromLocale = new FroalaEditor("Language from the locale");
        fromLocale.setId("from-locale");

        FroalaEditor explicit = new FroalaEditor("Explicit language", FroalaOptions.defaults().withLanguage("fr"));
        explicit.setId("explicit");

        // English has no file, so its direction option is not overridden by one the locale picked
        FroalaEditor englishRtl = new FroalaEditor("English, right to left",
                FroalaOptions.defaults().withLanguage("en").withDirection(FroalaTextDirection.RTL));
        englishRtl.setId("english-rtl");

        // the same options again, so only the locale has changed for the build this triggers
        Button toFrench = new Button("To French");
        toFrench.addClickListener(event -> {
            UI.getCurrent().setLocale(Locale.FRENCH);
            fromLocale.setOptions((FroalaOptions) null);
            toFrench.setEnabled(false);
        });
        toFrench.setId("to-french");

        add(fromLocale, explicit, englishRtl, toFrench);
    }

    @Override
    public void setParameter(BeforeEvent event, String languageTag) {
        // set before the view is attached, which is when the editors read the locale
        event.getUI().setLocale(Locale.forLanguageTag(languageTag));
    }
}
