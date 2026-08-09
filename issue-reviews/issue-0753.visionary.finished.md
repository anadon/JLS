# Issue #753: TASK-C547-1: every accessibility criterion is bound to a named automated test, and a claim with no test fails the build
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

#753 is the load-bearing half of #547. The generator (#754) is a printer; this task
builds the thing that decides what may be printed. Stripped of VPAT vocabulary, its
end is the project's oldest habit made mechanical for a new domain: **a document may
not out-claim its evidence.** That habit already has a canonical form in this tree —
`docs/file-format.md` + `FileFormatSpecTest`, `hotkeys.html` +
`HotkeysHelpAccuracyTest`, `docs/extension-points.md` + `ExtensionPointCatalogTest`,
`docs/keyboard-a11y-verification.md` + its named tests. A criterion-to-test mapping
with a check that reddens on an unbacked claim is that habit applied to accessibility
conformance. The instinct is right and this is the right seam to cut.

Three things about *how* it is cut pull against the arc, and each has a cheaper,
stronger alternative already present in the repository.

## 1. The mapping is keyed by "criteria JLS intends to claim" — the one key that hides the interesting rows

AC-1 keys the file on the criteria JLS "intends to claim." That makes the mapping a
subset chosen by the author, and it hands the author a silent escape: any criterion
that will not go green is removed from the intent list and the build goes green with
it. AC-3's falsification drill (add a claimed criterion with no test, watch it go red)
tests the check against the one failure mode nobody has — the honest maintainer adding
a claim — and not the one everybody has: quietly not claiming.

Key it on **totality over a pinned criteria union** instead — {WCAG 2.2 A+AA under
WCAG2ICT, 508 Ch.3/5/6, EN clauses 5/10/11/12} — with the ratchet failing when any
criterion in the union has *no row*. This is the same move as `ElementRegistryTest`,
`DialogCoverageRatchetTest` (every compiled `ElementDialog` subclass must be
represented), `PackageInfoRatchetTest`, and `HeadlessCoreRatchetTest`: enumerate the
world, then require coverage of it. Under totality "no test, no claim" survives as one
arm of a total function, and *dropping a claim stops being a way out* — the row still
exists, now rated `Does Not Support` or `Not Evaluated` with a reason. The
`03-accessibility-conformance.md` gap-analysis procedure (§5) already says "every A/AA
row needs a real answer" and "Not Evaluated is only permitted for AAA"; a
claimed-subset mapping structurally cannot express that, and the union is desk work
that has to be done once either way.

The second, larger payoff: a total ledger is a **work plan**, not paperwork. Filed
today with every row at its honest rating, it is the single shared statement of what
CAP-26's other five features buy — #542 flips 1.4.1/1.4.11, #549's ratchet flips the
2.1.1/4.1.2 chrome rows, #544's spike either flips or permanently pins the 502.3
live-announcement rows. A subset mapping is a trophy case; a total ledger is a
scoreboard.

## 2. "A named test exists and passes" is the weakest predicate the project owns; it has a much better one

AC-2 checks that a named test exists, runs, and passes. That is launderable by
construction: point 4.1.2 at `MenuMnemonicAndAccessibleNameTest`, which is green, and
a machine-verified claim about the whole product is minted from a fact about the menu
bar. A test name in a side file is an assertion with a drift seam, and this project
already knows the compensating control — `KeyPadAccessibilityPinTest` is a source-grep
pin for exactly this reason.

The stronger predicate is in the tree, in the very document this work is downstream
of. `docs/keyboard-a11y-verification.md` carries a **"Red-on-break evidence
(re-runnable)"** section: *revert the palette a11y-name fix → `PaletteButtonAccessibilityTest`
red; change any key in `hotkeys.html` → `HotkeysHelpAccuracyTest` red.* That is the
project's native unit of evidence, and it is qualitatively different from "a test
passes": it names the mutation the test is sensitive to. Make the ledger row's
evidence field a **falsifier** — cited test plus the recorded breakage that reddens it
— and irrelevant-test laundering becomes visible in review, because a row citing
`MenuMnemonicAndAccessibleNameTest` for a canvas criterion cannot state a falsifier
about the canvas. AC-3's one-time red run then generalizes from a ceremony performed
against the checker into a per-row property of the evidence.

