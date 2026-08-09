# Issue #390: TASK-0072: a retirement record whose type makes over-constrained parity a compile error, emitted as a canonical trace through the existing probe hook
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

One sentence, and the issue says it itself: *"JLS names the exact instruction where
your drawn machine first disagreed."* Everything else — the record, the strobe, the
flag, the file — is machinery in service of **localization**. That goal is correct,
it is squarely on the project's arc (README: JLS exists for "teaching computer
architecture by drawing datapaths"), and I endorse it without reservation.

What I do not endorse is the route. The issue frames the deliverable as *a type plus
a new simulator subclass plus a new CLI flag plus a new private file format*, proven
against a synthetic strobe fixture, sequenced behind an unratified contract and an
unfiled reference runner. Localization is achievable with a fraction of that, on
machinery this repository already ships, with a real CPU as the fixture on day one.

## Grounding note

`docs/parity-contract.md` — the source of the twelve-component alphabet, §4's
permitted-to-differ set, and the not-adopted table — **is not present in this
checkout** (`/home/user/JLS/docs/` has 26 `.md` files and 2 subdirectories; no
parity contract). Every substantive claim below is therefore derived from code and
from documents that are present. #347's own Open Question 1 states that contract is
unratified and that this *"blocks filing TASK-0072."* It was filed anyway with
`blocked_by: []`.

## Finding 1 — the stated design cannot drive the workload it exists for

`RetireTraceRecorder extends Simulator`, "a sibling of `BatchSimulator`", is stated
five times as the point of the task (§7.4, O3, H2, P6, §11). But:

- `/home/user/JLS/src/jls/JLSStart.java:246` — batch mode constructs exactly one
  simulator: `BatchSimulator batchSim = new BatchSimulator();`
- `/home/user/JLS/src/jls/JLSStart.java:250-251` — stimulus is applied via
  `batchSim.setTestFile(testFile); batchSim.addTestGen();`
- `addTestGen()` is at `/home/user/JLS/src/jls/sim/BatchSimulator.java:190`, and
  `displayOutcome()` at `:562` — **both on `BatchSimulator`, not on `Simulator`.**

A `Simulator` sibling therefore has no `-t` test-vector stimulus, no watched-element
output, no probe registration (`findProbes`, `:120-125`), and no outcome display. A
parity run *is* a stimulus run — `riscv/verify.py:gen_clock` exists precisely because
a CPU circuit does nothing without a driven clock waveform. So the sibling either
duplicates `addTestGen` to preserve the "zero change to `jls.sim`" trophy, or it is
not usable. Neither is stated. The one-line repair, entirely inside the issue's own
constraints: **`RetireTraceRecorder extends BatchSimulator`**, overriding
`probeSample`/`afterEvent` and calling `super`. That also buys `-vcd` *and* `-rvfi`
in one run — which is what a student staring at retirement index 47 actually wants,
and which the sibling design makes impossible.

## Finding 2 — the project already has the seam; this invents a parallel one

