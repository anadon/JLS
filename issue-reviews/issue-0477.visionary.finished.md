# Issue #477: TASK-0070: an independent RV32 machine exists as a pure leaf package with a headless runner — testable with no simulator, no circuit, and no wall clock
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the 14 sections and the issue makes one claim: *JLS's parity story needs a second
machine, written in Java, that shares no code with the drawn one.* The package-boundary
argument for it is the best part of the issue and it is correct. O3 (no `module-info.java`
anywhere in `src/`) plus O4 (`jls.elem` floored at 0.730/0.700/0.585, `jls.sim` at
0.930/0.920/0.845, `pom.xml:449-471` against the 0.545/0.535/0.505 bundle floor at
`pom.xml:355-375`) is a genuinely forced conclusion: sealed-hierarchy rules would drag ISA
logic into `jls.elem` and file 3,000 lines under the weakest floor in the tree. Landing the
ratchet prefix, the JaCoCo rule and the PIT entry in the *creating* commit is the right
governance instinct and matches how `jls.hdl`, `jls.core` and `jls.module` were born clean
(`HeadlessCoreRatchetTest:60-90`). Endorse all of that without reservation.

Everything below is about what goes in the package, in what order, and where the seam runs.

## 1. The gate is coverage; the property that matters is conformance

The Definition of Done has roughly eighteen checkboxes. Not one of them asks whether the
machine is *right*. P8 and P9 gate on 0.930/0.920/0.845 and 80/82 mutation score — but 93%
line coverage of a wrong decode table is 93% coverage of wrongness, and mutation score
measures whether your assertions bind, not whether your reading of the ISA manual was
correct. The issue knows this: T2 concedes §9.3's "two models by the same author can be
wrong together", and FEAT-033 (#343) Open Question 4 says outright that the mitigation "is
an external conformance corpus, not more tests" and that this is "the risk the whole
feature's value rests on" — then marks it *rides along*.

**I am explicitly disregarding P8/P9 as the primary acceptance gate.** The project has
already designed and costed the external oracle: `docs/standards-adoption/05-riscv-compliance.md`
specifies RISCOF + `riscv-arch-test`, the signature-region protocol (4 bytes/line,
little-endian hex), a `begin_signature`/`end_signature` diff against the Sail model, the
ELF32 reader (~150 lines, stdlib), the linker script, and the halt mechanism. That document
also warns that `riscv_ref.py`'s assembler "must not be extended to cope with the
C-preprocessor macro soup in arch-test sources" — the same warning applies to a Java rewrite
of it.

Concretely: land `test/jls/mach/Rv32ArchTestSignatureTest` in the same commit, driving
committed test binaries plus once-computed Sail signatures (no toolchain at test time, no
network — the same shape as the existing golden fixtures under `test/fixtures/`). Two things
follow that the issue never notices. First, the corpus *is* the coverage: a few hundred
arch-test programs walk nearly every decode arm, every trap path and every corner the hand
written unit tests in §8 would be laboriously reproducing. The cheapest route to 0.930 is
the corpus, not a hand-rolled test per opcode. Second, it converts model #2 from "same
author, may be wrong together" into "checked against RVI's corpus", which is the *only* form
in which the parity claim this whole feature exists to enable actually means something.
Deferring that to #423 means 3,000 lines of unvalidated logic become the fixture #392, #395
and #347 all pin themselves to before anyone has checked it against a third party.

## 2. The package holds two things with very different readiness

`jls.mach` as scoped is a *pure ISA core* (`ArchState`, decode table, `step`) welded to an
*SoC deck* (`Uart16550Model`, `ClintModel`, simulated `mtime`, `Runner`, `Termination`,
trace emission). Their consumers are not the same and are not close in time.

The ISA core is what #392 (TASK-0079) actually consumes: its P4 is an exhaustive 8×8 ALU
sweep, P5 a seeded 32-bit sweep, P6 the register-file write-read-same-cycle and x0 cases.
None of those needs a UART, a CLINT, `mtime`, a `Termination`, or a guest image. #392's
`alu` and `regfile` boundaries could go green against a ~1,000-line core.

The SoC deck exists to serve #395's guest image and a boot. Look at what has to be true for
that boot to have a counterparty: `docs/standards-adoption/05-riscv-compliance.md` records
that the shipped CPU "has no CSRs, no traps, no privilege modes, and no misaligned-access
behaviour"; `docs/capability-roadmap/README.md:259` records that "the CSR/trap element set —
masked-write addressable state — that RISC-V privileged mode needs" does not exist as an
element; #392's own dependency list waits on TASK-0076 for byte lanes, without which the
16550's three byte addresses on a 32-bit bus are unimplementable on the drawn side. And the
measured throughput is **8,090 simulated cycles/s warm, 318 ns/event**
(`docs/capability-roadmap/keystone-c-performance.md:137,655`) on a 228-element fixture; the
target machine is ~580 logic elements. A minimal RV32 nommu boot to shell is 10^8–10^9
instructions, i.e. **hours to days of wall time per drawn boot**, on a bigger and slower
circuit. That is not fatal — it is a scheduled long-run lane — but it means the SoC deck's
counterparty is a truncated prefix of a boot, many months and several element-level features
away.

