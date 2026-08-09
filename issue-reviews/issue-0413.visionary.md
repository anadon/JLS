# Issue #413: TASK-0025: the CPU-scale calibration anchor becomes a tracked, censused fixture and `riscv/` is deleted without taking the measurement basis with it
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: rethink

## The end is right; the model of the artifact is wrong

The goal — every wall-clock figure in `docs/` divides by a basis that can be
reproduced from the tracked tree, and `riscv/` stops being a load-bearing
directory nobody maintains — is correct and overdue. I endorse that end and
disregard most of §5, §8 and §14, because the task's central factual premise is
false, and nearly every expensive obligation in it descends from that premise.

**The premise.** "Delete the directory first and every wall-clock figure in the
plan becomes permanently unreproducible" (Abstract); H3, "the ordering
constraint is real and one-directional"; §11, "the deletion is irreversible in
practice"; `blocked_by: [377, 379]`.

**The measurement.** The anchor is already in the tree. `git ls-files` shows
four tracked `.jls` files; one of them, `test/fixtures/riscv-sum1to10.jls`
(120,179 bytes, plain `FORMAT 1` text), has this element histogram:

```
810 WireEnd, 43 Mux, 43 Constant, 34 Splitter, 34 AndGate, 32 Register,
9 Binder, 8 NotGate, 5 XorGate, 5 Extend, 4 Adder, 3 ShiftRegister,
3 OrGate, 3 Memory, 1 InputPin, 1 Decoder
```

`docs/capability-roadmap/keystone-c-performance.md` §2 publishes the k2000
census as `elements=1551 wireNets=297 maxBits=32` with the histogram
`Constant 43, Mux 43, AndGate 34, Splitter 34, Register 32, Binder 9, NotGate 8,
Extend 5, XorGate 5, Adder 4, Memory 3, OrGate 3, ShiftRegister 3, Decoder 1,
InputPin 1 (+ 810 WireEnd, 513 Wire)`. **Term for term identical.**
228 + 810 + 513 = 1551.

It is not a coincidence. `riscv/bench_kernel.py`'s `PROG` is, instruction for
instruction, `riscv/examples/sum1to10.s`. The tracked fixture's imem ROM reads
`0 93 / 1 100113 / 2 b00193 / 3 2080b3 / 4 110113 / 5 fe314ce3 / 6 102023` —
that is `bench_kernel.py` at `iters=10`. `k2000.jls` is the same 120 KB file
with **one hex literal changed**: word 2 becomes `7d100193`
(`addi x3, x0, 2001`) instead of `b00193` (`addi x3, x0, 11`).

And the 193 KB `-t` vector is already generated in Java, in this repository, at
`test/jls/RiscvCpuGoldenTest.java:50` — `clockVector()` is a line-for-line port
of `bench_kernel.gen_kernel`'s `gen_clock`, same `HALF = 1000`, same
`clk 0 / until k*HALF / end`. Set `STEPS = 6004` and you have `k2000_clk.txt`.

So H3's own falsification criterion — *"refuted if the anchor can be
reconstructed from what remains after deletion"* — is met, today, by `sed` and
one integer. There is no one-directional ordering constraint, no blocking
dependency on #377/#379 for *this* task's sake, and nothing irreversible about
the `git rm`. The apparatus scheduled for destruction is not the apparatus.

## What that dissolves

Once the anchor is understood as *one tracked circuit plus two integers* rather
than *an opaque build product*, the following stop being problems rather than
getting solved:

- **Open Question 2 / the #378 (TASK-0016) dependency.** The fixture is 120 KB
  — the third-largest tracked file, smaller than `src/jls/edit/SimpleEditor.java`
  (182 KB). Comment 2's "tens-of-megabytes fixture" and the LFS/size-cap
  question are about a file that does not exist. P7's `MAX_CIRCUIT_TEXT_BYTES`
  check is 0.2% of a 64 MiB cap.
- **The #728 double-ownership.** #728 wants "at least two circuits below CPU
  scale." Same circuit, `STEPS = 100`. Scale is a property of the vector, not
  the artifact, so there is nothing to double-own.
- **O6's "location is forced."** `.gitignore` is a tracked file editable in the
  same commit; `!test/fixtures/**/*.jls` is a line someone wrote for #56, not
  physics. Treating a self-imposed rule as a law of the repository is the
  clearest symptom of the framing error running through this issue.
- **§7.8's "if the regeneration is lost mid-run it cannot be re-run after the
  deletion."** It can, from a text editor.

## The reframing I would build instead

**1. Parameterize the workload; stop archiving artifacts.** One tracked CPU
circuit; scale supplied by the `-t` vector and the loop bound. Concretely: a
long-run test that loads the existing fixture and drives `clockVector(6004)`,
with the loop-bound word either patched in-test or carried by a sibling fixture
(also 120 KB — commit it, it costs nothing). k500/k1000/k2000 become three
integers in a Java file. **The more elegant version**, worth its small circuit
change: make the loop bound arrive from an input pin / memory-mapped word so
the vector supplies it, and there is exactly *one* CPU-scale artifact in the
tree forever. That is what `docs/batch-interface.md` is for — JLS's own
contract already says workloads are inputs.

