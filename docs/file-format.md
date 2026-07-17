# The JLS circuit save format (`.jls`)

<!-- Issue #79: a normative specification of the save format, derived
     from the writer and loader code. Anchors below cite the code that
     each rule is derived from (file:line as of the commit that added
     this document; line numbers drift, symbol names do not). -->

This document is the normative specification of the `.jls` circuit
save format, up to and including **format version 2**. It is written
so that a third party
can implement a reader or writer from this document alone, without
consulting the JLS source. Where the words MUST, MUST NOT, SHOULD and
MAY appear, they are used as in RFC 2119.

The format is deliberately simple: line-oriented UTF-8 text inside a
compressed container. The text is diffable and hand-editable; the
container is what lives on disk under the `.jls` name.

Conformance targets:

- A **reader** is conformant if it accepts every file this document
  permits a writer to produce, and rejects (with a diagnostic, never a
  misparse) files that declare a newer format version.
- A **writer** is conformant if everything it emits is accepted by the
  reference reader (`jls.Circuit.load`) and matches the grammar below.
  The stricter *canonical layout* rules marked "canonical" describe
  what JLS itself writes; a conformant writer SHOULD follow them so
  its files diff cleanly against JLS saves, but a reader MUST NOT
  require them.

Validated against reality by `test/jls/FileFormatSpecTest.java`, which
fails when this document and the code drift apart.

---

## 1. Containers

<!-- FileAbstractor.java: openCircuit (sniffing order), writeCircuit
     (current save container), readXZ/readZip/readText; README
     "Circuit file compatibility" section. -->

A `.jls` file is one of three containers, distinguished by **content
sniffing, never by file name**. A reader MUST try, in order:

1. **XZ-compressed text** — what current JLS writes by default. The file is a
   standard XZ stream (magic bytes `FD 37 7A 58 5A 00`, "ý7zXZ")
   whose decompressed payload is the circuit text.
2. **Zip archive** — the original JLS container: a zip holding the
   circuit text in a single entry named `JLSCircuit` (legacy editor
   checkpoints used the entry name `JLSCheckpoint`; a reader MUST
   accept either).
3. **Plain text** — the uncompressed circuit text itself.

The circuit text is UTF-8 in every container.

