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

// Generates the rules of the vaadin Froala theme from Froala's own stylesheet. Froala hard-codes its colours, radii,
// font sizes, shadows and cursors, so every rule that sets one is repeated under the theme class, with the value
// expressed through the theme's custom properties. Run it again after a Froala update:
//
//   node component/src/theme-generator/generate-vaadin-theme.js demo/node_modules/froala-editor
//
// Every colour is read as a full tone mixed with white. A grey is the neutral colour mixed with white, a light blue is
// the accent colour mixed with white, and so on. The white becomes the background colour, so the mix turns dark along with
// it. A grey border is read against the border colour instead, so one property recolours all grey borders, and grey text
// goes to the closest of Lumo's text colours. White text on a coloured background stays light, like the text on a Lumo
// primary button.
//
// Radii and shadows go to the closest Lumo size. Font sizes go to Lumo's where Froala's is exactly one of them, and the
// editable area takes the size of a Vaadin field's value. Corners are themed only on surfaces such as the box, popups,
// buttons and inputs, because other corners give a widget its shape. Heights, spacing and line heights of Froala's
// controls are left alone, because Froala places its dropdown arrows, badges and tooltip text at fixed offsets from
// them. Content styles inside the editable area keep their sizes, corners and shadows, because they shape the document.
//
// Only longhands are written, such as border-color, so widths, styles and background-clip stay Froala's. Froala's
// sans-serif fonts become the Vaadin theme's font. The repeated rules are one class more specific than Froala's, so a
// later Froala rule that sets the same property to a value the theme leaves alone, such as background: transparent or
// cursor: default, is repeated as well. That happens only where the theme set that longhand on one of the classes of
// the element the later rule applies to.

const fs = require('fs');
const path = require('path');

const froalaDir = process.argv[2];
if (!froalaDir) {
  console.error('usage: node generate-vaadin-theme.js <path to the froala-editor npm package>');
  process.exit(1);
}

const version = JSON.parse(fs.readFileSync(path.join(froalaDir, 'package.json'), 'utf8')).version;
const source = fs.readFileSync(path.join(froalaDir, 'css', 'froala_editor.pkgd.css'), 'utf8');
const target = path.join(
  __dirname,
  '..',
  'main',
  'resources',
  'META-INF',
  'frontend',
  'vcf-froala-editor',
  'vcf-froala-theme-vaadin-rules.css'
);

const THEME = 'froala-vaadin-theme';
const BASE = 'var(--vcf-froala-background-color)';
const ON_TONE = 'var(--vcf-froala-accent-contrast-color)';
const BORDER = 'var(--vcf-froala-border-color)';
const FOCUS_COLOR = 'var(--vcf-froala-focus-ring-color)';
const FOCUS_WIDTH = 'var(--vcf-froala-focus-ring-width)';

// Froala's font sizes that are exactly one of Lumo's
const FONT_SIZE = { '12px': 'xxs', '13px': 'xs', '14px': 's', '16px': 'm' };

// The box parts whose 10px corners are the field's corners
const FIELD_PARTS = ['.fr-box', '.fr-toolbar', '.fr-second-toolbar', '.fr-wrapper'];

// The surfaces whose corners are themed: the box, its bars, popups, dropdowns, dialogs, tooltips, buttons and inputs.
// Other corners, such as those of a slider thumb or a pill-shaped toggle, give a widget its shape and stay Froala's.
const RADIUS_SURFACES = [
  ...FIELD_PARTS,
  '.fr-popup',
  '.fr-dropdown-menu',
  '.fr-modal-wrapper',
  '.fr-tooltip',
  '.fr-btn',
  '.fr-command',
  'input',
  'textarea',
  'select',
  'button',
];

// Lumo's text colours by the share of contrast they carry, darkest first
const TEXT_COLORS = [
  [80, 'var(--vcf-froala-text-color)'],
  [60, 'var(--vcf-froala-text-color-secondary)'],
  [40, 'var(--vcf-froala-text-color-tertiary)'],
  [0, 'var(--vcf-froala-text-color-disabled)'],
];

// Shadows of elements Lumo has a level for, like its overlays and dialogs. Other shadows go by their blur.
const SHADOW_BY_CLASS = [
  ['.fr-modal-wrapper', 'l'],
  ['.fr-popup', 'm'],
  ['.fr-dropdown-menu', 'm'],
  ['.fr-inline', 'm'],
];

