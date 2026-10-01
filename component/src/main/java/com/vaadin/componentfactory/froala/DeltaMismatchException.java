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
 * Thrown by {@link FroalaEditor#applyDelta(String, String)} when a delta cannot be applied to the given value. The
 * usual cause is that the client computed the delta against a different base value, so the two sides no longer agree.
 * {@link FroalaEditor} catches it and asks the client to resend its value.
 */
class DeltaMismatchException extends RuntimeException {

    /**
     * Creates a new instance with the given message.
     *
     * @param message the detail message
     */
    DeltaMismatchException(String message) {
        super(message);
    }
}
