# Issue #587: FEAT-C35-4: a wrong hotkey, an undocumented flag or a missing element page fails the build — the two existing help ratchets generalize into docu-tests
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

The stated goal — "documentation that contradicts the program fails CI" — is right,
and it is already this project's house style. What the issue gets wrong is the
*mechanism* it generalizes. It reads the two help ratchets as the pattern to
replicate ("the two existing help ratchets generalize into docu-tests"), and so
proposes a fourth, fifth and sixth hand-rolled parser-plus-comparator. But the
same repo is simultaneously building #584: one plain-text source tree, one
`mvn`-reachable goal, **"no file in either output is hand-edited."** Once facts are
generated, most of what AC-1 through AC-4 assert is not a test at all — it is a
staleness diff, and the wrong claim becomes *unwritable* rather than *detectable*.

That is the reframing: **generate the facts, ratchet only the residue.**

## The trajectory it lands in (and what the issue's boundary note misses)

The boundary note names three prior tests. It undercounts. The doc-↔-code ratchet
is already a repeated project idiom in at least five places:

- `test/jls/HelpTopicsTest.java` (350 lines) — topic/link/reachability/palette coverage.
- `test/jls/HotkeysHelpAccuracyTest.java` (383 lines) — content of one page vs `EditOp`.
- `test/jls/CliFlagTableTest.java` (136 lines) — `FLAGS` ↔ `usage()` ↔ parser.
- `test/jls/ExtensionPointCatalogTest.java` — parses the markdown table in
  `docs/extension-points.md` and cross-checks it against `ExtensionPoint`
  constants **in both directions**.
- `test/jls/FileFormatSpecTest.java` — `SPEC = Path.of("docs", "file-format.md")`,
  drift-tested against the actual save format.

So the claim "AC-1 is entirely new ground: the doc side of that triangle is
currently unasserted" is true *for CLI flags specifically* and false as a
statement about the project. Bidirectional markdown-table ↔ code assertion is a
solved, twice-shipped pattern here. What is genuinely missing is not another
instance of it — it is a **named mechanism**. Five bespoke markdown/HTML parsers,
each with its own regex and its own idea of what a table row is, is the drift
surface this issue is trying to eliminate, reproduced one level up.

There is a third seam already in the tree pointing the same way: the
`@jls.testedby` javadoc tag (declared in `pom.xml`, used on
`JLSStart.commandLineFlags()`). It is a code→test claim with no enforcement. The
doc→code direction this issue wants is its missing counterpart, and both want the
same tiny piece of infrastructure.

## The reframe: three tiers, not one ratchet

Sort every documentable claim by what makes it true, and the work collapses:

**Tier 1 — generated, therefore unlieable.** Flag tables, port lists, parameter
lists, accelerator tables, element indexes. These are projections of a data
structure. Under #584 the generator already runs; emitting
`docs/cli-reference.md` from `FLAGS` and a per-element port/parameter block from
the live element costs less than writing the assertion that checks a human wrote
them correctly, and it never produces a red build that a maintainer must fix by
hand-editing prose. CI's whole obligation becomes one check: *regenerate, diff,
fail if dirty.* One test, not four. This is exactly what Digital's docu-tests
actually buy — the issue names Digital as the benchmark but imports the wrong
half of it.

