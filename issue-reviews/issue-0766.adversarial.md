# Issue #766: TASK-C548-2: the curated set reaches ten circuits across combinational, sequential, FSM and datapath — and the RV32I showcase stops being unsurfaced
- Lens: adversarial
- Date: 2026-08-08
- Reviewer: Claude Sonnet 5
- Verdict: needs-rework

## Summary of the ask

#766 (TASK-C548-2, part of feature #548, `ordering_after: [TASK-C548-1]` i.e.
#764) is the content task in the CAP-27 (#511) on-ramp chain: scale the
`resources/samples/` set #381 (TASK-0030) is meant to seed from 3-5 circuits
to at least ten, spanning combinational/sequential/FSM/datapath, plus
surfacing the RV32I showcase, each circuit categorized and provenance-recorded.

## Findings, most severe first

**1. [High] AC-3's categorization data has no committed consumer — the menu
task that precedes this one in the ordering never agrees to read it.**
Quoted AC-3: "Each circuit is categorized in data the menu reads, so the
categories are a property of the set rather than of the menu code." But
#764 (TASK-C548-1, `ordering_after: [381]`, which #766 itself orders after)
has exactly five acceptance criteria, and none of them mention categories:
"A single top-level Examples entry appears in the menu bar and lists the
shipped circuits" (AC-1), "Selecting an entry opens the circuit through the
standard reader" (AC-2), "Menu items are wired to shared `Action` objects"
(AC-3), K9/D9 (AC-4), "not constructed in headless" (AC-5). Nothing requires
the menu to group, filter, label, or display by category. As written, AC-3
is satisfiable by writing category metadata into a manifest that nothing
ever reads — the same "data exists but nobody surfaces it" failure mode my
sibling review of #768 found for its caption AC-1. The Outcome section's own
framing ("spanning combinational, sequential, FSM and datapath categories")
implies a student can tell these apart from the menu; AC-3 as written does
not require that.
Recommendation: either add a criterion that #764's menu (or a follow-up
task against it) actually groups/labels entries by category, or drop the
pedagogical framing and state plainly that AC-3 is a data-shape requirement
only, satisfied by e.g. a `category=` field parsed at build time with no
UI consumer committed yet.

