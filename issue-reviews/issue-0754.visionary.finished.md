# Issue #754: TASK-C547-2: the VPAT/ACR draft is generated, not authored — WCAG 2.2 AA under the desktop mapping, with Swing's limits listed as exceptions by name
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the VPAT vocabulary, #754 is an instance of the project's deepest
recurring move: **make a claim structurally unable to exceed its evidence.**
That move is everywhere in this tree — README's careful separation of what a
SHA256SUMS file proves from what a provenance attestation proves; the
"installers are *not* byte-reproducible, that is expected" paragraph; the
unsigned-macOS rationale filed as a decision rather than an omission; the
`docs/standards-adoption/11-costed-rejections.md` honesty rule; `ThemeTest`
enforcing the delta-E floor the javadoc claims. JLS's real product, alongside a
simulator, is a body of documents nobody has caught overstating. An ACR is the
first such document aimed at a reader with an institutional incentive to find a
false line in it. Generating it rather than writing it is exactly right.

So the goal is endorsed without reservation. The **mechanism** as specified is
the wrong shape in four ways, and the repository already contains the correct
shape — mostly in `docs/standards-adoption/03-accessibility-conformance.md`,
which is a 920-line playbook for this precise deliverable that #754 and #753
appear not to have consumed.

## Problem 1: a VPAT is a total function, not a filter

The issue's mechanism is a gate — "no claimed criterion without a named passing
test" (AC-1), inherited from #753's mapping over "each WCAG criterion JLS
intends to claim." That produces a document containing only the rows JLS wants
to talk about. **That document is not an ACR and procurement will reject it.**
The playbook is explicit (`03-accessibility-conformance.md:340-345`): every A/AA
row needs a real answer, "Not Evaluated is only permitted for AAA," and an
all-Supports report "is not credible and will be treated as such"; it goes as
far as prescribing that "Does Not Support" must appear at least once.

The right invariant is not *gating* but *totality*, which is a pattern this
project already owns: `test/jls/ElementRegistryTest.java`,
`test/jls/ui/DialogCoverageRatchetTest.java`,
`test/jls/HeadlessCoreRatchetTest.java`, `test/jls/PackageInfoRatchetTest.java`
— and CAP-26 §3.4 itself names registry-keyed totality as the fix for theming
decay. Reframed: the criterion registry enumerates the **entire** union of
{WCAG 2.2 A+AA under WCAG2ICT, 508 Ch.3/5/6, EN clauses 5/10/11/12}, and the
totality test fails when any criterion has no row. Each row carries a rating and
an evidence *kind* from a closed set: automated test, dated AT session,
Not Applicable + normative citation, or Does Not Support. "No claim without a
test" then falls out as one arm of a total function instead of being the whole
design. This is strictly stronger than AC-1 — under AC-1 the way to make a
failing criterion disappear is to stop claiming it; under totality there is no
such move.

## Problem 2: the evidence class is in-process; the shipped product is not

This is the fatal one. `03-accessibility-conformance.md:773-780` names it as the
single top failure mode: *"Every automated signal in this repo reads the Java
Accessibility API. Not one of them proves a bridge delivers anything to a
user."* And it has a concrete instance already found in the tree:
`scripts/build-installer.sh:141-146` derives the jlink module set from
`jdeps --print-module-deps`, which can never report `jdk.accessibility` because
nothing in the jar statically references it — so **every shipped `.msi` bundles
a runtime with no Java Access Bridge, and NVDA/JAWS read nothing at all from an
installed JLS.**

A generator built to #754's spec would consult a mapping of criteria to JUnit
tests, find `MenuMnemonicAndAccessibleNameTest` and friends green, and print
"Supports" for 502.3.* / EN 11.5.2.* / WCAG 4.1.2 — for a distribution where the
user gets silence. Every acceptance criterion in #754 would be satisfied. The
document would be false on its most-scrutinised rows. #754's exception
machinery does not catch this, because a missing bridge is not a "Swing
accessibility-API limit"; it is a packaging fact, and packaging facts are
invisible to a criterion-to-test mapping.

The reframe: the registry's evidence kinds must include **platform delivery**,
and the rating function must be a *minimum* over (API evidence, delivery
evidence), not a lookup on API evidence alone. Concretely, the generator refuses
to print above "Not Evaluated" for the interoperability rows without both a
green in-process test and a dated AT session recorded in the checklist the
playbook specifies (`docs/accessibility-at-checklist.md`) — the playbook's own
mitigation rule, mechanised. #745 (the NVDA checklist) is the raw material for
half of this and #754 already knows about it; what is missing is that the
checklist becomes an *input to the rating*, not merely a differently-formatted
paragraph in the output (AC-3 treats it as presentation only).

