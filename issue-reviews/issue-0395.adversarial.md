# Issue #395: TASK-0071: the guest image is built by a pinned, byte-reproducible recipe, its determinism requirements are machine-definition fields rather than prose, and where it lives is a decided, tested answer
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary

The issue asks for `scripts/build-guest-image.sh` (a digest-pinned,
byte-reproducible embedded-Linux build for an RV32 nommu guest), a
machine-definition record for four determinism settings, and a decided
residence for the resulting image. The concrete repo-facing observations
(O1, O3, O4) check out against HEAD. But the issue's own gating logic is
already broken by a sibling issue it cites, its central citations point at
documents that do not exist anywhere in this tree, and it silently assumes
JLS's existing interpreter-only simulator can execute a full embedded-Linux
boot — a scope leap the issue never argues for.

## Findings, most severe first

**1. Open Question 1 — the thing the issue calls "blocks execution
absolutely" — appears to already be resolved, and #395 doesn't reflect it.**
#395's Completion Criteria requires "Open Question 1 ratified by the
maintainer before work starts," and frames option (c) sidecar as merely a
"Recommended default." But issue #319 (FEAT-013, cited by #395 in its own
Related Work: "#319 ... irrelevant on the recommended sidecar branch"),
fetched directly, states in its own machine block: *"Maintainer ruling D15
decided it: the guest image is a SIDECAR FILE whose digest the circuit
records... #343 has already carried `blocked_by: []` since that ruling."*
That's a ratified decision recorded in the corpus #395 itself points to,
yet #395 still lists ratification as an open, blocking checkbox. Either
D15 postdates #395's filing and the issue is already stale on day one, or
#395 was filed without checking its own cited dependency. A picker either
wastes a step re-litigating a closed question or ships work whose gating
Completion Criterion can never be checked off as written.
**Recommendation:** before this task starts, reconcile #395's Open
Question 1 against #319's D15 and either strike the ratification
requirement or cite the ratifying comment directly.