**Reframing 1 (recommended): split TASK-0070 in two.** Land `jls.mach` with the ISA core,
its governance triad, and the conformance corpus. File the SoC deck (`mach.dev`, `Runner`,
`Termination`, trace emission) as its own task, sequenced with #395/#347 where it acquires a
consumer. Note what this does to the issue's own Open Questions: *all three* — which CSRs are
implemented, what `Runner`'s default `Termination` is, whether `StepResult` carries a full
`RetireRecord` — are SoC-deck questions. Two of them are marked "blocks execution". The split
makes the blocking questions disappear from the critical path and lets #392 start now.

## 3. The seam is cut at the ISA, and it should be cut above it

FEAT-033's out-of-scope list says the ternary machine's emulator "is an inhabitant of this
package and uses this runner seam", and #345 (FEAT-039) is a whole feature for a JLS-T3 ISA
with its own toolchain and drawn CPU. But §7.4 bakes RV32 into the package's top-level public
types: `record ArchState(int pc, int[] gpr, int privilege, ...)`, `MemoryView.read32(int)`,
`Machine.step`. A ternary machine has neither 32-bit words nor a 32-entry GPR array. The
first inhabitant after RV32 forces a rewrite of every public type in a package that by then
sits under the tree's strictest coverage bar with downstream consumers pinned to it.

**Reframing 2: `jls.mach` is the seam; `jls.mach.rv32` is an inhabitant.** The seam is
`MemoryView`, `Termination`, `Runner`, the retirement-trace contract and an `ArchState`
marker with an ISA-agnostic field-set descriptor (which is also FEAT-033 Open Question 3's
$\mathcal{F}$, currently unowned). RV32's `ArchState`, decode table and `step` live in the
subpackage. This costs nothing today — the same files, one directory deeper — and is the
difference between #345 being additive and #345 being a breaking rewrite. It also gives the
JaCoCo rule a natural per-inhabitant shape rather than one bar over a package whose contents
will diverge wildly in testability.

## 4. `MemoryView`'s two implementations test nearly nothing

H3/P4 — two differently-shaped `MemoryView`s produce element-for-element identical
`ArchState` sequences — is close to a tautology for a two-method, side-effect-free interface
over two array-ish stores. T1 correctly worries the chunked view will be a thin wrapper; the
deeper problem is that even a genuinely different layout tests only that `step` doesn't read
the map's private fields, which the type system already guarantees. Meanwhile the property
that *does* matter for #392's bus-backed view is nowhere asserted: a real bus has read
side effects (a 16550 RBR read pops a byte), fault responses, and ordering.

`int read32(int addr)` cannot express any of that. §7.11 says an out-of-range read is an
access-fault trap "attributed to the causing instruction" — but the signature has no channel
for a fault except an exception, which contradicts the data-only, pure-function style the
rest of the issue insists on, and §7.4 never says which it is.

**Reframing 3: cut `MemoryView` as request/response.** `Response access(Request)` where
`Request` is `(addr, op, value, byteMask)` and `Response` is a sealed `Ok(value) | Fault(cause)`.
Three things fall out for free: faults become data rather than control flow, so `step` stays
pure and total; MMIO read side effects become expressible, so the UART model and the bus
backed view share one contract; and the request stream *is* a transaction log, which is a
strictly stronger parity object than a retirement trace (retirement traces cannot see
memory-order divergence). Then replace P4 with (a) a shared `MemoryViewContract` test factory
every implementation must pass — including a read-side-effect device stub — and (b) a
transaction-log equality assertion. That is the artifact #392 needs and the throwaway second
array implementation is not.

## 5. What is being thrown away

`riscv/riscv_ref.py` is 975 lines of RV32I emulator with an assembler, a self-test, a
directed suite (`verify.py`) and a randomized differential harness (`fuzz_diff.py`) that has
been run against the *actual drawn circuit*. D5 correctly forbids routing deliverables
through `riscv/`, and #413 deletes it. But D5 says the replacement must be "a first-class,
in-tree, tested JLS mechanism" — it does not say the accumulated verification value must be
discarded. The issue takes only "the design" as input. The thing worth porting is
`fuzz_diff.py`'s *generator and its differential discipline*: a seeded random-program
property test driving `jls.mach` against the drawn machine through batch mode. That is a
better use of two days than hand-writing unit tests to reach a line-coverage number, and it
is the one piece of evidence in the tree that a Java model and a JLS circuit can be held to
each other at all. Recommend an explicit line in §6 Materials: the corpus and generator port
across, the Python does not.

## Verdict

**endorse-with-reframing.** The package boundary, the forced-by-O3/O4 argument, and the
same-commit governance are right and should land as written. The reframings, in priority
order: (1) make the arch-test conformance corpus a landing criterion, not #423's problem —
it is both the strongest evidence and the cheapest path to the coverage bar; (2) split the
ISA core from the SoC deck and land the core now, which also dissolves all three blocking
Open Questions and unblocks #392 immediately; (3) put RV32 in `jls.mach.rv32` under an
ISA-agnostic seam, before #345 makes that a rewrite; (4) re-cut `MemoryView` as
request/response with a shared contract test and a transaction log, replacing the near
vacuous two-implementations property. Taken together these make the package smaller, land it
sooner, give it real evidence, and leave the seam where the project's own roadmap says a
second ISA and a bus-backed memory are going to arrive.