Corollary on the binding's location: derive what is derivable. A JUnit annotation at
the point of evidence (`@Criterion("508-502.3.6")`, alongside the `@Tag("display")`
idiom `pom.xml`'s `display-tests` execution already keys on) plus the surefire XML of
the run that produced the artifact makes "claimed but untested" unrepresentable rather
than test-detected. The in-tree file then holds only what cannot be derived — the
criteria union itself, ratings, remarks, Not-Applicable citations, falsifiers.

## 3. Two categories are not enough, and the missing one is where the ACR would be false today

AC-4 splits evidence into automated and manual-checklist. The split that decides
correctness is different: **in-process API evidence vs. platform delivery.** Every
automated a11y signal in this repo calls `getAccessibleContext()` inside the test JVM.
`scripts/build-installer.sh:145` derives the jlink module set from
`jdeps --print-module-deps`, which can never emit `jdk.accessibility` because nothing
in the jar references it — so the shipped `.msi` bundles a runtime with no Access
Bridge and NVDA reads nothing, while `PaletteButtonAccessibilityTest` and friends stay
green. A mapping with AC-4's two categories records that state as *automated
coverage*. The playbook names this as failure mode #1 and states the mitigation as a
rule: no row above "Not Evaluated" for 502.3 / 11.5.2 / 4.1.2 without a dated AT
session in `docs/accessibility-at-checklist.md`.

So the mapping's category field should be a closed set of **evidence classes** —
`automated-in-process`, `platform-delivery` (packaging/bridge facts), `at-session`
(dated, naming AT and OS versions), `not-applicable` + normative citation,
`platform-limit` (the named Swing exceptions #547 wants) — and the check should
enforce a **rating ceiling per class**, with the row's rating the minimum over its
evidence. That is the one rule that makes the whole feature's honesty claim true
rather than merely mechanical, and it belongs here, in the checker, not in the
generator that formats the output.

## The alternative in one paragraph

`docs/accessibility-conformance-ledger.md` — authored, reviewable, diffable, one row
per criterion over the full union, each row `rating` + `class` + `surfaces` (chrome,
canvas, simulator+trace, help, CLI) + cited tests + falsifier + citation. One headless
`CriterionLedgerTest` joining `mvn verify`, enforcing in order: totality against the
pinned union; resolution of every cited test FQN to a real `@Test`; rating ceiling by
evidence class; `Supports` only when `surfaces` covers every in-scope surface (which
today forecloses a 2.1.1 "Supports" while `Trace` is a 626-line mouse-only `JPanel`);
staleness demotion for `at-session` citations older than the current minor release.
`VpatCoverageTest` survives as the second clause of that list.

## Sequencing, and the reason to promote this task

`ordering_after: [TASK-C549-2]` sequences the ledger behind the evidence it will
transcribe. Invert it: this artifact is cheapest and most useful **first**, filed red,
with the union enumerated before the other five features run — where it can steer them
rather than audit them. And there is a structural argument specific to #753: #547
carries a kill criterion (KC-26-3 — if the generator cannot be made mechanical, no
hand-authored VPAT ships). As specified, #753's mapping is scaffolding for exactly
that killable feature; if the kill fires, the mapping is orphaned. As a ledger it is
the playbook's own documented fallback (`ACCESSIBILITY.md` — "a half-audited ACR is
worse than no ACR") already mechanized, and it ships whether or not the generator
does. Designing the artifact to survive its consumer's kill switch is worth more than
the generator.

One prerequisite this task should own or block on, since it decides the ratings before
anything else does: the `jdk.accessibility` / `assistive_technologies=` installer fix,
pinned in the `KeyPadAccessibilityPinTest` style. Nothing in CAP-26 currently owns it,
and nothing owns the dated AT session logs the rating ceiling makes load-bearing.

## What I am disregarding, and what I would keep verbatim

Disregarded: **AC-1's key** ("each WCAG criterion JLS intends to claim") in favour of
totality over the criteria union, and **AC-2's predicate** ("names a test that exists,
runs, passes") in favour of resolution-plus-falsifier with a rating ceiling by
evidence class. Both are satisfiable today by a mapping that is green, mechanical, and
false about the primary Windows distribution — which is precisely the outcome the task
exists to prevent. AC-3 is kept and strengthened (the red run becomes per-row, not
per-checker); AC-4 is kept and widened from two categories to five, because the
category that would have caught the real defect is neither of the two named.

The insight is right and belongs to #753 rather than to #547: the honesty of a
conformance document is a property of its *binding mechanism*, not of its prose. Build
the binding so it enumerates the world, names what would break each claim, and refuses
to let in-process evidence speak for a shipped runtime.
