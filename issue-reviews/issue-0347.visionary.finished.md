# Issue #347: FEAT-034: two implementations of the same machine are compared per retired instruction, and a knowingly wrong one is rejected — so a green run means something
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of the machinery, the claim is: *JLS should be able to prove that a drawn
CPU is right, not merely demonstrate that it ran.* That is the correct ambition and
it is exactly on the project's arc. README sells JLS as the tool for "teaching
computer architecture by drawing datapaths"; `riscv/README.md` already stakes the
strongest possible version of that claim; ARCHITECTURE.md's §"Simulation execution
strategy" decision names "agree bit-for-bit with the #202 RV32I integration golden
run as a differential oracle" as *binding on any future execution strategy*. A
differential oracle is already load-bearing in a recorded architectural decision
while not existing as a maintained artifact. Building one is right.

Three of the issue's ideas are unambiguously worth keeping and I would defend them
against any cheaper alternative: **the null test** (IC-1 — a harness that has never
been seen failing is a green-check generator), **UNKNOWN is never PASS**, and
**indexing by retirement rather than cycle**. Those are the parts that make this a
verification feature rather than a test.

What I dispute is everything between the goal and those three ideas: the seam it
cuts, the artifacts it names, and its account of what already exists.

## The fact the issue does not mention: the harness largely exists

§"Background — what exists at `2d0ca9d`" says "Nothing implements any of it,"
evidenced by `git grep RetireRecord -- src test` and `git ls-tree src/jls/`. Both
greps are scoped to Java, and the capability is not in Java. In this checkout:

- `/home/user/JLS/riscv/riscv_ref.py` — 975 lines: an independent RV32I assembler
  and behavioral emulator with its own self-test. This is the counterparty
  TASK-0070 proposes to build.
- `/home/user/JLS/riscv/verify.py` — 206 lines: runs a program on the drawn circuit
  and on the reference and compares full architectural state. This is the
  comparator.
- `/home/user/JLS/riscv/fuzz_diff.py` — 110 lines: randomized differential testing,
  hundreds of generated programs, parallel. This is stronger than anything in §5.
- `/home/user/JLS/riscv/test_primitives.py` — validates every emitted element
  against the real simulator.
- `/home/user/JLS/riscv/README.md:142-154` — "How verification works," including
  the observation that all 31 architectural registers, the PC, and the data RAM are
  *watched*, so a batch run already reports complete architectural state, and
  "Nothing here modifies JLS itself."

That last line is criterion 2 and IC-3, already satisfied, by construction, today.
An issue proposing 8 enabling maintainer-weeks to build a differential oracle should
open by saying what the existing one cannot do, and it does not name it once.

Three things the existing harness genuinely lacks, and they are the real feature:

1. **It is not run.** `.github/workflows/*` contains no reference to `riscv/`,
   `verify.py`, or `fuzz_diff.py`. The only differential oracle in the tree is
   unexecuted on every push. The single highest-value action in this entire issue
   costs a CI job, not sixteen weeks.
2. **It has no null test.** Correct, and this is the issue's best contribution.
3. **It compares only final state**, so a failure says "x14 differs" with no
   locality. This is the honest motivation for retirement indexing, and the issue
   never states it in those terms.

## Reframing 1 — the deliverable is a format and a CLI, not a Java package

The issue's artifacts are a Java record type (`RetireRecord`), a Java comparator
(`Differ`), and a new in-jar `jls.mach` package holding a behavioral RV32 model.
That is the wrong shape for this project, on this project's own evidence.

JLS's durable contributions are *documented formats over a headless surface*:
`docs/batch-interface.md` (normative, an explicit stability contract),
`docs/file-format.md`, the VCD profile, and `docs/vcd-interop.md:19-23` recording
the decision that "external tools consume the finished outputs." The recorded
plugin-trust decision in ARCHITECTURE.md puts Yosys, Icarus and ELK on a subprocess
boundary and keeps them there. Every non-trivial consumer of JLS's verification
story — autograders, `iverilog` diffs, the existing `riscv/` tooling — already sits
outside the jar and speaks a format.

So cut the seam there:

