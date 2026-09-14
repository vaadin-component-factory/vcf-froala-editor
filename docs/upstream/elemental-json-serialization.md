# elemental.json serialization and key ordering

Measured facts about `elemental.json`, verified by bytecode inspection rather than assumption.

Froala add-on value types (`FroalaOptions`, `FroalaToolbar`, `FroalaToolbarGroup`, …) hold
`elemental.json.JsonObject`/`JsonArray` fields or build them on demand. Verified 2026-08-27
against `gwt-elemental-2.8.2.vaadin2.jar` (the jar that actually provides
`elemental.json.impl.JreJsonObject` on this project's classpath):

- **`JreJsonObject`'s backing `map` field is `transient`, but it is genuinely `Serializable`
  anyway** — it has custom `writeObject`/`readObject` that serialize via `toJson()` (a JSON
  string) and reparse on the way back. So a class whose only state is JSON values (like
  `FroalaOptions.values`) really does survive Vaadin session serialization. Confirmed end to
  end with a real `ObjectOutputStream`/`ObjectInputStream` round trip, not just by reading the
  bytecode.
- **`JreJsonObject.toJson()`/stringify sorts purely-numeric string keys ascending and puts them
  before all other keys**, mirroring plain JS object key semantics (`stringifyOrder` matches
  `\d+`). `.keys()`/insertion order (backed by a real `LinkedHashMap`) is unaffected — only the
  serialized/stringified form reorders. Practical effect: a `FroalaToolbarGroup` named e.g.
  `"1"` or `"2"` jumps ahead of alphabetically-named groups in the JSON sent to Froala,
  regardless of the order passed to `FroalaToolbar.ofGroups(...)`. Not a bug worth fixing — a
  browser would apply the identical reordering to the same JSON after `JSON.parse`, so avoiding
  it in our serialization wouldn't change what Froala actually sees. Worth a NOTE only if a
  reviewed API explicitly promises "in the order given" for something keyed by
  caller-supplied strings.

**How to check this quickly next time**: no `elemental-json` artifact exists standalone on this
project's classpath — the impl classes ship inside `gwt-elemental-2.8.2.vaadin2.jar`. Find it
with `unzip -l` grep for `elemental/json/impl/JreJsonObject.class` across `~/.m2`, then
`javap -p -c` the class file to read `writeObject`/`readObject`/field types directly rather than
guessing from behavior.
