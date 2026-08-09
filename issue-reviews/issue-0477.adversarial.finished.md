# Issue #477: TASK-0070: an independent RV32 machine exists as a pure leaf package with a headless runner — testable with no simulator, no circuit, and no wall clock
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for a new `src/jls/mach/` package: an immutable `ArchState`,
two differently-shaped `MemoryView` implementations, a pure `step`,
simulated-time-only UART/CLINT models, a headless `Runner`, and matching
JaCoCo/PIT governance, all landed in one commit. The repo-facing
observations (O1–O8) check out: `src/jls/mach` does not exist, `pom.xml`
has no `jls.mach` rule, `HeadlessCoreRatchetTest`'s `CORE_PACKAGE_PREFIXES`
and empty `BASELINE` match verbatim, and the `jls.sim`/`jls.elem` JaCoCo
floors (0.930/0.920/0.845 and 0.730/0.700/0.585) are exactly what
`pom.xml` lines ~449–490 contain today. But the issue's entire
justification apparatus — the documents it forbids restating, its
dependency direction on a sibling issue, and its own related-work table —
does not hold up.

## Findings, most severe first

**1. The issue's normative citations point at documents that do not exist anywhere in the checked-out tree, and were never more than "proposed" even when they did exist.**

The issue says, twice, "Do not restate those documents; this issue
implements against them," citing `docs/parity-contract.md` §9.3/§5.1,
`docs/machine-calibration.md` §5.3/§5.4, and `docs/virtual-hardware-parity.md`
P16/L5 as the source of the SoC minimum spec, the `mtime`-must-be-simulated
measurement (`1.5 × 10^10` instructions of idle spin), and sync-point-zero.
None of these files exist at HEAD:

```
$ ls docs | grep -iE "parity|machine-calibration|virtual-hardware"
(no output)
```

Tracing history: `docs/parity-contract.md` was added at `64c137d`
(2026-07-29) already carrying the header `**Status: proposed normative
contract — written, not yet ratified.** ... until then it binds nothing
and no other document may cite it as settled` (per commit `b299d63`'s own
message, "demote the contract to unratified"). It was then deleted
entirely by `742da74`, "docs: remove the planning corpus now that it is
encoded in issues." Sibling issue #347 (FEAT-034, the feature this task
is `blocks`) records the same unratified status as an *open question that
blocks filing this very task*: "Blocks filing TASK-0072 and TASK-0073,
whose acceptance criteria cite contract sections." #477 cites the same
contract as settled background with no caveat. An implementer picking
this up today cannot verify the SoC minimum, the `mtime` measurement
methodology, or sync-point-zero's justification — the source prose is
gone, was already caveated as non-normative before it vanished, and the
issue's own "do not restate" instruction makes that unrecoverable short
of digging through git history the issue never points to.

**Recommendation:** either inline the load-bearing facts (SoC minimum,
the `mtime` measurement, the sync-point argument) directly into this
issue's body, or re-file it after the parity contract is actually
ratified per #347's Open Question 1 — building "against" a deleted,
never-ratified spec is building against nothing.

**2. Self-contradiction: the issue's own Related Work table says no issue exists for the thing this issue is.**

§12's table has this row: *"No issue exists for `jls.mach`, the reference
runner or the guest software stack. Recorded as a gap, not a blocker (D10,
`docs/plan/evidence/BRIEF.md` section 12 ... not present at `2d0ca9d`)."*
But #477 **is** the issue for `jls.mach` and the `Runner` — that is its
title and its entire deliverable. This is leftover text from an earlier
gap-analysis draft that was never updated once the gap was filed against.
It is a small thing individually, but it is symptomatic of the larger
problem in Finding 1: large stretches of this issue are transcribed from
documents/analyses that were current at some earlier evidence commit and
have not been reconciled against the issue's own existence.

**Recommendation:** delete or correct that row before filing/re-filing.

**3. The `jls.parity.RetireRecord` dependency direction is asserted as settled but is actually an unresolved race with a sibling issue, and `blocked_by` is empty.**

§6 states flatly: *"One leaf dependency only: `jls.parity.RetireRecord`
(#390). `jls.mach` depends on `jls.parity` for that record and **never
the other way**."* §7.12 repeats it as a "documented contract, not an
accident." Yet `src/jls/parity` does not exist in the tree today (checked:
`find src -iname "*parity*"` returns nothing), #390 (TASK-0072, which owns
`RetireRecord`) is open and unimplemented, and #477 lists #390 only under
`related`, not `blocked_by` — `blocked_by: []`. #390's own dependency
section calls the relationship a "co-producer... whichever lands first
defines the type, and the second consumes it" — i.e. symmetric, not the
one-directional ownership #477 asserts. If #477 lands first (which
`blocked_by: []` explicitly permits), `StepResult` cannot compile against
`jls.parity.RetireRecord` unless TASK-0070 itself creates that type in
`jls.parity` — which contradicts both "never the other way" (§7.12) and
#390's stated ownership of the record. The issue never states what an
implementer should do if #390 has not landed when this is picked up,
despite `Predictions` P8/P9 and the DoD requiring `mvn verify` and the
JaCoCo/PIT gates to pass "on the same commit."

**Recommendation:** either add 390 to `blocked_by`, or state explicitly
(here, not in the sibling issue) what `jls.mach` compiles against if
`jls.parity` doesn't exist yet — e.g. a local stub `RetireRecord`-shaped
type that gets deleted when #390 lands. As written this is a real
compile-order hazard, not a documentation nit.

