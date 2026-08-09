# Issue #607: TASK-C486-3: `jls -check` says whether a net is still a wire — l_crit = v*t_r/k per opted-in net, and "not assessable" everywhere else, including every shipped example
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this task is really for

The end, stated by CAP-18 (#313) and by sweep-06, is that **JLS should stop
silently lying about wires**. `docs/simulation-semantics.md:186-189` is the lie,
written down honestly: *"**Wires are ideal**: propagation across a net takes zero
simulation time and the whole net carries one value."* What the spec never says
is the *domain over which that is true*. This task is the executable form of that
missing sentence, and the insight it carries — the regime is entered by **edge
rate, not clock rate** — is the single best pedagogic idea anywhere in the
high-frequency programme. The direction is right and I endorse it.

The mechanism as written, though, is vacuous by construction, hardwires one rule
into a permanent CLI surface, and buries its cheapest and most permanent
deliverable behind three unlanded features (#367, #319, #336). Four reframings
below. I explicitly disregard **AC-2** (the `examples/` noise measurement) and
**AC-4** (the default-velocity print), and restructure **AC-1**'s output; reasons
attached to each.

## 1. The two-attribute AND-gate points the diagnostic at the wrong student

The named audience is "students whose circuit works in the simulator and fails on
the breadboard." By construction those students do not know edge rates exist —
that is *why* the lab failed. They will never type `length = 150 mm` and
`t_r = 2 ns` onto a net. A net reports a verdict only when someone who already
understands transmission lines has annotated it. The lint therefore reaches
exactly the population that does not need it, and answers everyone else with
"not assessable", which is silence wearing the costume of rigour.

**Reframing: invert the solve.** `l_crit = v·t_r/k` has three variables; the task
demands two and reports nothing otherwise. But *one* already yields a teaching
statement, and the report should print the **threshold, not the verdict**:

- edge rate alone (which arrives free — see §2): *"your fastest driver stops
  being lumped past 48 mm on FR-4, 70 mm on a breadboard — go measure your
  longest jumper."*
- declared length alone: *"this net stops being a wire for edges faster than
  700 ps. A 74LS part is 18 ns; a modern FPGA output is 200 ps."*
- both: today's sentence, unchanged.

This is strictly more information at strictly less cost, it is the same
arithmetic, and it makes the command say something true and useful on
`riscv/gui/cpu.jls` — 1038 elements, the repository's flagship design — **on the
day it lands**, instead of printing "not assessable" 1038 times. It also removes
the need for the "never PASS" hedge: a threshold is not a pass, so there is
nothing to mistake for one.

## 2. The attributes belong to a technology and a medium, not to nets and arcs

Look at the task's own worked values: 20 ps, 2 ns (74AC), 18 ns (74LS); FR-4
1.446e8 m/s, breadboard 0.7c. **Every one of these is a family constant or a
medium constant.** Nothing about them is per-net or per-arc. Yet TASK-C486-1
(#603) puts `t_r` on every delay arc and TASK-C486-2 (#605) puts a length on
`WireNet` — the most-instantiated object in the model — and then #607 has to
invent a default-velocity policy (AC-4) because the medium was never declared
anywhere.

**Reframing: one design-level declaration.** `TECHNOLOGY 74AC` and `MEDIUM
breadboard` (or FR-4-stripline), declared once per circuit. Every driver inherits
`t_r`; every net inherits `v`. Consequences:

- AC-4 dissolves. There is no invisible default to print, because the medium is
  a declared fact printed once in the report header rather than guessed per net.
- The format change shrinks from a per-net optional section on `WireNet` to a
  single circuit-level record, which is a materially smaller bet against #319
  and #367.
- It lands on the seam the roadmap already identified as the real gap:
  `docs/capability-roadmap/sweep-02-timing.md:110` (*"JLS has one integer, no
  slew, no load, no fanout awareness"*) and sweep-06's unlock **D, a
  technology-cell layer**, which #87 Liberty needs anyway.
  `docs/simulation-semantics.md:264-288` is already a table of unitless per-class
  delay constants with no technology attached — that table is where a technology
  declaration wants to live, and attaching one there serves #87/#89/#93 rather
  than forking a parallel mechanism they later have to absorb.
- CAP-04's breadboard (#297) then gets its 2.1x-critical-length verdict *by
  declaring it is a breadboard*, which is the one fact a student in that lab
  actually knows.

## 3. `-check` should be a check *framework*; electrical length is rule #1

The task freezes a hand-composed English sentence and commits it as a golden on a
new corpus. The CLI is a documented stability contract (`JLSStart.FLAGS`, 15
flags, exit codes 0/1/2, `CliFlagTableTest`, `docs/batch-interface.md`), so what
this actually mints is a permanent surface whose contract is *one rule's prose*.

Nothing in the tree lints anything today: `grep -niE '\blint\b|design rule|drc'`
over `src/` and `test/` returns zero hits. But the trajectory is full of siblings
that all want the same shape — a static, per-design report over the netlist IR:
unconnected inputs; multi-driver contention (the `issue #98, S1` ordering comment
sits in `WireNet.java:19-20`); tri-state without enable; clock-domain crossing
(#327); fanout and load (sweep-02); the breadboard consistency check named in
#336's own Abstract; and HDL export's coverage refusal, which today *throws*
(`HdlExporter.java:190`) where it should *report*.

**Reframing: the permanent surface is a finding record**, not a sentence — rule
id, severity, anchor (stable net or element id), computed values, message — with
a stable machine-readable serialization and the prose as a rendering of it. Then:

- the golden pins a record, not a wording, and the wording stays free to improve;
- autograders consume it directly (`examples/autograde/autograde.py` already
  demonstrates the appetite, and `docs/vcd-interop.md` the pattern);
- **it discharges #336's IC-6 and its Open Question 4**, which asks for "a second
  in-tree consumer of `jls.netlist` that is not an HDL emitter" and names "a new
  headless net-report command" as a candidate. That is precisely this command.
  Built as one hardwired rule, it forfeits that; built as a framework, this task
  pays a debt on its own prerequisite.

## 4. The falsification guard is measured over an empty set — and that is a finding about the project

AC-2 is the design's load-bearing guard: *"a corpus-wide test over the shipped
`examples/` prints that verdict on every circuit and no other verdict."* The
shipped `examples/` tree is one Python file:

```
$ ls examples/            → autograde
$ ls examples/autograde/  → autograde.py
$ find . -name '*.jls' -not -path './.git/*' | wc -l   → 4
```

Three of the four are `test/fixtures/`; the fourth is `riscv/gui/cpu.jls`. **The
corpus AC-2, CAP-18's AC-7 and K18-3 are all organised around does not exist**,
so the guard is vacuously satisfiable and would stay green against `return;`. I
disregard AC-2 as written.

Visionarily this is worth more than the defect it names. README, the batch
contract, the help tree, HDL export coverage, student onboarding and this lint
all speak as though JLS ships example circuits, and it ships none. A curated
`examples/` corpus is a small, unblocked deliverable that serves six consumers at
once and is a *prerequisite* for the noise measurement, not a presumption of it.
If one recommendation from this review survives, make it that one.

## 5. Land the sentence before the software

AC-5 correctly refuses to touch `WireNet.propagate`, which honours
ARCHITECTURE.md's recorded rule that anything altering what a simulation *means*
is a documented change to `docs/simulation-semantics.md` first. Follow that rule
to its conclusion: the most permanent artefact in this entire rung is **one
normative paragraph beside §6.1's "Wires are ideal"** — *JLS models every net as
equipotential; that model holds while l < v·t_r/k; JLS does not check this unless
you declare l and t_r.* It costs an hour, is true of every design that has ever
opened in JLS, needs no attribute, no format bump, and **no dependency on #367,
#319 or #336** — while the lint waits on three unlanded features. It also
converts the lint from a floating diagnostic into the executable form of a
documented claim, which is how every other contract in this repository is
structured.

## 6. One ordering hazard worth recording

AC-3 commits the report as a golden keyed on #336's stable net names. #336 §6
records the opposite as a **hard gate (W0.3)**: *"Must land before any new
goldens are generated,"* precisely to avoid regenerating corpora twice. A golden
frozen in this task before #336's naming epoch is settled is a corpus that gate
exists to prevent. Prefer assertions over computed values plus a schema check,
and freeze the golden only after the naming convention's epoch is published.

## Summary

The arc is right, the refusal to re-model is right, and "edge rate, not clock
rate" is worth building around. Build it as: a normative paragraph now; a
technology/medium declaration instead of per-net and per-arc attributes; a
threshold report instead of a two-sided verdict with a vacuity hedge; a finding
record instead of a frozen sentence; and an actual `examples/` corpus before any
claim is made about noise over it.
