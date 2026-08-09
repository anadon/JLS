# Issue #490: FEAT-059: a drawn line reflects — 5.500 V on a 3.3 V rail, a flat 3.300 V when terminated, and 4.368 V when only the edge rate moves
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The physics and the code citations that anchor this issue check out under
direct verification — a rare thing for a plan this elaborate. But the issue
also contains a genuine internal numeric contradiction in its own acceptance
math, a stated design goal that conflicts with the actual test it cites as
the mechanism enforcing that goal, and a Definition-of-Done clause that is
already false today because the evidence documents it cites do not exist on
any branch of this repository. None of these are nitpicks; each sits on the
issue's own critical path (K18-1's stop gate, K9's palette obligation, and
the closing checklist respectively).

## Findings, most severe first

**1. [High] The truncation worked example is tuned to a laxer tolerance than the acceptance gate it must satisfy.**
The Feature-Level Interface section derives the series-truncation term count
for `tol = 1e-9`: "`|Γs|^k < 10⁻⁹` needs `k ≥ ⌈9 ln 10 / ln 1.5⌉ = 52`." That
arithmetic is correct (verified independently). But Integration Criterion 1 —
the single most load-bearing gate in the issue, K18-1, which must pass
*before any permanent surface lands* — requires the implementation to agree
with the closed-form lattice "to **1e-12 relative**." Redoing the same
formula for `tol = 1e-12` gives `k ≥ ⌈12 ln 10 / ln 1.5⌉ = 69`, not 52. An
implementer who takes the issue's own worked example (52 terms) as the
truncation target ships a series whose floating-point residual is on the
order of `1e-9` relative — three orders of magnitude too loose to pass the
1e-12 gate the same document mandates elsewhere. Either the "52" figure is
wrong for the stated purpose, or the 1e-12 criterion is unrealistically
tight relative to the issue's own truncation guidance; as written they
disagree with each other.
*Recommendation:* before filing the child, recompute and state the term
count against 1e-12 (or against whatever tolerance the truncation
sub-feature is actually contracted to hit), and make Criterion 1's bound and
the Feature-Level Interface's worked truncation example cite the same
number.

