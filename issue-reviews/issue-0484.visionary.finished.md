# Issue #484: Measured ground truth for the virtual-hardware / virtual-logic parity study: engine constants, boot arithmetic, element count, live-console limit, parity contract
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of its framing, #484 makes one claim: *the measured facts about what JLS's engine
costs are load-bearing for the project's future, and they are about to become unreachable,
so they must be written somewhere durable.* The first half is right and important — those
numbers are the evidence that fires `ARCHITECTURE.md`'s #221 revisit trigger, and they are
the only quantitative description of JLS's inner loop that has ever existed. The second
half is where the issue goes wrong, and the wrongness is architectural, not clerical: it
chooses **a GitHub issue body as the storage medium for versioned technical truth**, in a
repository whose entire documented practice is the opposite.

## The premise is false in both directions

**Upward — the primary source is on `master`.** `docs/capability-roadmap/keystone-c-performance.md`
is 869 tracked lines in this checkout, and the citations #484 marks *"(dead path)"* resolve
cleanly against it: `keystone-c-performance.md:126` is the event census, `:136-138` is
"318 ns per event / 8,090 cycles/s warm". #496 itself says so in plain text ("**is on
`master` and survives** … that citation is still resolvable"). #484's core instruction —
"do not try to open it" — sends readers away from a live, versioned, reviewable document
toward an issue body that cannot be diffed, cannot be anchor-checked by a test, and cannot
be amended by a PR.

**Sideways — the same corpus was rescued three more times.** #494, #495, #496 (and #485,
#493, #497, #498, #499) carry the same measurements at finer grain. #496 states outright
that it is "the later, finer-grained record" and that it carries the raw quantities, the
divisions, the profile breakdown and the sweeps that #484 lacks. The rescue produced
exactly the failure mode it was meant to prevent: **five uncoordinated copies of one
evidence base, already drifting.**

## #484 is the least accurate of the copies, and it is the one that reads as authoritative

Its §7 is labelled "authoritative" and "supersedes §§2–6". Checked against #496, §7 breaks
four of the quoting rules #496 declares normative, and contradicts it on facts:

- §7 says 2.0–2.6 M events/s "was the INCLUDING-`initSimulation` figure". #496 §2.2:
  "It is **not** true that the band is simply 'the including-init figure'" — on that
  workload including-init is 1.78 M/s, *below the band's floor*. #484's flagship
  correction is itself the error.
- §7 quotes "After the full stack (4.9x): **20–21 min**". #496 §4.5: "**Never quote 4.9×
  alone as 'the full stack'**… the honest row is **20–38 minutes**, not '20–21 minutes'."
- §7 asserts engine work "accrues **ENTIRELY** to the structural tier — the behavioral row
  does not move". #496 §4.5 refutes this from the arithmetic: the behavioral row divides by
  the same 3.14 M/s the stack multiplies.
- §7 prints "nommu **1.66–1.72 h**" as a correction. #496 §4.2 shows the honest band is
  **1.2–6 h** — a 5.2× spread — because `k` is unreconciled (1.07 vs 1.8) and α was never
  measured. Three significant figures over a 5.2× band is precision the inputs do not have.
- §7 prices the live-console gap at "~30–45 maintainer-weeks". #496: "Any figure in
  maintainer-weeks for that work is currently unsourced."
- §2 states `R ~ L^-0.12` as an engine law. #496 §2.4 shows most of that tax was
  `SigSim.initSim` pre-posting the whole stimulus vector — a harness artifact, not physics.
- The whole document pins to `2d0ca9d`; #493 rules that citations be read at `8288226`.

A reader who obeys #484's own instruction ("read §7 before quoting §§2–6") ends up quoting
the least reliable text in the set. That is worse than no rescue.

## What was actually lost was the instrument, not the numbers

The genuinely irrecoverable assets are not prose. `jls.sim.KernelProbe`, `KernelProbe2` and
`jls.sim.Census` appear nowhere under `src/` or `test/` — every number in §2 and §7 is
**currently unreproducible at HEAD**. `riscv/build/k2000.jls`, the anchor circuit for all of
§2, is untracked (`riscv/.gitignore` line 1 is `build/`). #484 preserves the outputs of an
experiment while the experiment itself evaporates, which guarantees that the next
re-measurement disagrees with the record and nobody can say why.

## Alternative 1 (the main reframe): deterministic event-census goldens, not a prose record

The decisive observation #484 never makes: **almost nothing in its evidence base is
actually a timing measurement.** Event counts are deterministic functions of the simulator
— 2,331,793 fired, 388.4 ev/cycle, `RegisterFile` +6.94 vs mirrored `Memory` +18.00 vs
flip-flop farm +114.53 ev/cycle, 82.3% `PinChanged`. Only ns/event and cycles/s are
machine-dependent, and those are two numbers, not a corpus.

