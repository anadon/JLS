# Issue #621: TASK-C559-1: a .cv project parses as untrusted JSON into an in-memory CircuitVerse model, bounded in depth and size

- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated deliverable is "a hardened JSON parser for `.cv`". The actual end
it serves is one of JLS's genuine spine commitments: **a circuit file is
data, and data cannot reach your machine.** #38 established that for `.jls`;
CAP-16 (#311) makes it ship-blocking for `.circ` (KC-16-4); CAP-29 (#513)
AC-5 extends it to every new format. That premise is correct, load-bearing,
and worth defending. Nothing below argues against it.

What I am reframing is the route. The issue reads as if JLS has never parsed
untrusted JSON. It has, and the parser is good.

## The thing the issue never mentions

`src/jls/hdl/yosys/JsonValue.java` — 580 lines, shipped for #61, pinned by
`test/jls/hdl/yosys/JsonValueTest.java` (126) and `JsonValueCoverageTest.java`
(160). Its class javadoc opens: *"An immutable JSON value with a strict
parser… JLS deliberately carries no JSON library dependency… Netlists are
untrusted input, so nesting depth is capped rather than letting a hostile
file overflow the parse stack."*

Measured against this issue's own acceptance criteria:

| AC-1 requirement | Already shipped in `JsonValue` |
|---|---|
| depth bound, named limit in the diagnostic | `MAX_DEPTH = 200`, `:259-260` throws `"nesting deeper than " + MAX_DEPTH`; test `rejectsHostileNestingDepth` |
| duplicate keys | `:326-328` rejects; test `rejectsDuplicateMemberNames` |
| unexpected types | typed accessors throw on wrong kind; test `typedAccessorsGuardTheKind` |
| errors name what and where | 1-based line/column; test `errorsCarryLineAndColumn` |
| oversized document | **absent** — this is the one real gap |

AC-3 ("malformed or truncated refuses loudly, no partial model") is likewise
already covered by construction — `parse` returns a root or throws — and
pinned by `rejectsTrailingGarbage`, `rejectsUnterminatedString`,
`anUnterminatedObjectIsRejected`, `anUnterminatedArrayIsRejected`,
`aTruncatedUnicodeEscapeIsRejected`, and ten more.

A task written without this file in view will produce JLS's *second*
hand-rolled JSON parser, and will spend its 1 mw band arriving at code that
already exists. That is the whole of my objection.

## The honest residual, which is smaller and sharper than the issue thinks

Three real gaps, none of them a parser:

1. **No document size cap.** `JsonValue` bounds depth only. JLS's size
   discipline lives a layer away in `FileAbstractor`
   (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`, plus a bounded decompression
   stream at `:347-408` so a hostile XZ file cannot expand past the cap).
   That machinery is welded to `.jls` container sniffing and `LoadError`.
   Generalizing the *bounded read* — not the parser — is the actual work.
2. **Wrong home and wrong name.** The parser sits in `jls.hdl.yosys` and
   throws `NetlistFormatException`, whose javadoc talks about `write_json`
   schemas. Neither is true of a `.cv` file. It wants a neutral home
   (`jls.util.json` or under a shared `jls.imp`) and a neutral
   `JsonSyntaxException`, with `NetlistFormatException` kept as a thin
   adapter so #61's call site does not churn.
3. **A number policy that will actively break.** `JsonValue` *rejects*
   non-integer numbers — deliberately: *"they never occur in a netlist, and
   a bit index that arrived as `1.5` is a malformed file, not data to
   round."* CircuitVerse `.cv` carries element coordinates and canvas
   scale as floats. Reuse without noticing this fails on the first real
   project file. This is the one place where the issue's instinct to write
   something new is *nearly* right — and the correct fix is a per-consumer
   `numberPolicy` on a `JsonLimits` record (maxDepth, maxBytes,
   maxStringLength, numberPolicy), not a fork.

That is a package move, a limits record, a bounded reader, and one policy
knob. It is comfortably inside the 1 mw band with room left over, and it
makes #612 (`.dig` XML) and #629 (Falstad text) cheaper rather than
neither-here-nor-there.

## Reframing 1 — AC-2 should be a ratchet, not a fixture

**I am disregarding AC-2 as written.** It says: *"a fixture containing
path-like and URL-like strings proves it."* It proves nothing. A parser that
never resolves paths passes that fixture trivially — it would pass today,
before any code is written — and a parser that *does* resolve them is caught
only if the fixture happens to name the exact path reached. The fixture is
green in both the safe and the unsafe world, which makes it a decoration.

JLS already ships the correct idiom, twice, and the issue does not cite it:

- `test/jls/HeadlessCoreRatchetTest.java` walks `src/`, reads import
  statements, and fails on any core file importing `java.awt`,
  `javax.swing`, or `jls.edit`.
- `test/jls/SocketConfinementRatchetTest.java` confines socket and channel
  *construction* to `jls.collab.net` — *"so the network-facing surface
  cannot quietly spread through the codebase"* — and separately bans
  `ObjectInputStream` anywhere under `jls.collab` because it would be *"a
  remote-code-execution primitive"* reachable from network bytes.

The network half of AC-2 is therefore **already structurally guaranteed**:
a new importer package that opened a socket would turn that ratchet red on
the next `mvn verify`, with no `.cv` fixture involved. The filesystem half
wants a sibling in the same idiom — an `ImportConfinementRatchetTest` over
the importer package tree banning `java.nio.file`, `java.io.File`,
`ProcessBuilder`, `Class.forName` and `ObjectInputStream`. That is a
structural proof rather than an anecdotal one, it covers `.dig`, `.cv`,
Falstad and `.circ` at once, and it stays true against code nobody has
written yet. Restate AC-2 as that test.

## Reframing 2 — AC-4 asks for a schema mirror JLS should not own

AC-4 wants a typed in-memory model preserving "CircuitVerse's per-circuit
structure and inter-circuit references". The shipped precedent for that is
`YosysNetlist.java` at **953 lines** — the largest file in the HDL import
path, and a mirror of a schema JLS does not control.

Four importers are filed (#612, #621, #629, #323). Four bespoke schema
mirrors is four upstream formats to track forever. CAP-16 Open Question 4
already decided migration is *one-way, with no `.circ` writer*, explicitly so
JLS does not "commit to tracking a format it does not control… forever, for
the benefit of leaving." A typed model per format quietly re-incurs most of
that cost on the read side.

Better cut: keep the parsed representation **generic** — the `JsonValue`
tree already in hand — and put the schema knowledge in TASK-C559-2's mapping
table, which #622 AC-1 already specifies as *"a written, reviewable table in
`docs/`, one row per CircuitVerse element type, **read by the code rather
than restated in it**."* If the table is data the code reads, a typed
intermediate model sits between two things that already agree and must be
kept in sync with both. The only structure genuinely worth typing is the
part TASK-C559-3 (#624) actually consumes: the circuit-id → circuit map and
the instance → definition reference graph, which is two records, not a
schema. Restate AC-4 to that: *the parsed result exposes the circuit set and
the inter-circuit reference graph; everything else stays generic JSON.*

## The larger arc: four "parse hostile input" tasks want one ingest boundary

#612 (XML), #621 (JSON), #629 (text) and #323 (XML) each independently
restate the #38 standard in prose, each will mint its own limit constants,
its own diagnostic shape, and its own per-vector test list. That is the
fragmentation the `.jls` path already solved and centralized: `FileAbstractor`
is *one* reader that sniffs every container JLS has ever meant, and
`LoadError` is *one* taxonomy every front end renders identically
(ARCHITECTURE.md, "Error-reporting contracts").

The importers are re-splitting what the loader unified. CAP-29 PF-1 already
funds "shared import-report infrastructure" for the *loss report* — the same
argument applies one layer down, to the *ingest boundary*: read bytes once
under one bounded reader with a `SourceLimits`, hand the bytes to a
format-specific syntax layer, and land every syntax failure in one taxonomy
(an `ImportError` sibling of `LoadError`, same category/location/detail/hint
shape). Then "hostile-input hardening per #38" is a property of the boundary,
asserted once, and each new format inherits it instead of re-arguing it.

TASK-C559-1 is the natural place to build that boundary, because JSON is the
format whose parser is already written — the cheapest of the four to lift.
Doing it here makes #612 and #629 subtractions rather than additions.

## Ordering

`ordering_after: [314]` is ceremonial. #314 (FEAT-002) changes
`Element.setValue`'s return type and five `Circuit` loader arms — the `.jls`
attribute-dispatch path. Nothing in a `.cv` JSON parser touches it. The
intended dependency is on the *discipline* ("dropped is an observable
event"), and discipline is better carried by the ratchet above than by an
ordering edge that blocks startable work. This task can start today.

## Verdict

**endorse-with-reframing.** The outcome is right and on the project's arc.
The route should change: lift and generalize `jls.hdl.yosys.JsonValue`
rather than write a second parser; add the size bound and a per-consumer
number policy (the float case will otherwise fail on the first real `.cv`);
replace AC-2's fixture with a package-import confinement ratchet in the
shipped `SocketConfinementRatchetTest` idiom; and narrow AC-4 from a schema
mirror to the reference graph #624 actually consumes. Done that way this
task costs a fraction of its band and leaves #612 and #629 cheaper — which
matters directly, because KC-29-1 gives the whole `.cv` importer only 1.5×
a 3-5 mw band before it downgrades to a conversion recipe.
