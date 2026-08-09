# Issue #592: FEAT-C37-1: the editor's ergonomic standing stops being an opinion — a published, cited parity catalog grades every interaction against the three incumbents before a single fix is funded
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the framing and #592 is not research. It is a **funding schema**. #510 already
mined the incumbents' trackers, already produced the complaint lists with issue links,
already scored JLS on a 12-dimension rubric with adversarial verification. The genuinely
new content here is per-row **grade + funding score + owning issue + stop-loss column** —
a ledger whose only job is to keep KC-37-2 honest, i.e. to stop #596 from becoming the
forever-bucket that "ergonomic polish" naturally becomes. That purpose is real and worth
1–1.5 mw. The two dedup comments confirm it: both are entirely about *ownership and
disjointness of rows*, not about what the incumbents' users want. Nobody is confused
about the complaints; they are confused about who owns which fix.

So the question is not "should this catalog exist" but "**what shape of artifact bounds a
budget?**" — and on that, the issue picks the one shape this project has spent two years
systematically refusing.

## The reframing: the catalog is a test tree that emits a document, not a document

JLS's actual architectural signature — the thing that distinguishes it from every tool in
#510's matrix — is that **its normative documents cannot drift, because a test cross-checks
them in both directions**:

- `docs/extension-points.md` ↔ `ExtensionPointCatalogTest` — "adding a typed-now row
  without a constant, or a constant without a row, is a build failure"
  (`docs/extension-points.md:19-24`).
- `docs/file-format.md` ↔ `SaveTagsTest` / `FileFormatSpecTest`; the CLI flag table ↔
  `CliFlagTableTest`; help topics ↔ `HelpTopicsTest`'s palette-coverage completeness test;
  `docs/pointer-geometry-census.md` ↔ `PointerApiRatchetTest`.
- `docs/keyboard-a11y-verification.md` is the nearest genus to what #592 wants, and its
  rows are already *behaviour → signal → the test that goes red* — not grades.

#592 as written produces the opposite: a hand-maintained markdown table with a subjective
score column, a HAVE/GAP/REFUSE grade, and **no mechanism whatsoever keeping it true** the
moment PF-2 lands. Its own dedup comments concede the failure mode — a row can be
"re-owned" by #596, a row can be ambiguous between #542 and #593 — and their remedy is
*another hand-maintained column*. Columns do not enforce disjointness; totality tests do.

Concretely, what I would build instead for the same 1–1.5 mw:

1. **`test/jls/ui/ParityCatalogTest` plus one annotation.** Each ergonomic behaviour is a
   `@ParityRow(id, grade, cite, owner, pin, score)` on a test method.
   - **HAVE** rows are live assertions that pass today.
   - **GAP** rows are `@Disabled("GAP: …")` tests that *already encode the target
     behaviour*. A GAP fixed by unrelated work turns green and is noticed; a GAP nobody
     can express as an assertion was never scorable and says so at authoring time.
   - **REFUSE** rows are present-and-asserted refusals carrying the prose reason as data.
2. **`docs/parity-catalog.md` is generated from the annotations**, and a cross-check test
   fails the build in both directions — the exact `ExtensionPointCatalogTest` idiom,
   already in the tree, ~200 lines of borrowed code.
3. **Totality replaces AC-2.** "Not scored is not an allowed grade" stops being a rule a
   reviewer must remember and becomes the same property `MouseMachine`'s transition table
   is required to have in #441 §7.11: every declared behaviour maps to a declared outcome
   or the build fails. This project already has the vocabulary; #592 should reuse it, not
   re-invent it in prose.
4. **Ownership is a field, not a judgement.** `owner = 593 | 594 | 595 | 596 | 542 | 289`,
   with a test asserting each id appears exactly once. Both dedup comments' entire concern
   evaporates, and #596's "an item with no catalog row is not started" becomes mechanically
   checkable rather than a promise.

