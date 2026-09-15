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

/*
 * Phase 2, check 3: can Froala's plugins and language files be loaded on demand after the Vaadin build, instead of
 * shipping `froala_editor.pkgd.min.js` (1956 KB) with all 49 plugins in it?
 *
 * A spike, deliberately in `demo/` and not in the add-on: it answers three questions and is then thrown away or
 * promoted.
 *
 *   1. Do the plugin files, which are UMD modules, register themselves into the same core that this file imports?
 *   2. Can every dynamic import be awaited before `new FroalaEditor(...)` builds the plugin registry?
 *   3. Does a production build emit one chunk per file instead of folding them back into the main bundle?
 *
 * Every specifier below is a string literal. Rollup cannot follow a variable through a bare package id, so the maps
 * are generated rather than built at runtime. That is also what makes each entry its own chunk.
 */

import FroalaEditor from 'froala-editor/js/froala_editor.min.js';
import 'froala-editor/css/froala_editor.min.css';

const PLUGINS = {
  ai_assist: () =>
    Promise.all([
      import('froala-editor/js/plugins/ai_assist.min.js'),
      import('froala-editor/css/plugins/ai_assist.min.css'),
    ]),
  align: () => import('froala-editor/js/plugins/align.min.js'),
  char_counter: () =>
    Promise.all([
      import('froala-editor/js/plugins/char_counter.min.js'),
      import('froala-editor/css/plugins/char_counter.min.css'),
    ]),
  code_beautifier: () => import('froala-editor/js/plugins/code_beautifier.min.js'),
  code_snippet: () =>
    Promise.all([
      import('froala-editor/js/plugins/code_snippet.min.js'),
      import('froala-editor/css/plugins/code_snippet.min.css'),
    ]),
  code_view: () =>
    Promise.all([
      import('froala-editor/js/plugins/code_view.min.js'),
      import('froala-editor/css/plugins/code_view.min.css'),
    ]),
  collaborative: () =>
    Promise.all([
      import('froala-editor/js/plugins/collaborative.min.js'),
      import('froala-editor/css/plugins/collaborative.min.css'),
    ]),
  colors: () =>
    Promise.all([import('froala-editor/js/plugins/colors.min.js'), import('froala-editor/css/plugins/colors.min.css')]),
  cryptojs: () => import('froala-editor/js/plugins/cryptojs.min.js'),
  draggable: () =>
    Promise.all([
      import('froala-editor/js/plugins/draggable.min.js'),
      import('froala-editor/css/plugins/draggable.min.css'),
    ]),
  edit_in_popup: () => import('froala-editor/js/plugins/edit_in_popup.min.js'),
  emoticons: () =>
    Promise.all([
      import('froala-editor/js/plugins/emoticons.min.js'),
      import('froala-editor/css/plugins/emoticons.min.css'),
    ]),
  entities: () => import('froala-editor/js/plugins/entities.min.js'),
  export_to_word: () => import('froala-editor/js/plugins/export_to_word.min.js'),
  file: () =>
    Promise.all([import('froala-editor/js/plugins/file.min.js'), import('froala-editor/css/plugins/file.min.css')]),
  files_manager: () =>
    Promise.all([
      import('froala-editor/js/plugins/files_manager.min.js'),
      import('froala-editor/css/plugins/files_manager.min.css'),
    ]),
  filestack: () =>
    Promise.all([
      import('froala-editor/js/plugins/filestack.min.js'),
      import('froala-editor/css/plugins/filestack.min.css'),
    ]),
  find_and_replace: () =>
    Promise.all([
      import('froala-editor/js/plugins/find_and_replace.min.js'),
      import('froala-editor/css/plugins/find_and_replace.min.css'),
    ]),
  font_family: () => import('froala-editor/js/plugins/font_family.min.js'),
  font_size: () => import('froala-editor/js/plugins/font_size.min.js'),
  forms: () => import('froala-editor/js/plugins/forms.min.js'),
  fullscreen: () =>
    Promise.all([
      import('froala-editor/js/plugins/fullscreen.min.js'),
      import('froala-editor/css/plugins/fullscreen.min.css'),
    ]),
  help: () =>
    Promise.all([import('froala-editor/js/plugins/help.min.js'), import('froala-editor/css/plugins/help.min.css')]),
  image: () =>
    Promise.all([import('froala-editor/js/plugins/image.min.js'), import('froala-editor/css/plugins/image.min.css')]),
  image_manager: () =>
    Promise.all([
      import('froala-editor/js/plugins/image_manager.min.js'),
      import('froala-editor/css/plugins/image_manager.min.css'),
    ]),
  import_from_word: () => import('froala-editor/js/plugins/import_from_word.min.js'),
  inline_class: () => import('froala-editor/js/plugins/inline_class.min.js'),
  inline_style: () => import('froala-editor/js/plugins/inline_style.min.js'),
  line_breaker: () =>
    Promise.all([
      import('froala-editor/js/plugins/line_breaker.min.js'),
      import('froala-editor/css/plugins/line_breaker.min.css'),
    ]),
  line_height: () => import('froala-editor/js/plugins/line_height.min.js'),
  link: () => import('froala-editor/js/plugins/link.min.js'),
  link_to_anchor: () =>
    Promise.all([
      import('froala-editor/js/plugins/link_to_anchor.min.js'),
      import('froala-editor/css/plugins/link_to_anchor.min.css'),
    ]),
  lists: () => import('froala-editor/js/plugins/lists.min.js'),
  markdown: () =>
    Promise.all([
      import('froala-editor/js/plugins/markdown.min.js'),
      import('froala-editor/css/plugins/markdown.min.css'),
    ]),
  page_break: () =>
    Promise.all([
      import('froala-editor/js/plugins/page_break.min.js'),
      import('froala-editor/css/plugins/page_break.min.css'),
    ]),
  paragraph_format: () => import('froala-editor/js/plugins/paragraph_format.min.js'),
  paragraph_style: () => import('froala-editor/js/plugins/paragraph_style.min.js'),
  print: () => import('froala-editor/js/plugins/print.min.js'),
  quick_insert: () =>
    Promise.all([
      import('froala-editor/js/plugins/quick_insert.min.js'),
      import('froala-editor/css/plugins/quick_insert.min.css'),
    ]),
  quote: () => import('froala-editor/js/plugins/quote.min.js'),
  save: () => import('froala-editor/js/plugins/save.min.js'),
  special_characters: () =>
    Promise.all([
      import('froala-editor/js/plugins/special_characters.min.js'),
      import('froala-editor/css/plugins/special_characters.min.css'),
    ]),
  table: () =>
    Promise.all([import('froala-editor/js/plugins/table.min.js'), import('froala-editor/css/plugins/table.min.css')]),
  track_changes: () => import('froala-editor/js/plugins/track_changes.min.js'),
  trim_video: () =>
    Promise.all([
      import('froala-editor/js/plugins/trim_video.min.js'),
      import('froala-editor/css/plugins/trim_video.min.css'),
    ]),
  url: () => import('froala-editor/js/plugins/url.min.js'),
  video: () =>
    Promise.all([import('froala-editor/js/plugins/video.min.js'), import('froala-editor/css/plugins/video.min.css')]),
  word_counter: () => import('froala-editor/js/plugins/word_counter.min.js'),
  word_paste: () => import('froala-editor/js/plugins/word_paste.min.js'),
};

