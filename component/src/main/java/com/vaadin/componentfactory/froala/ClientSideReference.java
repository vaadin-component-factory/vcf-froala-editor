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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Interface, that marks a class to have a single string client side representation.
 */
@FunctionalInterface
public interface ClientSideReference {

    /**
     * Returns the client side representation of this instance.
     * 
     * @return client side representation
     */
    String getClientSideRepresentation();

    /**
     * Returns a joined string of the given client side references, separated by a single space.
     * 
     * @param clientSideReferences references to join
     * @return joined string
     */
    static String join(ClientSideReference[] clientSideReferences) {
        return Arrays.stream(clientSideReferences).map(ClientSideReference::getClientSideRepresentation)
                .collect(Collectors.joining(" "));
    }
}