`docs/extension-points.md` is the recorded catalog of typed observation seams
(ratified in `ARCHITECTURE.md`, "Extension points: the typed seam catalog", #223).
Its rules say, verbatim: *"Pending seams are named here first. A seam gets its row
(and its owning issue) before its contract exists, **so nobody invents a parallel
mechanism in the meantime.**"* The catalog already ships a many-cardinality observer
seam of exactly the needed shape: `collab.op-observer` / `jls.collab.op.OpSink`,
"register, then observe every submit."

Subclassing `Simulator` to observe it is the parallel mechanism that rule exists to
prevent, and it does not compose: subclassing is cardinality-one, so VCD export,
retirement recording, and every future observer (coverage, fault injection, power —
`docs/capability-roadmap/lf-05-fault-and-power.md`) are mutually exclusive forever.

**I am explicitly disregarding P6 and #347's IC-3 ("zero change to `src/jls/sim/`").**
That invariant is aesthetic, and it is actively producing a worse architecture. The
right change is a `sim.probe-observer` row in the catalog with contract
`jls.sim.ProbeSink` in `jls.sim`, cardinality many, phase "register before run" —
copying the `OpSink` pattern the project already shipped. `Simulator.probeSample`
becomes a fan-out over registered sinks; `BatchSimulator`'s VCD folding becomes the
first contribution; the retirement recorder becomes the second. That is roughly the
same number of lines as the sibling subclass, it is one obviously-reviewable addition
rather than a duplicated construction path, and it converts a hard architectural
ceiling into an open seam. `ExtensionPointCatalogTest` then pins it in both
directions — a stronger ratchet than P6's "no member added" negative.

## Finding 3 — the fixture is synthetic when a real CPU is already in the tree

§11 concedes it: *"The fixture is a synthetic circuit, not a CPU. It exercises the
sampler, not the alphabet's adequacy; H1 is only genuinely tested when TASK-0070 and
a real machine exist."* H1 — that twelve components suffice — is the *only*
hypothesis here worth testing, and the plan defers it past two unfiled tasks and
three open blocking features.

Meanwhile `/home/user/JLS/riscv/` ships, today, at HEAD:

- `riscv_ref.py` (975 lines) — an independent RV32I assembler and functional
  emulator, explicitly "an executable oracle";
- `build_cpu.py` / `make_cpu.py` — assembly to a runnable `.jls` circuit;
- `gui/cpu.jls` + `gui/RiscvCpu.java` — a drawn single-cycle RV32I datapath;
- `verify.py` (206) and `fuzz_diff.py` (110) — **a working differential harness**,
  comparing register file and data memory against the emulator over a directed suite
  and hundreds of randomized programs;
- `examples/{fib,memtest,sum1to10}.s` — the very fixtures #278 wants promoted.

The issue never mentions any of it, though it cites #202. That is the blind spot
that makes this a redirect rather than a reframing. Note especially what
`fuzz_diff.py` compares: **final** architectural state. Its known weakness is exactly
what a retirement trace fixes — a divergence that self-corrects is invisible, and a
failing case reports "final state differs", not "instruction 47, field `rd_value`".
So the retirement trace has an immediate, demonstrable payoff **on a test that
already exists and already fails informatively-poorly**. That is incomparably better
evidence than P3/P4 against a hand-built strobe fixture, and it tests H1 in week one
instead of quarter three.

## Finding 4 — the format forecloses the interop the project has already costed

§7.12 point 5 argues the trace must never carry a version because it is a `diff`
input. Fine — but the format chosen is *private canonical text whose only consumer is
`diff`*, in a repository with a whole `docs/standards-adoption/` tree. The alphabet
is described as "RVFI-derived"; derive it all the way. `05-riscv-compliance.md`
documents RISCOF's signature artifact (4 bytes per line, little-endian hex) as the
thing a checkable conformance claim rests on, and names sail-riscv as the reference
model. #347's own re-planning protocol says the mitigation for "both implementations
are wrong together" is *"comparing against an implementation nobody on this project
wrote."* An RVFI-compatible trace shape gets Spike and sail-riscv as counterparties
for free and makes TASK-0070's two weeks of bespoke Java reference runner largely
redundant against `riscv_ref.py`. A private one-line-per-record format gets nothing
and must be re-justified the first time anyone wants an external oracle.
`07-waveform-formats.md` also models the right move here: emit a documented
*profile* of an external format, not an invention.

## Finding 5 — two of the type's enforcement claims do not hold

- **The title says "compile error." It is not one.** P2 is a reflective JUnit
  assertion over `getRecordComponents()`. That is a unit test, and a unit test is
  precisely the "review finding" the abstract says it is replacing. The real
  structural property — a comparator that cannot compare timing because no timing
  value is in its input — comes from the record having twelve components, not from
  the regex. Claim that, not the compile error.
- **The regex is theater.** `(?i)cycle|time|stall|pipeline|cache|latency` is defeated
  by naming a component `ticks`, `retire_stamp`, `elapsed`, or `depth`. The invariant
  that actually bites is *the count and the exact ordered name list*, which P2
  already asserts. Keep the twelve names; drop the regex, or state plainly that it is
  a courtesy hint rather than an enforcement mechanism.
- **`order` cannot diverge.** §7.10 defines `order(k) = |{j ∈ S : j ≤ k}| − 1` — it is
  manufactured by the recorder from strobe count, identically on both sides. §11 warns
  that "a field that is always zero is a false agreement"; this is the same defect
  applied to the alphabet's index field. It is eleven comparable components plus a
  stream counter. In real RVFI, `rvfi_order` is *reported by the core* so out-of-order
  retirement can be checked; manufacturing it discards that.

## Finding 6 — parity becomes a property of how a student named their wires

§7.3 admits the reserved `rvfi.*` names "arrive from the drawn circuit and are
therefore **user text**", needing TASK-0008's validation, and §7.11 concedes that a
missing probe silently yields a never-driven field. So the harness's correctness is
coupled to thirteen wire names a student must add by hand to be gradable.

An alternative seam is available and the codebase already uses it:
`BatchSimulator.findWatched` observes **elements**, which the loader validates and
which cannot be silently absent. A hybrid is strictly better: anchor the strobe to a
designated clocked element (the PC register latching is retirement, in any RV32
machine, and `riscv/README.md` describes exactly that timing), and project `rd_*` and
`mem_*` from the register-file and memory elements. A missing element is a loud
error; a missing probe name is a false agreement. That removes the TASK-0008 coupling
entirely and removes the instrumentation tax on the student's drawing.

## The alternative, concretely

1. Add `sim.probe-observer` to `docs/extension-points.md` and a `jls.sim.ProbeSink`
   contract; make `Simulator.probeSample` fan out; move VCD folding to the first
   contribution. One reviewed addition to `jls.sim`, pinned by
   `ExtensionPointCatalogTest`.
2. Land `RetireRecord` exactly as specified — twelve components, P2 written first.
   This part of the issue is right and survives unchanged.
3. Make the recorder a `ProbeSink` (or, if step 1 is refused, `extends
   BatchSimulator`), strobe-gated per §7.10. P4 stays; it is the correctness test.
4. Emit an RVFI-profile trace, documented as a profile, per `07-waveform-formats.md`'s
   model.
5. **First consumer is `riscv/`, not a synthetic fixture.** Probe `gui/cpu.jls`, teach
   `verify.py` to read the trace, and report the first differing retirement index
   against `riscv_ref.py`. `fuzz_diff.py`'s existing corpus becomes IC-1's null-test
   family for free — every seeded random program that already diverges is a fault
   with a known expected index.

Step 5 tests H1 against a real machine immediately, retires most of TASK-0070, and
gives #278 its fixtures. The eight-week enabling path in #347 collapses to a seam, a
record, a projection and a reader.

## Verdict

**redirect.** The goal is right and the record type is right. The route — a sibling
`Simulator` that cannot drive test vectors, a private trace format in a project with
a standards-adoption discipline, an enforcement regex that does not enforce, and a
synthetic fixture standing in for a CPU that is already in the tree — should be
replaced by the observer seam the architecture already records and by `riscv/` as the
first consumer. I am explicitly setting aside P6 and #347's IC-3; "zero change to
`jls.sim`" is the constraint doing the most damage here.
