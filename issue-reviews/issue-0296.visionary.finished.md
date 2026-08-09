# Issue #296: CAP-00: close a decade of deferred maintenance behind eight standing ratchets that cannot silently regress
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the machine block, the DAG walk and the four REPLAN comments, #296 makes one
claim: **JLS should stop relying on the maintainer's memory as an enforcement mechanism.**
Bus factor 1, a decade of drift, and the observation that a regression in any of these
eight areas is "caught by review or not at all."

That claim is correct, and it is not new — it is the project's own idiom, already
institutionalized. The tree carries `HeadlessCoreRatchetTest`, `NotificationRatchetTest`,
`CollabSecurityRatchetTest`, `NullMarkedRatchetTest`, `PackageInfoRatchetTest`,
`PointerApiRatchetTest`, `SocketConfinementRatchetTest`, `DialogCoverageRatchetTest`, plus
`ArchitectureRulesTest` (ArchUnit over bytecode) and five `*PolicyTest` classes.
`HeadlessCoreRatchetTest`'s header documents the shrinking-baseline pattern as a *named
convention*. `ARCHITECTURE.md`'s "Recorded decisions" section does the same for judgments
that cannot be expressed as a test: rationale plus an explicit revisit trigger.

So the thesis is endorsed without reservation. What I am rethinking is the *shape* of the
vehicle, because in three specific places the issue cuts against the seam the project has
already established.

## Reframe A — the deliverable is an invariant registry, not eight arms

AC-1 pins "the ratchet count is 8" in a new class `test/jls/DeferredMaintenanceRatchetTest`,
and states that **no feature owns the suite**. Both halves are wrong in the same way.

*The name encodes a date, not an invariant.* Every other ratchet in this tree is named for
the property it protects and lives next to its subject. "Deferred maintenance" is a
property of the 2026 backlog, not of the system. In five years the class documents nothing
except that someone once did an audit. Compare `HeadlessCoreRatchetTest` — still legible,
because #77's invariant is still the invariant.

