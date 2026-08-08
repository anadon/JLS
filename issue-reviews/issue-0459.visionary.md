# Issue #459: TASK-0083: the balanced-ternary machine exists as drawn boundaries against a stated census, with the ternary ALU and the three-way branch green against the emulator
- Lens: visionary
- Date: 2026-08-08
- Reviewer: Claude Opus 5
- Verdict: redirect

## What this issue is really for

Three different goods are fused into one deliverable, and they have different natural shapes:

1. **A teaching artifact.** #295 wants a student to "see that a computer does not have to be
   binary, and to step through one that is not, in the same editor they draw adders in."
2. **A completeness gate.** #361 is explicit that TASK-0083 is in its roster *only* as "the
   element census that proves the family is complete rather than merely present" — the drawn
   CPU is the acceptance test for the N-ary element family, "the only one that will find its
   holes."
3. **A calibration fixture.** §9 offers the measured events/instruction as "the first data
   points for α on a real multi-cycle JLS machine" (α is `never measured` per O7), and the
   Abstract offers the machine to #232 as a hot-path fixture.

The issue optimizes for none of these. It optimizes for a fourth thing: a census document with
a test that reads it.

## The contradiction that decides the verdict

#345 Open Question 1 — "Native radix 3, or binary-encoded ternary? **This is the maintainer's
choice and it must be made before TASK-0083 starts, not inside it**" — is answered inside
TASK-0083, in the direction that voids purpose (2). H1 states the answer plainly: a 16-trit bus
*is* a 32-bit bus, `Memory`/`RegisterFile`/`Splitter`/`Binder`/`Mux`/gates carry it "with zero
change and zero format version," §7.1 adds "no element type," and the TASK-0061 fallback draws
the ALU cells as "small binary subcircuits over the two BET planes."

Follow that through. A machine that adds no element type, moves no format version, and uses
only elements that exist on master today is **not a consumer of the N-ary element family**. It
will find none of its holes. It is a 32-bit binary circuit whose ALU implements an unusual
arithmetic, with "ternary" living in the labels. Nothing in the running system knows a trit
from a nibble: the trace shows 32 bits, `Display` shows radix 2/10/16 (`ElementRegistry` has 35
types on master, all radix-2), the VCD has no manifest, `-t` has no balanced literals. #295's
audience promise — "types `TRIT -5`, gets `-++`" — is delivered by #344/#361, not by this.

So #459 as written is, unintentionally, **the strongest available argument that #361 is not
needed**. That is a genuinely interesting result and it is buried in a fallback clause.

## Alignment with the project's larger arc

`docs/grand-architecture.md` claims to be the determination of "the single most correct target
architecture," and claims "**all ~46 open issues map cleanly onto six layers over one enabling
triad** ... with no leftovers." Grep it, and the 18 documents under `docs/capability-roadmap/`,
for ternary / N-ary / radix-as-value-domain: **zero hits.** The three funded trajectories it
names are CPU-scale teaching (`riscv/`), the FPGA bridge, and collaboration; the critical path
is #77 core / #78 registry / #167 ops. #167 has shipped (`src/jls/collab/op/`, 21 files,
`AddElements`/`AddWire`/`SetElementConfig`), #78 has shipped, #77 has not.