- **A retirement-trace format**, specified in `docs/` as a peer of
  `batch-interface.md`, emitted by JLS batch mode through the existing probe
  machinery. One record per retirement, no timing fields — the type-level guarantee
  the issue wants is a *grammar* guarantee, and a grammar in a normative doc with a
  golden test is stronger than a Java record, because it also binds the reference
  and every future counterparty, in any language.
- **A comparator that is a batch subcommand or a script**, taking two trace files
  and emitting `PASS` / `FAIL(k, field)` / `UNKNOWN`.
- **No in-jar behavioral CPU.** A logic simulator that ships an RV32 emulator inside
  its own jar has changed what it is. Keep the counterparty out-of-process, where
  `riscv_ref.py` already lives and where the recorded trust decision puts external
  models. If the autograder container needs a single-artifact story, that is an
  argument about the container image, not about the jar's contents — and it should
  be made explicitly, because it is the only real argument for `jls.mach` and the
  issue never makes it.

This deletes TASK-0070 as written, converts TASK-0072 from a Java type into a
document plus an emitter, and leaves TASK-0073 as a small program.

## Reframing 2 — adopt RVFI and the Spike commit log; do not invent a record

§2 says "the whole verification stack that solved this problem indexes at
instruction retirement" and then names nothing. The stack it is describing is
**RVFI** (the RISC-V Formal Interface): `rvfi_valid`, `rvfi_order`, `rvfi_insn`,
`rvfi_pc_rdata/wdata`, `rvfi_rs1/rs2_addr/rdata`, `rvfi_rd_addr/wdata`,
`rvfi_mem_addr/rmask/wmask/rdata/wdata` — a retirement-indexed field list with, by
design, no timing fields. Its sibling is Spike's commit log, the de-facto trace
format the entire RISC-V ecosystem diffs against.

Adopting RVFI's field list and one of these serializations instead of minting
`RetireRecord`:

- makes the *field list itself* peer-reviewed rather than a local invention;
- gives criterion 1 for free — the standard has no cycle field, so "no field for
  cycles" stops being a bespoke ratchet and becomes conformance;
- and, decisively, makes JLS traces comparable against **implementations nobody on
  this project wrote** — Spike, riscv-arch-test, riscv-formal. §7 lists "both
  implementations are wrong together" as a known weakness whose only mitigations are
  external, then defers them. Choosing a standard format converts that deferred
  mitigation into a side effect of the format choice. That is the single largest
  increase in what a green run means available anywhere in this issue, and it is
  cheaper than the bespoke route, not dearer.

The mirror-image benefit: a drawn JLS CPU emitting RVFI can be pointed at
riscv-formal's property suite without JLS building TASK-0112's property checker.

## Reframing 3 — streaming digest plus bisect, not two-pass whole traces

§3's concurrency model is "each implementation runs to completion producing a trace,
then the comparator reads both." TASK-0080 is a **Linux boot**. A boot-length RVFI
trace is 10^9–10^10 records per side; the design as specified requires materializing
both. The issue never mentions trace volume, and this is the kind of omission that
surfaces as "we have a harness nobody can run at the length that motivated it" —
which is exactly the failure Open Question 4 gestures at and then rides along.

The better shape uses artifacts the issue is already building:

1. Both sides maintain a **rolling digest over retirement records** and emit it at a
   fixed retirement stride. Comparison at boot length is then O(number of
   checkpoints) of I/O, not O(retirements).
2. On the first differing checkpoint, **replay the enclosing interval with full
   traces on and bisect within it**. TASK-0069's retirement-indexed input log makes
   replay deterministic; that is precisely what makes bisection sound.
3. `FAIL(k*, f*)` comes out of step 2 with the same precision criterion 3 demands.

This collapses §1's items 3 and 5 into one mechanism — the sync-point digest stops
being a "second net under the first" and becomes the primary comparison, with the
full record diff as the *localization* tool it actually is. And bisection over run
length is available against the existing final-state comparator *today*, with no
trace infrastructure at all: `verify.py` plus a clock vector cut at k rising edges
is already a "state at retirement k" oracle. A twenty-line bisect script buys most
of criterion 3's value this week.

