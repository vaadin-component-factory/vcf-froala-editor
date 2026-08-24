import 'froala-editor/css/froala_editor.pkgd.min.css';
import FroalaEditor from 'froala-editor/js/froala_editor.pkgd.min.js';
import {css, html, LitElement} from 'lit';
import {defineCustomElement} from '@vaadin/component-base/src/define.js';
import {ElementMixin} from '@vaadin/component-base/src/element-mixin.js';
import {PolylitMixin} from '@vaadin/component-base/src/polylit-mixin.js';
import {FieldMixin} from '@vaadin/field-base/src/field-mixin.js';
import {FocusMixin} from '@vaadin/a11y-base/src/focus-mixin.js';
import {DisabledMixin} from '@vaadin/a11y-base/src/disabled-mixin.js';
import {ThemableMixin} from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import {inputFieldShared} from '@vaadin/vaadin-lumo-styles/mixins/input-field-shared.js';
import {SlotStylesMixin} from '@vaadin/component-base/src/slot-styles-mixin.js';
// import {ThemeDetectionMixin} from "@vaadin/vaadin-themable-mixin/vaadin-theme-detection-mixin.js";
import {diff_match_patch} from 'diff-match-patch';

class FroalaEditorElement extends SlotStylesMixin(
    FieldMixin(
        ThemableMixin(
            ElementMixin(
                FocusMixin(
                    DisabledMixin(
                        PolylitMixin(LitElement)))))))
{

    // can be overridden by the server using #setConfig
    rawInitialConfig = {};

    // will be overridden by the server on attachment time
    initialConfig = {};

    // set by the server before the editor initializes; passed to Froala as its `key` option
    licenseKey = null;

    // Froala builds asynchronously, so its modules (edit, html, ...) must not be touched before its `initialized`
    // event has fired -- `this.editor` being assigned is not enough
    _editorInitialized = false;

    _lastSyncedValue = "";
    _lastSyncedValueTimestamp = 0;
    _valueChangeMode = "change";

    _valueChangeTimeout = 2_000;

    static properties = {
        // `disabled` comes from DisabledMixin, which also keeps aria-disabled in sync -- do not redeclare it here.
        // `focused` is an attribute that FocusMixin toggles directly; declaring it as a reflected property would let
        // Lit overwrite what the mixin just set.
        readonly: {
            type: Boolean,
            value: false,
            reflectToAttribute: true
        }
    }

    static get styles() {
        return [inputFieldShared, css`
            :host {
                display: flex;
                flex-direction: column;
                box-sizing: border-box;
            }

            .vcf-froala-editor-container {
                align-self: stretch;
                flex: 1;

                display: flex;
                flex-direction: column;
                row-gap: 0.5rem;
                overflow: hidden;
            }

            [part="input-field"] {
                flex: 1;
                display: flex;
                flex-direction: column;
                overflow: hidden;
                margin: 2px;
                border-radius: var(--vcf-froala-editor-input-field-border-radius, 10px); /* taken from froala toolbar border radius, update if necessary*/
            }
        `];
    }

    /** @protected */
    render() {
        return html`
            <div class="vcf-froala-editor-container vaadin-field-container">
                <div part="label" @click="${this.focus}">
                    <slot name="label"></slot>
                    <span part="required-indicator" aria-hidden="true"></span>
                </div>

                <div part="input-field">
                    <slot></slot>
                </div>

                <div part="helper-text">
                    <slot name="helper"></slot>
                </div>

                <div part="error-message">
                    <slot name="error-message"></slot>
                </div>
            </div>

            <slot name="tooltip"></slot>
        `;
    }

    ready() {
        super.ready();

        // TODO Phase 2: tooltip support. Froala has its own Tooltip and Popups modules, so decide there whether the
        // host exposes a single Vaadin tooltip or delegates to Froala's. The commented wiring below is the Vaadin half.
        // this._tooltipController = new TooltipController(this);
        // this.addController(this._tooltipController);
        // this._tooltipController.setShouldShow(target => {
        //     // const inputs = target.inputs || [];
        //     // return !inputs.some((el) => el.opened);
        //     return true;
        // });
    }

    async firstUpdated(changedProperties) {
        // PolylitMixin builds its `this.$` id map here, so the super call is not optional
        super.firstUpdated(changedProperties);

        await this._initEditor();
    }

    async connectedCallback() {
        super.connectedCallback();

        // `hasUpdated` is Lit's own flag: false until the element has rendered once. On a first connect the editor is
        // left to firstUpdated, so that properties the server sets in the same response (licenseKey, value) are applied
        // before Froala reads them -- the license key in particular is only read once, at init.
        //
        // A re-connect of an already rendered element means a client side DOM move: Lit does not run firstUpdated a
        // second time, so nothing else would rebuild the editor. A Flow detach and re-attach does not land here, it
        // discards the element and builds a new one.
        if (this.hasUpdated) {
            await this._initEditor();
        }
    }

    disconnectedCallback() {
        // FocusMixin, ControllerMixin and Vaadin's ResizeMixin all call super first and tear down afterwards
        super.disconnectedCallback();

        // these outlive the element otherwise: an interval keeps firing against a destroyed editor, and a re-attach
        // starts a second one on top of it
        clearInterval(this._valueChangeHandleForInterval);
        clearTimeout(this._valueChangeHandleForTimeout);
        clearTimeout(this._throttleHandle);
        delete this._valueChangeHandleForInterval;
        delete this._valueChangeHandleForTimeout;
        delete this._throttleHandle;

        if (this.editor) {
            // the clean up has to happen even if destroy throws: a leftover `this.editor` would make _initEditor skip
            // the rebuild and the field would stay dead
            try {
                this.editor.destroy();
            } finally {
                this.editorElement.remove();

                delete this.editorElement;
                delete this.editor;
            }
        }

        this._editorInitialized = false;
    }

    async _initEditor() {
        // `isConnected` is the DOM's own flag. Lit does not check it before running firstUpdated, and Flow can attach
        // and detach an element before that first update flushes -- the editor would then belong to a host that
        // already had its one and only disconnectedCallback, and nothing would ever destroy it.
        if (!this.isConnected) {
            return;
        }

        if (!this.editor) {
            this.editorElement = document.createElement('div');

            // Froala adopts the content of the element it initializes on, so seeding it here is what applies the
            // server's initial value -- there is no init option for the content.
            this.editorElement.innerHTML = this._lastSyncedValue;
            this.append(this.editorElement); // will be put into the default slot

            this.editor = new FroalaEditor(this.editorElement, {
                key: this.licenseKey ?? undefined,
                events: {
                    'initialized': () => {
                        this._editorInitialized = true;

                        // anything touching editor modules has to wait for this event, so re-apply what the server may
                        // already have set while Froala was still building
                        this.updateReadonlyMode();

                        if (this.valueChangeMode === "interval") {
                            this.startValueChangeInterval();
                        }
                    },
                    'blur': () => {
                        this.onValueChangeIfMode("blur");
                        this.dispatchEvent(new CustomEvent('blur'));
                    },
                    'focus': () => {
                        this.dispatchEvent(new CustomEvent('focus'));
                    },
                    'contentChanged': () => {
                        this.onValueChangeIfMode("change");
                        this.restartValueChangeTimeoutIfMode();
                    }
                }
            });
        }
    }

    set value(value) {
        this._lastSyncedValue = value ?? "";

        if (this.editor) {
            this.editor.html.set(this._lastSyncedValue);
        }
    }

    get value() {
        return this.editor?.html?.get() ?? this._lastSyncedValue;
    }

    /**
     * Calls #onValueChange(), if the given string matches the current value change mode.
     * @param expectedValueChangeMode expected value change mode
     */
    onValueChangeIfMode(expectedValueChangeMode) {
        if (this.valueChangeMode === expectedValueChangeMode) {
            this.onValueChange();
        }
    }

    /**
     * This method is to be called when ever a value change should be triggered. It will calculate the current value
     * delta, update the "old value" property and send an event to the server.
     */
    onValueChange() {
        const now = Date.now();
        const sinceLastSync = now - this._lastSyncedValueTimestamp;

        // Throttle, so a burst of changes does not become a burst of round trips. It defers rather than drops: a blur
        // or a timeout flush landing inside the window has no later change to carry it.
        if (sinceLastSync < 50) {
            clearTimeout(this._throttleHandle);
            this._throttleHandle = setTimeout(() => this.onValueChange(), 50 - sinceLastSync);
            return;
        }

        clearTimeout(this._throttleHandle);
        this._lastSyncedValueTimestamp = now;

        const currentValue = this.editor?.html?.get() ?? this._lastSyncedValue;

        // init lib
        const dmp = new diff_match_patch();
        const patch = dmp.patch_make(this._lastSyncedValue, currentValue);
        const delta = dmp.patch_toText(patch);

        this._lastSyncedValue = currentValue;

        if (delta) {
            this.dispatchEvent(new CustomEvent("_value-delta", {
                detail: {
                    delta
                }
            }));
        }
    }

    /**
     * Sends the full current value to the server, bypassing the delta channel. The server calls this when a delta did
     * not apply, which means the two sides drifted apart and only a full value can bring them back together.
     */
    resyncValue() {
        clearTimeout(this._throttleHandle);
        this._lastSyncedValue = this.editor?.html?.get() ?? this._lastSyncedValue;
        this._lastSyncedValueTimestamp = Date.now();

        this.dispatchEvent(new CustomEvent("_value-resync", {
            detail: {
                value: this._lastSyncedValue
            }
        }));
    }

    set valueChangeMode(newValueChangeMode) {
        if (!newValueChangeMode) {
            newValueChangeMode = "change";
        }
        if (this._valueChangeMode !== newValueChangeMode) {
            this._valueChangeMode = newValueChangeMode;

            if (this._valueChangeMode !== "timeout" && this._valueChangeHandleForTimeout) {
                this.stopValueChangeTimeout();
            }

            if (this._valueChangeMode === "interval") {
                this.startValueChangeInterval();
            } else if (this._valueChangeHandleForInterval) {
                this.stopValueChangeInterval();
            }

        }
    }

    get valueChangeMode() {
        return this._valueChangeMode;
    }

    set valueChangeTimeout(newTimeout) {
        if (!newTimeout || newTimeout < 0) {
            throw new Error("valueChangeTimeout must be greater than 0");
        }

        if (this._valueChangeTimeout !== newTimeout) {
            this._valueChangeTimeout = newTimeout;

            if (this._valueChangeHandleForTimeout) {
                this.stopValueChangeTimeout();
            } else if (this._valueChangeHandleForInterval) {
                this.startValueChangeInterval(); // also stops the current interval
            }

        }
    }

    get valueChangeTimeout() {
        return this._valueChangeTimeout;
    }

    stopValueChangeTimeout() {
        this.onValueChange(); // flush value to server
        clearTimeout(this._valueChangeHandleForTimeout);
        delete this._valueChangeHandleForTimeout;
    }

    stopValueChangeInterval() {
        this.onValueChange(); // flush value to server
        window.clearInterval(this._valueChangeHandleForInterval);
        delete this._valueChangeHandleForInterval;
    }

    /**
     * Restarts the debounce timer, if the current mode is "timeout". Any change before it elapses restarts it, so the
     * value is synced once the user pauses.
     */
    restartValueChangeTimeoutIfMode() {
        if (this.valueChangeMode !== "timeout") {
            return;
        }

        clearTimeout(this._valueChangeHandleForTimeout);
        this._valueChangeHandleForTimeout = setTimeout(() => {
            delete this._valueChangeHandleForTimeout;
            this.onValueChange();
        }, this.valueChangeTimeout);
    }

    startValueChangeInterval() {
        if (this._valueChangeHandleForInterval) {
            this.stopValueChangeInterval();
        }

        this._valueChangeHandleForInterval = setInterval(this.onValueChange.bind(this), this.valueChangeTimeout);
    }

    /**
     * Replaces the current selection with the given html snippet. If nothing
     * is selected, the content will be added at the caret's position
     * @param html
     */
    replaceSelectionContent(html) {
        // TODO Phase 2: `this.editor.html.insert(html, clean, doSplit)` is the Froala equivalent -- it inserts at the
        // selection, replacing it when there is one. Verified in froala-editor 5.4.0 index.d.ts:2256; Froala's
        // FroalaSelection has no setContent, that was TinyMCE API from the HugeRTE reference. Deferred because the
        // clean/doSplit flags are configuration decisions that belong with the option API.
        console.warn("replaceSelectionContent is not implemented yet")
        this.onValueChange();
    }

    focus() {
        super.focus();
        this.editor?.events.focus();
    }

    updated(changedProperties) {
        super.updated(changedProperties);
        if (changedProperties.has("disabled") || changedProperties.has("readonly")) {
            this.updateReadonlyMode();
        }
    }

    /**
     * Applies `disabled` / `readonly` to the editor. Froala has no mode API -- `edit.off()` drops the contenteditable
     * attribute and disables the toolbar, `edit.on()` restores both.
     */
    updateReadonlyMode() {
        if (!this._editorInitialized) {
            // re-applied from the `initialized` handler, so a component that starts out disabled is not lost
            return;
        }

        if (this.disabled || this.readonly) {
            this.editor.edit.off();
        } else {
            this.editor.edit.on();
        }
    }

    static get is() {
        return 'vcf-froala-editor';
    }

}

defineCustomElement(FroalaEditorElement);

export {FroalaEditorElement};