## Problem 3: a mapping file is an assertion, not a derivation

AC-4's integrity boundary — the claim-strength string is emitted by the
generator "so it cannot be raised by a prose edit" — protects the wrong surface.
Nobody's failure mode is editing the ACR's prose; the failure mode is editing
the *mapping*, which is a hand-maintained file naming test classes by string.
Point a claimed criterion at a passing-but-irrelevant test and the generator
launders it into a machine-verified claim. Test names in a side file are
assertions with a drift seam; the project knows this and pins such seams with
source-grep compensating controls (`test/jls/KeyPadAccessibilityPinTest.java`).

Better seam: put the binding **at the point of evidence**. A JUnit annotation
(`@Criterion("508-502.3.6")`, alongside the existing `@Tag("display")` idiom
that `pom.xml`'s `display-tests` execution already keys on) declares what a test
proves, in the test's own source. The generator reads the compiled test classes
plus the surefire XML from the run that produced the artifact, and derives the
mapping. Then #753's mapping file shrinks to what genuinely cannot be derived —
Not Applicable rows with their normative citations — and drift becomes
impossible rather than test-detected. This also makes #753's `VpatCoverageTest`
cheaper: with derivation, "claimed but untested" is unrepresentable.

## Problem 4: the ACR is a rendering, not the truth

#754 generates *a document*. The playbook warns that the ITI template is revised
roughly annually and that its current revision is unverified
(`03-accessibility-conformance.md:33-37`), and that publication has at least
four destinations: `docs/accessibility-conformance-report.md`,
`ACCESSIBILITY.md`, a release asset via `.github/workflows/release.yml`, and an
in-jar help page. Generating the ITI-shaped markdown directly couples the truth
to one template edition and one destination.

The elegant cut: the generator's output is a **machine-readable conformance
dataset** (criterion id, standard + edition, surface, rating, remark, evidence
pointers, version/commit/jar hash), and the ITI document, the ACCESSIBILITY.md
front door, and the help page are renderers over it. A template revision becomes
a renderer change; a procurement question becomes a query. It also lets the ACR
do something almost no commercial ACR does: bind every claim to a reproducible
artifact hash, which this project uniquely can (`docs/reproducibility.md`,
`VersionIdentityTest`). And it should be byte-deterministic on the house
convention (`test/jls/DeterministicSaveTest.java`,
`SvgExportTest.exportingTwiceIsByteIdentical`) so per-release regeneration
diffs mean something — #754 does not mention determinism and CAP-26's AC-6
covers accessible *exports*, not this.

## What I am disregarding, and why

I am disregarding AC-1 as written (claims gated by named tests) in favour of
totality over the full criteria union, and AC-4's framing of the integrity
boundary (prose vs. generator) in favour of derivation vs. assertion. Both
stated criteria are satisfiable by a generator that produces a document which is
confidently wrong on the rows that matter most. AC-2, AC-3 and AC-5 survive
intact, and AC-5 (KC-26-3) gets *stronger* under the reframe, because "cannot be
made mechanical" acquires a sharp test: if the delivery evidence does not exist,
totality forces the rows to Not Evaluated rather than letting them vanish.

## Sequencing, and one honest tension

The same playbook that makes this work implementable also records a judgment
against doing it early: *"If you only have budget for one, build the outline
view and fix the contrast — that helps an actual blind student; the ACR only
helps a procurement officer"* (`03-accessibility-conformance.md:812-819`), and
*"the §1 bridge fix is not landed first"* is listed as a hard do-not-proceed
condition. CAP-26's ordering (PF-5 after PF-1 and PF-3) respects the spirit of
this, but nothing in #547 or #754 owns the bridge fix, the contrast defects, or
the mouse-only trace window (`src/jls/edit/Trace.java:20`) — all of which the
playbook proves are red *today* and each of which directly determines an ACR
row. Filing #754 without an owner for the bridge fix risks a generator whose
first honest output is a wall of "Does Not Support", which is a fine outcome for
the document and a bad one for the student. Recommend the REPLAN on #507 pull
`03-accessibility-conformance.md` §1–§4 into the capstone's roster explicitly
rather than leaving them as an unowned prerequisite in a docs file.

## Verdict

**endorse-with-reframing.** The goal is squarely on the project's arc and the
capstone is right to want it mechanical. Rebuild the mechanism as: a total
criterion registry with a totality ratchet; ratings derived from annotations at
the point of evidence plus surefire output; a rating function that takes the
minimum of API evidence and platform-delivery evidence; and a deterministic
conformance dataset with the ITI document as one renderer among several. That
version cannot produce the specific false ACR the current spec would have
produced on day one.
