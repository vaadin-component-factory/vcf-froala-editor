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

/**
 * The names of the buttons {@link FroalaPlugin#QUICK_INSERT} offers next to an empty line, for
 * {@link FroalaOptions#withQuickInsertButtons(java.util.Collection)}. These are not command names like those of
 * {@link FroalaButton}. The quick insert plugin keeps a registry of its own, and froala-editor 5.4.0 registers these
 * seven. A button whose plugin is not enabled is left out.
 *
 * <p>
 * The constants are plain strings, so a button an application registers with Froala's {@code RegisterQuickInsertButton}
 * goes into the same list as a plain string.
 */
public final class FroalaQuickInsertButton {

    /** Inserts an image. Needs {@link FroalaPlugin#IMAGE}. */
    public static final String IMAGE = "image";

    /** Inserts a video. Needs {@link FroalaPlugin#VIDEO}. */
    public static final String VIDEO = "video";

    /** Embeds a URL. Needs {@link FroalaPlugin#EMBEDLY}. */
    public static final String EMBEDLY = "embedly";

    /** Inserts a table. Needs {@link FroalaPlugin#TABLE}. */
    public static final String TABLE = "table";

    /** Starts an unordered list. Needs {@link FroalaPlugin#LISTS}. */
    public static final String UL = "ul";

    /** Starts an ordered list. Needs {@link FroalaPlugin#LISTS}. */
    public static final String OL = "ol";

    /** Inserts a horizontal line. Needs no plugin besides quick insert itself. */
    public static final String HR = "hr";

    private FroalaQuickInsertButton() {
    }
}
