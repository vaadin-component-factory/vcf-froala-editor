import 'froala-editor/css/froala_editor.pkgd.min.css';

// Froala's core alone, which registers no plugin. The plugin and language files below register themselves into it,
// because their CommonJS branch requires this same package id.
import FroalaEditor from 'froala-editor';
// A module rather than a plugin, so pluginsEnabled has no say over it. Froala's packaged bundle always carries it.
import 'froala-editor/js/plugins/edit_in_popup.min.js';

// The plugin files of Froala's packaged bundle, by the name each plugin registers itself under, which is what
// pluginsEnabled holds. Each specifier is a string literal, because the bundler cannot follow a variable into a
// package. That is also what makes each file a chunk of its own, downloaded only when an editor enables it. Their CSS
// is part of the packaged stylesheet above, which is why they are kept apart from the others.
const BUNDLED_PLUGIN_FILES = {
  aiAssist: () => import('froala-editor/js/plugins/ai_assist.min.js'),
  align: () => import('froala-editor/js/plugins/align.min.js'),
  charCounter: () => import('froala-editor/js/plugins/char_counter.min.js'),
  codeBeautifier: () => import('froala-editor/js/plugins/code_beautifier.min.js'),
  codeSnippet: () => import('froala-editor/js/plugins/code_snippet.min.js'),
  codeView: () => import('froala-editor/js/plugins/code_view.min.js'),
  collaborative: () => import('froala-editor/js/plugins/collaborative.min.js'),
  colors: () => import('froala-editor/js/plugins/colors.min.js'),
  cryptoJSPlugin: () => import('froala-editor/js/plugins/cryptojs.min.js'),
  draggable: () => import('froala-editor/js/plugins/draggable.min.js'),
  emoticons: () => import('froala-editor/js/plugins/emoticons.min.js'),
  entities: () => import('froala-editor/js/plugins/entities.min.js'),
  exportToWord: () => import('froala-editor/js/plugins/export_to_word.min.js'),
  file: () => import('froala-editor/js/plugins/file.min.js'),
  filesManager: () => import('froala-editor/js/plugins/files_manager.min.js'),
  filestack: () => import('froala-editor/js/plugins/filestack.min.js'),
  findReplace: () => import('froala-editor/js/plugins/find_and_replace.min.js'),
  fontFamily: () => import('froala-editor/js/plugins/font_family.min.js'),
  fontSize: () => import('froala-editor/js/plugins/font_size.min.js'),
  forms: () => import('froala-editor/js/plugins/forms.min.js'),
  fullscreen: () => import('froala-editor/js/plugins/fullscreen.min.js'),
  help: () => import('froala-editor/js/plugins/help.min.js'),
  image: () => import('froala-editor/js/plugins/image.min.js'),
  imageManager: () => import('froala-editor/js/plugins/image_manager.min.js'),
  importFromWord: () => import('froala-editor/js/plugins/import_from_word.min.js'),
  inlineClass: () => import('froala-editor/js/plugins/inline_class.min.js'),
  inlineStyle: () => import('froala-editor/js/plugins/inline_style.min.js'),
  lineBreaker: () => import('froala-editor/js/plugins/line_breaker.min.js'),
  lineHeight: () => import('froala-editor/js/plugins/line_height.min.js'),
  link: () => import('froala-editor/js/plugins/link.min.js'),
  linkToAnchor: () => import('froala-editor/js/plugins/link_to_anchor.min.js'),
  lists: () => import('froala-editor/js/plugins/lists.min.js'),
  markdown: () => import('froala-editor/js/plugins/markdown.min.js'),
  pageBreak: () => import('froala-editor/js/plugins/page_break.min.js'),
  paragraphFormat: () => import('froala-editor/js/plugins/paragraph_format.min.js'),
  paragraphStyle: () => import('froala-editor/js/plugins/paragraph_style.min.js'),
  print: () => import('froala-editor/js/plugins/print.min.js'),
  quickInsert: () => import('froala-editor/js/plugins/quick_insert.min.js'),
  quote: () => import('froala-editor/js/plugins/quote.min.js'),
  save: () => import('froala-editor/js/plugins/save.min.js'),
  specialCharacters: () => import('froala-editor/js/plugins/special_characters.min.js'),
  table: () => import('froala-editor/js/plugins/table.min.js'),
  url: () => import('froala-editor/js/plugins/url.min.js'),
  video: () => import('froala-editor/js/plugins/video.min.js'),
  wordCounter: () => import('froala-editor/js/plugins/word_counter.min.js'),
  wordPaste: () => import('froala-editor/js/plugins/word_paste.min.js'),
};

// The plugin files that are in no Froala bundle, so an editor only gets them by naming them. The five under
// js/third_party/ bring their CSS along, because the packaged stylesheet does not carry it.
const OTHER_PLUGIN_FILES = {
  track_changes: () => import('froala-editor/js/plugins/track_changes.min.js'),
  trimVideoPlugin: () => import('froala-editor/js/plugins/trim_video.min.js'),
  embedly: () =>
    Promise.all([
      import('froala-editor/js/third_party/embedly.min.js'),
      import('froala-editor/css/third_party/embedly.min.css'),
    ]),
  fontAwesome: () =>
    Promise.all([
      import('froala-editor/js/third_party/font_awesome.min.js'),
      import('froala-editor/css/third_party/font_awesome.min.css'),
    ]),
  imageFilerobot: () =>
    Promise.all([
      import('froala-editor/js/third_party/imageFileRobot.min.js'),
      import('froala-editor/css/third_party/imageFileRobot.min.css'),
    ]),
  imageTUI: () =>
    Promise.all([
      import('froala-editor/js/third_party/image_tui.min.js'),
      import('froala-editor/css/third_party/image_tui.min.css'),
    ]),
  spellChecker: () =>
    Promise.all([
      import('froala-editor/js/third_party/spell_checker.min.js'),
      import('froala-editor/css/third_party/spell_checker.min.css'),
    ]),
};

// Froala has no file for English, its built-in language
const LANGUAGE_FILES = {
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

const PLUGIN_FILES = { ...BUNDLED_PLUGIN_FILES, ...OTHER_PLUGIN_FILES };

/**
 * Loads the files of the given plugins and language and resolves once they are registered, which has to happen before
 * the editor is built. Froala reads its plugin registry and its languages only then.
 *
 * A name without a file of ours, or a file that fails to load, does not stop the editor. Froala skips a plugin it does
 * not know and stays English without its language file, as it would with the packaged bundle.
 */
async function loadFroalaFiles(pluginsEnabled, language) {
  const loads = pluginsEnabled.filter((name) => Object.hasOwn(PLUGIN_FILES, name)).map((name) => PLUGIN_FILES[name]());

  if (Object.hasOwn(LANGUAGE_FILES, language ?? '')) {
    loads.push(LANGUAGE_FILES[language]());
  }

  const results = await Promise.allSettled(loads);
  results
    .filter((result) => result.status === 'rejected')
    .forEach((result) => console.error('vcf-froala-editor: a Froala file failed to load', result.reason));
}

export { FroalaEditor, loadFroalaFiles };
