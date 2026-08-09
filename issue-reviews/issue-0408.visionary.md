# Issue #408: TASK-0004: a fixture carrying an attribute no element declares fails the build instead of loading in silence
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## What this issue is really for

Strip the apparatus and #408 wants one thing: **the diagnostic #404 adds must not
be able to rot back into silence, and the loss class must stay legible to a
human.** That goal is right, it is aligned with the project's arc, and it is
worth paying for. Everything below is about the artifact chosen to buy it.

The artifact is: eight hand-written `.jls` files, a bespoke tab-separated
`expected.txt` with a hand-written parser, and two bidirectional
directory/manifest sync tests. That is a **hand-maintained table keyed by
element type and loader arm**, added to a codebase whose entire recorded
direction is the elimination of exactly that artifact class.

## The trajectory this pulls against

`ARCHITECTURE.md` §"The save/load pipeline" and `docs/grand-architecture.md` §3
name the move: `ElementRegistry`/`ElementType` (#78, **shipped** — the issue and
`ARCHITECTURE.md`'s "Adding an element today" section are both stale on this)
is "the seed of the plugin mechanism … a descriptor+factory table with a
build-enforced totality test," and #315 exists to make every registry-keyed
table total by construction. `ElementType.create(Circuit)`
(`/home/user/JLS/src/jls/elem/ElementType.java:125`) is public precisely so
tests can enumerate types.

The project already has an established idiom for "this property must hold for
every element type," and it is not a fixture directory:

- `/home/user/JLS/test/jls/elem/PinFaceContractTest.java:295` — loops
  `ElementRegistry.all()`, and pins the *exception sets*
  (`KNOWN_UNINITIALIZABLE`, `KNOWN_PUTLESS`, `KNOWN_OFF_EDGE`) with
  `assertEquals` in both directions, so a new type joins the property or fails
  the build.
- Same shape at `/home/user/JLS/test/jls/elem/CapabilityInterfaceTest.java:125`,
  `:196`, `:238`, `/home/user/JLS/test/jls/edit/PaletteContractTest.java:51`,
  `/home/user/JLS/test/jls/JlsModulesBootTest.java:38`.

#408's P5 ("a fixture with no manifest row fails the run") is a hand-rolled
re-implementation of the totality assertion those tests get for free from the
registry. The instinct is right; the mechanism is one generation behind the
codebase it is being added to.

## Where the corpus duplicates its own prerequisite

#404 §9 already ships, in `LoadErrorReportingTest`:

- `unconsumedAttributeOnEveryItemKindIsReported()` — one case per item kind,
  explicitly "the assertion that all five call sites were converted";
- `wireEndHandwrittenNamesAreStillConsumed()` — the `WireEnd` negative control;
- P4 over the three tracked fixtures — the zero-false-positive check.

#408's fixtures 1–5 (`unknown-{int,long,bigint,string,ref}.jls`) and fixture 8
(`wireend-handwritten-names.jls`) are those same three tests re-expressed as
files. Note also that `int` and `ref` reach the *same* Java overload
(`Circuit.java` passes an `int` in both arms), so two of the five fixtures
differ only in the call site they enter, which is a `Circuit`-internal fact a
unit test states better than a file does. After removing the duplicates, #408's
genuinely new content is: the `trpo`/`trpos` misspelling case, the docs pointer,
and the build-fails-on-unexpected-fixture ratchet.

## Two fixtures are fiction

`initrle-on-a-gate.jls` and `sync-on-a-register.jls` are advertised as "the two
historical instances the format spec records." They are not. Current JLS
*consumes* both names on `Memory` — `/home/user/JLS/src/jls/elem/Memory.java:383`
(`sync`) and `:420` (`initrle`), hand-written `setValue` arms. To make them drop,
the task must put them on the wrong element type, producing a file **no version
of JLS ever wrote**. The historical loss (`docs/file-format.md` §9, README
"Forward-compatibility caveat") is a *cross-version* property: an older reader
meeting a newer writer's attribute. A synthetic same-version file cannot witness
it. These two fixtures carry evocative filenames and zero discriminating power
beyond fixture 1, and they will teach a future reader something false about what
the corpus proves.

The honest witness for that property exists and is deferred: `test/fixtures/
legacy-4.1/` contains only a `README.md` (#56), and `test/fixtures/
fork-4.6-shiftregister.jls` is the single real cross-lineage file in the tree.
**That** corpus is non-substitutable — it is the only thing that can answer "does
the diagnostic fire on files instructors actually have?" — and it is exactly what
this task does not build.

## The alternative design

I am explicitly disregarding §7.6 (the manifest format), P5 and both manifest
sync directions, and the five per-arm fixtures. Replace them with three derived
properties and one golden idiom the project already owns.

**A1 — Writer/reader agreement as the negative control, total over the
registry.** For every `ElementRegistry.all()` type: `type.create(circuit)`,
initialize, `save`, reload the saved text, assert **zero**
`UNCONSUMED_ATTRIBUTE`. Pin the uninitializable set both ways, exactly as
`PinFaceContractTest` does. This covers every hand-written `setValue` in the
tree — `Memory`, `StateMachine`, `Group`, `TruthTable`, `SubCircuit`, `Pin`,
`JumpEnd`, `State`, `Extend`, `WireEnd` — not just the one `WireEnd` fixture, and
it self-maintains: a new element type joins the property the day it is
registered. It also does double duty, because a name an element *writes* that
nothing consumes is a real defect (the "written but never read" bucket #319
owns), and this property finds it. `AllElementsRoundTripTest` is the natural
host; the marginal cost is roughly one assertion.

**A2 — Mutation as the positive oracle.** Take known-good circuit text, rename
exactly one attribute *name* to a token no element declares, load, assert exactly
one diagnostic naming that token at the mutated line. The expectation is
**computed from the mutation**, never transcribed. Run it across every item kind,
every element in the everything-fixture, and seeded random circuits. The in-tree
precedent is already dependency-free and deterministic:
`/home/user/JLS/test/jls/GenerativeRoundTripFuzzTest.java` (seeded
`java.util.Random` over the save-format grammar, jqwik rejected by policy) and
`ContainerMutationFuzzTest`. Coverage is strictly larger than seven fixtures, and
§11's disclosed "fixture bias: all gates and pins" threat **disappears** rather
than being documented.

**A3 — If files are still wanted, use goldens, not a TSV.** JLS has a golden
culture (`BatchSimulationGoldenTest`, `VcdExportGoldenTest`,
`SequentialGoldenTest`, `RiscvCpuGoldenTest`). A `<fixture>.report` golden beside
each fixture is reviewable in a diff, regenerable through the path the project
already uses, and makes "fixture with no expectation" fail as a missing golden —
killing the parser, the comment convention, P5, and both sync tests at once.
Inventing a ninth in-tree data format, in a repository whose `docs/file-format.md`
exists because ad-hoc line-oriented formats accrete cost forever, is the wrong
instinct in this codebase specifically.

**A4 — Assert structure, not prose.** §11 admits the manifest cannot be defended
against #404 rewording its message. That is self-inflicted: #404 §7.6 promises a
`LoadError` carrying `category`, `line`, `element`, and `detail`. Assert those
fields; assert the rendered English exactly once, in #404's own suite, so a
wording change fails in one place instead of eight rows. The seam to cut along is
**structured miss data**, and the corpus should never hold a copy of the prose.

**A5 — Fix `Circuit.lineNumber` instead of hedging around it.** §7.8/§11 treat
the `private static int lineNumber` (`Circuit.java:89`) as a threat to validity
with P6 as a guard, and offer "if it proves unstable, fix the static." Make that
unconditional and prior. A per-load counter held statically is the same defect
class `docs/grand-architecture.md` §3 names as JLS's central one ("`JLSInfo` is a
~640-reference public-static hub"), it will bite the collab trajectory (#163/#167)
the moment two loads are in flight, and it is a ten-line instance field change.
That is a better standalone task than half of this one.

## What survives

Keep, in a much smaller task: (1) the plain-text container decision — right, and
right for the stated teaching reason; (2) `misspelled-base-attribute.jls`
(`trpo` for `trpos`), the one fixture with genuine pedagogical value, because it
is the realistic hand-edit whose silent drop changes what the user sees; (3) the
`docs/file-format.md` §5 pointer; (4) redirect the remaining fixture budget at
#56's deferred legacy/fork corpus, asserted with A1/A2. Note also that a
file-based corpus need not be `blocked_by: [404]` at all — the fixtures load
clean today and can land first, with the assertions arriving when the diagnostic
does.

## The larger prize this issue is one instance of

#323 (`.circ` import) needs "what did the reader drop" as an observable event;
#319 needs it to decide format epochs; #404 invents the first instance of it. The
general object is a **structured load report** — a value type listing everything
the reader saw and did not keep: unknown attributes, tolerated-but-unmodelled
constructs, coerced values, dropped foreign-format constructs. Build that once,
and "the corpus" becomes "input files and their expected load reports," reusable
verbatim by #323's migration report, and #408 stops being a bespoke directory
with a bespoke manifest and becomes two rows in a shared, growing table.

## Verdict

**rethink.** The goal is correct and should be funded. The deliverable as
specified duplicates its own prerequisite's acceptance suite, encodes two
fixtures that cannot witness what they claim, and adds a hand-maintained
registry-keyed table plus a new in-tree file format at the moment the project is
retiring both. Rebuild it as: registry-driven writer/reader agreement (A1),
seeded name-mutation (A2), goldens if files are wanted (A3), structural
assertions (A4), an unconditional fix to the static line counter (A5), and one
readable misspelling fixture — with the fixture budget that remains spent on the
real cross-version corpus #56 deferred.