**2. Promote the census to a shipped capability, not a private test helper.**
§7.6 justifies a Markdown table with "there is exactly one consumer." The issue's
own comment thread enumerates six (#442, #476, #393, #879, #362, #512), plus
#726 and #728. The right seam is `jls -census circuit.jls` as a batch mode
beside `-i`, `-vcd`, `-export`, `-savetext`, under the
`docs/batch-interface.md` stability contract — which also rescues the
uncommitted `jls.sim.Census` that comment 3 correctly flags as the real loss.
D5 explicitly permits this: *"the **approach** those files embody may survive."*
Census-as-a-command means every document cites a reproducible command instead of
a transcribed number, and P3 is one assertion anyone can run.

**3. Replace the bespoke ratchet with a general one.** `NoRiscvDirectoryReferencesTest`
is a permanent test whose job is to forbid one string that will be meaningless
to a maintainer in 2030 — permanent cost, one-time value, and an allowlist that
must be curated forever. A test asserting **every repo-relative path cited in
tracked Markdown and in javadoc `{@code}` spans resolves on disk** subsumes P2
(the O5 doclint gap the issue correctly identifies), subsumes P5, needs no
allowlist keyed to a deleted directory, and pays off on every future file move.
That is the same instinct as `HelpTopicsTest`'s link checker, generalized —
which is exactly the seam this project already cuts along.

**4. Fix the thing §8 does not mention, which is the most valuable edit here.**
`ARCHITECTURE.md:354`, inside a **binding recorded decision** (simulation
strategy, #221): *"Revisit trigger: a concrete CPU-scale design on the `riscv/`
trajectory (#200/#201/#202) that is unusably slow interactively."* Open Question
4 recommends classifying `ARCHITECTURE.md` as *historical*. It is not: the
condition under which JLS would ever adopt a second simulation strategy is
defined in terms of the directory this task deletes. Rewrite it to name the
tracked fixture and a measured threshold — precisely what FEAT-009's constants
exist to supply. Same for the equivalence criterion at `:366`.

**5. Promote, don't re-home.** `examples/` exists at the repo root (only
`autograde/` tracked). #202 (open) asks for the RV32I CPU as a worked example;
README and ARCHITECTURE frame JLS as a pedagogy tool for "teaching computer
architecture by drawing datapaths"; `riscv/README.md` is a genuinely good
document. Moving the CPU to `examples/riscv-cpu/` — circuit, `.s` sources,
README, and `gui/cpu.jls` (8.8 KB; Open Question 1 answers itself) — serves
#202, #726, #728, this task and the project's stated mission at once, and turns
"delete `riscv/`" from a loss into a promotion. Burying it in `test/fixtures/`
as anonymous data pulls against the arc; D5 forbids the *Python*, not the CPU.

## Two defects in the invariant, worth fixing whatever route is taken

- **P3's hard equality is red on day one.** §7.10 defines `L(C)` as
  non-`Wire`/non-`WireEnd` = **228** on the tracked fixture. The document being
  preserved publishes **225** logic elements, and `Levelized`'s 522 nodes =
  225 + 297 nets. Pin the projection (`instanceof LogicElement` — `Constant`
  and `InputPin` do not react) and say which count six documents divide by, or
  the ambiguity survives the whole exercise.
- **H2 is refuted by the evidence above, not by a future experiment.**
  `riscv-sum1to10.jls` and `k2000.jls` have *identical* censuses and differ by
  175x in total events. The census is lossy exactly where the anchor lives — in
  the ROM image. If you want an invariant that protects a published figure,
  commit the **SHA-256 of the fixture's canonical text** (total) and keep the
  census as human-legible commentary (lossy).

## What I would keep verbatim

The transcription of the differential-harness design into
`docs/parity-contract.md` **with its self-consistency limitation** — that
`riscv_ref.py` was written by the author of the design under test — is the best
paragraph in this issue and the one piece of genuine, unrecoverable knowledge in
`riscv/`. 975 lines of RV32I emulator is not the asset; the honest statement
about what it can and cannot witness is. Do that step first, before anything
else, because unlike the fixture it really is irreversible.

## Recommendation

Run the ten-minute experiment that settles this: regenerate `k2000.jls` while
the Python still exists and `diff` it against `test/fixtures/riscv-sum1to10.jls`.
If the diff is the single ROM word I predict, close out the blocked-by chain,
drop the storage-policy coordination, the #728 boundary, the bespoke ratchet and
the regenerate-before-delete ordering, and spend the freed effort on `-census`,
the general dangling-path test, the ARCHITECTURE.md revisit trigger, and moving
the CPU to `examples/`. If the diff is larger than I predict, this review is
wrong and the issue stands close to as written — but the diff is the cheapest
possible way to find out, and it is not in §8.
