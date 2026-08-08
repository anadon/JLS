# Issue #311: CAP-16: a decade of Logisim-Evolution course material opens in JLS as working, readable circuits with every loss named
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this capstone is actually for

Of the nineteen capstones, this is the only one whose success is measured in
*people who switch*. Everything else in the plan makes JLS better for people who
already use it. That makes #311 the highest-leverage item on the board, and it
means the correct optimization target is **adoption per maintainer-week**, not
parity per acceptance criterion. Judged against that target, the goal is right
and the acceptance criteria are assembled to prove parity rather than to deliver
migration. I endorse the outcome; I am disregarding AC-3 and AC-4 as written and
proposing a different seam, a different oracle and a different first increment.

## 1. The required set omits the one prerequisite that decides §1 step 2

§1 step 2 and AC-3 say: replay Logisim's own vectors, "outputs compared exactly,
pass." JLS cannot express the inputs, let alone match the outputs.

- Logisim-Evolution's test-vector format supports **don't-cares, high-Z and
  sequential mode** — this repo already records it as "the concrete catch-up bar
  for JLS's batch mode" (`docs/hdl-support-research.md:186-189`).
- JLS's `-t` grammar admits **integers only** — no `x`, no `z`, no `-`
  (`docs/batch-interface.md` §2.2 and its `value` production; also
  `docs/capability-roadmap/sweep-01-values-and-logic.md:63`).
- JLS's value domain is two states plus **whole-signal** HiZ (`ARCHITECTURE.md`
  line 362; `docs/batch-interface.md:298-303` — "JLS has no per-bit HiZ").
- FEAT-053 #369 does not touch this. Read its §1 and §4: expectations file,
  verdict trichotomy, reports, and invariant 1 is "the `-t` grammar is literally
  untouched." It adds a verdict over a value domain that cannot hold Logisim's
  vectors.

The work that *would* close this is the value-domain keystone —
`docs/capability-roadmap/keystone-b-migration.md`, **17–22 maintainer-weeks** —
and it appears nowhere in `requires_features`, `blocked_by` or the Cost table.
§3 risk 6 gestures at "a semantic boundary the vectors do not describe" and
hands it to a prose write-up. It is not a write-up; it is a hard, priced,
already-analysed dependency. Worse, it bites even on all-integer vectors: a
Logisim circuit with an uninitialised register reads `U`, JLS reads 0, and the
comparison is a false FAIL with no way to say "unknown."

**Consequence.** Either (a) add the four-state core to the required set and
accept a ~50–70 mw capstone, or (b) — my recommendation — **drop AC-3 and
retarget step 2**: replay vectors *JLS generates* through both tools and compare
only on the sublattice both can express (fully-driven, fully-defined vectors),
reporting every vector that falls outside it as `INCOMPARABLE`, never as a pass
or a fail. That is an honest parity claim, it is buildable today, and it is
exactly FEAT-053's own "'I don't know' is never a pass" lattice applied one
level up.

## 2. AC-4's refusal policy refuses the median sequential lab

The rule "never map by name onto different semantics" is correct. AC-4 converts
it into "**refuse** the file containing Logisim's sequential shift register."
Combine that with FEAT-025 invariant 1 — "no partial circuit is ever emitted" —
and the first sequential-logic lab in the corpus imports as nothing at all. A
shift register is not an exotic construct in a digital-logic course; it is week
six.

**Reframe: map to generated sub-circuits, not to elements.** The construct map's
codomain should be *JLS circuit fragments built from primitives*, not the 35 rows
of `ElementRegistry`. Logisim's sequential shift register becomes a generated
subcircuit of `Register` + `Mux`, named `Logisim_ShiftRegister`, marked
`approximated` in the report with its expansion documented. This:

- satisfies the actual invariant (semantics, not name, decide the mapping);
- collapses the totality problem — a map into fragments is far easier to make
  total than a map into a fixed element set, which is §3 risk 1's real content;
- shrinks "refused" to constructs with no digital-logic meaning (TTY, board I/O,
  hex displays), which is where refusal genuinely belongs;
- is *pedagogically better than the source tool*: the student can open the
  subcircuit and see how a shift register is built. This repo's own survey notes
  Logisim-Evolution's "pedagogical framing is thin"
  (`docs/hdl-support-research.md:192-195`). Migration is the moment to be better,
  not merely equal.

Keep AC-4 as a *test of the rule* (a name collision must never silently become a
1:1 element mapping); stop asserting that the only compliant outcome is refusal.

## 3. The seam is already in the tree, and it is not `NetlistImporter.Builder`

