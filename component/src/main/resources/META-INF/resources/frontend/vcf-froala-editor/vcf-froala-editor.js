import { FroalaEditor, loadFroalaFiles } from './froala-loader.js';
import { css, html, LitElement } from 'lit';
import { defineCustomElement } from '@vaadin/component-base/src/define.js';
import { ElementMixin } from '@vaadin/component-base/src/element-mixin.js';
import { PolylitMixin } from '@vaadin/component-base/src/polylit-mixin.js';
import { FieldMixin } from '@vaadin/field-base/src/field-mixin.js';
import { FocusMixin } from '@vaadin/a11y-base/src/focus-mixin.js';
import { DisabledMixin } from '@vaadin/a11y-base/src/disabled-mixin.js';
import { ThemableMixin } from '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import { inputFieldShared } from '@vaadin/vaadin-lumo-styles/mixins/input-field-shared.js';
import { SlotStylesMixin } from '@vaadin/component-base/src/slot-styles-mixin.js';
import { diff_match_patch } from 'diff-match-patch';

class FroalaEditorElement extends SlotStylesMixin(
  FieldMixin(ThemableMixin(ElementMixin(FocusMixin(DisabledMixin(PolylitMixin(LitElement))))))
) {
  // Set by the server before the editor initializes, and passed to Froala as its `key` option.
  licenseKey = null;

  // The options the current editor was built with, as JSON. Compared against `options` in updated(), so that the
  // update which builds the editor does not rebuild it right away.
  _appliedOptions = null;

  // Froala builds asynchronously, so its modules (edit, html, ...) must not be touched before its `initialized`
  // event has fired. `this.editor` being assigned is not enough.
  _editorInitialized = false;

  // Counts the editors this element has built. Froala's event handlers are bound to the element rather than to the
  // instance that registered them. So a handler of an editor we destroyed mid-build still runs, and would run
  // against its successor. Every handler carries the count it was registered under and stays quiet once it differs.
  _editorGeneration = 0;

  _lastSyncedValue = '';
  _lastSyncedValueTimestamp = 0;
  _valueChangeMode = 'change';

  // Froala's own typing debounce, its `typingTimer` option. Not one of our timers, see #valueChangeTimeout.
  // Null until the server sets one, so that a `typingTimer` coming in through the options is not overwritten by a
  // default nobody asked for.
  _valueChangeTimeout = null;

  _intervalPeriod = 2_000;

  // replaceSelectionContent calls that arrived before the editor was initialized, applied from its `initialized` event
  _pendingInserts = [];

  // a focus() that arrived before the editor was initialized, applied from its `initialized` event
  _pendingFocus = false;

  // what the server was last told about the selection, so that only a switch between "none" and "some" is reported
  _hasSelection = false;

  static properties = {
    // `disabled` comes from DisabledMixin, which also keeps aria-disabled in sync. Do not redeclare it here.
    // `focused` is an attribute that FocusMixin toggles directly. Declaring it as a reflected property would let
    // Lit overwrite what the mixin just set.
    readonly: {
      type: Boolean,
      value: false,
      reflectToAttribute: true,
    },

    // Froala's own options, as the server sent them. A declared property rather than a plain field, because Lit
    // rescues a value that was assigned before the element upgraded. A bare setter on the prototype would be
    // shadowed by it.
    options: {
      type: Object,
    },
  };

  static get styles() {
    return [
      inputFieldShared,
      css`
        :host {
          display: flex;
          flex-direction: column;
          box-sizing: border-box;

          /* Vaadin's focus ring and disabled value colour, taken from the theme's properties */
          --vaadin-focus-ring-color: var(--vcf-froala-focus-ring-color);
          --vaadin-focus-ring-width: var(--vcf-froala-focus-ring-width);
          --vaadin-input-field-disabled-value-color: var(--vcf-froala-disabled-value-color);
        }

        .vcf-froala-editor-container {
          align-self: stretch;
          flex: 1;

          display: flex;
          flex-direction: column;
          row-gap: 0.5rem;
          /* Keeps Froala within the field's height, but not its width, because Froala puts its quick-insert button
             left of its box when there is room. Not hidden, because next to hidden a visible turns into auto and clips.
             Unlike hidden, clip does not make a scroll container, so min-height has to be 0 for the flex item to
             shrink below its content. */
          overflow-x: visible;
          overflow-y: clip;
          min-height: 0;
        }

        [part='input-field'] {
          flex: 1;
          display: flex;
          flex-direction: column;
          /* see .vcf-froala-editor-container */
          overflow-x: visible;
          overflow-y: clip;
          min-height: 0;
          /* room for the focus ring, which the container would clip otherwise. Lumo draws the ring with this width. */
          margin: var(--_focus-ring-width, 2px);
          /* the corners of Froala's box, so the focus ring follows them */
          border-radius: var(--_vcf-froala-box-radius, 10px);
        }
      `,
    ];
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
    `;
  }

  async firstUpdated(changedProperties) {
    // PolylitMixin builds its `this.$` id map here, so the super call is not optional
    super.firstUpdated(changedProperties);

    await this._initEditor();
  }

  async connectedCallback() {
    super.connectedCallback();

    // `hasUpdated` is Lit's own flag, false until the element has rendered once. On a first connect the editor is
    // left to firstUpdated, so that properties the server sets in the same response (licenseKey, value) are applied
    // before Froala reads them. The license key in particular is only read when the editor is built.
    //
    // A re-connect of an already rendered element means a client side DOM move. Lit does not run firstUpdated a
    // second time, so nothing else would rebuild the editor. A Flow detach and re-attach does not land here, because
    // Flow discards the element and builds a new one.
    if (this.hasUpdated) {
      await this._initEditor();
    }
  }

  disconnectedCallback() {
    // FocusMixin, ControllerMixin and Vaadin's ResizeMixin all call super first and tear down afterwards
    super.disconnectedCallback();

    this._destroyEditor();
  }

  _destroyEditor() {
    // These outlive the element otherwise. An interval keeps firing against a destroyed editor, and a re-attach
    // starts a second one on top of it.
    clearInterval(this._valueChangeHandleForInterval);
    clearTimeout(this._throttleHandle);
    delete this._valueChangeHandleForInterval;
    delete this._throttleHandle;

    if (this.editor) {
      // The clean up has to happen even if destroy throws. A leftover `this.editor` would make _initEditor skip
      // the rebuild and the field would stay dead.
      try {
        this.editor.destroy();
      } finally {
        this.editorElement.remove();

        delete this.editorElement;
        delete this.editor;
      }
    }

    this._editorInitialized = false;
    this._editorGeneration++;
    this._appliedOptions = null;
  }

  /**
   * Applies a changed set of options by throwing the editor away and building a new one. Froala has no API to change
   * an option on a running instance. Its own answer is to destroy and initialize again.
   *
   * What the user typed is flushed first and seeds the new editor. Everything else Froala holds is gone (caret,
   * selection, scroll position, undo history). A detach and re-attach makes the same trade.
   */
  _rebuildEditor() {
    this.onValueChange();
    this._destroyEditor();
    this._initEditor();
  }

  async _initEditor() {
    // `isConnected` is the DOM's own flag. Lit does not check it before running firstUpdated, and Flow can attach
    // and detach an element before that first update flushes. The editor would then belong to a host that
    // already had its one and only disconnectedCallback, and nothing would ever destroy it.
    if (!this.isConnected || this.editor) {
      return;
    }

    // Counted up front, because loading the files is asynchronous. A destroy or another build in the meantime counts
    // again, and this build then gives up. See _editorGeneration.
    const generation = ++this._editorGeneration;
    const serverOptions = this.options;
    const pluginsEnabled = await loadFroalaFiles(serverOptions);

    if (generation !== this._editorGeneration || !this.isConnected) {
      return;
    }

    if (this.options !== serverOptions) {
      // the server sent other options while the files were loading, and those may need other files
      await this._initEditor();
      return;
    }

    this.editorElement = document.createElement('div');

    // Froala adopts the content of the element it initializes on, so seeding it here is what applies the
    // server's initial value. There is no init option for the content.
    this.editorElement.innerHTML = this._lastSyncedValue;
    this.append(this.editorElement); // will be put into the default slot

    // The server's options first, ours on top. Where this add-on owns a setter for something Froala also has as
    // an option, the setter wins. Assigned rather than spread so that an option the server did set is not
    // overwritten with an undefined we do not have.
    //
    // Underneath the server's options sit two defaults of ours. The vaadin theme is on unless they pick another.
    // The save plugin is off unless they ask for it. The value reaches the server through the value change
    // listener, and without a saveURL the plugin only runs a failing save after every edit.
    //
    // pluginsEnabled always comes from loadFroalaFiles, which names the plugins explicitly.
    const options = { saveInterval: 0, theme: 'vaadin', ...this.options, pluginsEnabled };
    this._appliedOptions = JSON.stringify(this.options ?? null);

    if (this.licenseKey) {
      options.key = this.licenseKey;
    }
    if (this._valueChangeTimeout !== null) {
      options.typingTimer = this._valueChangeTimeout;
    }

    // Froala builds asynchronously, so an options change can arrive while the editor being replaced is still
    // bootstrapping. See _editorGeneration.
    const fromCurrentEditor =
      (handler) =>
      (...args) => {
        if (generation === this._editorGeneration) {
          handler(...args);
        }
      };

    this.editor = new FroalaEditor(this.editorElement, {
      ...options,
      events: {
        initialized: fromCurrentEditor(() => {
          this._editorInitialized = true;

          // anything touching editor modules has to wait for this event, so re-apply what the server may
          // already have set while Froala was still building
          this.updateReadonlyMode();

          if (this.valueChangeMode === 'interval') {
            this.startValueChangeInterval();
          }

          this._pendingInserts.splice(0).forEach((html) => this.replaceSelectionContent(html));

          if (this._pendingFocus) {
            this._pendingFocus = false;
            this.editor.events.focus();
          }

          // Froala has no selection event of its own. The document's selectionchange catches every way a selection
          // is made (mouse, Shift+arrows, select all), and registered through Froala it goes away with the editor.
          this.editor.events.$on(
            this.editor.$doc,
            'selectionchange',
            fromCurrentEditor(() => this._reportSelection())
          );

          // After an options rebuild the server may still believe in the old editor's selection, because its
          // listener was gone before its content was removed. A re-attach is a new element, and the server handles
          // that one itself.
          this._reportSelection();
        }),
        blur: fromCurrentEditor(() => {
          // Flush in every mode, not just ON_BLUR. The click that moved focus can detach the component, which
          // clears pending timers and would take the last edit with it. An empty delta sends nothing.
          this.onValueChange();
          this.dispatchEvent(new CustomEvent('blur'));
        }),
        focus: fromCurrentEditor(() => {
          this.dispatchEvent(new CustomEvent('focus'));
        }),
        contentChanged: fromCurrentEditor(() => {
          if (this.valueChangeMode === 'change') {
            this.onValueChangeThrottled();
          }
        }),
      },
    });
  }

  _reportSelection() {
    const hasSelection = this.editor.selection.inEditor() && !this.editor.selection.isCollapsed();
    if (hasSelection !== this._hasSelection) {
      this._hasSelection = hasSelection;
      this.dispatchEvent(new CustomEvent('selection-change', { detail: { hasSelection } }));
    }
  }

  set value(value) {
    this._lastSyncedValue = value ?? '';

    if (this.editor) {
      this.editor.html.set(this._lastSyncedValue);
    }
  }

  get value() {
    return this.editor?.html?.get() ?? this._lastSyncedValue;
  }

  /**
   * Rate limit for ON_CHANGE. Typing rarely reaches this rate, because Froala debounces typing itself
   * (`typingTimer`). What this catches are the paths that bypass that debounce and fire contentChanged at once. A
   * toolbar command fires it twice in a row, and so do paste, cut and undo/redo. Defers rather than drops, because the
   * deferred call is the only one left to carry that change.
   *
   * Only this mode needs it. INTERVAL limits its own rate already, and a flush (from a blur, a mode switch or an
   * elapsed timer) must never be held back. That is why the throttle lives here and not in #onValueChange().
   */
  onValueChangeThrottled() {
    const sinceLastSync = Date.now() - this._lastSyncedValueTimestamp;

    if (sinceLastSync < 50) {
      clearTimeout(this._throttleHandle);
      this._throttleHandle = setTimeout(() => this.onValueChange(), 50 - sinceLastSync);
      return;
    }

    this.onValueChange();
  }

  /**
   * Sends whatever the editor holds now. It calculates the delta against the last synced value, updates it and
   * dispatches the event. Sends nothing if the delta is empty. Always immediate, because rate limiting is the
   * caller's business.
   */
  onValueChange() {
    clearTimeout(this._throttleHandle);
    this._lastSyncedValueTimestamp = Date.now();

    const currentValue = this.editor?.html?.get() ?? this._lastSyncedValue;

    const dmp = new diff_match_patch();
    const patch = dmp.patch_make(this._lastSyncedValue, currentValue);
    const delta = dmp.patch_toText(patch);

    this._lastSyncedValue = currentValue;

    if (delta) {
      this.dispatchEvent(
        new CustomEvent('_value-delta', {
          detail: {
            delta,
          },
        })
      );
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

    this.dispatchEvent(
      new CustomEvent('_value-resync', {
        detail: {
          value: this._lastSyncedValue,
        },
      })
    );
  }

  set valueChangeMode(newValueChangeMode) {
    if (!newValueChangeMode) {
      newValueChangeMode = 'change';
    }
    if (this._valueChangeMode !== newValueChangeMode) {
      this._valueChangeMode = newValueChangeMode;

      if (this._valueChangeMode === 'interval') {
        this.startValueChangeInterval();
      } else if (this._valueChangeHandleForInterval) {
        this.stopValueChangeInterval();
      }
    }
  }

  get valueChangeMode() {
    return this._valueChangeMode;
  }

  /**
   * The idle time before Froala reports a change, in milliseconds. This is Froala's own `typingTimer` option, not a
   * timer of ours. Froala restarts it on every keystroke and only fires contentChanged once it elapses, so this is what
   * decides how long after the last keypress ON_CHANGE syncs.
   *
   * Froala does not change the option, but reports changes after at least 250 ms whatever it says. A smaller value
   * would silently have no effect, so it is rejected here instead. It is
   * read on every keystroke, which is why setting it takes effect on a running editor.
   */
  set valueChangeTimeout(newTimeout) {
    if (!newTimeout || newTimeout < 250) {
      throw new Error('valueChangeTimeout must be at least 250 ms, the minimum Froala uses');
    }

    this._valueChangeTimeout = newTimeout;

    if (this.editor) {
      this.editor.opts.typingTimer = newTimeout;
    }
  }

  get valueChangeTimeout() {
    return this._valueChangeTimeout ?? this.editor?.opts?.typingTimer ?? 500;
  }

  /** The time between two syncs in INTERVAL mode, in milliseconds. */
  set intervalPeriod(newPeriod) {
    if (!newPeriod || newPeriod < 0) {
      throw new Error('intervalPeriod must be greater than 0');
    }

    if (this._intervalPeriod !== newPeriod) {
      this._intervalPeriod = newPeriod;

      if (this._valueChangeHandleForInterval) {
        this.startValueChangeInterval(); // also stops the current interval
      }
    }
  }

  get intervalPeriod() {
    return this._intervalPeriod;
  }

  stopValueChangeInterval() {
    this.onValueChange();
    window.clearInterval(this._valueChangeHandleForInterval);
    delete this._valueChangeHandleForInterval;
  }

  startValueChangeInterval() {
    if (this._valueChangeHandleForInterval) {
      this.stopValueChangeInterval();
    }

    this._valueChangeHandleForInterval = setInterval(this.onValueChange.bind(this), this.intervalPeriod);
  }

  /**
   * Inserts an HTML snippet at the caret, replacing the selection if there is one. Maps onto Froala's
   * `html.insert`. Froala cleans the snippet first. Its second argument is named `clean` but means "already clean,
   * skip cleaning", so it is left out on purpose. So is `doSplit`, which only matters for inline snippets inside a
   * block.
   *
   * A call that arrives while Froala is still building, typically in the same round trip as the attach, is held back
   * until the editor is initialized. The editor's modules do not exist before that.
   */
  replaceSelectionContent(html) {
    if (!this._editorInitialized) {
      this._pendingInserts.push(html);
      return;
    }

    this.editor.html.insert(html);

    // Reported at once rather than left to the value change mode, because the change came from the server.
    this.onValueChange();
  }

  /**
   * Focuses the editing area. A call that arrives while the editor is still loading its files or building, typically
   * in the same round trip as the attach, is held back until the editor is initialized.
   */
  focus() {
    super.focus();

    if (this._editorInitialized) {
      this.editor.events.focus();
    } else {
      this._pendingFocus = true;
    }
  }

  updated(changedProperties) {
    super.updated(changedProperties);

    if (changedProperties.has('disabled') || changedProperties.has('readonly')) {
      this.updateReadonlyMode();
    }

    // Compared by content, not by `changedProperties.has`. On the update that builds the editor the options are
    // already in it, and a rebuild would be for nothing.
    if (this.editor && JSON.stringify(this.options ?? null) !== this._appliedOptions) {
      this._rebuildEditor();
    }
  }

  /**
   * Applies `disabled` / `readonly` to the editor. Froala has no mode API. `edit.off()` drops the contenteditable
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

export { FroalaEditorElement };