const LANGUAGES = {
  ar: () => import('froala-editor/js/languages/ar.js'),
  bs: () => import('froala-editor/js/languages/bs.js'),
  cs: () => import('froala-editor/js/languages/cs.js'),
  da: () => import('froala-editor/js/languages/da.js'),
  de: () => import('froala-editor/js/languages/de.js'),
  el: () => import('froala-editor/js/languages/el.js'),
  en_ca: () => import('froala-editor/js/languages/en_ca.js'),
  en_gb: () => import('froala-editor/js/languages/en_gb.js'),
  es: () => import('froala-editor/js/languages/es.js'),
  et: () => import('froala-editor/js/languages/et.js'),
  fa: () => import('froala-editor/js/languages/fa.js'),
  fi: () => import('froala-editor/js/languages/fi.js'),
  fr: () => import('froala-editor/js/languages/fr.js'),
  he: () => import('froala-editor/js/languages/he.js'),
  hr: () => import('froala-editor/js/languages/hr.js'),
  hu: () => import('froala-editor/js/languages/hu.js'),
  id: () => import('froala-editor/js/languages/id.js'),
  it: () => import('froala-editor/js/languages/it.js'),
  ja: () => import('froala-editor/js/languages/ja.js'),
  ko: () => import('froala-editor/js/languages/ko.js'),
  ku: () => import('froala-editor/js/languages/ku.js'),
  me: () => import('froala-editor/js/languages/me.js'),
  nb: () => import('froala-editor/js/languages/nb.js'),
  nl: () => import('froala-editor/js/languages/nl.js'),
  pl: () => import('froala-editor/js/languages/pl.js'),
  pt_br: () => import('froala-editor/js/languages/pt_br.js'),
  pt_pt: () => import('froala-editor/js/languages/pt_pt.js'),
  ro: () => import('froala-editor/js/languages/ro.js'),
  ru: () => import('froala-editor/js/languages/ru.js'),
  sk: () => import('froala-editor/js/languages/sk.js'),
  sl: () => import('froala-editor/js/languages/sl.js'),
  sr: () => import('froala-editor/js/languages/sr.js'),
  sv: () => import('froala-editor/js/languages/sv.js'),
  th: () => import('froala-editor/js/languages/th.js'),
  tr: () => import('froala-editor/js/languages/tr.js'),
  uk: () => import('froala-editor/js/languages/uk.js'),
  vi: () => import('froala-editor/js/languages/vi.js'),
  zh_cn: () => import('froala-editor/js/languages/zh_cn.js'),
  zh_tw: () => import('froala-editor/js/languages/zh_tw.js'),
};