**Tier 2 — asserted, because prose is the point.** Free text that names a flag,
a key, or an element (`README.md`'s CLI paragraph, `docs/batch-interface.md`
§1–2, forty-odd pages under `resources/help/elements/**`). Here you cannot
generate, so you scan: every `-token` appearing in any doc must resolve to a
`FLAGS` entry; every keystroke-shaped token must resolve to an `EditOp`
accelerator. Note this is the *reverse* direction of AC-1 and it is the only
direction that genuinely needs a test — the forward direction ("every flag is
documented") is free once the reference page is generated. This is ~40 lines of
one shared helper, not three test classes.

**Tier 3 — executed, because behavior is not a table.** AC-2 asks that "stated
behavior" be checked "against the registry descriptor." No descriptor encodes
behavior, and none should. The honest lever for behavioral claims is the one the
project already owns: `BatchSimulationGoldenTest`, `ElementSimulationGoldenTest`,
`examples/`. Let element pages carry a runnable circuit fragment and its expected
trace, and have the docs build simulate it. That is a real docu-test — it makes a
false behavioral claim fail *and* makes the page teach. It is also the piece of
this issue that would most change what JLS is, and the issue does not ask for it.

## Where AC-2 points at something that does not exist — and shouldn't

AC-2 requires "each page's documented ports, parameters and stated behavior …
checked against the registry descriptor." Two problems, both structural:

1. **The registry carries none of that, by decision.** `ElementType`
   (`src/jls/elem/ElementType.java`) is explicit: it "carries only what loading,
   saving, and headless tooling need … GUI concerns — palette icon, category,
   help topic, creation dialog — belong to a separate GUI-side palette entry and
   never appear here." `PaletteEntry` is equally explicit that it "deliberately
   carries no capability set … duplicating them here would reintroduce the drift
   the interfaces removed." AC-2 as written silently commissions a *third*
   metadata layer — ports and parameters — over a two-layer split #78 chose on
   purpose. That is not a test; that is an architecture change, and it deserves a
   Recorded Decision in ARCHITECTURE.md rather than arriving as the incidental
   cost of a CI ratchet.
2. **A better oracle already exists and needs no new metadata.** Ports are
   discoverable from the *running* element: `LogicElement.getAllPuts()` on an
   instance loaded from the fixtures `AllElementsRoundTripTest` and
   `FileFormatSpecTest` already maintain (one instance of every savable type).
   Parameters are discoverable from the `Attribute` registry (#52) that already
   drives save/load and the dialogs. Both are the truth the user actually meets;
   a hand-authored descriptor would only be a third thing to keep in step.

Read register.html before committing to AC-2's wording. It documents **Name**,
**Bits**, **Initial value** — dialog fields — and never names a port. The
pre-fork help tree is written in a register that does not contain the facts AC-2
proposes to check. Enforcing AC-2 therefore means *rewriting forty pages into a
shape a parser can read*, concurrently with #584 rewriting the same forty pages
into a new source format. Doing that twice is the actual cost here, and it is
invisible in the issue's 1-2 MW band.

## AC-4 dissolves; AC-3's real delta is smaller than stated

AC-4 ("the ratchets run against *both* generated targets so a claim cannot be
true in one and false in the other") is a property #584 AC-1 already establishes
by construction: one source, no hand-edited output. Running every assertion twice
is paying for a guarantee you bought upstream. Keep one cheap invariant instead —
the two targets' extracted fact sets are equal — and drop the duplication.

AC-3 is worth doing, but note that `HotkeysHelpAccuracyTest`'s hand-maintained
`DOCUMENTED_OPS` map (17 label→`EditOp` rows) is itself a drift surface: adding
an `EditOp` does not fail that test. Generalizing "hotkey accuracy beyond one
page" while leaving that map hand-authored generalizes the weakness too. Under
the reframe, the hotkey table is Tier 1 (generated from `EditOp`), and the Tier 2
scan catches accelerators mentioned in prose elsewhere. The mac-accelerator gap
the issue names is real and orthogonal — it belongs with #265's platform work.

## What I would keep verbatim

- **AC-1's reverse direction.** "No document names a flag the parser rejects" is
  genuinely new, cheap, and the highest value-per-line in the issue. The
  dedup comment's recommendation — that #524 and this read `FLAGS` through one
  accessor — is already half-satisfied by `JLSStart.commandLineFlags()`; widen
  that accessor to expose arity and description and both consumers are served.
- **AC-5's committed negative-check record.** Falsification transcripts are this
  project's strongest habit; keep all four planted claims, including the stale
  port list, since that one falsifies the Tier 1 generator too.

## Disregarding parts of the stated acceptance criteria

I am explicitly setting aside AC-2 as written and AC-4 entirely. AC-2's oracle
(a registry descriptor of ports/parameters/behavior) does not exist, contradicts
#78's recorded two-layer split, and would be a worse oracle than the live element
and the `Attribute` registry even if built. AC-4 asks for duplicated enforcement
of an invariant #584 AC-1 guarantees structurally. Replace them with: *element
pages' factual blocks are generated from the loaded element and its attributes;
the docs build fails if regeneration produces a diff; behavioral claims are
carried by runnable examples checked against the batch simulator.*

## Ordering

`ordering_after: [FEAT-C35-1]` understates the coupling. Generate-vs-assert is a
**#584 design decision**, not a downstream consequence: if #584's source format
has no mechanism for injecting generated blocks, this issue is locked into the
expensive branch forever. Fold the generated-fact-block requirement into #584's
AC-1 now, and what remains here is one shared doc-scan harness plus the
falsification record — comfortably inside its band, and the ratchet stops being a
tax on every future doc edit.
