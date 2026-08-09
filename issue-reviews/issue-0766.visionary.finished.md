# Issue #766: TASK-C548-2: the curated set reaches ten circuits across combinational, sequential, FSM and datapath — and the RV32I showcase stops being unsurfaced
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: endorse-with-reframing

## What this issue is really for

This is the single highest-leverage artifact in CAP-27 (#511) and beyond it.
The deduplication comment on #548 records four surfaces built over this one
corpus — #551's SVG gallery, #573's in-browser demo, #552's build-along
lessons, #517's course kits — and says plainly that the corpus "must be
authored exactly once, here." Everything downstream inherits whatever shape is
chosen in this task. So the question is not "are ten circuits enough" but
"what shape of corpus makes four downstream surfaces cheap instead of
expensive."

Judged that way the *end* is unarguable — `resources/samples/` does not exist
(#381 O3), the tree holds exactly four `.jls` files and all four are fixtures
or generated artifacts, and #511's baseline verification is correct that a
switcher bounces in ten minutes. Endorse the corpus.

The *shape* is where I would reframe, and I say up front that I am
disregarding two of the stated acceptance bars: the count "at least ten" and
the four-category quota as the organizing principle.

## 1. A ladder, not a taxonomy

CAP-27 measures *time to a running, understood example*. AC-1 optimizes for
something else: breadth of a textbook taxonomy. Ten mutually independent
circuits give a newcomer ten front doors and no route; "combinational,
sequential, FSM, datapath" is a category system that serves a menu's section
headers, not a learner's next move.

The reframing costs one extra metadata field and changes everything
downstream:

> Order the corpus as a **ladder** in which each rung is built from the rung
> below it as a `SubCircuit`. Half-adder → 4-bit ripple adder → ALU slice →
> register file → controller FSM → single-cycle datapath → the RISC-V CPU at
> the summit. AC-3's category field gains a sibling: `builds_on: <id>`.

What that buys, none of it new work:

- **The four categories fall out for free.** A ladder that ends in a CPU
  necessarily crosses combinational, sequential, FSM and datapath. Coverage
  becomes a consequence rather than a target, and CAP-27 AC-2 still holds.
- **#552's lessons become nearly free.** Rung *n* is lesson *n*: "you built
  the adder; here is what it becomes." Authored over an unordered set, those
  lessons have to invent their own progression and will silently fork one.
- **It demonstrates the thing JLS is actually best at.** `riscv/README.md`
  states the project's own self-understanding: an ordinary circuit of ordinary
  elements is "exactly what JLS is *for* — teaching computer architecture by
  drawing datapaths." Subcircuit composition is that claim made visible; ten
  flat circuits hide it.
- **The RV32I showcase stops being an outlier.** Under AC-1 it is a tenth item
  bolted onto a list of nine toys, which is precisely why it reads as
  "unsurfaced" today. As the ladder's summit it is the payoff the other rungs
  earn.

One honest caveat I verified rather than assumed: `docs/file-format.md`
§7/§321 shows `SubCircuit` bodies are **nested `CIRCUIT` blocks embedded in
the file**, not external path references. Good news — a ladder has no
classpath-resolution hazard, unlike an import-by-filename design. Bad news —
each rung carries a *copy* of the rungs below, so a fix to the adder does not
propagate upward. That wants exactly this project's idiom: a test asserting
each rung's embedded block matches the standalone sample it names, in both
directions, the `ExtensionPointCatalogTest` / `HelpTopicsTest` completeness
shape. Cheap, and it makes the ladder a structural fact rather than a
convention.

## 2. The deliverable should be triples, not files

AC-2 asks that every circuit "loads … and simulates." That is a liveness
check: a circuit computing garbage passes it, and so does one whose memory
contents silently dropped. Meanwhile JLS's genuinely strongest asset —
`docs/batch-interface.md`'s stability contract, the `-t` grammar, VCD export,
the `ghcr.io/anadon/jls` autograder image, `AutogradeBridgeExampleTest`,
`BatchSimulationGoldenTest` — sits one inch away and is not used.

> Ship each sample as a **triple**: the circuit, a `-t` stimulus vector, and
> the expected watched-output transcript.

Same authoring effort (you cannot author a demonstration circuit without
deciding what to poke), four extra consumers:

- AC-2 upgrades from liveness to a correctness golden, in the shape
  `BatchSimulationGoldenTest` already establishes.
- #768's "suggested exercise" acquires something to *check* — a student's edit
  can be run against the shipped vector. That is the seed of self-checking
  exercises and of #517's course kits, and it is unreachable if the corpus is
  ten bare files.
- #573's pokeable browser demo needs a default stimulus; here it is.
- #551's gallery can render a waveform beside the SVG instead of a still life.
- The suite gains regression coverage over a far wider element mix than the
  three fixtures it has now.

This is the different seam to cut along. "Ten circuits" is a folder; "ten
demonstrated behaviors" is a corpus.

## 3. `resources/samples/` and `test/fixtures/` should not be two corpora

AC-4's licensing provenance and #73's fresh-authorship rule are right and
should stand — the 2014 MTU letter (`pop_GPLv3.pdf`) names specific authors,
and importing upstream 4.1 sample circuits would be a real hazard.

But "reuse an existing test fixture only where one already fits" points the
wrong way. It treats fixtures as the reservoir and samples as the derivative.
After this lands the tree has two circuit corpora under different rules,
different provenance discipline, and different tests — and the far larger,
better-exercised one is invisible to users. Invert it: **samples are
fixtures.** Author into `resources/samples/`, and let tests that need a
realistic circuit read from there. The corpus then gets exercised by the whole
suite continuously, and the licensing rule has one home instead of two.

## 4. The RV32I decision the issue hides

"The RV32I showcase" is singular, and the repository has two:

- `test/fixtures/riscv-sum1to10.jls` — 120 KB, plain-text `FORMAT 1`, the full
  single-cycle CPU **generated** by `riscv/make_cpu.py` / `build_cpu.py` from
  `riscv/examples/sum1to10.s`, pinned by `RiscvCpuGoldenTest` (34 clocked
  cycles, "multi-thousand-event scheduling").
- `riscv/gui/cpu.jls` — 8.8 KB, an accumulator CPU built by Robot-driven GUI
  automation.

Both are generated artifacts. `pom.xml:148` bundles all of `resources/` into
the jar, so "copy it to `resources/samples/`" means minting a *third* copy of a
generated file, shipping 120 KB of it to every user, and adding a second
full-CPU batch simulation to `mvn verify` beside the golden that already runs
one.

Two better routes, in order:

1. **"Unsurfaced" is a discoverability claim, not a file-location claim.**
   What a newcomer needs in ten minutes is not a 120 KB CPU that does not fit
   on a screen — it is to *see that JLS goes there*. Put the CPU in #551's
   gallery via the `-i out.svg` export that already ships, link it from the
   README's shop window (PF-1), and give the Examples menu one entry that
   opens the small GUI-built CPU. The claim is discharged at a fraction of the
   weight, and the summit of the ladder is a circuit a person can actually
   read.
2. If a shipped copy is required, make `resources/samples/` its **only** home
   and repoint `RiscvCpuGoldenTest` at it (per item 3), with a regeneration
   check against `make_cpu.py`. One file, one generator, no drift.

Either way, do not simulate the full CPU twice per build.

## What I am disregarding, and why

I would strike "at least ten" as an acceptance bar and the four-category quota
as the organizing principle, and replace AC-1 with:

> An ordered ladder whose every rung is composed from the rung below, whose
> rungs collectively cover combinational, sequential, FSM and datapath, and
> whose summit is a RISC-V CPU — plus a small number of standalone showpieces
> (traffic-light FSM, seven-segment decoder, memory read/write).

"≥10" is CAP-27's proxy for "a library, not a demo." A coherent seven-rung
ladder plus three showpieces reads *more* like a library than ten disconnected
files, and it is the only version of this corpus in which #552's lessons,
#551's gallery and #517's kits are cheap. If the number matters to the
capstone's scorecard, the reframing still lands ten.

Keep AC-3 (categories as data, not menu code) and AC-4 (provenance) exactly as
written — both are right, and AC-3 is the field the ladder extends.
