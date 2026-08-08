# Issue #458: TASK-0082: JLS-T3 gets an independent in-jar emulator, assembler and disassembler — the counterparty a drawn ternary CPU can be wrong against
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue is well-engineered as a specification of *what* to build (BET
codec, `T3Cpu.step`, two-pass assembler, T-null models, `RetireRecord`
shape), and its core technical claims check out against the current tree
(no `src/jls/mach/`, no `ProcessBuilder`/`Runtime.exec` in `src/`,
`ArchitectureRulesTest`'s serialization ban, the `PinBindings` reporting
idiom, `RegisterFile`'s parametric ports all verified as cited). But the
issue is not actionable as filed: it is blocked by a task that has no
issue number and cannot be tracked as "landed," and it cites three
normative documents by path and section that do not exist anywhere in
this repository. Below, numbered by severity.

## 1. [High] A hard blocking dependency has no issue to block on

Status & Dependencies declares `blocked_by: [428]` only, but the prose
immediately says:

> "Task-to-task edges still to be added by the link pass: this task is
> **also blocked by TASK-0070** ... `jls.mach` does not exist at the
> evidence commit and T3 is a **leaf inside it, not a parallel
> package**; **TASK-0070 is not filed.**"

Definition of Done then requires: `blocked_by: 428 (TASK-0081) has
landed, and TASK-0070 has landed, or the dependency was waived per rule
10`. There is no mechanism in this repo's tracker for "TASK-0070 landed"
to be verified — it has no issue number, so nothing can be closed,
cross-linked, or checked against it. `git grep -rn RetireRecord` and
`git ls-tree src/jls/` both confirm `jls.mach` doesn't exist (verified
below), so this isn't a stale citation — it's a real, currently-unfiled
prerequisite that a picker-upper cannot even subscribe to. **Recommendation:**
do not let this task be picked up until TASK-0070 is filed and its
number is added to `blocked_by`; the current YAML understates the
task's true blocking set and an executor trusting the machine-readable
field alone will start work that has no `MemoryView` to share with.

## 2. [High] Three normative documents this issue tells the implementer to "reference, not restate" do not exist in the repository

The issue repeatedly points at `docs/plan/evidence/BRIEF.md` §0,
`docs/virtual-hardware-parity.md` L5 (`:859-890`), and
`docs/parity-contract.md` §2.1/§3.1 as authoritative and says "Reference
them; do not restate them." I checked the actual tree:

```
$ ls docs/plan
ls: cannot access 'docs/plan': No such file or directory
$ find . -iname 'parity-contract*' -o -iname 'BRIEF.md' -o -iname 'virtual-hardware-parity*'
(no output)
```

`docs/plan/` does not exist at all — not just `evidence/BRIEF.md`, the
entire tree of feature/task/capstone planning docs the issue's own
`blocked_by`/`related` graph is built from is absent. `docs/parity-contract.md`
is cited extensively by the *sibling* issue #347 (FEAT-034) too, with
quoted line numbers (`:3-7`, `:79`, `:514`, `:739`) — none of which can
be checked because the file isn't there. This means the memory-view
sharing contract, the `RetireRecord` field semantics this task
instantiates, and the D8/D10 decision rationale are all unverifiable
from the checkout an implementer would actually have. **Recommendation:**
either the doc set needs to land first (and be added to `blocked_by`
rather than assumed present), or the issue needs to stop treating them
as "already landed, just not restated" and instead spell out the
sections it depends on inline.

## 3. [Medium] The evidence commit itself is unreachable in this checkout

`evidence_commit: 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` and the
"stale line numbers" threat notes cite HEAD `839fb3a`. Neither resolves:

```
$ git cat-file -t 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7
fatal: git cat-file: could not get object info
$ git cat-file -t 839fb3a
fatal: Not a valid object name 839fb3a
```

Every `git grep`/`git show` command block in §2 Observations is
therefore not literally re-runnable by a reviewer against this repo's
history as given — the "Re-verify every observation at the executor's
checkout" instruction in §8 step 1 can't be satisfied by replaying the
issue's own commands verbatim. In practice I re-ran the *substance* of
each check against current HEAD and every one still holds (no `src/jls/mach/`,
zero `ProcessBuilder`/`Runtime.exec` hits, `riscv/` still present and
undeleted with `riscv_ref.py` intact), so this is not a fabrication —
but the pinning mechanism the issue leans on for precision is broken
for whoever actually executes this. **Recommendation:** treat evidence-commit
hashes as advisory only; the checklist item "if O1 no longer shows an
absent package, stop and comment" is the right fallback and should be
promoted from a checklist bullet to the primary instruction.

## 4. [Medium] H3/P7's "report text" acceptance criterion is under-specified and therefore gameable

