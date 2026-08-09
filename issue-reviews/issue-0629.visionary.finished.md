# Issue #629: TASK-C561-1: Falstad's compact text format parses as untrusted input — a non-XML, non-JSON source refuses loudly on malformed lines
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is actually for

Read against the arc, #629 is not "a Falstad parser". It is the *input half* of
CAP-29's (#513) thesis. #556 generalizes the **output** of importing — construct,
disposition, location, explanation. Nothing in the tracker generalizes the
**input**: bounded reading of untrusted foreign bytes, a location type that
survives across formats, refuse-before-build. #629 is where that substrate gets
invented, and as written it gets invented Falstad-shaped, inside a 1 mw task,
for the third time in the tree (`FileAbstractor` for `.jls`, `#451`'s XML config
for `.circ`, this).

The trajectory is unambiguous about how JLS treats this class of work. There is
already one importer in the shipped tree — `src/jls/hdl/imp/NetlistImporter.java`
— and its shape is exactly what #451 tells the `.circ` reader to copy: aggregate
every problem, refuse before building anything, emit through **one** save-text
emitter, hand back an `ImportSummary`. There is already one bounded-input
discipline: `FileAbstractor.MAX_CIRCUIT_TEXT_BYTES` (`src/jls/FileAbstractor.java:65`)
enforced through `BoundedInputStream` (`:350`) so a hostile compressed stream
cannot inflate past the cap. There is already one diagnostic vocabulary:
`LoadError` with a fixed taxonomy carrying category + line + element + hint,
published through `JLSInfo` so every front end says the same thing
(ARCHITECTURE.md, "Error-reporting contracts"). #629 names none of these three.

## Reframing 1 — cut at "untrusted source", not at "Falstad"

The elegant seam is a `jls.imp` substrate that #629 is the first *client* of, not
the owner of:

- **`SourceRef`** — a sealed type: `LineRef(line, col)` for Falstad and `.jls`,
  `PathRef(element chain)` for `.dig`, `PointerRef(JSON pointer)` for `.cv`.
  This is precisely #556's `location` field. AC-4 as written ("a source location
  per element expressive enough for #556's `location` field on a line-oriented
  format") builds the line case first and lets the contract calcify around it.
  That is the *opposite* of what the issue claims to prove.
- **`ImportSource`** — declared budgets (bytes, records, record length, nesting
  depth) over a byte stream, lifting `BoundedInputStream`'s pattern rather than
  re-deriving it. AC-1's "unbounded line length / unbounded line count" and
  AC-3's "no partial model" are then properties of the substrate, asserted once.
- **A field-domain table**, not per-format overflow code. AC-2's real content is
  not `Integer.parseInt` overflow (the JDK already refuses that); it is *domain*
  bounds — coordinate range, bit width, fan-in — and those belong in the same
  table as the element grammar, one row per field.

Consequence: #629 shrinks to the one thing only it can do — a **machine-readable
Falstad element-code table** (code → arity → field domains → disposition) plus a
generic driver — and the 1 mw band starts pricing knowledge acquisition about a
foreign format rather than re-litigating bounds checks.

## Reframing 2 — the 629/631 boundary cuts across the knowledge, not along it

