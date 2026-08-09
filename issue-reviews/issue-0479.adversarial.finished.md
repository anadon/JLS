# Issue #479: TASK-0084: a hand-written monitor runs on the drawn ternary machine and prints — with the same transcript byte for byte on the behavioral and the structural binding
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: should-not-proceed

## Summary

This issue asks for a hand-written OS-like monitor (QDOS), an ECALL ABI,
a polled console driver, and a byte-for-byte transcript golden compared
across two implementations of a from-scratch balanced-ternary CPU — all
gated on three other unimplemented tasks that are themselves gated on
tasks that do not yet have issue numbers. The prose is exhaustively
detailed (14 sections, falsification criteria, a Definition of Done with
~20 checkboxes), but the detail sits on top of citations to normative
documents that do not exist anywhere in this repository, and the
dependency graph it requires is, at the time of filing, almost entirely
unbuilt. Filing it "in full" now, rather than as a stub blocked on its
prerequisites, invites review cycles on a plan that cannot be executed
as written.

## Findings, most severe first

### 1. The issue's own normative foundation does not exist in the repository (critical)

The "Background & Prior Work" section says: *"`docs/parity-contract.md`
section 2.4 makes the input log indexed by retirement... `docs/virtual-hardware-parity.md`
L3 records the host boundary as one door granted at invocation...
**Do not restate those documents**; this issue builds against them."*

Neither file exists:

```
$ ls docs | grep -iE 'parity|virtual-hardware'
batch-interface.md
$ find /home/user/JLS -iname "*parity*" -o -iname "*virtual-hardware*"
(only unrelated geometry-parity test files and an unrelated remote branch name)
```

`docs/machine-calibration.md` (cited repeatedly by #479's own blockers,
#454/#458/#459, for the register-file cost model and the events-per-second
constants) and `docs/plan/evidence/BRIEF.md` (cited as the source of "D5",
"D7", "D8", "D10" rulings that several of these issues treat as binding)
are likewise absent from the tree. So is the numbered "rule" apparatus
the Definition of Done leans on throughout ("rule 2", "rule 6", "rule 8",
"rule 10") — `grep` for "rule 3" / "rule 6" / "rule 10" across every
`.md` file in the repo returns nothing. An implementer who opens this
repo today has no way to verify H1's contract ("input log indexed by
retirement, never by time"), no way to check what "rule 10" permits a
waiver to look like, and no way to confirm the ECALL-numbering discipline
against a document that governs it. Recommendation: either land the
cited docs first (as their own reviewable PRs) or strip the issue of
claims that depend on them until they exist. An issue that says "do not
restate this document" about a document nobody can open is not
reviewable.

### 2. The dependency chain required before this task can even start is mostly unfiled, not just open (critical)

`blocked_by` lists #454, #458, #459 — all three currently open. But
walking one level further:

- #459 (drawn ternary CPU) is itself `blocked_by: [412]` (open) **and**
  states two more "hard prerequisites whose issue numbers do not exist
  yet": TASK-0061 (N-ary element family) and TASK-0082, which turns out
  to be #458 itself, filed concurrently.
- #458 (emulator/assembler) is `blocked_by: [428]` (checked: open) and
  says it is "**also blocked by TASK-0070**" (the `jls.mach` package,
  its `MemoryView`, its coverage floor) — "TASK-0070 is not filed."
- #454 (console element) is `blocked_by: [424]` (open).

So the real prerequisite set for #479 includes at minimum two tasks
(TASK-0070, TASK-0061) with no issue number at all as of filing, plus a
five-deep chain of open issues on top of that (#412, #424, #428, plus
whatever TASK-0070/TASK-0061 will require). Section 8's first checklist
item — "Confirm #454, #458 and #459 have landed" — cannot presently be
satisfied even in principle, because #459 cannot land until issues that
do not exist yet are filed, estimated, and landed. Recommendation: this
issue should not have been filed as an executable task; it should be a
placeholder blocked on FEAT-039 with a note that filing detail is
premature until TASK-0070/TASK-0061 exist.

### 3. Scope is far beyond "task" tier (high)

