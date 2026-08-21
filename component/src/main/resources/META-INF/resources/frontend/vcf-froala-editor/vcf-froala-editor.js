import 'froala-editor/css/froala_editor.pkgd.min.css';
import FroalaEditor from 'froala-editor/js/froala_editor.pkgd.min.js';
import {css, html, LitElement} from 'lit';
import {defineCustomElement} from '@vaadin/component-base/src/define.js';
import {ElementMixin} from '@vaadin/component-base/src/element-mixin.js';
import {PolylitMixin} from '@vaadin/component-base/src/polylit-mixin.js';
import {FieldMixin} from '@vaadin/field-base/src/field-mixin.js';
import {FocusMixin} from '@vaadin/a11y-base/src/focus-mixin.js';
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
                    PolylitMixin(LitElement))))))
    // FocusMixin is explicitly not used, as it would mess up the focused attribute
{

    // can be overridden by the server using #setConfig
    rawInitialConfig = {};

    // will be overridden by the server on attachment time
    initialConfig = {};

    _lastSyncedValue = "";
    _lastSyncedValueTimestamp = 0;
    _valueChangeMode = "change";

    _valueChangeTimeout = 2_000;

    static properties = {
        disabled: {
            type: Boolean,
            reflectToAttribute: true
        },
        readonly: {
            type: Boolean,
            reflectToAttribute: true
        },
        focused: {
            type: Boolean,
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

        // implement tooltip support later - might make sense to allow more distinct tooltip handling
        // than just one tooltip, for instance for buttons and so on, especially, since hugerte brings some
        // own tooltips
        // this._tooltipController = new TooltipController(this);
        // this.addController(this._tooltipController);
        // this._tooltipController.setShouldShow(target => {
        //     // const inputs = target.inputs || [];
        //     // return !inputs.some((el) => el.opened);
        //     return true;
        // });
    }

    firstUpdated() {
    }

    async connectedCallback() {
        super.connectedCallback();

        // not sure if this might be an issue, if called here already, since "render" has not yet happend
        // if so, add a check for "has been rendered" for reattachments and also add a call of init editor to
        // first update.
        await this._initEditor();
    }

    disconnectedCallback() {
        if (this.editor) {
            this.editor.destroy();
            this.editorElement.remove();

            delete this.editorElement;
            delete this.editor;
        }

        if (this.resizeObserver) {
            this.resizeObserver.disconnect();
            delete this.resizeObserver;
        }

        super.disconnectedCallback();
    }

    async _initEditor() {
        if (!this.editor) {
            this.editorElement = document.createElement('div');
            this.append(this.editorElement); // will be put into the default slot

            // TODO init the editor
            // TODO init initial value assignment
            // TODO init timeout and interval value change modes
            this.editor = new FroalaEditor(this.editorElement, {
                events: {
                    'blur': () => {
                        this.onValueChangeIfMode("blur");
                        this.dispatchEvent(new CustomEvent('blur'));
                    },
                    'focus': () => {
                        this.dispatchEvent(new CustomEvent('focus'));
                    },
                    'contentChanged': () => {
                        this.onValueChangeIfMode("change");
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
        let now = Date.now();
        if (this._lastSyncedValueTimestamp < now - 50) { // explicit throttle to prevent too many events fired at all
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
        console.warn("on selection replace")
        // this.editor.selection.setContent(html);
        this.onValueChange();
    }

    focus() {
        super.focus();
        this.editor.events.focus();
    }

    // TBD: necessary for froala?
    // setEnabled(enabled) {
    //     // Debounce is needed if mode is attempted to be changed more than once
    //     // during the attach
    //     if (this.readonlyTimeout) {
    //         clearTimeout(this.readonlyTimeout);
    //     }
    //
    //     this.readonlyTimeout = setTimeout(() => {
    //         this.editor.mode.set(enabled ? 'design' : 'readonly');
    //     }, 20);
    // }

    // TBD: necessary for froala?
    // isInDialog() {
    //     let inDialog = false;
    //     let parent = this.parentElement;
    //     while (parent != null) {
    //         if (parent.tagName.indexOf("VAADIN-DIALOG") === 0) {
    //             inDialog = true;
    //             break;
    //         }
    //         parent = parent.parentElement;
    //     }
    //
    //     return inDialog;
    // }

    updated(changedProperties) {
        super.updated(changedProperties);
        if (changedProperties.has("disabled") || changedProperties.has("readonly")) {
            this.updateReadonlyMode();
        }
    }

    updateReadonlyMode() {
        console.warn("tbd update readonly / disabled")
        // this.editor?.mode.set((this.disabled || this.readonly) ? 'readonly' : 'design');
    }

    static get is() {
        return 'vcf-froala-editor';
    }

}

defineCustomElement(FroalaEditorElement);

export {FroalaEditorElement};