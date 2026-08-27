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
 * Implemented by a type whose instances have one fixed string form for the client side, typically an enum whose
 * constants stand for the values a Froala option accepts.
 */
@FunctionalInterface
public interface ClientSideReference {

    /**
     * Returns the string the client side expects for this instance.
     *
     * @return the client side string, never null
     */
    String getClientSideRepresentation();
}
