# Issue #608: TASK-C556-1: one loss-report schema — construct, disposition, location, explanation — that any importer can adopt from a written document
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the framing and the claim is: *when a circuit crosses a boundary into or
out of JLS, the user must be told what did not survive, in one voice.* #608
scopes that to importers, and to importers that do not exist yet (#558, #559,
#561, and the `.circ` reader #451 that is itself blocked on #404). The
scoping is where the trouble is: the need is real and present, and the issue
defers it behind a chain of unbuilt work while excluding the three places
JLS already answers the same question badly.

## The finding: JLS already speaks four loss dialects, and none of them is `.circ`

Before any new importer exists, the tree already reports "something did not
survive" in four incompatible shapes:

| Boundary | Shape today | Anchor |
|---|---|---|
| Circuit load | structured record: `Category` enum + line + element + detail + hint | `src/jls/LoadError.java` |
| Yosys netlist import (success) | counts only: element tally, cell→count map, `coercedX` | `src/jls/hdl/imp/ImportSummary.java` |
| Yosys netlist import (refusal) | one multi-line `String` | `src/jls/hdl/imp/ImportException.java` |
| Verilog/VHDL export refusal | one `String`, built from a private `describe(el)` → `Type "name" at (x,y)` | `src/jls/hdl/HdlExporter.java:1343`, `HdlExportException.java` |
| Loading an unknown attribute | *nothing* — `Element.setValue` falls off the loop; #404's whole premise | `src/jls/elem/Element.java:344` |

`LoadError` is already (category, location, explanation, hint) with a closed
taxonomy tests assert on — that is #608's schema with one field renamed and
the disposition vocabulary swapped. `HdlExporter.describe` is already the
"location" renderer for JLS-side anchors, written once, private, and about to
be written a fifth time. #608 as filed would add a *sixth* dialect that unifies
only the three importers that do not exist, leaving the four that do.

## Reframing 1 (primary): a fidelity ledger in `core`, not a report schema in the importer family

Build one `Finding` type — *construct · disposition · location · explanation ·
next step* — in the headless kernel (`docs/grand-architecture.md` §5 puts
persistence and the op vocabulary in `core`; diagnostics belong beside them,
not under an importer package), and retrofit it onto the four existing call
sites. `LoadError` becomes the load-boundary projection of it; `ImportSummary`
becomes a ledger with `mapped` entries; `HdlExportException` carries a ledger
instead of a string; #404's unknown-attribute event emits into the same
vocabulary. Then `.circ` and the CAP-29 three are the *fifth through eighth*
consumers of a proven type, not the definers of a speculative one.

Three things fall out that #608 as written cannot get:

1. **It is buildable today.** Nothing in it depends on #404, #451, #323 or a
   corpus. The stated `ordering_after: [323, 314]` inverts the real order:
   generalizing from one unbuilt example is speculative; generalizing from four
   built, mutually inconsistent examples is a refactor with a test oracle.
2. **It survives every kill criterion above it.** KC-29-1 (per-format stop-loss)
   and KC-29-2 (re-price if the `.circ` corpus economics fail) can take all four
   importers off the board; a schema whose only value is those importers dies
   with them. A fidelity ledger that already unified the loader and both HDL
   directions has paid for itself before the first `.dig` file is opened.
3. **It dissolves the sibling-merge conditional.** #608 and #556 both carry
   "if #311/#323's REPLAN prefers absorbing this, it merges there, lower number
   wins" — an unresolved ownership hazard that exists *only because* the work is
   framed as a satellite of the `.circ` importer. Sited in `core` it belongs to
   neither capstone and both consume it.

I am explicitly disregarding AC-3's boundary ("a new *importer* author can emit
a conforming report without reading `.circ` importer code"): the right acceptance
test is that the *loader*, the *exporter* and both importers already emit
conforming findings before any new format lands.

## Reframing 2: `location` is a pair, not a source coordinate

AC-4 stresses that `location` must name a position in a non-XML source, with
Falstad as the worked example. That is the backward-facing half. The half that
makes a report actionable *inside JLS* is the forward-facing anchor: which JLS
element (id, name, grid position) this construct became — precisely what
`HdlExporter.describe` already prints. A finding with both anchors turns the
report from a text file into navigation: click the row, the editor selects the
approximated element. That also gives #451's per-circuit geometry record (P10:
snapped vs re-laid-out) its natural home as a per-element caveat rather than a
free-floating report field, and it answers #451 Open Question 5 ("where does the
report live after the dialog is dismissed?") structurally instead of by writing
a sidecar `.txt` — a `mapped-with-caveat` element that is still marked in the
editor cannot be lost by clicking OK.