**2. The issue's two most heavily-quoted supporting documents do not exist
in this repository.** `docs/parity-contract.md` is quoted verbatim by line
number ("`docs/parity-contract.md:3-7` records its own status as
'proposed normative contract...'") and `docs/machine-calibration.md` is
where P7 and a Completion Criterion require "a new subsection" to be
added. Neither file exists: `ls docs/` lists 24 `.md` files plus
subdirectories and neither name appears; direct reads of both paths fail.
Likewise, Method step 1 asserts "the layer stack simultaneously specifies
a CI lane that boots a guest and diffs a console stream and excludes
committed guest images" — no document named or resembling "layer stack"
exists anywhere in the tree (`grep -rl "layer stack"` returns nothing),
so the "recorded exclusion" this task claims to reopen is not verifiable
from the repository at all. A reviewer cannot check O5's claim, cannot
verify the "no committed guest images" position exists, and cannot confirm
what P7 is asking to be added to, because the target file is untracked.
**Recommendation:** either commit `docs/parity-contract.md` and
`docs/machine-calibration.md` (even as stubs) before citing them as
existing authority, or rewrite every citation to say "proposed in issue
thread #NNN," which is the only checkable claim.

**3. `evidence_commit` is not part of this repository's history.**
`git cat-file -e 2d0ca9dcd9db78b36c3caf4c6c57dd8701862cc7` succeeds (the
object exists) but `git merge-base --is-ancestor 2d0ca9d HEAD` fails — the
commit lives only on `remotes/origin/claude/jls-virtual-hardware-linux-
njsoma`, a branch the sibling issue #343's own comment thread already
concedes is "scheduled for deletion" and "not an ancestor of `master`."
Every "ABSENT at `2d0ca9d`" claim in O1/O2 (script inventory, missing
`test/fixtures/guest/`) happens to still hold when re-checked against the
live HEAD (`53116252...`), but that is luck, not method: the citation
methodology points reviewers at a commit that a normal clone of `master`
will never contain, so none of this issue's evidence is independently
reproducible by a contributor following the citations as written.
**Recommendation:** re-pin `evidence_commit` to a commit that is an
ancestor of `master` before treating the O-numbered observations as
verified evidence.

**4. Unstated, load-bearing feasibility gap: nothing in the codebase
suggests JLS's simulator can execute an embedded-Linux boot in tractable
time, and the issue never argues that it can.** The *only* RISC-V
precedent actually in this repo (`test/jls/RiscvCpuGoldenTest.java`,
`test/fixtures/riscv-sum1to10.jls`, `riscv/README.md`) is a hand-drawn
RV32I CPU built from ordinary JLS gate/mux/register elements running a
34-cycle "sum 1 to 10" test program — nothing resembling a PLIC-less SoC
booting a kernel, an initramfs, and a shell over "about 4.0×10^7 retired
instructions" (§7.12). `ARCHITECTURE.md`'s own recorded decision
("Simulation execution strategy: discrete-event interpreter is the sole
strategy," #221) explicitly calls a second, faster execution strategy
"premature optimization until CPU-scale designs are actually common" and
names the *drawn* RV32I CPU (#202) — not a Linux boot — as its revisit
trigger. #395 assumes the interpreter can carry a real Linux boot to a
shell in "the long-run lane" without a single benchmark, prototype, or
even an order-of-magnitude wall-clock estimate for JLS's own event loop
executing that workload. If the interpreter takes hours or days per boot,
P5 (boot the pinned image and diff two console streams) and P3 (two-host
reproducibility) become unaffordable to actually run, and nothing in the
issue would catch that until TASK-0070's runner exists.
**Recommendation:** add a feasibility spike — even a rough instructions/
second figure for the existing interpreter against a comparable
instruction count — as a precondition, or explicitly scope this task to
assume a runner whose performance is unproven.

**5. The two-host reproducibility check — the issue's own "load-bearing"
one — is explicitly kept out of CI, making the corresponding Completion
Criterion a one-time, unverifiable-after-the-fact box-check.** §11 states
plainly: *"P2 is nearly worthless without P3... an unpinned input shows up
across hosts."* Yet Open Question 5 recommends P3 run only as "a recorded
manual procedure... automatable later," and the Completion Criteria ask
only that "both comparisons [be] pasted" once, at close. Once merged,
nothing re-runs P3 on subsequent changes to the recipe, the toolchain
pin, or the kernel `.config` — a later contributor can silently reintroduce
an unpinned input (network timestamp, locale-dependent archive order,
etc.) and no lane will ever catch it, despite the issue itself identifying
this exact failure mode as the one P2-only testing hides.
**Recommendation:** at minimum, require P3 be re-run and re-pasted on any
future PR that touches the recipe, enforced by a PR template checklist
item or a doc comment near the script — "one-time and done" is not
sufficient given §11's own admission.

**6. Verification for P4/P5/P7 is entirely self-graded prose reading, not
tooling.** §9 states P4 "is judged by reading the checked-in record" and
P7 "is judged by reading the shipped subsection" — no test asserts the
four determinism fields are complete, correctly reasoned, or that a fifth
undiscovered source of guest-visible non-determinism (H2's own stated
failure mode) isn't lurking. A PR could add four fields that don't
actually cover every timestamp-dependent kernel config option and still
pass every literal Completion Criterion as written, because nothing
enumerates the full space of `CONFIG_PRINTK_*`-class options to check
against.
**Recommendation:** either add an automated linter/test asserting the
machine-definition record's schema and required-field set, or explicitly
acknowledge in the issue that P4/P7 are advisory human review, not gates.

**7. Cost is unbounded relative to the sibling estimate that actually
prices it.** #319 (fetched), the feature issue that names TASK-0071 as a
planned child, gives a cost reconciliation of *"TASK-0033 (2 wk) +
TASK-0034 (1.5 wk) + TASK-0071 (2 wk) = 5.5 wk."* Two maintainer-weeks for
a from-scratch, digest-pinned, two-host-reproducible embedded-Linux build
(kernel `.config`, uClibc+elf2flt bFLT toolchain pinning, busybox,
initramfs, device tree, a new machine-definition file format, residence
decision + docs + tests, absent-image diagnostics, negative controls) is
optimistic for a single-maintainer project — buildroot-class reproducible
embedded builds routinely take multiple person-weeks just for the
toolchain/rootfs iteration loop, before the two-host determinism work
this issue explicitly separates out as its own risk (§11). #395 neither
restates nor challenges that estimate.
**Recommendation:** have #395 (or #343) revisit the 2-week figure now that
this task's own §6-§9 detail is written out; if it's still 2 weeks, say
what's being cut to hit that.

**8. Minor: GPL interaction not addressed.** The Linux kernel and busybox
are GPL-2.0(-only in the kernel's case); JLS is GPL-3.0-or-later. The
issue is careful to note the image is "a test fixture, not a shipped
resource" (§7.12, correctly distinguishing it from the offline jar), which
avoids the sharpest linking concern, but committing GPL-2.0 kernel/
initramfs binaries into a GPL-3.0-or-later repository's source tree (or a
sidecar distributed alongside it) is still worth one sentence of license
review that the issue never gives it — particularly since `SECURITY.md`
and the README's provenance section (both read for this review) show this
project already tracks licensing custody carefully for release artifacts.
**Recommendation:** add a one-line note confirming the fixture's licensing
posture (kernel/busybox source vs. binary, redistribution terms) alongside
the residence decision.

## What's solid

- O1's `scripts/` inventory and "no `build-guest-image.sh`" claim are
  accurate against the live HEAD (`ls scripts/` matches the listed 18
  files exactly), independent of the stale evidence_commit problem above.
- O3's citation of `src/jls/FileAbstractor.java:65`
  (`MAX_CIRCUIT_TEXT_BYTES = 64L << 20`) is accurate, and the arithmetic
  that a 16 MiB image alone would consume ~99% of the decompressed-text
  cap is a real, checkable constraint that correctly rules out hex-text
  init as the residence mechanism.
- O4's `.gitattributes` precedent (`*.jls -text`) is accurate and a
  reasonable model for how a committed guest image would need to be
  marked.
- P9's "nothing homed under `riscv/`" negative control is well-motivated:
  `riscv/build_cpu.py` and `riscv/jlsbuild.py` do currently exist and are
  named elsewhere as slated for deletion, so guarding against a new
  dependency on that directory is a sound, cheap check.
- The task's explicit non-goals (§13: not building the toolchain, not the
  Sv32/OpenSBI hedge, not TASK-0070's runner, not #319's section
  mechanism) are clearly bounded and reduce scope-creep risk within the
  task itself, even though the *overall* program-level scope (finding 4
  above) is not similarly bounded.

## Verdict rationale

The concrete, repo-checkable claims (O1, O3, O4, P9) hold up. But the
issue's gating logic already conflicts with a sibling issue it cites
(finding 1), its two most-quoted supporting documents and its "recorded
exclusion" premise don't exist anywhere in the tree (finding 2), its
evidentiary anchor commit isn't reachable from `master` (finding 3), and
it commits the project to building and booting a full embedded-Linux
guest without ever establishing that JLS's own simulator can execute one
in a tractable time (finding 4) — a feasibility question that, if it
comes back negative, invalidates P5/P3 rather than just needing a tweak.
Needs-rework, not should-not-proceed: the buildable parts of the task
(the pinned recipe, the machine-definition record, the residence
decision) are independently sound engineering asks once the documentation
citations are fixed and Open Question 1 is reconciled with #319.
