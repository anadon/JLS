# Issue #733: TASK-C555-2: the README carries one performance line that cites the doc — and no public performance claim exists anywhere the doc does not back
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

It is not README copy. #512 names the deficit precisely — JLS scored 2/5 on
scale/perf "for lack of receipts, not lack of speed"; the gap is *epistemic*. #733
is the task that closes the loop: once a measured number exists (#554) and is
published with its method (#555), the project must be unable to say anything about
its own speed that the measurement does not support. The deliverable is a
**standing property of the claim surface**, not a paragraph.

That ambition is exactly on the project's arc. This repository already has an
unusual discipline about the provenance of everything it publishes: `SHA256SUMS`,
signed build-provenance attestations, keyless cosign on the container, a
byte-reproducible jar with `.buildinfo` and `docs/reproducibility.md`, a CycloneDX
`bom.json` — and, in the README's own voice, explicit scope statements about what
each guarantee does *not* cover (the installer non-reproducibility paragraph, the
unsigned-macOS paragraph, the JLS 4.1 memory-drop caveat). A performance number is
the one public claim that currently has no such chain. Building one is coherent
with everything around it.

Three things about *how* #733 proposes to do it pull against that arc.

## 1. The check lints prose where this project checks derivation

AC-3 asks for a build check that fails when "a performance-shaped claim appears in
README or `docs/` without a citation to the doc." Citation is the weakest possible
predicate: it passes a wrong number attached to a right link, which is precisely
the failure #512 fears (AC-4 there: "before a published number is a lie").

The tree already contains a much better pattern, used repeatedly:
`test/jls/HotkeysHelpAccuracyTest.java` parses the help page and asserts each row
names *exactly* the accelerator `EditOp` binds; `CliFlagTableTest` makes the flag
table the single authority and proves `usage()` and the parser both derive from it;
`FileFormatSpecTest`, `HelpTopicsTest`, `ExtensionPointCatalogTest` do the same for
their documents. The house style is: **the doc's content is checked against the
source of truth, not against the presence of a link.**

Concrete alternative: #554/#555 emit a committed machine-readable measurement
record (say `test/fixtures/perf/measurements.json` — fixture, node count, pass
count, clocking regime, JDK, flags, events/s, cycles/s, band). A
`PerformanceClaimsTest` then extracts every declared claim from README and
`docs/performance.md` and asserts *numeric equality* with that record. AC-1's "the
number it states matches the doc" stops being a one-time human check and becomes a
red build. Same cost band; strictly stronger instrument; and it is the mechanism
the project would build anyway for the next claim class.

## 2. `docs/` is the wrong scope, and the seam to cut is document *kind*

Measured against this checkout: README contains **zero** performance-shaped tokens.
`docs/` contains 57 markdown files, 31,574 lines, and **~230** number+unit tokens
that a "performance-shaped" regex will see. `docs/capability-roadmap/keystone-c-performance.md`
alone is 869 lines and opens by declaring its numbers were "measured this session
on this tree at HEAD" with harnesses and reproduction commands in its own §12 —
10.85 ns/op vs 21.11 ns/op, 4.32 ns/node, 37.6% of profile samples, 47.7% queue
machinery. `docs/hdl-support-research.md` cites ANTLR's 131 s / 1.3 GB. The
standards-adoption set quotes third-party figures throughout.

None of those are claims about JLS's shipped performance that `docs/performance.md`
could ever "back". So AC-3 as written has two outcomes: the build is red on day one,
or the check acquires an exemption list — and an exemption list is the exact
"quiet reintroduction" vector the criterion exists to close.

ARCHITECTURE.md already draws the line this needs: repo documents are "the
normative home for contracts," distinct from the dated evaluations
(`flatlaf-evaluation-2026-07.md`, `library-survey-2026-07.md`,
`mutation-testing-trial-2026-07.md`) and the roadmap sweeps, which are *records of
an investigation on a date*, not live promises. Propose: mark each document's kind
explicitly — a one-line front-matter or HTML comment (`claims: public` /
`claims: record` / `claims: external`) — and enforce the rule only on
`claims: public` documents, with a separate, cheap rule that a `record` document
must carry its own date and method (which keystone-c already does). This makes the
rule **decidable** rather than heuristic, is a few hours of work across 57 files,
and generalizes to the other guarantee classes this repo makes and will have to
police the same way: accessibility conformance, reproducibility, security scope.

A second predicate falls out of the same marking: a claim about *JLS* must derive
from the record; a claim about *Digital, ANTLR, Verilator* must carry an external
citation. One regex over prose cannot tell those apart. Explicit marking can.

## 3. AC-4 asks for a reconciliation that should be made impossible instead

AC-4 wants the doc and #335's internal-plan constants "shown not to disagree —
cited, not forked." But #335's constants live in `docs/machine-calibration.md`,
which **does not exist anywhere in this tree**, and #335 itself records all six of
its measurement tasks as "not filed". So AC-4 is a manual audit against a registry
that does not yet exist and, by #335's own diagnosis, is not yet fit to be audited
against.

The visionary correction is to take "cite, don't fork" seriously one level down,
at the artifact rather than the prose. There should be **exactly one** committed
measured-constants record. The internal plan reads from it; `docs/performance.md`
reads from it; the README line reads from it. Then "shown not to disagree" is not a
criterion anyone discharges — it is vacuous by construction, and it stays vacuous
next year. Two registries plus a periodic comparison is a fork with a chaperone.

## 4. What the README line should actually say

The README's audience is students installing a `.deb` and instructors wiring up an
autograder. A raw events/s figure is not decision-relevant to either. The README's
established voice is scope-honest and use-relevant, and its lead story for the
headless surface is grading: `-t` vectors, VCD, the container image, the
`docs/batch-interface.md` stability contract.

So the strongest one-liner is not "JLS does N events/s" but a throughput statement
in the units of the decision it informs — e.g. a censused CPU-scale fixture at a
stated simulated-clock rate, or a stated cost per graded circuit-run — with the
same "note the scope of this guarantee" framing the installer section uses, and the
link to the method. This also insulates the project against #512's KC-28-1: if the
head-to-head shows JLS behind Digital on raw speed, a published claim anchored on
*reproducible grading throughput* still says something true and differentiating,
because reproducibility is the axis this project has actually invested in.

## 5. Surface coverage the issue does not consider

README and `docs/` are not the whole public claim surface. Verified in this tree:
`resources/help/` currently has no performance language and `CHANGELOG.md` has one
incidental mention (line 821) — so the surface is clean *today*, which is the
cheapest possible moment to define it. A release-notes or CHANGELOG entry reading
"3× faster event loop" is the single most likely future unbacked claim, and it is
outside the AC-3 scope as written. Whatever check lands should name its surface
deliberately: README, `docs/` (public-kind only), CHANGELOG, in-jar help, and the
release-note template.

## Disposition

**Endorse-with-reframing.** The outcome is right and belongs to the project's
strongest habit. I would keep AC-1 and AC-2 as written, restate AC-3 as
*derivation-equality against a committed measurement record, scoped by declared
document kind*, and replace AC-4's audit with a single shared constants record so
the disagreement cannot arise. I would also expect the mechanism itself to be
factored into #554/#555 rather than built here: #555's FEAT-C28-3 owns staleness
and #335's TASK-0026 owns the ratchet, so a third bespoke gate filed at 0.25–0.5 mw
is the seam where this program builds the same thing three times.