P7 requires each T-null model be "rejected with the report text the
spec fixes," and the Definition of Done says `everyNullModelIsRejectedWithTheSpecifiedReportText()`
asserts the text "not the boolean." But the fixed report wording is
explicitly **owned by #428** ("§7.11 ... specify the expected report
wording here so the two do not drift"), and #428 is open with no
`docs/jls-t3-isa.md` written yet. Until that lands, TASK-0082's
implementer has no fixed string to assert against and could write the
report text *and* the test together, which is precisely the
self-consistency failure H3 exists to prevent — a test author who
invents both the wrong-implementation's rejection and the string that
proves it was rejected "for the specified reason" hasn't actually
demonstrated independence from a spec, they've demonstrated
self-agreement. **Recommendation:** make `everyNullModelIsRejectedWithTheSpecifiedReportText()`
fail loudly (not silently pass) if `docs/jls-t3-isa.md`'s wording table
is absent, rather than let the implementer backfill both sides.

## 5. [Medium] H4/P14's paired-negation test is asserted against itself, not against an independent oracle

P14: "`neg()` exists as a plane swap and agrees with `pack(-unpack(w))`
on every tested input." But per Stage 3, the plane swap *is* the
definition and the arithmetic form is only "the check" — meaning if
`Bet`'s trit-to-code assignment (`code(+1)=01, code(-1)=11`, etc.) has a
transposition bug, **both** implementations inherit it identically
(they're two encodings of the same wrong table, not two independent
derivations), and P14 would pass while H4 remains silently false. The
issue's own Falsification Criteria for H4 half-acknowledges this
("determine which by hand on a three-trit case") but that manual check
is not in any Prediction or Definition-of-Done checkbox — it's advisory
prose, not a gate. **Recommendation:** add a corpus-driven assertion
(exhaustive over the 9 one-trit values, cross-checked against the
document's fixed `code()` table, not derived from `Bet` itself) as a
DoD item, not just a threats-to-validity footnote.

## 6. [Low] Coverage-fraction framing in P3/Stage 6 invites a "green sampled run" to be over-read despite the issue's own warning

Stage 6 computes the sampled tier's coverage as ≈5.4×10⁻¹⁰ of the
two-operand 16-trit space and the issue explicitly warns "nobody
mistakes 'the sampled sweep passed' for 'the op is verified.'" Good
that it's named, but the actual acceptance criterion (P3, DoD) is only
"1e6 seeded vectors match, seed printed" — there is no requirement to
*display* the coverage fraction anywhere a reviewer sees it besides the
test's javadoc (Open Question 3, "rides along"). A green CI badge and a
merged PR carry no visible reminder that 5×10⁻¹⁰ coverage was all that
ran. **Recommendation:** promote "coverage fraction stated in the PR
description" from a data-collection note to a DoD checkbox (it's
adjacent to but not identical to the existing javadoc requirement).

## 7. [Low] Cost/scope: this is scoped as a single task but requires building an ISA-conformant CPU, assembler, disassembler, and file format simultaneously

FEAT-039's own cost accounting prices TASK-0082 at "2 wk" as a "leading
slice," explicitly warning "do not read 10 wk as the feature... the
residual is the depth." Given the DoD here spans an emulator covering
~28 instructions across seven families, a two-pass assembler with the
`PinBindings` idiom, an inverse disassembler, a byte-grammar `.timg`
format with typed rejection at every header field, 9+81 exhaustive
cases, 10^6 sampled cases, 16 illegal-lane corners, four T-null models
each independently correct, a new ArchUnit rule, and package-info/NullMarked
compliance — the "2 wk" figure this issue inherits is not something
#458 itself claims, but nothing in #458 flags that the estimate it's
implicitly working against is known-thin. **Recommendation:** not a
defect of this issue specifically, but worth an explicit note here
given it's the concrete task an executor will size against.

## What's solid

- **O1/O2/O4/O5 are independently verified true against current HEAD**:
  no `src/jls/mach/`, zero `ProcessBuilder`/`Runtime.exec` hits in `src/`,
  `ArchitectureRulesTest.java:201-212`'s serialization ban exists
  verbatim as quoted, and `PinBindings.java`'s multi-error reporting
  idiom with `UnbindablePortsTest` exists exactly as cited.
- **The BET codec math (Stage 1-2) is internally consistent** — the
  illegal-lane mask, the symmetric ±1093 immediate bound, and the
  waste-fraction arithmetic all check out by hand.
- **H1's independence guard (P13, a new ArchUnit rule) is feasible**:
  `test/jls/ArchitectureRulesTest.java` already has ~10 `noClasses()...resideInAPackage()`
  rules of exactly this shape, so the mechanism proposed is proven
  infrastructure, not speculative.
- **The scope boundary is unusually disciplined** — explicit "not
  modified" list (no `.jls` format change, no CLI change, no simulator
  change) and a clear DoD separating this task's artifacts from #428's,
  #390's, and #423's.

## Bottom line

The technical design is sound and the self-citations that *can* be
checked against this repository check out faithfully. The issue fails
on process grounds: it names a hard blocker with no issue number to
attach to, and it treats three specification documents as already-landed
prerequisites when they do not exist anywhere in the tree. An executor
who follows §8's re-verification step honestly will stop at both of
these before writing a line of `jls.mach.t3` code.
