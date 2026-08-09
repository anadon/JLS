# Issue #882: TASK-C367-1: a circuit may declare one physical time unit, recomputed from the integer tick every time — and declaring nothing saves byte-identically
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

Strip the re-tasking bookkeeping (#431 closed as a duplicate into #367, this is the
replacement child) and the claim is small and correct: **JLS's simulator has no unit,
its exporter asserts one anyway** (`src/jls/sim/BatchSimulator.java:423` writes
`$timescale 1 ns $end` unconditionally, while `docs/simulation-semantics.md` §1 says
time is dimensionless). Five capstones (#308, #303, #305, #309, #313) and a whole
column of `docs/capability-roadmap/README.md` P4 need a second before they can mean
anything: a sample rate is samples per second, a 0.16 Hz filter is a statement about
seconds, `t_PLH = 15 ns` is a datasheet line a student should be able to type.

That arc is real and this task points along it. My objection is not to the goal but to
**where the issue cuts the seam**: it treats the declaration as a change to the *file's
meaning* (AC-3, a global `FORMAT_VERSION` 2→3 that makes older readers refuse the file)
while simultaneously guaranteeing (AC-6) that **nothing in the simulation depends on
it** — delays stay integer ticks, only label text changes. Those two criteria cannot
both be load-bearing. One of them is wrong, and I think it is AC-3.

## Reframe 1 — a display annotation is not a must-understand section (disregarding AC-3)

The tree already wrote the test for this, in the document AC-7 proposes to amend:

> `docs/file-format.md` §9: *"Writers SHOULD therefore prefer a version bump over an
> 'ignorable' attribute whenever dropping the attribute would change **simulation
> behavior**."*

Apply it honestly. Under AC-6, a version-2 reader that drops `TIMEBASE` produces: the
same event queue, the same `long now` values, the same stdout, the same VCD timestamps,
and the same nominal `1 ns` header it writes today for *every* file. Nothing is
mis-simulated. The only loss is a label — and the label it falls back to is exactly the
one JLS has always shown. #367 §6's argument that an old reader "would misread every
duration in the file" is inherited from a world where delays are real-valued; #882
explicitly forecloses that world and then keeps the argument.

The cost of the bump is not zero. The README makes cross-reader interchange a stated
feature: the 4.6–4.10 fork lineage, JLS 4.1, `-savetext` for forks without an XZ
reader. `FORMAT 3` converts "your waveform axis says ticks instead of ns" into "this
file needs a newer JLS" — a hard refusal, on a lab machine, for a circuit the older
build could edit perfectly. That is a real regression in exchange for a guarantee no
user can observe.

**Alternative:** ship `TIMEBASE` as an optional non-semantic record with **no bump**,
and replace AC-3 with a stronger, testable invariant that a version integer only
proxies for:

> Loading the same fixture with and without a `TIMEBASE` record produces byte-identical
> batch stdout, byte-identical VCD *timestamps*, and an identical event trace. Only the
> `$timescale` line and rendered suffixes may differ.

That test is the actual promise. It is checkable on every commit, it cannot rot the way
a version number can, and it converts §9's SHOULD into a machine-enforced rule. The
first change that makes any simulated value depend on the declared base — a real-valued
delay, the analog `dt` (#351), an audio rate (#346) — is the change that bumps, and by
then #319's per-section must-understand flag likely exists to make the bump narrow.

This also dissolves the issue's own worst piece of coupling. #882's Ordering section
and Open Question 1 make the #319 interaction "blocks execution" of the version-policy
step and impose a mirrored REPLAN obligation on another feature as a
Definition-of-Done line. With no bump, **that entire negotiation evaporates**: nothing
to re-home, no two-mechanism hazard, no cross-issue mirror to keep honest. A 2–3
maintainer-week task shrinks toward one, and the part that shrinks is the part that
required a maintainer decision before an executor could start.

## Reframe 2 — the unit belongs to reuse, and reuse crosses files (extending AC-5)

AC-5 says "one timebase per file, owned by the top level", forbids `TIMEBASE` inside a
nested `CIRCUIT` block, and states the failure it forecloses: *"Two subcircuits
declaring different seconds for the same tick."* It forecloses that case in the one
place it cannot happen and misses the place it does.

JLS's unit of reuse is not the file. `SimpleEditor.doImport` (`:5463`) imports a
subcircuit **from another open editor window** — i.e. from another `.jls` file — and
`SubCircuit.copy` (`src/jls/elem/SubCircuit.java:337`) deep-copies its elements,
including their integer `delay` attributes, into the host circuit. `Circuit.save`'s
`subElement != null` arm (`:1479`) then writes that copy nested, with no header of its
own. So: circuit B is drawn at a 1 ps base with a gate delay of 15000 ticks (15 ns);
import B into A, which declares 1 ns; save. A's file now says that gate is 15000 ticks
= **15 microseconds**. No record was duplicated, no nested `TIMEBASE` appeared, AC-5's
assertion passes on its hierarchy fixture, and the design is silently wrong by 10³.

Two ways out, both better than the issue's rule:

1. **Rescale on import.** Delays are integers and the lattice is decimal, so importing
   into a *finer* base is an exact multiply; importing into a coarser one is the only
   lossy direction and is where a dialog belongs ("B is drawn in ps; A is in ns — adopt
   ps, or round?"). Adopting the finer of the two bases is the natural default and is
   exactly what a Verilog `timescale`-per-module compiler does.
2. **Nothing, because it is presentational.** Under Reframe 1 this hazard degrades from
   "wrong physics" to "wrong caption", which is another datum in favour of not making
   the declaration semantic until a semantic consumer exists.

Either way, AC-5 as written buys a guarantee against a case the format cannot produce
while the editor's copy path produces the real one. That is the wrong seam.

## Reframe 3 — take VCD's grammar, all of it (refining AC-1)

AC-1 fixes `TimeBase` as `(int mult, Unit unit)` "VCD's own `$timescale` grammar, so the
model and the exporter agree by construction." Good instinct, but the roadmap document
this task descends from specifies a *pair*: `docs/capability-roadmap/sweep-02-timing.md`
Change F says "a per-circuit `timescale` (unit + precision, e.g. `1ns / 1ps`)", and the
standards it unlocks — SDF `TIMESCALE`, Liberty `time_unit`, Verilog `` `timescale
1ns/1ps `` — all carry unit *and* precision. #882 silently narrows that to one number
and never says it is narrowing.

The narrowing is defensible today (with integer-tick delays, precision *is* the unit),
but it costs nothing to be forward-compatible and a lot to change a shipped grammar:
store `(displayUnit, precision)` with `precision` defaulting to `displayUnit`, accept
both `1ps` and `1ns/1ps` spellings from day one, and let `seconds(ticks)` use precision
while labels use the display unit. When structured delay (P4) lands and a student types
`15.7 ns`, the format needs no second change and `FileFormatSpecTest` needs no second
row. If the maintainer prefers the narrow form, that is fine — but it should be
**recorded as a deliberate divergence from Change F**, since a later reader will
otherwise read the two documents as agreeing when they do not.

## What I would keep exactly as written

- **The no-accumulation contract (AC-2)** is the best-designed thing in the issue. The
  two-sided test — `seconds(n) == seconds(1)*n` *and* the summed form provably differing
  at a stated scale — makes the invariant self-documenting and pins the number that
  justifies the rule. Keep it verbatim, including "if the chosen n makes them equal,
  raise n and record the scale."
- **Absent-is-default proven over the whole corpus (AC-4)**, including "if a golden
  moves, the fix is the condition — never the golden." That is the sentence that keeps
  this a zero-risk change.
- **`jls.core` as the home.** Its `package-info.java` already charters it as "types
  shared by the circuit model, the simulation engine, and persistence" with no GUI
  dependency, and `HeadlessCoreRatchetTest.BASELINE` is already `Set.of()`, so AC-1's
  "gains no BASELINE entry" is free. One gap: `ARCHITECTURE.md`'s module layout does not
  mention `jls.core` at all; a task adding its second non-geometry tenant should fix
  that line, and AC-7 does not list it.
- **The quantization figure (1.7 ppb at 1 ps for 44.1 kHz) documented rather than
  engineered away.** Documenting a real limit where a student will meet it is the
  project's house style and this is a good instance of it.

## Net judgement

This work strengthens the arc — it makes an existing dishonesty in the tree honest, and
it is the unit of measure five capstones are waiting on. It does not duplicate anything;
nothing named `TimeBase` or equivalent exists. But as specified it buys a file-format
epoch and a cross-feature version negotiation to protect a caption, and it defends
against a subcircuit-collision case the format cannot produce while the editor's import
path produces the real one.

Reframed: **declare the unit, prove by test that nothing simulated depends on it, do not
bump, handle import by adopting the finer base, and store the pair.** That version is
smaller, unblocks #682 identically, keeps interchange with the fork lineage intact, and
leaves the version bump available — earned, narrow, and probably per-section by then —
for the first change that actually changes what a circuit *does*.