**2. [High] "The RV32I showcase" is treated as one unambiguous artifact; the
checkout has at least three non-equivalent candidates and #766 names none
of them.**
`riscv/gui/cpu.jls` (530 lines) is described in `riscv/gui/README.md` as "a
working, minimal single-cycle RISC-V (RV32I) **accumulator** CPU" that
executes exactly one instruction repeatedly ("addi x1, x1, 3 every rising
clock edge") — its own README's Scope note explains it deliberately has no
instruction memory ("uses a single hardwired addi immediate... rather than
a PC-indexed instruction memory"), so calling it "RV32I" is a stretch: it
demonstrates one opcode, not the ISA. The circuit that actually implements
the documented "full RV32I base integer instruction set" (`riscv/README.md`)
is not a static file at all — it's generated on demand by
`riscv/make_cpu.py` from an assembly source under `riscv/examples/`. The
closest thing to a checked-in full-ISA artifact is
`test/fixtures/riscv-sum1to10.jls` (9,360 lines — 17x the accumulator demo),
already a test fixture, not user-facing content. #766's AC-1 says only "the
RV32I showcase surfaced in the set," with no pointer to which of these it
means, whether it must be copied as-is, re-authored, or built at package
time, and none of the three obviously satisfies both "showcase" (implying
representative ISA coverage) and "fresh authorship or named fixture" (AC-4)
cleanly — the accumulator undersells "RV32I," the fixture is 17x larger than
anything else in a ten-circuit onboarding set, and the toolchain-generated
option doesn't exist as a file to point at yet.
Recommendation: name the specific source artifact (or the exact
`make_cpu.py` invocation and target program) #766 must use, and state
whether it is copied, regenerated at build time, or hand-recreated smaller.

**3. [Medium] AC-2 only tests headless batch simulation; nothing requires
the showcase to actually open or render acceptably in the GUI the Examples
menu (#764) exists to drive.**
Quoted AC-2: "Every circuit loads from the classpath on a clean install and
simulates under the batch simulator in a headless test." A circuit could
pass this — and, if it is the 9,360-line fixture, plausibly would, since
`BatchSimulator` never touches Swing — while being slow or unwieldy to open
in the interactive editor, which is the actual surface a student clicking
"Examples → RV32I showcase" hits. #766 never requires a GUI-open smoke test
or any complexity bound tied to K9/D9's "no other conceptual load" language
that #764 AC-4 invokes.
Recommendation: add a criterion (even a size/element-count ceiling, or an
explicit GUI-open timing check under the project's existing display-tagged
test tier) so a circuit that is technically "loadable" but impractical to
actually look at in the editor is caught before it ships as a "showcase."

**4. [Medium] AC-1's citation "(CAP-27 AC-3, AC-2 share)" mixes two
different issues' AC-numbering without saying which is which, and one half
looks like a misfire.**
CAP-27 (#511) AC-2 reads: "A scripted fresh-user protocol (documented,
re-runnable) measures install→running-example in <10 minutes" — the timed
usability protocol, unrelated to category coverage. The four-category
requirement AC-1 is actually describing matches #548's own AC-2 ("Category
coverage: at least one combinational, one sequential, one FSM and one
datapath example, plus the RV32I showcase, are in the set") almost
verbatim. A reader chasing "CAP-27 AC-2" for the category rule will land on
the wrong acceptance criterion in the wrong issue.
Recommendation: cite `#548 AC-2` explicitly instead of the unqualified
"AC-2," and reserve bare "AC-2"/"AC-3" for #766's own criteria to avoid the
three-way collision (#766, #548, and #511 all number their ACs starting
from 1).

**5. [Medium] AC-4's binary licensing framing ("authored fresh, or the
fixture it was taken from named") does not fit machine-generated content,
and names no location for the record.**
The RV32I candidates are either produced by a `java.awt.Robot`-driven GUI
script (`riscv/gui/GuiDriver.java`) or by a Python assembler
(`riscv/make_cpu.py`) — neither "authored fresh" in the sense the other nine
hand-drawn circuits would be, nor a "fixture" in the `test/fixtures/`
sense the phrase evokes elsewhere in this issue chain. AC-4 also never says
where "recorded" means — a doc file, a header comment, a commit message.
This is the same unlocated-record gap my sibling review of #768 flagged for
its AC-2 transcript requirement.
Recommendation: extend AC-4's taxonomy to cover "generated by an in-repo
tool, tool and invocation named" as a third case, and pin the record to a
specific artifact (e.g., a `PROVENANCE.md` under `resources/samples/` or a
comment convention in each circuit's header `Text` element).

**6. [Low] AC-2's "extended to the full set, not a fixed subset" presupposes
`SampleCircuitsTest` already exists from #381, which is itself open and
unimplemented.**
`resources/samples/` does not exist in this checkout (confirmed:
`find . -iname resources -o -type d -iname '*sample*'` returns nothing
under `resources/`), matching #381's own O3 finding ("`resources/samples/`
does not exist today"). #766's `ordering_after: [TASK-C548-1]` (#764) is
technically correct — #764 itself orders after #381 — but the chain is two
unstarted issues deep before #766 has anything to extend, a planning
hazard my sibling reviews of #768 and #773 already flagged for the same
dependency line one and two hops further out. Unlike those two, #766 does
name #381 explicitly in its Outcome prose ("the 3-5 sample baseline #381
builds is scaled to..."), which is better citation discipline than the
downstream tasks show.
Recommendation: no action required beyond what's already good practice; a
one-line note ("if #381 has not landed, create `SampleCircuitsTest` from
scratch using #381 §8's planned shape") would remove the last bit of
ambiguity for whoever picks this up out of order.

**7. [Low] Copying rather than build-time-generating the RV32I showcase
creates a silent-drift risk with no test catching it.**
If the eventual source is `riscv/gui/cpu.jls` or a `make_cpu.py` output,
and that source is later regenerated (a driver bug fix, an ISA coverage
fix), the copy placed under `resources/samples/` has no committed
mechanism keeping it in sync — #766 states no such check.
Recommendation: either generate the sample at build time from the
canonical `riscv/` source (one command, per `riscv/README.md`'s Quick
start), or add a CI parity check between the two paths.

## What's solid

- The four-category-plus-RV32I coverage bar (AC-1/AC-2) is concrete and
  mechanically checkable once the set exists.
- AC-2's classpath-read, headless-batch-simulate pattern matches #381's
  already-planned `SampleCircuitsTest` shape (P3, P8) rather than inventing
  a parallel mechanism — consistent with the project's stated "don't fork a
  second sample mechanism" rule (#548's Boundary and reference notes).
- Explicitly citing #381 and the 2026-07-17 fresh-authorship decision by
  name in the Outcome section is better dependency hygiene than some
  sibling issues in this chain show for their own predecessors.
- Scope is properly narrow: #766 does not try to build the Examples menu
  (#764) or the caption/exercise metadata (#768) itself, leaving those to
  their own tasks.
- Labels (`enhancement`, `area:ux`, `tier:task`) match the issue's content.

## Verdict rationale

`needs-rework`: the circuit-authoring and count/category bar is sound, but
findings 1 and 2 are load-bearing — AC-3's categorization requirement can be
satisfied by data nothing ever reads, and "the RV32I showcase" names no
actual artifact among at least three non-equivalent candidates already in
the repo, one of which (the 9,360-line test fixture) is a poor onboarding
fit and none of which cleanly satisfies AC-4's licensing taxonomy. Both gaps
let an implementation close every stated acceptance criterion while missing
the on-ramp goal CAP-27 exists to serve, which is the same failure pattern
flagged in sibling reviews of #768 and #773 elsewhere in this task chain.