Deterministic quantities belong in a JUnit golden, not in an issue. The seam already
exists and needs no core change: `Simulator.afterEvent` is a `protected` no-op at
`src/jls/sim/Simulator.java:269`, already overridden by `BatchSimulator` at `:140` for trace
accumulation. A ~100-line `EngineCensusGoldenTest` over committed fixtures would pin the
element-cost table, the event mix and events/cycle exactly, with no benchmark flakiness and
no timing thresholds — and it would do so in the project's established idiom, where a
normative document is enforced by a test that cross-checks it (`ExtensionPointCatalogTest`,
`HelpTopicsTest`, `CliFlagTableTest`, `HeadlessCoreRatchetTest`). One
`docs/engine-calibration.md` then carries only the genuinely narrative parts (the two
machine-dependent constants with their hardware, the guest-side literature facts, the
quoting rules), amended by the `AMENDMENT.md` pattern the capability roadmap already uses.

That single move dissolves three of #484's "open contradictions" outright. The unexplained
2.02× TestGen-vs-`Clock` discrepancy is *two lines of a golden* measuring the same circuit
under both clocking regimes; α is a golden over the §11 two-cycle machine; the `k` = 1.07
vs 1.8 split is a division whose numerator and denominator both become test output. These
have sat unresolved as prose for a study's entire lifetime because prose cannot resolve them.

## Alternative 2: the behavioral tier does not have to live inside JLS

§6's parity contract is the best thing in this issue — RVFI's field list is exactly the
right comparison alphabet, and the permitted-to-differ list is correctly drawn from
industrial precedent. But it is spent on an in-JLS "virtual hardware" macro-element, and
that choice is what generates four of the five *fatal* rows in §8: no host I/O, no device
concept, a sealed `Element` hierarchy, no simulation-state serialization. JLS would be
building a mediocre RISC-V emulator inside a schematic editor.

`docs/grand-architecture.md` §9 already settled the general form of this question in the
other direction — "no in-house HDL simulation or parsing", orchestrate external tools over
a subprocess boundary (#61 Yosys, #62 ELK, #63 GHDL/Icarus). Apply the same rule here: the
behavioral tier is **mini-rv32ima or QEMU behind the existing subprocess seam**, emitting an
RVFI-shaped trace; JLS supplies only the structural tier and a trace comparator. The
contract in §6 survives unchanged — it was written to compare two models, and it does not
care that one of them is a different process. What disappears: the live-console problem
(the interactive console is the emulator's), the checkpoint format, the device concept in
the hot plane, and the sealed-hierarchy fight. `riscv/verify.py` and `fuzz_diff.py` already
prove this pattern works at whole-program granularity; RVFI is the same idea per-instruction.

## Alternative 3: the boot is a benchmark, not a deliverable

§7's own profile is the strongest argument in the whole corpus and it is buried: **47.7%
queue/dedup bookkeeping, 37.6% `BitSet` churn, 4.9% actual logic**, plus a zero-field record
allocated 1.92 M times per run and an O(n²) `SigSim` concat that is 95% of all allocation.
None of that needs Linux to justify it. A 2.26× semantics-preserving engine — explicitly
within #221's equivalence criterion, no new strategy, no ratification — makes *every
student's circuit* faster today, and it is the unavoidable prerequisite for the boot anyway.
#484's §11 already names the cheapest decisive experiment (a ~10-element two-cycle
unified-memory machine that measures α, CPI and `k` simultaneously). Do that plus the engine
work; the boot then either follows for free or is correctly abandoned on measured grounds.
Note also that RV32 nommu is on a published removal trajectory toward 2027 (§11) — betting
a multi-year programme's north star on it is strategically fragile in a way an engine
programme is not.

## Alternative 4 (small, high leverage)

15.87 bytes/word means a 2.4 MiB kernel is a 33 MB `.jls` and a 16 MiB RAM image alone
blows the 64 MiB load cap (`FileAbstractor.java:65`). Rather than treat that as a constraint
to route around, cut a different seam: an **out-of-line image reference** for `Memory`
initial contents (path + digest, resolved at `initSim`). That kills the boot-image problem
entirely and independently fixes save bloat for every student ROM — a win that exists with
or without Linux.

## Disposition

There are no acceptance criteria to disregard — #484 is a record, not a work item — so the
judgement is about where the record should live. Concretely: close #484 in favour of
#496/#494/#495 as the interim rescue (they are strictly better), and land one tracked
`docs/engine-calibration.md` plus the probes (`KernelProbe`, `Census`, the four element-cost
fixtures) and a committed `k2000.jls` fixture under `test/`, before `riscv/` is deleted.
Keep §6 verbatim as the parity contract's field list; keep §1's word-level correction and
§10's prior-art survey, which are genuinely valuable and belong in the roadmap doc. Discard
§7 rather than re-home it: every one of its headline corrections is superseded or wrong.
