# Issue #351: FEAT-046: JLS solves a continuous-time circuit in pure Java, produces the same bits on every platform, and proves the answer against closed forms and a real external simulator
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the issue

A tracking/planning issue (no code) proposing a new `jls.analog` package: an
MNA/Newton-Raphson transient solver, cross-platform byte-identity guarantees,
controlled sources/model cards, and a nightly differential comparison against
a real external SPICE-class simulator. Costed at 17.5–26 maintainer-weeks.
State: open, one comment (a duplicate-boundary note vs #331), both correctly
loaded via `issue_read`.

## Findings, most severe first

**1. (Critical — licensing/architecture contradiction) The absorb-in-process
plan directly reverses the project's own ratified precedent for exactly this
class of risk, and asserts licensability without an audit.**

The Cost section quotes the integration decision verbatim: *"PORT the
numerics to Java as `jls.analog`. Absorb ngspice, XSPICE, Sparse1.3,
SpiceSharp and CircuitJS1 source under D8 ... it is licensable: JLS is
GPL-3.0-or-later and can absorb the relevant open-source solvers with their
notices."* No license-by-license audit is shown. This is a live problem, not
a formality: **CircuitJS1** (Falstad's simulator, the most directly relevant
of the five) ships under **GPL-2.0-only**, not "or later" — code under
GPL-2.0-only cannot simply be folded into a GPL-3.0-or-later work without the
copyright holder's consent, because the upgrade grant that makes GPLv2/GPLv3
combination possible has to come from the GPLv2 side, not the downstream
project. **Sparse1.3** and parts of **ngspice** carry Berkeley/academic
notices with their own attribution/restriction terms, not a clean BSD grant.
This is precisely the class of provenance question this repository is
otherwise unusually careful about — see `README.md`'s own
`pop_GPLv3.pdf` consent letter for JLS's *original* code. An "absorb the
solvers with their notices" plan for five differently-licensed third-party
codebases deserves the same rigor, and the issue supplies none.

Worse, this reverses two of the project's own recorded decisions, both
findable in the checked-out tree:

- `ARCHITECTURE.md` §"Plugin trust boundary" (#222) states the reason
  out-of-process isolation exists at all: *"External tool integrations (#61
  Yosys, #63 GHDL/Icarus, #62 ELK) already sit on that subprocess boundary
  and stay there, which also sidesteps GPLv3 in-process-linking hazards
  (e.g. ELK's EPL-2.0)."* — i.e. the maintainer already chose subprocess
  isolation specifically to dodge exactly this kind of license-linking risk.
- `docs/grand-architecture.md` §9 states as a firm, "re-litigates a settled
  decision" boundary: *"No in-house HDL simulation or parsing beyond a
  header scanner (#59, #63); external synthesizer + JSON netlist import,
  subprocess co-sim."*

Issue #351's own § 1 rejects the subprocess option for the *opposite* reason
("external float solvers are not reproducible across platforms") — a
legitimate engineering argument, but the issue never acknowledges that it is
overriding two settled precedents, and never shows the license work needed
to make "absorb the source" actually safe. This should be argued explicitly
against #222 and grand-architecture §9, with a real per-file license audit,
before any TASK gets filed.

**2. (High — evidentiary integrity) The cost basis rests on a document that
does not exist in the current tree, and the cited evidence commit is not
reachable from this checkout.**

The Cost section says the stage-band derivation lives in
`docs/plan/evidence/analog-determination.md`, "landed in
`3a81a4a7d6a0f108ec201e632732d308cc02b3fc`." Checked: **no `docs/plan`
directory exists anywhere in the current `docs/` tree** (`find . -type d
-name plan` returns nothing; `docs/` contains only flat `.md` files and named
subdirectories like `capability-roadmap/`, `standards-adoption/`). Since the
claim is that this file "landed" (i.e., should be present on the default
branch, not merely at some ancestor commit), its absence at HEAD is a direct
verification failure, not a shallow-clone artifact. Separately,
`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` — cited for every
"ABSENT at 2d0ca9d" claim in the machine block and § Background — is not
reachable in this local clone (`git cat-file -e` fails); note the clone here
is shallow, so treat non-reachability alone as inconclusive, but combined
with finding (1) above the pattern is that the issue cites evidence artifacts
this reviewer cannot locate. The issue's own Definition of Done requires
"every cited evidence document and permalink resolves on the default branch
at close" — by that bar, the issue does not currently meet its own standard.
(The other Background claims — no `analog` package, zero `StrictMath` hits,
`JLS_REPLICA_ID` unpinned in `.github/`, the `HashSet`/
`getElementsInStableOrder` split in `Circuit.java`, and the `ToolLocator`
self-skip idiom in `test/jls/hdl/GhdlCompileTest.java:34-36` — all check out
against current HEAD, so this is not a blanket fabrication, just an
unverifiable cost/rationale document.)

**3. (High — gameable acceptance criterion) IC-1, billed as "the feature's
central claim," cannot actually fail the issue.**

The title promises "produces the same bits on every platform," and § 1 calls
byte identity "a required gate, not a nightly report." But § 7's Re-planning
Protocol pre-authorizes the opposite outcome: *"Byte identity fails. This is
the anticipated `REFUTED:` and it has a prepared response: the tolerance tier
survives, the byte-identity claim is withdrawn with its measured failure
recorded ... The feature does not close as failed — it closes with a
narrower claim."* The Definition of Done matches: *"The byte-identity matrix
has run green ... — or the claim has been explicitly narrowed with its
measured failure recorded."* A criterion whose failure mode is pre-negotiated
as an acceptable closing state is not a gate; it is a report with an escape
hatch already built in. This isn't wrong engineering practice on its own
(having a fallback plan for a real risk is sound), but it means the issue's
headline claim — the thing the title and abstract sell — is not actually
what closing the issue guarantees. Recommend either (a) stating the title's
claim conditionally ("...aims to produce the same bits...") so it matches
what DoD actually requires, or (b) if byte-identity really is meant to gate,
removing the narrowing exit from DoD and making refutation block close until
resolved by a follow-up issue.

**4. (Medium — weak anti-cheat) Criterion 2's two "anti-cheat" assertions
are nearly vacuous in double-precision arithmetic.**

`0 < ‖x(t) − x*(t)‖ ≤ τ` (error nonzero) and `‖x_JLS − x_oracle‖ ≥ β > 0`
(oracle disagreement nonzero) are both framed as guards against a degenerate
test. In practice, two independent floating-point computations of an
irrational closed-form value essentially never land on the exact same double
by accident — so a solver with a real correctness bug will satisfy "error
nonzero" exactly as easily as a correct one; the check mainly catches the one
specific historical failure mode of a hardcoded exact-equality test, not
general incorrectness. The oracle-disagreement floor `β` is not specified
anywhere in the issue (no value, no derivation rule, unlike τ which is
explicitly "derived ... with the derivation in a comment") — as written any
positive β, however microscopic, satisfies the letter of criterion 2 while
proving nothing about correctness. Recommend requiring β's derivation be
documented with the same rigor as τ's, and pairing the nonzero check with a
genuinely discriminating one (e.g. mutation testing on the stamp
coefficients themselves — the project already has infrastructure for this
per `docs/mutation-testing-trial-2026-07.md`).

**5. (Medium — feasibility/cost risk, CI matrix) The 3-OS × 2-arch × 2-JDK
matrix is heavier than anything this project's CI currently runs as a
required gate, and the issue does not name a runner strategy for the hard
cells.**

`.github/workflows/ci.yml` today runs Linux (amd64, JDK matrix),
Windows (`windows-latest`, presumably x64), macOS (`macos-latest`, Apple
Silicon by default on GitHub-hosted runners today) as required jobs, plus a
separate aarch64-Linux leg for installer reproducibility. Nothing in the
repo's existing CI exercises Windows-on-ARM or macOS-x64, and GitHub-hosted
Windows-arm64 runners are not a mature, generally-available option today.
IC-1 asks for "three operating systems, two architectures and two JDK
versions" as a *required, blocking* gate without saying which runners cover
the six-to-twelve legs implied, or what happens if a hosted runner for one
cell simply isn't available. For a project self-described in
`ARCHITECTURE.md` as "single-maintainer," committing up to half a
maintainer-year partly to standing up CI infrastructure that may not exist
as a hosted product yet is a real, unbudgeted risk that the Cost section
(itself unverifiable — see finding 2) doesn't appear to price separately.

**6. (Medium — scope/estimation risk, admitted by the issue itself) Open
Question 1 concedes roughly two-thirds of the estimated cost is unpriced,
and recommends proceeding anyway.**

"Band 17.5–26 mw" vs. "the four named rows sum to 8.0 wk" — a 3.25× gap the
issue calls "the largest gap of the features in this pass," attributed to
"the escape ladder's convergence work, the absorbed-code audit pass, the
full determinism matrix ... and the corpus's breadth — has no task id."
Recommended default is "(a) record the gap and schedule against the band" —
i.e., start work while acknowledging most of the cost has no owner, no task,
and no line item. Given finding 5 (CI matrix is itself expensive and
underspecified) and finding 1 (the license audit for five absorbed
codebases doesn't exist yet), this residual almost certainly includes real,
sizeable, currently-invisible work. Recommend pricing the license audit and
the full CI matrix stand-up explicitly before this issue is used to fund
child tasks.

**7. (Medium — underspecified fallback) The "grid flip" third outcome (IC-5)
has no bound on how often it may fire before it should count as a failure.**

Criterion 5 states a changed accepted-timestep sequence is reported as
"neither a pass nor a fail," falling back to "resampling on the fixture's
declared step" — but names no interpolation method, no accuracy bound for
resampling-introduced disagreement, and no cap on repeated grid-flip
declarations. As written, a solver whose timestep selection has become
nondeterministic or has regressed could report "grid flip" indefinitely on
every run and never be forced through a real pass/fail comparison, which is
exactly the kind of perpetual non-answer the criterion says it wants to
avoid for the pass/fail case ("Collapsing a grid flip into 'fail' trains
people to ignore failures; collapsing it into 'pass' hides real changes" —
true, but an unlimited third option hides changes just as effectively).
Recommend specifying the resampling method and escalating to a failure if
grid-flip fires on a fixture N runs in a row.

**8. (Low — cross-issue accounting, unverified) TASK-0100 is declared
"shared" and "counted once at the task level" with #331, but the
issue text doesn't show the subtraction.** Both issues apparently list
TASK-0100 in their own roster table at full weight; the "counted once"
claim is asserted, not demonstrated with arithmetic in either text (not
independently re-verified against #331's current body here, since #331 is
outside this assignment's scope, but the comment thread confirms the
mirrored `shares` obligation exists — worth a check by whoever reconciles
both features' totals before funding).

## What's solid

- The MNA/Newton-Raphson/LTE math (§3) is standard and correctly stated for
  a transient SPICE-class solver; no complaint there.
- The reused self-skip CI idiom is real and accurately quoted: `test/jls/hdl/GhdlCompileTest.java:34-36` matches verbatim.
- The Background section's factual claims about current HEAD (no `analog`
  package, zero `StrictMath` call sites, `Circuit.java`'s `HashSet` field
  and `getElementsInStableOrder()`, `ElementId.java`'s `JLS_REPLICA_ID`
  env var not being pinned in `.github/`) all check out against the actual
  checked-out source.
- The scope carve-out against #331 (comment thread) is clear and mutually
  consistent in both directions, and correctly identifies the ordering edge
  rather than collapsing it into a merge.
- Explicitly deferring sparse factorization with a numeric re-entry trigger
  (Open Question 2, backed by measured µs-at-N figures) is a reasonable,
  evidence-based scope cut rather than a hand-wave.

## Recommendation

Do not file the four planned tasks yet. First: (a) produce a real
per-codebase license audit for ngspice/XSPICE/Sparse1.3/SpiceSharp/CircuitJS1
against GPL-3.0-or-later, reconciled explicitly against the #222 and
grand-architecture §9 precedents this issue overrides; (b) restore or
re-derive the missing `docs/plan/evidence/analog-determination.md` so the
cost band is checkable; (c) decide whether IC-1 is truly a blocking gate or
a best-effort target, and make the title and DoD agree; (d) price the CI
matrix and the license-audit residual explicitly rather than leaving them
inside an unowned 3.25× gap.