// The properties that carry a colour, each with the longhand the colour goes to
const COLOR_LONGHAND = {
  color: 'color',
  'caret-color': 'caret-color',
  fill: 'fill',
  stroke: 'stroke',
  outline: 'outline-color',
  'outline-color': 'outline-color',
  'text-decoration': 'text-decoration-color',
  'text-decoration-color': 'text-decoration-color',
  background: 'background-color',
  'background-color': 'background-color',
  'background-image': 'background-image',
};
for (const side of ['', '-top', '-right', '-bottom', '-left']) {
  COLOR_LONGHAND[`border${side}`] = `border${side}-color`;
  COLOR_LONGHAND[`border${side}-color`] = `border${side}-color`;
}

const NAMED_COLORS = {
  white: [255, 255, 255],
  black: [0, 0, 0],
  whitesmoke: [245, 245, 245],
  gray: [128, 128, 128],
  grey: [128, 128, 128],
  red: [255, 0, 0],
  green: [0, 128, 0],
  blue: [0, 0, 255],
};
const COLOR_TOKEN = new RegExp(
  String.raw`#[0-9a-f]{3,8}\b|rgba?\([^)]*\)|\b(?:${Object.keys(NAMED_COLORS).join('|')})\b`,
  'gi'
);
const SANS_SERIF_FONT = /arial|helvetica|sans-serif|-apple-system/i;

/** What the generator skipped and a Froala update may have made relevant, reported once at the end. */
const skipped = new Set();

/**
 * Walks the text and calls visit(c, i, depth) for each character outside quotes, where depth counts the brackets
 * around it. A visit that returns true stops the walk and returns its index.
 */
function scan(text, from, open, close, visit) {
  let depth = 0;
  let quote = null;

  for (let i = from; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (c === quote) quote = null;
    } else if (c === '"' || c === "'") {
      quote = c;
    } else if (c === open) {
      depth++;
    } else if (c === close) {
      depth--;
    }
    if (!quote && visit(c, i, depth)) return i;
  }

  return -1;
}

/** Splits on a separator outside of quotes and parentheses. */
function splitTopLevel(text, separator) {
  const parts = [];
  let start = 0;

  scan(text, 0, '(', ')', (c, i, depth) => {
    if (c === separator && depth === 0) {
      parts.push(text.slice(start, i));
      start = i + 1;
    }
  });
  parts.push(text.slice(start));

  return parts;
}

/** Returns the index after the block that opens at the given brace. */
function blockEnd(text, openBrace) {
  const close = scan(text, openBrace, '{', '}', (c, i, depth) => c === '}' && depth === 0);
  if (close < 0) throw new Error('unbalanced braces at ' + openBrace);
  return close + 1;
}

/** Yields {media, selector, declarations} for every style rule, in stylesheet order. */
function* styleRules(css, media = null) {
  let i = 0;

  while (i < css.length) {
    const openBrace = css.indexOf('{', i);
    if (openBrace < 0) return;

    const head = css.slice(i, openBrace).trim();
    const end = blockEnd(css, openBrace);
    const body = css.slice(openBrace + 1, end - 1);

    if (head.startsWith('@media') || head.startsWith('@supports')) {
      yield* styleRules(body, head);
    } else if (head.startsWith('@')) {
      if (!/^@(-[a-z]+-)?(keyframes|font-face)\b/.test(head)) skipped.add(`at-rule ${head}`);
    } else {
      const declarations = splitTopLevel(body, ';')
        .map((declaration) => declaration.trim())
        .filter((declaration) => declaration.includes(':'))
        .map((declaration) => {
          const colon = declaration.indexOf(':');
          return [declaration.slice(0, colon).trim().toLowerCase(), declaration.slice(colon + 1).trim()];
        });
      yield { media, selector: head.replace(/\s+/g, ' '), declarations };
    }

    i = end;
  }
}

/** Returns [r, g, b, alpha] with channels from 0 to 255. */
function parseColor(token) {
  const lower = token.toLowerCase();
  if (NAMED_COLORS[lower]) return [...NAMED_COLORS[lower], 1];

  if (lower.startsWith('#')) {
    let hex = lower.slice(1);
    if (hex.length <= 4) hex = [...hex].map((c) => c + c).join('');
    const alpha = hex.length === 8 ? parseInt(hex.slice(6), 16) / 255 : 1;
    return [parseInt(hex.slice(0, 2), 16), parseInt(hex.slice(2, 4), 16), parseInt(hex.slice(4, 6), 16), alpha];
  }

  const [r, g, b, a = 1] = lower.match(/[\d.]+/g).map(Number);
  return [r, g, b, a];
}

