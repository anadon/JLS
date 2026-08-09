# Issue #753: TASK-C547-1: every accessibility criterion is bound to a named automated test, and a claim with no test fails the build
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#753 is `TASK-C547-1` (`part_of_feature: 547`, i.e. it is the mapping-and-gate
sub-task of #547/FEAT-C26-5, itself serving the CAP-26 accessibility capstone,
#507). It asks for two artifacts: (1) an in-tree criterion→test mapping keyed
by criterion identifier, each row naming a test class+method or declaring
itself an "exception" or a "manual-checklist claim"; and (2)
`VpatCoverageTest` (named in #507's own AC-4), which must fail when a claimed
criterion's named test does not exist, does not run, or does not pass. The
downstream document generator (#754, TASK-C547-2, already reviewed) consumes
this mapping and is explicitly scoped to only print what the mapping proves.
`ordering_after: [TASK-C549-2]` (#758).

## Findings, most severe first

**1. [High] The mapping has no completeness requirement — a maintainer can make `VpatCoverageTest` un-gameable in the letter while gaming it in effect, simply by never adding a row.**
AC-2 says the test "fails when a criterion marked claimed names a test that
does not exist, does not run, or does not pass." That only fires on rows that
exist in the mapping. Nothing in AC-1–AC-4 requires the mapping to cover a
defined criterion universe (the project's own playbook,
`docs/standards-adoption/03-accessibility-conformance.md:333` §5, sizes that
universe at "~90 criteria × 5 surfaces"). A maintainer — or an implementer
racing the `band_mw: 0.5-1` budget — satisfies every stated AC by shipping a
mapping with three rows, all green. `VpatCoverageTest` passes, #754's
generator (which per its own review "is satisfied by a generator that claims
almost nothing") prints an anemic-but-technically-honest document, and no
test anywhere goes red. This is the exact failure mode #754's review flagged
as "AC-1 is gameable toward a hollow-but-technically-honest document" — but
#754 only *consumes* the mapping; #753 is where the omission actually
originates and where a fix belongs. Recommendation: add an AC requiring the
mapping to enumerate the full applicable criterion set (WCAG 2.2 A+AA ∩ the
508/EN desktop-software mapping, per #507 Open Question 2's WCAG-2.2-AA
default) with every criterion present as claimed/exception/manual/not-
applicable — i.e. a completeness ratchet in the same house style as
`test/jls/ui/DialogCoverageRatchetTest.java`, not just a per-row correctness
check.

**2. [High] "Does not run" is not a well-defined, cheaply-checkable predicate against this test suite's actual shape, and the AC gives no mechanism.**
`VpatCoverageTest` is presumably itself a JUnit test executed inside the same
`mvn verify` pass as the tests it is supposed to gate on. Determining that
some *other* named test method "does not run" (as opposed to "does not
exist" or "does not pass") requires either (a) parsing Surefire/JUnit XML
reports written by a prior phase — order-dependent and not how the rest of
the suite is built — or (b) reflectively re-invoking the target test method
from inside `VpatCoverageTest`, which duplicates execution outside its normal
fixture/lifecycle and can silently diverge from what actually ran under CI.
Worse, the repo already has a real, evidenced case where "does not run" is
ambiguous by design: the skip-when-absent pattern (`README.md:223-231`,
`docs/standards-adoption/03-accessibility-conformance.md` §"External-tool
validation with skip-when-absent") uses `Assumptions.assumeTrue` to make a
test report as *passed-but-skipped* when a tool or bus is absent — exactly
the AT-bridge-dependent tests (Orca/AT-SPI2) this same capstone's PF-3/PF-6
work will produce. A criterion whose named test is tagged skip-when-absent
and skips in CI is, by JUnit's own semantics, neither "does not exist" nor
"does not pass" — whether it counts as "does not run" is undefined by the AC
and gameable either way depending on implementation choice.
Recommendation: name the concrete mechanism (e.g., a custom
`TestExecutionListener`/JUnit5 extension writing an execution ledger
`VpatCoverageTest` reads, not report-parsing) and explicitly state how a
skipped (`Assumptions`-aborted) test is classified — it should not silently
count as coverage for a claimed row.

**3. [High] AC-2/AC-3 inherit the parent capstone's core gameability gap almost verbatim, and #753 is the actual implementation site where it must be closed, not merely inherited.**
#547's own adversarial review (finding 1) already established that "a named
passing test" does not prove an assistive-technology bridge delivers
anything to a user — `scripts/build-installer.sh:143-146` derives the jlink
module set via `jdeps --print-module-deps`, which can never include
`jdk.accessibility`, so the shipped Windows `.msi` today bundles a runtime
with no Java Access Bridge and NVDA/JAWS get nothing from an installed JLS.
#753 is where "named test class and method" is the literal, sole bar for
"claimed" (AC-1, AC-2). As scoped, a green `AccessibleNameCoverageTest` (a
Java-Accessibility-API-level assertion, not a bridge-level one) is sufficient
under #753's own acceptance criteria to mark WCAG 4.1.2 / 508 §502.3.* /
EN 11.5.2.* as claimed-and-covered, on the exact build the playbook says
should rate those "Does Not Support" for the primary Windows distribution.
Recommendation: AC-1's mapping schema should carry an evidence-tier field
(API-level assertion vs. dated AT-bridge session per
`docs/accessibility-at-checklist.md`), and AC-2 should require the stronger
tier for the 502.3.*/11.5.2.*/4.1.2 criterion class before `VpatCoverageTest`
allows them "claimed" — not defer this distinction to the generator (#754)
or to prose review.

**4. [Medium] `ordering_after: [TASK-C549-2]` is plausible but the issue never states *why*, and the transitive dependency on TASK-C549-1 is left implicit.**
#758 (TASK-C549-2)'s own AC-4 is exactly the input #753 needs: "the manual
remainder is listed in one place with its execution procedure, so #547's VPAT
can consume the automated/manual split without re-deriving it." That's a
real and correct dependency — but #753's body never says so; a reader has to
reconstruct the reason from #758's text. #758 itself depends on
`TASK-C549-1` (#756), so #753 transitively can't start meaningfully until
both land, which is only implicit. Separately, #753 says nothing about the
`TASK-C544-*` family (#547's review flagged #741/#745 as undeclared
dependencies of the *sibling* task #754) — if a Swing-limit exception
(KC-26-2) from PF-3's spike needs to appear in #753's mapping as an
"exception" row (AC-1's third category), that dependency is absent from
`ordering_after` here too. Recommendation: state the dependency rationale
explicitly and confirm whether PF-3/#544-series exceptions are in scope for
#753's initial mapping or deferred to a later REPLAN.

**5. [Medium] "recorded" (AC-3) names no artifact, format, or location — a repeat of the same ambiguity already flagged against a sibling issue in this family.**
"A criterion added as claimed with no test turns the check red, and that red
run is recorded before any pass is counted" is unfalsifiable as written: no
CI artifact, doc section, or reproducible-mutation table is specified. #549's
adversarial review flagged this identical phrase-shape ("recorded" with no
defined artifact) as High severity against its own AC, and recommended the
project's established pattern: `docs/keyboard-a11y-verification.md`'s
"Red-on-break evidence (re-runnable)" section, which names the exact
mutation and the exact failure signal. #753 should adopt the same concrete
pattern rather than leave "recorded" to a PR-description sentence.

**6. [Medium] The mapping schema (AC-1) is under-specified for criteria that are legitimately backed by more than one test, or that rate "Partially Supports" rather than a clean pass/fail.**
"Naming a test class and method" (singular, per AC-1's own wording) does not
accommodate the playbook's own rating table
(`docs/standards-adoption/03-accessibility-conformance.md` lines 382-389),
where e.g. WCAG 4.1.2 is "Partially Supports" because Swing chrome passes and
the canvas does not — evidence for the same criterion legitimately spans
multiple tests (`AccessibleTreeGoldenTest`, `AccessibleNameCoverageTest`) and
a *known, permanent, non-test-closeable* gap (the canvas scene model,
explicitly not being built per the playbook's recommendation). AC-1 gives no
schema for "claimed at Partially-Supports strength, tests X and Y cover the
Supports part, canvas gap is a named permanent exception" — only a flat
test-exists/doesn't-exist binary. Without this, either #753's schema will be
reworked mid-implementation (cost risk against the 0.5-1 mw band) or #754's
generator will have to paper over the distinction, reintroducing the
over-claiming risk finding 3 already describes.

**7. [Medium] Feasibility/cost: `band_mw: 0.5-1` (roughly half a day to one day) looks significantly underscoped against what AC-1–AC-4 actually ask for.**
The project's own playbook budgets **2 maintainer-days** just for the *desk*
gap-analysis pass across "~90 criteria × 5 surfaces" (§5) — before any
tooling exists — and that is scoped as prose-table triage, not a
machine-checked, JUnit-integrated coverage gate with exists/runs/passes
detection (finding 2) plus a completeness ratchet (finding 1). Building
`VpatCoverageTest` with correct "does not run" semantics, designing a mapping
schema that survives finding 6, and populating even a partial mapping is
plausibly several days of work, not half a day. Recommendation: re-derive the
cost band against a concretely scoped mapping size (e.g., "cover the ~30
criteria the playbook already has evidence for; leave the rest as explicit
`Not Evaluated` placeholders") rather than leaving "0.5-1" to absorb whatever
scope creeps in during implementation.

## What's solid

- The core mechanism (a named criterion→test mapping that fails the build on
  a claimed-but-uncovered criterion) is a good structural fit for this repo's
  existing coverage-ratchet house style (`DialogCoverageRatchetTest`,
  `IdentityKeyCoverageTest` family), so the general shape is buildable, not
  speculative.
- Distinguishing manual-checklist claims as an explicit category (AC-4) so
  they "cannot be counted as automated coverage by omission" is a specific,
  checkable requirement and directly closes a real failure mode the
  playbook's own "Top three ways this goes wrong" list names.
- No licensing, security, or compatibility hazard: this is in-tree tooling
  and test infrastructure over an existing test suite; it touches no saved
  file format, no batch-interface contract, and introduces no new external
  dependency.
- The task correctly defers full document authoring to its sibling (#754)
  rather than absorbing that scope itself — clean task-boundary hygiene.

## Verdict rationale

The mapping-and-gate concept is sound and matches real project precedent,
but as literally acceptance-criteria'd it has no completeness requirement
(finding 1), leaves its core enforcement predicate ("does not run")
technically undefined against a test suite that already uses
skip-when-absent semantics (finding 2), and inherits the parent capstone's
API-level-vs-bridge-level evidence gap at the exact point where it is
cheapest to close (finding 3) — three defects that would let a fully green
`VpatCoverageTest` justify claims the project's own playbook says are false
today. These are fixable by tightening the acceptance criteria before
implementation, not by abandoning the task — hence **needs-rework**, not
should-not-proceed. Findings 4-7 (dependency rationale, "recorded" artifact,
mapping schema for split ratings, cost band) should be resolved in the same
pass since they compound the risk in findings 1-3 rather than standing
independently.