class FroalaOnDemandSpike extends HTMLElement {
  connectedCallback() {
    this.load();
  }

  async load() {
    const wanted = JSON.parse(this.getAttribute('plugins') || '[]');
    const language = this.getAttribute('language') || null;

    const pluginsBefore = Object.keys(FroalaEditor.PLUGINS).length;
    const languagesBefore = Object.keys(FroalaEditor.LANGUAGE || {});

    // Loaded one after another only to attribute registry names to files: `pluginsEnabled` wants the name a plugin
    // registers itself under (`fontFamily`), the file is called something else (`font_family.min.js`). Nothing
    // stops them from being loaded in parallel.
    const started = performance.now();
    const fileToPlugin = {};
    for (const name of wanted) {
      if (!PLUGINS[name]) {
        fileToPlugin[name] = '<no such file>';
        continue;
      }
      const before = new Set(Object.keys(FroalaEditor.PLUGINS));
      await PLUGINS[name]();
      const added = Object.keys(FroalaEditor.PLUGINS).filter((key) => !before.has(key));
      fileToPlugin[name] = added.length ? added.join('+') : '<registers no plugin>';
    }
    if (language && LANGUAGES[language]) {
      await LANGUAGES[language]();
    }
    const loadMillis = Math.round(performance.now() - started);

    const registered = Object.keys(FroalaEditor.PLUGINS);
    const languagesAfter = Object.keys(FroalaEditor.LANGUAGE || {});

    // What the browser actually fetched, so the claim "one request per selected file, nothing for the rest" can be
    // read off the page instead of the network tab. transferSize is 0 for anything served from the cache.
    const resources = performance
      .getEntriesByType('resource')
      .filter((entry) => entry.startTime >= started)
      .map((entry) => ({
        file: entry.name.split('/').pop().split('?')[0],
        bytes: entry.transferSize || entry.encodedBodySize || 0,
      }));
    // Everything else the page happened to pull in the same window belongs to Vaadin, not to this measurement.
    // A production chunk is named after its source file with a hash appended (`align.min-BowZ32Ae.js`), a dev one
    // after the whole path (`froala-editor_js_plugins_align__min__js.js`), so both forms have to be recognised.
    const known = [...Object.keys(PLUGINS), ...Object.keys(LANGUAGES)];
    const fetched = resources.filter(
      (entry) => /froala/i.test(entry.file) || known.some((name) => entry.file.startsWith(name))
    );
    const bytesFetched = fetched.reduce((sum, entry) => sum + entry.bytes, 0);

    const host = document.createElement('div');
    host.innerHTML = '<p>Hello <b>World</b></p>';
    this.appendChild(host);

    const editor = new FroalaEditor(host, {
      language: language || undefined,
      pluginsEnabled: registered,
      events: {
        initialized: () => {
          // Froala reads its plugin registry once, when the editor is constructed, so what ended up in the
          // toolbar is the honest answer to whether awaiting the imports was early enough.
          const toolbarButtons = [...editor.$tb.get(0).querySelectorAll('.fr-command')].map((button) =>
            button.getAttribute('data-cmd')
          );

          // Ordered so the answer is the first thing on screen and the long lists are at the bottom.
          const detail = {
            pluginsRequested: wanted.length,
            pluginsRegisteredBefore: pluginsBefore,
            pluginsRegisteredAfter: registered.length,
            languageRequested: language,
            languagesLoadedBefore: languagesBefore,
            languagesLoadedAfter: languagesAfter,
            filesFetched: fetched.length,
            bytesFetched,
            loadMillis,
            fetched,
            fileToPlugin,
            toolbarButtons,
          };
          detail.json = JSON.stringify(detail, null, 1);

          this.dispatchEvent(new CustomEvent('spike-report', { detail }));
        },
      },
    });
  }
}

customElements.define('froala-ondemand-spike', FroalaOnDemandSpike);