/** Grey, or so little tinted that it reads as grey, such as Froala's slightly blue #6A757E. */
function isGrey(rgb) {
  const max = Math.max(...rgb);
  const min = Math.min(...rgb);
  const saturation = max === min ? 0 : (max - min) / (255 - Math.abs(max + min - 255));
  return max - min < 12 || saturation < 0.25;
}

function hue([r, g, b]) {
  const max = Math.max(r, g, b);
  const chroma = max - Math.min(r, g, b);
  const h = max === r ? ((g - b) / chroma) % 6 : max === g ? (b - r) / chroma + 2 : (r - g) / chroma + 4;
  return (h * 60 + 360) % 360;
}

/** The theme property standing in for a tone, or null for a hue Lumo has no colour for. */
function roleOf(rgb) {
  if (isGrey(rgb)) return 'var(--vcf-froala-neutral-color)';

  const h = hue(rgb);
  if (h < 20 || h >= 340) return 'var(--vcf-froala-error-color)';
  if (h < 70) return 'var(--vcf-froala-warning-color)';
  if (h >= 90 && h < 160) return 'var(--vcf-froala-success-color)';
  if (h >= 190 && h < 245) return 'var(--vcf-froala-accent-color)';
  return null;
}

const toHex = (channels) => '#' + channels.map((c) => Math.round(c).toString(16).padStart(2, '0')).join('');

const mix = (color, percent, other) => (percent >= 100 ? color : `color-mix(in srgb, ${color} ${percent}%, ${other})`);

/**
 * Expresses one colour through the theme's properties. A grey border is read against the border colour, and grey
 * text goes to the closest of Lumo's text colours.
 */
function themed(token, onTone, border, text) {
  const [r, g, b, alpha] = parseColor(token);
  const whiteShare = Math.min(r, g, b) / 255;
  const percent = Math.round((1 - whiteShare) * 100);

  let color;
  if (onTone && whiteShare === 1) {
    color = ON_TONE;
  } else if (percent === 0) {
    color = BASE;
  } else if (text && isGrey([r, g, b])) {
    color = TEXT_COLORS.find(([minimum]) => percent >= minimum)[1];
  } else if (border && isGrey([r, g, b])) {
    // Froala's usual border, #ccc, is the border colour itself. Lighter ones are mixed with the background colour, darker
    // ones with the neutral colour.
    color =
      percent <= 20
        ? mix(BORDER, percent * 5, BASE)
        : mix('var(--vcf-froala-neutral-color)', Math.round(((percent - 20) * 100) / 80), BORDER);
  } else {
    // the full tone, with the white taken out
    const tone = [r, g, b].map((c) => (c - 255 * whiteShare) / (1 - whiteShare));
    color = mix(roleOf([r, g, b]) ?? toHex(tone), percent, BASE);
  }

  return alpha < 1 ? mix(color, Math.round(alpha * 100), 'transparent') : color;
}

/** Whether a colour is a tone rather than white, grey or black. */
const isTone = (token) => !isGrey(parseColor(token).slice(0, 3));

const WHOLE_COLOR_TOKEN = new RegExp(`^(?:${COLOR_TOKEN.source})$`, 'i');
const isColor = (token) => WHOLE_COLOR_TOKEN.test(token);

const isFocusAccent = (token) => roleOf(parseColor(token).slice(0, 3)) === 'var(--vcf-froala-accent-color)';

/** A value set by the theme. */
const set = (longhand, value) => ({ longhand, value });

/** A value the theme leaves alone, repeated only where it overrides one the theme set. */
const keep = (longhand, value) => ({ longhand, value, reset: true });