**2. [High] The Definition-of-Done cannot currently be satisfied, and the issue supplies no path to fix it.**
Item 2 of "Completion Criteria" requires "Every cited evidence document and
permalink resolves on the default branch at close." The issue's own Evidence
section (item 9) cites `docs/plan/capstones/CAP-18-net-that-stopped-being-a-wire.md`,
`docs/plan/features/FEAT-059-closed-form-transmission-line-and-reflection-lab.md`,
and `docs/plan/evidence/highfreq-determination.md` as the source of the
scope table and the cost band, and the closing Cost reconciliation cites
`REGISTRY.md`. None of these paths exist in this checkout, on `master`
(`git ls-tree -r --name-only master | grep -c '^docs/plan/'` → `0`), or
anywhere in `master`'s ancestry — the commit that once carried `docs/plan/`
(`3a81a4a`) and the commit that later deleted it (`742da74`, message "docs:
remove the planning corpus now that it is encoded in issues") are both
absent from `master`'s history entirely. There is no `REGISTRY.md` anywhere
in the repository. The issue's own D12 caveat acknowledges these paths don't
resolve at `evidence_commit` (`2d0ca9d`) and says they resolve "at the
working-tree commit that carries them" — but that commit is not on the
default branch, so the DoD's own resolve-on-default-branch bar is unmet by
construction, not by drift.
*Recommendation:* either commit the planning corpus (or a `REGISTRY.md`
successor) to the default branch, or rewrite the DoD item and every
citation to point at whatever venue "encoded in issues" actually means in
practice, before this is treated as actionable.

**3. [Medium-High] "Context-derived visibility" contradicts the exclusion-list test the issue cites as its own evidence.**
Task 4 and Criterion 4 both insist the element must be "kept out of the
default palette by a visibility rule rather than by an exclusion list."
The issue's own Evidence item 4 quotes the actual mechanism,
`test/jls/edit/PaletteContractTest.java:44-45`:
```java
private static final Set<String> NON_PALETTE_TAGS =
        Set.of("SubCircuit", "WireEnd", "TestGen");
```
with `:61-65`'s `paletteIsTotalOverTheElementRegistry` asserting **exact set
equality** between `ElementRegistry.all()` minus that 3-tag set and
`Palette.entries()`. Verified directly: registry count is 35
(`grep -c "new ElementType(" src/jls/elem/ElementRegistry.java`), palette
count is 32 (`grep -c "entry(Group\." src/jls/edit/Palette.java`), and
35 − 3 = 32 — the test currently holds with zero slack. That mechanism is a
static, compile-time tag set; it has no notion of "context" (canvas state,
user mode, first-year vs. advanced). For the new element to raise the
registry to 36 while the test still asserts total equality at 32, the *only*
way to satisfy the existing test as written is to add its tag to
`NON_PALETTE_TAGS` — i.e. exactly the exclusion-list mechanism the issue
says it is rejecting. If a genuinely dynamic/contextual visibility rule is
intended, that requires restructuring `PaletteContractTest` itself, which is
scope no planned task names and no cost band accounts for.
*Recommendation:* either concede the design is "add to the exclusion set"
(and drop the "not an exclusion list" language), or add a named task for
reworking the palette totality test's model before K18-4 can be evaluated
honestly.

**4. [Medium] Evidence item 4's own sentence is self-contradictory.**
"At `2d0ca9d` the registry has **35** types against **32** palette entries,
so **a green test currently enforces the violation**" — but a test asserting
exact set equality that is passing (green) by definition means there is no
current violation (confirmed above: 35 − 3 = 32 exactly). A green totality
test with zero margin is not "enforcing a violation," it's holding at
capacity. This is a small thing in isolation, but it is one of eleven
numbered evidence claims in the issue, most of which we could verify by
hand and did; finding one that is internally incoherent on inspection means
the other ten need to be checked individually rather than trusted as a
block, which is exactly what this review did (and found solid, see below).

**5. [Medium] `blocked_by: [487, 367]` silently omits a dependency the issue's own body asserts exists.**
Section 3 states #490 "Consumes... FEAT-058's two declared attributes
(#486)," and Integration Criterion 5 is explicitly "joint with #486." Yet
#490's machine-readable `blocked_by` list is `[487, 367]` — #486 is absent.
The DAG-walk paragraph patches this by noting #486 is reachable
transitively (#487's own `blocked_by` includes 486), and states "#490 needs
nothing #487 produces" — the #487 edge is pure permanence ordering, not
data. That's an internally consistent story *today*, but it means #490's
real data dependency on #486 is carried only as a side effect of an
unrelated, larger, and independently at-risk feature (#487, priced at
5.5–9.5 mw, the most expensive of the three rungs, with its own K18-2
re-planning trigger that can shrink or reshape it). If #487 is ever
descoped, re-costed, or split such that its `blocked_by [486, ...]` edge is
dropped or reordered, #490 loses its only path to a dependency it actually
needs, with no direct edge to catch the break. The issue's own convention
elsewhere is that composition edges are "mirrored on both ends" — this one
isn't.
*Recommendation:* add #486 to #490's `blocked_by` directly, redundant with
the transitive path or not; relying on a permanence-only edge to also carry
a real data dependency is fragile by the issue's own stated standards.

**6. [Medium] Process overhead is large relative to the implementation, and nothing is filed yet.**
The issue itself describes the kernel as "eight lines of textbook theory,"
priced at 2–3.5 mw (plus 0.5–1 mw for the shared trace row). Against that,
the issue imposes 8 global invariants, 6 cross-child integration criteria,
a 10-item Definition of Done, and a mandatory `REPLAN:` comment on every
single response mirrored onto #313 — plus the requirement to read #341
(FEAT-027) before starting, per Open Question 2. All four `planned_tasks`
are listed "Not filed." There is no actionable child issue an implementer
can pick up today; #490 as written is a coordination document, not a
work item, and the coordination machinery is heavier than the payload it
coordinates.

## What holds up

- **The reflection-series math is correct and internally consistent.**
  Independently recomputed: `Γs = -2/3` exactly, `V0 = 2.75 V`, and all
  seven listed ring values (`5.5000, 1.8333, 4.2778, 2.6481, 3.7346, 3.0103,
  3.4931`) match `V_k = 3.3·(1-(-2/3)^k)` to the printed precision. The
  correction of a prior document's erroneous `3.1914` figure is a genuine,
  checkable self-correction rather than an assertion.
- **Every direct code/doc citation checked resolves and matches**:
  `Element.java:17-18` (sealed `permits DisplayElement, LogicElement, Wire`),
  `LogicElement.java:17-20` (a second, more granular sealed `permits` list —
  confirms the registration-tax claim rather than contradicting it),
  `WireNet.java:405` (`private @Nullable BitSet value`),
  `docs/simulation-semantics.md:26` and `:44` (dimensionless time, BitSet
  value domain), `Adder.java:33/261` (unitless `defaultPropDelay`), and the
  35-registry/32-palette counts all verified byte-for-byte against HEAD.
- **The rejected-alternatives analysis (WireNet-as-distributed-net, RLGC
  ladder, routing through the analog engine) is genuinely argued with
  numbers** (ngspice comparison, 1/N convergence, format-version cost) rather
  than asserted, and the licensing review (Evidence item 8) reads actual
  license terms rather than assuming compatibility.
- **The sequencing rationale (cheapest-and-most-permanent-goes-last) is a
  coherent, explicitly-argued position**, not an unexamined default — even
  though finding 5 above shows one seam in how that ordering is wired.

## Verdict rationale

Two of the six findings sit on criteria the issue itself calls
non-negotiable stop conditions (K18-1's 1e-12 gate, the DoD's document-
resolution requirement), so this is not sound-with-concerns. But the
physics, the scope boundary, and most of the citation work are demonstrably
solid, so this is not should-not-proceed either: the underlying feature
design is close to buildable once the truncation bound is reconciled with
the acceptance tolerance, the palette-visibility mechanism is honestly
named, and the evidence citations point somewhere that actually exists on
the branch this will be judged against.