#323 §3 says the importer should build "through programmatic construction verbs,"
grades that op layer *beneficial* (FEAT-015 #337), and then makes Open Question 4
"how is `NetlistImporter.Builder` promoted?" — a private save-text emitter
(`src/jls/hdl/imp/NetlistImporter.java:410`).

`src/jls/collab/op/` **ships now**: `AddElements`, `AddWire`, `SetElementConfig`,
`RotateElement`, `AttachProbe`, applied through `OpSink` with
validate-before-mutate, invertibility, stable-id addressing and a strict reader
(`docs/operation-layer.md`). Build the importer as *an op stream* and you get,
free: undoable import, import-into-an-existing-circuit, a machine-readable
description of exactly what the importer did (the migration report and the op log
are the same artifact), and no second construction path to keep in sync. Open
Question 4 dissolves — don't promote the private builder, retire the question.

Second seam point: CAP-29 #513 books "the format-agnostic generalisation of the
loss-report infrastructure" as *later* work, after `.circ`. That is the classic
build-two-then-abstract move, and it is wrong here because the abstraction is
knowable in advance and costs almost nothing at design time: `parse → foreign
construct model → registry-keyed fragment map → op stream → LossReport`, with
`.circ` as the first driver. Fix the report schema (construct, disposition,
location, reason, version field) once, now — CAP-29 already declares a dependency
on it.

## 4. Use the incumbent as the oracle — and as a day-one bridge

#323 §5 I1 proposes comparing the imported net partition "against the source's own
computed nets." Those nets are computed by the geometry replication under test.
That is a circular oracle, and it cannot catch KC-16-2 (silent disconnection),
which is the failure mode the whole capstone fears most.

The independent oracle exists and is free: **Logisim-Evolution itself**, headless.
It ships `--test-vector`, `--tty` with csv/table output, and HDL generation
(`--test-fpga … HDLONLY`) — recorded in `docs/hdl-support-research.md:186-189`.
Two uses:

- **Oracle:** for each corpus file, generate vectors, run both tools headless,
  diff. This replaces AC-3's dependence on the rare course file that ships
  vectors with a generator that works on all 30+, and it is the only construction
  that actually detects silent disconnection.
- **Bridge:** Logisim HDL export → Yosys → JSON netlist → **the netlist importer
  that already ships** (`src/jls/hdl/imp/NetlistImporter.java`, wired to
  `HeuristicLayeredLayouter` at `:104`). This is the pipeline FEAT-019 #321 and
  FEAT-020 #320 are building anyway. It delivers a working migration path for the
  synthesisable subset with **zero new XML parsing**, and it measures — before a
  reader exists — what fraction of a real corpus is even synthesisable. That is a
  better two-day measurement than an accept/reject table over a map nobody has
  written, and it gives instructors something during the 12–18 weeks the native
  reader takes.

The bridge is not the endgame (it loses schematic readability, probes, memories,
non-synthesisable annotation) — but "not the endgame" is not "not worth
shipping" when the alternative is nothing for a year.

## 5. Two dependencies that should not be on this path

- **#349 (74-series / DIP part data) gating #323 is spurious.** `.circ` has no
  through-hole packages; that data belongs to the breadboard/PCB capstones (#298,
  #307). This edge puts a transcription-plus-licence-audit exercise on the
  critical path of a course migration, and it is the same edge that drags the
  GPLv3 "or later" question into the reader's schedule. Recommend a REPLAN
  withdrawing it from #323's `blocked_by` (both mirrors), which also removes
  #349 from this capstone's `blocked_by`.
- **Geometry: ship δ as a measured data table, not transcribed code.** The
  port-offset rules are *observable behaviour*. Derive them per (kind, input
  count, body size) by fitting against nets Logisim itself reports on the corpus,
  commit the table as versioned data with a generator harness, and the licence
  question in Open Questions 1–2 mostly evaporates — no source is absorbed, and
  the table is validated rather than trusted. Note the project has already lived
  through this exact ambiguity from the other side: `README.md:352-359` records
  that JLS's own provenance letter names GPLv3 without "or later," and the
  or-later election was a maintainer's judgment call. That precedent argues for
  asking upstream (OQ2) and for not blocking anything on the answer.

## 6. One thing R-1 is right about, at a fraction of its price

R-1 was removed because it spans beneficial FEAT-003. But the useful half is
cheap and belongs *inside* FEAT-025: **the importer must be deterministic and
idempotent** — same `.circ` in, byte-identical `.jls` out; re-import after an
upstream edit produces a minimal diff. With stable ids (`Element.java`, #165)
and an op-stream importer this is nearly free, and it is what makes a *transition
semester* possible — both tools alive, `.circ` still in git, JLS regenerated.
The one-way-door decision (OQ4) is right; a one-way door you can walk through
repeatedly is a different and much better thing than one you can walk through
once.

## What I would do instead, in order

1. Two weeks: the Logisim→HDL→Yosys→`NetlistImporter` bridge over a 30-file
   corpus. Publishes real coverage numbers and ships a usable path immediately.
2. Fix the loss-report schema and the foreign-construct pipeline shape once
   (shared with CAP-29 #513 from day one).
3. Native `.circ` reader as an **op stream**, geometry as a fitted data table,
   Logisim-as-oracle for connectivity; constructs map to generated subcircuits
   with expansions named in the report.
4. Retarget step 2 to the comparable-vector sublattice; open a separate,
   honestly-priced decision about the four-state value domain.

## Endorsed unchanged

The loss report as a batch artifact rather than a modal dialog (OQ6). XXE
hardening as ship-blocking (AC-5) — it is cheap and this is the first XML parse
in shipped code. AC-2's falsification transcript. Refusing to reconcile the two
cost estimates. The instinct behind KC-16-2. And, above all, the framing in the
abstract: *this is the one capstone that moves users*. Nothing in this review
disputes that it is the most valuable thing in the plan; the argument is only
that it is currently scoped to prove parity to a reviewer instead of to get an
instructor's labs open before the decade is out.
