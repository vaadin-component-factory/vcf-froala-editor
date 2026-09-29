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

import java.util.EnumSet;

import com.vaadin.componentfactory.froala.FroalaPlugin;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/** The basics view with every plugin that works without a service behind it. */
@Route("full")
@Menu(title = "Full", order = 0.5)
public class FullView extends BasicView {

    @Override
    protected EnumSet<FroalaPlugin> plugins() {
        return DemoPlugins.full();
    }

    @Override
    protected String description() {
        return "Every Froala plugin except those that need a server, a second library or a paid service, such as the AI "
                + "assistant, collaboration or the image manager. No upload URL is set, so a file or an image picked "
                + "from disk does not reach a server.";
    }
}
