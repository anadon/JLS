# Issue #622: TASK-C559-2: every CircuitVerse element maps to a JLS element by semantics from a written table, or refuses by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Not "CircuitVerse support." The arc it sits on — #510 → CAP-29 (#513) → FEAT-C29-3
(#559) — is the only line of work in the roadmap that moves *users* rather than
files (#323 §Abstract says so explicitly). The deliverable of #622 is therefore
not a converter; it is **a trustworthy statement, per source construct, of what
JLS is and is not**. The refusals are the product as much as the mappings: an
instructor adopts on the strength of a work list, not on an import percentage.

Judged against that, the issue is right about the hard part (semantics over
names, refusal over approximation, structural net-partition assertion) and wrong
about where the seam is. Three findings follow, with alternatives.

## 1. The seam is cut per format; this is the fourth copy of one contract

Read #451 (TASK-0054, `.circ`), #614 (TASK-C558-2, `.dig`), #622 (`.cv`) and the
Falstad sibling side by side. AC-1/2/3/4 of #622 are #614's AC-1/2/3/4 with the
tool name swapped — not paraphrased, near-verbatim. #559's own dedup comment
argues the repetition is inherited from #556 and therefore expected. That
argument holds for the *report*, because #556 exists to own the report. **No
issue owns the mapping and realization contract**, so its four restatements are
convention, not inheritance, and convention is exactly what erodes: whichever
format lands second will "read" its table differently, assert its partition
differently, and phrase refusals differently, and nobody will notice until the
third.

The alternative, and my main recommendation: **file a peer of #556 — call it the
construct-mapping kernel — and let it own, once, what #622 currently owns for one
format**:

- the mapping-table *type* (source construct id → JLS realization or refusal, plus
  attribute correspondence), independent of which format populates it;
- the refusal path, so a refusal is a value in that type rather than a per-importer
  code branch — this is what makes AC-3's "never approximated" structural instead
  of aspirational;
- the net-partition oracle of AC-4 (`P(imp(f)) ≅ P_src(f)`, #323 §3), which is
  format-independent by construction and should be one harness, one assertion
  helper, four callers;
- the name-collision rule of #323, already stated as format-agnostic in #622's
  own Outcome — so it should be enforced by shared code, not restated in four
  acceptance criteria.

#622 then shrinks to what only it can do: **the CircuitVerse rows, the fixtures,
and the judgement calls** — which is honestly a 1 mw task, not 1–2, and reads as
a real slice rather than a clone. Nothing in the CAP-29 sibling rule forbids this;
#556 already set the precedent that shared shape gets its own feature.

## 2. AC-1 as written cannot mean what it says, and the repo already has the answer

AC-1: "a written, reviewable table in `docs/`, one row per CircuitVerse element
type, **read by the code** rather than restated in it." Taken literally that puts
Markdown parsing on the import path. It cannot work in a shipped build: `pom.xml`
(lines 134–150) packages `src/` non-Java files and `resources/` into the jar;
`docs/` is not a Maven resource and is absent from every installer, AppImage,
container image and Maven Central artifact. An importer that reads `docs/*.md` at
runtime works only in a git checkout.

The project has already solved this exact tension three times, and the pattern is
better than what AC-1 asks for:

- `docs/extension-points.md` is normative; `ExtensionPointCatalogTest` (lines
  93–110) reads that Markdown **at test time** and cross-checks it against the
  typed `ExtensionPoint` constants in both directions.
- `docs/file-format.md` ↔ `FileFormatSpecTest`; `docs/component-naming.md` ↔
  `ComponentIdentityTest`; `ElementRegistry` ↔ `ElementRegistryTest` totality.

So: **I am disregarding AC-1's "read by the code" and substituting "cross-checked
against the code by a catalog test, in both directions."** That preserves the
whole intent (the mapping is reviewable as a document; the code cannot drift from
it; a reviewer never reverse-engineers a switch statement) at lower cost, with no
new runtime mechanism, no new failure mode in a packaged jar, and consistency with
four existing catalogs. If the table must also be *data* at runtime — reasonable —
it belongs on the classpath under `resources/` in a versioned form, with the
`docs/` page generated from it or cross-checked against it; not the other way
round.

## 3. "By semantics" has nothing to compare against — the missing middle

AC-2 forbids mapping by name and requires mapping by semantics. But JLS has no
machine-readable notion of what a JLS element *means*. `ElementType`
(`src/jls/elem/ElementType.java`) carries tag, aliases, class, factory — and its
own javadoc says it holds "what loading, saving, and headless tooling (the HDL
importer's cell map, #61 …) need." Today "semantics" lives in prose: the canonical
collision case is a comment, `HdlExporter.java:80-88`, where JLS's `ShiftRegister`
is documented as combinational while the same-named component elsewhere is
sequential. Four importers deciding semantic equality against prose will decide it
four ways.

Concrete alternative design: **add a small semantic profile to `ElementType`** —
a record of the properties every one of these mappings actually turns on: role
(combinational / sequential / net-topology / annotation / simulation-control),
clocking (none / level / edge, which edge), reset (none / sync / async), width
policy, tri-state capability, port roles, and whether it carries state across
`initSim`. Then the per-format table maps *source construct → semantic profile*,
and profile → element is written once. Three consequences:

1. A name collision stops being a review-discipline problem and becomes a type
   error: profiles differ, the mapping does not compile into a valid row, AC-2
   is enforced by construction rather than by one pinned fixture.
2. The refusal set becomes computable — "no JLS element carries this profile" is
   the honest reason string AC-3 wants, generated rather than hand-written.
3. It is paid for by more than migration: the Yosys cell map (#61,
   `docs/hdl-support-research.md` §7.2), the HDL exporter's map/warn/reject
   partition (`HdlExporter.java:76-90`), and the IP-XACT direction in
   `docs/standards-adoption/08-ipxact-export.md` all want the same table and today
   each keeps its own. This is the #78 registry finishing its job, which
   ARCHITECTURE.md already names as the recorded direction for element metadata.

If that is too much to carry inside a 1–2 mw task — it is — it is the right
content for the kernel feature in finding 1, and #622 is the right issue to
*trigger* it.

## 4. Two duplications the issue does not see

**The realization stage already exists and is private.** `NetlistImporter.Builder`
(`src/jls/hdl/imp/NetlistImporter.java:410`, `private static final class`) is the
existing "turn a foreign model into JLS elements and wires" engine, complete with
the no-partial-circuit discipline (lines 41–47) that every one of these importers
inherits. #323 Open Question 4 says plainly that a second importer must not fork
it. #622 does not mention it. Two format tasks (#614, #622) that each quietly
build their own realizer is precisely the outcome OQ4 was written to prevent.

**The construction route is unresolved and drifting.** #323 §2 alternative 2
rejects "emit save text and reparse it" — yet `NetlistImporter` does exactly that,
by design and for good reasons of its own. Meanwhile `docs/operation-layer.md`
now describes a landed, validated, invertible op vocabulary (`jls.collab.op.CircuitOp`
/ `OpSink`), which is both the "construction verbs" #323 §6 wanted and the thing
that makes #628's "single undoable operation" fall out for free rather than being
engineered per format. **Say in this issue which route the mapping realizes
through, and make it the op layer.** Leaving it unstated guarantees three
importers pick three routes.

## 5. The alternative I considered and would keep as the floor, not the plan

There is a route that makes the mapping table disappear: CircuitVerse can export
Verilog; Yosys reads Verilog and writes JSON; JLS already imports Yosys JSON with
auto-layout. `.cv` → Verilog → Yosys → `NetlistImporter`: no new parser, no new
table, no new hardening surface.

I do not recommend it as the plan, and the reasons are worth writing into the
issue because they are the actual justification for spending 3–5 mw on #559:
synthesis flattens exactly what a *course* needs — displays, clocks, signal
generators, memory initial contents, pin labels, layout, and the subcircuit
boundaries #624 is built to preserve; it covers only the synthesizable subset;
it degrades every loss into "Yosys optimized it away," which is the opposite of
a located, explained report; and it makes migration depend on the tool the
instructor is trying to leave.

But it is a superb **KC-29-1 fallback and baseline**, and it costs about a page.
CAP-29 already says a blown format "downgrades to a documented external-conversion
recipe" — write that recipe *first*, publish it, and the importer then has a
concrete bar to clear and a safe landing if the corpus economics fail. Cheap
insurance that the arc currently defers until it is needed most.

## 6. One document, not three

CAP-29 PF-6 plans per-format "what survives, what doesn't" pages; #556 owns a
report schema; #622 adds a mapping table in `docs/`. That is three artifacts
describing one fact set. The mapping table should **be** the source of the PF-6
page (generated or cross-checked), and the report's per-entry explanation should
cite table rows by id. A published "what survives from CircuitVerse" matrix is the
adoption artifact the whole capstone is chasing; it should not be a fourth thing
written later from the same knowledge.

## Verdict

**endorse-with-reframing.** The obligations are right and load-bearing; keep AC-2,
AC-3 and AC-4's substance without weakening. Reframe: (a) hoist the mapping-table
type, refusal path, name-collision enforcement and partition oracle into a kernel
feature peer to #556, leaving #622 to own the CircuitVerse rows and judgements;
(b) replace AC-1's "read by the code" with the repo's established catalog-test
cross-check, since `docs/` is not on the classpath; (c) name the realization route
(the op layer) and the shared realizer (promote `NetlistImporter.Builder`) inside
this issue rather than discovering both twice; (d) write the Yosys-route external
recipe now as the documented floor. The semantic-profile extension to
`ElementType` is the version of this work that pays for itself four times over —
if only one idea here is adopted, adopt that one.