function colorOf(property, plain, context) {
  const longhand = COLOR_LONGHAND[property];
  const border = /^border/.test(longhand);
  const focusable = context.focus && /^(border|outline)/.test(longhand);
  const themedToken = (token) => {
    if (focusable && isFocusAccent(token)) return FOCUS_COLOR;
    if (property === 'color' && context.subjects.includes('.fr-placeholder'))
      return 'var(--vcf-froala-placeholder-color)';
    if (property === 'color' && context.subjects.includes('.fr-element')) return 'var(--vcf-froala-value-color)';
    return themed(token, context.onTone && (property === 'color' || property === 'fill'), border, property === 'color');
  };

  if (longhand === property) {
    // a colour property of its own, or background-image with its gradient
    let found = false;
    const value = plain
      .split(/(url\([^)]*\))/)
      .map((part, index) =>
        index % 2
          ? part
          : part.replace(COLOR_TOKEN, (token) => {
              found = true;
              return themedToken(token);
            })
      )
      .join('');
    return [found ? set(longhand, value) : keep(longhand, plain)];
  }

  // A shorthand. Its colour goes to the longhand, and a shorthand without one resets the colour.
  const tokens = splitTopLevel(plain, ' ');
  const color = tokens.find(isColor);
  if (!color) {
    return [
      keep(longhand, property === 'background' || tokens.includes('transparent') ? 'transparent' : 'currentcolor'),
    ];
  }

  const result = [set(longhand, themedToken(color))];
  if (property === 'outline' && focusable && isFocusAccent(color)) {
    result.push(set('outline-width', FOCUS_WIDTH));
  }
  return result;
}

function radiusOf(property, plain, context) {
  if (context.content || !context.subjects.some((name) => RADIUS_SURFACES.includes(name))) {
    return [keep(property, plain)];
  }

  let found = false;
  const value = plain.replace(/(\d+(?:\.\d+)?)px/g, (length, number) => {
    const n = Number(number);
    const size = n === 0 ? null : n <= 4 ? 's' : n <= 8 ? 'm' : n <= 16 ? 'l' : null;
    if (!size) return length;

    found = true;
    return n === 10 && context.subjects.some((name) => FIELD_PARTS.includes(name))
      ? 'var(--vcf-froala-field-border-radius)'
      : `var(--vcf-froala-radius-${size})`;
  });
  return [found ? set(property, value) : keep(property, plain)];
}

function fontSizeOf(property, plain, context) {
  // The editable element takes the size of a Vaadin field's value. Sizes inside the content shape the document and
  // stay Froala's.
  if (context.subjects.includes('.fr-element') || context.subjects.includes('.fr-placeholder')) {
    return [set(property, 'var(--vcf-froala-content-font-size)')];
  }
  if (context.content) return [keep(property, plain)];

  const size = FONT_SIZE[plain];
  return [size ? set(property, `var(--vcf-froala-font-size-${size})`) : keep(property, plain)];
}

function shadowOf(property, plain, context) {
  if (context.content || /inset/.test(plain) || plain === 'none') return [keep(property, plain)];

  const layers = splitTopLevel(plain, ',').map((layer) =>
    (layer.replace(COLOR_TOKEN, '').match(/-?[\d.]+px|\b0\b/g) ?? []).map(parseFloat)
  );

  // A ring without blur is how Froala draws some focus indicators.
  if (layers.length === 1 && layers[0].length === 4 && layers[0][2] === 0 && context.focus) {
    return [set(property, `0 0 0 ${FOCUS_WIDTH} ${FOCUS_COLOR}`)];
  }

  const blur = Math.max(...layers.map((lengths) => lengths[2] ?? 0));
  const size =
    SHADOW_BY_CLASS.find(([name]) => context.subjects.includes(name))?.[1] ??
    (blur <= 2 ? 'xs' : blur <= 6 ? 's' : blur <= 18 ? 'm' : 'l');
  return [set(property, `var(--vcf-froala-shadow-${size})`)];
}

function fontFamilyOf(property, plain) {
  return [
    SANS_SERIF_FONT.test(plain) && !/monospace/i.test(plain)
      ? set(property, 'var(--vcf-froala-font-family)')
      : keep(property, plain),
  ];
}

const cursorOf = (property, plain) => [
  plain === 'pointer' ? set(property, 'var(--vcf-froala-clickable-cursor)') : keep(property, plain),
];

const OTHER_THEMED = { 'font-size': fontSizeOf, 'box-shadow': shadowOf, 'font-family': fontFamilyOf, cursor: cursorOf };

/** What turns a declaration of this property into the theme's, or undefined for one the theme leaves alone. */
const themerOf = (property) =>
  COLOR_LONGHAND[property]
    ? colorOf
    : /^border(-(top|bottom)-(left|right))?-radius$/.test(property)
    ? radiusOf
    : OTHER_THEMED[property];

