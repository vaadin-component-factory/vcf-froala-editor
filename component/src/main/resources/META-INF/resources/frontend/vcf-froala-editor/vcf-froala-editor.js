import { FroalaEditor, firstLanguageWithFile, loadFroalaFiles } from './froala-loader.js';
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

const DIFF_MATCH_PATCH = new diff_match_patch();

// The shortest time between two syncs in ON_CHANGE, in milliseconds, see _onValueChangeThrottled
const THROTTLE_MS = 50;

// Froala draws an icon from a template string, filling in each [NAME] from the icon's definition. An own command's
// icon is a <vaadin-icon> with the attributes the server sent, already escaped by registerCommands.
FroalaEditor.DefineIconTemplate('vcfVaadinIcon', '<vaadin-icon [ATTRS]></vaadin-icon>');

// Froala puts titles and icons into its HTML unescaped. Brackets included, because the icon template would read
// them as a placeholder.
function escapeHtml(text) {
  return String(text).replace(/[&<>"'[\]]/g, (character) => `&#${character.charCodeAt(0)};`);
}

// The plugin name an own command is tied to. Froala shows a command's button only in editors whose pluginsEnabled
// holds the command's plugin, which is how a command registered for the whole page appears only in the editors that
// added it. Froala skips a plugin name it has no plugin for, so the name needs nothing behind it.
function commandPlugin(name) {
  return `vcfCommand_${name}`;
}

/**
 * Registers the given own commands with Froala. Froala keeps commands, icons and shortcuts for the whole page, so the
 * editor built last decides a name's title, icon, shortcut and toggle in all editors.
 */
function registerCommands(commands) {
  for (const { name, title, icon, shortcut, toggle } of commands) {
    // The names go into Froala's HTML unescaped, so only plain attribute names pass
    const attributes = Object.entries(icon)
      .filter(([attribute]) => /^[a-z][a-z0-9-]*$/.test(attribute))
      .map(([attribute, value]) => `${attribute}="${escapeHtml(value)}"`)
      .join(' ');
    FroalaEditor.DefineIcon(name, { template: 'vcfVaadinIcon', ATTRS: attributes });

    FroalaEditor.RegisterCommand(name, {
      title: escapeHtml(title),
      icon: name,
      plugin: commandPlugin(name),
      // The command changes nothing in the editor itself. What the server does afterwards arrives as a change of its
      // own.
      undo: false,
      refreshAfterCallback: false,
      // Froala clears fr-active and aria-pressed before it calls refresh, with the editor as `this`. That is how two
      // editors show their own state of a command registered for the whole page. forcedRefresh keeps the state while
      // the editor has no focus, where Froala would clear the button without calling refresh.
      toggle,
      forcedRefresh: toggle,
      refresh(button) {
        if (toggle && this.el?.closest('vcf-froala-editor')?._isCommandActive(name)) {
          button.addClass('fr-active').attr('aria-pressed', true);
        }
      },
      callback() {
        // Froala calls this with the editor as `this` from a button and from commands.exec. A shortcut goes through
        // the toolbar's button if there is one. Without one Froala calls this without an editor, which is why the
        // shortcut event below goes through commands.exec instead.
        this?.el?.closest('vcf-froala-editor')?._runCommand(name);
      },
    });

    // Froala's key for a shortcut in SHORTCUTS_MAP, see RegisterShortcut
    const key = shortcut && `${shortcut.shift ? '^' : ''}${shortcut.alt ? '@' : ''}${shortcut.keyCode}`;
    // keys an earlier definition of the name bound, so that the last definition is the only one
    Object.keys(FroalaEditor.SHORTCUTS_MAP)
      .filter((other) => other !== key && FroalaEditor.SHORTCUTS_MAP[other].cmd === name)
      .forEach((other) => delete FroalaEditor.SHORTCUTS_MAP[other]);
    // RegisterShortcut adds the name to the default shortcutsEnabled on every call, so only when something changes
    if (shortcut && FroalaEditor.SHORTCUTS_MAP[key]?.cmd !== name) {
      FroalaEditor.RegisterShortcut(shortcut.keyCode, name, null, shortcut.letter, shortcut.shift, shortcut.alt);
    }
  }
}

// the kinds of file Froala uploads, each with its own options such as imageUploadURL and imageUpload
const UPLOAD_KINDS = ['image', 'file', 'video'];

// The popup button lists a plugin edits in place, see _initEditor. The files manager edits imageInsertButtons.
const INSERT_BUTTON_LISTS = ['imageInsertButtons', 'videoInsertButtons', 'fileInsertButtons'];

class FroalaEditorElement extends SlotStylesMixin(
  FieldMixin(ThemableMixin(ElementMixin(FocusMixin(DisabledMixin(PolylitMixin(LitElement))))))
) {
  // Set by the server before the editor initializes, and passed to Froala as its `key` option.
  licenseKey = null;

  // The options and commands the current editor was built with, as JSON. Compared against the current ones in
  // updated(), so that the update which builds the editor does not rebuild it right away.
  _appliedConfig = null;

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

  // The defaults and the 250 ms minimum below mirror the constants in FroalaEditor.java, keep them in step
  _intervalPeriod = 2_000;

  // replaceSelectionContent calls that arrived before the editor was initialized, applied from its `initialized` event
  _pendingInserts = [];

  // a focus() that arrived before the editor was initialized, applied from its `initialized` event
  _pendingFocus = false;

  // a selectAll() that arrived before the editor was initialized, applied from its `initialized` event
  _pendingSelectAll = false;

  // the popovers of the own commands, by command name, see _setCommandPopover
  _commandPopovers = {};

  // what the server was last told about the selection, so that only a switch between "none" and "some" is reported
  _hasSelection = false;

  // a tabindex the server set, moved from the host to the editable area, see attributeChangedCallback
  _tabIndex = null;

  // whether the host's `dir` is one this element set from the editor's direction, and the `dir` it had before, which
  // may be null. See _mirrorDirection.
  _dirFromEditor = false;
  _dirBeforeEditor = null;

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

    // The plugins for options without pluginsEnabled, FroalaPlugin.basics() on the server. Declared for the same
    // reason as `options`.
    defaultPluginsEnabled: {
      type: Array,
    },

    // The language file names that fit the UI's locale, best first, for options without a language. Declared for the
    // same reason as `options`.
    localeLanguages: {
      type: Array,
    },

    // The application's own commands, as FroalaCommand sends them. Declared for the same reason as `options`.
    commands: {
      type: Array,
    },

    // The names of the toggle commands whose buttons show as pressed. Declared for the same reason as `options`, and
    // not part of _config(), because the buttons show a change without a rebuild.
    activeCommands: {
      type: Array,
    },

    // The URLs of the upload handlers the server set, from the attributes image-upload-url, file-upload-url and
    // video-upload-url, which Flow fills in when it registers a handler. Declared for the same reason as `options`.
    imageUploadUrl: {
      type: String,
    },
    fileUploadUrl: {
      type: String,
    },
    videoUploadUrl: {
      type: String,
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
          --vaadin-focus-ring-color: var(--_vcf-froala-focus-ring-color);
          --vaadin-focus-ring-width: var(--_vcf-froala-focus-ring-width);
          --vaadin-input-field-disabled-value-color: var(--_vcf-froala-disabled-value-color);
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

    // A move in the DOM disconnects and connects again, and the editor is rebuilt from the last synced value. What was
    // typed since then has to be sent first, like before a rebuild.
    this._onValueChange();
    this._destroyEditor();
  }

  _destroyEditor() {
    // These outlive the element otherwise. An interval keeps firing against a destroyed editor, and a re-attach
    // starts a second one on top of it.
    clearInterval(this._valueChangeHandleForInterval);
    clearTimeout(this._throttleHandle);
    delete this._valueChangeHandleForInterval;
    delete this._throttleHandle;

    // Vaadin's overlay closes an open popover once its target has no size, which a removed button has. Without a
    // target it stays where it is, until the next build points it at the new button.
    Object.values(this._commandPopovers).forEach((popover) => {
      popover.target = null;
    });

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
    this._appliedConfig = null;
  }

  /**
   * Puts the direction the editor was built with onto the host, so the label, helper text and error message sit on the
   * same side as the text. Read from Froala rather than from the options, because a language file such as `ar` brings
   * its own direction, which Froala writes into `opts.direction` while it builds.
   *
   * Froala's default `auto` names no direction. Then the host gets back the `dir` it had before this element set one,
   * a `dir` the application set or none at all. With none, DirMixin goes back to the document's direction. A `dir`
   * equal to the document's is taken to be DirMixin's copy, not the application's, so it is not kept as stale.
   *
   * ponytail: two cases are left out. While the editor's direction equals the document's, the host stays subscribed
   * to DirMixin, so a later change of the document's direction reaches it. And a `dir` the server changes on a running
   * editor is only looked at on the next build.
   */
  _mirrorDirection() {
    const direction = this.editor.opts.direction;
    if (direction === 'rtl' || direction === 'ltr') {
      if (!this._dirFromEditor) {
        const dir = this.getAttribute('dir');
        this._dirBeforeEditor = dir === document.documentElement.getAttribute('dir') ? null : dir;
        this._dirFromEditor = true;
      }
      this.setAttribute('dir', direction);
    } else if (this._dirFromEditor) {
      if (this._dirBeforeEditor === null) {
        this.removeAttribute('dir');
      } else {
        this.setAttribute('dir', this._dirBeforeEditor);
      }
      this._dirFromEditor = false;
    }
  }

  /**
   * Applies changed options or commands by throwing the editor away and building a new one. Froala has no API to
   * change an option on a running instance, and builds its toolbar and popups only once. Its own answer is to destroy
   * and initialize again.
   *
   * What the user typed is flushed first and seeds the new editor. Everything else Froala holds is gone (caret,
   * selection, scroll position, undo history). A detach and re-attach makes the same trade.
   */
  _rebuildEditor() {
    this._onValueChange();
    try {
      this._destroyEditor();
    } finally {
      // _destroyEditor cleans up even if Froala's destroy throws, and the field must not stay dead after that
      this._initEditor();
    }
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
    const pluginsEnabled = serverOptions?.pluginsEnabled ?? this.defaultPluginsEnabled ?? [];
    // Without a language in the options the editor takes the UI's locale, which the server sent as the file names
    // that fit it, best first. Its `en` has no file and leaves Froala in English.
    const language = serverOptions?.language ?? firstLanguageWithFile(this.localeLanguages);
    await loadFroalaFiles(pluginsEnabled, language);

    if (generation !== this._editorGeneration || !this.isConnected) {
      return;
    }

    if (this.options !== serverOptions) {
      // the server sent other options while the files were loading, and those may need other files
      await this._initEditor();
      return;
    }

    this.editorElement = document.createElement('div');
    this.append(this.editorElement); // will be put into the default slot

    // The server's options first, ours on top. Where this add-on owns a setter for something Froala also has as
    // an option, the setter wins. Assigned rather than spread so that an option the server did set is not
    // overwritten with an undefined we do not have.
    //
    // Underneath the server's options sit defaults of ours. The vaadin theme is on unless they pick another.
    // The save plugin is off unless they ask for it. The value reaches the server through the value change
    // listener, and without a saveURL the plugin only runs a failing save after every edit. An upload without a
    // target is off, see _uploadsWithoutUrlOff. The URL of an upload handler goes on top, like the license key.
    //
    // pluginsEnabled is always named explicitly, because Froala's own default is every plugin registered on the page,
    // and that depends on what other editors happened to load.
    const commands = this.commands ?? [];
    registerCommands(commands);
    const options = {
      saveInterval: 0,
      theme: 'vaadin',
      ...this._uploadsWithoutUrlOff(),
      ...this.options,
      pluginsEnabled: [...pluginsEnabled, ...commands.map(({ name }) => commandPlugin(name))],
    };
    this._appliedConfig = this._config();

    if (language) {
      options.language = language;
    }
    if (this.licenseKey) {
      options.key = this.licenseKey;
    }
    if (this._valueChangeTimeout !== null) {
      options.typingTimer = this._valueChangeTimeout;
    }
    UPLOAD_KINDS.filter((kind) => this[`${kind}UploadUrl`]).forEach((kind) => {
      options[`${kind}UploadURL`] = this[`${kind}UploadUrl`];
    });
    this._filesUploadWithoutUrlOff(options);

    // Froala copies the options flat, so these lists are the very arrays of FroalaEditor.DEFAULTS, or the server's,
    // which outlive a rebuild. With its upload off a plugin cuts the upload button out of them in place, and the file
    // plugin cuts the last button on a second go. Lists of its own keep that inside this build.
    INSERT_BUTTON_LISTS.forEach((name) => {
      const buttons = options[name] ?? FroalaEditor.DEFAULTS[name];
      if (buttons) {
        options[name] = [...buttons];
      }
    });

    // Froala builds asynchronously, so an options change can arrive while the editor being replaced is still
    // bootstrapping. See _editorGeneration.
    const fromCurrentEditor =
      (handler) =>
      (...args) => {
        if (generation === this._editorGeneration) {
          return handler(...args);
        }
      };

    this.editor = new FroalaEditor(this.editorElement, {
      ...options,
      events: {
        initialized: fromCurrentEditor(() => {
          this._editorInitialized = true;

          // The server's value goes in through Froala, which cleans it first. Seeding the element's innerHTML instead
          // would let the browser parse it raw, and an event handler such as an image's onerror would run.
          this.editor.html.set(this._lastSyncedValue);

          // FieldMixin points the target at the label, helper text and error message with aria-labelledby and
          // aria-describedby, and keeps it up to date. Set here on every build, because each build brings a new
          // editable area and the target has to move to it.
          this.ariaTarget = this.editor.el;

          this._applyTabIndex();

          this._mirrorDirection();

          this._targetCommandPopovers();

          // Froala refreshes its buttons on a selection change only, which a new editor has not had yet
          if (this.activeCommands?.length) {
            this.editor.button.bulkRefresh();
          }

          // For a command without a toolbar button Froala runs the shortcut without the editor, see registerCommands.
          // Returning false stops Froala from running it itself. Put in front of Froala's own handlers, because the
          // toolbar's handler returns false for a command with a button, and no later handler would see it.
          this.editor.events.on(
            'shortcut',
            fromCurrentEditor((event, name) => {
              if (this._hasCommand(name)) {
                event.preventDefault();
                this.editor.commands.exec(name);
                // Froala runs a toolbar button's command without clicking the button, so the popover does not see it
                const popover = this._commandPopovers[name];
                if (popover?.target) {
                  popover.opened = true;
                }
                return false;
              }
            }),
            true
          );

          // anything touching editor modules has to wait for this event, so re-apply what the server may
          // already have set while Froala was still building
          this._updateReadonlyMode();

          if (this.valueChangeMode === 'interval') {
            this._startValueChangeInterval();
          }

          this._pendingInserts.splice(0).forEach((snippet) => this.replaceSelectionContent(snippet));

          if (this._pendingSelectAll) {
            this._pendingSelectAll = false;
            this.selectAll();
          }

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
          this._onValueChange();
          this.dispatchEvent(new CustomEvent('blur'));
        }),
        focus: fromCurrentEditor(() => {
          this.dispatchEvent(new CustomEvent('focus'));
        }),
        contentChanged: fromCurrentEditor(() => {
          if (this.valueChangeMode === 'change') {
            this._onValueChangeThrottled();
          }
        }),
      },
    });
  }

  _isCommandActive(name) {
    return (this.activeCommands ?? []).includes(name);
  }

  _hasCommand(name) {
    return (this.commands ?? []).some((command) => command.name === name);
  }

  /** Sets the popover of an own command, or removes it for null. Called by the server. */
  _setCommandPopover(name, popover) {
    if (popover) {
      this._commandPopovers[name] = popover;
    } else {
      delete this._commandPopovers[name];
    }

    if (this._editorInitialized) {
      this._targetCommandPopovers();
    }
  }

  /**
   * Points each command's popover at the command's toolbar button, or at nothing without one. Froala replaces its
   * buttons on every build. The popover opens and closes on a click on its target by itself, and it stays open while
   * its target changes.
   */
  _targetCommandPopovers() {
    for (const [name, popover] of Object.entries(this._commandPopovers)) {
      popover.target = this.editor.$tb?.get(0)?.querySelector(`.fr-command[data-cmd="${name}"]`) ?? null;
    }
  }

  /** Tells the server that one of its commands was triggered in this editor. */
  _runCommand(name) {
    if (this._hasCommand(name)) {
      this.dispatchEvent(new CustomEvent('_command', { detail: { name } }));
    }
  }

  /** What an editor is built from and has to be built again for when it changes, as JSON. */
  _config() {
    return JSON.stringify([
      this.options ?? null,
      this.commands ?? [],
      // the build picks its language from these when the options name none
      this.localeLanguages ?? [],
      UPLOAD_KINDS.map((kind) => this[`${kind}UploadUrl`] ?? null),
    ]);
  }

  /**
   * Switches off every upload that has nowhere to go: no handler, no URL and no S3 or Azure target in the options.
   * Froala would read the file in the browser and insert a `blob:` URL, which ends up in the stored HTML and is dead
   * after a reload (#11). `imageUpload` and its siblings take the button out of the insert popup and ignore a dropped
   * file, `imagePaste` drops a pasted image.
   */
  _uploadsWithoutUrlOff() {
    const off = {};
    UPLOAD_KINDS.filter(
      (kind) =>
        !this[`${kind}UploadUrl`] &&
        !this.options?.[`${kind}UploadURL`] &&
        !this.options?.[`${kind}UploadToS3`] &&
        !this.options?.[`${kind}UploadToAzure`]
    ).forEach((kind) => {
      off[`${kind}Upload`] = false;
    });
    if (off.imageUpload === false) {
      off.imagePaste = false;
    }
    return off;
  }

  /**
   * The files manager inserts a `blob:` URL as well when its upload has nowhere to go, but it has no switch like
   * `imageUpload`. Its upload tab is taken out of the popup instead, and the by-URL and embed tabs stay. Applied to the
   * final options, because the server's own `filesInsertButtons` must lose the tab too.
   */
  _filesUploadWithoutUrlOff(options) {
    const tabs = options.filesInsertButtons ?? FroalaEditor.DEFAULTS.filesInsertButtons;
    if (
      tabs &&
      !options.filesManagerUploadURL &&
      !options.filesManagerUploadToS3 &&
      !options.filesManagerUploadToAzure
    ) {
      options.filesInsertButtons = tabs.filter((tab) => tab !== 'filesUpload');
    }
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

    // Before that, the `initialized` handler puts the value in, because Froala's modules do not exist yet
    if (this._editorInitialized) {
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
   * elapsed timer) must never be held back. That is why the throttle lives here and not in #_onValueChange().
   */
  _onValueChangeThrottled() {
    const sinceLastSync = Date.now() - this._lastSyncedValueTimestamp;

    if (sinceLastSync < THROTTLE_MS) {
      clearTimeout(this._throttleHandle);
      this._throttleHandle = setTimeout(() => this._onValueChange(), THROTTLE_MS - sinceLastSync);
      return;
    }

    this._onValueChange();
  }

  /**
   * Sends whatever the editor holds now. It calculates the delta against the last synced value, updates it and
   * dispatches the event. Sends nothing if the delta is empty. Always immediate, because rate limiting is the
   * caller's business.
   */
  _onValueChange() {
    clearTimeout(this._throttleHandle);
    this._lastSyncedValueTimestamp = Date.now();

    const currentValue = this.editor?.html?.get() ?? this._lastSyncedValue;

    let delta;
    try {
      delta = DIFF_MATCH_PATCH.patch_toText(DIFF_MATCH_PATCH.patch_make(this._lastSyncedValue, currentValue));
    } catch (e) {
      // patch_toText encodes with encodeURI, which throws for a diff that splits an emoji's surrogate pair, e.g. 😀
      // to 😁. The full value carries the same change.
      this._resyncValue();
      return;
    }

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
  _resyncValue() {
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
        this._startValueChangeInterval();
      } else if (this._valueChangeHandleForInterval) {
        this._stopValueChangeInterval();
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
    if (!newPeriod || newPeriod <= 0) {
      throw new Error('intervalPeriod must be greater than 0');
    }

    if (this._intervalPeriod !== newPeriod) {
      this._intervalPeriod = newPeriod;

      if (this._valueChangeHandleForInterval) {
        this._startValueChangeInterval(); // also stops the current interval
      }
    }
  }

  get intervalPeriod() {
    return this._intervalPeriod;
  }

  _stopValueChangeInterval() {
    this._onValueChange();
    clearInterval(this._valueChangeHandleForInterval);
    delete this._valueChangeHandleForInterval;
  }

  _startValueChangeInterval() {
    if (this._valueChangeHandleForInterval) {
      this._stopValueChangeInterval();
    }

    this._valueChangeHandleForInterval = setInterval(this._onValueChange.bind(this), this.intervalPeriod);
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
  replaceSelectionContent(snippet) {
    // Both lock the value against the client, and the snippet would reach the server as a change from it
    if (this.disabled || this.readonly) {
      return;
    }

    if (!this._editorInitialized) {
      this._pendingInserts.push(snippet);
      return;
    }

    this.editor.html.insert(snippet);

    // Reported at once rather than left to the value change mode, because the change came from the server.
    this._onValueChange();
  }

  /**
   * Selects the whole content. Maps onto Froala's `commands.selectAll`. A call that arrives while Froala is still
   * building is held back until the editor is initialized, like replaceSelectionContent.
   */
  selectAll() {
    if (this._editorInitialized) {
      this.editor.commands.selectAll();
    } else {
      this._pendingSelectAll = true;
    }
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

  /** The focus sits in Froala's editable area, not on the host, so blurring the host alone would leave it there. */
  blur() {
    this.editor?.el?.blur();
    super.blur();
  }

  static get observedAttributes() {
    return [...super.observedAttributes, 'tabindex'];
  }

  /**
   * Moves a `tabindex` the server sets on the host to Froala's editable area, which is what takes the focus. Left on
   * the host, it would make the host a tab stop of its own next to the editable area.
   */
  attributeChangedCallback(name, oldValue, newValue) {
    super.attributeChangedCallback(name, oldValue, newValue);

    if (name === 'tabindex' && newValue !== null) {
      this._tabIndex = newValue;
      this.removeAttribute('tabindex');
      this._applyTabIndex();
    }
  }

  _applyTabIndex() {
    if (this._tabIndex !== null && this._editorInitialized) {
      this.editor.el.setAttribute('tabindex', this._tabIndex);
    }
  }

  updated(changedProperties) {
    super.updated(changedProperties);

    if (changedProperties.has('disabled') || changedProperties.has('readonly')) {
      this._updateReadonlyMode();
    }

    // Froala refreshes its buttons on a selection change only, so a state the server switched shows right away
    if (changedProperties.has('activeCommands') && this._editorInitialized) {
      this.editor.button.bulkRefresh();
    }

    // Compared by content, not by `changedProperties.has`. On the update that builds the editor the options and
    // commands are already in it, and a rebuild would be for nothing.
    if (this.editor && this._config() !== this._appliedConfig) {
      this._rebuildEditor();
    }
  }

  /**
   * Applies `disabled` / `readonly` to the editor. Froala has no mode API. `edit.off()` drops the contenteditable
   * attribute and disables the toolbar, `edit.on()` restores both.
   */
  _updateReadonlyMode() {
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
