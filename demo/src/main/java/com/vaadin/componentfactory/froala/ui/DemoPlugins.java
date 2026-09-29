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
package com.vaadin.componentfactory.froala.ui;

import java.util.Set;

import com.vaadin.componentfactory.froala.FroalaPlugin;

/** The plugins the demo shows beyond {@link FroalaPlugin#basics()}. */
final class DemoPlugins {

    private DemoPlugins() {
    }

    /**
     * Every plugin except those that need a server, a second library or a paid service, which this demo does not
     * provide.
     */
    static Set<String> full() {
        Set<String> full = FroalaPlugin.all();
        full.removeAll(Set.of(FroalaPlugin.AI_ASSIST, FroalaPlugin.COLLABORATIVE, FroalaPlugin.EMBEDLY,
                FroalaPlugin.FILESTACK, FroalaPlugin.FONT_AWESOME, FroalaPlugin.IMAGE_FILEROBOT,
                FroalaPlugin.IMAGE_MANAGER, FroalaPlugin.IMAGE_TUI, FroalaPlugin.IMPORT_FROM_WORD, FroalaPlugin.SAVE,
                FroalaPlugin.SPELL_CHECKER, FroalaPlugin.TRIM_VIDEO));

        return full;
    }
}