A conformant writer MUST write either the XZ container or the
plain-text container; the zip container is read-only legacy. JLS
writes XZ by default and plain text on explicit request (the Save As
file-type choice or the `-savetext` flag, issue #129) — plain text is
the interchange form for version control and for readers without an
XZ decoder.
Writes go to a temporary file that is atomically renamed over the
target, so a crash mid-write never leaves a truncated file where a
complete one used to be; readers therefore MAY assume a file is
either absent or complete.

**Resource limits** (issue #38): circuit files are exchanged between
untrusting parties by design, so a reader MUST bound container
expansion. JLS refuses any container whose text payload exceeds
64 MiB (`FileAbstractor.MAX_CIRCUIT_TEXT_BYTES`), whatever the
compressed size claims.

**Checkpoint files** (`.jls~`) are ordinary saves in the same
containers, written by the editor for crash recovery.

---

## 2. Lexical structure

<!-- Circuit.java: load(Scanner,int) and loadElementItems - the reader
     is java.util.Scanner-token based; String/probe values are read
     with findInLine(".*"), which is the one line-sensitive rule. -->

The circuit text is a sequence of whitespace-separated **tokens**
(maximal runs of non-whitespace characters), with one exception: a
**quoted value** (the value part of a `String` or `probe` item) is
the remainder of its line, located by its first and last `"`
characters. Consequently:

- Outside quoted values, line breaks are just whitespace. The
  canonical layout below is one record per line, but a reader MUST
  NOT require it (except where a quoted value forces it).
- A quoted value MUST begin and end on the line of its item; embedded
  newlines are escaped (§6). Text before the first `"` and after the
  last `"` on that line is ignored by the reader; a writer MUST NOT
  emit any.

Keywords (`FORMAT`, `CIRCUIT`, `ENDCIRCUIT`, `ELEMENT`, `END`, and
the item kinds of §5) are case-sensitive. Integer tokens are optional
sign plus decimal digits; `int` fits a Java 32-bit int, `long` a
64-bit long, and `Int` is unbounded (BigInteger).

**Canonical layout** (what JLS writes): one record per line;
`FORMAT`, `CIRCUIT`, `ELEMENT`, `END`, `ENDCIRCUIT` at column 0;
attribute items indented one space (StateMachine state sub-records
use two and three spaces, purely cosmetically). Load-error line
numbers assume this layout.

---

## 3. Grammar

```
file          = format-line circuit-block

format-line   = "FORMAT" version              ; §4; absent in legacy files
version       = decimal integer               ; 1 or 2 in current files

circuit-block = "CIRCUIT" name { element } "ENDCIRCUIT"
name          = token                         ; §3.1

element       = "ELEMENT" tag { item } "END"
tag           = token                         ; §7

item          = int-item | long-item | bigint-item | string-item
              | ref-item | pair-item | probe-item
              | circuit-block                 ; SubCircuit elements only

int-item      = "int"    attr-name integer
long-item     = "long"   attr-name integer
bigint-item   = "Int"    attr-name integer    ; unbounded (BigInteger)
string-item   = "String" attr-name quoted     ; quoted: §2, escaping: §6
ref-item      = "ref"    attr-name integer    ; element-id reference, §8
pair-item     = "pair"   integer integer
probe-item    = "probe"  integer quoted       ; WireEnd only
attr-name     = token
```

A reader encountering anything else where an item kind is expected
MUST fail the load: unknown item *kinds* are a format extension and
require a version bump (§9). Unknown *attribute names* inside a known
kind are the opposite — see §5.

After `ENDCIRCUIT` of the top-level circuit the file MUST end;
JLS's one-shot loaders reject trailing content explicitly.
<!-- JLSStart.loadCircuitHeadless: "extra content after the
     ENDCIRCUIT trailer". -->

### 3.1 Names

The top-level `CIRCUIT` name token is informational: JLS names the
circuit after the file, not the token (the token is still required).
Nested `CIRCUIT` names (subcircuits) are meaningful and MUST match
`letter (letter | digit | "_")*` (`Util.isValidName`); the same rule
applies to the base name of the `.jls` file itself.

---

## 4. The FORMAT header and version negotiation

<!-- Circuit.java: FORMAT_VERSION, readFormatHeader, save. -->

The first token of a current file is `FORMAT`, followed by the format
version as a decimal integer. Version-1 files begin exactly:

```
FORMAT 1
```

Rules:

- **Legacy files have no header** and are implicitly version 0. A
  reader MUST accept a file whose first token is `CIRCUIT` and treat
  it as version 0. Version 0 and version 1 differ *only* in the
  header's presence. Version 2 differs from version 1 only in that
  the `orient` attribute of `Binder` and `Splitter` elements may be
  `UP` or `DOWN` as well as `LEFT`/`RIGHT` (issue #124) — a
  version-1 reader would silently load such a group horizontal,
  which is exactly the mis-load the header exists to prevent.
- A reader MUST accept any declared version less than or equal to
  the newest it implements, and MUST refuse a greater one with an
  explicit "this file needs a newer reader" diagnostic — refusing is
  the point: version negotiation exists so a newer format can never
  be silently misread as an older one.
- A negative or non-numeric version is malformed; refuse the load.
- The header appears **exactly once, at the top of the file**. Nested
  subcircuit `CIRCUIT` blocks (§7, `SubCircuit`) MUST NOT carry their
  own `FORMAT` line — a file states its format version once, and the
  reference reader rejects a `FORMAT` token inside an element body as
  an unknown item kind.
- A writer MUST emit the header with the highest version whose
  features the file uses: `FORMAT 2` when any group in the file
  (including inside nested subcircuit blocks) is vertically
  oriented, otherwise `FORMAT 1` — so files that avoid newer
  features stay readable by older JLS versions.
  <!-- Circuit.formatVersionNeeded, Element.saveFormatVersion. -->

---

## 5. ELEMENT records and attributes

Each element of a circuit is one `ELEMENT` record. The tag names the
element type (§7). The items between the tag and `END` set the
element's attributes; their order is not significant to the reader,
but the canonical writer order is: the base attributes, then the
type's own attributes, in each type's historical order.

**Base attributes** (every element type; all `int`):
<!-- Element.java: BASE_ATTRIBUTES, in save order. -->

| attribute | meaning | omitted when |
|---|---|---|
| `id` | save-scoped element identity (§8) | never |
| `x`, `y` | grid position | never |
| `width`, `height` | drawn size | the type recomputes size on load |
| `fixed` | element is not editable | editable (any value ⇒ fixed) |
| `trpos` | position in the signal-trace window | `-1` (not traced) |

**Unknown attribute names are silently ignored.** The reader offers
each item's name and value to the element; if no declared attribute
consumes it, the value is dropped without error.
<!-- Element.setValue: the attribute loop simply falls through. -->
This is the format's main forward-compatibility valve (§9): a newer
JLS may add attributes to an element without breaking older readers —
at the cost that the older reader silently drops that data. Unknown
*item kinds* and unknown *tags*, by contrast, are hard errors.

Values may exceed an attribute's valid range only syntactically; the
element rejects out-of-range values during load with a diagnostic
(e.g. a negative `bits`). This document does not enumerate every
per-type attribute; the per-type constraint of record is each element
class's `savedAttributes()` declaration (or handwritten
`save`/`setValue` pair, noted in §7).

---

## 6. String escaping

<!-- Writer: Attribute.StringAttribute.save (and the identical
     handwritten sites, e.g. Memory.save). Reader:
     Circuit.unquoteAndUnescape - a single left-to-right scan;
     issue #53 records why sequential replace() passes were wrong. -->

A writer MUST transform a string value in this order, then wrap it in
`"`:

1. every `\` becomes `\\`
2. every `"` becomes `\"`
3. every newline (U+000A) becomes `\n`

A reader MUST take the text between the first and last `"` of the
line and decode it in **a single left-to-right scan** — never as
sequential whole-string replacements, which corrupt adjacent escapes:

- `\n` → newline; `\\` → `\`; `\"` → `"`
- `\` followed by any other character: keep the backslash verbatim
  and process the next character normally
- a trailing lone `\` is kept verbatim

The escaping round-trips every string. Carriage returns are not
escaped; writers MUST NOT put unescaped line terminators inside a
quoted value.

---

## 7. Element type tags

<!-- Loader routing: Circuit.load resolves a tag by
     Class.forName("jls.elem." + tag) and instantiating the
     (Circuit) constructor; unknown tags fail the load with
     UNKNOWN_ELEMENT. Writers: each element class's save() prints
     "ELEMENT <tag>"; the gate family prints its Kind saveName
     (Gate.java, "must match the class name"). -->

A tag is resolved by the reference reader to the Java class
`jls.elem.<tag>` — the tag namespace is currently the loader's class
namespace, so **tags are case-sensitive and exactly the simple class
names below**. A reader implementing this specification only needs
this table; a tag not in it (and not a documented later addition)
MUST fail the load with a diagnostic naming the tag, and SHOULD
suggest that the file may need a newer reader.

Version-1 and version-2 writers emit exactly these 32 tags:

| tag | element | notes |
|---|---|---|
| `Adder` | ripple adder | |
| `AndGate` | AND gate | |
| `Binder` | wire bundler | `pair` items: (input index, bundle bit); `orient` values `UP`/`DOWN` require version 2 (§4) |
| `Clock` | clock generator | |
| `Constant` | constant driver | value is an `Int` item |
| `Decoder` | decoder | |
| `DelayGate` | delay buffer | |
| `Display` | value display | |
| `Extend` | 1-to-n bit extender | |
| `InputPin` | circuit input | |
| `JumpEnd` | named-net receiver | |
| `JumpStart` | named-net source | |
| `Memory` | RAM/ROM | initial contents: `String init` (raw dump) **or** `String initrle` (run-length encoded); §9 caveat |
| `Mux` | multiplexer | |
| `NandGate` | NAND gate | |
| `NorGate` | NOR gate | |
| `NotGate` | inverter | |
| `OrGate` | OR gate | |
| `OutputPin` | circuit output | |
| `Pause` | simulation pause control | |
| `Register` | latch / D flip-flop | |
| `ShiftRegister` | combinational barrel shifter | despite the name, stateless; tag and attributes match the bsiever-fork 4.6 element (issue #122) |
| `SigGen` | signal generator | |
| `Splitter` | wire unbundler | `pair` items: (output index, bundle bit); `orient` values `UP`/`DOWN` require version 2 (§4) |
| `StateMachine` | finite state machine | state list encoded as ordered `String state`/`output`/`trans`/`next` items with interleaved `int`/`long` values; the item *sequence* is significant for this type only |
| `Stop` | simulation stop control | |
| `SubCircuit` | nested circuit instance | body contains one nested `CIRCUIT` block (no `FORMAT` line) |
| `Text` | annotation text | |
| `TriState` | tri-state buffer | |
| `TruthTable` | truth table | `String input`/`output` items name columns; `pair` items carry cells (row, value) in row-major order |
| `WireEnd` | wire endpoint | see below |
| `XorGate` | XOR gate | |

Additional notes:

- **Wires are not saved as elements.** Each `WireEnd` records the put
  it attaches to (`String put`, `ref attach <element-id>`) and one
  `ref wire <wire-end-id>` per wire segment leaving it; the reader
  reconstructs wires from these mutual references. A wire's probe is
  saved on its ends as `probe <other-end-id> "<probe-name>"`; a
  tri-state net marks its ends with `int tristate 1`.
- **Gate `pair`/sequence-free types**: for every type except
  `StateMachine`, items are order-independent; `pair` items are
  applied in file order.
- **`TestGen`** resolves through the same routing (it is a loadable
  class) but is never written by a conformant writer; it exists for
  the batch test facility. Readers MAY reject it.
- `SubCircuit` nested blocks recurse: the nested circuit uses this
  same grammar minus the `FORMAT` line, and its elements may include
  further `SubCircuit`s.

---

## 8. Ids and references

<!-- Circuit.save assigns ids 0..n-1 at save time; the loader's
     elementMap maps id -> element; ref items resolve after load. -->

Element `id`s are assigned at save time, dense from 0, **scoped to
their own `CIRCUIT` block** — a nested subcircuit restarts at 0. A
`ref` item names another element of the *same* block by id. Readers
MUST resolve refs only within the block; writers MUST NOT emit
forward ids that don't exist in the block. Ids carry no meaning
beyond reference resolution and are not stable across saves.

---

## 9. Evolution policy

<!-- Issue #79 §7 and the README forward-compatibility caveat
     (issue #47). Circuit.FORMAT_VERSION is the reader/writer bound. -->

The format version is bumped when — and only when — a change could
make an **older reader misparse or silently mis-load** a new file.
Concretely:

- **No bump needed** (older readers keep working, possibly dropping
  the new data with the caveat below):
  - adding a new attribute (any existing item kind) to an existing
    element type — older readers silently ignore unknown attribute
    names (§5);
  - adding a new element *type* — older readers fail loudly with
    "no element type named X", which is detectable, not a misparse.
- **Bump required**:
  - a new item kind (anything other than `int`, `long`, `Int`,
    `String`, `ref`, `pair`, `probe`) — older readers report it as a
    malformed file, and per §3 that is the intended failure mode, but
    the file MUST declare the version that introduced the kind so
    current readers can say "needs a newer JLS" instead;
  - any change to the block structure, escaping rules, or the meaning
    of an existing record.
- A reader MUST keep accepting **all older versions** (including the
  headerless version 0) indefinitely; version support is only ever
  added, never removed.

Version history:

- **0**: headerless legacy files (everything before the header).
- **1**: version 0 plus the `FORMAT` header itself; no other change.
- **2**: version 1 plus vertical (`UP`/`DOWN`) `orient` values on
  `Binder`/`Splitter` (issue #124). A version-1 reader silently
  ignores the unknown value and loads the group horizontal — a
  mis-load, hence the bump. Writers emit `FORMAT 2` only for files
  that contain a vertical group (§4).

**The silent-drop caveat** (issue #47): "no bump needed" trades
compatibility for silent data loss in *pre-versioning* readers. The
standing example is `Memory`'s `initrle` attribute: JLS 4.1 loads
such files but silently drops the memory's initial contents (it
ignores the unknown `initrle` name and never sees an `init`). Files
that must round-trip through JLS 4.1 should avoid Memory initial
values. This class of loss is exactly what the `FORMAT` header ends:
a post-versioning reader confronted with a file declaring a newer
version refuses it outright instead of loading a subtly wrong
circuit. Writers SHOULD therefore prefer a version bump over an
"ignorable" attribute whenever dropping the attribute would change
simulation behavior.

Historical note: tags are Java simple class names today (§7), which
is why the `edu.mtu.cs.jls` → `jls` package rename did not break the
format. Decoupling tags from class names via a registry with an alias
table is planned (issue #79 method, item 3); when it lands, this
section will record every historical alias.

---

## 10. Worked example

A complete version-1 file (plain-text container) with one constant
driving an output pin through a wire:

```
FORMAT 1
CIRCUIT example
ELEMENT Constant
 int id 0
 int x 60
 int y 60
 int width 24
 int height 24
 Int value 5
 int base 10
 String orient "RIGHT"
END
ELEMENT OutputPin
 int id 1
 int x 180
 int y 60
 int width 48
 int height 24
 String name "out"
 int bits 3
 int watch 0
 String orient "RIGHT"
END
ELEMENT WireEnd
 int id 2
 int x 84
 int y 72
 int width 8
 int height 8
 String put "output"
 ref attach 0
 ref wire 3
END
ELEMENT WireEnd
 int id 3
 int x 180
 int y 72
 int width 8
 int height 8
 String put "input"
 ref attach 1
 ref wire 2
END
ENDCIRCUIT
```