## Reframing 4 — the exclusion set should start capped at zero, and IC-1 is mutation testing

Open Question 2 asks whether the exclusion set is bounded and by what. The existing
evidence answers it: **`verify.py` compares register-by-register and word-by-word
with no exclusions and passes.** A per-bit exclusion set with its own ratchet, its
own null test (IC-4), a numeric cap and per-entry written justifications (IC-8) is
elaborate governance erected around a set that is empirically empty. An exclusion
set is a symptom of comparing at the wrong boundary; the right response to its first
entry is usually to move the boundary, which §7 half-recognizes. Ship the ratchet
with the cap set to **0** and make every entry a maintainer decision. That is one
constant, and it makes IC-4 trivial and IC-8 vacuous-until-needed.

Open Question 5 asks what the knowingly wrong implementation is, concretely. The
answer is in the repo: `docs/mutation-testing-trial-2026-07.md` records an adopted
PIT trial, and `.github/workflows/mutation.yml` runs it. A "family of injected
faults, each with its own expected divergence index" *is* a mutation run scoped to
the reference implementation, with survivors interpreted as harness blind spots.
Reuse the adopted apparatus rather than hand-curating a wrong-implementation tree;
mutation testing generates a larger fault family than anyone will write by hand, and
survivor triage is already a practice here.

## Where the issue pulls against the project, and what I am disregarding

- **IC-6 cannot honestly be asserted at close.** "The same comparator points at the
  ternary machine with no comparator change" is the acceptance criterion for
  generality, but the ternary counterparty is TASK-0082, declared a *consumer* that
  does not block. A generality claim closed without its second instance is exactly
  the unexamined assumption IC-1 exists to forbid. **I am disregarding IC-6**: build
  for one machine, keep the record projection pluggable because RVFI's shape makes
  that free, and let the ternary work discover the generalization. Speculative
  generality is the one design sin this otherwise careful issue commits.
- **Criterion 2 ("not one line of `jls.sim`") is over-tight in one direction and
  under-specified in another.** `Simulator.probeSample` (`:285`) fires per probe per
  time; nothing in it knows what a retirement is. Detecting retirement in a *drawn*
  circuit means a convention — probes named for the RVFI fields, sampled on the PC
  register's commit edge — which lives in the `.jls` file and in a document, not in
  `jls.sim`. That is good news for criterion 2 and it should be stated, because as
  written a reader will look for the retirement notion inside the simulator and not
  find one.
- **The unratified contract (Open Question 1).** `docs/parity-contract.md` is not
  present in this checkout at all, while §1 instructs "Do not restate it; implement
  it." Implementing an unratified, currently-absent specification is the weakest
  link in the plan. Recommendation (a) — record the decision block first — is right,
  and the reframings above should be folded into it before ratification rather than
  discovered afterward.
- **Cost.** Booking 16 mw here of which 8 are other features' deliverables makes
  this feature look twice its size in any roll-up. The reframed feature is closer to
  2–4 mw: a trace-format document, an emitter over existing probes, a comparator, a
  bisect driver, a mutation-based null test, and a CI job.

## The order I would actually run

1. **This week:** wire `riscv/verify.py` and `fuzz_diff.py` into CI. The project's
   only differential oracle currently never runs. Everything below is worth less
   than this line until it is done.
2. **Next:** the null test against the existing harness, via PIT scoped to
   `riscv_ref.py`'s Java successor or its Python original. Observe it failing.
   Record the output. §1 item 6 is now true.
3. **Then:** bisect-over-run-length for first-divergence localization, on the
   existing final-state comparator. Criterion 3 satisfied with no new trace type.
4. **Then:** the RVFI-shaped trace document and emitter, when step 3's bisection
   proves too slow at boot length — and not before, because that is the measured
   trigger Open Question 4 asks for.
5. **Only then:** `jls.mach`, if and only if the container-single-artifact argument
   is made explicitly and accepted.

Steps 1–3 deliver most of "a green run means something" at roughly a tenth of the
booked cost, and they leave the reframed steps 4–5 strictly easier, not harder.
