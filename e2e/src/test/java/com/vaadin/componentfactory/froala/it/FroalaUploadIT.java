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

import java.util.Base64;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.FilePayload;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import com.vaadin.componentfactory.froala.it.views.FroalaUploadTestView;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end test for the upload handlers. Whether an upload reaches the handler and its link reaches the document
 * needs Froala's own upload request, which only runs in a browser.
 */
@SpringBootTest(classes = E2eApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class FroalaUploadIT extends SpringPlaywrightIT {

    /** A transparent PNG of one pixel. */
    private static final String PIXEL_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private static final FilePayload PIXEL = new FilePayload("pixel.png", "image/png",
            Base64.getDecoder().decode(PIXEL_BASE64));

    /** The link the test view's handler answers with for the pixel. */
    private static final String PIXEL_LINK = "data:image/png;source=handler;base64," + PIXEL_BASE64;

    @Override
    protected String getView() {
        return FroalaUploadTestView.ROUTE;
    }

    @Test
    void imageUpload_putsTheHandlersLinkIntoTheValue() {
        page.locator("#editor .fr-element[contenteditable='true']").click();
        page.locator("#editor .fr-toolbar [data-cmd='insertImage']").click();

        page.locator(".fr-popup.fr-active .fr-image-upload-layer input[type='file']").setInputFiles(PIXEL);

        assertThat(page.locator("#viewer img")).hasAttribute("src", PIXEL_LINK);
    }

    @Test
    void droppedImage_goesThroughTheHandler() {
        dispatchPixel("drop");

        assertThat(page.locator("#viewer img")).hasAttribute("src", PIXEL_LINK);
    }

    @Test
    void pastedImage_goesThroughTheHandler() {
        dispatchPixel("paste");

        assertThat(page.locator("#viewer img")).hasAttribute("src", PIXEL_LINK);
    }

    /** Drops or pastes the pixel into the editor with handlers, as a file the way a browser hands one over. */
    private void dispatchPixel(String type) {
        page.locator("#editor .fr-element[contenteditable='true']").click();
        page.evaluate(
                """
                        ([type, base64]) => {
                            const bytes = Uint8Array.from(atob(base64), c => c.charCodeAt(0));
                            const data = new DataTransfer();
                            data.items.add(new File([bytes], 'pixel.png', { type: 'image/png' }));
                            const area = document.querySelector('#editor .fr-element');
                            const box = area.getBoundingClientRect();
                            const init = { bubbles: true, cancelable: true };
                            area.dispatchEvent(type === 'paste'
                                    ? new ClipboardEvent('paste', { ...init, clipboardData: data })
                                    : new DragEvent('drop', { ...init, dataTransfer: data, clientX: box.x + 5, clientY: box.y + 5 }));
                        }""",
                new Object[] { type, PIXEL_BASE64 });
    }

    @Test
    void handler_answersWithTheLinkAndRefusesWhileReadOnly() {
        page.locator("#editor .fr-element").waitFor();

        APIResponse accepted = postPixel();
        assertEquals(200, accepted.status());
        assertEquals("{\"link\":\"" + PIXEL_LINK + "\"}", accepted.text());

        page.locator("#read-only").click();
        assertThat(page.locator("#read-only")).isDisabled();

        assertEquals(500, postPixel().status());
    }

    /** Posts the pixel to the image handler's URL as Froala does, a multipart request with the file as {@code file}. */
    private APIResponse postPixel() {
        String url = (String) page.evaluate(
                "() => new URL(document.querySelector('#editor').getAttribute('image-upload-url'), document.baseURI).href");

        return page.request().post(url, RequestOptions.create().setMultipart(FormData.create().set("file", PIXEL)));
    }

    @Test
    void handlers_reachFroalasOptionsAndTakePrecedenceOverTheUrl() {
        page.locator("#editor .fr-element").waitFor();

        for (String kind : new String[] { "image", "file", "video" }) {
            assertEquals(page.locator("#editor").getAttribute(kind + "-upload-url"),
                    option("#editor", kind + "UploadURL"));
            assertEquals(true, option("#editor", kind + "Upload"), kind);
        }
    }

    @Test
    void uploadWithoutHandlerOrUrl_isOff() {
        page.locator("#without-handlers .fr-element").waitFor();

        assertEquals(false, option("#without-handlers", "imageUpload"));
        assertEquals(false, option("#without-handlers", "imagePaste"));
        assertEquals(false, option("#without-handlers", "fileUpload"));
        assertEquals(false, option("#without-handlers", "videoUpload"));
    }

    @Test
    void uploadWithAnOwnUrl_staysOn() {
        page.locator("#with-own-url .fr-element").waitFor();

        assertEquals(FroalaUploadTestView.OWN_IMAGE_UPLOAD_URL, option("#with-own-url", "imageUploadURL"));
        assertEquals(true, option("#with-own-url", "imageUpload"));
        assertEquals(true, option("#with-own-url", "imagePaste"));
    }

    @Test
    void removedHandlers_leaveTheOptionsUrlOrSwitchUploadOff() {
        page.locator("#editor .fr-element").waitFor();

        page.locator("#remove-handlers").click();
        assertThat(page.locator("#remove-handlers")).isDisabled();

        // the rebuild, which the running editor needs to see a handler go
        page.waitForFunction("() => document.querySelector('#editor').editor?.opts?.fileUpload === false");
        assertEquals(false, option("#editor", "videoUpload"));
        // the image handler took precedence over the URL in the options, which is left once it goes
        assertEquals(FroalaUploadTestView.OWN_IMAGE_UPLOAD_URL, option("#editor", "imageUploadURL"));
        assertEquals(true, option("#editor", "imageUpload"));
    }

    private Object option(String editor, String name) {
        return page.evaluate("([editor, name]) => document.querySelector(editor).editor.opts[name]",
                new Object[] { editor, name });
    }
}