*The count is the wrong thing to protect.* §5 says "Do not silently drop an arm — AC-1's
count of 8 is the thing being protected." But 8 is also a ceiling: find a ninth
never-enforced invariant and AC-1 goes red for the *right* reason. The issue already knows
this pattern — AC-3 deliberately refuses to pin N ("only the count *without* a timeout is
pinned, so a legitimate 24th job cannot falsify this step") — and then does the opposite
one criterion earlier.

*An unowned class in a bus-factor-1 project is an orphan by construction.*

**Concrete alternative.** Make the capstone's product a `RatchetRegistryTest`: a table of
`invariant → enforcing test → owning issue`, plus a meta-assertion that (a) every row
resolves to a live `@Test`, (b) every class matching `*RatchetTest`/`*PolicyTest` appears in
the table, and (c) rows are append-only — deleting one requires a `WAIVED:` row naming its
successor. The eight arms then land **where they belong**: totality with
`ElementRegistryTest`, fail-loud loading with `CircuitLoadErrorTest`, event-limit semantics
in `test/jls/sim/`, and the two build-configuration lints (CI timeouts, JaCoCo floors) in a
new `BuildPolicyTest` — a genuinely new seam, since *nothing under `test/` parses
`.github/workflows/` or `pom.xml` today*. AC-1 becomes "the registry is complete and
append-only," which is self-maintaining, generalizes to the other eighteen capstones that
will each want the same guarantee, and survives CAP-00's close.

## Reframe B — the critical path is an accident of categorization

The band is 29–52 mw. **FEAT-008 (#316) alone is 12–20 mw — 38% of the top** — and it is
the one row whose prerequisite (#337, the headless `CircuitOp` layer) CAP-00 does not fund,
which the issue honestly records as risk 6 and as "the schedule is not self-contained."

Why is a 12–20 mw editor decomposition inside a maintenance capstone? Because of D-07. And
D-07 is not a defect. `pom.xml:408` reads: *"jls.edit is deliberately unfloored until the
#91/#84 work makes editor code testable."* That is a **recorded decision with a stated
trigger** — precisely the artifact `ARCHITECTURE.md`'s "Recorded decisions" section exists
to legitimize. Listing it as one of eight "verified defects" reclassifies a reasoned
exemption as rot, and then drags the entire `SimpleEditor` decomposition (5,852 lines, still
unshrunk) plus an unfunded external prerequisite onto the critical path of a capstone whose
whole pitch is *cheap, standing, mechanical*.

D-04 has the same defect: `BatchSimulator.pause` is documented as intentional — *"It doesn't
make sense to pause it in batch mode. @param which Ignored."* There is a real bug hiding
there (pause **stops** rather than no-ops), but naming it requires a semantic decision about
what batch pause means, not a maintenance chore.

**Concrete alternative.** Ratchet the *exemption*, not the floor: an arm asserting that
every package with no `PACKAGE` rule carries a recorded exemption naming its trigger issue,
and that the exemption list only shrinks. That is a sub-week lint. It preserves the entire
intent of D-07 — no package goes silently unfloored — while letting the `jls.edit` floor
land where the decomposition is actually planned and funded (#84/#91/#162 behind #337,
inside CAP-01). KC-00-3 already anticipates this outcome after burning 3.5 weeks; I am
saying make it the design instead of the fallback. **Effect: the band drops to roughly
17–32 mw, the #337 external prerequisite leaves the critical path entirely, §3 risk 4
(zero-margin floors flaking across a widened matrix) evaporates, and FEAT-011 (#355) — which
is only in the set because it is FEAT-008's harness consumer — can follow the same route to
CAP-06.**

## Reframe C — diffability is a tooling property, not a storage property

FEAT-003 welds two separable things: stable-id `ref`s (D-02) and flipping the default
container from XZ to plain text (D-08). The first is genuinely load-bearing and genuinely
cheap — `Circuit.java:1497-1502` is a five-line dense-renumbering loop sitting *directly
under* a comment explaining that the list was already sorted by `getStableId()`, and
`ElementId`'s string form is documented as surviving the save format's escaping untouched.
Emitting the stable id as the ref is close to a deletion.

The second is a format epoch — and it is the *sole* reason §3 risk 1 exists. The issue
states that risk plainly: ship the fail-loud loader before the format change and "every
pre-FEAT-003 fixture in `test/` becomes a hard load failure," a hazard the 2026-08-03 REPLAN
found is now **unguarded** because the ordering edge it assumed does not exist in the filed
tier.

The stated end is "a saved circuit is a reviewable text artifact whose diff is proportional
to the edit." Review-time legibility does not require changing what students' machines
write. A `.gitattributes` entry plus a `git config diff.jls.textconv` driver over the
already-shipped `savetext` path (`JLSStart.java:787-788`) gives byte-perfect textual diffs
of compressed `.jls` files in every review surface, today, for well under a week — and
`FileAbstractor.openCircuit` already sniffs all three containers, so nothing regresses.
Under that framing D-08 becomes an independent UX decision that can land whenever it likes,
the format epoch stops colliding with the fail-loud loader, and risk 1 — "the one ordering
hazard in the set with a real chance of a red default branch" — stops existing rather than
being mitigated by an allowlist contract term between two sibling issues with no ordering
between them.

## The shape that emerges

Once B and C are applied, what is left is not one capstone. It is three programs that share
only a vintage:

1. **The persistence contract** (D-01, D-02): make the save/load pipeline total and
   fail-loud. Deep, architectural, maps onto `ARCHITECTURE.md`'s "save/load pipeline," and
   is what CAP-01's convergence oracle and CAP-02's guest-image section actually need.
2. **Simulator loop correctness** (D-03, D-04, D-05): three small, well-anchored fixes with
   one semantic decision embedded in D-04.
3. **Build and CI policy** (D-06, D-07 as an exemption ratchet, plus the registry from
   Reframe A): a new and genuinely valuable seam — the first tests in this repo that assert
   things about the *build*, not about the code.

Grouping (1) with (3) buys nothing architecturally; it buys a program-management rollup.
I would rather see (3) ship first and alone — it is the substrate everything else is
measured on, it is a few weeks, and it makes the other two verifiable — than see all three
priced as a 29–52 mw block gated on an editor refactor.

## One trajectory concern the issue cannot see from inside

The plan corpus this capstone derives from — `docs/plan/**`, `docs/machine-calibration.md`,
`docs/parity-contract.md` — **does not exist on master** (verified: `ls docs/plan` fails;
`docs/machine-calibration.md` is absent). The evidence-pin comment referencing #493 confirms
195 such files live only on a branch that will be deleted. This matters beyond bookkeeping:
FEAT-009 (#335, 5–10 mw) is re-scoped to a "residual" specifically because "the calibration
**document** ships (1,124 lines)" — on master it does not, so that row is a plan, not a
residual, and the Definition-of-Done line "every cited evidence document and permalink
resolves on the default branch at close" is already unsatisfiable.

The deeper point: this project's durable reasoning lives *in the tree* — `ARCHITECTURE.md`,
`docs/simulation-semantics.md`, `docs/file-format.md`, the recorded decisions with revisit
triggers. A 19-capstone / 57-feature tier that exists only as GitHub issue prose, maintained
by REPLAN comments reconciling mermaid graphs against machine blocks, is the opposite arc.
The single highest-leverage thing CAP-00 could do for the plan as a whole is commit the tier
to `docs/plan/` and add a ratchet asserting that every `requires_features` number resolves
to an open issue with a matching title — turning four hand-written verification comments on
this issue into one test.

## What I would keep verbatim

§1's eight-step walk-through is the best artifact here — an executable outcome statement a
cold reviewer can run. AC-3's refusal to pin N is exactly right and should be the template
for AC-1. The D-01…D-08 table with quoted lines at named anchors is a model of falsifiable
issue writing; every anchor I spot-checked (`Simulator.java:224-234`, `SigSim.java:64-74`,
`BatchSimulator.java:77/89`, `Circuit.java:1497-1502`, `pom.xml:408`, zero `timeout-minutes`
in `.github/workflows/`) held. KC-00-1's public correction of its own broken arithmetic
(1.5 × 62 = 93, not 63) is the kind of self-refutation most plans never perform.

## Verdict

**endorse-with-reframing.** The thesis — every invariant this project cares about should be
enforced by the build, not by the maintainer — is the correct direction and is already the
project's native idiom. But I am explicitly disregarding **AC-1 as written** (an 8-arm class
named for a backlog vintage that no feature owns; substitute an append-only ratchet registry
with arms colocated with their subjects), **D-07's classification as a defect** (it is a
recorded decision with a stated trigger; ratchet the exemption and let the `jls.edit` floor
follow the decomposition into CAP-01, removing #337 from the critical path and ~12–20 mw
from the band), and **FEAT-003's welding of stable refs to a container-default change**
(ship stable refs; solve review-time diffability with a git textconv driver, which dissolves
§3 risk 1 rather than mitigating it). Reframed this way the capstone is roughly half the
cost, has no unfunded external prerequisite, carries no red-default-branch ordering hazard,
and leaves behind a mechanism the other eighteen capstones can reuse instead of a suite that
retires when its eight defects close.