/** Returns what the theme makes of one declaration, as a list of values set or kept. */
function themedDeclaration([property, value], context) {
  const important = /!important/.test(value) ? ' !important' : '';
  const plain = value.replace(/!important/, '').trim();

  // A function this generator does not know, such as a preprocessor call Froala left in its CSS, is not read at all.
  const unknown = (plain.match(/[a-z-]+\(/gi) ?? []).filter((name) => !/^(rgba?|url|[a-z-]*gradient)\($/i.test(name));
  if (unknown.length > 0) {
    if (themerOf(property)) skipped.add(`function ${unknown.join(', ')} in ${property}: ${plain}`);
    return [];
  }

  // rgb(0 0 0 / 50%) would be read as opaque, its alpha taken as 50
  if (/rgba?\([^)]*\//i.test(plain)) {
    skipped.add(`space-separated colour in ${property}: ${plain}`);
    return [];
  }

  // Froala picks these classes itself after measuring a dark content background, so their colours already fit it
  if (COLOR_LONGHAND[property] && context.subjects.some((name) => name.endsWith('--on-dark'))) return [];

  const themer = themerOf(property);
  const result = themer ? themer(property, plain, context) : [];

  return result.map((entry) => ({ ...entry, value: entry.value + important }));
}

/** The classes and the tag of the element a selector part styles, i.e. of its last compound. */
function subjects(part) {
  const last = part.split(/\s*[\s>+~]\s*/).pop();
  return [...(last.match(/\.[\w-]+/g) ?? []), last.match(/^[a-z][\w-]*/i)?.[0] ?? '*'];
}

/**
 * Froala puts the theme class on the box, the toolbar and each popup, tooltip, overlay and modal (THM-3 in #27), and
 * those elements carry more classes of Froala's, such as fr-desktop. So every selector is written twice, once with the
 * theme class on its first element and once with it on an ancestor.
 */
function themedSelector(selector) {
  return splitTopLevel(selector, ',')
    .map((part) => part.trim())
    .flatMap((part) => {
      const type = part.match(/^(\*|[a-z][\w-]*)/i)?.[0] ?? '';
      const onElement = (type === '*' ? '' : type) + '.' + THEME + part.slice(type.length);
      return [onElement, `.${THEME} ${part}`];
    })
    .join(',\n');
}

const output = [];
let openMedia = null;

// longhand|class (or tag) for every value this theme sets, so a value it leaves alone is only repeated where it overrides one
const themedValues = new Set();

const css = source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/@charset[^;]*;/, '');

for (const rule of styleRules(css)) {
  const parts = splitTopLevel(rule.selector, ',');
  const classes = parts.flatMap(subjects);
  const context = {
    subjects: classes,
    // only where every part is a focus state, so a hover sharing the rule is not given the focus ring
    focus: parts.every((part) => /:focus/.test(part)),
    content: /\.fr-view|\.fr-element/.test(rule.selector) && !/\.fr-(toolbar|popup|modal)/.test(rule.selector),
    onTone: rule.declarations.some(
      ([property, value]) => /^background/.test(property) && (value.match(COLOR_TOKEN) ?? []).some(isTone)
    ),
  };

  const declarations = [];
  for (const entry of rule.declarations.flatMap((declaration) => themedDeclaration(declaration, context))) {
    const keys = classes.map((name) => `${entry.longhand}|${name}`);
    if (entry.reset) {
      if (!keys.some((key) => themedValues.has(key))) continue;
    } else {
      keys.forEach((key) => themedValues.add(key));
    }
    declarations.push(`${entry.longhand}: ${entry.value}`);
  }
  if (declarations.length === 0) continue;

  if (rule.media !== openMedia) {
    if (openMedia) output.push('}\n');
    if (rule.media) output.push(`${rule.media} {\n`);
    openMedia = rule.media;
  }

  const indent = openMedia ? '  ' : '';
  output.push(
    themedSelector(rule.selector)
      .split('\n')
      .map((line) => indent + line)
      .join('\n') +
      ' {\n' +
      declarations.map((declaration) => `${indent}  ${declaration};\n`).join('') +
      `${indent}}\n`
  );
}
if (openMedia) output.push('}\n');

// The rules read the private copies, which vcf-froala-theme-vaadin.css resolves on the editor's own elements. Read
// there, a Lumo variant on any ancestor reaches them, not only one on html.
fs.writeFileSync(
  target,
  `/* Generated from Froala ${version}'s froala_editor.pkgd.css by component/src/theme-generator/generate-vaadin-theme.js.
   Do not edit it. Run the generator again instead. The properties it uses are set in vcf-froala-theme-vaadin.css. */

` + output.join('\n').replaceAll('var(--vcf-froala-', 'var(--_vcf-froala-')
);
console.log(`wrote ${target}`);
skipped.forEach((entry) => console.warn(`skipped ${entry}`));
