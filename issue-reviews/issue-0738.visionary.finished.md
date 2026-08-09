# Issue #738: TASK-C557-2: a red perf lane names which published number is now a lie, so the outcome is an engine fix or a reviewed re-publication — never silence
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the CI vocabulary and #738 is an **anti-drift claim about a normative document**:
`docs/performance.md` will make public assertions, and the project must be structurally
incapable of letting those assertions and the engine diverge in silence. CAP-28 (#512)
says the deficit is epistemic — "2/5 on scale/perf for lack of receipts, not lack of
speed" — so the artifact being defended is the receipt, not the speed.

That goal is squarely on the project's arc, and the arc is stronger than #738 realizes.
JLS already has a *genre* for exactly this problem and has applied it four times:

- `docs/file-format.md` ↔ `test/jls/FileFormatSpecTest.java:52` (`Path.of("docs",
  "file-format.md")`) — the spec's tag table is asserted equal to the set a
  full-coverage circuit actually saves.
- `docs/extension-points.md` ↔ `test/jls/ExtensionPointCatalogTest.java` — the class
  javadoc's own words are "the normative table … agrees with the constants in **both
  directions**, so the doc can never drift from the code", implemented as two
  `assertEquals(new TreeSet<String>(), …)` over an `undocumented` set and a `phantom`
  set (~:173-183).
- `docs/simulation-semantics.md` ↔ `SimulationSemanticsRegressionTest`.
- `docs/batch-interface.md`, declared in README as "a documented stability contract".

#738's AC-4 — "a published number with no corresponding band, or a band with no
corresponding published number, is itself a lane failure" — **is the phantom/undocumented
pair, verbatim**, applied to a fifth document. That is the single most valuable sentence
in the issue, and the issue files it as a clause of a cron-triggered benchmark lane.

## The reframing: this is a spec-drift unit test, not a CI-lane feature

Cut the seam differently. Split #738 into two things that have nothing in common except
subject matter:

1. **`PerformanceDocSpecTest`** — a plain JUnit test in the *fast* lane. It parses
   `docs/performance.md`'s published-number table and `test/fixtures/simulation-budget.properties`
   (#442's file), and asserts bidirectional correspondence, plus that every published
   sentence carries a band key and every band row carries the doc anchor it backs. It
   runs no benchmark, times nothing, needs no runner, and finishes in milliseconds.
2. **The lane's failure message** — with (1) in place, "name the stale published number"
   stops being a traceability subsystem and becomes a map lookup: the failing row already
   carries its anchor.

What this buys, concretely:

- **AC-2's hardest clause becomes mechanical rather than procedural.** "A band move
  requires the doc change in the same review" is prose in the issue, enforced by
  convention — the same convention CONTRIBUTING.md already admits is soft for the
  coverage ratchet. Under the reframing, moving a band without touching the doc turns the
  *fast lane* red in that PR. Not a policy, a build failure.
- **The escape route #738 exists to close is closed where it actually lives.** The issue's
  premise — "a red lane nobody can connect to a published sentence gets muted" — is a
  social failure, and a nightly cron is the most mutable object in a repo. This project
  knows it: `.github/workflows/ci.yml:12-13` runs `cron: "17 4 * * *"` on *only* the
  gui-wayland lane, and the file carries hand-maintained green-history commentary
  ("the last twenty non-cron ci.yml runs", ~:331, :482) precisely because nightly red is
  easy to ignore. A better failure message does not survive a `continue-on-error`. A
  fast-lane assertion does.
- **It can be built first, not last.** #738 is the fourth link of 554 → 555 → 735 → 738,
  and `docs/performance.md` does not exist in the tree today (nor does
  `docs/machine-calibration.md`, which #442 cites normatively with line numbers). The
  correspondence test can land *now*, with both sets empty, and then the day #555 writes
  the first published sentence the test forces a band into existence in the same PR.
  Built in that order #738 stops being the last link in a fragile chain and becomes the
  thing that makes the chain unbreakable from the start. That is a reordering, not a
  scope change, and it is the strongest concrete recommendation in this review.

## The stronger design: one datum with a projection, not two sets in correspondence

AC-4 accepts that there are two populations and polices their agreement. The more elegant
route deletes the second: make `docs/performance.md`'s number table **generated from or
transcluded out of** `simulation-budget.properties`, so correspondence is a type
property rather than an invariant needing a guard. If full generation is too much
machinery for a hand-written prose doc (a fair objection — this doc must also carry
honest comparative framing per #555 AC-3, which no generator writes), then take the
`ExtensionPointCatalogTest` middle: a backticked band key in each doc table row, an
anchor field in each properties row, and the bidirectional set test. Cheap, idiomatic
here, and it is what I would actually build.

Two completions the issue leaves on the table, both free once the parser exists:

- **Three-way, not two-way.** #555 AC-4 promises "no public performance claim exists
  anywhere in README/docs that the doc does not back". Extend the same test to README ↔
  doc ↔ band. Otherwise #738 bolts the front door while the README claims "~8k cycles/s"
  with nothing behind it.
- **Anti-vacuity.** "The two sets must stay in correspondence" is *vacuously green* when
  both are empty — which is the state today and the state a bad merge produces. #442
  already learned this (P6: at least two fixture rows, "emptying the properties file
  turns it red"). Mirror it: a floor on published-number count, ratcheting up with the
  doc. Without it the gate reads green in exactly the situation it exists to catch.

## Disregarding AC-1's framing: defend the deterministic quantity, report the noisy one

I am setting aside AC-1's implicit model — that the thing to attribute is a wall-clock
regression — because the project's own measurements argue against it. #442's O4 records
ns/event spanning 432,915 → 103,918 across three reps **in one JVM on one machine with no
code change** (4.17×), and its O7 records the in-tree precedent (`SpatialIndexTest`:
assert the exact hit-count equality, *print* the timing). Its Open Question 3 pre-
authorizes demoting the timing band to reporting on the first false failure. #738 is
building an attribution-and-disposition apparatus around the one quantity the parent
mechanism has already pre-authorized demoting.

The quantities that genuinely cannot go stale are the deterministic ones: retired event
count (`194`, three reps, exactly — #442 O4/H1) and events per cycle (388.4 on `k2000`,
`docs/capability-roadmap/keystone-c-performance.md:126-139`, with the clocking regime
recorded per O6). A published doc built on *those* — plus machine-tagged, dated absolute
throughput ("8,090 cycles/s, Linux x86-64, JDK 25, `k2000.jls`, 2026-08-04") — is a doc
whose sentences do not become lies when a runner is noisy. Digital's own claim is in
exactly this form ("120 kHz on a 2012 i5"). Under that framing a dated measurement does
not become *false*, it becomes *old*, and freshness is a date-and-commit stamp checked
against a max age — a fraction of the machinery #738 contemplates. I do not recommend
dropping the ceiling band (it catches real engine regressions, and #476/#475 will move
these numbers by 30-40% by design). I do recommend that AC-1's "names the stale number"
be satisfied for the deterministic rows by an equality with a printed delta, and for the
throughput rows by naming the *dated measurement* rather than asserting the doc lies.

## Where it pulls against the arc

AC-2's "documented disposition path" is about to become the project's **third** bespoke
re-baseline ritual — after CONTRIBUTING.md's coverage-ratchet rules (:69-120, including
"a floor that has never been seen to fail should be assumed vacuous") and #442's
simulation-budget protocol. Three near-identical rituals in three places will drift from
each other, which is the exact disease every other normative doc here has a test against.
Write one "Re-baselining a ratchet" section with three subjects under it — coverage,
simulation budget, published performance — and have #738 add a subject, not a document.

## Bottom line

The claim #738 makes about what JLS should become — that a public number is a contract
with a mechanical guard, not a marketing sentence — is correct and is the natural fifth
member of this repo's normative-doc family. Endorsed. But build AC-4 first, as a
fast-lane bidirectional spec-drift test in the `ExtensionPointCatalogTest` idiom, with an
anti-vacuity floor and README included; let AC-1 fall out of it as a lookup; and fold
AC-2 into one shared re-baseline protocol rather than a third copy. Done that way the
issue is roughly half its stated size and strictly harder to defeat.