The deliverable is: a full ECALL ABI (~10 syscalls with fixed numbers
and error conventions), a trap dispatcher, a polled console driver with
a *measured* bounded busy-wait guarantee, a balanced-ternary printing
routine, a memory-resident loader, six interactive built-ins, and a
byte-identical transcript golden verified on two independently-built
CPU implementations — while simultaneously standing up the toolchain
(assembler/emulator) it's developed against, per section 6's own
admission ("Develop against the emulator... iterating structurally is a
week of waiting"). The issue is labeled `tier: task` — the same tier
label used elsewhere in this tracker for single-element additions (e.g.
#454's Console element). Comparing the two Definition-of-Done lists,
#479's is materially larger and depends on getting an entire novel ISA,
toolchain and CPU right first. Recommendation: either split into a
suite of smaller tasks (ECALL table + trap dispatch; console driver;
TRIT printing; loader; golden capture) or relabel as its own feature.

### 4. The structural-binding acceptance test's cost/automation status is unspecified and inconsistent with its own sibling task (medium-high)

Section 6 states the structural tier costs "roughly 0.18 s per
character" — the same figure #425 (TASK-0080) uses to justify treating
its own structural boot comparison as a **manual, non-CI "expedition"**,
explicitly kept out of the required gate ("Recorded, not automated...
It is an expedition, not a CI job"). #479, by contrast, lists
`theSameTranscriptIsProducedOnTheStructuralBinding()` as a plain test
name in Predictions (P4) and as a green checkbox in the Definition of
Done, alongside `mvn verify` green, with no language marking it as an
expedition, nightly-only, or excluded from the required gate. Given the
QDOS session (`HELP`, `MEM`, two `TRIT` calls, a `PEEK`/`POKE` round
trip, a three-instruction `GO`) is on the order of dozens of characters,
this specific run is smaller than #425's boot transcript and might be
tolerable in CI — but the issue never says so, never states a lane, and
never reconciles the discrepancy with #425's precedent that structural
runs at this per-character cost don't belong in the blocking suite.
As written, an implementer could satisfy P4 by running it once by hand
and recording a "green" result, then never re-verifying it — which is
exactly the gap #425 was careful to close for its own golden with an
explicit "expedition, not a CI job" framing. Recommendation: state
explicitly whether P4 runs in `mvn verify`, in a nightly lane, or as a
one-time recorded expedition, and say why given the character count.

### 5. A load-bearing falsification mechanism (#423) is cited but never made a dependency (medium)

Falsification Criteria for H1 says: *"the divergence is located by
retirement index — report the first differing index and the field,
which is exactly what the parity comparator (**#423**) exists to do."*
#423 (TraceDiffer/ExclusionSet/Verdict) is listed only under `related`,
not `blocked_by`, and is itself open and gated on #390 (also unverified
here). If #423 has not landed, H1's stated refutation-diagnosis path —
"do not regenerate one golden to match the other, locate the divergence
by index" — has no tool to execute it with; the only recourse would be
manual byte-diffing of the two transcripts, which the issue never
authorizes as a fallback. Recommendation: either add #423 to
`blocked_by` or state the fallback diagnostic procedure when it hasn't
landed.

### 6. "Nothing has shipped" supersession check is honest but underlines the premature filing (low, self-aware)

Section 1's "Supersession check (rule 6)" candidly states: "Nothing has
shipped: there is no `machines/` directory at all... no console element
and no block device among the 35 registered types... and no ternary
toolchain. Filed in full." Verified independently against HEAD
(`53116252`): `machines/` does not exist, and the registry does list
exactly 35 types with no console/block-device among them — the issue's
own O1/O2 observations reproduce. This part of the issue is accurate
and well-evidenced; the problem is scope and sequencing (Findings 1-3),
not honesty about current state.

### 7. Solid parts worth acknowledging

- The balanced-ternary worked example ($-5 \mapsto {-}{+}{+}$) is
  arithmetically correct and the non-negative-remainder trap it calls
  out is a real, easy-to-miss implementation hazard in any language
  whose `%` returns negative for negative operands.
- H2/P9 (clock-period independence as a falsification guard, "run
  early... if it fails, everything downstream is meaningless") is a
  legitimately good engineering discipline, directly modeled on #425's
  precedent.
- The scope fence in H4/H5/T6 (six storage-free built-ins; explicitly
  deferring `DIR`/`TYPE`/`COPY`/`DEL`/`REN`/`RUN` and a `BlockDevice`
  element, "priced at 3-5 weeks") is a genuinely disciplined boundary —
  it names what's out of scope and why, rather than silently expanding.
- The `ECALL`-table-as-data-plus-parsed-test-constant pattern (P6) is a
  reasonable anti-drift mechanism, mirroring an idiom already used
  elsewhere in this same tracker (frozen tag tables, etc.).

## Recommendation

Do not proceed with this issue in its current form. At minimum: (a) do
not schedule implementation until #454, #458, #459 have actually landed
and TASK-0070/TASK-0061 exist as filed, scoped issues; (b) land or
retract the `docs/parity-contract.md`, `docs/virtual-hardware-parity.md`,
and `docs/machine-calibration.md` citations before treating them as
binding; (c) split the task or explicitly re-tier it given its actual
size; (d) resolve the structural-binding CI-cost question against
#425's own precedent before writing P4's test.
