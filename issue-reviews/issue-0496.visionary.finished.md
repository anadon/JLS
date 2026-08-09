# Issue #496: Machine calibration, part 1 of 2: re-homing preconditions, measured engine constants, element-cost table, boot-cost model (rescued from a branch that will be deleted)
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Stripped of its content, #496 makes a claim about what JLS should become: **a project whose
hard-won measurements outlive the directories they were taken in.** That claim is correct and
the urgency is real — `riscv/` is the only CPU-scale circuit JLS has ever measured, its
generator and its harnesses are inside the doomed tree, and `jls.sim.Census` / `KernelProbe`
do not exist in `src/jls/sim/` (I checked: seven files, none of them a probe). When the branch
goes, the *ability to re-measure* goes with it, not just the numbers.

What I do not endorse is the container, the scope, or the theory of durability. Pasting 659
lines of a normative, line-cited document into a GitHub issue — and splitting it in half
(#494) because an issue body could not hold it — is the project doing the opposite of what it
does everywhere else. README's Documentation section, ARCHITECTURE.md's "Recorded decisions",
`docs/simulation-semantics.md`, `docs/batch-interface.md`, `ExtensionPointCatalogTest`,
`HelpTopicsTest`'s link checker: JLS's whole arc is *normative content lives in the tree and is
guarded by a test*. An issue body is not diffable in a checkout, not greppable from `mvn
verify`, not reviewable as a PR, not covered by the doclint gate this very document complains
about, and — the tell — not stably line-citable, which is why the issue needs a hand-built
`:83 → §1.2` mapping table. The rescue reproduces, in its own mechanism, the exact failure mode
it was written to prevent.

## Grounding (verified at this checkout)

Every code claim I sampled is exact. `riscv/.gitignore` line 1 is `build/`; `riscv/build/`
does not exist and `git ls-files '*.jls'` returns exactly four files with `riscv/gui/cpu.jls`
among them. `RiscvCpuGoldenTest.java` cites `riscv/examples/sum1to10.s` and `riscv/README.md`
in `{@code}` spans, not `{@link}` — the doclint blind spot is real. `Simulator.java:25,27` is
`PriorityQueue` + `HashSet dupCheck`; `Simulator.java:269` is the empty `afterEvent` hook and
`BatchSimulator.java:140` overrides it. `HdlExporter.EXPORTED` lists 22 classes and neither
`RegisterFile` nor `FieldExtend` appears. `grep -c "RegisterFile\|FieldExtend"
docs/simulation-semantics.md` returns **0**. Both classes declare `propDelay`. This is careful,
honest work and I am arguing with its packaging, not its accuracy.

One grounding fact that reframes everything: **`docs/capability-roadmap/keystone-c-performance.md`
is on master, is 869 lines, and already carries 318 ns/event and 8,090 cycles/s at `:136`,
`:138`, `:655`.** The issue names it as the primary source that survives — and then declares
itself the later record where the two disagree. That ships a master document known to be
superseded by a GitHub issue, in a repository whose architecture doc opens with "everything
here describes HEAD ... verify rather than trust."

## Reframe 1 — a tag is the archive, and it makes the problem disappear

The issue's premise is "a link to a path or a commit on a deleted branch preserves nothing."
That is true of a branch and false of a **tag**. One command —
`git tag evidence/machine-calibration 36cbd37 && git push origin evidence/...` — makes the
commit a permanent ref that GitHub will not collect, at zero maintenance cost, and it preserves
*everything*: all 1,124 lines at their original line numbers, `parity-contract.md`,
`virtual-hardware-parity.md`, `bench_kernel.py`, `riscv_ref.py`, the harness code, and the
sibling documents #496 explicitly writes off as dying. Every `machine-calibration.md:NNN`
citation in #301/#335/#377/#295/#407/#413 then resolves *directly*, and the mapping table,
the two-part split, and the "what was dropped, and why" section all become unnecessary.

The deletion of `riscv/` from the working tree is about keeping master clean. It was never
about destroying history. Conflating the two is what generated ~1,800 lines of issue prose
across #496 and #494.

## Reframe 2 — fold the corrections into the surviving document, don't fork it

Whatever must be *normative* — the five quoting rules, the k/α unreconciled 1.68×, the
2.02× TestGen-vs-`Clock` discrepancy, the corrections to `keystone-c`'s own
`:140-141` figures — belongs as a PR against `docs/capability-roadmap/keystone-c-performance.md`,
which already survives and already owns the same measurement. One document, on master,
reviewable, with the corrections applied where readers will actually hit them. The rest is
archived by the tag from Reframe 1. Three parallel records of the same engine constant
(keystone-c on master, #484's §7 table, this issue) is not preservation; it is the beginning
of the drift the document's own quoting rules exist to police.

## Reframe 3 — the big one: event counts are not measurements, they are assertions

This is where the issue misses a far better route to its own goal. It treats ev/cycle,
Δev-per-element, and events-per-instruction as *measurements* needing hardware provenance,
dates, and best-of-8 reps. They are not. **Event counts are a deterministic function of the
circuit and the engine.** Only the ns figures and the JFR profile shares are machine-dependent.

Section 3 — the document's own "most reusable measurement in this study" — was produced by
subclassing `BatchSimulator` and overriding `afterEvent`, a seam that exists at HEAD and needs
no change to `jls.sim`. That is not a study. That is a JUnit test:

- `test/fixtures/` gains the four register-file fixtures and a `k2000`-class benchmark circuit
  (which §1.2 already demands be committed before deletion, for a different reason).
- `test/jls/sim/ElementEventCostTest.java` counts events via `afterEvent` and asserts the Δ
  table as a golden — 14.01 / 20.95 / 32.01 / 128.54, the depth-and-width flatness rows, the
  queue-depth curve.
- The §2.5 discrepancy that the document calls "the cheapest high-value experiment available"
  and then preserves *as an open question for all time* becomes two rows of the same golden:
  identical circuit, TestGen drive vs internal `Clock`, both counts asserted. The 2.02× is
  either reproduced and explained or it evaporates. Either way it stops being folklore.

That test is perhaps 200 lines and it retires most of §2, all of §3, and the load-bearing half
of §4.2's uncertainty — permanently, in CI, where a future engine change (the levelized pass
of #221 option 2/3) would break it loudly instead of silently invalidating a prose archive.
JLS already has this instinct everywhere else: `BatchSimulationGoldenTest`,
`SequentialGoldenTest`, `HeadlessCoreRatchetTest`, `NotificationRatchetTest`. The project's
answer to "how do we keep a fact true" has always been a ratchet test. This issue's answer is
a paste.

## Reframe 4 — separate the three PR-blocking items from the archive

§1.2 is the only content with a **deadline** ("preconditions, not follow-ups"), and burying it
under a `documentation` label inside a 659-line paste is the most likely way to lose it. Three
items there are live defects at HEAD, independent of whether any Linux machine is ever built,
and each deserves its own issue:

1. `riscv/build/k2000.jls` was never tracked. Regenerate and commit it as a fixture **in the
   deletion PR** or §2 becomes permanently unreproducible. This is a merge blocker, not a doc.
2. `RiscvCpuGoldenTest`'s `{@code}` citations to about-to-be-deleted paths, which `-Werror`
   doclint will not catch. A one-line fix plus a re-homed regeneration recipe.
3. `RegisterFile` and `FieldExtend`: absent from `HdlExporter.EXPORTED`, absent from
   `simulation-semantics.md` in both the delay table and the zero-delay set (0 grep hits), and
   both declare a `propDelay` that never fires. That last one directly contradicts
   ARCHITECTURE.md's #221 equivalence criterion, which binds any future strategy to "observably
   identical per-element propagation delays." Two shipped elements already violate the
   invariant a recorded decision rests on. That is worth an issue by itself and has nothing to
   do with booting Linux.

## What I am explicitly setting aside

**I am disregarding the premise that §4 must be preserved verbatim.** ARCHITECTURE.md records
the discrete-event interpreter as JLS's *sole* strategy (#221, 2026-07-26), with a revisit
trigger naming "a concrete CPU-scale design on the `riscv/` trajectory" — a trajectory being
deleted. Section 4 is a boot-cost model for an unbuilt machine, with two contradictory values
of `k` (1.68× apart), an `α` that was never measured because no multi-cycle JLS machine exists,
a behavioral row whose 12 ev/instr is modeled and whose only cross-check reuses the disputed
`k`, and a self-admitted 5.2× band on the answer. Preserving that verbatim preserves a
question, dressed as a record, downstream of #301's speculative roadmap.

The single highest-leverage line in the whole document is the one-sentence note that follows
it: **restate the #221 revisit trigger quantitatively** — `keystone-c`'s "below 10 kcycles/s on
the #202 golden's CPU" — so the trigger survives the directory. That is a one-line
ARCHITECTURE.md edit and it is worth more to the project's actual trajectory than the other 658
lines combined, because it is the only thing here that changes what a future maintainer *does*.

## Concrete alternative, end to end

1. Tag `36cbd37` as `evidence/machine-calibration`. Archive complete, line numbers intact,
   every sibling document preserved. Close #496 and #494 pointing at the tag.
2. One PR against `docs/capability-roadmap/keystone-c-performance.md`: fold in the corrections,
   the five quoting rules, the k/α disagreement, the `:140-141` retraction.
3. One PR: `test/fixtures/` benchmark circuit + `ElementEventCostTest` over the `afterEvent`
   seam, including the TestGen-vs-`Clock` rows that settle §2.5.
4. Three small issues from §1.2, one of which (the k2000 fixture) blocks the deletion PR.
5. One line in ARCHITECTURE.md re-anchoring #221's revisit trigger to a cycles/s threshold.

Same end. Roughly a tenth the prose, and none of it rots.