"Parsing only. The logic-subset mapping is TASK-C561-2." For a *positional*
format that boundary does not exist. Falstad's lines are `<code> <x1> <y1> <x2>
<y2> <flags> <params…>`; the field count and the field meanings are a function of
the element code. You cannot validate a line's arity without the mapping table,
so either #629 ships a validator that checks "is it a number" and calls it
hardening (AC-1 satisfied, nothing actually validated), or the code table is
written twice. Better line: **#629 owns the table and the table-driven driver**
(the reviewable *document* #451 argued for, pinned in both directions like
`docs/extension-points.md` / `ExtensionPointCatalogTest`), **#631 owns
realization into elements and nets**, **#633 owns the report**. Same three tasks,
seam moved to where the knowledge actually clusters.

## Reframing 3 — AC-1's unknown-element rule contradicts the capstone

**I am explicitly disregarding AC-1's "unknown element codes" clause.** AC-1
makes an unknown element code a loud refusal. #561 AC-1 and CAP-29 AC-1 require
the opposite: an unrepresentable construct is *named, located, explained* and the
circuit still opens. Falstad's element codes are defined by another project's
source and grow with it; a refusal rule means a JLS release stops opening
circuits the week Falstad adds an element — stranding exactly the instructors the
capstone exists to unstrand. #451 already recorded the correct default for the
sibling format: an unrecognized `.circ` schema version "proceeds and is reported,
because refusing on a version bump would strand exactly the users this feature
exists for". Apply it here. The refusal set should be **unparseable structure**
(wrong arity for a *known* code, out-of-domain numeric, budget exceeded); an
unknown code is a reported loss keyed by code and line. That change also gives
#633 a second, sharper loss category than "analog", which is the only one it has
today.

## Reframing 4 — refuse in the vocabulary the project already has

AC-1's "bounded diagnostic naming the line" is, field for field,
`LoadError(MALFORMED, detail, line, element, hint)`; the budget cases are
`LIMIT_EXCEEDED`. Restating AC-1 as "refuses through the existing `LoadError`
taxonomy, published via `JLSInfo.setLoadError`" is strictly stronger (it pins the
batch `jls: error:` line and the GUI dialog to the same text as every other bad
file) and is less code. Otherwise JLS ends up with three error dialects for one
user action: loader errors, import refusals, loss reports.

## Reframing 5 — the entry point, and the vector nobody listed

Falstad circuits do not exist as files in the wild. They exist as
`circuitjs.html?ctz=…` links in lab handouts and as clipboard text from "Export
as Text". A File→Open path over a `.txt` satisfies every AC here and does not
fire the migration lever #510 priced. Make the entry point **paste text or URL**,
decode the fragment, decompress under the existing bounded-stream discipline.
Note what falls out: AC-1 enumerates four attack vectors and **decompression
bombs are not among them**, because the issue assumed plain text — yet the real
transport is compressed. Reusing `BoundedInputStream` closes a vector the ACs
never contemplated, from code that already ships.

## Reframing 6 — assert the property, keep the vectors as seeds

"One test per attack vector" enumerates only the vectors the author imagined.
The tree already runs `ContainerMutationFuzzTest` (deterministic seeds,
dependency-free) and `GenerativeRoundTripFuzzTest`. One property — *for any byte
string the reader returns either a complete model or a bounded diagnostic, within
declared budgets, never a partial model* — subsumes AC-1/2/3 and catches the
unimagined case. Keep the four named vectors as regression seeds.

## The claim I do not accept

"Being neither XML nor JSON is the point: this is the format that proves #556's
report contract is genuinely format-agnostic rather than markup-shaped." Falstad
text is line-oriented, and so is `.jls` itself — this is home turf, not the
falsifying case. A `location` that works for lines proves nothing about a
location that must address a nested node. The generalization is falsified (or
not) by #558 (`.dig` XML) and #559 (`.cv` JSON). Two honest resolutions: drop the
proof claim and keep Falstad as the cheapest end-to-end walk, or — better —
require `SourceRef` to be designed against one nested format's needs before #629
lands. Then the demo slice really does de-risk the capstone instead of
advertising that it does.

## Net

The work is real and correctly ordered after #314 (fail-loud loader): a parse
must exist, and it must be bounded. What is wrong is the altitude. As written,
#629 spends a 1 mw budget writing the fourth private copy of "read hostile bytes
carefully" and hard-codes the one deliverable that must outlive it (`location`)
to the shape of the easiest format, while a rule inside AC-1 quietly reverses the
capstone's central promise. Move the substrate out, move the seam to the code
table, make unknown codes losses, refuse through `LoadError` — and the task gets
smaller, the three siblings get cheaper, and #558/#559 inherit something instead
of starting over.
