# Manual test: plugins and languages loaded on demand

Checks by hand what `FroalaLoadingIT` checks automatically (#14). The demo has no control for the
language or the plugins yet, so the options are set from the browser console.

## 1. Start the demo in production mode

Only a production build names each Froala file as a chunk of its own, such as `align.min-DbhRAmNW.js`.
A dev build names them after the whole path, such as `froala-editor_js_plugins_align__min__js.js`.

```
mvn -pl component install -DskipTests
mvn -pl demo -Pproduction spring-boot:run
```

In the devcontainer both commands also need `-Drequire.home.node=false`.

## 2. The default editor

Open `http://localhost:8080/` with the DevTools open, in the Network tab filtered to JS, and reload.

Expected:

- 14 plugin files, those of `FroalaPlugin.basics()`, such as `align.min-….js` and `link.min-….js`
- no `table.min-….js`, no `ai_assist.min-….js` and no language file such as `de-….js`

## 3. Restricted plugins and a language

In the console:

```js
const e = document.querySelector('vcf-froala-editor');
e.options = { language: 'de', pluginsEnabled: ['align', 'aiAssist'] };
```

Expected:

- the tooltips are German, for example `Fett (Ctrl+B)` on the bold button
- `de-….js` and `ai_assist.min-….js` show up in the Network tab
- `e.editor.opts.pluginsEnabled` answers `['align', 'aiAssist']`

This sets the options in the browser only, so the server does not know about them. That is enough for
the check.

## 4. Before the next check

Reload the page. A file the page has loaded once stays loaded, and the Network tab does not show it
again.
