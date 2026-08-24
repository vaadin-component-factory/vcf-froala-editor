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
 * Thrown by {@link FroalaEditor#applyDelta(String, String)} when a delta cannot be applied to the value it is handed.
 *
 * <p>
 * Usually that means the client built the delta against a different base and the two sides have drifted apart. It also
 * covers diff-match-patch answering with something other than its documented result shape. Both are answered the same
 * way -- the client is asked to resend its full value -- which is why they share one exception.
 */
public class DeltaMismatchException extends RuntimeException {

    /**
     * Creates a new instance with the given message.
     *
     * @param message message
     */
    public DeltaMismatchException(String message) {
        super(message);
    }
}