**4. Cost/scope mismatch against the feature's own budget.**

#347 (FEAT-034), which this task `blocks` the close-out of, reconciles its
cost as: *"Named task rows, all at 2 wk each: TASK-0069, TASK-0070,
TASK-0072, TASK-0073, TASK-0080, TASK-0082, TASK-0111, TASK-0112. Printed
sum: 16.0 maintainer-weeks."* So TASK-0070 (this issue) is explicitly
budgeted at **2 maintainer-weeks**. The Definition of Done here asks for:
~3,000 lines of ISA logic (per the Abstract) covering an RV32IMA decode
table, two genuinely-differently-shaped `MemoryView` backends compared
element-for-element over whole execution traces, a UART model, a CLINT
model, a headless `Runner`, nine required test predicates (P3–P11)
including two from-scratch source-scan ratchets, a JaCoCo floor of
0.930/0.920/0.845, and PIT mutation/test-strength floors of 80/82 — all
gated green in one commit, alongside three separate governance edits.
Nothing in #477 argues that this fits in 2 weeks, or reconciles against
#347's number. Given the coverage/PIT bar is deliberately the *strictest*
in the repository (O4's whole point), 2 weeks for ~3k lines of new,
93%-covered, 80%-mutation-killed ISA logic plus two independent memory
backends reads as materially underestimated.

**Recommendation:** either revise #347's cost table with a citation back
to this issue's actual scope, or explicitly split TASK-0070 into a
smaller landing (e.g. `ArchState` + `step` + one `MemoryView`) with UART/
CLINT/second-view work as follow-ups — the issue currently promises the
whole machine "from birth" under the strong bar in a single commit.

**5. Several Definition-of-Done items are unenforceable narrative, not test-checked facts, despite reading as hard gates.**

P10 requires "`ArchState`'s constructor states the power-on value of every
architecturally visible register, and a test asserts it — reproduced as a
table in the PR." The test can only assert that *some* fixed values exist
and match what the code says; it cannot verify the PR's prose table
matches the code, or that "every architecturally visible register" is
actually complete (an omitted CSR fails silently — there is no register
enumeration the test checks against an independent list). Similarly, "the
seed printed in the failure report" pattern from sibling issue #392 is
absent here entirely for anything sampled; P4's "genuinely differently
shaped, not a wrapper" requirement (T1) has no automated check beyond
"the author says so" — a reviewer must eyeball the second `MemoryView`
implementation to catch a thin wrapper, which is exactly the failure mode
Threat T1 warns about but does not gate.

**Recommendation:** add a structural test that enumerates the CSR/register
set from one authoritative source (e.g. reflection over `ArchState`'s
record components) and asserts the power-on-value test covers all of
them, so P10 can't silently miss a register; consider a crude anti-wrapper
check for the second `MemoryView` (e.g. asserting its internal field
shapes differ, or its per-access cost model differs) rather than relying
on reviewer vigilance for T1.

**6. `docs/machine-calibration.md`'s headline number (`1.5 × 10^10` instructions of idle spin) cannot be audited.**

H4 and the failure-mode table both hinge on this figure as the reason
`mtime` must be simulated-time-only. Given Finding 1 (the document is
deleted), this number cannot be checked for methodology (was it measured
on this simulator? what "8s to type root" scenario, what clock rate?) —
it has to be taken on faith from the issue text alone. That's a fine
default in isolation, but combined with "governance lands in the same
commit" and a zero-tolerance ratchet built on it, an implementer has no
way to independently sanity-check the requirement before locking it in.

**Recommendation:** restate the underlying arithmetic (host clock rate ×
typing delay ÷ simulated instruction rate) inline so the number is
re-derivable without the missing document.

## What holds up

- O1/O2 (no `src/jls/mach`, no `jls.mach` pom rule) — verified true
  against HEAD.
- O3 (no `module-info.java` anywhere in `src/`) — verified true.
- O4 (`jls.sim` floor 0.930/0.920/0.845 vs. `jls.elem` 0.730/0.700/0.585) —
  verified byte-for-byte against `pom.xml`.
- O6 (`HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` and empty
  `BASELINE`) — verified verbatim against `test/jls/HeadlessCoreRatchetTest.java`.
- O7 (repo-wide `ObjectInputStream`/`ObjectOutputStream` ban, issue #170) —
  verified present in `test/jls/ArchitectureRulesTest.java`.
- JDK target (`maven.compiler.release` = 25) matches the issue's "JDK 25
  only" interface constraint.
- The "consumed, not depended on" treatment of `riscv/riscv_ref.py` is
  consistent with #413's D5 ruling and with the fact that `riscv/` is
  Python, not a JLS mechanism.

## Verdict

**needs-rework.** The concrete repo observations are solid, but the
issue's justificatory backbone cites documents that no longer exist (and
were explicitly non-normative before they were deleted), contains a
self-contradicting related-work row, leaves an unresolved compile-order
race with #390 despite an empty `blocked_by`, and is budgeted by its own
parent feature at roughly a quarter of what its Definition of Done
actually demands. None of these are fatal to the underlying goal (an
independent RV32 model is clearly the right idea, per #347's IC-1 null
test), but the issue as written cannot be picked up and executed against
without the implementer first resolving problems the issue asserts are
already resolved.
