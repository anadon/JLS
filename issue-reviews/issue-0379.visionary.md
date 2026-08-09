# Issue #379: TASK-0023: the behavioral events-per-instruction constant and the levelized per-node cost stop being estimates, and every ns/node figure carries its node count and pass count
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is actually for

Stripped of apparatus, #379 asks for three things: (a) a real number for how much a
*non-structural* binding costs per retired instruction, (b) a real number for what a
levelized pass costs at CPU scale, (c) a guarantee that no speed figure escapes without
its denominators. (a) exists to price the fidelity toggle (#325). (b) exists to price
the engine programme (#362). (c) exists because this project's own history contains
agents quoting bare numbers off by 4.6x.

Those are good ends. The route chosen to reach them is, in all three cases, the most
expensive available one, and in case (a) it cannot reach the end at all. I am
disregarding the acceptance criteria in §14 wholesale; the reasoning follows.

## 1. The behavioral half measures a surrogate for a thing that does not exist yet

#325 (FEAT-031), the consumer this task exists to serve, says the quiet part in its own
§3 and §6:

> TASK-0023 consumes a landed behavioural binding to instrument it — it cannot run
> before one exists.
> Critical path: TASK-0065 → TASK-0066 → {TASK-0023, TASK-0079}

#379 declares `blocked_by: []` and names only TASK-0022 as a real prerequisite. It
resolves the contradiction by *building its own* behavioral machine: a ~200-line
accumulator element with a bespoke eight-opcode ISA, in `src/jls/elem/`, behind a new
sealed permit.

β measured on that machine is not the number #325 needs. β for a fidelity binding is a
property of *how the binding is implemented* — how a `Boundary` maps ports and settled
words at declared instants — over a *drawn definition*. A hand-written accumulator
element driven by an internal `Clock` shares nothing with that except the word "event".
The fidelity toggle's cost ratio has a numerator only once TASK-0065/0066 land; before
that, β is a measurement of a fixture invented to be measured.

Worse, the fixture is the shape #325 §2 explicitly rejected:

> *Add a `Cpu` element instead of a per-instance toggle.* Rejected: it costs a new sealed
> permit, a palette entry, a help page, an icon and a switch-exhaustiveness ripple, and
> it makes parity a property of a whole program rather than of a boundary.

#379 reintroduces exactly that element under a measurement banner, minus the palette
entry, plus a named exemption in the registry totality lint (TASK-0001/0002). One
feature's rejected alternative arriving as another task's apparatus is the clearest
signal in this issue that the seam is wrong.

## 2. The sealed hierarchy is telling the truth, and O4 mis-hears it

O4 treats `LogicElement`'s `permits` clause as friction to be honestly declared. Read it
the other way: the compiler is refusing a design, and it is right to. Two seams already
exist for exactly this and neither is considered:

- `SubCircuit` is **already** in the permits chain (#325 §1 makes this its central cheapness
  argument). A behavioral binding of a `SubCircuit` instance needs no new permit at all.
- `docs/extension-points.md:30` types `elem.element-provider` (`jls.elem.ElementType`,
  cardinality many, registered eagerly before any load). #78 shipped it; #212 stages
  external providers. A measurement element that must not be savable is a textbook
  in-process first-party contribution through `ExtensionRegistry` — the mechanism
  ARCHITECTURE.md's "Extension points" decision block was recorded to provide.

A measurement that requires one product-code permit entry, an update to
`SealedHierarchyTest`, and a named exemption in a totality lint is paying three
structural taxes to avoid noticing that it is on the wrong side of two boundaries the
project already drew.

## 3. The 1.39x "unreconciled" gap is legible in the surviving source

`docs/machine-calibration.md` does not exist on master (issue #493 §2: 195 branch-only
planning docs, unrecoverable). The surviving home,
`docs/capability-roadmap/keystone-c-performance.md`, prints the two figures under
*different headings*:

```
:474  full levelized pass over 522 nodes, plane arrays : 2.26 µs/pass  (4.32 ns/node)
:489  With an activity bitmap so unchanged cones are skipped:
:490  activity 100% (522 live) : 1.62 µs/pass
```

These are two kernels, not one pass reported twice: `Levelized.java`'s plain pass and its
activity-gated variant with all cones live. The "same 522-node pass, two costs, never
reconciled" framing is an artifact of the deleted document flattening two labelled rows.
H2 and D2 propose days of instrumented re-running to recover a distinction that survives
in the source at two adjacent line numbers. Whatever residual doubt remains is settled by
reading `Levelized.java` (§12 names it), not by a new harness.

## 4. The levelized half measures a model against a decision it cannot move

ARCHITECTURE.md's recorded decision (#221) is that the event interpreter is the **sole**
strategy, with a revisit trigger of "a concrete CPU-scale design on the `riscv/`
trajectory that is unusably slow interactively." keystone-c §2 and
`lf-02-compiled-evaluation.md:19` both say the same thing about that trigger: it is not a
testable condition, and it was recorded without a number.

Re-running `Levelized.java` at 1,346 and 1,400 slots does not move it. Nothing about a
synthetic array benchmark at 2.6x working set is "a concrete design on the `riscv/`
trajectory"; it is a model of an engine whose *design document already exists*
(`lf-02-compiled-evaluation.md`, 756 lines, including the elaboration seam JLS lacks at
`Simulator.initSimulation:180-202`). H3 answered either way changes no decision anyone
is authorized to take.

## 5. The route I would take instead

**A1 — make #221's revisit trigger testable. This is the real deliverable.** keystone-c
already asks for it in prose: restate the trigger as a threshold ("below N kcycles/s on
the #202 golden's CPU"). Land an in-tree `@Tag("slow")` benchmark over the *existing*
`test/fixtures/riscv-sum1to10.jls` and `riscv/build/k2000.jls` fixtures — which
`RiscvCpuGoldenTest` already loads and simulates — that reports cycles/s with its census,
and amend the ARCHITECTURE.md block. That is one afternoon of harness plus one decision
block, it uses the real engine on the real design, and it is the only artifact in this
neighborhood that can legitimately open or close the engine programme. Note the trap
keystone-c §2 flags: `SigSim.initSim`'s O(n²) `String +=` dominates end-to-end wall time,
so this benchmark must separate elaboration from loop, and fixing that string bug is
worth more real cycles/s than the entire levelized study.

**A2 — make denominators structural instead of prosaic.** Requirement (c) is the most
durable thing in this issue and it is not a measurement problem. Three quoting rules in a
markdown file are the same class of control that failed twice already. The elegant form:
one machine-readable calibration table (`docs/calibration.yaml`) with one row per figure —
value, unit, node count, pass count, clocking regime, census, fixture, commit — and a test
that (i) fails if any roadmap document contains a `ns/node`, `ns/event` or `cycles/s`
token that is not a reference to a row id, and (ii) fails if any row lacks a required
field. Quoting a bare number then becomes impossible rather than forbidden, and #493's
whole failure mode — citations into documents that evaporate — gets a single durable home.
This is a lint, not a measurement, and it can land today with the *existing* keystone-c
numbers as its first rows.

**A3 — if a levelized number at scale is genuinely wanted, prototype the elaborator, not
a bigger array.** `lf-02` §2.2 already specifies Kahn levelization over the combinational
graph with sequential cuts, and lists two free by-products the roadmap wants anyway
(combinational-loop detection for sweep-02's ERC gap, and the timing DAG P4's STA needs).
A levelizer that runs over a real `Circuit` and emits `order[]`/`levelStart[]` is a better
week than a synthetic benchmark: it produces a number on the real netlist, it produces two
capabilities with independent value, and it cannot silently model the wrong shape.

**A4 — for β, wait.** Take the disposition #325 §7 already prescribes for a soft
refutation: if the behavioral binding turns out not to be materially faster, the boundary
discipline survives and the arithmetic is re-derived. That is a note on #325, not 1.5
maintainer-weeks of surrogate.

## 6. What survives from the issue as written

- P3 (verify the machine computes correctly before quoting its cost) is a good discipline
  and should be a general rule for any future binding measurement.
- The clocking-regime warning (O3: internal `Clock`, never a `-t` vector, because
  `SigSim.initSim` pre-posts every transition) is a real and non-obvious hazard; it belongs
  in the calibration table's required `regime` field, where it binds everything rather than
  one task.
- The `stackdepth=512` note and the warm-versus-including-init separation are already in
  keystone-c §12; A2 is where they stop being folklore.

## Verdict

**redirect.** The end is sound; every route in the issue is wrong. The behavioral half
cannot measure what its consumer needs and reintroduces an architecture #325 rejected by
name; the levelized half re-measures a model against a decision it cannot move, to resolve
a contradiction that is already legible in the surviving source; and the one genuinely
durable requirement — denominators travel with figures — is a data-and-lint problem being
solved as a measurement task. Point the effort at A1 (quantify #221's trigger on the real
riscv fixture) and A2 (the calibration table plus its lint). Both are smaller than this
issue, both land now, and between them they leave #379 with nothing left to do.