The prize: **#521 AC-2 stops being separate work.** "Every closed row has a harness test
pinning it" is no longer an obligation to discharge per fix — closing a row *is* deleting
an `@Disabled`. The catalog and the acceptance evidence become the same artifact, which is
the elegance the project claims (4/5 on #510's own scorecard) and here declines to use.

## AC-5 is the wrong instrument, and the right one is already built

A stopwatched 4-bit-counter build, timed once by the single maintainer who wrote the
editor, is not a baseline. It cannot be reproduced, cannot be re-run in CI, and #521 AC-4's
"the after is not slower" would need the same person with the same muscle memory months
later. It is the one row of this issue that a markdown table physically cannot hold honest.

The project already owns the instrument that makes it objective: **the op layer is a closed,
serializable, invertible vocabulary of 17 mutation kinds with `CircuitOpReader` as its exact
inverse** (`src/jls/collab/op/`, `docs/operation-layer.md`). Record the canonical counter
build **once as an op transcript**, and let the baseline be *gesture count, op count, dialog
count* — not wall-clock. Those numbers are reproducible, diffable, CI-assertable, and are
the quantity every PF-2..5 fix is actually supposed to reduce. "13 gestures / 9 ops / 4
dialogs → 9 / 7 / 2" is a claim a reader can check; "4 min 20 s → 3 min 55 s" is not. Make
it a ratchet in the same style as the JaCoCo floors (`pom.xml:400-418`) and AC-4 enforces
itself forever instead of being remembered once.

I am explicitly disregarding AC-5 as written for that reason.

## Three concrete frictions with the trajectory

- **AC-1's citations have nowhere on main to point.** #510's evidence tree lives at
  `docs/reviews/evidence/2026-08-niche-survey/` on branch `claude/jls-project-review-505pnf`;
  `docs/` in this checkout has no `reviews/` directory. Both #316 and #441 carry the
  completion criterion *"every cited evidence document and permalink resolves on the default
  branch at close — no branch-path links."* This feature should **land #510's evidence tree on
  main and cite it**, not re-mine the same trackers into a second corpus. Re-mining is
  duplication of #510; citing is the cheap, correct move.
- **The named acceptance vehicle no longer exists as an issue.** #592's boundary note pins
  rows to "#91 (UI harness) and #441 TASK-0020 (headless interaction machine)". **#441 was
  closed as `duplicate` on 2026-08-08**; its scope survives as #316's TASK-0020 row, which
  #316 §2 still lists as *"not filed"*, behind `blocked_by: [317, 337]`. A per-row column
  naming which of two vehicles will pin it therefore names one vehicle with no filed issue.
  Under the reframing this stops mattering — an unpinnable row is a `@Disabled` with a reason
  — which is itself an argument for the reframing.
- **The KC-37-1 gate is applied at the wrong moment.** Requiring, at *authoring* time, that
  every scored row be implementable in collaborators #316 has not yet produced makes a
  documentation-only feature depend on a decomposition census (TASK-0019) that is unfiled.
  As an annotation value re-evaluated by a test, "blocked on #316" is free and self-updating.
  As a prose flag frozen into a table, it is stale the day #316 lands its first extraction.

## The uncomfortable sequencing question

#510's §4 lists the four things *every* teardown produced independently: shop window,
first-run experience, chronogram, published benchmark. **Editor ergonomic parity is not among
them.** Its bounce finding is that a switcher "leaves in the first ten minutes without ever
discovering the parts of JLS that are genuinely superior" — because launch is an empty
`JTabbedPane`, the README has no screenshots, and no example circuit is discoverable
(`examples/` in this tree contains exactly one entry, `autograde`). The bounce happens
*before the first wire is drawn*. CAP-37 measures the ninth minute; #510 says users leave in
the first.

That does not kill this catalog — a scored gate on 8–14 mw of polish is the right instrument
for that spend regardless. But it does mean the catalog's ranking column should be weighted
by **bounce evidence**, and its front matter should say plainly that every row below the
first-run gate (#381, examples, screenshots) is unfundable until that gate lands. A parity
scorecard that ranks by incumbent-tracker reaction count will happily fund wire coloring
while the product still opens to a blank tab.

## Verdict

**endorse-with-reframing.** The purpose — a cheap, cited, disjoint gate that bounds an
open-ended ergonomics budget — is aligned and worth funding before PF-2..5. The artifact is
wrong: this project's whole claim to elegance is documents that a test keeps true, and #592
proposes the one document genre it has never allowed to be prose. Build the catalog as an
annotated, totality-checked test tree that *emits* `docs/parity-catalog.md`; replace AC-5's
stopwatch with an op-transcript gesture/op count ratchet; cite #510's evidence from main
rather than re-mining it. Every worry raised in both dedup comments is then enforced by the
build instead of by a reviewer's memory.