## Reframing 3: the disposition vocabulary is under-specified where it matters most

#608's four terms (mapped / mapped-with-caveat / refused / dropped-by-design)
and #451 Stage 3's four terms (mapped / approximated / unmapped / refused) are
not the same four, and the union exposes the gap: **neither has a clean term for
"JLS could map this and does not yet."** For the instructor the whole report
exists to serve, that is the single most decision-relevant category — it is the
difference between "this course cannot migrate" and "this course migrates after
one issue is filed." Collapsing it into `dropped-by-design` (permanent) or
`refused` (loud ambiguity) destroys exactly the signal the report is for.
Derive the vocabulary from the decision it drives: *realized* / *realized with a
stated semantic difference* / *not yet supported (work-list, with an issue link
where one exists)* / *refused as ambiguous* / *out of scope by design*. Five
terms, each mapping to a different instructor action.

## Reframing 4: make the machine-readable rendering a normative interface, not a document

AC-1 says a grading script branches on `disposition`. That makes the rendering a
public stability surface, and JLS has an established shape for those:
`docs/batch-interface.md` and `docs/file-format.md` are normative specs with
golden tests, and `ExtensionPointCatalogTest` / `ConstructMapTest` pin documents
against code in both directions. AC-3's "a written contract document lands in
`docs/`" should be that, not prose. Two consequences the issue never raises:

- **Format.** The tree carries a JSON *parser* only (`jls/hdl/yosys/JsonValue.java`,
  with a documented "JLS deliberately carries no JSON library dependency") and no
  writer anywhere. So "machine-readable" is a real build decision, not a
  formatting detail: either hand-roll the first JSON emitter in the tree (and
  own it as general infrastructure), or use the line-oriented grammar JLS already
  uses for saves, `-t` vectors and batch output — greppable, diffable, no new
  code. Pick deliberately and record it.
- **Reachability.** The report must be emitted in batch mode. README's
  container-image audience is "autograders and CI"; a report only an EDT dialog
  can render is invisible to exactly the consumer AC-1 names.

## Alignment with the project's arc

Positive: this pulls in the same direction as the headless-core keystone (#77),
the fail-loud loader (#314/#404), and the "unknown input names its own gap"
discipline #451 calls the first user-visible product of that invariant. It also
lets the pending `hdl.importer` extension point — `docs/extension-points.md` row
7, "cell-map/layout contract to be defined" — finally be defined as *parse →
construct model → ledger*, so the fourth importer is a module contribution
rather than a new package. That is a real architectural payoff #608 leaves on
the table by treating the report as an artifact rather than as half of the
importer SPI.

Negative only in siting and sequencing: as filed, shared diagnostic vocabulary
would be born inside a capstone's importer family, which is precisely the
implicit wiring §5 of `grand-architecture.md` exists to stop.

## What to keep unchanged

The closed vocabulary over free text (AC-1), both renderings golden-tested from
one in-memory value so they cannot drift (AC-2), the written contract (AC-3),
and Falstad as the non-XML worked example for `location` (AC-4) are all right,
and the boundary note forbidding any re-litigation of `.circ` mapping decisions
is exactly correct. The 1 mw band survives the reframing; retrofitting four
existing call sites is perhaps another half, and it buys an oracle the issue as
written does not have.