The radix programme — #344 → #361 → #345 → #459, under a capstone (#295) requiring **seventeen
features** — is a fourth trajectory that the project's own architecture document does not own,
has not costed against the other three, and does not mention. #459 is a leaf of that unowned
tree. Before another task-tier issue in this programme is executed, `grand-architecture.md`
owes an amendment that either adopts the radix trajectory as a peer of the other three (with
the leverage argument against #77 made explicitly) or records it as speculative. Executing
leaves of an unowned tree is how a single-maintainer project acquires a second architecture.

## The alternative I would build instead

**Invert the dependency: make the cheap machine the demand evidence that decides the expensive
family.** The project already has this exact pattern recorded — #212 external providers are
"gated behind demonstrated demand," and `grand-architecture.md` §9 forbids "plugin execution
surface ahead of demand." Apply it here:

- Build the BET datapath **now**, on master, with no dependency on #344, #361, TASK-0061 or a
  format bump — the issue's own H1/O6 prove this is possible today. It lands as an
  `examples/`-tier artifact plus a bring-up harness, not as a capstone prerequisite.
- Make the *deliverable* the number the issue currently treats as an aside: **the measured
  element-count and events-per-instruction cost of BET-over-binary versus the projected native
  N-ary cost.** The issue already carries the estimate ("roughly 6-10x the element count for
  the ALU boundary"). Measuring it turns a 9-13 mw feature (#361) and a 17-feature capstone
  from a plan into a decision with evidence under it. If BET-over-binary is legible and cheap,
  #361's cost/benefit changes materially; if the ALU really is 10x and unreadable, that is the
  strongest possible case *for* funding the family, made for two weeks instead of twenty.
- Only then decide #345 OQ1, with a number rather than a preference.

This makes the problem the issue is fighting — a hard `blocked_by` on two unfiled tasks
(TASK-0061, TASK-0082) whose "issue numbers do not exist yet" — largely disappear, because the
BET machine's only irreducible counterparty is TASK-0082's emulator, and that is a plain Java
program against a written ISA (#428), not a value-domain change.

## Second reframe: the reusable asset is the harness, not the machine

#459 reinvents, for the second time, the machinery #392 (RV32) invented: a `machines/<cpu>/`
tree, a `CENSUS.md`, a `BRINGUP.md`, a bespoke `T3BringupTest`, and a package-private markdown
reader. Two machines with two hand-rolled ledgers and two document-parsing tests is one machine
too many to still be a coincidence. The general capability is: *compare a drawn circuit's
retired-instruction stream against a reference runner, boundary by boundary, against a declared
budget.* That is #347 (FEAT-034), and both machines should be its clients with **zero bespoke
test code**.

Concretely, and this is where I disregard the stated acceptance criteria:

- **Drop P1's exact per-tag census equality.** A test asserting `ĉ(b) = c(b)` exactly on a
  *generated* artifact is a golden over a derived value: it fails on every legitimate
  improvement, and its failures carry no information about correctness. The band
  (`Σc(b) ∈ [450,950]`) and the events budget (`[500,620]`) already catch "a boundary doubled,"
  which §11 names as the thing worth catching. Keep the budget; delete the exact-equality gate.
- **Stop treating `CENSUS.md` as a test fixture.** §7.5's "package-private census reader ... so
  the file format stays an internal contract between the document and its test" is a
  machine-readable contract wearing a document's clothes, and it guarantees the document and
  the gate can drift into each other. Put the numbers where the machine is (a sibling
  properties/JSON file, or attributes on the `.jls`), and **generate** the markdown. The
  document then cannot lie, and the reader disappears.
- **Draw only what you verify.** §6 commits eight `.jls` files (`talu`, `balu`, `regfile`,
  `decode`, `loadstore`, `br3`, `packunpack`, `cpu`) plus `t3-soc.jls`, then brings **two**
  green. P8's rule ("no green row without a named passing harness verdict") is a confession
  that six red rows are being committed. Unverified drawn circuits in-tree are worse than
  absent ones: they look like assets and are liabilities. Land `talu.jls` and `br3.jls` as
  verified standalone subcircuits with their harnesses, and land nothing else until it retires
  an instruction against the emulator. That also removes the §11 embarrassment of committing a
  `cpu.jls` that "opens as an illegible mesh" pending #62.

## What I would keep, unchanged and emphatically

- **P6 (the illegal-lane trap) is the best test in this issue.** "The same trap, not merely the
  same non-answer" is the only prediction that distinguishes two implementations that agree by
  accident. It is worth more than P1, P7, P8 and P9 combined, and it survives every reframing
  above.
- **P3's exhaustive-at-reduced-width discipline** (all 9 and all 81 cases) is exactly right and
  is what makes the 10^6 sample trustworthy rather than decorative.
- **H4, NEG as a plane swap**, is the single most teachable fact the whole machine contains —
  negation with no carry, no complement, no sign bit. If only one thing ships, ship that.
- **O4's measurement-over-taste discipline** (+6.94 vs +114.53 ev/cycle for `RegisterFile` vs a
  27-register farm) is the standard the rest of the ternary programme should be held to, and
  currently is not: #345's own cost section prints a 1.8x-3x gap between its task rows and its
  band and calls it Open Question 5.

## One accuracy note

The evidence-pin comment is right and load-bearing for O2/O3: `HdlExporter` on master has
`EXPORTED`/`SKIPPED`/`TOPOLOGY` (`:422`, `:431`, `:436`) and **no** `REJECTED` map. The
substantive claim survives — export still refuses this datapath — but the quoted per-class
reasons are a proposal (#492), not present behavior. P9's `CENSUS.md` statement should cite the
refusal, not the message text.
